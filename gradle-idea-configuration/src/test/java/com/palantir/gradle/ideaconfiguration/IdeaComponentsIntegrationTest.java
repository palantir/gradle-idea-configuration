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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class IdeaComponentsIntegrationTest {

    @BeforeEach
    void setup(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("com.palantir.idea-configuration");
    }

    @Test
    void writes_component_options_to_a_new_file(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'JavacSettings' {
                        file = 'compiler.xml'
                        options.put('ADDITIONAL_OPTIONS_STRING', '-parameters')
                        options.put('PREFER_TARGET_JDK_COMPILER', 'false')
                    }
                }
            }
            """);

        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        ProjectFile<?> compilerXml = rootProject.directory(".idea").file("compiler.xml");
        assertThat(compilerXml.text().trim()).isEqualTo("""
            <project version="4">
              <component name="JavacSettings">
                <option name="ADDITIONAL_OPTIONS_STRING" value="-parameters"/>
                <option name="PREFER_TARGET_JDK_COMPILER" value="false"/>
              </component>
            </project>
            """.trim());
    }

    @Test
    void options_from_repeated_component_declarations_are_merged(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'JavacSettings' {
                        file = 'compiler.xml'
                        options.put('ADDITIONAL_OPTIONS_STRING', '-Xlint')
                    }
                    'JavacSettings' {
                        options.put('ADDITIONAL_OPTIONS_STRING', '-parameters')
                        options.put('PREFER_TARGET_JDK_COMPILER', 'false')
                    }
                }
            }
            """);

        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        ProjectFile<?> compilerXml = rootProject.directory(".idea").file("compiler.xml");
        assertThat(compilerXml.text().trim()).isEqualTo("""
            <project version="4">
              <component name="JavacSettings">
                <option name="ADDITIONAL_OPTIONS_STRING" value="-parameters"/>
                <option name="PREFER_TARGET_JDK_COMPILER" value="false"/>
              </component>
            </project>
            """.trim());
    }

    @Test
    void nothing_happens_if_no_idea_active(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'JavacSettings' {
                        file = 'compiler.xml'
                        options.put('ADDITIONAL_OPTIONS_STRING', '-parameters')
                    }
                }
            }
            """);

        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat().task(":updateIdeaComponentsXml").notOnTaskGraph();
        rootProject.directory(".idea").file("compiler.xml").assertThat().doesNotExist();
    }

    @Test
    void skipped_if_no_components_are_defined(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        result.assertThat().task(":updateIdeaComponentsXml").skipped();
    }

    @Test
    void fails_naming_the_component_if_its_file_is_not_set(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'JavacSettings' {
                        options.put('ADDITIONAL_OPTIONS_STRING', '-parameters')
                    }
                }
            }
            """);

        InvocationResult result = gradle.withArgs("-Didea.active=true").buildsWithFailure();

        assertThat(result.output())
                .contains("IntelliJ component 'JavacSettings' must set the file it lives in under .idea/");
    }

    @Test
    void merges_into_an_existing_file(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'JavacSettings' {
                        file = 'compiler.xml'
                        options.put('ADDITIONAL_OPTIONS_STRING', '-parameters')
                        options.put('PREFER_TARGET_JDK_COMPILER', 'false')
                    }
                }
            }
            """);
        ProjectFile<?> compilerXml = rootProject.directory(".idea").file("compiler.xml");
        compilerXml.overwrite("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project version="4">
              <component name="CompilerConfiguration">
                <annotationProcessing>
                  <profile name="Gradle Imported" enabled="true">
                    <outputRelativeToContentRoot value="true" />
                  </profile>
                </annotationProcessing>
              </component>
              <component name="JavacSettings">
                <option name="ADDITIONAL_OPTIONS_STRING" value="-Xlint" />
                <option name="GENERATE_NO_WARNINGS" value="true" />
              </component>
            </project>
            """);

        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        assertThat(compilerXml.text().trim()).isEqualTo("""
            <project version="4">
              <component name="CompilerConfiguration">
                <annotationProcessing>
                  <profile name="Gradle Imported" enabled="true">
                    <outputRelativeToContentRoot value="true"/>
                  </profile>
                </annotationProcessing>
              </component>
              <component name="JavacSettings">
                <option name="ADDITIONAL_OPTIONS_STRING" value="-parameters"/>
                <option name="GENERATE_NO_WARNINGS" value="true"/>
                <option name="PREFER_TARGET_JDK_COMPILER" value="false"/>
              </component>
            </project>
            """.trim());
    }

    @Test
    void writes_each_component_to_its_file(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            ideaConfiguration {
                components {
                    'FirstComponent' {
                        file = 'shared.xml'
                        options.put('first', '1')
                    }
                    'SecondComponent' {
                        file = 'shared.xml'
                        options.put('second', '2')
                    }
                    'OtherComponent' {
                        file = 'other.xml'
                        options.put('other', '3')
                    }
                }
            }
            """);

        gradle.withArgs("-Didea.active=true").buildsSuccessfully();

        assertThat(rootProject.directory(".idea").file("shared.xml").text().trim())
                .isEqualTo("""
                    <project version="4">
                      <component name="FirstComponent">
                        <option name="first" value="1"/>
                      </component>
                      <component name="SecondComponent">
                        <option name="second" value="2"/>
                      </component>
                    </project>
                    """.trim());
        assertThat(rootProject.directory(".idea").file("other.xml").text().trim())
                .isEqualTo("""
                    <project version="4">
                      <component name="OtherComponent">
                        <option name="other" value="3"/>
                      </component>
                    </project>
                    """.trim());
    }

    @Test
    void option_values_can_lazily_resolve_root_project_configurations(GradleInvoker gradle, RootProject rootProject) {
        rootProject.directory("libs").file("processor.jar").overwrite("");
        rootProject.buildGradle().append("""
            gradle.projectsEvaluated {
                def processorPath = configurations.detachedConfiguration(dependencies.create(files('libs/processor.jar')))
                ideaConfiguration.components {
                    'JavacSettings' {
                        file = 'compiler.xml'
                        options.put('ADDITIONAL_OPTIONS_STRING', provider { '-processorpath ' + processorPath.singleFile.name })
                    }
                }
            }
            """);

        InvocationResult result =
                gradle.withArgs("-Didea.active=true", "--warning-mode=all").buildsSuccessfully();

        assertThat(result.output())
                .doesNotContain("without an exclusive lock")
                .doesNotContain("context different than the project context");
        assertThat(rootProject.directory(".idea").file("compiler.xml").text())
                .contains("<option name=\"ADDITIONAL_OPTIONS_STRING\" value=\"-processorpath processor.jar\"/>");
    }
}
