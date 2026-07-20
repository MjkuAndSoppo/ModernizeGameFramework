package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import com.modernizegameframework.stamina.StaminaHelper;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 肢节血量惩罚效果处理器
 * 负责在腿黑/臂黑时给玩家施加移动、挖掘、换弹等 debuff
 */
public class BodyPartPenaltyHandler {

    /**
     * 腿黑移动速度惩罚的属性修饰符 UUID
     */
    private static final UUID LEG_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    /**
     * 腿黑移动速度惩罚的属性修饰符名称
     */
    private static final String LEG_SPEED_NAME = "modernizegameframework.leg_destroyed_speed";

    private BodyPartPenaltyHandler() {}

    /**
     * 判断左腿或右腿是否已经黑掉
     *
     * @param cap 肢节血量能力
     * @return 是否腿黑
     */
    public static boolean isLegDestroyed(BodyPartCapability cap) {
        return cap.isDestroyed(BodyPartType.LEFT_LEG) || cap.isDestroyed(BodyPartType.RIGHT_LEG);
    }

    /**
     * 判断左手或右手是否已经黑掉
     *
     * @param cap 肢节血量能力
     * @return 是否臂黑
     */
    public static boolean isArmDestroyed(BodyPartCapability cap) {
        return cap.isDestroyed(BodyPartType.LEFT_ARM) || cap.isDestroyed(BodyPartType.RIGHT_ARM);
    }

    /**
     * 应用腿黑惩罚
     *
     * @param player 玩家
     */
    public static void applyLegPenalty(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;
        if (movementSpeed.getModifier(LEG_SPEED_UUID) != null) return;

        double multiplier = Config.BODYPART_LEG_DESTROYED_SPEED_MULTIPLIER.get();
        double amount = multiplier - 1.0;
        movementSpeed.addTransientModifier(new AttributeModifier(LEG_SPEED_UUID, LEG_SPEED_NAME, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /**
     * 移除腿黑惩罚
     *
     * @param player 玩家
     */
    public static void removeLegPenalty(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;
        movementSpeed.removeModifier(LEG_SPEED_UUID);
    }

    /**
     * 更新腿黑惩罚状态
     *
     * @param player 玩家
     * @param cap    肢节血量能力
     */
    public static void updateLegPenalty(Player player, BodyPartCapability cap) {
        if (isLegDestroyed(cap)) {
            applyLegPenalty(player);
        } else {
            removeLegPenalty(player);
        }
    }

    /**
     * 应用臂黑惩罚
     * 给玩家添加"手无力"自定义效果和原版挖掘疲劳
     * 避免每 tick 重复刷新，仅在效果缺失或即将过期时补充
     *
     * @param player 玩家
     */
    public static void applyArmPenalty(Player player) {
        int level = Config.BODYPART_ARM_WEAKNESS_MINING_FATIGUE_LEVEL.get();
        int duration = 200;
        int refreshThreshold = 60;

        if (BodyPartEffects.HAND_WEAKNESS != null) {
            MobEffectInstance existing = player.getEffect(BodyPartEffects.HAND_WEAKNESS.get());
            if (existing == null || existing.getDuration() < refreshThreshold) {
                player.addEffect(new MobEffectInstance(BodyPartEffects.HAND_WEAKNESS.get(), duration, level, false, false, true));
            }
        }
        // 同时添加原版挖掘疲劳以实现挖掘减速
        MobEffectInstance existingDig = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (existingDig == null || existingDig.getDuration() < refreshThreshold) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, level, false, false, true));
        }
    }

    /**
     * 移除臂黑惩罚
     *
     * @param player 玩家
     */
    public static void removeArmPenalty(Player player) {
        if (BodyPartEffects.HAND_WEAKNESS != null) {
            player.removeEffect(BodyPartEffects.HAND_WEAKNESS.get());
        }
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    /**
     * 更新臂黑惩罚状态
     *
     * @param player 玩家
     * @param cap    肢节血量能力
     */
    public static void updateArmPenalty(Player player, BodyPartCapability cap) {
        if (isArmDestroyed(cap)) {
            applyArmPenalty(player);
        } else {
            removeArmPenalty(player);
        }
    }

    /**
     * 腿黑时行走扣体力
     *
     * @param player 玩家
     * @param cap    肢节血量能力
     */
    public static void tickLegStaminaCost(Player player, BodyPartCapability cap) {
        if (!isLegDestroyed(cap)) return;
        if (!player.onGround()) return;
        if (player.zza == 0.0f && player.xxa == 0.0f) return;
        if (player.tickCount % Config.BODYPART_LEG_DESTROYED_STAMINA_INTERVAL.get() != 0) return;

        StaminaHelper.consume(player, Config.BODYPART_LEG_DESTROYED_STAMINA_COST.get());
    }
}
