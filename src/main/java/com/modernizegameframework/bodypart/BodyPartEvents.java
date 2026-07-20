package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 肢节血量系统 Forge 事件总线监听器
 * 处理玩家能力挂载、血量上限加成、数据同步与重生重置
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class BodyPartEvents {

    /**
     * 血量上限加成的属性修饰符 UUID
     */
    private static final UUID HEALTH_BONUS_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    /**
     * 血量上限加成的属性修饰符名称
     */
    private static final String HEALTH_BONUS_NAME = "modernizegameframework.bodypart_health_bonus";

    /**
     * 为玩家实体附加肢节血量能力
     * 无论开关是否开启都附加，避免开关切换时已在线玩家丢失能力
     */
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "bodypart"), new BodyPartCapabilityProvider(player));
        }
    }

    /**
     * 玩家进入世界时同步肢节血量数据
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Player player) {
            if (Config.BODYPART_ENABLED.get()) {
                applyHealthBonus(player);
            }
            BodyPartNetwork.syncToClient(player);
        }
    }

    /**
     * 每 tick 检查开关状态并同步数据
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            // 处理出血 tick
            cap.tickBleeding();

            // 应用/移除部位黑掉惩罚，并处理腿黑额外体力消耗
            BodyPartPenaltyHandler.updateLegPenalty(player, cap);
            BodyPartPenaltyHandler.updateArmPenalty(player, cap);
            BodyPartPenaltyHandler.tickLegStaminaCost(player, cap);

            // 腿黑时强制取消疾跑（与服务端体力耗尽逻辑保持一致）
            if (BodyPartPenaltyHandler.isLegDestroyed(cap) && player.isSprinting()) {
                player.setSprinting(false);
            }

            // 开关状态变化时统一处理所有在线玩家
            // 实际开关切换由命令触发 updateAllPlayers 立即处理，此处仅做同步
            float currentMaxHealth = (float) player.getMaxHealth();
            float cachedMaxHealth = cap.getTotalMaxHealth();
            if (Math.abs(currentMaxHealth - cachedMaxHealth) > 0.001f) {
                cap.recalculateMaxHealth(currentMaxHealth);
            }

            if (cap.pollDirty()) {
                BodyPartNetwork.syncToClient(player);
            }
        });
    }

    /**
     * 玩家受伤时接管原版伤害，转换为肢节血量伤害
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!Config.BODYPART_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        DamageSource source = event.getSource();
        // 避免递归处理通用击杀和虚空伤害
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return;

        event.setCanceled(true);
        BodyPartDamageHandler.applyBodyPartDamage(player, source, event.getAmount());
    }

    /**
     * 腿黑时禁止跳跃
     * LivingJumpEvent 不可取消，通过撤销 Y 速度实现禁止跳跃
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!Config.BODYPART_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            if (BodyPartPenaltyHandler.isLegDestroyed(cap)) {
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(delta.x, 0.0, delta.z);
            }
        });
    }

    /**
     * 玩家死亡重生后重置肢节血量
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!Config.BODYPART_ENABLED.get()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            cap.recalculateMaxHealth((float) player.getMaxHealth());
            cap.healAll();
        });
        BodyPartNetwork.syncToClient(player);
    }

    /**
     * 给玩家应用血量上限加成
     *
     * @param player 玩家
     */
    public static void applyHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        if (maxHealth.getModifier(HEALTH_BONUS_UUID) != null) return;

        maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_BONUS_UUID, HEALTH_BONUS_NAME, Config.BODYPART_HEALTH_BONUS.get(), AttributeModifier.Operation.ADDITION));
        player.heal(Config.BODYPART_HEALTH_BONUS.get().floatValue());
        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> cap.recalculateMaxHealth((float) player.getMaxHealth()));
    }

    /**
     * 移除玩家的血量上限加成
     *
     * @param player 玩家
     */
    public static void removeHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        if (maxHealth.getModifier(HEALTH_BONUS_UUID) == null) return;

        maxHealth.removeModifier(HEALTH_BONUS_UUID);
        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> cap.recalculateMaxHealth((float) player.getMaxHealth()));
    }

    /**
     * 根据开关状态更新所有在线玩家的血量上限加成
     *
     * @param enable 是否开启
     */
    public static void updateAllPlayers(boolean enable) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (enable) {
                applyHealthBonus(player);
            } else {
                removeHealthBonus(player);
            }
            BodyPartNetwork.syncToClient(player);
        }
    }
}
