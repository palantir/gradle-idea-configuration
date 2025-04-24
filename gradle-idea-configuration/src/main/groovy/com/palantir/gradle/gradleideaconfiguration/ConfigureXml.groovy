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

package com.palantir.gradle.gradleideaconfiguration

class ConfigureXml {
    static void configureExternalDependencies(Node rootNode, Set dependencies) {
        dependencies.each { dependency ->
            if (dependency.minVersion()) {
                configureExternalDependencies(rootNode, dependency)
            } else {
                configureExternalDependencies(rootNode, dependency.name())
            }
        }
    }

    static void configureExternalDependencies(Node rootNode, PluginDependency dependency) {
        def externalDependencies = matchOrCreateChild(rootNode, 'component', [name: 'ExternalDependencies'])
        def pluginNode = matchChild(externalDependencies, 'plugin', [id: dependency.name()]).orElse(null)
        def minVersion = dependency.minVersion()
        if (pluginNode) {
            String existingVersion = pluginNode.'@min-version'
            if (existingVersion && PluginDependency.compareVersions(minVersion, existingVersion) < 0) {
                minVersion = existingVersion
            }
        }
        matchOrCreateChild(externalDependencies, 'plugin', [id: dependency.name()], ['min-version' : minVersion])
    }

    static void configureExternalDependencies(Node rootNode, String dependency) {
        def externalDependencies = matchOrCreateChild(rootNode, 'component', [name: 'ExternalDependencies'])
        matchOrCreateChild(externalDependencies, 'plugin', [id: dependency])
    }


    private static Node matchOrCreateChild(Node base, String name, Map attributes = [:], Map overrides = [:]) {
        matchChild(base, name, attributes).map {it -> {
            it.attributes().putAll(overrides)
            return it
        } }.orElseGet {
            base.appendNode(name, attributes + overrides)
        }

    }

    private static Optional<Node> matchChild(Node base, String name, Map attributes = [:]) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }

        return Optional.ofNullable(child) as Optional<Node>
    }
}
