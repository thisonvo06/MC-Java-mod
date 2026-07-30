---
name: mc-fabric-worldgen
description: "Minecraft Fabric world generation design for MC 1.21.x with Java. This skill should be used when creating custom ore generation, world features (trees, plants, structures), biome modifications, and custom feature placement. Covers ConfiguredFeature, PlacedFeature, BiomeModifications API, ore vein configuration, height ranges, and biome-specific generation rules."
agent_created: true
---

# Minecraft Fabric World Generation (MC 1.21.x / Java)

## Overview

This skill provides the workflow for adding custom world generation to a Fabric mod targeting MC 1.21.x. It covers ore generation, feature placement, and biome modifications.

## When to Use

- Generating custom ore deposits underground
- Adding custom trees, plants, or decorations to biomes
- Modifying existing biome generation (adding/removing features)
- Creating custom generation features (block patches, clusters)
- Controlling spawn height, frequency, and biome distribution of features
- Adding or removing mob spawns in specific biomes

## World Generation Pipeline

```
ConfiguredFeature  →  PlacedFeature  →  BiomeModification
(what to generate)    (where/how many)   (which biomes)
```

1. **ConfiguredFeature**: Defines WHAT to generate (ore block, tree shape, flower type)
2. **PlacedFeature**: Defines WHERE and HOW MANY (height range, count per chunk, spread)
3. **BiomeModification**: Defines WHICH BIOMES get the feature

## Ore Generation

### Step 1: Define ConfiguredFeature (Code)

```java
package com.example.modid.worldgen;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import com.example.modid.ModNameMod;
import com.example.modid.registry.ModBlocks;

public class ModConfiguredFeatures {
    public static final RegistryKey<ConfiguredFeature<?, ?>> MAGIC_ORE_OVERWORLD = registerKey("magic_ore_overworld");
    public static final RegistryKey<ConfiguredFeature<?, ?>> MAGIC_ORE_NETHER = registerKey("magic_ore_nether");

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> context) {
        // Overworld ore: stone replacement, vein size 9
        register(context, MAGIC_ORE_OVERWORLD, Feature.ORE,
            new OreFeatureConfig(
                List.of(
                    OreFeatureConfig.createTarget(
                        OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                        ModBlocks.MAGIC_ORE.getDefaultState()),
                    OreFeatureConfig.createTarget(
                        OreConfiguredFeatures.DEEPSLATE_ORE_REPLACEABLES,
                        ModBlocks.DEEPSLATE_MAGIC_ORE.getDefaultState())
                ),
                9));  // Vein size

        // Nether ore: netherrack replacement, vein size 12
        register(context, MAGIC_ORE_NETHER, Feature.ORE,
            new OreFeatureConfig(
                List.of(
                    OreFeatureConfig.createTarget(
                        OreConfiguredFeatures.NETHERRACK,
                        ModBlocks.MAGIC_ORE.getDefaultState())
                ),
                12));
    }

    public static RegistryKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
            Identifier.of(ModNameMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfig, F extends Feature<FC>> void register(
            Registerable<ConfiguredFeature<?, ?>> context,
            RegistryKey<ConfiguredFeature<?, ?>> key,
            F feature, FC config) {
        context.register(key, new ConfiguredFeature<>(feature, config));
    }
}
```

### Step 2: Define PlacedFeature (Code)

```java
package com.example.modid.worldgen;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.*;
import java.util.List;
import com.example.modid.ModNameMod;

public class ModPlacedFeatures {
    public static final RegistryKey<PlacedFeature> MAGIC_ORE_PLACED = registerKey("magic_ore_placed");
    public static final RegistryKey<PlacedFeature> MAGIC_ORE_NETHER_PLACED = registerKey("magic_ore_nether_placed");

    public static void bootstrap(Registerable<PlacedFeature> context) {
        // Overworld: Y -64 to 80, 20 veins per chunk
        context.register(MAGIC_ORE_PLACED,
            new PlacedFeature(
                RegistryEntry.of(ModConfiguredFeatures.MAGIC_ORE_OVERWORLD.getValue()),
                List.of(
                    CountPlacementModifier.of(20),           // 20 attempts per chunk
                    SquarePlacementModifier.getInstance(),   // Spread X/Z
                    HeightRangePlacementModifier.uniform(
                        YOffset.BOTTOM,    // Min Y: -64
                        YOffset.TOP)       // Max Y: 320
                )));

        // Nether: Y 0 to 128, 15 veins per chunk
        context.register(MAGIC_ORE_NETHER_PLACED,
            new PlacedFeature(
                RegistryEntry.of(ModConfiguredFeatures.MAGIC_ORE_NETHER.getValue()),
                List.of(
                    CountPlacementModifier.of(15),
                    SquarePlacementModifier.getInstance(),
                    HeightRangePlacementModifier.uniform(
                        YOffset.fixed(0),
                        YOffset.fixed(128))
                )));
    }

    public static RegistryKey<PlacedFeature> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE,
            Identifier.of(ModNameMod.MOD_ID, name));
    }
}
```

### Step 3: Register Datagen (or use JSON)

To use code-based worldgen, register a Datagen provider. Alternatively, create JSON files directly.

#### JSON: ConfiguredFeature

Place at `data/<modid>/worldgen/configured_feature/magic_ore_overworld.json`:

```json
{
  "type": "minecraft:ore",
  "config": {
    "size": 9,
    "targets": [
      {
        "target": {
          "predicate_type": "minecraft:tag_match",
          "tag": "minecraft:stone_ore_replaceables"
        },
        "state": {
          "Name": "modid:magic_ore"
        }
      },
      {
        "target": {
          "predicate_type": "minecraft:tag_match",
          "tag": "minecraft:deepslate_ore_replaceables"
        },
        "state": {
          "Name": "modid:deepslate_magic_ore"
        }
      }
    ]
  }
}
```

#### JSON: PlacedFeature

Place at `data/<modid>/worldgen/placed_feature/magic_ore_placed.json`:

```json
{
  "feature": "modid:magic_ore_overworld",
  "placement": [
    {
      "type": "minecraft:count",
      "count": 20
    },
    {
      "type": "minecraft:in_square"
    },
    {
      "type": "minecraft:height_range",
      "height": {
        "type": "minecraft:uniform",
        "min_inclusive": {
          "absolute": -64
        },
        "max_inclusive": {
          "absolute": 320
        }
      }
    },
    {
      "type": "minecraft:biome"
    }
  ]
}
```

### Step 4: Add to Biomes (BiomeModification)

```java
package com.example.modid.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import com.example.modid.ModNameMod;

public class ModWorldGeneration {
    public static void generateWorldGen() {
        // Add ore to overworld biomes
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(ModNameMod.MOD_ID, "magic_ore_placed"))
        );

        // Add ore to nether biomes
        BiomeModifications.addFeature(
            BiomeSelectors.foundInTheNether(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(ModNameMod.MOD_ID, "magic_ore_nether_placed"))
        );
    }
}
```

Call `ModWorldGeneration.generateWorldGen()` from `onInitialize()`.

## BiomeSelectors Reference

| Selector | Description |
|----------|-------------|
| `foundInOverworld()` | All overworld biomes |
| `foundInTheNether()` | All nether biomes |
| `foundInTheEnd()` | All end biomes |
| `foundOverworld()` | Same as foundInOverworld |
| `includeByKey(key)` | Specific biome(s) by key |
| `excludeByKey(key)` | All biomes except specified |
| `tag(tagKey)` | Biomes matching a tag |
| `all()` | All biomes |
| `and(selector1, selector2)` | Both selectors match |
| `or(selector1, selector2)` | Either selector matches |

## GenerationStep Order

| Step | Description | Typical Use |
|------|-------------|-------------|
| `RAW_GENERATION` | First | Bedrock, islands |
| `LAKES` | Lakes | Water/lava lakes |
| `LOCAL_MODIFICATIONS` | Local | Cacti, bamboo |
| `UNDERGROUND_STRUCTURES` | Underground | Mineshafts, dungeons |
| `SURFACE_STRUCTURES` | Surface | Villages, temples |
| `STRONGHOLDS` | Strongholds | Strongholds |
| `UNDERGROUND_ORES` | Ores | Ore veins |
| `UNDERGROUND_DECORATION` | Underground | Cave plants, mushrooms |
| `FLUID_SPRINGS` | Springs | Water/lava springs |
| `VEGETAL_DECORATION` | Plants | Trees, grass, flowers |
| `TOP_LAYER_MODIFICATIONATION` | Last | Snow, ice |

## Common Feature Types

### Simple Block Placement (Flowers, Plants)

```java
// ConfiguredFeature: single block placement
register(context, MAGIC_FLOWER, Feature.SIMPLE_BLOCK,
    new SimpleBlockFeatureConfig(
        BlockStateProvider.of(ModBlocks.MAGIC_FLOWER.getDefaultState())));

// PlacedFeature: scatter with rarity modifier
List.of(
    RarityFilterPlacementModifier.of(32),     // 1 in 32 chunks
    SquarePlacementModifier.getInstance(),
    HeightRangePlacementModifier.uniform(YOffset.fixed(62), YOffset.TOP),
    BiomePlacementModifier.getInstance()
);
```

### Random Patch (Grass, Flowers Spread)

```java
register(context, MAGIC_GRASS, Feature.RANDOM_PATCH,
    new RandomPatchFeatureConfig(
        32,    // tries
        7,     // spread_xz
        3,     // spread_y
        PlacedFeatures.createEntry(Feature.SIMPLE_BLOCK,
            new SimpleBlockFeatureConfig(
                BlockStateProvider.of(ModBlocks.MAGIC_GRASS.getDefaultState())))
    ));
```

### Tree Generation

```java
// ConfiguredFeature: oak-like tree with custom leaves
register(context, MAGIC_TREE, Feature.TREE,
    new TreeFeatureConfig.Builder(
        BlockStateProvider.of(ModBlocks.MAGIC_LOG.getDefaultState()),   // trunk
        new StraightTrunkPlacer(4, 2, 0),                               // trunk height
        BlockStateProvider.of(ModBlocks.MAGIC_LEAVES.getDefaultState()), // leaves
        new BlobFoliagePlacer(ConstantIntProvider.create(2),
            ConstantIntProvider.create(0), 3),                         // leaf shape
        new TwoLayersFeatureSize(1, 0, 1))                             // size
    .build());
```

## Height Range Types

| Type | Description | Example |
|------|-------------|---------|
| `uniform` | Even distribution between min and max | `-64 to 320` |
| `trapezoid` | Peak in middle, tapers at edges | `-64 to 320, peak at 128` |
| `very_biased_to_bottom` | More likely near bottom | Used for diamonds |
| `biased_to_bottom` | Slightly more likely near bottom | |
| `biased_to_top` | More likely near top | |

## Complete Worldgen Checklist

1. [ ] Define ConfiguredFeature (code or JSON)
2. [ ] Define PlacedFeature (code or JSON)
3. [ ] Add BiomeModification to target biomes
4. [ ] Call `generateWorldGen()` from `onInitialize()`
5. [ ] Set appropriate GenerationStep
6. [ ] Test with `/locate` or explore in dev world
7. [ ] Adjust vein size, count, and height based on testing

## Cross-References

- For project setup, use `mc-fabric-init`
- For ore blocks, use `mc-fabric-block`
- For entity biome spawning, use `mc-fabric-entity`
- For data generation (auto-generate worldgen JSON), use `mc-fabric-assets`

## Resources

### references/
- `worldgen_api.md` - Detailed API reference for Feature types, ConfiguredFeature, PlacedFeature, placement modifiers, biome modification context, and custom feature creation
