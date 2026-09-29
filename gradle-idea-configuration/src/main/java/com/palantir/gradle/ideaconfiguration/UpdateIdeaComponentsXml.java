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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.xml.sax.SAXException;

@DisableCachingByDefault(because = "Updates IntelliJ configuration files in place")
public abstract class UpdateIdeaComponentsXml extends DefaultTask {

    @Nested
    public abstract SetProperty<IdeaComponent> getComponents();

    @Internal("The files written are tracked by getXmlFiles()")
    public abstract DirectoryProperty getIdeaDirectory();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateIdeaComponentsXml() {
        getIdeaDirectory().set(getProjectLayout().getProjectDirectory().dir(".idea"));
        onlyIf(
                "at least one component is configured",
                _task -> !getComponents().get().isEmpty());
    }

    @OutputFiles
    public final FileCollection getXmlFiles() {
        return getProjectLayout()
                .files(getComponents()
                        .zip(
                                getIdeaDirectory(),
                                (components, ideaDirectory) -> components.stream()
                                        .map(component -> xmlFile(ideaDirectory, component))
                                        .collect(Collectors.toSet())));
    }

    @TaskAction
    public final void updateXml() {
        Directory ideaDirectory = getIdeaDirectory().get();
        Map<File, List<IdeaComponent>> componentsByFile = getComponents().get().stream()
                .collect(Collectors.groupingBy(component -> xmlFile(ideaDirectory, component)));
        componentsByFile.forEach(UpdateIdeaComponentsXml::updateXmlFile);
    }

    private static File xmlFile(Directory ideaDirectory, IdeaComponent component) {
        return ideaDirectory.file(component.getFile().get()).getAsFile();
    }

    private static void updateXmlFile(File xmlFile, List<IdeaComponent> components) {
        Node rootNode = readOrCreate(xmlFile);
        components.forEach(component -> {
            Node componentNode = matchOrCreateChild(rootNode, "component", component.getName());
            component
                    .getOptions()
                    .get()
                    .forEach((name, value) -> attributes(matchOrCreateChild(componentNode, "option", name))
                            .put("value", value));
        });
        write(xmlFile, rootNode);
    }

    private static Node readOrCreate(File xmlFile) {
        if (!xmlFile.isFile()) {
            return new Node(null, "project", orderedAttributes("version", "4"));
        }
        try {
            return new XmlParser().parse(xmlFile);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new GradleException("Couldn't parse existing configuration file: " + xmlFile, e);
        }
    }

    private static Node matchOrCreateChild(Node parent, String elementName, String nameAttribute) {
        return ((List<?>) parent.children())
                .stream()
                        .filter(Node.class::isInstance)
                        .map(Node.class::cast)
                        .filter(child ->
                                elementName.equals(child.name()) && nameAttribute.equals(child.attribute("name")))
                        .findFirst()
                        .orElseGet(() -> parent.appendNode(elementName, orderedAttributes("name", nameAttribute)));
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> attributes(Node node) {
        return node.attributes();
    }

    private static Map<String, String> orderedAttributes(String name, String value) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put(name, value);
        return attributes;
    }

    private static void write(File xmlFile, Node rootNode) {
        try {
            Files.createDirectories(xmlFile.toPath().getParent());
            try (PrintWriter writer =
                    new PrintWriter(Files.newBufferedWriter(xmlFile.toPath(), StandardCharsets.UTF_8))) {
                XmlNodePrinter printer = new XmlNodePrinter(writer);
                printer.setPreserveWhitespace(true);
                printer.print(rootNode);
                if (writer.checkError()) {
                    throw new IOException("Error writing " + xmlFile);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write back to configuration file: " + xmlFile, e);
        }
    }
}
