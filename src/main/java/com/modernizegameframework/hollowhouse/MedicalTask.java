package com.modernizegameframework.hollowhouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 医疗站生产任务
 * 记录正在生产的配方、数量与剩余时间
 */
public class MedicalTask {

    /**
     * 当前任务使用的配方
     */
    private final MedicalRecipe recipe;

    /**
     * 计划生产数量
     */
    private final int amount;

    /**
     * 剩余总秒数
     */
    private int remainingSeconds;

    /**
     * 已完成的数量（用于计算当前在制作第几个）
     */
    private int completedAmount;

    public MedicalTask(MedicalRecipe recipe, int amount) {
        this.recipe = recipe;
        this.amount = Math.max(1, amount);
        this.remainingSeconds = recipe.getProductionSeconds() * this.amount;
        this.completedAmount = 0;
    }

    public MedicalTask(MedicalRecipe recipe, int amount, int remainingSeconds, int completedAmount) {
        this.recipe = recipe;
        this.amount = Math.max(1, amount);
        this.remainingSeconds = remainingSeconds;
        this.completedAmount = completedAmount;
    }

    /**
     * 获取配方
     */
    public MedicalRecipe getRecipe() {
        return recipe;
    }

    /**
     * 获取总制作数量
     */
    public int getAmount() {
        return amount;
    }

    /**
     * 获取剩余总秒数
     */
    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * 获取已完成数量
     */
    public int getCompletedAmount() {
        return completedAmount;
    }

    /**
     * 将剩余秒数格式化为 00:00:00
     */
    public String getFormattedRemainingTime() {
        int total = Math.max(0, remainingSeconds);
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * 获取任务显示文本
     * 格式：医疗物品*n{剩余时间}
     */
    public String getDisplayText() {
        ItemStack output = recipe.getOutput();
        String itemName = output.getHoverName().getString();
        return itemName + "*" + amount + "{" + getFormattedRemainingTime() + "}";
    }

    /**
     * 每秒推进一次任务进度
     *
     * @return 如果完成一个或多个产出，返回产出的物品栈；否则返回空
     */
    public ItemStack tick() {
        if (remainingSeconds <= 0) {
            return ItemStack.EMPTY;
        }
        remainingSeconds--;

        // 计算当前应完成的数量
        int productionTime = recipe.getProductionSeconds();
        int expectedCompleted = productionTime <= 0
                ? amount
                : Math.min(amount, (productionTime * amount - remainingSeconds) / productionTime);

        if (expectedCompleted > completedAmount) {
            int produceCount = expectedCompleted - completedAmount;
            completedAmount = expectedCompleted;
            ItemStack output = recipe.getOutput().copy();
            output.setCount(output.getCount() * produceCount);
            return output;
        }

        return ItemStack.EMPTY;
    }

    /**
     * 判断任务是否已全部完成
     */
    public boolean isFinished() {
        return completedAmount >= amount;
    }

    /**
     * 序列化为 NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Recipe", recipe.name());
        tag.putInt("Amount", amount);
        tag.putInt("RemainingSeconds", remainingSeconds);
        tag.putInt("CompletedAmount", completedAmount);
        return tag;
    }

    /**
     * 从 NBT 反序列化
     */
    public static MedicalTask deserializeNBT(CompoundTag tag) {
        MedicalRecipe recipe = MedicalRecipe.fromName(tag.getString("Recipe"));
        if (recipe == null) {
            return null;
        }
        int amount = tag.getInt("Amount");
        int remainingSeconds = tag.getInt("RemainingSeconds");
        int completedAmount = tag.getInt("CompletedAmount");
        return new MedicalTask(recipe, amount, remainingSeconds, completedAmount);
    }
}
