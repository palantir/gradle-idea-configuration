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

    private static void assertXmlEquals(String expected, String actual) {
        assert expected.trim() == actual.trim()
    }

    def 'nothing happens if no language injections defined'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                // no languageInjections defined
            }
        '''.stripIndent(true)

        when: 'we run the first time'
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

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
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'we generate the correct config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="SQL" injector-id="java">
                  <display-name>SqlExecutor.execute (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, intelliLangFile.text)
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
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'we generate the correct config'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="HTML" injector-id="java">
                  <display-name>HtmlRenderer.render (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("render").withParameters("java.lang.String").definedInClass("com.example.HtmlRenderer"))]]></place>
                </injection>
                <injection language="SQL" injector-id="java">
                  <display-name>SqlExecutor.execute (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, intelliLangFile.text)
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
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'we generate the correct config with both injections'
        def newIntelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        newIntelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="RegExp" injector-id="java">
                  <display-name>Existing.pattern (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("pattern").withParameters("java.lang.String").definedInClass("com.example.Existing"))]]></place>
                </injection>
                <injection language="SQL" injector-id="java">
                  <display-name>SqlExecutor.execute (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String").definedInClass("com.example.SqlExecutor"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, newIntelliLangFile.text)
    }

    def 'merges multiple patterns with same language and display name into single injection'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-query-1' {
                        language = 'SQL'
                        displayName = 'DatabaseLibrary (com.example.db)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("executeQuery").withParameters("java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))'
                    }
                    'sql-query-2' {
                        language = 'SQL'
                        displayName = 'DatabaseLibrary (com.example.db)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("executeUpdate").withParameters("java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))'
                    }
                    'sql-query-3' {
                        language = 'SQL'
                        displayName = 'DatabaseLibrary (com.example.db)'
                        pattern = 'psiParameter().ofMethod(1, psiMethod().withName("prepareStatement").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the task'
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'all patterns with same language and display name are merged into single injection with multiple place elements'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="SQL" injector-id="java">
                  <display-name>DatabaseLibrary (com.example.db)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("executeQuery").withParameters("java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("executeUpdate").withParameters("java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(1, psiMethod().withName("prepareStatement").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.db.DatabaseLibrary"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, intelliLangFile.text)
    }

    def 'merges new patterns with existing patterns that have same language and display name'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'sql-new-1' {
                        language = 'SQL'
                        displayName = 'SqlLibrary (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("newMethod1").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))'
                    }
                    'sql-new-2' {
                        language = 'SQL'
                        displayName = 'SqlLibrary (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("newMethod2").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))'
                    }
                }
            }
        '''.stripIndent(true)

        //language=xml
        def existing = '''
          <project version="4">
            <component name="LanguageInjectionConfiguration">
              <injection language="SQL" injector-id="java">
                <display-name>SqlLibrary (com.example)</display-name>
                <single-file value="false"/>
                <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("existingMethod1").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
                <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("existingMethod2").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
              </injection>
            </component>
          </project>
        '''.stripIndent(true).trim()

        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.parentFile.mkdirs()
        intelliLangFile.text = existing

        when: 'we run the task'
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'new patterns are merged with existing patterns into single injection'
        def newIntelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        newIntelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="SQL" injector-id="java">
                  <display-name>SqlLibrary (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("existingMethod1").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("existingMethod2").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("newMethod1").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("newMethod2").withParameters("java.lang.String").definedInClass("com.example.SqlLibrary"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, newIntelliLangFile.text)
    }

    def 'complex merge with multiple languages and display names'() {
        //language=gradle
        buildFile << '''
            ideaConfiguration {
                languageInjections {
                    'xml-case-1' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("processXml").withParameters("java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-2' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("queryWithArray").withParameters("java.lang.String", "int[]").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-3' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("queryWithList").withParameters("java.lang.String", "java.util.List").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-4' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("queryWithPrimitives").withParameters("java.lang.String", "int", "long", "boolean", "double").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-5' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(1, psiMethod().withName("EdgeCaseLibrary").withParameters("int", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-6' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(1, psiMethod().withName("EdgeCaseLibrary").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'xml-case-7' {
                        language = 'XML'
                        displayName = 'EdgeCaseLibrary (com.example.edgecases)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("EdgeCaseLibrary").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))'
                    }
                    'sql-separate' {
                        language = 'SQL'
                        displayName = 'AnotherLibrary (com.example)'
                        pattern = 'psiParameter().ofMethod(0, psiMethod().withName("execute").definedInClass("com.example.AnotherLibrary"))'
                    }
                }
            }
        '''.stripIndent(true)

        when: 'we run the task'
        runTasksSuccessfully('-Didea.active=true', '-Didea.sync.active=true')

        then: 'complex patterns are properly merged by language and display name'
        def intelliLangFile = new File(projectDir, '.idea/IntelliLang.xml')
        intelliLangFile.exists()

        //language=xml
        def expected = '''
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="SQL" injector-id="java">
                  <display-name>AnotherLibrary (com.example)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("execute").definedInClass("com.example.AnotherLibrary"))]]></place>
                </injection>
                <injection language="XML" injector-id="java">
                  <display-name>EdgeCaseLibrary (com.example.edgecases)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("EdgeCaseLibrary").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("processXml").withParameters("java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryWithArray").withParameters("java.lang.String", "int[]").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryWithList").withParameters("java.lang.String", "java.util.List").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryWithPrimitives").withParameters("java.lang.String", "int", "long", "boolean", "double").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(1, psiMethod().withName("EdgeCaseLibrary").withParameters("int", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(1, psiMethod().withName("EdgeCaseLibrary").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.edgecases.EdgeCaseLibrary"))]]></place>
                </injection>
              </component>
            </project>
        '''.stripIndent(true).trim()

        assertXmlEquals(expected, intelliLangFile.text)
    }
}
