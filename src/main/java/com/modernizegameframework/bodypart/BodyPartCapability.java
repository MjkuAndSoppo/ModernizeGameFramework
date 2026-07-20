package com.modernizegameframework.bodypart;

import net.minecraft.nbt.CompoundTag;

/**
 * 肢节血量能力接口
 * 定义玩家各部位血量的读取、修改、同步与序列化操作
 */
public interface BodyPartCapability {

    /**
     * 获取指定部位的当前血量
     *
     * @param type 部位
     * @return 当前血量
     */
    float getHealth(BodyPartType type);

    /**
     * 设置指定部位的当前血量，会自动限制在 [0, 最大血量] 范围内
     *
     * @param type  部位
     * @param value 目标血量
     */
    void setHealth(BodyPartType type, float value);

    /**
     * 获取指定部位的最大血量
     *
     * @param type 部位
     * @return 最大血量
     */
    float getMaxHealth(BodyPartType type);

    /**
     * 设置指定部位的最大血量
     *
     * @param type  部位
     * @param value 最大血量
     */
    void setMaxHealth(BodyPartType type, float value);

    /**
     * 判断指定部位是否已经黑掉（血量为 0 且上限大于 0）
     *
     * @param type 部位
     * @return 是否黑掉
     */
    boolean isDestroyed(BodyPartType type);

    /**
     * 获取所有部位当前血量之和
     *
     * @return 总血量
     */
    float getTotalHealth();

    /**
     * 获取所有部位最大血量之和
     *
     * @return 总上限
     */
    float getTotalMaxHealth();

    /**
     * 根据总血量重新计算各部位最大血量
     * 首次计算时会将当前血量设为最大血量
     *
     * @param totalMaxHealth 玩家总血量上限
     */
    void recalculateMaxHealth(float totalMaxHealth);

    /**
     * 将所有部位血量回满
     */
    void healAll();

    /**
     * 为指定部位回复血量，不会超过最大血量
     *
     * @param type   部位
     * @param amount 回复量
     */
    void heal(BodyPartType type, float amount);

    /**
     * 对指定部位造成伤害
     *
     * @param type   部位
     * @param amount 伤害量
     */
    void applyDamage(BodyPartType type, float amount);

    /**
     * 设置指定部位的出血剩余 tick
     *
     * @param type 部位
     * @param ticks 剩余 tick
     */
    void setBleedingTicks(BodyPartType type, int ticks);

    /**
     * 获取指定部位的出血剩余 tick
     *
     * @param type 部位
     * @return 剩余 tick
     */
    int getBleedingTicks(BodyPartType type);

    /**
     * 执行一次出血 tick 更新
     */
    void tickBleeding();

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
