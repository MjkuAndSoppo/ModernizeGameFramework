package com.modernizegameframework.securecontainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 安全箱右键打开菜单
 * 包含容器槽位 + 玩家背包 + 快捷栏
 */
public class SecureContainerMenu extends AbstractContainerMenu {

    private final SecureContainerType type;
    private final ItemStackHandler containerInventory;

    /**
     * 服务端构造函数
     */
    public SecureContainerMenu(int id, Inventory playerInv, SecureContainerType type) {
        super(SecureContainerRegistry.SECURE_CONTAINER_MENU.get(), id);
        this.type = type;
        this.containerInventory = new ItemStackHandler(type.getSlotCount());

        // 从玩家能力中加载容器库存
        loadFromCapability(playerInv.player);

        addContainerSlots();
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
    }

    /**
     * 客户端构造函数（从 buffer 读取类型）
     */
    public SecureContainerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readEnum(SecureContainerType.class));
    }

    /**
     * 从玩家能力中加载容器库存数据
     */
    private void loadFromCapability(Player player) {
        player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
            ItemStackHandler source = inv.getInventory(type);
            for (int i = 0; i < type.getSlotCount(); i++) {
                containerInventory.setStackInSlot(i, source.getStackInSlot(i).copy());
            }
        });
    }

    /**
     * 将容器库存数据保存回玩家能力
     */
    private void saveToCapability(Player player) {
        player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
            ItemStackHandler target = inv.getInventory(type);
            for (int i = 0; i < type.getSlotCount(); i++) {
                target.setStackInSlot(i, containerInventory.getStackInSlot(i).copy());
            }
            inv.markDirty();
        });
    }

    /**
     * 添加容器槽位（居中布局，投掷器风格）
     */
    private void addContainerSlots() {
        int cols = type.getCols();
        int rows = type.getRows();
        int startX = (176 - cols * 18) / 2 + 1;
        int startY = 18;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                this.addSlot(new SlotItemHandler(containerInventory, index,
                        startX + col * 18, startY + row * 18));
            }
        }
    }

    /**
     * 添加玩家背包槽位（3行 × 9列）
     */
    private void addPlayerInventory(Inventory playerInv) {
        int startY = 18 + type.getRows() * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, startY + row * 18));
            }
        }
    }

    /**
     * 添加玩家快捷栏（1行 × 9列）
     */
    private void addPlayerHotbar(Inventory playerInv) {
        int startY = 18 + type.getRows() * 18 + 14 + 54 + 4;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, startY));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            int containerSlots = type.getSlotCount();

            if (index < containerSlots) {
                // 从容器槽位移到玩家背包
                if (!this.moveItemStackTo(stackInSlot, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移到容器槽位
                if (!this.moveItemStackTo(stackInSlot, 0, containerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // 关闭菜单时保存数据回能力
        if (!player.level().isClientSide) {
            saveToCapability(player);
        }
    }

    /**
     * 获取容器类型
     */
    public SecureContainerType getContainerType() {
        return type;
    }

    /**
     * 获取屏幕高度
     */
    public int getScreenHeight() {
        return 18 + type.getRows() * 18 + 14 + 54 + 4 + 18 + 7;
    }
}