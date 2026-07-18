package com.modernizegameframework.securecontainer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * 安全箱库存能力的默认实现
 * 每个玩家存储一份独立的安全箱数据
 */
public class SecureContainerInventoryImpl implements SecureContainerInventory {

    /** 主槽位中的容器物品 */
    private ItemStack containerItem = ItemStack.EMPTY;

    /** 各类型容器的库存映射 */
    private final Map<SecureContainerType, ItemStackHandler> inventories = new EnumMap<>(SecureContainerType.class);

    /** 脏标记，用于同步 */
    private boolean dirty = false;

    public SecureContainerInventoryImpl() {
        // 初始化每种容器的空库存
        for (SecureContainerType type : SecureContainerType.values()) {
            ItemStackHandler handler = new ItemStackHandler(type.getSlotCount());
            // 监听库存变化，标记脏数据
            handler.setSize(type.getSlotCount());
            inventories.put(type, handler);
        }
    }

    @Override
    public ItemStack getContainerItem() {
        return containerItem;
    }

    @Override
    public void setContainerItem(ItemStack stack) {
        this.containerItem = stack.copy();
        markDirty();
    }

    @Override
    public ItemStackHandler getInventory(SecureContainerType type) {
        return inventories.get(type);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 序列化主槽位容器物品
        if (!containerItem.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            containerItem.save(itemTag);
            tag.put("containerItem", itemTag);
        }

        // 序列化各类型容器库存
        ListTag inventoriesTag = new ListTag();
        for (Map.Entry<SecureContainerType, ItemStackHandler> entry : inventories.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("type", entry.getKey().name());
            entryTag.put("items", entry.getValue().serializeNBT());
            inventoriesTag.add(entryTag);
        }
        tag.put("inventories", inventoriesTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // 反序列化主槽位容器物品
        if (tag.contains("containerItem", Tag.TAG_COMPOUND)) {
            containerItem = ItemStack.of(tag.getCompound("containerItem"));
        } else {
            containerItem = ItemStack.EMPTY;
        }

        // 反序列化各类型容器库存
        if (tag.contains("inventories", Tag.TAG_LIST)) {
            ListTag inventoriesTag = tag.getList("inventories", Tag.TAG_COMPOUND);
            for (int i = 0; i < inventoriesTag.size(); i++) {
                CompoundTag entryTag = inventoriesTag.getCompound(i);
                String typeName = entryTag.getString("type");
                try {
                    SecureContainerType type = SecureContainerType.valueOf(typeName);
                    ItemStackHandler handler = inventories.get(type);
                    if (handler != null) {
                        handler.deserializeNBT(entryTag.getCompound("items"));
                    }
                } catch (IllegalArgumentException ignored) {
                    // 未知类型，跳过
                }
            }
        }
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