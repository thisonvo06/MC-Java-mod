# Fabric Recipe Types Reference (MC 1.21.x)

## Vanilla Recipe Types

| Type ID | Description | Key Fields |
|---------|-------------|------------|
| `minecraft:crafting_shaped` | Grid pattern crafting | `pattern`, `key`, `result` |
| `minecraft:crafting_shapeless` | Any-position crafting | `ingredients`, `result` |
| `minecraft:crafting_transmute` | Transform item (keep nbt) | `input`, `material`, `result` |
| `minecraft:smelting` | Furnace cooking | `ingredient`, `result`, `experience`, `cookingtime` |
| `minecraft:blasting` | Blast furnace cooking | `ingredient`, `result`, `experience`, `cookingtime` |
| `minecraft:smoking` | Smoker cooking | `ingredient`, `result`, `experience`, `cookingtime` |
| `minecraft:campfire_cooking` | Campfire cooking | `ingredient`, `result`, `cookingtime` |
| `minecraft:stonecutting` | Stonecutter | `ingredient`, `result`, `count` |
| `minecraft:smithing_transform` | Equipment upgrade | `template`, `base`, `addition`, `result` |
| `minecraft:smithing_trim` | Armor trim | `template`, `base`, `addition` |
| `minecraft:decorated_pot` | Pot from sherds | `ingredients` (4 sherds) |

## Ingredient Formats

An `ingredient` field can be:

### Single Item
```json
{ "item": "modid:magic_gem" }
```

### Tag
```json
{ "tag": "minecraft:planks" }
```

### Multiple Items (OR)
```json
[
  { "item": "modid:magic_gem" },
  { "item": "modid:magic_crystal" }
]
```

### Item with Components (1.21.x)
```json
{
  "item": "minecraft:potion",
  "components": {
    "minecraft:potion_contents": {
      "potion": "minecraft:healing"
    }
  }
}
```

## Result Format (MC 1.21.x)

```json
{
  "id": "modid:magic_item",
  "count": 1,
  "components": {
    "minecraft:custom_name": '{"text":"Named Item"}'
  }
}
```

Note: In 1.21.x, the result field uses `"id"` instead of `"item"`, and supports `"components"` for NBT-equivalent data.

## Recipe Group (for recipe book)

Add a `"group"` field to categorize recipes in the recipe book:

```json
{
  "type": "minecraft:crafting_shaped",
  "group": "magic_door",
  "pattern": ["##", "##", "##"],
  "key": { "#": { "item": "modid:magic_planks" } },
  "result": { "id": "modid:magic_door", "count": 3 }
}
```

## Recipe Unlock (Advancement)

To show a recipe in the recipe book, create an advancement at:
`data/<modid>/advancements/recipes/<recipe_name>.json`

```json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_magic_ingot": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "items": [
          { "items": ["modid:magic_ingot"] }
        ]
      }
    }
  },
  "requirements": [["has_magic_ingot"]],
  "rewards": {
    "recipes": ["modid:magic_sword"]
  }
}
```

## Custom Recipe Input

For custom recipe types that use a custom crafting container:

```java
// Define a recipe input that wraps your inventory
public class MagicBenchInput implements RecipeInput {
    private final ItemStack slotA;
    private final ItemStack slotB;

    public MagicBenchInput(ItemStack slotA, ItemStack slotB) {
        this.slotA = slotA;
        this.slotB = slotB;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return switch (slot) {
            case 0 -> slotA;
            case 1 -> slotB;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int getSize() { return 2; }
}
```

## Recipe Manager Access

To get recipes of a specific type at runtime:

```java
// Server-side
world.getRecipeManager()
    .getFirstMatch(ModRecipeTypes.MAGIC_BENCH, input, world)
    .ifPresent(recipe -> {
        ItemStack result = recipe.value().craft(input, world.getRegistryManager());
        // Do something with result
    });

// List all recipes of a type
List<RecipeEntry<MagicBenchRecipe>> recipes = world.getRecipeManager()
    .listAllOfType(ModRecipeTypes.MAGIC_BENCH);
```

## Recipe Removal (Fabric)

To remove vanilla recipes:

```java
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;

// Or override with an empty recipe JSON at:
// data/minecraft/recipes/<recipe_name>.json
// with a result of air

// For proper removal, use a mixin to RecipeManager or
// use the Fabric API's RecipeManager registration
```
