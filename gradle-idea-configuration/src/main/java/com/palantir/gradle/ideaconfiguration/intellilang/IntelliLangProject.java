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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value;

/**
 * Root element for IntelliLang.xml configuration file.
 */
@Value.Immutable
@JacksonXmlRootElement(localName = "project")
@JsonDeserialize(as = ImmutableIntelliLangProject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface IntelliLangProject {

    @JacksonXmlProperty(isAttribute = true)
    String version();

    @JacksonXmlProperty(localName = "component")
    IntelliLangComponent component();

    static ImmutableIntelliLangProject.Builder builder() {
        return ImmutableIntelliLangProject.builder();
    }

    static IntelliLangProject of(IntelliLangComponent component, String version) {
        return builder().component(component).version(version).build();
    }
}
