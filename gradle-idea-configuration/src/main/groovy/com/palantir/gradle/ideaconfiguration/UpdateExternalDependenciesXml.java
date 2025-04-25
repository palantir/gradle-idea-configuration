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

import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdateExternalDependenciesXml extends DefaultTask {
    @Input
    public abstract SetProperty<PluginDependency> getDependencies();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public UpdateExternalDependenciesXml() {
        getOutputFile().set(getProject().file(".idea/externalDependencies.xml"));
    }

    @TaskAction
    public final void updateXml() {
        Set<PluginDependency> dependencies = getDependencies().get();

        if (dependencies.isEmpty()) {
            getLogger().info("No external dependencies to update.");
            return;
        }

        XmlUtils.createOrUpdateIdeaXmlFile(
                getProject().file(".idea/externalDependencies.xml"),
                node -> ConfigureXml.configureExternalDependencies(node, dependencies));
    }
}
