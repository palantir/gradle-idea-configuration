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
package com.palantir.gradle.ideaconfiguration

import nebula.test.IntegrationSpec

class UpdateIntelliLangXmlIntegrationSpec extends IntegrationSpec {

    def setup() {
        //language=gradle
        buildFile << '''
            apply plugin: 'com.palantir.idea-configuration'
            apply plugin: 'idea'
        '''.stripIndent(true)
    }

    def 'nothing happens if no language injections defined'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                // no languageInjections defined
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we dont generate the config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        !intelliLangFile.exists()
    }

    def 'plugin creates IntelliLang.xml file in the .idea folder'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-executor' {
                        language = 'SQL'
                        displayName = 'SqlExecutor.execute (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        def xml = new XmlSlurper().parse(intelliLangFile)
        xml.@version == '4'
        xml.component.@name == 'LanguageInjectionConfiguration'

        def injection = xml.component.injection[0]
        injection.@language == 'SQL'
        injection.@'injector-id' == 'java'
        injection.'display-name'.text() == 'SqlExecutor.execute (com.example)'
        injection.'single-file'.@value == 'false'

        def pattern = injection.place.text()
        pattern.contains('psiParameter().ofMethod(0')
        pattern.contains('.withName("execute")')
        pattern.contains('.definedInClass("com.example.SqlExecutor")')
    }

    def 'handles multiple language injections'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-executor' {
                        language = 'SQL'
                        displayName = 'SqlExecutor.execute (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                    'html-renderer' {
                        language = 'HTML'
                        displayName = 'HtmlRenderer.render (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("render").withParameters("java.lang.String").definedInClass("com.example.HtmlRenderer"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        def xml = new XmlSlurper().parse(intelliLangFile)
        xml.component.injection.size() == 2

        def languages = xml.component.injection.@language*.text()
        languages.contains('SQL')
        languages.contains('HTML')

        def displayNames = xml.component.injection.'display-name'*.text()
        displayNames.contains('SqlExecutor.execute (com.example)')
        displayNames.contains('HtmlRenderer.render (com.example)')
    }

    def 'merges with existing IntelliLang.xml'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-executor' {
                        language = 'SQL'
                        displayName = 'SqlExecutor.execute (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                }
            }
        '''.stripIndent(true)

        //language=xml
        def existing = '''
          <project version="4">
            <component name="LanguageInjectionConfiguration">
              <injection language="RegExp" injector-id="java">
                <display-name>Existing.pattern (com.example)</display-name>
                <single-file value="false"/>
                <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("pattern").withParameters("java.lang.String").definedInClass("com.example.Existing"))]]></place>
              </injection>
            </component>
          </project>
        '''.stripIndent(true).trim()

        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.parentFile.mkdirs()
        intelliLangFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def newIntelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        newIntelliLangFile.exists()

        def xml = new XmlSlurper().parse(newIntelliLangFile)
        xml.component.injection.size() == 2

        def languages = xml.component.injection.@language*.text()
        languages.contains('SQL')
        languages.contains('RegExp')

        def displayNames = xml.component.injection.'display-name'*.text()
        displayNames.contains('SqlExecutor.execute (com.example)')
        displayNames.contains('Existing.pattern (com.example)')
    }

    def 'replaces injection with same display name'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-executor' {
                        language = 'SQL'
                        displayName = 'SqlExecutor.execute (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                }
            }
        '''.stripIndent(true)

        //language=xml
        def existing = '''
          <project version="4">
            <component name="LanguageInjectionConfiguration">
              <injection language="HTML" injector-id="java">
                <display-name>SqlExecutor.execute (com.example)</display-name>
                <single-file value="false"/>
                <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("old").withParameters("java.lang.String").definedInClass("com.example.Old"))]]></place>
              </injection>
            </component>
          </project>
        '''.stripIndent(true).trim()

        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.parentFile.mkdirs()
        intelliLangFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config with new injection replacing old'
        def newIntelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        newIntelliLangFile.exists()

        def xml = new XmlSlurper().parse(newIntelliLangFile)
        xml.component.injection.size() == 1

        def injection = xml.component.injection[0]
        injection.@language == 'SQL'
        injection.'display-name'.text() == 'SqlExecutor.execute (com.example)'
        injection.place.text().contains('.withName("execute")')
        !injection.place.text().contains('.withName("old")')
    }

    def 'uses name as display name if displayName not provided'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'my-sql-injection' {
                        language = 'SQL'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we generate the correct config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        def xml = new XmlSlurper().parse(intelliLangFile)
        xml.component.injection[0].'display-name'.text() == 'my-sql-injection'
    }

    def 'keeps existing project version'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-executor' {
                        language = 'SQL'
                        displayName = 'SqlExecutor.execute (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))'
                    }
                }
            }
        '''.stripIndent(true)

        //language=xml
        def existing = '''
          <project version="3">
            <component name="LanguageInjectionConfiguration"></component>
          </project>
        '''.stripIndent(true).trim()

        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.parentFile.mkdirs()
        intelliLangFile.text = existing

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'we keep the existing version'
        def newIntelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        newIntelliLangFile.exists()

        def xml = new XmlSlurper().parse(newIntelliLangFile)
        xml.@version == '3'
    }

    def 'injections are sorted by display name'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'z-last' {
                        language = 'SQL'
                        displayName = 'Z Last'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("z").definedInClass("com.example.Z"))'
                    }
                    'a-first' {
                        language = 'HTML'
                        displayName = 'A First'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("a").definedInClass("com.example.A"))'
                    }
                    'm-middle' {
                        language = 'RegExp'
                        displayName = 'M Middle'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("m").definedInClass("com.example.M"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true')

        then: 'injections are sorted alphabetically'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        def xml = new XmlSlurper().parse(intelliLangFile)

        def displayNames = xml.component.injection.'display-name'*.text()
        displayNames == ['A First', 'M Middle', 'Z Last']
    }
}
