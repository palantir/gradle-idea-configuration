/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.ideaconfiguration;

import groovy.util.Node;
import groovy.xml.XmlNodePrinter;
import groovy.xml.XmlParser;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.xml.sax.SAXException;

@DisableCachingByDefault(because = "Updates IntelliJ configuration files in place")
public abstract class UpdateIdeaComponentsXml extends DefaultTask {

    @Nested
    public abstract SetProperty<IdeaComponent> getComponents();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateIdeaComponentsXml() {
        onlyIf(
                "at least one component is configured",
                _task -> !getComponents().get().isEmpty());
    }

    @OutputFiles
    public final FileCollection getXmlFiles() {
        return getProjectLayout()
                .files(getComponents()
                        .map(components ->
                                components.stream().map(this::xmlFile).collect(Collectors.toSet())));
    }

    @TaskAction
    public final void updateXml() {
        getComponents().get().stream()
                .collect(Collectors.groupingBy(this::xmlFile))
                .forEach(UpdateIdeaComponentsXml::updateXmlFile);
    }

    private File xmlFile(IdeaComponent component) {
        if (!component.getFile().isPresent()) {
            throw new GradleException("IntelliJ component '" + component.getName()
                    + "' must set the file it lives in under .idea/, e.g. file = 'compiler.xml'");
        }
        return getProjectLayout()
                .getProjectDirectory()
                .dir(".idea")
                .file(component.getFile().get())
                .getAsFile();
    }

    private static void updateXmlFile(File xmlFile, List<IdeaComponent> components) {
        Node rootNode = readOrCreate(xmlFile);
        components.forEach(component -> {
            Node componentNode = matchOrCreateChild(rootNode, "component", component.getName());
            options(component).forEach((name, value) -> {
                Node optionNode = matchOrCreateChild(componentNode, "option", name);
                attributes(optionNode).put("value", value);
            });
        });
        write(xmlFile, rootNode);
    }

    private static Map<String, String> options(IdeaComponent component) {
        Map<String, String> options = new LinkedHashMap<>();
        component
                .getOptions()
                .get()
                .forEach(option -> Optional.ofNullable(options.putIfAbsent(option.name(), option.value()))
                        .filter(existingValue -> !existingValue.equals(option.value()))
                        .ifPresent(existingValue -> {
                            throw new GradleException("IntelliJ component '" + component.getName() + "' sets option '"
                                    + option.name() + "' to both '" + existingValue + "' and '" + option.value() + "'");
                        }));
        return options;
    }

    private static Node readOrCreate(File xmlFile) {
        if (!xmlFile.isFile()) {
            return new Node(null, "project", new LinkedHashMap<>(Map.of("version", "4")));
        }
        try {
            return new XmlParser().parse(xmlFile);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new GradleException("Couldn't parse existing configuration file: " + xmlFile, e);
        }
    }

    private static Node matchOrCreateChild(Node parent, String elementName, String nameAttribute) {
        List<?> children = parent.children();
        return children.stream()
                .filter(Node.class::isInstance)
                .map(Node.class::cast)
                .filter(child -> elementName.equals(child.name()) && nameAttribute.equals(child.attribute("name")))
                .findFirst()
                .orElseGet(() -> parent.appendNode(elementName, new LinkedHashMap<>(Map.of("name", nameAttribute))));
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> attributes(Node node) {
        return node.attributes();
    }

    private static void write(File xmlFile, Node rootNode) {
        StringWriter xml = new StringWriter();
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(xml));
        printer.setPreserveWhitespace(true);
        printer.print(rootNode);
        try {
            Files.writeString(xmlFile.toPath(), xml.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write back to configuration file: " + xmlFile, e);
        }
    }
}
