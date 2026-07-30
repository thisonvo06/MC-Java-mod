# Fabric Data Generation Guide (MC 1.21.x)

## Overview

Data generation (datagen) auto-generates JSON files (models, lang, recipes, loot tables, tags, worldgen) from Java code. This eliminates manual JSON authoring and keeps data consistent.

## Build Configuration

### build.gradle additions

```gradle
// Already included if using fabric-loom, but verify:
// The 'fabric-datagen' entrypoint is supported by fabric-loom out of the box

// To run datagen:
// ./gradlew runDatagen
```

### fabric.mod.json entrypoint

```json
{
  "entrypoints": {
    "main": ["com.example.modid.ModNameMod"],
    "client": ["com.example.modid.ModNameModClient"],
    "fabric-datagen": ["com.example.modid.datagen.ModDatagen"]
  }
}
```

## Provider Types

### 1. Language Provider

```java
public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataGenerator.Output output,
            CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, "en_us", registriesFuture);
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        // Items
        builder.add(ModItems.FIRE_WAND, "Fire Wand");
        builder.add(ModItems.MAGIC_BERRY, "Magic Berry");

        // Blocks
        builder.add(ModBlocks.MAGIC_ORE, "Magic Ore");
        builder.add(ModBlocks.MAGIC_BLOCK, "Magic Block");

        // Item groups
        builder.add("itemgroup.modid.magic_group", "Magic Items");

        // Containers
        builder.add("container.modid.magic_bench", "Magic Bench");

        // Entities
        builder.add(ModEntities.MAGIC_SLIME, "Magic Slime");
        builder.add(ModItems.MAGIC_SLIME_SPAWN_EGG, "Magic Slime Spawn Egg");

        // Tooltips
        builder.add("item.modid.fire_wand.tooltip", "Shoots fireballs");

        // Custom text
        builder.add("biome.modid.magic_forest", "Magic Forest");

        // Add from existing JSON (merge)
        builder.add("path/to/existing/lang.json");
    }
}
```

### 2. Model Provider

```java
public class ModModelProvider extends FabricModelProvider {
    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        // Standard 2D item
        generator.register(ModItems.FIRE_WAND, Models.GENERATED);

        // Handheld (tools/weapons)
        generator.register(ModItems.MAGIC_SWORD, Models.HANDHELD);
        generator.register(ModItems.MAGIC_PICKAXE, Models.HANDHELD);

        // Spawn egg (builtin)
        generator.register(ModItems.MAGIC_SLIME_SPAWN_EGG, Models.TEMPLATE_SPAWN_EGG);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        // Simple cube (all sides same)
        generator.registerSimpleCubeAll(ModBlocks.MAGIC_BLOCK);

        // Pillar (log)
        generator.registerLog(ModBlocks.MAGIC_LOG)
            .log("magic_log")
            .wood("magic_wood");

        // Stairs
        generator.registerSimpleStairs(ModBlocks.MAGIC_STAIRS);

        // Slab
        generator.registerSimpleSlab(ModBlocks.MAGIC_SLAB);

        // Fence
        generator.registerFence(ModBlocks.MAGIC_FENCE, "magic_block");

        // Wall
        generator.registerWall(ModBlocks.MAGIC_WALL, "magic_block");

        // Door (needs item model separately)
        generator.registerDoor(ModBlocks.MAGIC_DOOR, "magic_door_bottom", "magic_door_top");

        // Crop (age 0-7)
        generator.registerCrop(ModBlocks.MAGIC_CROP, 0, 7,
            "magic_crop_stage_0", "magic_crop_stage_1", ...);

        // Custom block state
        generator.blockStateCollector.accept(VariantsBlockStateSupplier
            .create(ModBlocks.MAGIC_LAMP)
            .variant(BlockStateVariant.create()
                .put(VariantSettings.MODEL, Identifier.of(MOD_ID, "block/magic_lamp"))));
    }
}
```

### 3. Recipe Provider

```java
public class ModRecipeProvider extends FabricRecipeProvider {
    @Override
    public void generate(RecipeExporter exporter) {
        // Shaped crafting
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.MAGIC_SWORD, 1)
            .pattern("#")
            .pattern("#")
            .pattern("X")
            .input('#', ModItems.MAGIC_GEM)
            .input('X', Items.STICK)
            .criterion(hasItem(ModItems.MAGIC_GEM), conditionsFromItem(ModItems.MAGIC_GEM))
            .offerTo(exporter, Identifier.of(MOD_ID, "magic_sword"));

        // Shapeless crafting
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.MAGIC_DUST, 4)
            .input(ModItems.MAGIC_GEM)
            .criterion(hasItem(ModItems.MAGIC_GEM), conditionsFromItem(ModItems.MAGIC_GEM))
            .offerTo(exporter, Identifier.of(MOD_ID, "magic_dust"));

        // Smelting
        offerSmelting(exporter, List.of(ModItems.RAW_MAGIC),
            RecipeCategory.MISC, ModItems.MAGIC_INGOT,
            0.7f, 200, "magic_ingot");

        // Blasting
        offerBlasting(exporter, List.of(ModItems.RAW_MAGIC),
            RecipeCategory.MISC, ModItems.MAGIC_INGOT,
            0.7f, 100, "magic_ingot");

        // Stonecutting
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.MAGIC_BRICK, ModBlocks.MAGIC_STONE, 2);

        // Block -> items (3x3)
        offerReversibleCompactingRecipes(exporter, RecipeCategory.BUILDING_BLOCKS,
            ModItems.MAGIC_INGOT, RecipeCategory.BUILDING_BLOCKS, ModBlocks.MAGIC_BLOCK);
    }
}
```

### 4. Loot Table Provider

```java
public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {
    public ModBlockLootTableProvider(FabricDataGenerator.Output output) {
        super(output);
    }

    @Override
    public void generate() {
        // Self-drop
        addDrop(ModBlocks.MAGIC_BLOCK);

        // Ore with silk touch alternative
        addDrop(ModBlocks.MAGIC_ORE, oreDrops(ModBlocks.MAGIC_ORE, ModItems.MAGIC_GEM));

        // Drops nothing
        addDrop(ModBlocks.MAGIC_GLASS, dropsNothing());

        // Drops another item
        addDrop(ModBlocks.MAGIC_LEAVES,
            leavesDrops(ModBlocks.MAGIC_LEAVES, ModBlocks.MAGIC_SAPLING, 0.05f));

        // Conditional drop
        addDrop(ModBlocks.MAGIC_BLOCK, drops(ModBlocks.MAGIC_BLOCK)
            .apply(LootTable.builder()
                .pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(ItemEntry.builder(ModItems.MAGIC_GEM)
                        .apply(SetCountLootFunction.builder(
                            UniformLootNumberProvider.create(1.0f, 3.0f)))))));
    }
}
```

### 5. Tag Provider

```java
public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataGenerator.Output output,
            CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture,
            FabricTagProvider.BlockTagProvider blockTagProvider) {
        super(output, registriesFuture, blockTagProvider);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
        // Add to vanilla tags
        getOrCreateTagBuilder(ItemTags.PICKAXES)
            .add(ModItems.MAGIC_PICKAXE);
        getOrCreateTagBuilder(ItemTags.SWORDS)
            .add(ModItems.MAGIC_SWORD);

        // Create custom tag
        getOrCreateTagBuilder(RegistryKey.of(RegistryKeys.ITEM,
            Identifier.of(MOD_ID, "magic_tools")))
            .add(ModItems.MAGIC_PICKAXE)
            .add(ModItems.MAGIC_SWORD)
            .add(ModItems.MAGIC_AXE);

        // Add to mod items tag (for recipe ingredients)
        getOrCreateTagBuilder(ItemTags.create(Identifier.of(MOD_ID, "magic_dusts")))
            .add(ModItems.MAGIC_DUST)
            .add(ModItems.SPARKLE_DUST);
    }
}

// Block tags
public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
        // Mining tags
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
            .add(ModBlocks.MAGIC_ORE)
            .add(ModBlocks.MAGIC_BLOCK);
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
            .add(ModBlocks.MAGIC_ORE);
    }
}
```

### 6. Worldgen Provider

```java
public class ModWorldGenProvider extends FabricDynamicRegistryProvider {
    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
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

## Model Helper Constants

| Constant | Parent | Use Case |
|----------|--------|----------|
| `Models.GENERATED` | `item/generated` | Standard 2D items |
| `Models.HANDHELD` | `item/handheld` | Tools, weapons |
| `Models.TEMPLATE_SPAWN_EGG` | `item/template_spawn_egg` | Spawn eggs |
| `Models.BED_ITEM` | `item/template_bed` | Beds |

## Block Model Helpers

| Method | Description |
|--------|-------------|
| `registerSimpleCubeAll(block)` | All sides same texture |
| `registerLog(block)` | Pillar with end/side |
| `registerSimpleStairs(block)` | Stairs |
| `registerSimpleSlab(block)` | Slab |
| `registerFence(block, texture)` | Fence |
| `registerWall(block, texture)` | Wall |
| `registerDoor(block, bottom, top)` | Door |
| `registerTrapdoor(block)` | Trapdoor |
| `registerCrop(block, min, max, ...)` | Growing crop |
| `registerCross(block)` | X-shaped (flowers) |
| `registerTintableCross(block)` | Tinted X-shaped |
| `registerAxis(block)` | Rotatable pillar |
| `registerOrientable(block, front, side)` | Machine with front |

## Common Pitfalls

1. **Missing criterion**: Every recipe needs a criterion (unlock condition). Use `hasItem()` helper.
2. **Duplicate IDs**: Recipe names must be unique. Use `Identifier.of(MOD_ID, "name")` to control.
3. **Generated resources not loaded**: Add `srcDirs += ["src/main/generated"]` to `sourceSets.main.resources`.
4. **Client-only code in common**: Datagen runs on server side; don't reference client classes.
5. **Order matters**: Block tags must be generated before item tags (item tag provider depends on block tags).
