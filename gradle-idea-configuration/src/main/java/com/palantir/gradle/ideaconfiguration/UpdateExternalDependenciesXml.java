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

        // If idea-configuration is managing the external dependencies but no dependencies are defined, delete the file
        if (dependencies.isEmpty()) {
            if (!outputFile.exists()) {
                return;
            }

            boolean deleted = outputFile.delete();
            if (!deleted) {
                throw new RuntimeException("Failed to delete configuration file: " + outputFile);
            }
            return;
        }

        List<PluginDependencyXml> pluginXmls = toXmlDependencies(dependencies);
        ComponentXml component = ComponentXml.of(pluginXmls);
        ProjectXml project = ProjectXml.of(component);

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            xmlMapper.writeValue(outputFile, project);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write back to configuration file: "
                            + getOutputFile().get(),
                    e);
        }
    }

    private static List<PluginDependencyXml> toXmlDependencies(Set<PluginDependency> deps) {
        return deps.stream().map(PluginDependencyXml::from).collect(Collectors.toList());
    }
}
