<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/gradle-idea-configuration"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# gradle-idea-configuration

This gradle plugin allows for the configuration of Intellij IDEA xml files.

To apply the plugin:

```gradle
apply plugin: 'com.palantir.idea-configuration'
```

## Configuration

### External Dependencies

```gradle
ideaConfiguration {
    externalDependencies {
        'CheckStyle-IDEA' {
            atLeastVersion '9.2.1'
        }
        'palantir-java-format' {
            atLeastVersion '2.57.0'
        }
    }
}
```

### Language Injections

Configure IntelliJ language injections for your code. This is useful when you have methods that accept string parameters containing code in other languages (SQL, HTML, RegExp, etc.).

```gradle
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
```

The plugin will automatically update `.idea/IntelliLang.xml` with your configured injections.