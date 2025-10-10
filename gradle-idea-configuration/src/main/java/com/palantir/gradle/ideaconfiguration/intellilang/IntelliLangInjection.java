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

package com.palantir.gradle.ideaconfiguration.intellilang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.palantir.gradle.ideaconfiguration.LanguageInjection;
import java.util.List;
import org.immutables.value.Value;

/**
 * Represents a single language injection rule in IntelliLang.xml.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableIntelliLangInjection.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface IntelliLangInjection {

    @JacksonXmlProperty(isAttribute = true)
    String language();

    @JacksonXmlProperty(isAttribute = true, localName = "injector-id")
    @Value.Default
    default String injectorId() {
        return "java";
    }

    @JacksonXmlProperty(localName = "display-name")
    String displayName();

    @JacksonXmlProperty(localName = "single-file")
    @Value.Default
    default IntelliLangSingleFile singleFile() {
        return ImmutableIntelliLangSingleFile.builder().build();
    }

    @JacksonXmlProperty(localName = "place")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<IntelliLangPlace> places();

    static ImmutableIntelliLangInjection.Builder builder() {
        return ImmutableIntelliLangInjection.builder();
    }

    static IntelliLangInjection from(LanguageInjection injection) {
        String displayName = injection.getDisplayName().isPresent()
                ? injection.getDisplayName().get()
                : injection.getName();

        return builder()
                .language(injection.getLanguage().get())
                .displayName(displayName)
                .addPlaces(IntelliLangPlace.of(injection.getPattern().get()))
                .build();
    }
}
