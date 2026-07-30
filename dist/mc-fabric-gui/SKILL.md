---
name: mc-fabric-gui
description: "Minecraft Fabric GUI and container inventory design for MC 1.21.x with Java. This skill should be used when creating custom GUIs with inventory containers, ScreenHandler (server-side slot management and sync), HandledScreen (client-side rendering), block entity inventories, network packets for button actions, and opening GUIs from blocks or items. Covers the full client-server GUI synchronization pipeline."
agent_created: true
---

# Minecraft Fabric GUI & Container Design (MC 1.21.x / Java)

## Overview

This skill provides the workflow for creating custom GUIs with inventory containers in a Fabric mod targeting MC 1.21.x. It covers the full pipeline: block entity inventory, ScreenHandler (server), HandledScreen (client), slot management, and network synchronization.

## When to Use

- Creating a custom container GUI (chest-like, furnace-like, custom layout)
- Adding inventory slots to a block entity
- Rendering custom GUI backgrounds with textures
- Handling button clicks in GUIs (crafting buttons, mode toggles)
- Synchronizing data between server and client in GUIs
- Opening a GUI from a block right-click or item use

## Architecture Overview

A GUI consists of three components working together:

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  BlockEntity    │────▶│  ScreenHandler   │◀───▶│  HandledScreen  │
│  (inventory)    │     │  (server-side)   │     │  (client-side)  │
│  holds items    │     │  manages slots   │     │  renders GUI    │
└─────────────────┘     │  syncs data      │     │  handles input  │
                        └──────────────────┘     └─────────────────┘
```

## Step 1: Block Entity with Inventory

```java
package com.example.modid.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import com.example.modid.ModNameMod;
import com.example.modid.screen.MagicBenchScreenHandler;

public class MagicBenchBlockEntity extends BlockEntity
        implements NamedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);

    public MagicBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_BENCH, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.modid.magic_bench");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MagicBenchScreenHandler(syncId, playerInventory, this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
    }
}
```

### ImplementedInventory Interface

```java
package com.example.modid.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import java.util.List;

public interface ImplementedInventory extends SidedInventory {
    DefaultedList<ItemStack> getItems();

    @Override
    default int size() { return getItems().size(); }

    @Override
    default boolean isEmpty() {
        for (ItemStack stack : getItems()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) { return getItems().get(slot); }

    @Override
    default ItemStack removeStack(int slot, int count) {
        return Inventories.splitStack(getItems(), slot, count);
    }

    @Override
    default ItemStack removeStack(int slot) {
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    default void clear() { getItems().clear(); }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    default int[] getAvailableSlots(Direction side) {
        int[] result = new int[getItems().size()];
        for (int i = 0; i < result.length; i++) result[i] = i;
        return result;
    }

    @Override
    default boolean canInsert(int slot, ItemStack stack, Direction dir) { return true; }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction dir) { return true; }
}
```

## Step 2: ScreenHandler (Server-Side)

```java
package com.example.modid.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import com.example.modid.block.entity.MagicBenchBlockEntity;

public class MagicBenchScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    // Server-side constructor (from BlockEntity)
    public MagicBenchScreenHandler(int syncId, PlayerInventory playerInventory, MagicBenchBlockEntity blockEntity) {
        super(ModScreenHandlers.MAGIC_BENCH, syncId);
        this.inventory = blockEntity;

        // Add block entity slots (custom layout)
        this.addSlot(new Slot(inventory, 0, 30, 17));   // Input A
        this.addSlot(new Slot(inventory, 1, 68, 17));   // Input B
        this.addSlot(new Slot(inventory, 2, 124, 35));   // Output (use custom Slot for no-insert)

        // Add player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                    col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // Client-side constructor (from registry)
    public MagicBenchScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack stackInSlot = slot.getStack();
            originalStack = stackInSlot.copy();

            // Move from block inventory to player
            if (slotIndex < 3) {
                if (!this.insertItem(stackInSlot, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Move from player to block inventory
            else if (!this.insertItem(stackInSlot, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return originalStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
```

## Step 3: Register ScreenHandlerType

```java
package com.example.modid.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import com.example.modid.ModNameMod;

public class ModScreenHandlers {
    public static final ScreenHandlerType<MagicBenchScreenHandler> MAGIC_BENCH =
        Registry.register(Registries.SCREEN_HANDLER,
            Identifier.of(ModNameMod.MOD_ID, "magic_bench"),
            new ScreenHandlerType<>(MagicBenchScreenHandler::new));

    public static void initialize() {}
}
```

For ScreenHandlers that need extra data on open (like BlockPos), use `ExtendedScreenHandlerType`:

```java
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;

public static final ScreenHandlerType<MagicBenchScreenHandler> MAGIC_BENCH =
    Registry.register(Registries.SCREEN_HANDLER,
        Identifier.of(MOD_ID, "magic_bench"),
        new ExtendedScreenHandlerType<>(MagicBenchScreenHandler::new, BlockPos.PACKET_CODEC));
```

## Step 4: HandledScreen (Client-Side)

```java
package com.example.modid.client.screen;

import com.example.modid.ModNameMod;
import com.example.modid.screen.MagicBenchScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MagicBenchScreen extends HandledScreen<MagicBenchScreenHandler> {
    private static final Identifier TEXTURE =
        Identifier.of(ModNameMod.MOD_ID, "textures/gui/magic_bench.png");

    public MagicBenchScreen(MagicBenchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Draw the GUI background texture
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
```

## Step 5: Register Screen (Client)

```java
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

@Override
public void onInitializeClient() {
    ScreenRegistry.register(ModScreenHandlers.MAGIC_BENCH, MagicBenchScreen::new);
}
```

## Step 6: Opening the GUI

### From Block Right-Click

```java
@Override
public ActionResult onUse(BlockState state, World world, BlockPos pos,
                          PlayerEntity player, Hand hand, BlockHitResult hit) {
    if (!world.isClient) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MagicBenchBlockEntity bench) {
            player.openHandledScreen(bench);
        }
    }
    return ActionResult.SUCCESS;
}
```

### From Item Use

```java
@Override
public ActionResult use(World world, PlayerEntity player, Hand hand) {
    if (!world.isClient) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, inv, p) -> new MagicBenchScreenHandler(syncId, inv),
            Text.translatable("container.modid.magic_bench")
        ));
    }
    return ActionResult.SUCCESS;
}
```

## GUI Texture Setup

Create a PNG texture at `assets/<modid>/textures/gui/<name>.png`:

- Size: 176x166 pixels (standard GUI)
- Slots: 18x18 pixels each (item is 16x16, with 1px border)
- Background: semi-transparent dark area (or use vanilla style)
- Player inventory area: starts at y=84 (relative to GUI top-left)
- Title area: top center
- Arrow/progress indicators: can be overlaid with `context.drawTexture()`

### Drawing Progress Indicators

```java
@Override
protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
    context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

    // Draw progress arrow (animated)
    int progress = this.handler.getProgress(); // Custom sync data
    if (progress > 0) {
        context.drawTexture(TEXTURE,
            this.x + 79, this.y + 35,        // Position on screen
            176, 0,                           // UV in texture (progress bar area)
            progress, 16);                    // Width = progress, height = 16
    }
}
```

## Network Synchronization

### Sync Custom Data (PropertyDelegate)

For simple integer sync (progress, fuel, etc.):

```java
// In ScreenHandler
private final PropertyDelegate propertyDelegate;

public MagicBenchScreenHandler(int syncId, PlayerInventory inv, MagicBenchBlockEntity entity) {
    super(ModScreenHandlers.MAGIC_BENCH, syncId);
    this.propertyDelegate = entity.getPropertyDelegate();
    this.addProperties(propertyDelegate);  // Auto-syncs to client
}

// Client constructor
public MagicBenchScreenHandler(int syncId, PlayerInventory inv) {
    this(syncId, inv, new SimpleInventory(3));
}

private MagicBenchScreenHandler(int syncId, PlayerInventory inv, Inventory inventory) {
    super(ModScreenHandlers.MAGIC_BENCH, syncId);
    this.inventory = inventory;
    this.propertyDelegate = new ArrayPropertyDelegate(2); // 2 synced ints
    this.addProperties(propertyDelegate);
    // ... add slots
}

// Access synced data on client
public int getProgress() { return propertyDelegate.get(0); }
public int getMaxProgress() { return propertyDelegate.get(1); }
```

### Custom Slot (Output-Only)

```java
public class OutputSlot extends Slot {
    public OutputSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;  // Cannot place items into output slot
    }
}
```

## Complete GUI Checklist

1. [ ] Create BlockEntity implementing `NamedScreenHandlerFactory` + inventory
2. [ ] Implement `ImplementedInventory` interface (or use `SimpleInventory`)
3. [ ] Create `ScreenHandler` subclass with slot layout and `quickMove`
4. [ ] Register `ScreenHandlerType`
5. [ ] Create `HandledScreen` subclass for client rendering
6. [ ] Register screen with `ScreenRegistry`
7. [ ] Open GUI from block/item interaction
8. [ ] Create GUI texture PNG (176x166 or custom)
9. [ ] Add language entries for container name
10. [ ] Handle block break (drop inventory contents via `ItemScatterer`)
11. [ ] Test in-game with `./gradlew runClient`

## Cross-References

- For project setup, use `mc-fabric-init`
- For blocks that open GUIs, use `mc-fabric-block`
- For custom recipe types in GUIs, use `mc-fabric-recipe`
- For GUI texture creation guidelines, use `mc-fabric-assets`

## Resources

### references/
- `gui_api.md` - Detailed API reference for ScreenHandler, HandledScreen, Slot, PropertyDelegate, ExtendedScreenHandlerType, and custom network packets for GUI button actions
