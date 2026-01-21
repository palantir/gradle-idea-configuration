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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
@DisabledConfigurationCache
class IdeaConfigurationPluginIntegrationTest {

    @BeforeEach
    void setup(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("com.palantir.idea-configuration");
        rootProject.buildGradle().plugins().add("idea");
    }

    @Test
    void nothing_happens_if_no_external_dependency_is_defined(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                // no externalDependency defined
            }
            """);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we dont generate the config").doesNotExist();
    }

    @Test
    void nothing_happens_if_no_idea_active(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        // we run the first time
        gradle.withArgs().buildsSuccessfully();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we dont generate the config").doesNotExist();
    }

    @Test
    void plugin_creates_external_dependencies_xml_file_in_the_idea_folder(
            GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void higher_version_of_the_same_dependency_is_taken(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.2.0'
                    }
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.2.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void higher_version_of_the_same_dependency_is_taken_with_different_length_versions(
            GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0.1'
                    }
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0.1"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void merges_with_existing_external_dependencies_xml(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
             ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        String existing = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="existing" min-version="0.2.0"/>
              </component>
            </project>
            """.trim();

        rootProject.directory(".idea").file("externalDependencies.xml").overwrite(existing);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="existing" min-version="0.2.0"/>
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void keeps_existing_project_version(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
             ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        String existing = """
            <project version="3">
              <component name="ExternalDependencies"></component>
            </project>
            """.trim();

        rootProject.directory(".idea").file("externalDependencies.xml").overwrite(existing);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="3">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void merges_with_existing_external_dependencies_xml_higher_value_used_from_external_file(
            GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
             ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.1.0'
                    }
                }
            }
            """);

        String existing = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.2.0"/>
              </component>
            </project>
            """.trim();

        rootProject.directory(".idea").file("externalDependencies.xml").overwrite(existing);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.2.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void merges_with_existing_external_dependencies_xml_higher_value_used_from_build_file(
            GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
             ideaConfiguration {
                externalDependencies {
                    'test' {
                        atLeastVersion '0.2.0'
                    }
                }
            }
            """);

        String existing = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        rootProject.directory(".idea").file("externalDependencies.xml").overwrite(existing);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.2.0"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void merges_with_existing_external_dependencies_xml_does_not_override_version_if_no_version_provided(
            GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
             ideaConfiguration {
                externalDependencies {
                    'test' {}
                }
            }
            """);

        String existing = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        rootProject.directory(".idea").file("externalDependencies.xml").overwrite(existing);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test" min-version="0.1.0"/>
              </component>
            </project>
            """.trim();

        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }

    @Test
    void can_add_if_no_minimum_version_provided(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                externalDependencies {
                    'test' {}
                }
            }
            """);

        // we run the first time
        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        String expected = """
            <project version="4">
              <component name="ExternalDependencies">
                <plugin id="test"/>
              </component>
            </project>
            """.trim();

        ProjectFile<?> externalDepsFile = rootProject.directory(".idea").file("externalDependencies.xml");
        externalDepsFile.assertThat().as("we generate the correct config").exists();
        assertThat(externalDepsFile.text().trim()).isEqualTo(expected);
    }
}
