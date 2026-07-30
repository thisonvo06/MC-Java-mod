# Fabric World Generation API Reference (MC 1.21.x)

## Feature Types (Feature<FC extends FeatureConfig>)

| Feature | Config Class | Description |
|---------|-------------|-------------|
| `ORE` | `OreFeatureConfig` | Ore vein generation |
| `SIMPLE_BLOCK` | `SimpleBlockFeatureConfig` | Single block placement |
| `RANDOM_PATCH` | `RandomPatchFeatureConfig` | Scattered blocks (flowers, grass) |
| `TREE` | `TreeFeatureConfig` | Tree generation |
| `FOREST_ROCK` | `ForestRockFeatureConfig` | Mossy cobblestone boulders |
| `LAKE` | `LakeFeatureConfig` | Water/lava lakes |
| `DISK` | `DiskFeatureConfig` | Circular patches (clay, sand) |
| `REPLACE_SINGLE_BLOCK` | `ReplaceSingleBlockFeatureConfig` | Single block swap |
| `NO_BONEMEAL_FLOWER` | `RandomPatchFeatureConfig` | Flowers that can't be bonemealed |
| `BLOCK_COLUMN` | `BlockColumnFeatureConfig` | Vertical pillar of blocks |
| `DELTA_FEATURE` | `DeltaFeatureConfig` | Nether deltas (lava + obsidian) |
| `NETHER_FOREST_VEGETATION` | `NetherForestVegetationFeatureConfig` | Nether plants |
| `SCATTERED_ORE` | `OreFeatureConfig` | Smaller, scattered ore |
| `BAMBOO` | `BambooFeatureConfig` | Bamboo stalks |
| `HUGE_BROWN_MUSHROOM` | `HugeMushroomFeatureConfig` | Big brown mushroom |
| `HUGE_RED_MUSHROOM` | `HugeMushroomFeatureConfig` | Big red mushroom |
| `CRYSTAL_FORMATION` | - | Amethyst clusters |
| `KELP` | - | Kelp underwater |
| `CORAL` | - | Coral reefs |

## Placement Modifiers

| Modifier | Parameters | Description |
|----------|-----------|-------------|
| `CountPlacementModifier.of(count)` | int count | Attempts per chunk |
| `SquarePlacementModifier.getInstance()` | - | Spread X/Z within chunk |
| `HeightRangePlacementModifier.uniform(min, max)` | YOffset | Uniform Y distribution |
| `HeightRangePlacementModifier.trapezoid(min, max)` | YOffset | Trapezoidal Y distribution |
| `RarityFilterPlacementModifier.of(chance)` | int (1 in N) | 1 in N chunks |
| `BiomePlacementModifier.getInstance()` | - | Only in valid biomes |
| `EnvironmentScanPlacementModifier` | direction, target, maxSteps | Scan for condition |
| `RandomOffsetPlacementModifier.of(x, y, z)` | IntProvider | Random offset |
| `HeightmapPlacementModifier.onHeightmap(type)` | Heightmap.Type | Place on heightmap |
| `CountOnEveryLayerPlacementModifier.of(count)` | int | Per-layer placement |
| `NoisePlacementModifier` | noise, threshold | Based on noise map |
| `SurfaceWaterThresholdFilterPlacementModifier` | int | Above/below water level |

## YOffset Types

```java
YOffset.BOTTOM              // Y = -64 (world bottom)
YOffset.TOP                 // Y = 320 (world top)
YOffset.fixed(int)          // Absolute Y level
```

## Ore Replacement Targets

| Target | Replaces | Used For |
|--------|----------|----------|
| `OreConfiguredFeatures.STONE_ORE_REPLACEABLES` | Stone, Granite, Diorite, Andesite, Tuff | Overworld stone ores |
| `OreConfiguredFeatures.DEEPSLATE_ORE_REPLACEABLES` | Deepslate, Tuff | Overworld deepslate ores |
| `OreConfiguredFeatures.NETHERRACK` | Netherrack | Nether ores |
| `OreConfiguredFeatures.ENDSTONE` | End Stone | End ores |

## Tree Placers

### Trunk Placers
| Class | Parameters | Description |
|-------|-----------|-------------|
| `StraightTrunkPlacer` | height, height_a, height_b | Simple straight trunk |
| `DarkOakTrunkPlacer` | height, height_a, height_b | Dark oak trunk (2x2) |
| `MegaJungleTrunkPlacer` | height, height_a, height_b | Jungle giant trunk |
| `BendingTrunkPlacer` | height, height_a, height_b, min_length_to_bend | Bent trunk |
| `LargeOakTrunkPlacer` | height, height_a, height_b | Large oak branching |
| `CherryTrunkPlacer` | height, height_a, height_b, branch_count, branch_horizontal_length, branch_start_offset_from_top | Cherry trunk |

### Foliage Placers
| Class | Parameters | Description |
|-------|-----------|-------------|
| `BlobFoliagePlacer` | radius, offset, height | Round blob (oak) |
| `SpruceFoliagePlacer` | radius, offset, crown_height | Spruce cone |
| `PineFoliagePlacer` | radius, offset, crown_height | Pine cone |
| `AcaciaFoliagePlacer` | radius, offset | Flat acacia canopy |
| `BushFoliagePlacer` | radius, offset | Small bush |
| `FancyFoliagePlacer` | radius, offset | Fancy rounded |
| `JungleFoliagePlacer` | radius, offset | Jungle canopy |
| `DarkOakFoliagePlacer` | radius, offset | Dark oak canopy |

## Datagen for Worldgen

### FabricDataGenerator Setup

```java
// In build.gradle, add:
// modImplementation 'net.fabricmc.fabric-api:fabric-api:${project.fabric_version}'

// Create a datagen entry point
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import com.example.modid.ModNameMod;

public class ModDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ModWorldGenProvider::new);
    }
}
```

### Register Datagen Entrypoint

In `fabric.mod.json`:
```json
{
  "entrypoints": {
    "fabric-datagen": ["com.example.modid.datagen.ModDatagen"]
  }
}
```

### WorldGen Provider

```java
package com.example.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;
import java.util.concurrent.CompletableFuture;
import com.example.modid.worldgen.ModConfiguredFeatures;
import com.example.modid.worldgen.ModPlacedFeatures;

public class ModWorldGenProvider extends FabricDynamicRegistryProvider {
    public ModWorldGenProvider(FabricDataGenerator.Output output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
        // Bootstrap configured and placed features
        ModConfiguredFeatures.bootstrap(
            registries.getWrapperOrThrow(RegistryKeys.CONFIGURED_FEATURE).getOrCreateEntryWriter()
        );
        ModPlacedFeatures.bootstrap(
            registries.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE).getOrCreateEntryWriter()
        );
    }

    @Override
    public String getName() { return "World Generation"; }
}
```

### Run Datagen

```bash
./gradlew runDatagen
```

Output JSON files are written to `src/main/generated/`.
