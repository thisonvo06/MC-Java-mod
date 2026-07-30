# Fabric Block API Reference (MC 1.21.x)

## Core Classes

### Block
Base class for all blocks. Extend to create custom behavior.

**Key overridable methods:**
```java
// Player right-click interaction
ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit)

// Block is broken by player
void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player)

// Block is removed/replaced
void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved)

// Redstone power output
int getWeakRedstonePower(BlockState state, WorldView world, BlockPos pos, Direction direction)
int getStrongRedstonePower(BlockState state, WorldView world, BlockPos pos, Direction direction)

// Whether block emits redstone
boolean emitsRedstonePower(BlockState state)

// Light level
int luminance(BlockState state)  // Note: use settings.luminance() in 1.21.x for static values

// Neighbor update (when adjacent block changes)
void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify)

// Random tick
void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random)

// Scheduled tick
void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random)

// Entity steps on block
void stepOn(World world, BlockPos pos, BlockState state, Entity entity)

// Entity falls on block
void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance)

// Can entity spawn on
boolean canMobSpawnInside(BlockState state)

// Block opacity (for light/sight blocking)
boolean isOpaque(BlockState state)

// Collision shape (hitbox)
VoxelShape getCollisionShape(BlockState state, WorldView world, BlockPos pos, ShapeContext context)

// Outline shape (visual wireframe)
VoxelShape getOutlineShape(BlockState state, WorldView world, BlockPos pos, ShapeContext context)

// Raycast shape
VoxelShape getRaycastShape(BlockState state, WorldView world, BlockPos pos)

// Render type
BlockRenderType getRenderType(BlockState state)  // MODEL, INVISIBLE, ENTITYBLOCK_ANIMATED

// Drop experience
void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool)
```

### AbstractBlock.Settings

```java
AbstractBlock.Settings.create()
    .strength(float hardness, float resistance)
    .strength(float combined)                    // Sets both to same value
    .hardness(float hardness)                     // Mining time factor
    .resistance(float resistance)                 // Explosion resistance
    .sounds(BlockSoundGroup group)
    .luminance(ToIntFunction<BlockState> function) // Dynamic light based on state
    .lightLevel(ToIntFunction<BlockState> function) // Alias in some mappings
    .requiresTool()                                // Needs correct tool tier to drop
    .drops(BlockLootTableProvider provider)
    .dropsNothing()
    .dropsLike(Block other)
    .nonOpaque()                                   // Allows light/sight through
    .slipperiness(float value)                     // Default 0.6
    .velocityMultiplier(float value)               // Movement speed multiplier
    .jumpVelocityMultiplier(float value)           // Jump height multiplier
    .ticksRandomly()                               // Enables randomTick()
    .dynamicBounds()                               // Variable collision shape
    .air()                                         // Treat as air
    .replaceable()                                 // Can be replaced (e.g., grass)
    .burnable()                                    // Flamable
    .liquid()                                      // Liquid behavior
    .solid()                                        // Fully solid (default)
    .forceSolid()                                  // Force solid rendering
    .instrument(NoteBlockInstrument instrument)    // Note block sound
    .mapColor(MapColor color)                      // Map appearance
    .pistonBehavior(PistonBehavior behavior)       // NORMAL, DESTROY, BLOCK, PUSH_ONLY
    .offsetType(AbstractBlock.OffsetType type)     // NONE, XZ, XYZ (for plant rendering)
    .collisionShape(VoxelShape shape)
    .suffocates(Predicate<BlockState> predicate)
    .blockVision(Predicate<BlockState> predicate)
    .solidBlock(Predicate<BlockState> predicate)
    .emitsRedstonePower(Predicate<BlockState> predicate)
```

### BlockState

Immutable state of a block at a position. Contains property values.

```java
// Get property value
state.get(BooleanProperty / IntegerProperty / EnumProperty)

// Get with new value
BlockState newState = state.with(LIT, true);

// Cycle to next value
BlockState newState = state.cycle(LIT);

// Check property value
state.isOf(ModBlocks.MAGIC_BLOCK)
```

### Properties

```java
// Boolean (on/off, lit/unlit)
public static final BooleanProperty LIT = BooleanProperty.of("lit");

// Integer (0-15 for rotation, 0-3 for age)
public static final IntProperty AGE = IntProperty.of("age", 0, 3);

// Direction
public static final DirectionProperty FACING = Properties.FACING; // or Properties.HORIZONTAL_FACING

// Enum
public static final EnumProperty<Half> HALF = Properties.HALF; // TOP, BOTTOM
public static final EnumProperty<Axis> AXIS = Properties.AXIS;  // X, Y, Z

// Register properties
@Override
protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(LIT, FACING);
}
```

### Vanilla Block Subclasses

| Class | Purpose |
|-------|---------|
| `Block` | Basic block |
| `FallingBlock` | Falls like sand when unsupported |
| `ExperienceDroppingBlock` | Drops XP when mined |
| `OreBlock` | Standard ore with XP and fortune |
| `PlantBlock` | Flowers, crops (needs cutout render layer) |
| `CropBlock` | Growable crop with AGE property |
| `StemBlock` | Pumpkin/melon stem |
| `SlabBlock` | Half slabs (top, bottom, double) |
| `StairsBlock` | Stair variants |
| `FenceBlock` | Connectable fence |
| `FenceGateBlock` | Openable fence gate |
| `WallBlock` | Connectable wall |
| `DoorBlock` | 2-tall door |
| `TrapdoorBlock` | Trapdoor |
| `ButtonBlock` | Redstone button |
| `PressurePlateBlock` | Redstone pressure plate |
| `LeverBlock` | Redstone lever |
| `TorchBlock` | Wall/floor torch |
| `SignBlock` | Standing/wall sign |
| `BedBlock` | 2-block bed |
| `AnvilBlock` | Falling anvil with facing |
| `PaneBlock` | Glass pane / iron bars |
| `FluidBlock` | Liquid source/flowing |

### BlockEntityProvider

Implement this interface on a Block to give it a BlockEntity:

```java
public class MyBlock extends Block implements BlockEntityProvider {
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MyBlockEntity(pos, state);
    }
}
```

### RenderLayer Types

| Layer | Use Case | Method |
|-------|----------|--------|
| `Solid` | Opaque blocks (default) | `RenderLayer.getSolid()` |
| `CutoutMipped` | Leaves (mipmapped cutout) | `RenderLayer.getCutoutMipped()` |
| `Cutout` | Plants, doors, glass panes | `RenderLayer.getCutout()` |
| `Translucent` | Water, stained glass, ice | `RenderLayer.getTranslucent()` |
| `Tripwire` | Tripwire | `RenderLayer.getTripwire()` |

## Tag Registration

Register block tags for tool harvesting:

```java
// In a DatagenProvider or via JSON at data/minecraft/tags/blocks/mineable/pickaxe.json
{
  "replace": false,
  "values": ["modid:magic_ore", "modid:magic_block"]
}

// Also needs tool level tag (optional):
// data/minecraft/tags/blocks/needs_iron_tool.json
{
  "replace": false,
  "values": ["modid:magic_ore"]
}
```
