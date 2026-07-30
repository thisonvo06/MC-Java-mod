# Fabric Mod Project Structure Reference

## Complete Directory Layout

```
my-mod/
├── build.gradle                    # Gradle build config with Fabric Loom plugin
├── settings.gradle                 # Gradle plugin repositories
├── gradle.properties               # Version numbers and mod metadata
├── gradle/wrapper/                 # Gradle wrapper files
│   └── gradle-wrapper.properties
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── LICENSE                         # License file
├── README.md
│
├── src/
│   ├── main/                       # Common source set (server + client)
│   │   ├── java/
│   │   │   └── com/example/modid/
│   │   │       ├── ModNameMod.java           # ModInitializer entry point
│   │   │       ├── ModNameModClient.java     # ClientModInitializer entry point
│   │   │       ├── registry/                 # Registration helper classes
│   │   │       │   ├── ModItems.java
│   │   │       │   ├── ModBlocks.java
│   │   │       │   └── ModEntities.java
│   │   │       ├── item/                     # Custom item classes
│   │   │       ├── block/                    # Custom block classes
│   │   │       ├── entity/                   # Custom entity classes
│   │   │       ├── screen/                   # GUI screen handlers
│   │   │       ├── mixin/                    # Server/common mixins
│   │   │       └── util/                     # Utility classes
│   │   └── resources/
│   │       ├── fabric.mod.json              # Mod metadata (REQUIRED)
│   │       ├── modid.mixins.json            # Common mixin config
│   │       └── assets/
│   │           └── modid/
│   │               ├── lang/                 # Language files
│   │               │   ├── en_us.json
│   │               │   └── zh_cn.json
│   │               ├── models/               # Model JSON files
│   │               │   ├── item/
│   │               │   └── block/
│   │               ├── textures/             # PNG texture files
│   │               │   ├── item/
│   │               │   ├── block/
│   │               │   ├── entity/
│   │               │   ├── gui/
│   │               │   └── particle/
│   │               ├── blockstates/          # Block state JSON files
│   │               ├── particles/            # Particle definitions
│   │               ├── sounds/               # Sound files + sounds.json
│   │               └── icon.png              # Mod icon
│   │
│   └── client/                     # Client-only source set
│       ├── java/
│       │   └── com/example/modid/
│       │       ├── client/
│       │       │   ├── renderer/             # Entity/block renderers
│       │       │   ├── screen/               # Client GUI screens
│       │       │   ├── model/                # Custom models
│       │       │   └── keybind/              # Key bindings
│       │       └── mixin/client/            # Client-only mixins
│       └── resources/
│           └── modid.client.mixins.json     # Client mixin config
│
└── run/                            # Dev runtime directory (generated)
    ├── mods/                       # Dev mod jars
    └── config/                     # Config files
```

## Data Directory (Datapack Resources)

```
src/main/resources/data/
├── modid/
│   ├── loot_tables/
│   │   └── blocks/                 # Block drop tables
│   ├── recipes/                    # Crafting/smelting recipes
│   ├── advancements/               # Custom advancements
│   ├── tags/
│   │   ├── items/                  # Item tags
│   │   ├── blocks/                 # Block tags
│   │   └── entity_types/           # Entity type tags
│   ├── worldgen/
│   │   ├── configured_feature/     # Feature configs (ore veins, trees)
│   │   ├── placed_feature/         # Placed feature configs
│   │   ├── biome/                  # Custom biomes
│   │   └── structure/              # Custom structures
│   └── predicates/                 # Loot condition predicates
└── minecraft/                      # Override/extend vanilla data
    └── tags/
        └── ...
```

## File Purpose Reference

### fabric.mod.json
The mod's identity card. Fabric Loader reads this to register the mod, its entry points, dependencies, and mixins. Without this file, the mod will not load.

### Entry Points
- **main** (`ModInitializer`): Runs on both client and server. Register all common content (items, blocks, entities, recipes) here.
- **client** (`ClientModInitializer`): Runs only on the client. Register renderers, keybindings, client GUIs, and client-side event handlers here.
- **server** (`DedicatedServerModInitializer`): Runs only on dedicated servers. Rarely needed.

### Source Set Split
Fabric Loom creates two source sets by default:
- `main`: Compiled into the mod JAR for all environments
- `client`: Compiled only for client environments

Put client-only code (renderers, screen classes, client mixins) in `src/client/`. This prevents `ClassNotFoundException` crashes on dedicated servers.

### Mixin Configs
- `modid.mixins.json`: Applied in all environments (server + client)
- `modid.client.mixins.json`: Applied only on the client (set `"environment": "client"` in fabric.mod.json)
- `compatibilityLevel`: Must be `"JAVA_21"` for MC 1.21.x
- `package`: Points to the Java package containing mixin classes

## Gradle Commands Reference

| Command | Purpose |
|---------|---------|
| `./gradlew genSources` | Download Minecraft sources for debugging |
| `./gradlew build` | Compile and package the mod JAR |
| `./gradlew runClient` | Launch Minecraft client with the mod |
| `./gradlew runServer` | Launch dedicated server with the mod |
| `./gradlew runDatagen` | Run data generation (if configured) |
| `./gradlew clean` | Clean build outputs |
| `./gradlew --refresh-dependencies` | Force refresh all dependencies |
