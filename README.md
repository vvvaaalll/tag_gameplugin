# Compiling TagPlugin

This guide explains how to compile **TagPlugin** from source.

## Requirements

Before compiling, make sure you have:

* Java Development Kit (JDK) 8
* Apache Maven 3.x
* CraftBukkit 1060 / Project Poseidon development library

TagPlugin was designed for:

```
Minecraft Beta 1.7.3
CraftBukkit 1060
Java 8
```

---

## 1. Install Java 8

Check your Java version:

```bash
java -version
```

The output should show Java 8:

```
java version "1.8.x"
```

---

## 2. Install Maven

Check Maven:

```bash
mvn -version
```

Example:

```
Apache Maven 3.x
Java version: 1.8.x
```

---

## 3. Add the CraftBukkit 1060 library

TagPlugin requires the old Bukkit API from CraftBukkit 1060.

Place the CraftBukkit jar in the project libraries folder:

```
TagPlugin/
 ├── libs/
 │    └── craftbukkit.jar
 ├── src/
 ├── pom.xml
 └── ...
```

The dependency is configured in `pom.xml`.

Because CraftBukkit 1060 is not available from modern Maven repositories, it must be provided manually.

---

## 4. Compile the plugin

Open a terminal in the project folder:

```bash
cd TagPlugin
```

Run:

```bash
mvn clean package
```

If compilation succeeds, Maven will create:

```
target/
 └── TagPlugin-1.5.jar
```

This jar can be installed on a Beta 1.7.3 server running CraftBukkit 1060 or Project Poseidon.

---

## 5. Installing the plugin

Copy the generated jar:

```
target/TagPlugin-1.5.jar
```

into:

```
plugins/
```

Start your server.

The console should display:

```
[TagPlugin] v1.5 active!
```

---

## Troubleshooting

### "Could not find artifact org.bukkit:bukkit"

The Bukkit dependency is not available through Maven.

Make sure the CraftBukkit 1060 library is correctly installed in:

```
libs/
```

and that `pom.xml` points to the correct file.

---

### "cannot find symbol"

This usually means:

* a missing source file;
* a wrong Bukkit/CraftBukkit version;
* incomplete project files.

Make sure the whole `src/main/java` folder is present.

---

## Development notes

TagPlugin uses old Bukkit APIs because it targets Minecraft Beta 1.7.3.

Modern Bukkit/Spigot/Paper versions are not supported.

Supported server software:

* CraftBukkit 1060
* Project Poseidon

Supported Java version:

* Java 8

## Info

TagPlugin was originally developed in French, so the default language of the plugin is French.

All player messages are configurable and can be modified through the language files located in:

```
plugins/TagPlugin/lang/
```

The plugin includes language support files that can be edited or translated to create your own language version.
