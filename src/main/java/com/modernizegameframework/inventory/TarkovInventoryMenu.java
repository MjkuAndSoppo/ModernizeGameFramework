package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 塔科夫三段式背包菜单
 * 包含左侧装备区、中部主仓库+扩展格、右侧容器区（可选）
 */
public class TarkovInventoryMenu extends AbstractContainerMenu {

    // ===== 界面布局常量 =====
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GAP = 1;
    private static final int SECTION_PADDING = 8;
    private static final int LEFT_WIDTH = 90;
    private static final int MIDDLE_WIDTH = 152;

    // ===== 槽位范围（按添加顺序） =====
    public static final int EQUIPMENT_START = 0;
    public static final int EQUIPMENT_COUNT = 3;       // 胸挂、背包、安全箱
    public static final int ARMOR_START = 3;
    public static final int ARMOR_COUNT = 5;           // 头、胸、腿、脚、副手
    public static final int OFFHAND_INDEX = ARMOR_START + 4;  // 副手槽位索引（最后一个护甲槽）
    public static final int RESULT_SLOT = 8;
    public static final int CRAFTING_START = 9;
    public static final int CRAFTING_COUNT = 4;
    public static final int CONTAINER_START = 13;

    private final Inventory playerInventory;
    private final TarkovInventoryCapability tarkovInv;
    private final ResultContainer resultContainer = new ResultContainer();
    private final CraftingContainer craftSlots;
    private final Container externalContainer;
    private final Component externalTitle;

    private int containerSlotCount = 0;
    private int secureStart = 0;
    private int mainStart = 0;
    private int expansionStart = 0;
    private int hotbarStart = 0;
    private boolean hasSecureSlots = false;
    /** 安全箱实际槽位行列数（由装备的安全箱类型决定） */
    private int secureCols = 0;
    private int secureRows = 0;
    private int actualSecureSlots = 0;

    /**
     * 服务端构造函数
     */
    public TarkovInventoryMenu(int id, Inventory playerInv, Container externalContainer, Component externalTitle) {
        super(TarkovInventoryRegistry.TARKOV_INVENTORY_MENU.get(), id);
        this.playerInventory = playerInv;
        this.tarkovInv = playerInv.player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("玩家缺少塔科夫背包能力"));
        this.externalContainer = externalContainer;
        this.externalTitle = externalTitle == null ? Component.empty() : externalTitle;
        this.containerSlotCount = externalContainer == null ? 0 : externalContainer.getContainerSize();
        this.craftSlots = new TransientCraftingContainer(this, 2, 2, NonNullList.withSize(4, ItemStack.EMPTY));

        addEquipmentSlots();
        addArmorSlots();
        addCraftingSlots();
        addContainerSlots();
        addSecureSlots();
        addMainInventorySlots();
        addExpansionSlots();
        addHotbarSlots();
    }

    /**
     * 客户端构造函数（从网络缓冲读取右侧容器信息）
     */
    public TarkovInventoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(TarkovInventoryRegistry.TARKOV_INVENTORY_MENU.get(), id);
        this.playerInventory = playerInv;
        this.tarkovInv = playerInv.player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("玩家缺少塔科夫背包能力"));
        this.containerSlotCount = buf.readInt();
        this.externalTitle = buf.readComponent();
        this.externalContainer = containerSlotCount > 0 ? new SimpleContainer(containerSlotCount) : null;
        this.craftSlots = new TransientCraftingContainer(this, 2, 2, NonNullList.withSize(4, ItemStack.EMPTY));

        addEquipmentSlots();
        addArmorSlots();
        addCraftingSlots();
        addContainerSlots();
        addSecureSlots();
        addMainInventorySlots();
        addExpansionSlots();
        addHotbarSlots();
    }

    // ===== 槽位添加 =====

    private void addEquipmentSlots() {
        ItemStackHandler equipment = tarkovInv.getEquipmentInventory();
        // 胸挂、背包、安全箱放在玩家模型右侧，从上到下排列
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_CHEST_RIG, 70, 35));
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_BACKPACK, 70, 57));
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_SECURE_CASE, 70, 79));
    }

    private void addArmorSlots() {
        // 头盔、胸甲、护腿、鞋子放在玩家模型左侧
        addSlot(new ArmorSlot(playerInventory, 39, EquipmentSlot.HEAD, 10, 35));
        addSlot(new ArmorSlot(playerInventory, 38, EquipmentSlot.CHEST, 10, 57));
        addSlot(new ArmorSlot(playerInventory, 37, EquipmentSlot.LEGS, 10, 79));
        addSlot(new ArmorSlot(playerInventory, 36, EquipmentSlot.FEET, 10, 101));
        // 副手放在安全箱下方（背包→安全箱→副手）
        addSlot(new Slot(playerInventory, 40, 70, 101));
    }

    private void addCraftingSlots() {
        // 2x2 合成栏移到玩家模型左下方
        addSlot(new ResultSlot(playerInventory.player, craftSlots, resultContainer, 0, 55, 145));
        addSlot(new Slot(craftSlots, 0, 10, 135));
        addSlot(new Slot(craftSlots, 1, 30, 135));
        addSlot(new Slot(craftSlots, 2, 10, 155));
        addSlot(new Slot(craftSlots, 3, 30, 155));
    }

    private void addContainerSlots() {
        secureStart = CONTAINER_START + containerSlotCount;
        if (externalContainer == null || containerSlotCount == 0) {
            return;
        }
        int rightStartX = LEFT_WIDTH + SECTION_PADDING + MIDDLE_WIDTH + SECTION_PADDING * 2;
        int startY = 34;
        for (int i = 0; i < containerSlotCount; i++) {
            int row = i / 9;
            int col = i % 9;
            addSlot(new Slot(externalContainer, i,
                    rightStartX + col * (SLOT_SIZE + SLOT_GAP),
                    startY + row * (SLOT_SIZE + SLOT_GAP)));
        }
    }

    private void addSecureSlots() {
        // 未装备安全箱时不添加内容格，实现完全隐藏
        ItemStack secureCase = tarkovInv.getSecureCase();
        if (secureCase.isEmpty()) {
            hasSecureSlots = false;
            secureCols = 0;
            secureRows = 0;
            actualSecureSlots = 0;
            mainStart = secureStart;
            return;
        }

        // 根据安全箱类型决定行列数
        SecureContainerType type = null;
        if (secureCase.getItem() instanceof SecureContainerItem sci) {
            type = sci.getType();
        }
        if (type == null) {
            // 未知安全箱类型，默认 2×2
            secureCols = 2;
            secureRows = 2;
        } else {
            secureCols = type.getCols();
            secureRows = type.getRows();
        }
        actualSecureSlots = secureCols * secureRows;

        hasSecureSlots = true;
        // 以中部面板全宽居中安全箱格子
        int middleStartX = LEFT_WIDTH + SECTION_PADDING;
        int gridPixelWidth = secureCols * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = middleStartX + (MIDDLE_WIDTH - gridPixelWidth) / 2;
        // 安全箱区域位置：主仓库 3 行 + 扩展格 3 行 + 间距
        int startY = 34 + 3 * (SLOT_SIZE + SLOT_GAP) + 8 + 3 * (SLOT_SIZE + SLOT_GAP) + 8;

        ItemStackHandler secure = tarkovInv.getSecureInventory();
        for (int row = 0; row < secureRows; row++) {
            for (int col = 0; col < secureCols; col++) {
                int index = row * secureCols + col;
                addSlot(new SecureSlot(secure, index,
                        startX + col * (SLOT_SIZE + SLOT_GAP),
                        startY + row * (SLOT_SIZE + SLOT_GAP)));
            }
        }
        mainStart = secureStart + actualSecureSlots;
    }

    private void addMainInventorySlots() {
        int startX = LEFT_WIDTH + SECTION_PADDING;
        int startY = 34;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int mainIndex = row * 9 + col;
                addSlot(new MainInventorySlot(playerInventory, mainIndex + 9, mainIndex,
                        startX + col * (SLOT_SIZE + SLOT_GAP),
                        startY + row * (SLOT_SIZE + SLOT_GAP)));
            }
        }
        expansionStart = mainStart + 27;
    }

    private void addExpansionSlots() {
        int startX = LEFT_WIDTH + SECTION_PADDING;
        int startY = 34 + 3 * (SLOT_SIZE + SLOT_GAP) + 8;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                addSlot(new ExpansionSlot(tarkovInv.getExpansionInventory(), index,
                        startX + col * (SLOT_SIZE + SLOT_GAP),
                        startY + row * (SLOT_SIZE + SLOT_GAP)));
            }
        }
        hotbarStart = expansionStart + 27;
    }

    private void addHotbarSlots() {
        int startX = LEFT_WIDTH + SECTION_PADDING;
        // 快捷栏锚定面板底部：紧接安全箱区域下方
        int secureEndY = 34 + 3 * (SLOT_SIZE + SLOT_GAP) + 8 + 3 * (SLOT_SIZE + SLOT_GAP) + 8
                + secureRows * (SLOT_SIZE + SLOT_GAP);
        int startY = secureEndY + 8;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    startX + col * (SLOT_SIZE + SLOT_GAP),
                    startY));
        }
    }

    // ===== crafting 结果更新 =====

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == craftSlots) {
            updateCraftingResult();
        }
        // 装备槽变更时重新整理物品（转移被锁定的物品）
        if (container == tarkovInv.getEquipmentInventory()) {
            TarkovInventoryHelper.rebalanceItems(playerInventory.player);
        }
    }

    private void updateCraftingResult() {
        if (playerInventory.player.level().isClientSide) {
            return;
        }
        ServerPlayer player = (ServerPlayer) playerInventory.player;
        ItemStack result = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = player.server.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level());
        if (recipe.isPresent()) {
            result = recipe.get().assemble(craftSlots, player.level().registryAccess());
        }
        resultContainer.setItem(0, result);
        broadcastChanges();
    }

    // ===== 快速移动 =====

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        // 装备区终点：排除副手槽（OFFHAND_INDEX），防止 shift+左键 优先到副手
        int equipmentEnd = OFFHAND_INDEX;
        int containerEnd = externalContainer == null ? CONTAINER_START : CONTAINER_START + containerSlotCount;
        int secureEnd = secureStart + actualSecureSlots;
        int mainEnd = mainStart + 27;
        int expansionEnd = expansionStart + 27;
        int hotbarEnd = hotbarStart + 9;

        if (index == RESULT_SLOT) {
            // 合成结果 -> 安全箱 / 主仓库 / 扩展格 / 快捷栏
            if (!moveItemStackTo(stack, secureStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= CRAFTING_START && index < CRAFTING_START + CRAFTING_COUNT) {
            // 合成材料 -> 安全箱 / 主仓库 / 扩展格 / 快捷栏
            if (!moveItemStackTo(stack, secureStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= EQUIPMENT_START && index < equipmentEnd) {
            // 装备区（不含副手） -> 安全箱 / 主仓库 / 扩展格 / 快捷栏
            if (!moveItemStackTo(stack, secureStart, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (externalContainer != null && index >= CONTAINER_START && index < containerEnd) {
            // 容器 -> 安全箱 / 主仓库 / 扩展格 / 快捷栏
            if (!moveItemStackTo(stack, secureStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 安全箱 / 主仓库 / 扩展格 / 快捷栏 -> 容器或装备区（不含副手）
            if (externalContainer != null) {
                if (!moveItemStackTo(stack, CONTAINER_START, containerEnd, false)) {
                    if (!moveItemStackTo(stack, EQUIPMENT_START, equipmentEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!moveItemStackTo(stack, EQUIPMENT_START, equipmentEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    // ===== 关闭菜单时处理 =====

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // 将合成格中的物品返还给玩家，放不下的掉落
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (externalContainer != null) {
            return externalContainer.stillValid(player);
        }
        return true;
    }

    // ===== getter =====

    public Container getExternalContainer() {
        return externalContainer;
    }

    public Component getExternalTitle() {
        return externalTitle;
    }

    public int getContainerSlotCount() {
        return containerSlotCount;
    }

    public int getMainStart() {
        return mainStart;
    }

    public int getExpansionStart() {
        return expansionStart;
    }

    public int getHotbarStart() {
        return hotbarStart;
    }

    public int getSecureStart() {
        return secureStart;
    }

    /** 获取安全箱列数 */
    public int getSecureCols() {
        return secureCols;
    }

    /** 获取安全箱行数 */
    public int getSecureRows() {
        return secureRows;
    }

    /** 获取实际安全箱槽位数 */
    public int getActualSecureSlots() {
        return actualSecureSlots;
    }

    public int getLeftWidth() {
        return LEFT_WIDTH;
    }

    public int getMiddleWidth() {
        return MIDDLE_WIDTH;
    }

    // ===== 自定义槽位类 =====

    /**
     * 装备槽位 SlotItemHandler，变更时通知菜单触发 rebalance
     */
    private class NotifyingSlotItemHandler extends SlotItemHandler {
        NotifyingSlotItemHandler(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // 装备槽变更时重新整理物品（转移被锁定的物品）
            if (!playerInventory.player.level().isClientSide) {
                TarkovInventoryHelper.rebalanceItems(playerInventory.player);
            }
        }
    }

    /**
     * 主仓库槽位，根据胸挂等级决定是否锁定
     */
    private class MainInventorySlot extends Slot {
        private final int mainIndex;

        MainInventorySlot(Inventory inv, int vanillaIndex, int mainIndex, int x, int y) {
            super(inv, vanillaIndex, x, y);
            this.mainIndex = mainIndex;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return mainIndex < TarkovInventoryHelper.getUnlockedMainSlots(playerInventory.player);
        }
    }

    /**
     * 背包扩展格槽位，根据背包类型决定是否锁定
     */
    private class ExpansionSlot extends SlotItemHandler {
        private final int expansionIndex;

        ExpansionSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.expansionIndex = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return expansionIndex < TarkovInventoryHelper.getExpansionSlotCount(playerInventory.player);
        }
    }

    /**
     * 安全箱内容槽位，未装备安全箱时锁定，禁止放入安全箱物品
     */
    private class SecureSlot extends SlotItemHandler {
        SecureSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            // 安全箱内不能放入安全箱
            if (stack.getItem() instanceof SecureContainerItem) return false;
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return !tarkovInv.getSecureCase().isEmpty();
        }
    }

    /**
     * 护甲槽位，限制只能放对应部位的护甲
     */
    private static class ArmorSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        ArmorSlot(Inventory inv, int index, EquipmentSlot slot, int x, int y) {
            super(inv, index, x, y);
            this.equipmentSlot = slot;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return Mob.getEquipmentSlotForItem(stack) == equipmentSlot;
        }
    }

    // ===== 槽位类型判断（供 Screen 渲染与交互使用） =====

    /**
     * 判断槽位是否属于装备区（胸挂、背包、护甲、副手）
     */
    public boolean isEquipmentSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= EQUIPMENT_START && index < CONTAINER_START;
    }

    /**
     * 判断槽位是否属于主仓库区域（不含快捷栏和扩展格）
     */
    public boolean isMainInventorySlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= mainStart && index < mainStart + 27;
    }

    /**
     * 判断槽位是否属于背包扩展格区域
     */
    public boolean isExpansionSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= expansionStart && index < expansionStart + 27;
    }

    /**
     * 判断槽位是否属于安全箱内容区
     */
    public boolean isSecureSlot(Slot slot) {
        if (!hasSecureSlots) return false;
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= secureStart && index < secureStart + actualSecureSlots;
    }

    /**
     * 判断槽位是否属于右侧容器区
     */
    public boolean isContainerSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0 || externalContainer == null) return false;
        return index >= CONTAINER_START && index < CONTAINER_START + containerSlotCount;
    }

    /**
     * 判断槽位是否属于快捷栏
     */
    public boolean isHotbarSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= hotbarStart && index < hotbarStart + 9;
    }

    /**
     * 判断槽位是否被锁定（主仓库、扩展格或安全箱）
     */
    public boolean isLockedSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        if (hasSecureSlots && index >= secureStart && index < secureStart + actualSecureSlots) {
            return tarkovInv.getSecureCase().isEmpty();
        }
        if (index >= mainStart && index < mainStart + 27) {
            int mainIndex = index - mainStart;
            return mainIndex >= TarkovInventoryHelper.getUnlockedMainSlots(playerInventory.player);
        }
        if (index >= expansionStart && index < expansionStart + 27) {
            int expansionIndex = index - expansionStart;
            return expansionIndex >= TarkovInventoryHelper.getExpansionSlotCount(playerInventory.player);
        }
        return false;
    }

    /**
     * 判断当前菜单是否关联了右侧容器
     */
    public boolean hasExternalContainer() {
        return externalContainer != null && containerSlotCount > 0;
    }

    /**
     * 获取胸挂装备槽物品
     */
    public ItemStack getChestRig() {
        return tarkovInv.getChestRig();
    }

    /**
     * 获取背包装备槽物品
     */
    public ItemStack getBackpack() {
        return tarkovInv.getBackpack();
    }

    /**
     * 获取安全箱装备槽物品
     */
    public ItemStack getSecureCase() {
        return tarkovInv.getSecureCase();
    }
}