---
name: mc-fabric-item
description: "Minecraft Fabric custom item design for MC 1.21.x with Java. This skill should be used when creating custom items, food items, fuel items, tools, armor, item groups (creative tabs), item models, and item textures. Covers item registration, Item.Settings configuration, custom item behaviors (use/useOnBlock), and client-side model/texture setup."
agent_created: true
---

# Minecraft Fabric Custom Item Design (MC 1.21.x / Java)

## Overview

This skill provides the workflow for creating custom items in a Fabric mod targeting MC 1.21.x. It covers item registration, settings configuration, special item types (food, fuel, tools, armor), creative tab management, and model/texture setup.

## When to Use

- Creating a new custom item (basic or with special behaviors)
- Creating food items with hunger/saturation/effects
- Registering items as fuel sources
- Creating custom tools (pickaxe, sword, etc.) or armor
- Adding items to vanilla or custom creative tabs
- Setting up item model JSON and texture files
- Implementing custom item behaviors (right-click, use on block, etc.)

## Item Registration

### Basic Registration Pattern

Create a dedicated `ModItems` class to hold all item registrations:

```java
package com.example.modid.registry;

import com.example.modid.ModNameMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // Basic item
    public static final Item FIRE_WAND = register("fire_wand",
        new Item(new Item.Settings()));

    // Food item
    public static final Item MAGIC_BERRY = register("magic_berry",
        new Item(new Item.Settings()
            .food(new FoodComponent.Builder()
                .nutrition(6)
                .saturationModifier(0.8f)
                .alwaysEdible()
                .build())));

    // Fuel item (burns for 200 ticks = 10 items)
    public static final Item MAGIC_COAL = register("magic_coal",
        new Item(new Item.Settings()));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(ModNameMod.MOD_ID, name), item);
    }

    public static void initialize() {
        // Register fuel
        FuelRegistry.INSTANCE.add(MAGIC_COAL, 200);
    }
}
```

Call `ModItems.initialize()` from the main mod entry point's `onInitialize()`.

### Registration in onInitialize

```java
@Override
public void onInitialize() {
    ModItems.initialize();
}
```

## Item.Settings Reference

`Item.Settings` controls most item behavior. Chain methods as needed:

```java
new Item.Settings()
    .maxCount(16)                    // Max stack size (default 64)
    .maxDamage(256)                  // Durability (makes it damageable)
    .fireproof()                     // Immune to fire/lava
    .rarity(Rarity.EPIC)             // Rarity (COMMON, UNCOMMON, RARE, EPIC)
    .food(foodComponent)             // Makes it edible
    .useBlockPrefixedTranslationKey() // Use block name for item
    .recipeRemainder(Items.STICK)    // Item left after crafting
```

| Setting | Method | Default |
|---------|--------|---------|
| Stack size | `.maxCount(n)` | 64 |
| Durability | `.maxDamage(n)` | 0 (no durability) |
| Fireproof | `.fireproof()` | false |
| Rarity | `.rarity(Rarity.X)` | COMMON |
| Food | `.food(component)` | null |
| Fuel burn | `FuelRegistry.INSTANCE.add(item, ticks)` | 0 |
| Enchantability | `.enchantability(value)` | 0 |

## Special Item Types

### Food Items

```java
public static final FoodComponent MAGIC_FOOD = new FoodComponent.Builder()
    .nutrition(8)                    // Hunger restored (0-20)
    .saturationModifier(1.2f)        // Saturation multiplier
    .alwaysEdible()                  // Can eat at full hunger
    .meat()                          // Can feed to wolves
    .statusEffect(new StatusEffectInstance(
        StatusEffects.REGENERATION, 200, 1), 1.0f) // Effect + probability
    .build();

public static final Item MAGIC_STEW = register("magic_stew",
    new Item(new Item.Settings()
        .food(MAGIC_FOOD)
        .maxCount(1)
        .recipeRemainder(Items.BOWL)));
```

### Custom Item with Behavior

Extend `Item` and override methods:

```java
public class FireWandItem extends Item {
    public FireWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            // Spawn a fireball or effect
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            // Add custom logic here
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (!world.isClient) {
            // Set the target block on fire
            world.setBlockState(pos.offset(context.getSide()), Blocks.FIRE.getDefaultState());
            context.getStack().damage(1, context.getPlayer(), p -> p.sendToolBreakStatus(context.getHand()));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.modid.fire_wand.tooltip")
            .formatted(Formatting.GOLD));
    }
}
```

### Tool Items

Use vanilla `ToolMaterials` or create custom ones:

```java
public enum ModToolMaterials implements ToolMaterial {
    MAGIC(Materials.DIAMOND_ingraftIngredient(Items.DIAMOND), 2, 1024, 10.0f, 4.0f, 18);

    // MC 1.21.x ToolMaterial is an interface
    // Implement: incorrectBlocksForDrops(), durability(), speed(), attackDamage(), 
    //            miningLevel(), enchantmentValue(), repairIngredient()
}
```

In MC 1.21.x, `ToolMaterial` is an interface. Implement it:

```java
public class MagicToolMaterial implements ToolMaterial {
    public static final MagicToolMaterial INSTANCE = new MagicToolMaterial();

    @Override
    public int getDurability() { return 1024; }

    @Override
    public float getMiningSpeedMultiplier() { return 10.0f; }

    @Override
    public float getAttackDamage() { return 4.0f; }

    @Override
    public int getMiningLevel() { return 3; } // 0=wood, 1=stone, 2=iron, 3=diamond, 4=netherite

    @Override
    public int getEnchantability() { return 18; }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.DIAMOND);
    }
}

// Register tools
public static final Item MAGIC_SWORD = register("magic_sword",
    new SwordItem(MagicToolMaterial.INSTANCE, 5, -2.4f, new Item.Settings()));
public static final Item MAGIC_PICKAXE = register("magic_pickaxe",
    new PickaxeItem(MagicToolMaterial.INSTANCE, 3, -2.8f, new Item.Settings()));
```

### Armor Items

```java
public static final ArmorMaterial MAGIC_ARMOR_MATERIAL = new ArmorMaterial() {
    @Override
    public int getDurability(EquipmentType type) {
        return switch (type) {
            case HELMET -> 400;
            case CHESTPLATE -> 580;
            case LEGGINGS -> 540;
            case BOOTS -> 460;
            default -> 0;
        };
    }

    @Override
    public int getProtection(EquipmentType type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
            default -> 0;
        };
    }

    @Override
    public int getEnchantability() { return 18; }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.DIAMOND);
    }

    @Override
    public String getName() { return "magic"; }

    @Override
    public float getToughness() { return 3.0f; }

    @Override
    public float getKnockbackResistance() { return 0.1f; }
};

// Register armor pieces
public static final Item MAGIC_HELMET = register("magic_helmet",
    new ArmorItem(MAGIC_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings()));
public static final Item MAGIC_CHESTPLATE = register("magic_chestplate",
    new ArmorItem(MAGIC_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings()));
```

## Item Groups (Creative Tabs)

### Adding to Vanilla Groups

```java
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public static void initializeItemGroups() {
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
        .register(entries -> entries.add(ModItems.FIRE_WAND));
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
        .register(entries -> entries.add(ModItems.MAGIC_BERRY));
}
```

### Creating Custom Item Group

```java
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

public static final ItemGroup MAGIC_GROUP = Registry.register(
    Registries.ITEM_GROUP,
    Identifier.of(MOD_ID, "magic_group"),
    FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModItems.FIRE_WAND))
        .displayName(Text.translatable("itemgroup.modid.magic_group"))
        .entries((displayContext, entries) -> {
            entries.add(ModItems.FIRE_WAND);
            entries.add(ModItems.MAGIC_BERRY);
            entries.add(ModItems.MAGIC_COAL);
        })
        .build()
);
```

## Model and Texture Setup

### Item Model JSON

Place at `src/main/resources/assets/<modid>/models/item/<item_name>.json`:

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "modid:item/fire_wand"
  }
}
```

For items using a 2D handheld render (tools/swords), use parent `"item/handheld"`:

```json
{
  "parent": "item/handheld",
  "textures": {
    "layer0": "modid:item/magic_sword"
  }
}
```

### Texture File

Place PNG at `src/main/resources/assets/<modid>/textures/item/<item_name>.png`:
- Resolution: 16x16 pixels (standard), multiples of 16 supported
- Format: PNG with transparency
- File name must match the model texture path exactly

### Language Entries

Add to `src/main/resources/assets/<modid>/lang/en_us.json`:

```json
{
  "item.modid.fire_wand": "Fire Wand",
  "item.modid.fire_wand.tooltip": "Shoots fireballs when used",
  "item.modid.magic_berry": "Magic Berry",
  "item.modid.magic_coal": "Magic Coal",
  "itemgroup.modid.magic_group": "Magic Items"
}
```

## Complete Registration Checklist

For each new item, ensure ALL of the following are completed:

1. [ ] Register the `Item` in `ModItems` class
2. [ ] Call `ModItems.initialize()` from `onInitialize()`
3. [ ] Add fuel registration if the item is burnable
4. [ ] Add to an item group via `ItemGroupEvents`
5. [ ] Create model JSON at `assets/<modid>/models/item/<name>.json`
6. [ ] Create texture PNG at `assets/<modid>/textures/item/<name>.png`
7. [ ] Add language entry in `assets/<modid>/lang/en_us.json`
8. [ ] Test in-game with `./gradlew runClient`

## Cross-References

- For project setup, use `mc-fabric-init`
- For block items (blocks that can be held), use `mc-fabric-block`
- For recipes involving items, use `mc-fabric-recipe`
- For data generation (auto-generate models/lang), use `mc-fabric-assets`

## Resources

### references/
- `item_api.md` - Detailed API reference for Item, Item.Settings, FoodComponent, ToolMaterial, ArmorMaterial, and all related classes with full method signatures
