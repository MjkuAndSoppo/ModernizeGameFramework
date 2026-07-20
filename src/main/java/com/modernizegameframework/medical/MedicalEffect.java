package com.modernizegameframework.medical;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 医疗物品效果接口
 * 每个医疗物品绑定一个效果，负责目标选择、可用性判断和实际治疗
 */
public interface MedicalEffect {

    /**
     * 判断当前玩家是否可以使用该物品
     *
     * @param player 玩家
     * @param stack  物品堆
     * @return true 表示可以使用
     */
    boolean canApply(Player player, ItemStack stack);

    /**
     * 应用一次医疗效果
     * 对循环型物品会在读条期间被多次调用
     *
     * @param player 玩家
     * @param stack  物品堆
     * @return true 表示还有治疗空间（未满血），false 表示已经满血可终止
     */
    boolean apply(Player player, ItemStack stack);

    /**
     * 是否为循环效果
     * 循环效果在“使用读条”期间每 tick 调用一次 apply
     *
     * @return true 表示循环
     */
    boolean isLoop();

    /**
     * 单次读条结算时消耗的耐久值
     *
     * @return 耐久消耗
     */
    int durabilityCost();
}
