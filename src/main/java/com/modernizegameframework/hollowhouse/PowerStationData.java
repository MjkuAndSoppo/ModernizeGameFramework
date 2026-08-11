package com.modernizegameframework.hollowhouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 供电站数据
 * 管理燃油槽位、发电状态、剩余时间
 */
public class PowerStationData {

    /**
     * 燃油槽位列表，数量等于供电站等级
     */
    private final List<ItemStack> fuelSlots = new ArrayList<>();

    /**
     * 当前是否处于发电状态
     */
    private boolean generating = false;

    /**
     * 剩余发电秒数
     */
    private int remainingSeconds = 0;

    /**
     * 上次更新时间戳（毫秒），用于校准倒计时
     */
    private long lastUpdateTime = System.currentTimeMillis();

    /**
     * 当前发电值（未转换为时间的单位数）
     */
    private int powerValue = 0;

    /**
     * 设置燃油槽位数量（随供电站等级变化）
     */
    public void setFuelSlotCount(int count) {
        while (fuelSlots.size() < count) {
            fuelSlots.add(ItemStack.EMPTY);
        }
        while (fuelSlots.size() > count) {
            fuelSlots.remove(fuelSlots.size() - 1);
        }
    }

    public List<ItemStack> getFuelSlots() {
        return Collections.unmodifiableList(fuelSlots);
    }

    public ItemStack getFuelSlot(int index) {
        if (index < 0 || index >= fuelSlots.size()) {
            return ItemStack.EMPTY;
        }
        return fuelSlots.get(index);
    }

    public void setFuelSlot(int index, ItemStack stack) {
        if (index >= 0 && index < fuelSlots.size()) {
            fuelSlots.set(index, stack.copy());
        }
    }

    public boolean isGenerating() {
        return generating;
    }

    public void setGenerating(boolean generating) {
        this.generating = generating;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int seconds) {
        this.remainingSeconds = Math.max(0, seconds);
    }

    public int getPowerValue() {
        return powerValue;
    }

    public void setPowerValue(int value) {
        this.powerValue = Math.max(0, value);
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long time) {
        this.lastUpdateTime = time;
    }

    /**
     * 根据供电站等级获取每发电值生效秒数
     * 1 级 1 倍（60 秒），2 级 1.5 倍（90 秒），3 级 2 倍（120 秒）
     */
    public static int getSecondsPerPowerUnit(int level) {
        return switch (level) {
            case 2 -> 90;
            case 3 -> 120;
            default -> 60;
        };
    }

    /**
     * 获取预测总发电秒数（当前剩余 + 燃油槽位可转换）
     */
    public int getPredictedTotalSeconds(int level) {
        int secondsPerUnit = getSecondsPerPowerUnit(level);
        int fuelUnits = computeFuelUnits();
        return remainingSeconds + powerValue * secondsPerUnit + fuelUnits * secondsPerUnit;
    }

    /**
     * 计算燃油槽位中可转换的总发电值
     */
    public int computeFuelUnits() {
        int total = 0;
        for (ItemStack stack : fuelSlots) {
            total += computeFuelValue(stack);
        }
        return total;
    }

    /**
     * 计算单个物品可提供的发电值
     * 燃油罐/桶：每点耐久 1 单位
     * 原版可燃物品：每个物品 1 单位
     */
    public static int computeFuelValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(HollowHouseRegistry.FUEL_CANISTER.get()) || stack.is(HollowHouseRegistry.FUEL_BARREL.get())) {
            // 耐久型燃油：剩余耐久 = 可提供的发电值
            int maxDamage = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            return Math.max(0, maxDamage - damage);
        }
        if (isVanillaFuel(stack)) {
            // 原版可燃物品：每个物品 1 单位
            return stack.getCount();
        }
        return 0;
    }

    /**
     * 判断是否为原版可燃物品
     */
    public static boolean isVanillaFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 排除本模组的燃油物品
        if (stack.is(HollowHouseRegistry.FUEL_CANISTER.get()) || stack.is(HollowHouseRegistry.FUEL_BARREL.get())) {
            return false;
        }
        return net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null) > 0;
    }

    /**
     * 从槽位中消耗 1 单位燃料，返回是否成功
     */
    public boolean consumeOneFuelUnit() {
        for (int i = 0; i < fuelSlots.size(); i++) {
            ItemStack stack = fuelSlots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(HollowHouseRegistry.FUEL_CANISTER.get()) || stack.is(HollowHouseRegistry.FUEL_BARREL.get())) {
                // 耐久型：扣 1 点耐久
                int damage = stack.getDamageValue();
                if (damage < stack.getMaxDamage()) {
                    stack.setDamageValue(damage + 1);
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        fuelSlots.set(i, ItemStack.EMPTY);
                    }
                    return true;
                }
            } else if (isVanillaFuel(stack)) {
                // 原版可燃物品：扣 1 个
                stack.shrink(1);
                if (stack.isEmpty()) {
                    fuelSlots.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 校准剩余时间（基于上次更新时间）
     */
    public void calibrateTime() {
        if (!generating || remainingSeconds <= 0) {
            lastUpdateTime = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        int elapsed = (int) ((now - lastUpdateTime) / 1000L);
        if (elapsed > 0) {
            remainingSeconds = Math.max(0, remainingSeconds - elapsed);
            lastUpdateTime = now;
        }
    }

    /**
     * 序列化为 NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag slots = new ListTag();
        for (ItemStack stack : fuelSlots) {
            slots.add(stack.save(new CompoundTag()));
        }
        tag.put("FuelSlots", slots);
        tag.putBoolean("Generating", generating);
        tag.putInt("RemainingSeconds", remainingSeconds);
        tag.putLong("LastUpdateTime", lastUpdateTime);
        tag.putInt("PowerValue", powerValue);
        return tag;
    }

    /**
     * 从 NBT 反序列化
     */
    public void deserializeNBT(CompoundTag tag) {
        fuelSlots.clear();
        if (tag.contains("FuelSlots", Tag.TAG_LIST)) {
            ListTag slots = tag.getList("FuelSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < slots.size(); i++) {
                fuelSlots.add(ItemStack.of(slots.getCompound(i)));
            }
        }
        generating = tag.getBoolean("Generating");
        remainingSeconds = tag.getInt("RemainingSeconds");
        lastUpdateTime = tag.getLong("LastUpdateTime");
        powerValue = tag.getInt("PowerValue");
    }
}
