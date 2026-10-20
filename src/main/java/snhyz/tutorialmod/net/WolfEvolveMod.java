package snhyz.tutorialmod.net;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WolfEvolveMod {

    public static final int FEEDS_REQUIRED = 20;
    private static final double SEARCH_RADIUS = 16.0;
    private static final int TICK_INTERVAL = 5;
    private static final String TAG_EVOLVED = "TemplateModEvolved";
    private static final String TAG_FEED_COUNT = "TemplateModFeedCount";

    // 内存缓存：UUID -> 喂食次数（进化成功后移除）
    private static final Map<UUID, Integer> feedCountMap = new ConcurrentHashMap<>();
    // 内存缓存：已进化的狼（进化和实体加载/死亡时维护）
    private static final Set<UUID> evolvedWolves = ConcurrentHashMap.newKeySet();

    private static final ResourceLocation BONUS_HEALTH = ResourceLocation.fromNamespaceAndPath(TemplateMod.MOD_ID, "evolved_health");
    private static final ResourceLocation BONUS_DAMAGE = ResourceLocation.fromNamespaceAndPath(TemplateMod.MOD_ID, "evolved_damage");
    private static final ResourceLocation BONUS_ARMOR = ResourceLocation.fromNamespaceAndPath(TemplateMod.MOD_ID, "evolved_armor");
    private static final ResourceLocation BONUS_SPEED = ResourceLocation.fromNamespaceAndPath(TemplateMod.MOD_ID, "evolved_speed");

    public static void init() {
        registerFeedEvent();
        registerAutoAttackEvent();
        registerCacheReset();
    }

    public static boolean isEvolved(Wolf wolf) {
        return evolvedWolves.contains(wolf.getUUID());
    }

    private static void registerFeedEvent() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Wolf wolf)) {
                return InteractionResult.PASS;
            }

            // ===== 客户端：只负责拦截，不执行逻辑 =====
            if (world.isClientSide()) {
                if (wolf.isTame()
                        && (player.getMainHandItem().is(Items.BEEF) || evolvedWolves.contains(wolf.getUUID()))) {
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // ===== 服务端逻辑 =====
            if (!wolf.isTame() || wolf.isBaby()) return InteractionResult.PASS;

            boolean holdingBeef = player.getMainHandItem().is(Items.BEEF);

            if (evolvedWolves.contains(wolf.getUUID())) {
                if (holdingBeef) {
                    player.displayClientMessage(Component.literal("这只狼已经进化了！"), true);
                } else {
                    wolf.setOrderedToSit(!wolf.isOrderedToSit());
                }
                return InteractionResult.SUCCESS;
            }

            if (!holdingBeef) return InteractionResult.PASS;

            if (wolf.getOwner() != player) {
                player.displayClientMessage(Component.literal("§c只能喂你自己的狼！"), true);
                return InteractionResult.SUCCESS;
            }

            UUID wolfId = wolf.getUUID();
            int currentFeed = feedCountMap.getOrDefault(wolfId, 0) + 1;

            // 创造模式不消耗牛肉
            if (!player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }

            if (currentFeed >= FEEDS_REQUIRED) {
                feedCountMap.remove(wolfId);
                evolve(wolf, (ServerLevel) world, player);
            } else {
                feedCountMap.put(wolfId, currentFeed);
                player.displayClientMessage(
                        Component.literal("§a喂食成功！(" + currentFeed + "/" + FEEDS_REQUIRED + ") 继续喂食助狼进化！"),
                        true
                );
                wolf.heal(2.0f);
            }

            return InteractionResult.SUCCESS;
        });
    }

    private static void evolve(Wolf wolf, ServerLevel level, net.minecraft.world.entity.player.Player player) {
        evolvedWolves.add(wolf.getUUID());
        // 永久属性修饰符会随实体自带 Attributes NBT 一起存档
        wolf.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(BONUS_HEALTH, 10.0, AttributeModifier.Operation.ADD_VALUE));
        wolf.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(BONUS_DAMAGE, 4.0, AttributeModifier.Operation.ADD_VALUE));
        wolf.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(BONUS_ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE));
        wolf.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(BONUS_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        wolf.setHealth(wolf.getMaxHealth());

        wolf.setCustomName(Component.literal("§c§l进化之狼"));
        wolf.setCustomNameVisible(true);
        player.displayClientMessage(
                Component.literal("§6§l✨ 狼进化了！现在它会自动攻击附近的怪物！✨"),
                true
        );
        level.playSound(null, wolf.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, wolf.getX(), wolf.getY() + 0.5, wolf.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void registerCacheReset() {
        // 世界（重新）加载时清空缓存，之后由各实体的 readAdditionalSaveData 逐步回填，避免旧 UUID 无限堆积
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            feedCountMap.clear();
            evolvedWolves.clear();
        });
    }

    private static void registerAutoAttackEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % TICK_INTERVAL != 0 || evolvedWolves.isEmpty()) {
                return;
            }

            for (var iterator = evolvedWolves.iterator(); iterator.hasNext(); ) {
                UUID wolfId = iterator.next();

                Wolf wolf = findWolf(server, wolfId);
                // 未加载的狼保留在集合中（区块卸载或世界刚启动），只有确认死亡才移除
                if (wolf == null) {
                    continue;
                }
                if (!wolf.isAlive()) {
                    iterator.remove();
                    feedCountMap.remove(wolfId);
                    continue;
                }

                // 坐下（玩家命令）时不主动战斗；无主人的野生进化狼不参战
                if (wolf.isOrderedToSit() || wolf.getOwner() == null) {
                    continue;
                }

                // 已有有效目标则不打断
                if (wolf.getTarget() != null && wolf.getTarget().isAlive()) {
                    continue;
                }

                Level level = wolf.level();
                AABB searchBox = new AABB(wolf.position(), wolf.position()).inflate(SEARCH_RADIUS);

                // 骷髅、僵尸、蜘蛛三类敌对怪物，取最近的一个
                List<Mob> enemies = level.getEntitiesOfClass(Mob.class, searchBox, e ->
                        e.isAlive()
                                && !e.isBaby()
                                && (e instanceof AbstractSkeleton || e instanceof Zombie || e instanceof Spider)
                );

                Mob target = null;
                double minDist = Double.MAX_VALUE;
                for (Mob e : enemies) {
                    double dist = wolf.distanceToSqr(e);
                    if (dist < minDist) {
                        minDist = dist;
                        target = e;
                    }
                }
                if (target != null) {
                    wolf.setTarget(target);
                }
            }
        });
    }

    private static Wolf findWolf(MinecraftServer server, UUID wolfId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(wolfId) instanceof Wolf wolf) {
                return wolf;
            }
        }
        return null;
    }

    // ===== Mixin 回调：随 Wolf 存档读写进化数据 =====

    public static void writeSaveData(Wolf wolf, ValueOutput output) {
        output.putBoolean(TAG_EVOLVED, evolvedWolves.contains(wolf.getUUID()));
        Integer feeds = feedCountMap.get(wolf.getUUID());
        if (feeds != null && feeds > 0) {
            output.putInt(TAG_FEED_COUNT, feeds);
        }
    }

    public static void readSaveData(Wolf wolf, ValueInput input) {
        if (input.getBooleanOr(TAG_EVOLVED, false)) {
            evolvedWolves.add(wolf.getUUID());
        }
        int feeds = input.getIntOr(TAG_FEED_COUNT, 0);
        UUID id = wolf.getUUID();
        if (feeds > 0) {
            feedCountMap.put(id, feeds);
        } else {
            feedCountMap.remove(id);
        }
    }
}
