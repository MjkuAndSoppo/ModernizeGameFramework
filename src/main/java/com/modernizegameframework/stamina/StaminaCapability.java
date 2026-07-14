package com.modernizegameframework.stamina;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 体力值能力的默认实现
 * 当前体力值存储在玩家能力中，最大体力值由属性系统决定
 */
public class StaminaCapability implements Stamina {

    private final Player player;
    private double current = 0;
    private boolean dirty = true;
    private boolean initialized = false;
    private boolean sprintKeyHeld = false;
    private int regenCooldown = 0;
    private double lastTickMax = -1;

    public StaminaCapability(Player player) {
        this.player = player;
    }

    @Override
    public double getCurrent() {
        return current;
    }

    @Override
    public void setCurrent(double value) {
        double max = getMax();
        double newValue = Math.max(0, Math.min(value, max));
        if (newValue != current) {
            current = newValue;
            markDirty();
        }
    }

    @Override
    public double getMax() {
        AttributeInstance attribute = player.getAttribute(MaxStaminaAttribute.MAX_STAMINA.get());
        return attribute == null ? StaminaConfig.DEFAULT_MAX_STAMINA.get() : attribute.getValue();
    }

    @Override
    public double consume(double amount) {
        if (amount <= 0) return 0;
        double actual = Math.min(current, amount);
        setCurrent(current - actual);
        return actual;
    }

    @Override
    public double restore(double amount) {
        if (amount <= 0) return 0;
        double before = current;
        setCurrent(current + amount);
        return current - before;
    }

    @Override
    public void onActionConsume(double amount) {
        if (amount <= 0) return;
        consume(amount);
        regenCooldown = (int) Math.ceil(StaminaConfig.REGEN_DELAY.get());
    }

    @Override
    public void setSprintKeyHeld(boolean held) {
        if (this.sprintKeyHeld != held) {
            this.sprintKeyHeld = held;
            markDirty();
        }
    }

    @Override
    public boolean isSprintKeyHeld() {
        return sprintKeyHeld;
    }

    @Override
    public void tick() {
        double max = getMax();

        // 首次 tick 时将当前体力设为最大值
        if (!initialized) {
            initialized = true;
            setCurrent(max);
            lastTickMax = max;
            return;
        }

        // 最大体力发生变化时标记同步
        if (max != lastTickMax) {
            lastTickMax = max;
            markDirty();
        }

        // 若最大体力发生变化导致当前值溢出，则进行限制
        if (current > max) {
            setCurrent(current);
        }

        // 正在疾跑时禁止自然恢复
        if (player.isSprinting()) {
            return;
        }

        if (regenCooldown > 0) {
            regenCooldown--;
            return;
        }

        double rate = StaminaConfig.REGEN_RATE.get();
        if (rate > 0 && current < max) {
            restore(rate);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("current", current);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        current = tag.getDouble("current");
        markDirty();
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public boolean pollDirty() {
        boolean result = dirty;
        dirty = false;
        return result;
    }
}
