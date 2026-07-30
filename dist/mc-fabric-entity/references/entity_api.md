# Fabric Entity API Reference (MC 1.21.x)

## Entity Type Hierarchy

```
Entity
├── LivingEntity
│   ├── MobEntity (has AI, can despawn)
│   │   ├── AmbientEntity (bats)
│   │   ├── PathAwareEntity (can pathfind)
│   │   │   ├── HostileEntity (zombies, skeletons)
│   │   │   ├── PassiveEntity (animals, villagers)
│   │   │   │   ├── AnimalEntity (can breed)
│   │   │   │   │   ├── TameableEntity (can be tamed)
│   │   │   │   │   ├── HorseEntity
│   │   │   │   │   └── ...
│   │   │   │   ├── FishEntity
│   │   │   │   └── ...
│   │   ├── WaterMobEntity (squid)
│   │   └── ...
│   ├── ArmorStandEntity
│   └── PlayerEntity
├── VehicleEntity (minecarts, boats)
├── ProjectileEntity (arrows, fireballs)
│   ├── PersistentProjectileEntity
│   ├── FireballEntity
│   ├── SnowballEntity
│   └── ...
├── HangingEntity (paintings, item frames)
├── ItemEntity (dropped items)
├── ExperienceOrbEntity
├── TntEntity
└── ...
```

## Key Entity Methods

### Lifecycle Methods
```java
void tick()                          // Called every game tick
void onSpawn()                       // Called when entity spawns
void onRemoved()                     // Called when entity is removed
void writeCustomDataToNbt(NbtCompound nbt)   // Save custom data
void readCustomDataFromNbt(NbtCompound nbt)  // Load custom data
```

### Interaction Methods
```java
boolean interact(PlayerEntity player, Hand hand)  // Player right-clicks entity
ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand)
boolean isAttackable()                              // Can be attacked
boolean damage(DamageSource source, float amount)   // Takes damage
void onDeath(DamageSource source)                    // Killed
boolean cannotDespawn()                              // Prevents despawning
boolean isImmuneTo(DamageSource source)              // Damage immunity
```

### Movement Methods
```java
void travel(Vec3d movementInput)                    // Movement physics
void tickMovement()                                 // Movement + AI ticking
boolean canWalkOnFluid(FluidState state)            // Jesus mode
boolean shouldSwimInFluids()                        // Fluid swimming
Vec3d getPos()
void setPosition(double x, double y, double z)
void setVelocity(Vec3d velocity)
```

## Goal System

### Goal Interface
```java
public interface Goal {
    boolean canStart();        // Can this goal begin?
    boolean shouldContinue();  // Should it keep running?
    void start();              // Called when goal starts
    void stop();               // Called when goal stops
    void tick();               // Called each tick while active
    boolean canStop()          // Can be interrupted? (default false)
}
```

### Goal Control Flags
```java
// Set control flags to prevent conflicts
this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false) {{
    setControls(EnumSet.of(Control.MOVE, Control.LOOK));
}});
```

Control flags: `MOVE`, `LOOK`, `JUMP`, `TARGET`

### Custom Goal Example
```java
public class FollowOwnerGoal extends Goal {
    private final TameableEntity mob;
    private PlayerEntity owner;
    private int updateCountdown;

    public FollowOwnerGoal(TameableEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        PlayerEntity owner = (PlayerEntity) mob.getOwner();
        if (owner == null || owner.isSpectator() || mob.isSitting()) return false;
        // Start if owner is far enough away
        return mob.squaredDistanceTo(owner) > 100; // 10 blocks
    }

    @Override
    public boolean shouldContinue() {
        return !mob.getNavigation().isIdle() && mob.squaredDistanceTo(owner) > 9;
    }

    @Override
    public void start() {
        updateCountdown = 0;
    }

    @Override
    public void tick() {
        if (--updateCountdown <= 0) {
            updateCountdown = 10;
            mob.getNavigation().startMovingTo(owner, speed);
        }
    }
}
```

## Entity Renderer API

### MobEntityRenderer<T, M extends EntityModel<T>>

```java
// Constructor parameters
MobEntityRenderer(EntityRendererFactory.Context ctx, M model, float shadowRadius)

// Key methods
Identifier getTexture(T entity)                           // Texture path
void render(T entity, float yaw, float tickDelta,         // Main render
            MatrixStack matrices, VertexConsumerProvider vertices, int light)
protected void scale(T entity, MatrixStack matrices, float tickDelta)  // Scaling
boolean shouldRender(T entity, Frustum frustum, double x, double y, double z)  // Visibility check
```

### EntityModel<T>

```java
public class MyModel extends EntityModel<MyEntity> {
    private final ModelPart root;
    private final ModelPart head;

    public MyModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    // Define model geometry
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0)
                .cuboid(-4, -8, -4, 8, 8, 8),  // x, y, z, width, height, depth
            ModelTransform.pivot(0, 0, 0));
        root.addChild("body",
            ModelPartBuilder.create()
                .uv(16, 16)
                .cuboid(-4, 0, -2, 8, 12, 4),
            ModelTransform.pivot(0, 0, 0));
        return TexturedModelData.of(data, 64, 32);  // texture width, height
    }

    @Override
    public void setAngles(MyEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // headYaw and headPitch are in radians
        head.yaw = headYaw * 0.017453292f;
        head.pitch = headPitch * 0.017453292f;

        // Walking animation
        body.pitch = (float) Math.sin(limbAngle) * 0.5f * limbDistance;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices,
                       int light, int overlay, float r, float g, float b, float a) {
        root.render(matrices, vertices, light, overlay, r, g, b, a);
    }
}
```

## Projectile Entities

### Simple Thrown Projectile

```java
public class MagicProjectileEntity extends ThrownItemEntity {
    public MagicProjectileEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public MagicProjectileEntity(EntityType<? extends ThrownItemEntity> type, LivingEntity owner, World world) {
        super(type, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MAGIC_PROJECTILE;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity target = entityHitResult.getEntity();
        target.damage(this.getDamageSources().magic(), 6.0f);
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }
}
```

### Registering Projectile Entity

```java
public static final EntityType<MagicProjectileEntity> MAGIC_PROJECTILE = Registry.register(
    Registries.ENTITY_TYPE,
    Identifier.of(MOD_ID, "magic_projectile"),
    FabricEntityTypeBuilder.<MagicProjectileEntity>create(SpawnGroup.MISC, MagicProjectileEntity::new)
        .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
        .trackRangeBlocks(4).trackedUpdateRate(20)
        .build()
);
```

### Rendering Projectile

```java
EntityRendererRegistry.register(ModEntities.MAGIC_PROJECTILE, (context) ->
    new FlyingItemEntityRenderer<MagicProjectileEntity>(context) {{
        // Optionally set item model display
    }}
);
```

## Spawn System Details

### SpawnRestriction.Location
- `ON_GROUND` - Surface spawning (most animals, monsters)
- `IN_WATER` - Water spawning (fish, squid)
- `IN_AIR` - Air spawning (bats, flying)

### Custom Spawn Predicate
```java
public static boolean canSpawn(EntityType<MagicSlimeEntity> type, ServerWorldAccess world,
                                SpawnReason reason, BlockPos pos, Random random) {
    return world.getBlockState(pos.down()).isIn(BlockTags.DIRT)
        && world.getLightLevel(pos) < 7
        && pos.getY() > world.getSeaLevel();
}
```

### SpawnReason Enum
- `NATURAL` - Natural spawning
- `SPAWN_EGG` - From spawn egg
- `COMMAND` - From /summon command
- `DISPENSE_EGG` - From dispenser
- `TRIGGERED` - From structure/event
- `BREEDING` - From breeding
- `MOB_SUMMONED` - From another entity
- `PATROL` - Pillager patrol
- `EVENT` - Raid/event
- `REINFORCEMENT` - Zombie reinforcement
