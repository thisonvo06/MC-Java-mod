# Fabric Item API Reference (MC 1.21.x)

## Core Classes

### Item
Base class for all items. Extend to create custom behavior.

**Key overridable methods:**
```java
// Called when player right-clicks with item in hand
ActionResult use(World world, PlayerEntity user, Hand hand)

// Called when player right-clicks a block with the item
ActionResult useOnBlock(ItemUsageContext context)

// Called each tick while item is in use (eating, drinking, bow drawing)
void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks)

// Called when player finishes using item (after use duration)
ItemStack finishUsing(ItemStack stack, World world, LivingEntity user)

// Maximum use duration in ticks
int getMaxUseTime(ItemStack stack, LivingEntity user)

// Use animation type
UseAction getUseAction(ItemStack stack)  // NONE, EAT, DRINK, BLOCK, BOW, CROSSBOW, SPEAR, BRUSH

// Called when player stops using before completion
void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks)

// Mining speed multiplier for specific blocks
float getMiningSpeed(ItemStack stack, BlockState state)

// Whether this item can mine a block (drops)
boolean isCorrectForDrops(ItemStack stack, BlockState state)

// Tooltip lines
void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type)

// Called when crafted (e.g., return empty bottle for honey bottles)
void onCraft(ItemStack stack, World world, PlayerEntity player)

// Enchantability value
int getEnchantability()

// Whether item can be enchanted with this enchantment in anvil
boolean isEnchantable(ItemStack stack)

// Default equipment slot for right-click equipping (armor)
EquipmentSlot getSlotType()

// Whether pressing attack triggers item special attack
boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner)

// Break speed penalty in water
float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource damageSource)
```

### Item.Settings (Item.Settings in Yarn)

Factory for configuring item properties.

```java
new Item.Settings()
    .maxCount(int maxCount)                         // Max stack size (1-99)
    .maxDamage(int maxDamage)                        // Makes item damageable, sets durability
    .maxDamageIf(int maxDamage, Predicate<ItemStack> condition)
    .recipeRemainder(Item remainder)                 // Item left in crafting grid
    .rarity(Rarity rarity)                           // COMMON, UNCOMMON, RARE, EPIC
    .fireproof()                                      // Cannot burn in lava/fire
    .food(FoodComponent food)                        // Makes item edible
    .useBlockPrefixedTranslationKey()                // Uses "block." prefix in lang
    .enchantability(int value)                        // Enchantability level
    .equipOnUse()                                     // Right-click to equip (armor)
    .useItemPrefixedTranslationKey()
    .component(DataComponentType<T> type, T value)   // Set data component (1.21+)
```

### FoodComponent

```java
new FoodComponent.Builder()
    .nutrition(int hunger)                           // 0-20 hunger points
    .saturationModifier(float modifier)              // Saturation = nutrition * modifier
    .alwaysEdible()                                  // Can eat at full hunger
    .meat()                                           // Wolves will eat it
    .statusEffect(StatusEffectInstance effect, float chance) // 0.0-1.0 probability
    .build()
```

### ToolMaterial (Interface in 1.21.x)

```java
public interface ToolMaterial {
    TagKey<Block> getIncorrectBlocksForDrops();  // Blocks this tool can't harvest
    int getDurability();
    float getMiningSpeedMultiplier();
    float getAttackDamage();       // Base attack damage bonus
    int getMiningLevel();          // 0=wood/gold, 1=stone, 2=iron, 3=diamond, 4=netherite
    int getEnchantability();
    Ingredient getRepairIngredient();
}
```

### ArmorMaterial (Interface in 1.21.x)

```java
public interface ArmorMaterial {
    int getDurability(EquipmentType type);        // Per-slot durability
    int getProtection(EquipmentType type);        // Per-slot armor points
    int getEnchantability();
    SoundEvent getEquipSound();
    Ingredient getRepairIngredient();
    String getName();                               // Used in texture path: models/armor/<name>_layer_1.png
    float getToughness();
    float getKnockbackResistance();
}
```

### EquipmentType Enum (1.21.x)

Replaces `ArmorItem.Type`:
- `HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS`
- `BODY` (for wolf/horse armor)
- `SADDLE`

## Registration APIs

### Registry.register

```java
// Standard item registration
Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "name"), item);

// Item group registration
Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "name"), itemGroup);
```

### FuelRegistry

```java
import net.fabricmc.fabric.api.registry.FuelRegistry;

FuelRegistry.INSTANCE.add(item, burnTimeTicks);  // 200 ticks = 1 smelting operation
FuelRegistry.INSTANCE.remove(item);
```

### ItemGroupEvents

```java
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

// Add to existing vanilla group
ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
    entries.add(item);
    entries.addAfter(Items.DIAMOND_SWORD, item);  // Place after specific item
    entries.prepend(item);                          // Add to beginning
});

// Custom group
FabricItemGroup.builder()
    .icon(() -> new ItemStack(item))
    .displayName(Text.translatable("key"))
    .entries((displayContext, entries) -> { ... })
    .build();
```

## Vanilla Tool/Weapon Item Classes

| Class | Constructor |
|-------|-------------|
| `SwordItem` | `new SwordItem(ToolMaterial, attackDamage, attackSpeed, Settings)` |
| `PickaxeItem` | `new PickaxeItem(ToolMaterial, attackDamage, attackSpeed, Settings)` |
| `AxeItem` | `new AxeItem(ToolMaterial, attackDamage, attackSpeed, Settings)` |
| `ShovelItem` | `new ShovelItem(ToolMaterial, attackDamage, attackSpeed, Settings)` |
| `HoeItem` | `new HoeItem(ToolMaterial, attackDamage, attackSpeed, Settings)` |
| `ArmorItem` | `new ArmorItem(ArmorMaterial, EquipmentType, Settings)` |
| `MiningToolItem` | Custom mining tool with specific block effectiveness |

Attack speed: lower = slower. Reference values: sword=-2.4, pickaxe=-2.8, axe=-3.0, shovel=-3.0, hoe=0 to -3.0.

## Model JSON Formats

### Standard Item
```json
{
  "parent": "item/generated",
  "textures": { "layer0": "modid:item/name" }
}
```

### Handheld (Tools/Weapons)
```json
{
  "parent": "item/handheld",
  "textures": { "layer0": "modid:item/name" }
}
```

### With Overrides (e.g., bow pulling state)
```json
{
  "parent": "item/generated",
  "textures": { "layer0": "modid:item/name" },
  "overrides": [
    { "predicate": { "custom_predicate": 1 }, "model": "modid:item/name_variant" }
  ]
}
```

## Data Components (MC 1.21.x)

MC 1.21 replaced NBT tags with data components. Access via `DataComponentTypes`:

```java
// Get component value
OptionalInt damage = stack.getOrDefault(DataComponentTypes.DAMAGE, 0);

// Set component value
stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Custom Name"));

// Check if component present
boolean hasEnchantments = stack.contains(DataComponentTypes.ENCHANTMENTS);

// Custom data component (requires registration)
public static final DataComponentType<Integer> CHARGE = Registry.register(
    Registries.DATA_COMPONENT_TYPE,
    Identifier.of(MOD_ID, "charge"),
    new DataComponentType.Builder<Integer>()
        .codec(Codec.intRange(0, 100))
        .packetCodec(PacketCodecs.VAR_INT)
        .build()
);

// Usage
stack.set(ModComponents.CHARGE, 50);
int charge = stack.getOrDefault(ModComponents.CHARGE, 0);
```
