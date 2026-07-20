package com.modernizegameframework.bodypart;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 肢节血量工具类
 * 提供便捷方法获取玩家的肢节血量能力实例
 */
public class BodyPartHelper {

    private BodyPartHelper() {}

    /**
     * 获取玩家的肢节血量能力
     *
     * @param player 玩家
     * @return 肢节血量能力 Optional
     */
    public static Optional<BodyPartCapability> getBodyPartCapability(Player player) {
        return player.getCapability(BodyPartCapabilityRegistry.BODY_PART_CAPABILITY).resolve();
    }

    /**
     * 对指定部位造成伤害
     *
     * @param player 玩家
     * @param type   部位
     * @param amount 伤害量
     * @return 实际造成的伤害量
     */
    public static float applyDamage(Player player, BodyPartType type, float amount) {
        return getBodyPartCapability(player).map(cap -> {
            float before = cap.getHealth(type);
            cap.applyDamage(type, amount);
            return before - cap.getHealth(type);
        }).orElse(0.0f);
    }

    /**
     * 将所有部位血量回满
     *
     * @param player 玩家
     */
    public static void healAll(Player player) {
        getBodyPartCapability(player).ifPresent(BodyPartCapability::healAll);
    }
}
