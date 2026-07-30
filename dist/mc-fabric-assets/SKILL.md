---
name: mc-fabric-assets
description: "Minecraft Fabric asset management for MC 1.21.x with Java. This skill should be used when creating language files (en_us, zh_cn), model JSON files (items, blocks, blockstates), texture specifications, sound registration, and data generation (datagen) for auto-generating models, lang, recipes, loot tables, and worldgen JSON. Covers the complete resource pack and data pack file structure."
agent_created: true
---

# Minecraft Fabric Assets, Localization & Data Generation (MC 1.21.x / Java)

## Overview

This skill provides the workflow for managing mod assets: language files, model JSON, textures, sounds, and automated data generation. It covers the complete resource/data file structure and the Fabric datagen framework.

## When to Use

- Creating or updating language files (translations)
- Creating model JSON files (items, blocks, entities)
- Setting up texture files (naming, resolution, format)
- Registering custom sounds
- Setting up data generation to auto-create models, lang, recipes, loot tables
- Understanding the complete resource pack / data pack directory structure

## Asset Directory Structure

### Client Resources (assets/)

```
src/main/resources/assets/<modid>/
├── lang/
│   ├── en_us.json          # English (REQUIRED - fallback)
│   └── zh_cn.json          # Chinese Simplified
├── models/
│   ├── item/               # Item models
│   ├── block/              # Block models
│   └── entity/             # Entity models (JSON-based)
├── textures/
│   ├── item/               # Item textures (16x16)
│   ├── block/              # Block textures (16x16)
│   ├── entity/             # Entity textures (varies)
│   ├── gui/                # GUI textures
│   ├── particle/           # Particle textures
│   ├── armor/              # Armor layer textures (64x32)
│   └── icon.png            # Mod icon
├── blockstates/            # Block state -> model mapping
├── particles/              # Particle definitions
├── sounds/                 # Sound files (.ogg)
├── sounds.json             # Sound event definitions
├── shaders/                # Custom shaders (optional)
└── atlases/                # Texture atlas configuration (optional)
```

### Server Data (data/)

```
src/main/resources/data/<modid>/
├── recipes/                # Crafting/smelting recipes
├── loot_tables/
│   └── blocks/             # Block drop tables
├── advancements/           # Custom advancements
├── tags/
│   ├── items/              # Item tags
│   ├── blocks/             # Block tags
│   ├── entity_types/       # Entity type tags
│   └── fluids/             # Fluid tags
├── worldgen/
│   ├── configured_feature/ # Feature configurations
│   ├── placed_feature/     # Feature placement
│   ├── biome/              # Custom biomes
│   └── structure/          # Custom structures
└── predicates/             # Loot condition predicates
```

## Language Files

### Translation Key Conventions

| Category | Key Format | Example |
|----------|-----------|---------|
| Item | `item.<modid>.<name>` | `item.modid.fire_wand` |
| Block | `block.<modid>.<name>` | `block.modid.magic_ore` |
| Item group | `itemgroup.<modid>.<name>` | `itemgroup.modid.magic_group` |
| Entity | `entity.<modid>.<name>` | `entity.modid.magic_slime` |
| Container/GUI | `container.<modid>.<name>` | `container.modid.magic_bench` |
| Advancement | `advancements.<modid>.<path>` | `advancements.modid.magic.root.title` |
| Biome | `biome.<modid>.<name>` | `biome.modid.magic_forest` |
| Effect | `effect.<modid>.<name>` | `effect.modid.magic_effect` |
| Enchantment | `enchantment.<modid>.<name>` | `enchantment.modid.magic_power` |
| Tooltip | `item.<modid>.<name>.tooltip` | `item.modid.fire_wand.tooltip` |
| Subtitle | `subtitles.<modid>.<name>` | `subtitles.modid.magic_bench.use` |

### en_us.json (Required)

```json
{
  "item.modid.fire_wand": "Fire Wand",
  "item.modid.fire_wand.tooltip": "Shoots fireballs",
  "item.modid.magic_berry": "Magic Berry",
  "block.modid.magic_ore": "Magic Ore",
  "block.modid.magic_block": "Magic Block",
  "itemgroup.modid.magic_group": "Magic Items",
  "entity.modid.magic_slime": "Magic Slime",
  "item.modid.magic_slime_spawn_egg": "Magic Slime Spawn Egg",
  "container.modid.magic_bench": "Magic Bench"
}
```

### zh_cn.json

```json
{
  "item.modid.fire_wand": "火焰法杖",
  "item.modid.fire_wand.tooltip": "发射火球",
  "item.modid.magic_berry": "魔法浆果",
  "block.modid.magic_ore": "魔法矿石",
  "block.modid.magic_block": "魔法方块",
  "itemgroup.modid.magic_group": "魔法物品",
  "entity.modid.magic_slime": "魔法史莱姆",
  "item.modid.magic_slime_spawn_egg": "魔法史莱姆刷怪蛋",
  "container.modid.magic_bench": "魔法工作台"
}
```

## Model JSON Formats

### Item Models

Place at `assets/<modid>/models/item/<name>.json`:

#### Standard Item (2D in inventory)
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "modid:item/fire_wand"
  }
}
```

#### Handheld Item (tools, weapons)
```json
{
  "parent": "item/handheld",
  "textures": {
    "layer0": "modid:item/magic_sword"
  }
}
```

#### Block as Item (uses block model)
```json
{
  "parent": "modid:block/magic_block"
}
```

#### With Overrides (conditional models)
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "modid:item/charged_wand"
  },
  "overrides": [
    {
      "predicate": {
        "minecraft:custom_model_data": 1
      },
      "model": "modid:item/charged_wand_active"
    }
  ]
}
```

### Block Models

Place at `assets/<modid>/models/block/<name>.json`:

#### All-sides-same (cube_all)
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "modid:block/magic_block"
  }
}
```

#### Pillar (different top/side)
```json
{
  "parent": "block/cube_column",
  "textures": {
    "end": "modid:block/magic_log_top",
    "side": "modid:block/magic_log"
  }
}
```

#### Different faces (cube_bottom_top)
```json
{
  "parent": "block/cube_bottom_top",
  "textures": {
    "top": "modid:block/magic_machine_top",
    "side": "modid:block/magic_machine_side",
    "bottom": "modid:block/magic_machine_bottom"
  }
}
```

#### Cross model (plants, flowers)
```json
{
  "parent": "block/cross",
  "textures": {
    "cross": "modid:block/magic_flower"
  }
}
```

### Blockstate JSON

Place at `assets/<modid>/blockstates/<name>.json`:

#### Simple (single model)
```json
{
  "variants": {
    "": { "model": "modid:block/magic_block" }
  }
}
```

#### Directional (facing)
```json
{
  "variants": {
    "facing=north": { "model": "modid:block/magic_machine" },
    "facing=east":  { "model": "modid:block/magic_machine", "y": 90 },
    "facing=south": { "model": "modid:block/magic_machine", "y": 180 },
    "facing=west":  { "model": "modid:block/magic_machine", "y": 270 }
  }
}
```

## Texture Specifications

### Resolution and Format
- **Format**: PNG (with alpha channel for transparency)
- **Standard resolution**: 16x16 pixels
- **Supported resolutions**: Any power of 2 (16, 32, 64, 128...)
- **Entity textures**: Varies by model (typically 64x32 or 64x64)
- **GUI textures**: Custom dimensions (typically 176x166 for standard GUI)
- **Armor layer**: 64x32 pixels

### Texture Naming Convention
```
textures/
├── item/
│   ├── fire_wand.png          # matches item name
│   └── magic_sword.png
├── block/
│   ├── magic_block.png        # matches block name
│   ├── magic_log.png          # side texture
│   └── magic_log_top.png      # end texture
├── entity/
│   └── magic_slime.png        # matches entity name
├── gui/
│   └── magic_bench.png        # matches GUI name
└── armor/
    └── magic_layer_1.png      # ArmorMaterial.getName() + "_layer_1"
```

## Sound Registration

### 1. Define Sound Events

```java
package com.example.modid.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import com.example.modid.ModNameMod;

public class ModSounds {
    public static final SoundEvent MAGIC_USE = register("magic_use");
    public static final SoundEvent MAGIC_BENCH_CRAFT = register("magic_bench_craft");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(ModNameMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {}
}
```

### 2. Create sounds.json

Place at `assets/<modid>/sounds.json`:

```json
{
  "magic_use": {
    "sounds": [
      {
        "name": "modid:magic_use",
        "volume": 0.5,
        "pitch": 1.0
      }
    ],
    "subtitle": "subtitles.modid.magic_use"
  },
  "magic_bench_craft": {
    "sounds": [
      "modid:magic_bench_craft_1",
      "modid:magic_bench_craft_2"
    ],
    "subtitle": "subtitles.modid.magic_bench_craft"
  }
}
```

### 3. Place Sound Files

Put `.ogg` files at `assets/<modid>/sounds/magic_use.ogg`

### 4. Play Sounds

```java
world.playSound(null, pos, ModSounds.MAGIC_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
```

## Data Generation (Datagen)

### Setup

Add datagen entrypoint to `fabric.mod.json`:

```json
{
  "entrypoints": {
    "fabric-datagen": ["com.example.modid.datagen.ModDatagen"]
  }
}
```

### Datagen Entry Point

```java
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class ModDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        pack.addProvider(ModEnglishLangProvider::new);
        pack.addProvider(ModItemModelProvider::new);
        pack.addProvider(ModBlockLootTableProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModTagProvider::new);
    }
}
```

### Language Provider

```java
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import java.util.concurrent.CompletableFuture;

public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataGenerator.Output output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, "en_us", registriesFuture);
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        builder.add(ModItems.FIRE_WAND, "Fire Wand");
        builder.add(ModBlocks.MAGIC_ORE, "Magic Ore");
        builder.add("itemgroup.modid.magic_group", "Magic Items");
        builder.add("container.modid.magic_bench", "Magic Bench");
    }
}
```

### Item Model Provider

```java
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;

public class ModItemModelProvider extends FabricModelProvider {
    public ModItemModelProvider(FabricDataGenerator.Output output) {
        super(output);
    }

    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        generator.register(ModItems.FIRE_WAND, Models.GENERATED);
        generator.register(ModItems.MAGIC_SWORD, Models.HANDHELD);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        generator.registerSimpleCubeAll(ModBlocks.MAGIC_BLOCK);
        generator.registerLog(ModBlocks.MAGIC_LOG).log("magic_log").wood("magic_wood");
    }
}
```

### Recipe Provider

```java
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.recipe.book.RecipeCategory;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataGenerator.Output output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        offerShapedRecipe(exporter, ModItems.MAGIC_SWORD,
            new String[]{"#", "#", "X"},
            Map.of('#', Ingredient.ofItems(ModItems.MAGIC_GEM),
                   'X', Ingredient.ofItems(Items.STICK)),
            "magic_tools");

        offerSmelting(exporter, List.of(ModItems.RAW_MAGIC),
            RecipeCategory.MISC, ModItems.MAGIC_INGOT,
            0.7f, 200, "magic_ingot");
    }
}
```

### Run Datagen

```bash
./gradlew runDatagen
```

Generated files appear in `src/main/generated/`. Add this to `build.gradle`:

```gradle
sourceSets {
    main {
        resources {
            srcDirs += ["src/main/generated"]
        }
    }
}
```

## Complete Asset Checklist

For each new content item:

1. [ ] Language entry in `en_us.json` (and `zh_cn.json` if Chinese support)
2. [ ] Item/block model JSON (or datagen)
3. [ ] Texture PNG (16x16 or appropriate size)
4. [ ] Blockstate JSON (for blocks)
5. [ ] Loot table JSON (for blocks, or datagen)
6. [ ] Sound files + `sounds.json` (if custom sounds)
7. [ ] Run `./gradlew runClient` to verify

## Cross-References

- For project setup, use `mc-fabric-init`
- For items requiring models, use `mc-fabric-item`
- For blocks requiring models/blockstates, use `mc-fabric-block`
- For entities requiring textures, use `mc-fabric-entity`
- For GUI textures, use `mc-fabric-gui`
- For worldgen data files, use `mc-fabric-worldgen`

## Resources

### references/
- `datagen_guide.md` - Complete data generation guide: all provider types, model helpers, tag generation, and build configuration
