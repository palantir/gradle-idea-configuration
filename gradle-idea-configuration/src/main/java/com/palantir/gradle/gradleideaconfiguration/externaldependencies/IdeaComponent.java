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

package com.palantir.gradle.gradleideaconfiguration.externaldependencies;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableIdeaComponent.class)
@JsonSerialize(as = ImmutableIdeaComponent.class)
public interface IdeaComponent {
    @JacksonXmlProperty(isAttribute = true)
    String name();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "plugin")
    List<IdeaPlugin> plugins();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableIdeaComponent.Builder {}
}
