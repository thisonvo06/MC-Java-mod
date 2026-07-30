package snhyz.tutorialmod.net;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class WolfEvolveMod {

    // 记录每只狼的喂食次数（使用 UUID 避免弱引用问题）
    public static final Map<UUID, Integer> feedCountMap = new HashMap<>();
    // 记录已进化的狼
    public static final Set<UUID> evolvedWolves = new HashSet<>();

    public static void init() {
        registerFeedEvent();
        registerAutoAttackEvent();
    }

    private static void registerFeedEvent() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // ===== 客户端：只做拦截，不执行逻辑 =====
            if (world.isClientSide()) {
                if (entity instanceof Wolf wolf
                        && wolf.isTame()
                        && (player.getMainHandItem().is(Items.BEEF) || evolvedWolves.contains(wolf.getUUID()))) {
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // ===== 服务端逻辑 =====
            if (!(entity instanceof Wolf wolf)) return InteractionResult.PASS;

            if (!wolf.isTame()) return InteractionResult.PASS;

            // 如果已经进化
            if (evolvedWolves.contains(wolf.getUUID())) {
                // 手里没拿牛肉 -> 切换蹲起状态
                ItemStack handItem = player.getMainHandItem();
                if (!handItem.is(Items.BEEF)) {
                    wolf.setOrderedToSit(!wolf.isOrderedToSit());
                    return InteractionResult.SUCCESS;
                }
                // 拿着牛肉 -> 提示已进化
                player.displayClientMessage(Component.literal("这只狼已经进化了！"), true);
                return InteractionResult.SUCCESS;
            }

            // 检查手持物品是否是牛肉
            ItemStack mainHandItem = player.getMainHandItem();
            if (!mainHandItem.is(Items.BEEF)) return InteractionResult.PASS;

            UUID wolfId = wolf.getUUID();

            // 消耗1块牛肉
            mainHandItem.shrink(1);

            // 累加喂食次数
            int currentFeed = feedCountMap.getOrDefault(wolfId, 0) + 1;
            feedCountMap.put(wolfId, currentFeed);

            if (currentFeed < 20) {
                player.displayClientMessage(
                        Component.literal("§a喂食成功！(" + currentFeed + "/20) 继续喂食助狼进化！"),
                        true
                );
                wolf.heal(1.0f);
            } else {
                // 第20次，进化！
                evolvedWolves.add(wolfId);
                feedCountMap.remove(wolfId);
                wolf.setCustomName(Component.literal("§c§l进化之狼"));
                wolf.setCustomNameVisible(true);
                player.displayClientMessage(
                        Component.literal("§6§l✨ 狼进化了！现在它会自动攻击附近的怪物！✨"),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        });
    }

    @SuppressWarnings("unchecked")
    private static void registerAutoAttackEvent() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var iterator = evolvedWolves.iterator();
            while (iterator.hasNext()) {
                UUID wolfId = iterator.next();

                // 在所有维度中查找这只狼
                Wolf wolf = null;
                for (var world : server.getAllLevels()) {
                    Entity entity = world.getEntity(wolfId);
                    if (entity instanceof Wolf w && w.isAlive()) {
                        wolf = w;
                        break;
                    }
                }

                if (wolf == null || !wolf.isAlive()) {
                    iterator.remove();
                    continue;
                }

                // ===== 如果狼正在坐（玩家让它蹲下），不自动攻击 =====
                if (wolf.isOrderedToSit()) {
                    continue;
                }

                // 如果狼已经有一个目标且目标还活着，不打断
                if (wolf.getTarget() != null && wolf.getTarget().isAlive()) {
                    continue;
                }

                // 在周围 16 格范围内搜索所有敌对怪物
                AABB searchBox = wolf.getBoundingBox().inflate(16.0);

                // 收集骷髅、僵尸、蜘蛛
                List<Mob> enemies = new ArrayList<>();

                // 骷髅类
                enemies.addAll(wolf.level().getEntitiesOfClass(
                        AbstractSkeleton.class, searchBox,
                        e -> e.isAlive() && e.isEffectiveAi()
                ));
                // 僵尸类
                enemies.addAll(wolf.level().getEntitiesOfClass(
                        Zombie.class, searchBox,
                        e -> e.isAlive() && e.isEffectiveAi()
                ));
                // 蜘蛛类
                enemies.addAll(wolf.level().getEntitiesOfClass(
                        Spider.class, searchBox,
                        e -> e.isAlive() && e.isEffectiveAi()
                ));

                if (!enemies.isEmpty()) {
                    // 选择最近的敌人
                    Mob target = enemies.get(0);
                    double minDist = wolf.distanceToSqr(target);
                    for (Mob e : enemies) {
                        double dist = wolf.distanceToSqr(e);
                        if (dist < minDist) {
                            minDist = dist;
                            target = e;
                        }
                    }
                    wolf.setTarget(target);
                }
            }
        });
    }
}

