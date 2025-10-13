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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.gradle.ideaconfiguration.intellilang.IntelliLangComponent;
import com.palantir.gradle.ideaconfiguration.intellilang.IntelliLangInjection;
import com.palantir.gradle.ideaconfiguration.intellilang.IntelliLangPlace;
import com.palantir.gradle.ideaconfiguration.intellilang.IntelliLangProject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UpdateIntelliLangXml extends DefaultTask {
    private static final Logger log = LoggerFactory.getLogger(UpdateIntelliLangXml.class);

    private static final ObjectMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory())
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Nested
    public abstract SetProperty<LanguageInjection> getInjections();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateIntelliLangXml() {
        getOutputFile().set(getProjectLayout().getProjectDirectory().file(".idea/IntelliLang.xml"));
    }

    @TaskAction
    public final void updateXml() {
        Set<LanguageInjection> injections = getInjections().get();
        File outputFile = getOutputFile().get().getAsFile();

        if (injections.isEmpty()) {
            log.info("No language injections found. Skipping update.");
            return;
        }

        List<IntelliLangInjection> addedInjections = toIntelliLangInjections(injections);
        IntelliLangProject updatedXml = readXml(outputFile)
                .map(existingProject -> mergeInjectionsIntoXml(existingProject, addedInjections))
                .orElseGet(() -> createNewProject(addedInjections));

        writeXml(outputFile, updatedXml);
    }

    private static Optional<IntelliLangProject> readXml(File outputFile) {
        if (!outputFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(XML_MAPPER.readValue(outputFile, IntelliLangProject.class));
        } catch (IOException e) {
            log.error("Failed to parse existing configuration file: {}", outputFile, e);
        }
        return Optional.empty();
    }

    private static IntelliLangProject mergeInjectionsIntoXml(
            IntelliLangProject existingProject, List<IntelliLangInjection> newInjections) {

        List<IntelliLangInjection> existingInjections =
                existingProject.component().injections();

        // Use composite key: (displayName, language, injectorId)
        Map<InjectionKey, IntelliLangInjection> mergedMap = Stream.concat(
                        existingInjections.stream(), newInjections.stream())
                .collect(Collectors.toMap(
                        InjectionKey::from,
                        injection -> injection,
                        // If duplicate key, merge the places from both injections
                        UpdateIntelliLangXml::mergeInjections,
                        LinkedHashMap::new));

        List<IntelliLangInjection> mergedInjections = mergedMap.values().stream()
                .sorted(Comparator.comparing(IntelliLangInjection::displayName)
                        .thenComparing(IntelliLangInjection::language)
                        .thenComparing(IntelliLangInjection::injectorId))
                .collect(Collectors.toList());

        return IntelliLangProject.of(IntelliLangComponent.of(mergedInjections), existingProject.version());
    }

    private static IntelliLangInjection mergeInjections(
            IntelliLangInjection existing, IntelliLangInjection replacement) {
        // Combine places from both injections and remove duplicates
        List<String> mergedPlaces = Stream.concat(existing.places().stream(), replacement.places().stream())
                .map(IntelliLangPlace::pattern)
                .distinct()
                .sorted()
                .toList();

        return IntelliLangInjection.builder()
                .from(existing)
                .places(mergedPlaces.stream().map(IntelliLangPlace::of).collect(Collectors.toList()))
                .build();
    }

    private static IntelliLangProject createNewProject(List<IntelliLangInjection> injections) {
        // Merge injections with same composite key before creating project
        Map<InjectionKey, IntelliLangInjection> mergedMap = injections.stream()
                .collect(Collectors.toMap(
                        InjectionKey::from,
                        injection -> injection,
                        UpdateIntelliLangXml::mergeInjections,
                        LinkedHashMap::new));

        List<IntelliLangInjection> sortedInjections = mergedMap.values().stream()
                .sorted(Comparator.comparing(IntelliLangInjection::displayName)
                        .thenComparing(IntelliLangInjection::language)
                        .thenComparing(IntelliLangInjection::injectorId))
                .collect(Collectors.toList());
        return IntelliLangProject.of(IntelliLangComponent.of(sortedInjections), "4");
    }

    private static List<IntelliLangInjection> toIntelliLangInjections(Set<LanguageInjection> injections) {
        return injections.stream().map(IntelliLangInjection::from).collect(Collectors.toList());
    }

    private void writeXml(File outputFile, IntelliLangProject updatedXml) {
        try {
            outputFile.getParentFile().mkdirs();
            XML_MAPPER.writeValue(outputFile, updatedXml);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write back to configuration file: "
                            + getOutputFile().get(),
                    e);
        }
    }

    /**
     * Composite key for uniquely identifying an injection by display name, language, and injector ID.
     */
    private record InjectionKey(String displayName, String language, String injectorId) {
        static InjectionKey from(IntelliLangInjection injection) {
            return new InjectionKey(injection.displayName(), injection.language(), injection.injectorId());
        }
    }
}
