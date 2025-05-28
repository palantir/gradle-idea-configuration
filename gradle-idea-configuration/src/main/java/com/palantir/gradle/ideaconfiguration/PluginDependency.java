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

import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

public abstract class PluginDependency implements Named {
    private static final Pattern DOT_SPLITTER = Pattern.compile("\\.");

    @Internal("Used to build up required min versions, not the final min version that is used")
    public abstract SetProperty<String> getMinRequiredVersions();

    @Input
    @Override
    public abstract String getName();

    @Optional
    @Input
    public final Provider<String> getMinVersion() {
        return getMinRequiredVersions().map(requiredMinVersions -> requiredMinVersions.stream()
                .max(PluginDependency::compareVersions)
                .orElse(null));
    }

    public final void atLeastVersion(String candidateVersion) {
        getMinRequiredVersions().add(candidateVersion);
    }

    public final void atLeastVersion(Property<String> candidateVersion) {
        getMinRequiredVersions().add(candidateVersion);
    }

    public static int compareVersions(String v1, String v2) {
        int[] parts1 =
                DOT_SPLITTER.splitAsStream(v1).mapToInt(Integer::parseInt).toArray();
        int[] parts2 =
                DOT_SPLITTER.splitAsStream(v2).mapToInt(Integer::parseInt).toArray();

        int length = Math.max(parts1.length, parts2.length);
        return IntStream.range(0, length)
                .map(i -> {
                    int num1 = i < parts1.length ? parts1[i] : 0;
                    int num2 = i < parts2.length ? parts2[i] : 0;
                    return Integer.compare(num1, num2);
                })
                .filter(comp -> comp != 0)
                .findFirst()
                .orElse(0);
    }
}
