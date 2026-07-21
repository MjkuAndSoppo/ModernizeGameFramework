package com.modernizegameframework.medical;

import com.modernizegameframework.bodypart.BodyPartNetwork;
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
                usageTicksRemaining = item.getUsageTicks();
            }
            return true;
        } else {
            usageTicksRemaining--;
            if (usageTicksRemaining <= 0) {
                // 使用读条结束：应用效果并结算耐久
                boolean canContinue = applyEffectAndCheckContinue();
                settleDurability();
                // 只有声明为循环性质且效果返回还能继续时才进入下一次使用读条
                if (item.getEffect().isLoop() && canContinue) {
                    usageTicksRemaining = item.getUsageTicks();
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    /**
     * 应用效果并检查是否还能继续治疗
     * 应用后同步肢节数据到客户端，确保 HUD 及时刷新
     */
    private boolean applyEffectAndCheckContinue() {
        boolean canContinue = item.getEffect().apply(player, stack);
        if (!player.level().isClientSide()) {
            BodyPartNetwork.syncToClient(player);
        }
        return canContinue;
    }

    /**
     * 结算本次使用消耗的耐久
     */
    private void settleDurability() {
        if (item.isDurabilityItem()) {
            item.consumeDurability(stack, item.getEffect().durabilityCost());
            if (item.getDurability(stack) <= 0) {
                stack.shrink(1);
            }
        } else {
            stack.shrink(1);
        }
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
     * 拥有"无视"性质的物品免疫移动、载具、游泳等打断，但仍要求手持相同物品
     */
    private boolean isInterrupted() {
        if (item.getEffect().isUnbreakable()) {
            return false;
        }
        if (player.isSprinting() || player.isSwimming() || player.isFallFlying()) return true;
        if (player.getVehicle() != null) return true;
        if (player.position().distanceToSqr(startPosition) > 0.001) return true;
        return false;
    }

    /**
     * 会话结束时的清理
     * 耐久结算已在 tick 中完成，这里只做中断时的清理
     */
    public void finish(boolean completed) {
        // 中断时不消耗物品；正常完成已在 settleDurability 中处理
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
