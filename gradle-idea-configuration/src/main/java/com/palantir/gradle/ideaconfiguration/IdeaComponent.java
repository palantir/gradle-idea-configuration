/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

import java.io.Serializable;
import org.gradle.api.Named;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;

public abstract class IdeaComponent implements Named {

    @Input
    @Override
    public abstract String getName();

    /** The XML file the component lives in, relative to the {@code .idea} directory, e.g. {@code compiler.xml}. */
    @Input
    public abstract Property<String> getFile();

    /** Every {@link #option} call, including repeated calls for the same option name. */
    @Input
    public abstract ListProperty<Option> getOptions();

    /** Written as an {@code <option name="name" value="value"/>} child of the component. */
    public final void option(String name, String value) {
        getOptions().add(new Option(name, value));
    }

    /** Written as an {@code <option name="name" value="value"/>} child of the component. */
    public final void option(String name, Provider<String> value) {
        getOptions().add(value.map(resolvedValue -> new Option(name, resolvedValue)));
    }

    public record Option(String name, String value) implements Serializable {}
}
