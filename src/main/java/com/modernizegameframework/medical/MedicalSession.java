package com.modernizegameframework.medical;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 医疗读条会话
 * 记录玩家当前正在使用的医疗物品、读条进度与判定状态
 */
public class MedicalSession {

    /**
     * 读条阶段
     */
    public enum Phase {
        ACTIVATING, // 启用读条
        USING       // 使用读条
    }

    private final Player player;
    private final InteractionHand hand;
    private final MedicalItem item;
    private final ItemStack stack;
    private final Vec3 startPosition;
    private final int startSlot;

    private Phase phase;
    private int activationTicksRemaining;
    private int usageTicksRemaining;

    public MedicalSession(Player player, InteractionHand hand, MedicalItem item, ItemStack stack) {
        this.player = player;
        this.hand = hand;
        this.item = item;
        this.stack = stack;
        this.startPosition = player.position();
        this.startSlot = player.getInventory().selected;
        this.phase = Phase.ACTIVATING;
        this.activationTicksRemaining = item.getActivationTicks();
        this.usageTicksRemaining = item.getUsageTicks();
    }

    /**
     * 每 tick 更新会话
     *
     * @return true 表示会话继续，false 表示结束
     */
    public boolean tick() {
        // 物品必须仍拿在手中且未切换槽位
        if (!isHoldingSameItem()) return false;

        // 读条期间不能移动、切换物品
        if (isInterrupted()) return false;

        if (phase == Phase.ACTIVATING) {
            activationTicksRemaining--;
            if (activationTicksRemaining <= 0) {
                phase = Phase.USING;
                // 进入使用阶段时先应用一次效果，便于循环物品
                return applyEffectAndCheckContinue();
            }
            return true;
        } else {
            usageTicksRemaining--;
            MedicalEffect effect = item.getEffect();
            if (effect.isLoop()) {
                // 循环物品每 tick 应用一次效果
                boolean canContinue = applyEffectAndCheckContinue();
                if (!canContinue) return false;
            }
            if (usageTicksRemaining <= 0) {
                // 非循环物品在结算时应用效果
                if (!effect.isLoop()) {
                    applyEffectAndCheckContinue();
                }
                return false;
            }
            return true;
        }
    }

    /**
     * 应用效果并检查是否需要继续
     */
    private boolean applyEffectAndCheckContinue() {
        return item.getEffect().apply(player, stack);
    }

    /**
     * 判断是否仍持有相同物品且未切换槽位
     */
    private boolean isHoldingSameItem() {
        ItemStack current = player.getItemInHand(hand);
        return current.getItem() == item && player.getInventory().selected == startSlot;
    }

    /**
     * 判断读条是否被打断
     * 移动、切换物品、切换视角都会打断
     */
    private boolean isInterrupted() {
        if (player.isSprinting() || player.isSwimming() || player.isFallFlying()) return true;
        if (player.getVehicle() != null) return true;
        if (player.position().distanceToSqr(startPosition) > 0.001) return true;
        return false;
    }

    /**
     * 结算耐久或消耗物品
     */
    public void finish(boolean completed) {
        if (!completed) return;
        if (item.isDurabilityItem()) {
            if (!item.consumeDurability(stack, item.getEffect().durabilityCost())) {
                // 耐久不足时中断，不应该走到这里
                return;
            }
            if (item.getDurability(stack) <= 0) {
                stack.shrink(1);
            }
        } else {
            stack.shrink(1);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public MedicalItem getItem() {
        return item;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getActivationTicksRemaining() {
        return activationTicksRemaining;
    }

    public int getUsageTicksRemaining() {
        return usageTicksRemaining;
    }

    public int getTotalTicks() {
        return item.getActivationTicks() + item.getUsageTicks();
    }

    public int getElapsedTicks() {
        if (phase == Phase.ACTIVATING) {
            return item.getActivationTicks() - activationTicksRemaining;
        }
        return item.getActivationTicks() + (item.getUsageTicks() - usageTicksRemaining);
    }
}
