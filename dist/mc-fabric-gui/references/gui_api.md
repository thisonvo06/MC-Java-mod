# Fabric GUI API Reference (MC 1.21.x)

## ScreenHandler

### Key Methods
```java
// Add slots
Slot addSlot(Slot slot)

// Add synced properties (ints)
void addProperty(PropertyDelegate propertyDelegate)
void addProperties(PropertyDelegate propertyDelegate)
void addProperty(Property property)

// Item transfer (shift-click)
ItemStack quickMove(PlayerEntity player, int slotIndex)

// Access check
boolean canUse(PlayerEntity player)

// Called when container is closed
void onClosed(PlayerEntity player)

// Called when slot content changes
void onContentChanged(Inventory inventory)

// Send state to client (called when slots change)
void sendContentUpdates()

// Slot count
int getRevision()
```

### Slot Subclasses

| Class | Purpose |
|-------|---------|
| `Slot` | Basic slot |
| `Slot.Output` | Output-only (cannot insert) |
| `Slot.FurnaceOutput` | Furnace output (gives XP) |
| `Slot.FurnaceFuel` | Fuel slot (shows burn time) |
| `CraftingResultSlot` | Crafting table output |
| `PlayerInvSlot` | Player inventory slot |

### Custom Slot Example
```java
public class FilteredSlot extends Slot {
    private final Predicate<ItemStack> filter;

    public FilteredSlot(Inventory inventory, int index, int x, int y, Predicate<ItemStack> filter) {
        super(inventory, index, x, y);
        this.filter = filter;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return filter.test(stack);
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return super.getMaxItemCount(stack);
    }

    @Override
    public ItemStack takeStack(int amount) {
        return super.takeStack(amount);
    }
}
```

## Standard Slot Positions

### Player Inventory (3x9 + 1x9)
```
Row 0: x=8+0*18, y=84   (slots 9-17)
Row 1: x=8+0*18, y=102  (slots 18-26)
Row 2: x=8+0*18, y=120  (slots 27-35)
Hotbar: x=8+0*18, y=142 (slots 0-8)
```

### Crafting Table Layout
```
Crafting grid: (30,17) to (66,53), 3x3 slots
Result: (124,35)
Player inv: standard positions
```

### Furnace Layout
```
Input: (56,17)
Fuel: (56,53)
Output: (116,35)
Player inv: standard positions
```

## HandledScreen

### Key Methods
```java
// Background rendering (behind slots)
void drawBackground(DrawContext context, float delta, int mouseX, int mouseY)

// Foreground rendering (labels, slot highlights)
void drawForeground(DrawContext context, int mouseX, int mouseY)

// Main render (calls background, slots, foreground, tooltips)
void render(DrawContext context, int mouseX, int mouseY, float delta)

// Mouse click handling
boolean mouseClicked(double mouseX, double mouseY, int button)

// Mouse drag handling
boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)

// Key press
boolean keyPressed(int keyCode, int scanCode, int modifiers)

// Mouse scroll
boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
```

### DrawContext Methods

```java
// Draw texture
context.drawTexture(Identifier texture, int x, int y, int u, int v, int width, int height)
context.drawTexture(Identifier texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight)

// Draw text
context.drawText(textRenderer, Text text, int x, int y, int color, boolean shadow)
context.drawText(textRenderer, String text, int x, int y, int color, boolean shadow)

// Draw item
context.drawItem(ItemStack stack, int x, int y)
context.drawItemWithoutEntity(ItemStack stack, int x, int y)

// Draw item overlay (count, durability bar)
context.drawItemInSlot(textRenderer, ItemStack stack, int x, int y)

// Draw tooltip
context.drawTooltip(textRenderer, Text text, int x, int y)
context.drawItemTooltip(textRenderer, ItemStack stack, int x, int y)

// Fill rectangle
context.fill(int x1, int y1, int x2, int y2, int color)

// Draw nine-slice (scalable bordered box)
context.drawNineSlicedTexture(Identifier texture, int x, int y, int width, int height, int leftSlice, int topSlice, int rightSlice, int bottomSlice, int centerSliceWidth, int centerSliceHeight, int u, int v)
```

## ExtendedScreenHandlerType (Extra Data)

For passing extra data when opening a GUI (e.g., BlockPos):

### Server-side
```java
// ScreenHandlerType registration
public static final ScreenHandlerType<MyHandler> MY_HANDLER =
    Registry.register(Registries.SCREEN_HANDLER,
        Identifier.of(MOD_ID, "my_gui"),
        new ExtendedScreenHandlerType<>(MyHandler::new, BlockPos.PACKET_CODEC));

// ScreenHandler constructor receives extra data
public MyHandler(int syncId, PlayerInventory inv, BlockPos pos) {
    super(ModScreenHandlers.MY_HANDLER, syncId);
    // Use pos to get BlockEntity
    BlockEntity be = inv.player.getWorld().getBlockEntity(pos);
    // ...
}

// Opening with extra data
player.openHandledScreen(new ExtendedScreenHandlerFactory() {
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.modid.my_gui");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new MyHandler(syncId, inv, pos);
    }
});
```

### Client-side
```java
// Client constructor must match the extra data codec
public MyHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
    this(syncId, inv, buf.readBlockPos());
}
```

## Custom Network Packets (GUI Actions)

For button clicks that need server-side processing:

### Register Payload (1.21.x)
```java
public record MagicButtonPayload(int buttonId) implements CustomPayload {
    public static final CustomPayload.Id<MagicButtonPayload> ID =
        new CustomPayload.Id<>(Identifier.of(MOD_ID, "magic_button"));
    public static final PacketCodec<RegistryByteBuf, MagicButtonPayload> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, MagicButtonPayload::buttonId,
            MagicButtonPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
```

### Register Handler
```java
// Server-side receiver
ServerPlayNetworking.registerGlobalReceiver(MagicButtonPayload.ID, (payload, context) -> {
    context.player().server.execute(() -> {
        // Handle button action on server thread
        if (context.player().currentScreenHandler instanceof MagicBenchScreenHandler handler) {
            handler.onButtonClicked(payload.buttonId());
        }
    });
});
```

### Send from Client
```java
@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (isInButton(mouseX, mouseY)) {
        ClientPlayNetworking.send(new MagicButtonPayload(0));
        return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
}
```

## PropertyDelegate

### ArrayPropertyDelegate
```java
// Create with N synced integers
PropertyDelegate delegate = new ArrayPropertyDelegate(3); // 3 values

// Set values (server)
delegate.set(0, progress);    // index 0 = progress
delegate.set(1, maxProgress); // index 1 = max progress
delegate.set(2, fuelTicks);   // index 2 = fuel

// Read values (client)
int progress = delegate.get(0);
```

### Custom PropertyDelegate
```java
public class MagicBenchPropertyDelegate implements PropertyDelegate {
    private int progress = 0;
    private int fuel = 0;

    @Override public int get(int index) {
        return switch (index) {
            case 0 -> progress;
            case 1 -> fuel;
            default -> 0;
        };
    }

    @Override public void set(int index, int value) {
        switch (index) {
            case 0 -> progress = value;
            case 1 -> fuel = value;
        }
    }

    @Override public int size() { return 2; }
}
```
