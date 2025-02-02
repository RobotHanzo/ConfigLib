# ConfigLib

**A Minecraft library for saving, loading, updating, and commenting YAML configuration files.**

This library facilitates creating, saving, loading, updating, and commenting YAML configuration
files. It does so by automatically mapping instances of configuration classes to serializable maps
which are first transformed into YAML and then saved to some specified file.

For a step-by-step tutorial that shows most features of this library in action check out
the [Tutorial](https://github.com/Exlll/ConfigLib/wiki/Tutorial) page on the wiki!

## Features

* Automatic creation, saving, loading, and updating of configuration files
* Support for comments through annotations
* Support for all primitive types, their wrapper types, and strings
* Support for enums, records, and POJOs (+ inheritance!)
* Support for (nested) lists, sets, arrays, and maps
* Support for `BigInteger` and `BigDecimal`
* Support for `LocalDate`, `LocalTime`, `LocalDateTime`, and `Instant`
* Support for `UUID`, `File`, `Path`, `URL`, and `URI`
* Support for Bukkit's `ConfigurationSerializable` types (e.g. `ItemStack`)
* Option to exclude fields from being converted
* Option to format field and component names before conversion
* Option to customize null handling
* Option to customize serialization by providing your own serializers
* Option to add headers and footers to configuration files
* ...and a few more!

## Usage example

This section contains a short usage example to get you started. The whole range of features is
discussed in the following sections. Information on how to import this library is located at the end
of this documentation.

For a step-by-step tutorial with a more advanced example check out
the [Tutorial](https://github.com/Exlll/ConfigLib/wiki/Tutorial) page on the wiki.

If you want support for Bukkit classes like `ItemStack`, check out
the [Configuration properties](#configuration-properties) section.

```java
public final class Example {
    // * To create a configuration annotate a class with @Configuration and make sure that
    //   it has a no-args constructor.
    // * Now add fields to that class and assign them default values.
    // * That's it! Fields can be private; setters are not required.
    @Configuration
    public static class BaseConfiguration {
        private String host = "127.0.0.1";
        private int port = 1234;
        // The library supports lists, sets, and maps.
        private Set<String> blockedAddresses = Set.of("8.8.8.8");
        // Fields can be ignored by making them final, transient, static or by
        // annotating them with @Ignore.
        private final double ignoreMe = 3.14;
    }

    // This library supports records; no @Configuration annotation required
    public record User(
            String username,
            @Comment("Please choose a strong password.")
            String password
    ) {}

    // Subclassing of configurations and nesting of configurations in other configurations
    // is also supported. Subclasses don't need to be annotated again.
    public static final class UserConfiguration extends BaseConfiguration {
        // You can add comments with the @Comment annotation. Each string in the comment
        // array is written (as a comment) on a new line.
        @Comment({"The admin user has full access.", "Choose a proper password!"})
        User admin = new User("root", "toor"); // The User class is a record!
        List<User> blockedUsers = List.of(
                new User("user1", null), // null values are supported
                new User("user2", null)
        );
    }

    public static void main(String[] args) {
        var configFile = Paths.get("/tmp/config.yml");
        var config = new UserConfiguration();

        // Save a new instance to the configuration file
        YamlConfigurations.save(configFile, UserConfiguration.class, config);

        // Load a new instance from the configuration file
        config = YamlConfigurations.load(configFile, UserConfiguration.class);
        System.out.println(config.admin.username);
        System.out.println(config.blockedUsers);

        // Modify and save the configuration file
        config.blockedUsers.add(new User("user3", "pass3"));
        YamlConfigurations.save(configFile, UserConfiguration.class, config);
    }
}
```

By running the above code, a new YAML configuration is created at `/tmp/config.yml`. Its content
looks like this:

```yaml
host: 127.0.0.1
port: 1234
blockedAddresses:
  - 8.8.8.8
# The admin user has full access.
# Choose a proper password!
admin:
  username: root
  # Please choose a strong password.
  password: toor
blockedUsers:
  - username: user1
  - username: user2
  - username: user3
    password: pass3
```

Two things are noticeable here:

1. Not every user in the `blockedUsers` list has a `password` mapping. This is because null values
   are not output by default. That behavior can be changed by the builder.
2. The password of the user with username `user3` has no comment. This is due to limitations of
   the YAML library. Configurations in lists, sets, or maps cannot have their comments printed.

## General information

In the following sections the term _configuration type_ refers to any record type or to any
non-generic class that is directly or indirectly (i.e. through subclassing) annotated with
`@de.exlll.configlib.Configuration`. Accordingly, the term _configuration_ refers to an instance of
such a type.

### Declaring configuration types

To declare a configuration type, either define a record or annotate a class with `@Configuration`
and make sure that it has a no-args constructor. The no-args constructor can be set `private`. Inner
classes (i.e. the ones that are nested but not `static`) have an implicit synthetic constructor with
at least one argument and are therefore not supported.

Now simply add components to your record or fields to your class whose type is any of the supported
types listed in the next section. You can (and should) initialize all fields of a configuration
class with non-null default values.

### Supported types

A configuration type may only contain fields or components of the following types:

| Type class                  | Types                                                              |
|-----------------------------|--------------------------------------------------------------------|
| Boolean types               | `boolean`, and `Boolean`                                           |
| Integer types               | `byte`, `short`, `int`, `long`, and their respective wrapper types |
| Floating point types        | `float`, `double`, and their respective wrapper types              |
| Characters and strings      | `char`, `Character`, `String`                                      |
| Big numeric types           | `BigInteger`, `BigDecimal`                                         |
| Time related types          | `LocalTime`, `LocalDate`, `LocalDateTime`, `Instant`               |
| Utility types               | `UUID`, `File`, `Path`, `URL`, `URI`                               |
| Enums                       | Any Java enum                                                      |
| Configurations              | Any Java record or any class annotated with `@Configuration`       |
| `ConfigurationSerializable` | All Bukkit classes that implement this interface, like `ItemStack` |
| Collections                 | (Nested) Lists, sets, maps*, or arrays of previously listed types  |

(*) Map keys can only be of simple or enum type, i.e. they cannot be in the `Collections`,
`Configurations`, or `ConfigurationSerializable` type class.

#### Examples of supported types

The following class contains examples of types that this library supports:

```java
public final class SupportedTypes {
    boolean supported;
    Character supported;
    String supported;
    LocalTime supported;
    UUID supported;
    ExampleEnum supported;  // where 'ExampleEnum' is some Java enum type
    ExampleConfig supported;  // where 'ExampleConfig' is a class annotated with @Configuration
    ExampleRecord supported;  // where 'ExampleRecord' is a Java record

    /* collection types */
    List<BigInteger> supported;
    Set<Double> supported;
    LocalDate[] supported;
    Map<ExampleEnum, ExampleConfig> supported;

    /* nested collection types */
    List<Map<ExampleEnum, LocalDate>> supported;
    int[][] supported;
    Map<Integer, List<Map<Short, Set<ExampleRecord>>>> supported;

    // supported if a custom serializer is registered
    java.awt.Point supported;

    // supported when a special properties object is used (explained further below)
    org.bukkit.inventory.ItemStack supported;
}
```

<details>
 <summary>Examples of unsupported types</summary>

The following class contains examples of types that this library does (and will) not support:

```java
public final class UnsupportedTypes<T> {
    Map<Point, String> unsupported;        // invalid map key
    Map<List<String>, String> unsupported; // invalid map key
    Box<String> unsupported;               // custom parameterized type
    List<? extends String> unsupported;    // wildcard type
    List<?> unsupported;                   // wildcard type
    List<?>[] unsupported;                 // wildcard type
    T unsupported;                         // type variable
    List unsupported;                      // raw type
    List[] unsupported;                    // raw type
    List<String>[] unsupported;            // generic array type
    Set<Integer>[] unsupported;            // generic array type
    Map<Byte, Byte>[] unsupported;         // generic array type
}
```

</details>

### Loading and saving configurations

There are two ways to load and save configurations. Which way you choose depends on your liking.
Both ways have three methods in common:

* The `save` method saves a configuration to a file. The file is created if it does not exist and
  is overwritten otherwise.
* The `load` method creates a new configuration instance and populates it with values taken from a
  file. For classes, the no-args constructor is used to create a new instance. For records, the
  canonical constructor is called.
* The `update` method is a combination of `load` and `save` and the method you'd usually want to
  use: it takes care of creating the configuration file if it does not exist and otherwise updates
  it to reflect changes to (the fields or components of) the configuration type.

<details>
 <summary>Example of <code>update</code> behavior when configuration file exists</summary>

Let's say you have the following configuration type:

```java 
@Configuration 
public final class C {
    int i = 10; 
    int j = 11; 
}
```

... and a YAML configuration file that contains:

```yaml
i: 20
k: 30
```

Now, when you use one of the methods below to call `update` for that configuration type and file,
the configuration instance that `update` returns will have its `i` variable initialized to `20`
and its `j` variable will have its default of `11`. After the operation, the configuration file will
contain:

```yaml
i: 20
j: 11
```

</details>

To exemplify the usage of these three methods we assume for the following sections that you have
implemented the configuration type below and have access to some regular `java.nio.file.Path`
object `configurationFile`.

```java 
@Configuration
public final class Config { /* some fields */ }
```

#### Way 1

The first way is to create a configuration store and use it directly to save, load, or update
configurations.

```java 
YamlConfigurationProperties properties = YamlConfigurationProperties.newBuilder().build();
YamlConfigurationStore<Config> store = new YamlConfigurationStore<>(Config.class, properties);

Config config1 = store.load(configurationFile);
store.save(config1, configurationFile);
Config config2 = store.update(configurationFile);
```

#### Way 2

The second way is to use the static methods from the `YamlConfigurations` class.

```java 
Config config1 = YamlConfigurations.load(configurationFile, Config.class);
YamlConfigurations.save(configurationFile, Config.class, config1);
Config config2 = YamlConfigurations.update(configurationFile, Config.class);
```

Each of these methods has two additional overloads: One that takes a properties object and another
that lets you configure a properties object builder. For example, the overloads of the `load`
method are:

```java 
// overload 1
YamlConfigurationProperties properties = YamlConfigurationProperties.newBuilder().build();
Config c1 = YamlConfigurations.load(configurationFile, Config.class, properties);

// overload 2
Config c2 = YamlConfigurations.load(
    configurationFile,
    Config.class,
    builder -> builder.inputNulls(true).outputNulls(false)
); 
```

<hr>

All three methods can also be passed a Java record instead of a class. To provide default values
for records when calling the `update` method, you can add a constructor with no parameters that
initializes its components. This constructor is only called if the configuration file does not
exist.

```java 
record User(String name, String email) {
    User() { this("John Doe", "john@doe.com"); }
}
User user = YamlConfigurations.update(configurationFile, User.class);
```

### Configuration properties

Instances of the `ConfigurationProperties` class allow customization of how configurations are
stored and loaded. To create such an instance, instantiate a new builder using
the `YamlConfigurationProperties.newBuilder()` method, configure it, and finally call its `build()`
method. Alternatively, you can use the `toBuilder()` method of an
existing `YamlConfigurationProperties` to create a new builder that is initialized with values
takes from the properties object.

Check out the methods of the builder class to see which configuration options are available.

#### Support for Bukkit classes like `ItemStack`

There is a special `YamlConfigurationProperties` object with name `BUKKIT_DEFAULT_PROPERTIES`
that adds support for Bukkit's `ConfigurationSerializable` types. If you want to use any of these
types in your configuration, you have to use that object as a starting point:

```java 
YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
     // ...further configure the builder...
    .build();
```

To get access to this object, you have to import `configlib-paper` instead of `configlib-yaml` as
described in the [Import](#import) section.

### Comments

The fields or components of a configuration can be annotated with the `@Comment` annotation. This
annotation takes an array of strings. Each of these strings is written onto a new line as a comment.
Empty strings are written as newlines.

If a configuration type _C_ that defines comments is used (as a field or component) within another
configuration type, the comments of _C_ are written with the proper indentation. However, if
instances of _C_ are stored inside a collection, their comments are not printed when the collection
is written.

Serializing the following configuration as YAML ...

```java 
@Configuration
public final class ExampleConfiguration {
    @Comment({"Hello", "", " ", "World"})
    private String commentedField = "commented field";
}
```

... results in the YAML file shown below:

```yaml
 # Hello

 #  
 # World
 commentedField: commented field
```

Similarly, if you define the following record configuration and save it ...

```java 
record User(@Comment("The name") String name, @Comment("The address") Address address) {}
record Address(@Comment("The street") String street) {}

User user = new User("John Doe", new Address("10 Downing St"));
```

... you get:

```yaml
# The name
name: John Doe
# The address
address:
  # The street
  street: 10 Downing St
```

### Subclassing

Subclassing of configurations types is supported. Subclasses of configuration types don't need to be
annotated with `@Configuration`. When a configuration is written, the fields of parent classes
are written before the fields of the child in a top to bottom manner. Parent configurations can
be `abstract`.

#### Shadowing of fields

Shadowing of fields refers to the situation where a subclass of configuration has a field that has
the same name as a field in one of its super classes. Shadowing of fields is currently not
supported. (This restriction might easily be lifted. If you need this feature, please open an issue
and describe how to handle name clashes.)

### Ignoring and filtering fields

Fields that are `final`, `static`, `transient` or annotated with `@Ignore` are neither serialized
nor updated during deserialization. You can filter out additional fields by providing an instance of
`FieldFilter` to the configuration properties. Record components cannot be filtered.

### Handling of missing and `null` values

#### Missing values

When a configuration file is read, values that correspond to a field of a configuration type or to a
component of a record type might be missing.
That can happen, for example, when somebody deleted that field from the configuration file, when the
definition of a configuration or record type is changed, or when the `NameFormatter` that was used
to create that file is replaced.

In such cases, fields of configuration types keep the default value you assigned to them and record
components are initialized with the default value of their corresponding type.

#### Null values

**NOTE:** Null values written to a configuration file generally don't give any indication about
which kinds of values the configuration expects. Therefore, they not only make it harder for the
users of that configuration file to properly configure it, but they might also prevent loading a
configuration if the values the users set are of the wrong type.

Although strongly discouraged, null values are supported and `ConfigurationProperties` let you
configure how they are handled when serializing and deserializing a configuration:

* By setting `outputNulls` to false, class fields, record components, and collection elements that
  are null are not output. Any comments that belong to such fields are also not written.
* By setting `inputNulls` to false, null values read from the configuration file are treated as
  missing and are, therefore, handled as described in the section above.
* By setting `inputNulls` to true, null values read from the configuration file override the
  corresponding default values of a configuration type with null or set the component value of a
  record type to null. If the field or component type is primitive, an exception is thrown.

The following code forbids null values to be output but allows null values to be input. By default,
both are forbidden which makes the call to `outputNulls` in this case redundant.

```java 
YamlConfigurationProperties.newBuilder()
        .outputNulls(false)
        .inputNulls(true)
        .build();
```

### Field and component name formatting

You can define how fields and component names are formatted by configuring the configuration
properties with a custom formatter. Formatters are implementations of the `NameFormatter`
interface. You can implement this interface yourself or use one of the several formatters this
library provides. These pre-defined formatters can be found in the `NameFormatters` class.

The following code formats fields using the `IDENTITY` formatter (which is the default).

```java 
YamlConfigurationProperties.newBuilder()
        .setNameFormatter(NameFormatters.IDENTITY)
        .build();
```

### Type conversion and custom serializers

Before instances of the types listed in the [supported types](#supported-types) section can be
stored, they need to be converted into serializable types (i.e. into types the underlying YAML
library knows how to handle). The conversion happens according to the following table:

| Source type                 | Target type      |
|-----------------------------|------------------|
| Boolean types               | `Boolean`        |
| Integer types               | `Long`           |
| Floating point types        | `Double`         |
| Characters and strings      | `String`         |
| Big numeric types           | `String`         |
| Time related types          | `String`         |
| Utility types               | `String`         |
| Enums                       | `String`         |
| Configurations              | `Map<String, ?>` |
| `Set<S>`                    | `List<T>`*       |
| `List<S>`                   | `List<T>`        |
| `S[]`                       | `List<T>`        |
| `Map<S1, S2>`               | `Map<T1, T2>`    |
| `ConfigurationSerializable` | `String`         |

(*) By default, sets are serialized as lists. This can be changed through the configuration
properties. This also means that `Set`s are valid target types.

#### Serializer selection

To convert the value of a field or record component `F` with (source) type `S` into a serializable
value of some target type, a serializer has to be selected. Serializers are instances of
the `de.exlll.configlib.Serializer` interface and are selected based on `S`. Put differently,
serializers are always selected based on the compile-time type of `F` and never on the runtime type
of its value.

<details>
 <summary>Why should I care about this?</summary>

This distinction makes a difference (and might lead to confusion) when you have fields or record
components whose type is a configuration type, and you extend that configuration type. Concretely,
assume you have written two configuration classes `A` and `B` where `B extends A`. Then, if you
use `A a = new B()` in your main configuration, only the fields of a `A` will be stored when you
save your main configuration. That is because the serializer of field `a` was selected based on the
compile-time type of `a` which is `A` and not `B`. The same happens if you have a `List<A>` and put
instances of `B` (or some other subclass of `A`) in it.

</details>

#### Custom serializers

If you want to add support for a type that is not a record or whose class is not annotated
with `@Configuration`, you can register a custom serializer. Serializers are instances of
the `de.exlll.configlib.Serializer` interface. When implementing that interface you have to make
sure that you convert your source type into one of the valid target types listed in the table above.
The serializer then has to be registered through a `ConfigurationProperties` object.

The following `Serializer` serializes instances of `java.awt.Point` into strings and vice versa.

```java
public final class PointSerializer implements Serializer<Point, String> {
    @Override
    public String serialize(Point element) {
        return element.x + ":" + element.y;
    }

    @Override
    public Point deserialize(String element) {
        String[] parts = element.split(":");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        return new Point(x, y);
    }
}
```

Custom serializers takes precedence over the serializers provided by this library.

### Changing the type of fields or record components

Changing the type of fields or record components is not supported. If you change the type of one of
these but your configuration file still contains a value of the old type, a type mismatch will
occur when loading a configuration from that file. Instead, remove the old element and add a new one
with a different name.

### Recursive type definitions

Recursive type definitions are currently not allowed but might be supported in a future version if
this feature is requested.

<details>
 <summary>Examples of recursive type definitions</summary>

Neither direct nor indirect recursive type definitions are supported.

```java
public final class RecursiveTypDefinitions {
    // Direct recursive definition
    @Configuration
    static final class R {
        R r;
    }

    // Indirect recursive definition
    @Configuration
    static final class R1 {
        R2 r2;
    }

    @Configuration
    static final class R2 {
        R1 r1;
    }
}
```

</details>

## Project structure

This project contains three classes of modules:

* The `configlib-core` module contains most of the logic of this library. In it, you can find (among
  other things), the object mapper that converts configuration instances to maps (and vice versa),
  most serializers, and the classes responsible for the extraction of comments. It does not
  contain anything Minecraft related.
* The `configlib-yaml` module contains the classes that can save configuration instances as YAML
  files and instantiate new instances from such files. This module does not contain anything
  Minecraft related, either.
* The `configlib-paper`, `configlib-velocity`, and `configlib-waterfall` modules contain basic
  plugins that are used to conveniently load this library. These three modules shade the `-core`
  module, the `-yaml` module, and the YAML parser when the `shadowJar` task is executed. The shaded
  jar files are released on the [releases page](https://github.com/Exlll/ConfigLib/releases).
    * The `configlib-paper` module additionally contains the `ConfigLib.BUKKIT_DEFAULT_PROPERTIES`
      object which adds support for the serialization of Bukkit classes like `ItemStack` as
      described [here](#support-for-bukkit-classes-like-itemstack).

## Import

**INFO:** I'm currently looking for an easier way for you to import this library that does not
require authentication with GitHub. Please check
this [issue](https://github.com/Exlll/ConfigLib/issues/12) if you have authentication problems.

To use this library, import it into your project with either Maven or Gradle as shown in the two
examples below. This library has additional dependencies (namely, a YAML parser) which are not
exposed by the artifact you import.

This repository provides plugin versions of this library which bundle all its dependencies, so you
don't have to worry about them. Also, these versions make it easier for you to update this library
if you have written multiple plugins that use it.

The plugin versions can be downloaded from
the [releases page](https://github.com/Exlll/ConfigLib/releases) where you can identify them by
their `-paper-`, `-waterfall-`, and `-velocity-` infix and `-all` suffix. Except for the `-paper-`
version, the other two plugin versions currently don't add any additional functionality. If you use
these versions, don't forget to add them as a dependency in the `plugin.yml` (for Paper and
Waterfall) or to the dependencies array (for Velocity) of your own plugin.

Alternatively, if you don't want to use an extra plugin, you can shade the `-yaml` version with its
YAML parser yourself.

**NOTE:** If you want serialization support for Bukkit classes like `ItemStack`,
replace `configlib-yaml` with `configlib-paper`
(see [here](#support-for-bukkit-classes-like-itemstack)).

#### Maven

```xml 
<repository>
    <id>de.exlll</id>
    <url>https://maven.pkg.github.com/Exlll/ConfigLib</url>
</repository>

<dependency>
    <groupId>de.exlll</groupId>
    <artifactId>configlib-yaml</artifactId>
    <version>4.0.0</version>
</dependency>
```

#### Gradle

```groovy
repositories { maven { url 'https://maven.pkg.github.com/Exlll/ConfigLib' } }

dependencies { implementation 'de.exlll:configlib-yaml:4.0.0' }
```

```kotlin
repositories { maven { url = uri("https://maven.pkg.github.com/Exlll/ConfigLib") } }

dependencies { implementation("de.exlll:configlib-yaml:4.0.0") }
```

## Future work

This section contains ideas for upcoming features. If you want any of these to happen any time soon,
please [open an issue](https://github.com/Exlll/ConfigLib/issues/new) where we can discuss the
details.

- JSON, TOML, XML support
- Post-load/Pre-save hooks
- More features and control over updating/versioning
- More control over the ordering of fields, especially in parent/child class scenarios
- Recursive definitions
- Shadowing of fields
