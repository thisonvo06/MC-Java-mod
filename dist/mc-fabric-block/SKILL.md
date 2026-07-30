---
name: mc-fabric-block
description: "Minecraft Fabric custom block design for MC 1.21.x with Java. This skill should be used when creating custom blocks, configuring block properties (hardness, resistance, sounds, light level), block items, blockstate/model JSON files, loot tables, render layers, block entities, and custom block behaviors. Covers the full block creation pipeline from registration to rendering."
agent_created: true
---

# Minecraft Fabric Custom Block Design (MC 1.21.x / Java)

## Overview

This skill provides the workflow for creating custom blocks in a Fabric mod targeting MC 1.21.x. It covers block registration, property configuration, blockstate/model JSON, loot tables, render layers, and block entities.

## When to Use

- Creating a new custom block (decorative, functional, or interactive)
- Configuring block properties (hardness, resistance, sound, light emission)
- Creating blockstate and model JSON files
- Setting up block loot tables (drops)
- Configuring render layers (cutout, translucent) for blocks with transparency
- Creating blocks with BlockEntities (containers, tickable blocks)
- Implementing custom block behaviors (interaction, redstone, etc.)

## Block Registration

### Registration Pattern

Blocks require two registrations: the `Block` itself and a `BlockItem` (if obtainable as an item).

```java
package com.example.modid.registry;

import com.example.modid.ModNameMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    // Basic decorative block
    public static final Block MAGIC_BLOCK = register("magic_block",
        new Block(AbstractBlock.Settings.create()
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> 7)));

    // Ore block that drops experience
    public static final Block MAGIC_ORE = register("magic_ore",
        new ExperienceDroppingBlock(
            AbstractBlock.Settings.create()
                .strength(3.0f, 3.0f)
                .sounds(BlockSoundGroup.STONE)
                .requiresTool(),
            UniformIntProvider.create(2, 5)));  // XP drop range

    private static Block register(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(ModNameMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(ModNameMod.MOD_ID, name),
            new BlockItem(block, new Item.Settings()));
    }

    public static void initialize() {
        // Initialization is triggered by class loading
    }
}
```

Call `ModBlocks.initialize()` from `onInitialize()`.

## AbstractBlock.Settings Reference

```java
AbstractBlock.Settings.create()
    .strength(float hardness, float resistance)   // Combined hardness + resistance
    .strength(float hardness)                      // Hardness only, resistance = hardness
    .sounds(BlockSoundGroup group)                 // Sound type
    .luminance(state -> lightLevel)                // Light emission (0-15)
    .requiresTool()                                 // Must use correct tool to drop
    .nonOpaque()                                    // Not fully solid (no culling)
    .slipperiness(float value)                      // Friction (default 0.6, ice=0.98)
    .velocityMultiplier(float value)               // Speed multiplier on top (soul sand=0.4)
    .jumpVelocityMultiplier(float value)           // Jump height multiplier
    .ticksRandomly()                                // Random tick updates
    .dynamicBounds()                                // Variable collision shape
    .dropsNothing()                                 // No drops when broken
    .dropsLike(Block other)                         // Drop same as another block
    .air()                                          // Behaves like air
    .solidBlock(Predicate<BlockState> predicate)    // Custom solid check
    .suffocates(Predicate<BlockState> predicate)    // Custom suffocation check
    .blockVision(Predicate<BlockState> predicate)  // Blocks vision check
    .instrument(NoteBlockInstrument instrument)     // Note block sound
    .mapColor(MapColor color)                       // Map color
    .burnable()                                      // Can be set on fire
    .replaceable()                                   // Can be replaced (like grass)
    .liquid()                                        // Acts as a liquid
    .pistonBehavior(PistonBehavior behavior)        // NORMAL, DESTROY, BLOCK, PUSH_ONLY
```

### Common Sound Groups

| Sound Group | Example Block |
|-------------|--------------|
| `STONE` | Stone, Cobblestone |
| `WOOD` | Planks, Logs |
| `GRAVEL` | Dirt, Gravel |
| `GRASS` | Grass Block |
| `SAND` | Sand |
| `GLASS` | Glass |
| `METAL` | Iron Block |
| `NETHER_WART` | Netherrack |
| `BONE` | Bone Block |
| `AMETHYST` | Amethyst |
| `WET_GRASS` | Wet sponge |
| `HONEY` | Honey block |
| `SNOW` | Snow block |

## Blockstate JSON

Place at `src/main/resources/assets/<modid>/blockstates/<block_name>.json`:

### Single-Model Block (most common)

```json
{
  "variants": {
    "": {
      "model": "modid:block/magic_block"
    }
  }
}
```

### Multi-State Block (e.g., facing direction)

```json
{
  "variants": {
    "facing=north": { "model": "modid:block/magic_machine" },
    "facing=south": { "model": "modid:block/magic_machine", "y": 180 },
    "facing=east":  { "model": "modid:block/magic_machine", "y": 90 },
    "facing=west":  { "model": "modid:block/magic_machine", "y": 270 }
  }
}
```

### Block with Multiple States (multipart)

```json
{
  "multipart": [
    { "when": { "north": "low" }, "apply": { "model": "modid:block/magic_fence_side" } },
    { "when": { "east": "low" }, "apply": { "model": "modid:block/magic_fence_side", "y": 90 } },
    { "apply": { "model": "modid:block/magic_fence_post" } }
  ]
}
```

## Block Model JSON

Place at `src/main/resources/assets/<modid>/models/block/<model_name>.json`:

### Simple Cube Block

```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "modid:block/magic_block"
  }
}
```

### Pillar Block (logs, different top/side)

```json
{
  "parent": "block/cube_column",
  "textures": {
    "end": "modid:block/magic_log_top",
    "side": "modid:block/magic_log"
  }
}
```

### Cross Model (plants, flowers)

```json
{
  "parent": "block/cross",
  "textures": {
    "cross": "modid:block/magic_flower"
  }
}
```

### Stairs Model

```json
{
  "parent": "block/stairs",
  "textures": {
    "bottom": "modid:block/magic_block",
    "top": "modid:block/magic_block",
    "side": "modid:block/magic_block"
  }
}
```

## Item Model for Block

Also create the item model at `assets/<modid>/models/item/<block_name>.json`:

```json
{
  "parent": "modid:block/magic_block"
}
```

This makes the inventory icon use the block model.

## Loot Tables

Place at `src/main/resources/data/<modid>/loot_tables/blocks/<block_name>.json`:

### Self-Drop (default behavior)

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "minecraft:item", "name": "modid:magic_block" }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
```

### Ore with Fortune

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "minecraft:item", "name": "modid:magic_gem" }
      ],
      "conditions": [
        {
          "condition": "minecraft:match_tool",
          "predicate": {
            "tag": "minecraft:mineable/pickaxe"
          }
        }
      ],
      "functions": [
        {
          "function": "minecraft:apply_bonus",
          "enchantment": "minecraft:fortune",
          "formula": "minecraft:ore_drops"
        },
        {
          "function": "minecraft:explosion_decay"
        }
      ]
    }
  ]
}
```

### Silk Touch Alternative Drop

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:alternatives",
          "children": [
            {
              "type": "minecraft:item",
              "name": "modid:magic_ore",
              "conditions": [
                { "condition": "minecraft:match_tool", "predicate": { "enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}] } }
              ]
            },
            {
              "type": "minecraft:item",
              "name": "modid:magic_gem"
            }
          ]
        }
      ]
    }
  ]
}
```

## Render Layers

Blocks with transparency need render layer configuration. In the client initializer:

```java
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

@Override
public void onInitializeClient() {
    // Cutout: 1-bit transparency (plants, glass panes, doors)
    BlockRenderLayerMap.put(RenderLayer.getCutout(), ModBlocks.MAGIC_FLOWER);
    BlockRenderLayerMap.put(RenderLayer.getCutoutMipped(), ModBlocks.MAGIC_LEAVES);

    // Translucent: Partial transparency (water, stained glass, ice)
    BlockRenderLayerMap.put(RenderLayer.getTranslucent(), ModBlocks.MAGIC_GLASS);
}
```

## Custom Block with Behavior

```java
public class MagicMachineBlock extends Block implements BlockEntityProvider {
    public MagicMachineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MagicMachineBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MagicMachineBlockEntity machine) {
                player.openHandledScreen(machine);  // Open GUI
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MagicMachineBlockEntity machine) {
                ItemScatterer.scatter(world, pos, machine);  // Drop inventory contents
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
```

## Block with Properties (States)

```java
public class CustomLampBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    public CustomLampBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            world.setBlockState(pos, state.cycle(LIT));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public int luminance(BlockState state) {
        return state.get(LIT) ? 15 : 0;
    }
}
```

## Complete Block Creation Checklist

For each new block, ensure ALL of the following:

1. [ ] Register `Block` in `ModBlocks`
2. [ ] Register `BlockItem` (if obtainable in survival)
3. [ ] Call `ModBlocks.initialize()` from `onInitialize()`
4. [ ] Create blockstate JSON at `assets/<modid>/blockstates/<name>.json`
5. [ ] Create block model JSON at `assets/<modid>/models/block/<name>.json`
6. [ ] Create item model JSON at `assets/<modid>/models/item/<name>.json` (parent = block model)
7. [ ] Create texture PNG at `assets/<modid>/textures/block/<name>.png`
8. [ ] Create loot table at `data/<modid>/loot_tables/blocks/<name>.json`
9. [ ] Configure render layer in client init (if transparent)
10. [ ] Add to item group via `ItemGroupEvents`
11. [ ] Add language entries in `lang/en_us.json`
12. [ ] Test in-game with `./gradlew runClient`

## Cross-References

- For project setup, use `mc-fabric-init`
- For items, use `mc-fabric-item`
- For block entities with GUI, use `mc-fabric-gui`
- For ore world generation, use `mc-fabric-worldgen`
- For data generation (auto-generate models/loot tables), use `mc-fabric-assets`

## Resources

### references/
- `block_api.md` - Detailed API reference for Block, AbstractBlock.Settings, BlockState, properties, BlockEntity, and all related classes
