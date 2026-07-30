#!/usr/bin/env python3
"""
Fabric Mod Project Scaffolding Script for MC 1.21.x

Generates a complete Fabric mod project directory structure with all
configuration files, entry points, and mixin configs.

Usage:
    python scaffold_project.py --mod-name "Magic Wands" --mod-id magicwands \
        --package com.example.magicwands --mc-version 1.21.1 --mod-version 1.0.0 \
        --author "AuthorName" --output /path/to/project
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path


# Version mapping for MC 1.21.x
VERSION_MAP = {
    "1.21.1": {
        "yarn": "1.21.1+build.3",
        "loader": "0.16.9",
        "fabric_api": "0.105.0+1.21.1",
    },
    "1.21": {
        "yarn": "1.21+build.9",
        "loader": "0.15.11",
        "fabric_api": "0.100.7+1.21",
    },
    "1.21.4": {
        "yarn": "1.21.4+build.8",
        "loader": "0.16.10",
        "fabric_api": "0.119.2+1.21.4",
    },
    "1.21.5": {
        "yarn": "1.21.5+build.2",
        "loader": "0.16.10",
        "fabric_api": "0.119.5+1.21.5",
    },
}


def validate_mod_id(mod_id: str) -> str:
    """Validate mod ID follows Fabric conventions."""
    if not re.match(r'^[a-z][a-z0-9_-]*$', mod_id):
        print(f"ERROR: Mod ID '{mod_id}' must be lowercase, start with a letter, "
              f"and contain only [a-z0-9_-]", file=sys.stderr)
        sys.exit(1)
    return mod_id


def validate_package(package: str) -> str:
    """Validate Java package name."""
    if not re.match(r'^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$', package):
        print(f"ERROR: Invalid Java package name '{package}'", file=sys.stderr)
        sys.exit(1)
    return package


def get_class_name(mod_name: str) -> str:
    """Convert mod name to PascalCase class name."""
    parts = re.split(r'[\s_-]+', mod_name)
    return ''.join(part.capitalize() for part in parts) + "Mod"


def write_file(path: Path, content: str):
    """Write content to a file, creating parent directories."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding='utf-8')
    print(f"  Created: {path}")


def generate_gradle_properties(args, versions):
    return f"""# Minecraft & Mappings
minecraft_version={args.mc_version}
yarn_mappings={versions['yarn']}

# Fabric
loader_version={versions['loader']}
fabric_version={versions['fabric_api']}

# Loom
loom_version=1.7-SNAPSHOT

# Mod
mod_version={args.mod_version}
maven_group={args.package}
archives_base_name={args.mod_id}

# Java
java_version=21
"""


def generate_build_gradle():
    return """plugins {
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
        rename { "${it}_${project.archivesBase_name}"}
    }
}
"""


def generate_settings_gradle():
    return """pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
    }
}
"""


def generate_fabric_mod_json(args, class_name):
    mod_json = {
        "schemaVersion": 1,
        "id": args.mod_id,
        "version": "${version}",
        "name": args.mod_name,
        "description": args.description or f"{args.mod_name} - A Fabric mod for MC 1.21.x",
        "authors": [args.author] if args.author else [],
        "contact": {},
        "license": args.license,
        "icon": f"assets/{args.mod_id}/icon.png",
        "environment": "*",
        "entrypoints": {
            "main": [f"{args.package}.{class_name}"],
            "client": [f"{args.package}.{class_name}Client"],
        },
        "mixins": [
            f"{args.mod_id}.mixins.json",
            {
                "config": f"{args.mod_id}.client.mixins.json",
                "environment": "client",
            },
        ],
        "depends": {
            "fabricloader": ">=0.16.0",
            "minecraft": "~1.21",
            "java": ">=21",
            "fabric-api": "*",
        },
        "suggests": {
            "modmenu": "*",
        },
    }
    return json.dumps(mod_json, indent=2)


def generate_main_class(args, class_name):
    return f"""package {args.package};

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class {class_name} implements ModInitializer {{
    public static final String MOD_ID = "{args.mod_id}";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {{
        LOGGER.info("[{args.mod_name}] Initializing!");
        // Register items, blocks, entities, etc. here
    }}
}}
"""


def generate_client_class(args, class_name):
    return f"""package {args.package};

import net.fabricmc.api.ClientModInitializer;

public class {class_name}Client implements ClientModInitializer {{
    @Override
    public void onInitializeClient() {{
        // Register client-side content: renderers, keybindings, screens, etc.
    }}
}}
"""


def generate_mixin_json(args, client=False):
    pkg = args.package + ".mixin"
    if client:
        pkg += ".client"
    mixin_json = {
        "required": True,
        "minVersion": "0.8",
        "package": pkg,
        "compatibilityLevel": "JAVA_21",
        "mixins": [],
        "injectors": {
            "defaultRequire": 1,
        },
    }
    if client:
        mixin_json["client"] = []
    return json.dumps(mixin_json, indent=2)


def generate_lang_json(args):
    return json.dumps({
        "itemgroup." + args.mod_id: args.mod_name,
    }, indent=2)


def generate_gitignore():
    return """# Gradle
.gradle/
build/
out/
classes/

# Eclipse
*.launch
.settings/
.metadata/
.classpath
.project

# IntelliJ IDEA
.idea/
*.iml
*.ipr
*.iws

# macOS
.DS_Store

# Fabric
run/
"""


def main():
    parser = argparse.ArgumentParser(
        description="Scaffold a Fabric mod project for MC 1.21.x"
    )
    parser.add_argument("--mod-name", required=True, help='Human-readable mod name (e.g. "Magic Wands")')
    parser.add_argument("--mod-id", required=True, help="Mod ID (lowercase, [a-z0-9_-])")
    parser.add_argument("--package", required=True, help="Java package (e.g. com.example.modid)")
    parser.add_argument("--mc-version", default="1.21.1", help="Minecraft version (default: 1.21.1)")
    parser.add_argument("--mod-version", default="1.0.0", help="Mod version (default: 1.0.0)")
    parser.add_argument("--author", default="", help="Author name")
    parser.add_argument("--description", default="", help="Mod description")
    parser.add_argument("--license", default="MIT", help="License (default: MIT)")
    parser.add_argument("--output", required=True, help="Output directory path")

    args = parser.parse_args()

    # Validate
    validate_mod_id(args.mod_id)
    validate_package(args.package)

    if args.mc_version not in VERSION_MAP:
        print(f"WARNING: MC version '{args.mc_version}' not in known mapping. "
              f"Using 1.21.1 defaults. Known: {list(VERSION_MAP.keys())}", file=sys.stderr)
        versions = VERSION_MAP["1.21.1"]
    else:
        versions = VERSION_MAP[args.mc_version]

    class_name = get_class_name(args.mod_name)
    base = Path(args.output)
    pkg_path = args.package.replace('.', '/')

    print(f"\nScaffolding Fabric mod project:")
    print(f"  Mod Name:    {args.mod_name}")
    print(f"  Mod ID:      {args.mod_id}")
    print(f"  Package:     {args.package}")
    print(f"  MC Version:  {args.mc_version}")
    print(f"  Main Class:  {class_name}")
    print(f"  Output:      {base}\n")

    # Root config files
    write_file(base / "build.gradle", generate_build_gradle())
    write_file(base / "settings.gradle", generate_settings_gradle())
    write_file(base / "gradle.properties", generate_gradle_properties(args, versions))
    write_file(base / ".gitignore", generate_gitignore())

    # Main source set
    main_res = base / "src" / "main" / "resources"
    main_java = base / "src" / "main" / "java" / pkg_path

    write_file(main_java / f"{class_name}.java", generate_main_class(args, class_name))
    write_file(main_java / f"{class_name}Client.java", generate_client_class(args, class_name))
    write_file(main_res / "fabric.mod.json", generate_fabric_mod_json(args, class_name))
    write_file(main_res / f"{args.mod_id}.mixins.json", generate_mixin_json(args))
    write_file(main_res / "assets" / args.mod_id / "lang" / "en_us.json", generate_lang_json(args))

    # Client source set
    client_res = base / "src" / "client" / "resources"
    write_file(client_res / f"{args.mod_id}.client.mixins.json", generate_mixin_json(args, client=True))

    # Create empty resource directories
    for d in ["models/item", "models/block", "textures/item", "textures/block",
              "textures/gui", "blockstates", "particles", "sounds"]:
        (main_res / "assets" / args.mod_id / d).mkdir(parents=True, exist_ok=True)

    # Create mixin package directories
    (base / "src" / "main" / "java" / pkg_path / "mixin").mkdir(parents=True, exist_ok=True)
    (base / "src" / "client" / "java" / pkg_path / "mixin" / "client").mkdir(parents=True, exist_ok=True)

    # Create data directory for datapack-style resources
    for d in ["loot_tables/blocks", "recipes", "tags/items", "tags/blocks",
              "worldgen/configured_feature", "worldgen/placed_feature",
              "worldgen/biome"]:
        (main_res / "data" / args.mod_id / d).mkdir(parents=True, exist_ok=True)

    print(f"\nProject scaffolded successfully at {base}")
    print(f"\nNext steps:")
    print(f"  1. cd {base}")
    print(f"  2. ./gradlew genSources  (or gradlew.bat on Windows)")
    print(f"  3. ./gradlew runClient   (to test in dev environment)")
    print(f"  4. ./gradlew build       (to build the mod JAR)")


if __name__ == "__main__":
    main()
