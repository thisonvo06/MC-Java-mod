---
name: mc-fabric-recipe
description: "Minecraft Fabric recipe and crafting design for MC 1.21.x with Java. This skill should be used when creating custom crafting recipes (shaped and shapeless), smelting recipes (furnace, blast furnace, smoker, campfire), stonecutting recipes, smithing recipes, custom recipe types, and recipe serialization. Covers JSON recipe format, code-based recipe registration, and recipe data generation."
agent_created: true
---

# Minecraft Fabric Recipe Design (MC 1.21.x / Java)

## Overview

This skill provides the workflow for creating custom recipes in a Fabric mod targeting MC 1.21.x. It covers all vanilla recipe types (shaped, shapeless, smelting, stonecutting, smithing) and custom recipe type creation.

## When to Use

- Creating shaped crafting recipes (grid-based patterns)
- Creating shapeless crafting recipes (any arrangement)
- Creating smelting recipes (furnace, blast furnace, smoker, campfire)
- Creating stonecutting recipes (1 input -> 1 output)
- Creating smithing transform/trim recipes
- Creating custom recipe types with special logic
- Registering recipes programmatically (FuelRegistry, etc.)
- Converting between item types via recipes

## Recipe File Location

All recipe JSON files go in: `src/main/resources/data/<modid>/recipes/<recipe_name>.json`

Recipes can also override/extend vanilla recipes by placing them in the `minecraft` namespace: `data/minecraft/recipes/`

## Shaped Crafting Recipes

### Basic Pattern

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "###",
    "# #",
    "###"
  ],
  "key": {
    "#": {
      "item": "modid:magic_ingot"
    }
  },
  "result": {
    "id": "modid:magic_chestplate",
    "count": 1
  }
}
```

### Pattern Rules

- Each string in `pattern` represents one row of the crafting grid
- Characters map to ingredients in `key`
- Space `" "` means empty slot
- Pattern dimensions: 1x1 to 3x3 for crafting table
- Pattern is position-independent on the grid (recipe can be placed anywhere)

### Multiple Ingredients for One Key

```json
"key": {
  "#": {
    "tag": "minecraft:planks"
  },
  "X": {
    "item": "minecraft:stick"
  },
  "Y": [
    { "item": "modid:magic_gem" },
    { "item": "modid:magic_crystal" }
  ]
}
```

### Advanced: With Conditions

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["###", "###", "###"],
  "key": {
    "#": { "item": "modid:magic_ingot" }
  },
  "result": {
    "id": "modid:magic_block",
    "count": 1
  }
}
```

### Full Example: Sword Recipe

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "#",
    "#",
    "X"
  ],
  "key": {
    "#": { "item": "modid:magic_gem" },
    "X": { "item": "minecraft:stick" }
  },
  "result": {
    "id": "modid:magic_sword",
    "count": 1
  }
}
```

## Shapeless Crafting Recipes

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "modid:magic_dust" },
    { "item": "modid:magic_dust" },
    { "item": "modid:magic_dust" },
    { "item": "minecraft:gold_ingot" }
  ],
  "result": {
    "id": "modid:magic_ingot",
    "count": 1
  }
}
```

Ingredients can be in any position on the crafting grid. Order in the `ingredients` array does not matter.

## Smelting Recipes

### Furnace

```json
{
  "type": "minecraft:smelting",
  "ingredient": {
    "item": "modid:magic_ore"
  },
  "result": {
    "id": "modid:magic_ingot"
  },
  "experience": 0.7,
  "cookingtime": 200
}
```

### Blast Furnace

```json
{
  "type": "minecraft:blasting",
  "ingredient": {
    "item": "modid:magic_ore"
  },
  "result": {
    "id": "modid:magic_ingot"
  },
  "experience": 0.7,
  "cookingtime": 100
}
```

### Smoker

```json
{
  "type": "minecraft:smoking",
  "ingredient": {
    "item": "modid:raw_magic_meat"
  },
  "result": {
    "id": "modid:cooked_magic_meat"
  },
  "experience": 0.35,
  "cookingtime": 100
}
```

### Campfire

```json
{
  "type": "minecraft:campfire_cooking",
  "ingredient": {
    "item": "modid:raw_magic_meat"
  },
  "result": {
    "id": "modid:cooked_magic_meat"
  },
  "cookingtime": 600
}
```

### Cooking Time Reference

| Type | Default Time (ticks) | Seconds |
|------|---------------------|---------|
| Smelting (furnace) | 200 | 10 |
| Blasting | 100 | 5 |
| Smoking | 100 | 5 |
| Campfire | 600 | 30 |

## Stonecutting Recipes

```json
{
  "type": "minecraft:stonecutting",
  "ingredient": {
    "item": "modid:magic_stone"
  },
  "result": {
    "id": "modid:magic_brick"
  },
  "count": 2
}
```

Stonecutting always takes exactly 1 input item and produces 1+ output items. No pattern needed.

## Smithing Recipes (MC 1.21.x)

### Smithing Transform (equipment upgrade)

```json
{
  "type": "minecraft:smithing_transform",
  "template": {
    "item": "modid:magic_upgrade_smithing_template"
  },
  "base": {
    "item": "minecraft:diamond_sword"
  },
  "addition": {
    "item": "modid:magic_ingot"
  },
  "result": {
    "id": "modid:magic_sword"
  }
}
```

### Smithing Trim (cosmetic armor trim)

```json
{
  "type": "minecraft:smithing_trim",
  "template": {
    "item": "minecraft:bolt_armor_trim_smithing_template"
  },
  "base": {
    "tag": "minecraft:trimmable_armor"
  },
  "addition": {
    "tag": "minecraft:trim_materials"
  }
}
```

## Code-Based Recipe Registration

Some recipe types are registered via code rather than JSON:

### Fuel Registration

```java
import net.fabricmc.fabric.api.registry.FuelRegistry;

// Register item as fuel
FuelRegistry.INSTANCE.add(ModItems.MAGIC_COAL, 200);  // 200 ticks = 10 smelts
FuelRegistry.INSTANCE.add(ModItems.MAGIC_BLOCK, 2000); // 2000 ticks = 100 smelts

// Remove vanilla fuel
FuelRegistry.INSTANCE.remove(Items.COAL);
```

| Fuel | Burn Time (ticks) | Smelts |
|------|-------------------|--------|
| Stick | 100 | 0.5 |
| Coal | 1600 | 8 |
| Charcoal | 1600 | 8 |
| Coal Block | 16000 | 80 |
| Blaze Rod | 2400 | 12 |
| Lava Bucket | 20000 | 100 |

### Composting

```java
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;

CompostingChanceRegistry.INSTANCE.add(ModItems.MAGIC_BERRY, 0.65f);  // 0.0-1.0 probability
```

### Stripping Logs (Axe)

```java
import net.fabricmc.fabric.api.registry.AxeStripRegistry;  // If available
// Or use SurvivalAxeItem interactions

// In 1.21.x, stripping is handled via item interactions
// Add to AxeItem STRIPPED_BLOCKS map via mixin or Fabric API
```

## Custom Recipe Types

### 1. Define Recipe Type

```java
package com.example.modid.recipe;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.example.modid.ModNameMod;

public class ModRecipeTypes {
    public static final RecipeType<MagicBenchRecipe> MAGIC_BENCH =
        Registry.register(Registries.RECIPE_TYPE,
            Identifier.of(ModNameMod.MOD_ID, "magic_bench"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return ModNameMod.MOD_ID + ":magic_bench";
                }
            });
}
```

### 2. Define Recipe Serializer

```java
package com.example.modid.recipe;

import net.minecraft.recipe.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import com.example.modid.ModNameMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class MagicBenchRecipe implements Recipe<MagicBenchInput> {
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack output;

    public MagicBenchRecipe(Ingredient inputA, Ingredient inputB, ItemStack output) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
    }

    public Ingredient getInputA() { return inputA; }
    public Ingredient getInputB() { return inputB; }

    @Override
    public boolean matches(MagicBenchInput input, net.minecraft.world.World world) {
        return inputA.test(input.getStackInSlot(0)) && inputB.test(input.getStackInSlot(1));
    }

    @Override
    public ItemStack craft(MagicBenchInput input, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) { return true; }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registries) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MAGIC_BENCH;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MAGIC_BENCH;
    }

    // Serializer
    public static class Serializer implements RecipeSerializer<MagicBenchRecipe> {
        public static final MapCodec<MagicBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Ingredient.CODEC.fieldOf("input_a").forGetter(MagicBenchRecipe::getInputA),
                Ingredient.CODEC.fieldOf("input_b").forGetter(MagicBenchRecipe::getInputB),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output)
            ).apply(instance, MagicBenchRecipe::new)
        );

        public static final PacketCodec<RegistryByteBuf, MagicBenchRecipe> PACKET_CODEC =
            PacketCodec.tuple(
                Ingredient.PACKET_CODEC, MagicBenchRecipe::getInputA,
                Ingredient.PACKET_CODEC, MagicBenchRecipe::getInputB,
                ItemStack.PACKET_CODEC, r -> r.output,
                MagicBenchRecipe::new
            );

        @Override
        public MapCodec<MagicBenchRecipe> codec() { return CODEC; }

        @Override
        public PacketCodec<RegistryByteBuf, MagicBenchRecipe> packetCodec() { return PACKET_CODEC; }
    }
}
```

### 3. Register Serializer

```java
public class ModRecipeSerializers {
    public static final RecipeSerializer<MagicBenchRecipe> MAGIC_BENCH =
        Registry.register(Registries.RECIPE_SERIALIZER,
            Identifier.of(ModNameMod.MOD_ID, "magic_bench"),
            new MagicBenchRecipe.Serializer());
}
```

### 4. JSON Format for Custom Recipe

```json
{
  "type": "modid:magic_bench",
  "input_a": { "item": "modid:magic_gem" },
  "input_b": { "item": "minecraft:stick" },
  "result": {
    "id": "modid:magic_wand",
    "count": 1
  }
}
```

## Recipe Naming Conventions

- Use descriptive names: `magic_sword_from_sticks`, `magic_ingot_from_smelting`
- Prefix smelting recipes: `<output>_from_smelting`, `<output>_from_blasting`
- Place recipe JSON in `data/<modid>/recipes/`

## Complete Recipe Checklist

1. [ ] Determine recipe type (shaped, shapeless, smelting, etc.)
2. [ ] Create JSON file at `data/<modid>/recipes/<name>.json`
3. [ ] Verify ingredient references (item or tag)
4. [ ] Verify result item ID and count
5. [ ] For custom types: register RecipeType and Serializer
6. [ ] For custom types: implement matching logic
7. [ ] Test in-game with `./gradlew runClient`

## Cross-References

- For project setup, use `mc-fabric-init`
- For items used in recipes, use `mc-fabric-item`
- For blocks used in recipes, use `mc-fabric-block`
- For data generation (auto-generate recipe JSON), use `mc-fabric-assets`

## Resources

### references/
- `recipe_types.md` - Complete reference for all vanilla recipe types, ingredient formats, recipe conditions, and custom recipe type implementation patterns
