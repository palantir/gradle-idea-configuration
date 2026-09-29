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

import org.gradle.api.Named;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public abstract class IdeaComponent implements Named {

    @Input
    @Override
    public abstract String getName();

    /** The XML file the component lives in, relative to the {@code .idea} directory, e.g. {@code compiler.xml}. */
    @Input
    public abstract Property<String> getFile();

    /** Written as {@code <option name="key" value="value"/>} children of the component. */
    @Input
    public abstract MapProperty<String, String> getOptions();
}
