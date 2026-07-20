package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 安全箱系统网络通道
 */
public class SecureContainerNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "secure_container"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, OverlayClickPacket.class,
                OverlayClickPacket::encode, OverlayClickPacket::decode, OverlayClickPacket::handle);
        CHANNEL.registerMessage(packetId++, InventoryQuickMovePacket.class,
                InventoryQuickMovePacket::encode, InventoryQuickMovePacket::decode, InventoryQuickMovePacket::handle);
        CHANNEL.registerMessage(packetId++, RequestSyncPacket.class,
                RequestSyncPacket::encode, RequestSyncPacket::decode, RequestSyncPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncResultPacket.class,
                SyncResultPacket::encode, SyncResultPacket::decode, SyncResultPacket::handle);
    }

    public static void syncAll(ServerPlayer player) {
        player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncResultPacket(inv,
                    player.containerMenu.getCarried()));
        });
    }

    // ==================== OverlayClickPacket ====================

    /**
     * 附加面板点击包（客户端 → 服务端）
     * slotIndex = -1 表示主槽位，>= 0 表示容器槽位
     */
    public static class OverlayClickPacket {

        private final int slotIndex;
        private final int button; // 0 = 左键，1 = 右键
        private final boolean shift;

        public OverlayClickPacket(int slotIndex, int button, boolean shift) {
            this.slotIndex = slotIndex;
            this.button = button;
            this.shift = shift;
        }

        public static void encode(OverlayClickPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.slotIndex);
            buf.writeInt(packet.button);
            buf.writeBoolean(packet.shift);
        }

        public static OverlayClickPacket decode(FriendlyByteBuf buf) {
            return new OverlayClickPacket(buf.readInt(), buf.readInt(), buf.readBoolean());
        }

        public static void handle(OverlayClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
                    // 同步 Curios 槽位到能力（若 Curios 已安装）
                    syncCuriosToCapability(player, inv);

                    ItemStack currentContainer = inv.getContainerItem();

                    if (packet.slotIndex == -1) {
                        // === 主槽位操作 ===
                        // shift 点击对安全箱本身无效
                        if (packet.shift) return;
                        handleMainSlotClick(player, inv, packet.button);
                    } else {
                        // === 容器槽位操作 ===
                        if (currentContainer.isEmpty()) return;
                        if (!(currentContainer.getItem() instanceof SecureContainerItem sci)) return;

                        SecureContainerType type = sci.getType();
                        if (packet.shift) {
                            // shift 点击：快速将物品从安全箱移到玩家背包
                            quickMoveFromContainer(player, inv, type, packet.slotIndex);
                        } else {
                            // 普通点击：原版纳移/放置逻辑
                            handleContainerSlotClick(player, inv, type, packet.slotIndex, packet.button);
                        }
                    }
                });

                syncAll(player);
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== InventoryQuickMovePacket ====================

    /**
     * 将原版物品栏槽位中的物品快速移入安全箱
     */
    public static class InventoryQuickMovePacket {

        private final int vanillaSlotIndex;

        public InventoryQuickMovePacket(int vanillaSlotIndex) {
            this.vanillaSlotIndex = vanillaSlotIndex;
        }

        public static void encode(InventoryQuickMovePacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.vanillaSlotIndex);
        }

        public static InventoryQuickMovePacket decode(FriendlyByteBuf buf) {
            return new InventoryQuickMovePacket(buf.readInt());
        }

        public static void handle(InventoryQuickMovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
                    syncCuriosToCapability(player, inv);
                    quickMoveToContainer(player, inv, packet.vanillaSlotIndex);
                });

                syncAll(player);
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== RequestSyncPacket ====================

    /**
     * 同步请求包（客户端 → 服务端）
     * 打开物品栏时由客户端发送，服务端返回最新安全箱数据
     */
    public static class RequestSyncPacket {

        public RequestSyncPacket() {
        }

        public static void encode(RequestSyncPacket packet, FriendlyByteBuf buf) {
            // 空包，无需数据
        }

        public static RequestSyncPacket decode(FriendlyByteBuf buf) {
            return new RequestSyncPacket();
        }

        public static void handle(RequestSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                syncAll(player);
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== 槽位交互逻辑 ====================

    /**
     * 处理安全箱容器槽位的原版纳移/放置
     */
    private static void handleContainerSlotClick(ServerPlayer player, SecureContainerInventory inv,
                                                  SecureContainerType type, int slotIndex, int button) {
        ItemStackHandler handler = inv.getInventory(type);
        if (slotIndex < 0 || slotIndex >= handler.getSlots()) return;

        ItemStack slotStack = handler.getStackInSlot(slotIndex);
        ItemStack carried = player.containerMenu.getCarried();

        if (button == 0) {
            // 左键：整组取放或交换
            if (carried.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    player.containerMenu.setCarried(slotStack.copy());
                    handler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                }
            } else if (slotStack.isEmpty()) {
                if (!canPlaceInContainer(carried)) return;
                handler.setStackInSlot(slotIndex, carried.copy());
                player.containerMenu.setCarried(ItemStack.EMPTY);
            } else if (ItemStack.isSameItemSameTags(carried, slotStack)) {
                mergeStacks(handler, slotIndex, slotStack, carried, player);
            } else {
                if (!canPlaceInContainer(carried)) return;
                handler.setStackInSlot(slotIndex, carried.copy());
                player.containerMenu.setCarried(slotStack.copy());
            }
        } else if (button == 1) {
            // 右键：单个取放
            if (carried.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    int half = (slotStack.getCount() + 1) / 2;
                    ItemStack pickup = slotStack.copy();
                    pickup.setCount(half);
                    player.containerMenu.setCarried(pickup);
                    ItemStack remain = slotStack.copy();
                    remain.shrink(half);
                    handler.setStackInSlot(slotIndex, remain);
                }
            } else {
                if (slotStack.isEmpty()) {
                    if (!canPlaceInContainer(carried)) return;
                    placeOne(handler, slotIndex, carried, player);
                } else if (ItemStack.isSameItemSameTags(carried, slotStack)) {
                    if (slotStack.getCount() < slotStack.getMaxStackSize()) {
                        placeOne(handler, slotIndex, carried, player);
                    }
                }
            }
        }

        inv.markDirty();
    }

    /**
     * 处理主槽位点击（仅非 Curios 模式）
     */
    private static void handleMainSlotClick(ServerPlayer player, SecureContainerInventory inv, int button) {
        ItemStack currentContainer = inv.getContainerItem();
        ItemStack carried = player.containerMenu.getCarried();

        // 主槽位只支持完整取放（安全箱不可堆叠）
        if (carried.isEmpty()) {
            if (!currentContainer.isEmpty()) {
                player.containerMenu.setCarried(currentContainer.copy());
                inv.setContainerItem(ItemStack.EMPTY);
            }
        } else if (carried.getItem() instanceof SecureContainerItem) {
            ItemStack oldContainer = currentContainer.copy();
            inv.setContainerItem(carried.copy());
            player.containerMenu.setCarried(oldContainer);
        }
    }

    /**
     * 将安全箱槽位中的物品快速移到玩家背包
     */
    private static void quickMoveFromContainer(ServerPlayer player, SecureContainerInventory inv,
                                                SecureContainerType type, int slotIndex) {
        ItemStackHandler handler = inv.getInventory(type);
        if (slotIndex < 0 || slotIndex >= handler.getSlots()) return;

        ItemStack stack = handler.getStackInSlot(slotIndex);
        if (stack.isEmpty()) return;

        ItemStack remain = stack.copy();
        player.getInventory().add(remain);
        handler.setStackInSlot(slotIndex, remain);
        inv.markDirty();
    }

    /**
     * 将原版物品栏槽位中的物品快速移入安全箱
     */
    private static void quickMoveToContainer(ServerPlayer player, SecureContainerInventory inv, int vanillaSlotIndex) {
        if (vanillaSlotIndex < 0 || vanillaSlotIndex >= player.containerMenu.slots.size()) return;

        Slot slot = player.containerMenu.getSlot(vanillaSlotIndex);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        if (!canPlaceInContainer(stack)) return;

        SecureContainerType type = getCurrentContainerType(inv);
        if (type == null) return;

        ItemStackHandler handler = inv.getInventory(type);
        ItemStack remain = stack.copy();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (remain.isEmpty()) break;
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                handler.setStackInSlot(i, remain.copy());
                remain = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameTags(slotStack, remain)) {
                int canAdd = slotStack.getMaxStackSize() - slotStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remain.getCount());
                    ItemStack newSlot = slotStack.copy();
                    newSlot.grow(toAdd);
                    handler.setStackInSlot(i, newSlot);
                    remain.shrink(toAdd);
                }
            }
        }

        if (remain.getCount() != stack.getCount()) {
            slot.set(remain);
            inv.markDirty();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 检查物品是否可以放入安全箱（禁止嵌套）
     */
    private static boolean canPlaceInContainer(ItemStack stack) {
        return !(stack.getItem() instanceof SecureContainerItem);
    }

    /**
     * 获取当前装备的安全箱类型
     */
    private static SecureContainerType getCurrentContainerType(SecureContainerInventory inv) {
        ItemStack container = inv.getContainerItem();
        if (container.isEmpty()) return null;
        if (!(container.getItem() instanceof SecureContainerItem sci)) return null;
        return sci.getType();
    }

    /**
     * 将携带物品合并到槽位，并更新 cursor
     */
    private static void mergeStacks(ItemStackHandler handler, int slotIndex, ItemStack slotStack,
                                     ItemStack carried, ServerPlayer player) {
        int maxStackSize = slotStack.getMaxStackSize();
        int canAdd = maxStackSize - slotStack.getCount();
        if (canAdd <= 0) return;

        int toAdd = Math.min(canAdd, carried.getCount());
        ItemStack newSlot = slotStack.copy();
        newSlot.grow(toAdd);
        handler.setStackInSlot(slotIndex, newSlot);

        ItemStack newCarried = carried.copy();
        newCarried.shrink(toAdd);
        player.containerMenu.setCarried(newCarried.isEmpty() ? ItemStack.EMPTY : newCarried);
    }

    /**
     * 向槽位放入一个物品，并更新 cursor
     */
    private static void placeOne(ItemStackHandler handler, int slotIndex, ItemStack carried, ServerPlayer player) {
        ItemStack one = carried.copy();
        one.setCount(1);
        handler.setStackInSlot(slotIndex, one);

        ItemStack newCarried = carried.copy();
        newCarried.shrink(1);
        player.containerMenu.setCarried(newCarried.isEmpty() ? ItemStack.EMPTY : newCarried);
    }

    /**
     * 将 Curios 安全箱槽位的数据同步到能力
     */
    private static void syncCuriosToCapability(ServerPlayer player, SecureContainerInventory inv) {
        if (!SecureContainerCurios.isCuriosLoaded()) return;
        ItemStack curioItem = SecureContainerCurios.getCuriosSecureContainer(player);
        ItemStack capItem = inv.getContainerItem();
        if (!ItemStack.isSameItemSameTags(curioItem, capItem)) {
            inv.setContainerItem(curioItem.copy());
        }
    }

    // ==================== SyncResultPacket ====================

    /**
     * 同步结果包（服务端 → 客户端）
     */
    public static class SyncResultPacket {

        private ItemStack containerItem;
        private ItemStack carried;
        private final Map<SecureContainerType, CompoundTag> inventories;

        public SyncResultPacket(SecureContainerInventory inventory, ItemStack carried) {
            this.containerItem = inventory.getContainerItem().copy();
            this.carried = carried.copy();
            this.inventories = new HashMap<>();
            for (SecureContainerType type : SecureContainerType.values()) {
                this.inventories.put(type, inventory.getInventory(type).serializeNBT());
            }
        }

        private SyncResultPacket() {
            this.inventories = new HashMap<>();
        }

        public static void encode(SyncResultPacket packet, FriendlyByteBuf buf) {
            buf.writeItem(packet.containerItem);
            buf.writeItem(packet.carried);
            buf.writeInt(packet.inventories.size());
            for (var entry : packet.inventories.entrySet()) {
                buf.writeEnum(entry.getKey());
                buf.writeNbt(entry.getValue());
            }
        }

        public static SyncResultPacket decode(FriendlyByteBuf buf) {
            SyncResultPacket packet = new SyncResultPacket();
            packet.containerItem = buf.readItem();
            packet.carried = buf.readItem();
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                SecureContainerType type = buf.readEnum(SecureContainerType.class);
                CompoundTag tag = buf.readNbt();
                packet.inventories.put(type, tag);
            }
            return packet;
        }

        public static void handle(SyncResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
                        inv.setContainerItem(packet.containerItem);
                        for (var entry : packet.inventories.entrySet()) {
                            inv.getInventory(entry.getKey()).deserializeNBT(entry.getValue());
                        }
                    });
                    player.containerMenu.setCarried(packet.carried);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
