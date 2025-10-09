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

import java.io.Serializable;
import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

/**
 * Represents a language injection configuration for IntelliJ IDEA.
 */
public abstract class LanguageInjection implements Named, Serializable {
    private final String name;

    @Inject
    public LanguageInjection(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this language injection.
     */
    @Override
    @Internal
    public String getName() {
        return name;
    }

    /**
     * The language ID for the injection (e.g., "SQL", "HTML", "RegExp").
     */
    @Input
    public abstract Property<String> getLanguage();

    /**
     * The display name shown in IntelliJ (optional).
     */
    @Input
    @Optional
    public abstract Property<String> getDisplayName();

    /**
     * The PSI pattern that defines where the language should be injected. Example:
     * psiParameter().ofMethod(0,
     * psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.Executor"))
     */
    @Input
    public abstract Property<String> getPattern();
}
