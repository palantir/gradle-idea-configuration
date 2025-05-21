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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.palantir.gradle.ideaconfiguration.externaldependencies.ComponentXml;
import com.palantir.gradle.ideaconfiguration.externaldependencies.PluginDependencyXml;
import com.palantir.gradle.ideaconfiguration.externaldependencies.ProjectXml;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdateExternalDependenciesXml extends DefaultTask {
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
            return;
        }

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        List<PluginDependencyXml> existingPluginXmls = readExistingPluginXmls(xmlMapper, outputFile);
        List<PluginDependencyXml> newPluginXmls = toXmlDependencies(dependencies);
        List<PluginDependencyXml> merged = mergePluginXmls(existingPluginXmls, newPluginXmls);

        writeMergedXml(xmlMapper, outputFile, merged);
    }

    private static List<PluginDependencyXml> readExistingPluginXmls(XmlMapper xmlMapper, File outputFile) {
        if (!outputFile.exists()) {
            return List.of();
        }
        try {
            ProjectXml existingProject = xmlMapper.readValue(outputFile, ProjectXml.class);
            if (existingProject != null && existingProject.component() != null) {
                return existingProject.component().plugins();
            }
        } catch (IOException e) {
            // Ignore and return empty
        }
        return List.of();
    }

    private static List<PluginDependencyXml> toXmlDependencies(Set<PluginDependency> deps) {
        return deps.stream().map(PluginDependencyXml::from).collect(Collectors.toList());
    }

    private static List<PluginDependencyXml> mergePluginXmls(
            List<PluginDependencyXml> existing, List<PluginDependencyXml> incoming) {
        return java.util.stream.Stream.concat(existing.stream(), incoming.stream())
                .collect(Collectors.toMap(
                        PluginDependencyXml::id, dep -> dep, UpdateExternalDependenciesXml::pickHigherVersion))
                .values()
                .stream()
                .sorted(Comparator.comparing(PluginDependencyXml::id))
                .collect(Collectors.toList());
    }

    private static PluginDependencyXml pickHigherVersion(
            PluginDependencyXml firstDependency, PluginDependencyXml secondDependency) {
        String firstVersion = firstDependency.minVersion();
        String secondVersion = secondDependency.minVersion();
        if (firstVersion == null && secondVersion == null) {
            return firstDependency;
        }
        if (firstVersion == null) {
            return secondDependency;
        }
        if (secondVersion == null) {
            return firstDependency;
        }
        return PluginDependency.compareVersions(firstVersion, secondVersion) >= 0 ? firstDependency : secondDependency;
    }

    private void writeMergedXml(XmlMapper xmlMapper, File outputFile, List<PluginDependencyXml> merged) {
        ComponentXml component = ComponentXml.of(merged);
        ProjectXml project = ProjectXml.of(component);
        try {
            xmlMapper.writeValue(outputFile, project);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write back to configuration file: "
                            + getOutputFile().get(),
                    e);
        }
    }
}
