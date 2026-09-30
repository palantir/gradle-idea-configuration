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

import java.util.ArrayList;
import java.util.List;
import org.gradle.StartParameter;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class IdeaConfigurationPlugin implements Plugin<Project> {

    @Override
    public final void apply(Project project) {
        if (!project.getRootProject().equals(project)) {
            throw new GradleException("Must be applied only to root project");
        }

        IdeaConfigurationExtension extension =
                project.getExtensions().create("ideaConfiguration", IdeaConfigurationExtension.class);

        if (!Boolean.getBoolean("idea.active")) {
            return;
        }

        registerSyncTask(project, "updateExternalDepsXml", UpdateExternalDependenciesXml.class, task -> {
            task.getDependencies().set(extension.getExternalDependencies());
        });
        registerSyncTask(project, "updateIdeaComponentsXml", UpdateIdeaComponentsXml.class, task -> {
            task.getComponents().set(extension.getComponents());
        });
    }

    private static <T extends Task> void registerSyncTask(
            Project project, String name, Class<T> type, Action<? super T> configure) {
        project.getTasks().register(name, type, configure);

        // Add the task to the Gradle start parameters so it executes automatically.
        StartParameter startParameter = project.getGradle().getStartParameter();
        List<String> taskNames = new ArrayList<>(startParameter.getTaskNames());
        taskNames.add(":" + name);
        startParameter.setTaskNames(taskNames);
    }
}
