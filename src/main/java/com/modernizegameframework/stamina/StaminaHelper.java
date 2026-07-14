package com.modernizegameframework.stamina;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * 体力值工具类
 * 提供便捷方法获取玩家的体力能力实例
 */
public class StaminaHelper {

    private StaminaHelper() {}

    /**
     * 获取玩家的体力能力
     *
     * @param player 玩家
     * @return 体力能力 Optional
     */
    public static Optional<Stamina> getStamina(Player player) {
        return player.getCapability(StaminaRegistry.STAMINA_CAPABILITY).resolve();
    }

    /**
     * 消耗玩家体力
     *
     * @param player 玩家
     * @param amount 消耗量
     * @return 实际消耗量
     */
    public static double consume(Player player, double amount) {
        return getStamina(player).map(stamina -> stamina.consume(amount)).orElse(0.0);
    }

    /**
     * 恢复玩家体力
     *
     * @param player 玩家
     * @param amount 恢复量
     * @return 实际恢复量
     */
    public static double restore(Player player, double amount) {
        return getStamina(player).map(stamina -> stamina.restore(amount)).orElse(0.0);
    }
}
