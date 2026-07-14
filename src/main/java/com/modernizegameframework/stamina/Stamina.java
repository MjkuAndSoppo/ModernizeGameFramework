package com.modernizegameframework.stamina;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家体力值能力接口
 * 用于定义体力的读取、写入、消耗与恢复操作
 */
public interface Stamina {

    /**
     * 获取当前体力值
     *
     * @return 当前体力
     */
    double getCurrent();

    /**
     * 设置当前体力值
     *
     * @param value 目标值，会自动限制在 [0, 最大体力] 范围内
     */
    void setCurrent(double value);

    /**
     * 获取最大体力值
     *
     * @return 最大体力
     */
    double getMax();

    /**
     * 消耗指定数量的体力
     *
     * @param amount 消耗量
     * @return 实际消耗量（若体力不足则返回当前剩余量）
     */
    double consume(double amount);

    /**
     * 恢复指定数量的体力
     *
     * @param amount 恢复量
     * @return 实际恢复量
     */
    double restore(double amount);

    /**
     * 获取当前体力百分比
     *
     * @return 0.0 ~ 1.0
     */
    default double getPercent() {
        double max = getMax();
        return max <= 0 ? 0 : getCurrent() / max;
    }

    /**
     * 判断当前是否处于低体力状态（影响跳跃高度与攻击力）
     * 以最大体力的百分比作为判定标准，默认 10%
     *
     * @return 是否为低体力
     */
    default boolean isLow() {
        return getPercent() <= StaminaConfig.WEAK_THRESHOLD.get();
    }

    /**
     * 判断是否允许开始疾跑（体力高于锁定阈值）
     *
     * @return 是否可疾跑
     */
    default boolean canSprint() {
        return getCurrent() > StaminaConfig.SPRINT_LOCK_THRESHOLD.get();
    }

    /**
     * 判断体力是否已耗尽（归零）
     *
     * @return 体力是否耗尽
     */
    default boolean isDepleted() {
        return getCurrent() <= 0;
    }

    /**
     * 将数据序列化为 NBT
     *
     * @return NBT 标签
     */
    CompoundTag serializeNBT();

    /**
     * 从 NBT 反序列化数据
     *
     * @param tag NBT 标签
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * 执行一次行为消耗，并触发恢复延迟
     *
     * @param amount 消耗量
     */
    void onActionConsume(double amount);

    /**
     * 设置疾跑按键是否被按住
     *
     * @param held 是否按住
     */
    void setSprintKeyHeld(boolean held);

    /**
     * 获取疾跑按键是否被按住
     *
     * @return 是否按住
     */
    boolean isSprintKeyHeld();

    /**
     * 每 tick 更新一次体力状态（恢复、延迟等）
     */
    void tick();

    /**
     * 标记数据已变更，需要同步到客户端
     */
    void markDirty();

    /**
     * 检查并清除脏标记
     *
     * @return 是否需要同步
     */
    boolean pollDirty();
}
