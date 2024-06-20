<h1 align="center">Omega Config Architectury Ω </h1>
<p align="center">A configuration library by <a href="https://github.com/Draylar">Draylar</a> and <a href="https://github.com/frqnny">Frqnny</a></p>

---

ΩConfig is a hyper-minimal config library based on [Auto Config](https://github.com/shedaniel/AutoConfig). It aims to
achieve the following goals:

- Be lightweight (<25 KB) for JIJ usage
- Exceedingly simple design & API for developers
- Intuition and usability for players
- Bonus annotations for advanced config options (syncing values)

This is a fork of the original <a href="https://github.com/Draylar/omega-config">Omega Config</a>, built for use in architectury mods by Frqnny. However, it is available for anyone. 
If you have any questions or issues, please feel free to reach out to my discord: https://discord.gg/uvqTeQzQXK

The following is an example of a simple ΩConfig setup:

```java
public class TestConfig implements Config {

    @Comment(value = "Hello!")
    boolean value = false;

    @Syncing
    @Comment(value = "This value will sync to the client!")
    boolean syncableValue = false;

    @Override
    public String getFileName() {
        return "test-config";
    }
}

```

```java
public class MyModInitializer {

    public static final TestConfig CONFIG = OmegaConfig.register(TestConfig.class);

    @Override
    public void onInitialize() {
        System.out.printf("Config value: %s%n", CONFIG.value);
    }
}
```

---

### Pulling Omega Config into Development

To use Omega Config, you will have to add it to your build.gradle file.

What you pull in depends on whether you want GUI functionality. For basic config files using the base module (~20KB),
you can use the following gradle declarations:

```groovy
repositories {
    maven { url 'https://maven.draylar.dev/releases' }
}

// 1.20.6 : 1.5.0
dependencies {
    modImplementation include("io.draylar.omega-config:omega-config-base:${project.omega_config_version}")
}
```

Easy - you now have a bundled configuration library. Use the examples in the first section to implement your config.

---

### Extra API Utilities

ΩConfig provides several utility methods for developers.

**save()** - *saves a modified configuration instance to disk*

```java
MyModInitializer.CONFIG.value = false;
MyModInitializer.CONFIG.save(); // writes the new value to disk
```


`@Syncing` - *configuration options marked with this annotation will automatically sync to the client when they join a server.*

---

### License

ΩConfig is available under MIT. Omega Config will bundle the MIT license inside the jar you pull as a dependency, which means you can distribute it as a bundled dependency without any additional steps.