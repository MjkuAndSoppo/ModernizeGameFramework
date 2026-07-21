package com.modernizegameframework.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 塔科夫背包系统辅助工具
 * 负责计算胸挂解锁格数、背包扩展行数以及锁定格物品转移
 */
public class TarkovInventoryHelper {

    /**
     * 根据胸挂物品返回解锁的主仓库格数
     */
    public static int getUnlockedMainSlots(ItemStack chestRig) {
        if (chestRig.isEmpty()) {
            return 0;
        }
        Item item = chestRig.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return 0;
        }
        return switch (id.getPath()) {
            case "chest_rig_level_1" -> 5;
            case "chest_rig_level_2" -> 9;
            case "chest_rig_level_3" -> 13;
            case "chest_rig_level_4" -> 18;
            case "chest_rig_level_5" -> 22;
            case "chest_rig_level_6" -> 27;
            default -> 0;
        };
    }

    /**
     * 根据背包物品返回扩展格数量
     */
    public static int getExpansionSlotCount(ItemStack backpack) {
        if (backpack.isEmpty()) {
            return 0;
        }
        Item item = backpack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return 0;
        }
        return switch (id.getPath()) {
            case "small_backpack" -> 9;
            case "medium_backpack" -> 18;
            case "large_backpack" -> 27;
            default -> 0;
        };
    }

    /**
     * 获取玩家当前解锁的主仓库格数
     */
    public static int getUnlockedMainSlots(Player player) {
        return player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .map(cap -> getUnlockedMainSlots(cap.getChestRig()))
                .orElse(0);
    }

    /**
     * 获取玩家当前可用的背包扩展格数量
     */
    public static int getExpansionSlotCount(Player player) {
        return player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .map(cap -> getExpansionSlotCount(cap.getBackpack()))
                .orElse(0);
    }

    /**
     * 重新整理玩家背包：
     * 把被胸挂等级锁定的主仓库格子和被卸下的背包扩展格中的物品
     * 转移到仍解锁的格子中，装不下的掉在地上
     */
    public static void rebalanceItems(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY).ifPresent(cap -> {
            Inventory inv = player.getInventory();
            int unlockedMain = getUnlockedMainSlots(player);
            int expansionCount = getExpansionSlotCount(player);

            // 收集被锁定主仓库格子中的物品（主仓库对应原版背包槽 9~35）
            List<ItemStack> toMove = new ArrayList<>();
            for (int i = 0; i < 27; i++) {
                int vanillaSlot = i + 9;
                if (i >= unlockedMain) {
                    ItemStack stack = inv.getItem(vanillaSlot);
                    if (!stack.isEmpty()) {
                        toMove.add(stack.copy());
                        inv.setItem(vanillaSlot, ItemStack.EMPTY);
                    }
                }
            }

            // 收集超过当前背包容量的扩展格物品
            var expansion = cap.getExpansionInventory();
            for (int i = expansionCount; i < TarkovInventoryCapability.MAX_EXPANSION_SLOTS; i++) {
                ItemStack stack = expansion.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    toMove.add(stack.copy());
                    expansion.setStackInSlot(i, ItemStack.EMPTY);
                }
            }

            // 尝试将物品放入可用格子，装不下的掉落
            List<ItemStack> remain = new ArrayList<>();
            for (ItemStack stack : toMove) {
                ItemStack leftover = tryPlaceInOpenSlots(stack, player, unlockedMain, expansionCount);
                if (!leftover.isEmpty()) {
                    remain.add(leftover);
                }
            }

            for (ItemStack stack : remain) {
                player.drop(stack, false);
            }
        });
    }

    /**
     * 尝试将物品放入可用的解锁格子
     */
    private static ItemStack tryPlaceInOpenSlots(ItemStack stack, Player player, int unlockedMain, int expansionCount) {
        Inventory inv = player.getInventory();
        ItemStack result = stack.copy();

        // 1. 先合并到已解锁主仓库的已有堆叠
        result = mergeIntoVanillaRange(result, inv, 9, 9 + unlockedMain);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 2. 合并到可用的扩展格
        final ItemStack[] expansionMergeRef = new ItemStack[]{result};
        player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY).ifPresent(cap -> {
            ItemStack leftover = mergeIntoHandler(expansionMergeRef[0], cap.getExpansionInventory(), expansionCount);
            expansionMergeRef[0] = leftover;
        });
        result = expansionMergeRef[0];
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 3. 合并到快捷栏
        result = mergeIntoVanillaRange(result, inv, 0, 9);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 4. 放入空的已解锁主仓库格子
        result = placeIntoEmptyVanillaRange(result, inv, 9, 9 + unlockedMain);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 5. 放入空的扩展格
        final ItemStack[] expansionPlaceRef = new ItemStack[]{result};
        player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY).ifPresent(cap -> {
            ItemStack leftover = placeIntoEmptyHandler(expansionPlaceRef[0], cap.getExpansionInventory(), expansionCount);
            expansionPlaceRef[0] = leftover;
        });
        result = expansionPlaceRef[0];
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 6. 放入空的快捷栏
        result = placeIntoEmptyVanillaRange(result, inv, 0, 9);
        return result;
    }

    /**
     * 将物品合并到原版背包指定槽位范围内的已有堆叠
     */
    private static ItemStack mergeIntoVanillaRange(ItemStack stack, Inventory inv, int start, int end) {
        ItemStack result = stack.copy();
        for (int i = start; i < end && !result.isEmpty(); i++) {
            ItemStack slotStack = inv.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, result)) {
                int canAdd = slotStack.getMaxStackSize() - slotStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, result.getCount());
                    slotStack.grow(toAdd);
                    inv.setItem(i, slotStack);
                    result.shrink(toAdd);
                }
            }
        }
        return result;
    }

    /**
     * 将物品放入原版背包指定范围内的空格
     */
    private static ItemStack placeIntoEmptyVanillaRange(ItemStack stack, Inventory inv, int start, int end) {
        ItemStack result = stack.copy();
        for (int i = start; i < end && !result.isEmpty(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, result.copy());
                result = ItemStack.EMPTY;
            }
        }
        return result;
    }

    /**
     * 将物品合并到 ItemStackHandler 前 slotCount 个格子的已有堆叠
     */
    private static ItemStack mergeIntoHandler(ItemStack stack, net.minecraftforge.items.ItemStackHandler handler, int slotCount) {
        ItemStack result = stack.copy();
        for (int i = 0; i < slotCount && !result.isEmpty(); i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, result)) {
                int canAdd = slotStack.getMaxStackSize() - slotStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, result.getCount());
                    slotStack.grow(toAdd);
                    handler.setStackInSlot(i, slotStack);
                    result.shrink(toAdd);
                }
            }
        }
        return result;
    }

    /**
     * 将物品放入 ItemStackHandler 前 slotCount 个格子中的空格
     */
    private static ItemStack placeIntoEmptyHandler(ItemStack stack, net.minecraftforge.items.ItemStackHandler handler, int slotCount) {
        ItemStack result = stack.copy();
        for (int i = 0; i < slotCount && !result.isEmpty(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                handler.setStackInSlot(i, result.copy());
                result = ItemStack.EMPTY;
            }
        }
        return result;
    }

    private TarkovInventoryHelper() {
    }
}
