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
import java.util.List;
import org.immutables.value.Value;

/**
 * Component element containing language injection configuration.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableIntelliLangComponent.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface IntelliLangComponent {

    @JacksonXmlProperty(isAttribute = true)
    @Value.Default
    default String name() {
        return "LanguageInjectionConfiguration";
    }

    @JacksonXmlProperty(localName = "injection")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<IntelliLangInjection> injections();

    static ImmutableIntelliLangComponent.Builder builder() {
        return ImmutableIntelliLangComponent.builder();
    }

    static IntelliLangComponent of(List<IntelliLangInjection> injections) {
        return builder().injections(injections).build();
    }
}
