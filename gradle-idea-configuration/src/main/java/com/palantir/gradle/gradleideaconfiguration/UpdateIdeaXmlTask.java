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

package com.palantir.gradle.gradleideaconfiguration;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.gradle.gradleideaconfiguration.externaldependencies.IdeaComponent;
import com.palantir.gradle.gradleideaconfiguration.externaldependencies.IdeaPlugin;
import com.palantir.gradle.gradleideaconfiguration.externaldependencies.IdeaProject;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class UpdateIdeaXmlTask extends DefaultTask {
    private static final ObjectMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());

    @Input
    public abstract SetProperty<PluginDependency> getDependencies();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public UpdateIdeaXmlTask() {
        getOutputFile().set(getProject().file(".idea/externalDependencies.xml"));
    }

    @TaskAction
    public final void updateXml() throws IOException {
        File file = getOutputFile().get().getAsFile();
        Set<PluginDependency> dependencies = getDependencies().get();

        if (dependencies.isEmpty()) {
            getLogger().info("No external dependencies to update.");
            return;
        }

        IdeaProject ideaProject;
        if (file.length() == 0) {
            ideaProject = createDefaultIdeaProject();
        } else {
            ideaProject = XML_MAPPER.readValue(file, IdeaProject.class);
            if (ideaProject.components().stream().noneMatch(c -> "ExternalDependencies".equals(c.name()))) {
                getLogger()
                        .warn("No 'ExternalDependencies' component found in the existing XML file. Creating a new"
                                + " one.");
                ideaProject = createDefaultIdeaProject();
            }
        }

        List<IdeaComponent> updatedComponents = ideaProject.components().stream()
                .map(component -> "ExternalDependencies".equals(component.name())
                        ? updateExternalDependencies(component, dependencies)
                        : component)
                .collect(Collectors.toList());

        IdeaProject updatedProject = IdeaProject.builder()
                .from(ideaProject)
                .components(updatedComponents)
                .build();

        try {
            XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, updatedProject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IdeaComponent updateExternalDependencies(IdeaComponent component, Set<PluginDependency> dependencies) {
        List<IdeaPlugin> dependencyPlugins = dependencies.stream()
                .map(dep -> IdeaPlugin.builder()
                        .id(dep.name())
                        .minVersion(Optional.ofNullable(dep.minVersion()))
                        .build())
                .collect(Collectors.toList());

        List<IdeaPlugin> existingPlugins = component.plugins();

        Map<String, IdeaPlugin> uniquePlugins = Stream.concat(dependencyPlugins.stream(), existingPlugins.stream())
                .collect(Collectors.toMap(IdeaPlugin::id, Function.identity(), (plugin1, plugin2) -> {
                    if (plugin1.minVersion().isPresent() && plugin2.minVersion().isPresent()) {
                        return (PluginDependency.compareVersions(
                                                plugin1.minVersion().get(),
                                                plugin2.minVersion().get())
                                        >= 0)
                                ? plugin1
                                : plugin2;
                    }
                    if (plugin1.minVersion().isPresent()) {
                        return plugin1;
                    }
                    return plugin2;
                }));

        List<IdeaPlugin> combined = uniquePlugins.values().stream()
                .sorted(Comparator.comparing(IdeaPlugin::id))
                .collect(Collectors.toList());

        return IdeaComponent.builder().from(component).plugins(combined).build();
    }

    private IdeaProject createDefaultIdeaProject() {
        IdeaComponent externalComponent = IdeaComponent.builder()
                .name("ExternalDependencies")
                .plugins(Collections.emptyList())
                .build();
        return IdeaProject.builder()
                .version("4")
                .addComponents(externalComponent)
                .build();
    }
}
