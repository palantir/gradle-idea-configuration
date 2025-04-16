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

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class PluginDependency implements Serializable {
    private static final Pattern DOT_SPLITTER = Pattern.compile("\\.");

    private final String name;
    private String minVersion;

    public PluginDependency(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String minVersion() {
        return minVersion;
    }

    public void updateMinVersion(String candidateVersion) {
        if (minVersion == null || compareVersions(candidateVersion, minVersion) > 0) {
            this.minVersion = candidateVersion;
        }
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

    @Override
    public String toString() {
        return minVersion != null ? name + " (" + minVersion + ")" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginDependency that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
