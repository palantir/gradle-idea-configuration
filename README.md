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
    externalDependency 'CheckStyle-IDEA'                // plugin-id
    externalDependency 'palantir-java-format', '2.57.0' // plugin-id, min-version
}
```