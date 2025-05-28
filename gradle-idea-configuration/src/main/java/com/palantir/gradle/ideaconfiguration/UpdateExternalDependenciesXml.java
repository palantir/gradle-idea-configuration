/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.gradle.ideaconfiguration.externaldependencies.ExternalDependenciesComponent;
import com.palantir.gradle.ideaconfiguration.externaldependencies.ExternalDependenciesPlugin;
import com.palantir.gradle.ideaconfiguration.externaldependencies.ExternalDependenciesProject;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UpdateExternalDependenciesXml extends DefaultTask {
    private static final Logger log = LoggerFactory.getLogger(UpdateExternalDependenciesXml.class);

    private static final ObjectMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory())
            .registerModule(new Jdk8Module())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Nested
    public abstract SetProperty<PluginDependency> getDependencies();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public UpdateExternalDependenciesXml() {
        getOutputFile().set(getProject().file(".idea/externalDependencies.xml"));
    }

    @TaskAction
    public final void updateXml() {
        Set<PluginDependency> dependencies = getDependencies().get();
        File outputFile = getOutputFile().get().getAsFile();

        if (dependencies.isEmpty()) {
            log.info("No external dependencies found. Skipping update.");
            return;
        }

        Optional<ExternalDependenciesProject> existingXml = readXml(outputFile);
        List<ExternalDependenciesPlugin> addedPlugins = toExternalDependenciesPlugins(dependencies);
        ExternalDependenciesProject updatedXml = mergePluginsIntoProject(existingXml, addedPlugins);

        writeXml(outputFile, updatedXml);
    }

    private static Optional<ExternalDependenciesProject> readXml(File outputFile) {
        if (!outputFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(XML_MAPPER.readValue(outputFile, ExternalDependenciesProject.class));
        } catch (IOException e) {
            log.error("Failed to parse existing configuration file: {}", outputFile, e);
        }
        return Optional.empty();
    }

    private static ExternalDependenciesProject mergePluginsIntoProject(
            Optional<ExternalDependenciesProject> externalDependenciesProject,
            List<ExternalDependenciesPlugin> externalDependenciesPlugins) {

        List<ExternalDependenciesPlugin> mergedPlugins = Stream.concat(
                        externalDependenciesProject
                                .map(ExternalDependenciesProject::component)
                                .map(ExternalDependenciesComponent::plugins)
                                .orElseGet(Collections::emptyList)
                                .stream(),
                        externalDependenciesPlugins.stream())
                .collect(Collectors.toMap(
                        ExternalDependenciesPlugin::id, dep -> dep, UpdateExternalDependenciesXml::pickHigherVersion))
                .values()
                .stream()
                .sorted(Comparator.comparing(ExternalDependenciesPlugin::id))
                .collect(Collectors.toList());

        String version = externalDependenciesProject
                .map(ExternalDependenciesProject::version)
                .orElse("4");
        return ExternalDependenciesProject.of(ExternalDependenciesComponent.of(mergedPlugins), version);
    }

    private static List<ExternalDependenciesPlugin> toExternalDependenciesPlugins(Set<PluginDependency> deps) {
        return deps.stream().map(ExternalDependenciesPlugin::from).collect(Collectors.toList());
    }

    private static ExternalDependenciesPlugin pickHigherVersion(
            ExternalDependenciesPlugin dep1, ExternalDependenciesPlugin dep2) {

        Optional<String> v1 = dep1.minVersion();
        Optional<String> v2 = dep2.minVersion();

        if (v1.isPresent() && v2.isPresent()) {
            return PluginDependency.compareVersions(v1.get(), v2.get()) >= 0 ? dep1 : dep2;
        }
        if (v1.isPresent()) {
            return dep1;
        }
        return dep2;
    }

    private void writeXml(File outputFile, ExternalDependenciesProject updatedXml) {
        try {
            XML_MAPPER.writeValue(outputFile, updatedXml);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write back to configuration file: "
                            + getOutputFile().get(),
                    e);
        }
    }
}
