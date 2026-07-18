package com.modernizegameframework.securecontainer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 安全箱库存能力接口
 * 绑定在玩家身上，存储主槽位容器物品和各类型容器的库存
 */
public interface SecureContainerInventory {

    /**
     * 获取主槽位中的容器物品
     * @return 容器物品，空则返回 ItemStack.EMPTY
     */
    ItemStack getContainerItem();

    /**
     * 设置主槽位中的容器物品
     * @param stack 容器物品
     */
    void setContainerItem(ItemStack stack);

    /**
     * 获取指定类型容器的库存
     * @param type 容器类型
     * @return 对应类型的物品栈处理器
     */
    ItemStackHandler getInventory(SecureContainerType type);

    /**
     * 将数据序列化为 NBT
     */
    CompoundTag serializeNBT();

    /**
     * 从 NBT 反序列化数据
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * 标记数据变更，需要同步
     */
    void markDirty();

    /**
     * 检查并清除脏标记
     * @return 是否需要同步
     */
    boolean pollDirty();
}