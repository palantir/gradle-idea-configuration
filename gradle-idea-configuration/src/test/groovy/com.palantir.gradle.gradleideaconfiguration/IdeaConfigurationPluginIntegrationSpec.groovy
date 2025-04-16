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
import nebula.test.IntegrationSpec
import org.gradle.api.GradleException


class IdeaConfigurationPluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        //language=gradle
        buildFile << """
            apply plugin: 'com.palantir.idea-configuration'
            apply plugin: 'idea'
        """.stripIndent(true)
    }

    def "nothing happens if no externalDependency is defined"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                // no externalDependency defined
            }
        """.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we dont generate the config'
        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        !externalDepsFile.exists()
    }

    def "nothing happens if no idea.active"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.1.0'
            }
        """.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully()

        then: 'we dont generate the config'
        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        !externalDepsFile.exists()
    }

    def "plugin creates externalDependencies.xml file in the .idea folder"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.1.0'
            }
        """.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        externalDepsFile.exists()

        //language=xml
        def expected = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.1.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        assert externalDepsFile.text.trim() == expected
    }

    def "higher version of the same dependency is taken"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.2.0'
                externalDependency 'test', '0.1.0'
            }
        """.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        externalDepsFile.exists()

        //language=xml
        def expected = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.2.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        assert externalDepsFile.text.trim() == expected
    }

    def "merges with existing externalDependencies.xml"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.1.0'
            }
        """.stripIndent(true)

        //language=xml
        def existing = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="existing" min-version="0.2.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        externalDepsFile.parentFile.mkdirs()
        externalDepsFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def newExternalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        newExternalDepsFile.exists()

        //language=xml
        def expected = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="existing" min-version="0.2.0"/>
              <plugin id="test" min-version="0.1.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        assert externalDepsFile.text.trim() == expected
    }

    def "merges with existing externalDependencies.xml higher value used from file"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.1.0'
            }
        """.stripIndent(true)

        //language=xml
        def existing = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.2.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        externalDepsFile.parentFile.mkdirs()
        externalDepsFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def newExternalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        newExternalDepsFile.exists()

        //language=xml
        def expected = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.2.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        assert externalDepsFile.text.trim() == expected
    }

    def "merges with existing externalDependencies.xml higher value used from build"() {
        //language=gradle
        buildFile << """
            ideaConfiguration {
                externalDependency 'test', '0.2.0'
            }
        """.stripIndent(true)

        //language=xml
        def existing = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.1.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        def externalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        externalDepsFile.parentFile.mkdirs()
        externalDepsFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def newExternalDepsFile = new File(projectDir, '.idea/externalDependencies.xml')
        newExternalDepsFile.exists()

        //language=xml
        def expected = """
          <project version="4">
            <component name="ExternalDependencies">
              <plugin id="test" min-version="0.2.0"/>
            </component>
          </project>
        """.stripIndent(true).trim()

        assert externalDepsFile.text.trim() == expected
    }

}