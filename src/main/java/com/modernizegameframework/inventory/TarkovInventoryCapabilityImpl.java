package com.modernizegameframework.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 塔科夫背包系统玩家能力实现
 * 使用两个 ItemStackHandler 分别保存装备槽和背包扩展格
 */
public class TarkovInventoryCapabilityImpl implements TarkovInventoryCapability {

    /**
     * 装备槽：0=胸挂，1=背包，2=安全箱
     */
    private final ItemStackHandler equipmentInventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.isEmpty()) {
                return true;
            }
            return switch (slot) {
                case SLOT_CHEST_RIG -> stack.is(TarkovInventoryTags.CHEST_RIG);
                case SLOT_BACKPACK -> stack.is(TarkovInventoryTags.BACKPACK);
                case SLOT_SECURE_CASE -> stack.is(TarkovInventoryTags.SECURE_CASE);
                default -> false;
            };
        }
    };

    /**
     * 背包扩展格，最多 3×9
     */
    private final ItemStackHandler expansionInventory = new ItemStackHandler(MAX_EXPANSION_SLOTS);

    /**
     * 安全箱内容格，最多 3×4（kappa 最大容量 12 格）
     */
    private final ItemStackHandler secureInventory = new ItemStackHandler(MAX_SECURE_SLOTS);

    @Override
    public ItemStack getChestRig() {
        return equipmentInventory.getStackInSlot(SLOT_CHEST_RIG);
    }

    @Override
    public void setChestRig(ItemStack stack) {
        equipmentInventory.setStackInSlot(SLOT_CHEST_RIG, stack);
    }

    @Override
    public ItemStack getBackpack() {
        return equipmentInventory.getStackInSlot(SLOT_BACKPACK);
    }

    @Override
    public void setBackpack(ItemStack stack) {
        equipmentInventory.setStackInSlot(SLOT_BACKPACK, stack);
    }

    @Override
    public ItemStack getSecureCase() {
        return equipmentInventory.getStackInSlot(SLOT_SECURE_CASE);
    }

    @Override
    public void setSecureCase(ItemStack stack) {
        equipmentInventory.setStackInSlot(SLOT_SECURE_CASE, stack);
    }

    @Override
    public ItemStackHandler getExpansionInventory() {
        return expansionInventory;
    }

    @Override
    public ItemStackHandler getSecureInventory() {
        return secureInventory;
    }

    @Override
    public ItemStackHandler getEquipmentInventory() {
        return equipmentInventory;
    }

    @Override
    public void clearExpansion() {
        for (int i = 0; i < expansionInventory.getSlots(); i++) {
            expansionInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Equipment", equipmentInventory.serializeNBT());
        tag.put("Expansion", expansionInventory.serializeNBT());
        tag.put("Secure", secureInventory.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Equipment")) {
            // 手动读取装备槽，兼容旧存档中 Equipment 只有 2 个槽位的情况
            CompoundTag equipmentTag = tag.getCompound("Equipment");
            ListTag items = equipmentTag.getList("Items", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < equipmentInventory.getSlots(); i++) {
                equipmentInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (slot >= 0 && slot < equipmentInventory.getSlots()) {
                    equipmentInventory.setStackInSlot(slot, ItemStack.of(itemTag));
                }
            }
        }
        if (tag.contains("Expansion")) {
            expansionInventory.deserializeNBT(tag.getCompound("Expansion"));
        }
        if (tag.contains("Secure")) {
            // 手动反序列化安全箱，防止旧存档中 Size=4 把 handler 缩回 4 格
            CompoundTag secureTag = tag.getCompound("Secure");
            ListTag secureItems = secureTag.getList("Items", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < secureInventory.getSlots(); i++) {
                secureInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < secureItems.size(); i++) {
                CompoundTag itemTag = secureItems.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (slot >= 0 && slot < secureInventory.getSlots()) {
                    secureInventory.setStackInSlot(slot, ItemStack.of(itemTag));
                }
            }
        }
    }
}
