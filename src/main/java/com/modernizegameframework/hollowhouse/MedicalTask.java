package com.modernizegameframework.hollowhouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 医疗站生产任务
 * 记录正在生产的配方、数量、开始时间与已完成数量
 * 根据实际经过时间实时校准剩余时间，避免玩家离开维度后时间异常
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
     * 任务开始时间（毫秒时间戳）
     */
    private final long startTime;

    /**
     * 已完成的数量
     */
    private int completedAmount;

    /**
     * 累计暂停时长（毫秒）
     */
    private long pausedDuration;

    /**
     * 暂停开始时间（毫秒），未暂停时为 0
     */
    private long pauseStartTime;

    public MedicalTask(MedicalRecipe recipe, int amount) {
        this.recipe = recipe;
        this.amount = Math.max(1, amount);
        this.startTime = System.currentTimeMillis();
        this.completedAmount = 0;
        this.pausedDuration = 0;
        this.pauseStartTime = 0;
    }

    public MedicalTask(MedicalRecipe recipe, int amount, long startTime, int completedAmount,
                       long pausedDuration, long pauseStartTime) {
        this.recipe = recipe;
        this.amount = Math.max(1, amount);
        this.startTime = startTime;
        this.completedAmount = completedAmount;
        this.pausedDuration = pausedDuration;
        this.pauseStartTime = pauseStartTime;
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
     * 获取任务开始时间
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取已完成数量
     */
    public int getCompletedAmount() {
        return completedAmount;
    }

    /**
     * 获取剩余制作数量
     */
    public int getRemainingAmount() {
        return amount - completedAmount;
    }

    /**
     * 获取总生产时间（秒）
     */
    public int getTotalSeconds() {
        return recipe.getProductionSeconds() * amount;
    }

    /**
     * 判断当前是否处于暂停状态
     */
    public boolean isPaused() {
        return pauseStartTime > 0;
    }

    /**
     * 暂停任务倒计时
     */
    public void pause() {
        if (!isPaused()) {
            pauseStartTime = System.currentTimeMillis();
        }
    }

    /**
     * 恢复任务倒计时
     */
    public void resume() {
        if (isPaused()) {
            pausedDuration += System.currentTimeMillis() - pauseStartTime;
            pauseStartTime = 0;
        }
    }

    /**
     * 获取当前累计暂停时长（包含正在进行的暂停）
     */
    public long getCurrentPausedDuration() {
        long current = pausedDuration;
        if (isPaused()) {
            current += System.currentTimeMillis() - pauseStartTime;
        }
        return current;
    }

    /**
     * 获取暂停开始时间
     */
    public long getPauseStartTime() {
        return pauseStartTime;
    }

    /**
     * 获取已经过秒数（扣除暂停时间）
     */
    public int getElapsedSeconds() {
        long effectiveElapsed = System.currentTimeMillis() - startTime - getCurrentPausedDuration();
        return (int) (effectiveElapsed / 1000L);
    }

    /**
     * 获取剩余总秒数（基于开始时间实时校准）
     */
    public int getRemainingSeconds() {
        if (completedAmount >= amount) {
            return 0;
        }
        int remaining = getTotalSeconds() - getElapsedSeconds();
        return Math.max(0, remaining);
    }

    /**
     * 将剩余秒数格式化为 00:00:00
     */
    public String getFormattedRemainingTime() {
        int total = Math.max(0, getRemainingSeconds());
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
     * 根据当前时间与开始时间的差值重新校准已完成数量
     * 处于暂停状态时不会推进进度
     *
     * @return 如果完成一个或多个产出，返回产出的物品栈；否则返回空
     */
    public ItemStack tick() {
        if (completedAmount >= amount) {
            return ItemStack.EMPTY;
        }

        // 暂停期间不推进实际进度，仅依赖暂停时长校准
        if (isPaused()) {
            return ItemStack.EMPTY;
        }

        int productionTimePerItem = recipe.getProductionSeconds();
        int elapsed = getElapsedSeconds();
        int expectedCompleted = productionTimePerItem <= 0
                ? amount
                : Math.min(amount, elapsed / productionTimePerItem);

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
     * 获取该任务的总经验消耗
     */
    public int getTotalExperienceCost() {
        return recipe.getExperienceCost() * amount;
    }

    /**
     * 获取剩余未完成部分的经验消耗（取消时返还）
     */
    public int getRemainingExperienceCost() {
        return recipe.getExperienceCost() * getRemainingAmount();
    }

    /**
     * 获取剩余未完成部分需要返还的材料列表
     */
    public List<ItemStack> getRemainingIngredients() {
        int remaining = getRemainingAmount();
        if (remaining <= 0) {
            return Collections.emptyList();
        }
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack ingredient : recipe.getIngredients()) {
            list.add(new ItemStack(ingredient.getItem(), ingredient.getCount() * remaining));
        }
        return list;
    }

    /**
     * 序列化为 NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Recipe", recipe.name());
        tag.putInt("Amount", amount);
        tag.putLong("StartTime", startTime);
        tag.putInt("CompletedAmount", completedAmount);
        tag.putLong("PausedDuration", pausedDuration);
        tag.putLong("PauseStartTime", pauseStartTime);
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
        long startTime = tag.getLong("StartTime");
        int completedAmount = tag.getInt("CompletedAmount");
        long pausedDuration = tag.getLong("PausedDuration");
        long pauseStartTime = tag.getLong("PauseStartTime");
        return new MedicalTask(recipe, amount, startTime, completedAmount, pausedDuration, pauseStartTime);
    }
}
