---
name: mc-fabric-entity
description: "Minecraft Fabric custom entity design for MC 1.21.x with Java. This skill should be used when creating custom mobs/creatures, entity registration, entity attributes (health, speed, attack), AI goals, entity rendering (EntityRenderer + EntityModel), spawn eggs, spawn restrictions, and biome spawn modifications. Covers both passive and hostile mob creation with full client-side rendering setup."
agent_created: true
---

# Minecraft Fabric Custom Entity Design (MC 1.21.x / Java)

## Overview

This skill provides the workflow for creating custom entities (mobs, creatures, projectiles) in a Fabric mod targeting MC 1.21.x. It covers entity registration, attributes, AI goals, rendering, spawn eggs, and natural spawning.

## When to Use

- Creating a custom mob (passive, hostile, or neutral)
- Adding entity attributes (health, speed, attack damage, follow range)
- Implementing AI behavior (wandering, attacking, following, breeding)
- Setting up entity rendering (model, texture, shadow size)
- Creating spawn eggs for the entity
- Configuring natural spawning conditions (biomes, light level, height)
- Creating custom projectiles (arrows, fireballs, thrown items)

## Entity Registration

### Registration Pattern

Entities need: registration (server), attribute registration (server), and renderer registration (client).

```java
package com.example.modid.registry;

import com.example.modid.ModNameMod;
import com.example.modid.entity.MagicSlimeEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;

public class ModEntities {
    public static final EntityType<MagicSlimeEntity> MAGIC_SLIME = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(ModNameMod.MOD_ID, "magic_slime"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, MagicSlimeEntity::new)
            .dimensions(EntityType.SLIME.getDimensions())  // Hitbox size
            .build()
    );

    public static void initialize() {
        // Register attributes
        FabricDefaultAttributeRegistry.register(MAGIC_SLIME, MagicSlimeEntity.createMobAttributes());
    }
}
```

### Entity Dimensions

```java
.dimensions(EntityDimensions.fixed(0.6f, 1.8f))  // width, height
.dimensions(EntityDimensions.fixed(1.0f, 1.0f))  // Square hitbox
```

## Entity Class

### Passive Mob (extends AnimalEntity)

```java
package com.example.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class MagicCowEntity extends AnimalEntity {
    public MagicCowEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 2.0));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(3, new TemptGoal(this, 1.25, 
            net.minecraft.recipe.Ingredient.ofItems(ModItems.MAGIC_BERRY), false));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createMagicCowAttributes() {
        return AnimalEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 20.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.2)
            .add(EntityAttributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return ModEntities.MAGIC_COW.create(world);
    }
}
```

### Hostile Mob (extends HostileEntity)

```java
public class MagicZombieEntity extends HostileEntity {
    public MagicZombieEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createMagicZombieAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, 30.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
            .add(EntityAttributes.FOLLOW_RANGE, 32.0);
    }
}
```

## Entity Attributes Reference

```java
AnimalEntity.createMobAttributes()          // Base mob attributes
    .add(EntityAttributes.MAX_HEALTH, 20.0)           // Default 20 (10 hearts)
    .add(EntityAttributes.MOVEMENT_SPEED, 0.25)       // Default 0.25
    .add(EntityAttributes.ATTACK_DAMAGE, 2.0)         // Default 2.0
    .add(EntityAttributes.FOLLOW_RANGE, 16.0)         // Aggro range
    .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.0)  // 0.0-1.0
    .add(EntityAttributes.FLYING_SPEED, 0.0)          // For flying entities
    .add(EntityAttributes.ARMOR, 0.0)                 // Defense points
    .add(EntityAttributes.ARMOR_TOUGHNESS, 0.0)       // Armor penetration resistance
    .add(EntityAttributes.JUMP_STRENGTH, 0.42)        // Horse jump strength
    .add(EntityAttributes.SAFE_FALL_DISTANCE, 3.0)    // Fall distance before damage
```

## AI Goals

### Goal Selector Priority

Lower number = higher priority. Goals with same priority are randomized.

```java
this.goalSelector.add(priority, goal);        // Behavior goals
this.targetSelector.add(priority, goal);       // Target selection goals
```

### Common Goals

| Goal Class | Behavior | Parameters |
|-----------|----------|------------|
| `SwimGoal` | Stay afloat in water | `(mob)` |
| `MeleeAttackGoal` | Walk to and attack target | `(mob, speed, pauseWhenMobIdle)` |
| `RangedAttackGoal` | Shoot projectiles at target | `(mob, speed, projectileInterval, maxRange)` |
| `WanderAroundFarGoal` | Random walking | `(mob, speed)` |
| `WanderAroundGoal` | Random walking (short range) | `(mob, speed)` |
| `LookAtEntityGoal` | Face an entity | `(mob, entityClass, range)` |
| `LookAroundGoal` | Random head rotation | `(mob)` |
| `EscapeDangerGoal` | Flee when hit | `(mob, speed)` |
| `AnimalMateGoal` | Breed with same species | `(mob, speed)` |
| `TemptGoal` | Follow player holding food | `(mob, speed, ingredient, scared)` |
| `FollowParentGoal` | Follow adult (babies) | `(mob, speed)` |
| `FollowMobGoal` | Follow other mobs | `(mob, speed, minDistance, maxDistance)` |
| `AttackWithOwnerGoal` | Attack owner's target (tamed) | `(mob)` |
| `TrackOwnerAttackerGoal` | Revenge owner's attacker (tamed) | `(mob)` |
| `SitGoal` | Sit command (tamed) | `(mob)` |
| `FlyGoal` | Fly around (bats, parrots) | `(mob)` |

### Target Goals

| Goal Class | Behavior |
|-----------|----------|
| `ActiveTargetGoal<T>` | Actively seek and attack target type T |
| `RevengeGoal` | Attack entity that damaged this mob |
| `TrackOwnerAttackerGoal` | Target whoever hurt owner |
| `UniversalAngerGoal` | Anger at all nearby entities of a type |

## Entity Rendering (Client-Side)

### Entity Renderer

```java
package com.example.modid.client.renderer;

import com.example.modid.ModNameMod;
import com.example.modid.entity.MagicSlimeEntity;
import com.example.modid.client.model.MagicSlimeModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MagicSlimeRenderer extends MobEntityRenderer<MagicSlimeEntity, MagicSlimeModel> {
    private static final Identifier TEXTURE = Identifier.of(ModNameMod.MOD_ID, "textures/entity/magic_slime.png");

    public MagicSlimeRenderer(EntityRendererFactory.Context context) {
        super(context, new MagicSlimeModel(context.getPart(ModModelLayers.MAGIC_SLIME)), 0.5f);
    }

    @Override
    public Identifier getTexture(MagicSlimeEntity entity) {
        return TEXTURE;
    }
}
```

### Entity Model

```java
package com.example.modid.client.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import com.example.modid.entity.MagicSlimeEntity;

public class MagicSlimeModel extends EntityModel<MagicSlimeEntity> {
    private final ModelPart body;

    public MagicSlimeModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("body",
            ModelPartBuilder.create()
                .uv(0, 0)
                .cuboid(-4, -4, -4, 8, 8, 8),
            ModelTransform.pivot(0, 20, 0));
        return TexturedModelData.of(modelData, 32, 16);
    }

    @Override
    public void setAngles(MagicSlimeEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // Animate body parts based on entity state
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
```

### Model Layer Registration

```java
package com.example.modid.client;

import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import com.example.modid.ModNameMod;

public class ModModelLayers {
    public static final EntityModelLayer MAGIC_SLIME =
        new EntityModelLayer(Identifier.of(ModNameMod.MOD_ID, "magic_slime"), "main");
}
```

### Registering Renderer and Model

In client init:

```java
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Override
public void onInitializeClient() {
    // Register renderer
    EntityRendererRegistry.register(ModEntities.MAGIC_SLIME, MagicSlimeRenderer::new);

    // Register model layer
    EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MAGIC_SLIME,
        MagicSlimeModel::getTexturedModelData);
}
```

### Entity Model JSON (alternative to Java model)

For models defined in JSON (like vanilla entity models), place at:
`src/main/resources/assets/<modid>/models/entity/magic_slime.json`

## Spawn Eggs

```java
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SpawnEggItem;

public static final Item MAGIC_SLIME_SPAWN_EGG = Registry.register(
    Registries.ITEM,
    Identifier.of(MOD_ID, "magic_slime_spawn_egg"),
    new SpawnEggItem(ModEntities.MAGIC_SLIME,
        0x440044,  // Primary color (background)
        0xFF00FF,  // Secondary color (spots)
        new FabricItemSettings())
);
```

Register spawn eggs BEFORE `ModItems.initialize()` is called, as `SpawnEggItem` requires all entity types to be registered first.

## Natural Spawning

### Spawn Restrictions

```java
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;

SpawnRestriction.register(
    ModEntities.MAGIC_SLIME,
    SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING,
    MagicSlimeEntity::canMobSpawn  // Custom predicate or use MobEntity::canMobSpawn
);
```

### Biome Spawn Additions

```java
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;

BiomeModifications.addSpawn(
    biome -> biome.hasTag(BiomeTags.IS_SWAMP),  // Biome filter
    SpawnGroup.MONSTER,                            // Spawn group
    ModEntities.MAGIC_SLIME,                      // Entity type
    10,    // Weight (higher = more common)
    2,     // Min group size
    4      // Max group size
);
```

### Common Biome Tags

| Tag | Description |
|-----|-------------|
| `IS_OVERWORLD` | All overworld biomes |
| `IS_NETHER` | All nether biomes |
| `IS_END` | All end biomes |
| `IS_FOREST` | Forest biomes |
| `IS_DESERT` | Desert biomes |
| `IS_SWAMP` | Swamp biomes |
| `IS_JUNGLE` | Jungle biomes |
| `IS_SAVANNA` | Savanna biomes |
| `IS_OCEAN` | Ocean biomes |
| `IS_RIVER` | River biomes |
| `IS_MOUNTAIN` | Mountain biomes |

## Complete Entity Creation Checklist

1. [ ] Create entity class (extends appropriate base: AnimalEntity, HostileEntity, MobEntity)
2. [ ] Implement `initGoals()` with behavior and target goals
3. [ ] Create static `createAttributes()` method
4. [ ] Register `EntityType` in `ModEntities`
5. [ ] Register attributes with `FabricDefaultAttributeRegistry`
6. [ ] Create entity renderer class (client)
7. [ ] Create entity model class (client)
8. [ ] Register model layer and renderer in client init
9. [ ] Create entity texture PNG at `assets/<modid>/textures/entity/<name>.png`
10. [ ] Create spawn egg item
11. [ ] Set up spawn restrictions
12. [ ] Add biome spawns (if natural spawning desired)
13. [ ] Add language entries for entity and spawn egg
14. [ ] Test in-game with `./gradlew runClient`

## Cross-References

- For project setup, use `mc-fabric-init`
- For spawn egg items, use `mc-fabric-item`
- For natural spawning biomes, use `mc-fabric-worldgen`
- For entity models/textures, use `mc-fabric-assets`

## Resources

### references/
- `entity_api.md` - Detailed API reference for EntityType, MobEntity, Goal system, EntityRenderer, EntityModel, spawn system, and projectile creation
