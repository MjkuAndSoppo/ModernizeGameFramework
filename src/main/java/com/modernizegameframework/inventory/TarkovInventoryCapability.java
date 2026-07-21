package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 塔科夫背包系统玩家能力接口
 * 提供胸挂槽、背包装备槽和背包扩展格的读写
 */
public interface TarkovInventoryCapability {

    /** 装备槽索引：胸挂 */
    int SLOT_CHEST_RIG = 0;
    /** 装备槽索引：背包 */
    int SLOT_BACKPACK = 1;
    /** 装备槽索引：安全箱 */
    int SLOT_SECURE_CASE = 2;
    /** 最大扩展格数量（3行 × 9列） */
    int MAX_EXPANSION_SLOTS = 27;
    /** 最大安全箱格数量（兼容所有类型，kappa 3×4 = 12） */
    int MAX_SECURE_SLOTS = 12;

    /**
     * 获取胸挂装备槽中的物品
     */
    ItemStack getChestRig();

    /**
     * 设置胸挂装备槽中的物品
     */
    void setChestRig(ItemStack stack);

    /**
     * 获取背包装备槽中的物品
     */
    ItemStack getBackpack();

    /**
     * 设置背包装备槽中的物品
     */
    void setBackpack(ItemStack stack);

    /**
     * 获取安全箱装备槽中的物品
     */
    ItemStack getSecureCase();

    /**
     * 设置安全箱装备槽中的物品
     */
    void setSecureCase(ItemStack stack);

    /**
     * 获取背包扩展库存
     */
    ItemStackHandler getExpansionInventory();

    /**
     * 获取安全箱库存
     */
    ItemStackHandler getSecureInventory();

    /**
     * 获取装备槽库存（胸挂、背包、安全箱）
     */
    ItemStackHandler getEquipmentInventory();

    /**
     * 序列化为 NBT
     */
    CompoundTag serializeNBT();

    /**
     * 清空扩展格内容（死亡掉落时使用）
     */
    void clearExpansion();

    /**
     * 从 NBT 反序列化
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * 获取当前装备的安全箱类型，未装备时返回 null
     */
    default SecureContainerType getSecureCaseType() {
        ItemStack secureCase = getSecureCase();
        if (secureCase.isEmpty()) return null;
        if (secureCase.getItem() instanceof SecureContainerItem sci) {
            return sci.getType();
        }
        return null;
    }
}
