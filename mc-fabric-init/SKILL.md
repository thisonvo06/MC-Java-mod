---
name: mc-fabric-init
description: "Minecraft Fabric mod project scaffolding for MC 1.21.x with Java. This skill should be used when creating a new Fabric mod project from scratch, setting up Gradle build files, fabric.mod.json metadata, mod entry points, mixin configuration, and the standard directory structure. Covers project initialization, build configuration, and development environment setup."
agent_created: true
---

# Minecraft Fabric Mod Project Scaffolding (MC 1.21.x / Java)

## Overview

This skill provides the complete workflow for scaffolding a new Minecraft Fabric mod project targeting MC 1.21.x with Java 21. It covers directory structure, Gradle configuration, mod metadata, entry points, and mixin setup.

## When to Use

- Creating a brand-new Fabric mod project for MC 1.21.x
- Setting up Gradle build configuration (build.gradle, settings.gradle, gradle.properties)
- Configuring fabric.mod.json metadata and entry points
- Setting up mixin configuration files
- Initializing the standard source directory layout

## Technical Stack

| Component | Value |
|-----------|-------|
| Minecraft | 1.21.x (1.21.1 stable reference) |
| Java | 21 |
| Fabric Loader | 0.16.x |
| Fabric API | 0.105.x+ |
| Yarn Mappings | 1.21.x+build.x |
| Fabric Loom | 1.7-SNAPSHOT |

Consult `references/fabric_versions.md` for the full version mapping table.

## Standard Project Directory Structure

```
my-mod/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/modid/
│   │   │       ├── ModNameMod.java          (ModInitializer)
│   │   │       └── ModNameModClient.java    (ClientModInitializer)
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       ├── modid.mixins.json
│   │       └── assets/
│   │           └── modid/
│   │               ├── lang/en_us.json
│   │               ├── models/
│   │               ├── textures/
│   │               └── blockstates/
│   └── client/
│       ├── java/
│       │   └── com/example/modid/client/
│       │       └── (client-only classes)
│       └── resources/
│           └── modid.client.mixins.json
```

Key principle: split `main` (common/server+client) and `client` (client-only) source sets. This prevents client-only classes from crashing dedicated servers.

## Scaffolding Workflow

### Step 1: Collect Project Parameters

Gather the following from the user before generating any files:

| Parameter | Example | Rules |
|-----------|---------|-------|
| Mod name | "Magic Wands" | Human-readable display name |
| Mod ID | `magicwands` | Lowercase, `[a-z0-9_-]` only, no spaces |
| Java package | `com.example.magicwands` | Reverse domain, must match mod ID grouping |
| MC version | `1.21.1` | Must be 1.21.x |
| Mod version | `1.0.0` | Semantic versioning |
| Author name | (user-provided) | For metadata |

### Step 2: Generate Project Files

Run the scaffolding script to auto-generate the complete project:

```bash
python scripts/scaffold_project.py \
  --mod-name "Magic Wands" \
  --mod-id magicwands \
  --package com.example.magicwands \
  --mc-version 1.21.1 \
  --mod-version 1.0.0 \
  --author "AuthorName" \
  --output /path/to/project
```

The script creates the full directory tree with all config files, entry points, and mixin configs. After generation, review the output and customize as needed.

If the script is unavailable or manual setup is preferred, create each file following the templates below.

### Step 3: Core Configuration Files

#### gradle.properties

```properties
# Minecraft & Mappings
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3

# Fabric
loader_version=0.16.9
fabric_version=0.105.0+1.21.1

# Loom
loom_version=1.7-SNAPSHOT

# Mod
mod_version=1.0.0
maven_group=com.example.magicwands
archives_base_name=magicwands

# Java
java_version=21
```

#### settings.gradle

```gradle
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
    }
}
```

#### build.gradle

```gradle
plugins {
    id 'fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    maven { url = 'https://maven.fabricmc.net/' }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = project.java_version as int
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.archivesBaseName}"}
    }
}
```

### Step 4: Mod Metadata (fabric.mod.json)

Place at `src/main/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "magicwands",
  "version": "${version}",
  "name": "Magic Wands",
  "description": "A mod that adds magical wands.",
  "authors": ["AuthorName"],
  "contact": {
    "homepage": "https://example.com",
    "sources": "https://github.com/example/magicwands"
  },
  "license": "MIT",
  "icon": "assets/magicwands/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.example.magicwands.MagicWandsMod"
    ],
    "client": [
      "com.example.magicwands.MagicWandsModClient"
    ]
  },
  "mixins": [
    "magicwands.mixins.json",
    {
      "config": "magicwands.client.mixins.json",
      "environment": "client"
    }
  ],
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21",
    "java": ">=21",
    "fabric-api": "*"
  },
  "suggests": {
    "modmenu": "*"
  }
}
```

Critical fields:
- `id`: must match mod ID, lowercase alphanumeric + `_`/`-`
- `entrypoints.main`: class implementing `ModInitializer`
- `entrypoints.client`: class implementing `ClientModInitializer`
- `depends.minecraft`: use `~1.21` to match any 1.21.x
- `depends.java`: must be `>=21` for MC 1.21.x

### Step 5: Main Mod Entry Point

`src/main/java/com/example/magicwands/MagicWandsMod.java`:

```java
package com.example.magicwands;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicWandsMod implements ModInitializer {
    public static final String MOD_ID = "magicwands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Magic Wands mod is initializing!");
        // Register items, blocks, etc. here
    }
}
```

### Step 6: Client Entry Point

`src/main/java/com/example/magicwands/MagicWandsModClient.java`:

```java
package com.example.magicwands;

import net.fabricmc.api.ClientModInitializer;

public class MagicWandsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register client-side things: renderers, keybindings, screens, etc.
    }
}
```

### Step 7: Mixin Configuration

`src/main/resources/magicwands.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.magicwands.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

`src/client/resources/magicwands.client.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.magicwands.mixin.client",
  "compatibilityLevel": "JAVA_21",
  "mixins": [],
  "client": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### Step 8: Initial Language File

`src/main/resources/assets/magicwands/lang/en_us.json`:

```json
{
  "itemgroup.magicwands": "Magic Wands",
  "item.magicwands.fire_wand": "Fire Wand"
}
```

### Step 9: Build and Verify

```bash
# Generate sources and download dependencies
./gradlew genSources

# Build the mod
./gradlew build

# Run in dev environment (client)
./gradlew runClient

# Run in dev environment (dedicated server)
./gradlew runServer
```

## Cross-References

- For registering items after project setup, use the `mc-fabric-item` skill
- For blocks, use `mc-fabric-block`
- For entities, use `mc-fabric-entity`
- For recipes, use `mc-fabric-recipe`
- For GUIs, use `mc-fabric-gui`
- For world generation, use `mc-fabric-worldgen`
- For localization, models, and data generation, use `mc-fabric-assets`

## Resources

### scripts/
- `scaffold_project.py` - Automated project generator. Accepts mod name, ID, package, MC version, and output path. Generates the complete directory tree with all config files and entry points.

### references/
- `fabric_versions.md` - Version mapping table for MC 1.21.x (loader, API, yarn, loom versions)
- `project_structure.md` - Detailed explanation of each directory and file's purpose
