package com.modernizegameframework.hollowhouse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 藏身处仓库辅助类
 * 负责计算仓库容量、构建当前等级可见容器、物品存取等工具方法
 */
public class HollowHouseStorehouseHelper {

    /** 仓库每行固定 8 格 */
    public static final int STOREHOUSE_COLUMNS = 8;
    /** 仓库最大等级 */
    public static final int MAX_STOREHOUSE_LEVEL = 4;

    private HollowHouseStorehouseHelper() {}

    /**
     * 根据仓库等级计算行数
     * 1 级 20 行，2 级 +10 行，3 级 +5 行，4 级 +20 行，溢出等级每级 +1 行
     */
    public static int getStorehouseRows(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level == 1) {
            return 20;
        }
        if (level == 2) {
            return 30;
        }
        if (level == 3) {
            return 35;
        }
        if (level == 4) {
            return 55;
        }
        // 溢出等级：4 级基础 55 行，之后每级 +1 行
        return 55 + (level - 4);
    }

    /**
     * 根据仓库等级计算可用槽位数
     */
    public static int getStorehouseSlots(int level) {
        return getStorehouseRows(level) * STOREHOUSE_COLUMNS;
    }

    /**
     * 获取仓库最大槽位数（4 级时）
     */
    public static int getMaxStorehouseSlots() {
        return getStorehouseSlots(MAX_STOREHOUSE_LEVEL);
    }

    /**
     * 从玩家藏身处数据中获取仓库当前等级
     */
    public static int getStorehouseLevel(ServerPlayer player) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return 0;
        }
        return data.getWorkBlockLevel(HollowHouseWorkBlockType.STOREHOUSE.getId());
    }

    /**
     * 构建当前等级可见的仓库容器
     * 将最大容量容器的前 N 个槽位复制到新的容器中返回
     */
    public static SimpleContainer createVisibleStorehouseContainer(ServerPlayer player) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return new SimpleContainer(0);
        }
        return createVisibleStorehouseContainer(data);
    }

    /**
     * 根据藏身处数据构建当前等级可见的仓库容器
     */
    public static SimpleContainer createVisibleStorehouseContainer(HollowHouseData data) {
        int level = data.getWorkBlockLevel(HollowHouseWorkBlockType.STOREHOUSE.getId());
        int visibleSlots = getStorehouseSlots(level);
        SimpleContainer visible = new SimpleContainer(visibleSlots);
        SimpleContainer max = data.getStorehouseInventory();
        int copyCount = Math.min(visibleSlots, max.getContainerSize());
        for (int i = 0; i < copyCount; i++) {
            visible.setItem(i, max.getItem(i).copy());
        }
        return visible;
    }

    /**
     * 将可见容器中的物品同步回最大容量仓库容器
     */
    public static void syncBackToStorehouse(HollowHouseData data, SimpleContainer visible) {
        SimpleContainer max = data.getStorehouseInventory();
        int copyCount = Math.min(visible.getContainerSize(), max.getContainerSize());
        for (int i = 0; i < copyCount; i++) {
            max.setItem(i, visible.getItem(i).copy());
        }
    }

    /**
     * 尝试从玩家仓库中消耗指定物品与数量
     *
     * @return 实际消耗数量
     */
    public static int consumeItem(ServerPlayer player, ItemStack stack, int count) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null || stack.isEmpty() || count <= 0) {
            return 0;
        }
        return consumeItem(data, stack, count);
    }

    /**
     * 从指定藏身处仓库中消耗物品
     */
    public static int consumeItem(HollowHouseData data, ItemStack stack, int count) {
        SimpleContainer inventory = data.getStorehouseInventory();
        int remaining = count;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty() || !slotStack.is(stack.getItem())) {
                continue;
            }
            int take = Math.min(remaining, slotStack.getCount());
            slotStack.shrink(take);
            if (slotStack.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            remaining -= take;
        }
        return count - remaining;
    }

    /**
     * 检查仓库中是否包含足够数量的指定物品
     */
    public static boolean hasItem(HollowHouseData data, ItemStack stack, int count) {
        SimpleContainer inventory = data.getStorehouseInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty() && slotStack.is(stack.getItem())) {
                found += slotStack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return found >= count;
    }

    /**
     * 获取仓库中指定物品的总数量
     */
    public static int getItemCount(HollowHouseData data, ItemStack stack) {
        SimpleContainer inventory = data.getStorehouseInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty() && slotStack.is(stack.getItem())) {
                found += slotStack.getCount();
            }
        }
        return found;
    }

    /**
     * 将物品放入仓库，优先堆叠到已有槽位，剩余放入空槽
     *
     * @return 未能放入的物品数量
     */
    public static int addItem(HollowHouseData data, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        SimpleContainer inventory = data.getStorehouseInventory();
        int remaining = stack.getCount();
        // 先尝试堆叠到已有同类槽位
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty() || !slotStack.is(stack.getItem())) {
                continue;
            }
            int maxStack = slotStack.getMaxStackSize();
            int canAdd = Math.min(remaining, maxStack - slotStack.getCount());
            if (canAdd > 0) {
                slotStack.grow(canAdd);
                remaining -= canAdd;
            }
        }
        // 再尝试放入空槽
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty()) {
                continue;
            }
            int maxStack = stack.getMaxStackSize();
            int placeCount = Math.min(remaining, maxStack);
            ItemStack placed = stack.copy();
            placed.setCount(placeCount);
            inventory.setItem(i, placed);
            remaining -= placeCount;
        }
        return remaining;
    }
}
