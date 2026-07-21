package com.modernizegameframework.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * 塔科夫背包快捷键移动逻辑
 * 处理 Alt+点击移到装备区、Ctrl+点击移到容器区
 */
public class TarkovInventoryQuickMove {

    /**
     * 将指定槽位的物品移到装备区合适槽位
     */
    public static void moveToEquipment(ServerPlayer player, TarkovInventoryMenu menu, int slotIndex) {
        Slot sourceSlot = menu.slots.get(slotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return;
        if (menu.isEquipmentSlot(sourceSlot)) return;

        ItemStack stack = sourceSlot.getItem();
        ItemStack moved = stack.copy();

        // 尝试放入胸挂或背包槽
        Slot chestRigSlot = menu.slots.get(TarkovInventoryMenu.EQUIPMENT_START);
        Slot backpackSlot = menu.slots.get(TarkovInventoryMenu.EQUIPMENT_START + 1);

        if (tryInsertIntoSlot(chestRigSlot, moved)) {
            finalizeMove(sourceSlot, stack, moved);
            return;
        }
        if (tryInsertIntoSlot(backpackSlot, moved)) {
            finalizeMove(sourceSlot, stack, moved);
            return;
        }

        // 尝试放入护甲槽
        EquipmentSlot equipSlot = Mob.getEquipmentSlotForItem(moved);
        if (equipSlot != null && equipSlot.getType() == EquipmentSlot.Type.ARMOR) {
            int armorIndex = TarkovInventoryMenu.ARMOR_START + armorSlotIndex(equipSlot);
            if (armorIndex >= TarkovInventoryMenu.ARMOR_START && armorIndex < TarkovInventoryMenu.RESULT_SLOT) {
                Slot armorSlot = menu.slots.get(armorIndex);
                if (tryInsertIntoSlot(armorSlot, moved)) {
                    finalizeMove(sourceSlot, stack, moved);
                    return;
                }
            }
        }

        // 尝试副手
        Slot offhandSlot = menu.slots.get(TarkovInventoryMenu.ARMOR_START + 4);
        if (tryInsertIntoSlot(offhandSlot, moved)) {
            finalizeMove(sourceSlot, stack, moved);
        }

        // 广播变化到客户端，防止物品位置不同步
        menu.broadcastChanges();
    }

    /**
     * 将指定槽位的物品移到右侧容器区
     */
    public static void moveToContainer(ServerPlayer player, TarkovInventoryMenu menu, int slotIndex) {
        if (!menu.hasExternalContainer()) return;

        Slot sourceSlot = menu.slots.get(slotIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return;
        if (menu.isContainerSlot(sourceSlot)) return;

        ItemStack stack = sourceSlot.getItem();
        ItemStack moved = stack.copy();

        int containerStart = TarkovInventoryMenu.CONTAINER_START;
        int containerEnd = containerStart + menu.getContainerSlotCount();

        for (int i = containerStart; i < containerEnd && !moved.isEmpty(); i++) {
            Slot targetSlot = menu.slots.get(i);
            moved = tryMergeIntoSlot(targetSlot, moved);
        }

        finalizeMove(sourceSlot, stack, moved);

        // 广播变化到客户端，防止容器区物品位置不同步
        menu.broadcastChanges();
    }

    /**
     * 尝试将物品插入槽位，返回是否成功插入至少一个
     */
    private static boolean tryInsertIntoSlot(Slot slot, ItemStack stack) {
        if (slot == null || stack.isEmpty()) return false;
        if (!slot.mayPlace(stack)) return false;

        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            slot.set(stack.copy());
            stack.setCount(0);
            slot.setChanged();
            return true;
        } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
            int canAdd = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), stack.getCount());
            if (canAdd > 0) {
                slotStack.grow(canAdd);
                stack.shrink(canAdd);
                slot.setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试将物品合并到槽位，返回剩余物品
     */
    @Nonnull
    private static ItemStack tryMergeIntoSlot(Slot slot, ItemStack stack) {
        if (slot == null || stack.isEmpty()) return stack;
        if (!slot.mayPlace(stack)) return stack;

        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            slot.set(stack.copy());
            stack = ItemStack.EMPTY;
            slot.setChanged();
        } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
            int canAdd = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), stack.getCount());
            if (canAdd > 0) {
                slotStack.grow(canAdd);
                stack.shrink(canAdd);
                slot.setChanged();
            }
        }
        return stack;
    }

    /**
     * 完成移动，更新源槽位并广播变化
     */
    private static void finalizeMove(Slot sourceSlot, ItemStack original, ItemStack moved) {
        if (moved.getCount() != original.getCount()) {
            int taken = original.getCount() - moved.getCount();
            ItemStack remain = original.copy();
            remain.shrink(taken);
            sourceSlot.set(remain);
            sourceSlot.setChanged();
        }
    }

    /**
     * 将原版 EquipmentSlot 映射到护甲槽位索引
     */
    private static int armorSlotIndex(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
    }
}
