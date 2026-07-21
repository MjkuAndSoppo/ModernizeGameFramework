package com.modernizegameframework.medical;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 医疗物品基类
 * 支持消耗品与耐久型两种模式，右键开始读条治疗
 * 耐久型物品采用原版耐久条显示
 */
public class MedicalItem extends Item {

    /**
     * 物品最大耐久，-1 表示消耗品
     */
    private final int maxDurability;

    /**
     * 启用读条时长（tick）
     */
    private final int activationTicks;

    /**
     * 使用读条时长（tick）
     */
    private final int usageTicks;

    /**
     * 绑定医疗效果
     */
    private final MedicalEffect effect;

    public MedicalItem(Properties properties, int maxDurability, int activationTicks, int usageTicks, MedicalEffect effect) {
        super(maxDurability > 0 ? properties.durability(maxDurability) : properties);
        this.maxDurability = maxDurability;
        this.activationTicks = activationTicks;
        this.usageTicks = usageTicks;
        this.effect = effect;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 任意性质物品跳过 canApply 检查
        if (!effect.isAnytime() && !effect.canApply(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        // 已经在读条中则不再重启
        if (level.isClientSide) {
            if (MedicalClientHandler.isInSession()) {
                return InteractionResultHolder.consume(stack);
            }
            if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
                MedicalClientHandler.startSession(localPlayer, hand, this, stack);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (MedicalHandler.isInSession(player)) {
            return InteractionResultHolder.consume(stack);
        }

        MedicalHandler.startSession(player, stack, hand, this);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 是否为耐久型物品
     */
    public boolean isDurabilityItem() {
        return maxDurability > 0;
    }

    /**
     * 获取当前耐久（采用原版耐久值）
     */
    public int getDurability(ItemStack stack) {
        if (!isDurabilityItem()) return 0;
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /**
     * 设置当前耐久（采用原版耐久值）
     */
    public void setDurability(ItemStack stack, int durability) {
        if (!isDurabilityItem()) return;
        int value = Math.max(0, Math.min(maxDurability, durability));
        stack.setDamageValue(stack.getMaxDamage() - value);
    }

    /**
     * 消耗耐久，返回是否成功
     */
    public boolean consumeDurability(ItemStack stack, int amount) {
        if (!isDurabilityItem()) return true;
        int current = getDurability(stack);
        if (current < amount) return false;
        setDurability(stack, current - amount);
        return true;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getActivationTicks() {
        return activationTicks;
    }

    public int getUsageTicks() {
        return usageTicks;
    }

    public MedicalEffect getEffect() {
        return effect;
    }
}
