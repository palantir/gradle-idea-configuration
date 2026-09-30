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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
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
        Document document = readOrCreate(xmlFile);
        components.forEach(component -> {
            Element componentElement =
                    matchOrCreateChild(document.getDocumentElement(), "component", component.getName());
            component
                    .getOptions()
                    .get()
                    .forEach((name, value) ->
                            matchOrCreateChild(componentElement, "option", name).setAttribute("value", value));
        });
        write(xmlFile, document);
    }

    private static Document readOrCreate(File xmlFile) {
        DocumentBuilder documentBuilder = newDocumentBuilder();
        if (!xmlFile.isFile()) {
            Document document = documentBuilder.newDocument();
            Element project = document.createElement("project");
            project.setAttribute("version", "4");
            document.appendChild(project);
            return document;
        }
        try {
            return documentBuilder.parse(xmlFile);
        } catch (IOException | SAXException e) {
            throw new GradleException("Couldn't parse existing configuration file: " + xmlFile, e);
        }
    }

    private static DocumentBuilder newDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new GradleException("Couldn't create an XML parser", e);
        }
    }

    private static Element matchOrCreateChild(Element parent, String elementName, String nameAttribute) {
        NodeList children = parent.getChildNodes();
        return IntStream.range(0, children.getLength())
                .mapToObj(children::item)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(child ->
                        elementName.equals(child.getTagName()) && nameAttribute.equals(child.getAttribute("name")))
                .findFirst()
                .orElseGet(() -> appendChild(parent, elementName, nameAttribute));
    }

    private static Element appendChild(Element parent, String elementName, String nameAttribute) {
        Document document = parent.getOwnerDocument();
        String parentIndent = "  ".repeat(depth(parent));
        Node closingWhitespace =
                parent.getLastChild() instanceof Text text && text.getData().isBlank()
                        ? text
                        : parent.appendChild(document.createTextNode("\n" + parentIndent));
        Element child = document.createElement(elementName);
        child.setAttribute("name", nameAttribute);
        parent.insertBefore(document.createTextNode("\n" + parentIndent + "  "), closingWhitespace);
        parent.insertBefore(child, closingWhitespace);
        return child;
    }

    private static int depth(Node node) {
        int depth = 0;
        for (Node ancestor = node.getParentNode(); ancestor instanceof Element; ancestor = ancestor.getParentNode()) {
            depth++;
        }
        return depth;
    }

    private static void write(File xmlFile, Document document) {
        StringWriter xml = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(xml));
        } catch (TransformerException e) {
            throw new GradleException("Couldn't serialise configuration file: " + xmlFile, e);
        }
        try {
            Files.writeString(xmlFile.toPath(), xml.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write back to configuration file: " + xmlFile, e);
        }
    }
}
