package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
 * 负责客户端与服务端之间的安全箱数据同步
 *
 * 设计原则：
 * - 客户端只发送操作意图（点击了哪个槽位、当前鼠标上有什么）
 * - 服务端权威执行操作
 * - 服务端把执行后的完整状态（库存 + 鼠标指针物品）同步回客户端
 * - 客户端不做乐观更新，完全依据服务端响应更新显示
 */
public class SecureContainerNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "secure_container"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    /**
     * 注册所有网络消息
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, OverlayClickPacket.class,
                OverlayClickPacket::encode, OverlayClickPacket::decode, OverlayClickPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncResultPacket.class,
                SyncResultPacket::encode, SyncResultPacket::decode, SyncResultPacket::handle);
    }

    /**
     * 向指定玩家同步全部安全箱数据
     */
    public static void syncAll(ServerPlayer player) {
        player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncResultPacket(inv,
                    player.containerMenu.getCarried()));
        });
    }

    // ==================== 数据包定义 ====================

    /**
     * 附加面板槽位点击包（客户端 → 服务端）
     * slotIndex = -1 表示主槽位，>= 0 表示容器槽位
     * cursorItem 为客户端点击时鼠标指针上的物品（仅作参考，服务端会重新判断）
     */
    public static class OverlayClickPacket {

        private final int slotIndex;
        private final ItemStack cursorItem;

        public OverlayClickPacket(int slotIndex, ItemStack cursorItem) {
            this.slotIndex = slotIndex;
            this.cursorItem = cursorItem;
        }

        public static void encode(OverlayClickPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.slotIndex);
            buf.writeItem(packet.cursorItem);
        }

        public static OverlayClickPacket decode(FriendlyByteBuf buf) {
            return new OverlayClickPacket(buf.readInt(), buf.readItem());
        }

        public static void handle(OverlayClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).ifPresent(inv -> {
                    ItemStack currentContainer = inv.getContainerItem();
                    ItemStack carried = player.containerMenu.getCarried();

                    if (packet.slotIndex == -1) {
                        // === 主槽位操作 ===
                        if (carried.isEmpty()) {
                            // 玩家空手点击主槽位 → 取出容器物品到鼠标指针
                            if (!currentContainer.isEmpty()) {
                                player.containerMenu.setCarried(currentContainer.copy());
                                inv.setContainerItem(ItemStack.EMPTY);
                            }
                        } else if (carried.getItem() instanceof SecureContainerItem) {
                            // 玩家手上拿着容器物品 → 放入主槽位，旧容器回到鼠标指针
                            ItemStack oldContainer = currentContainer.copy();
                            inv.setContainerItem(carried.copy());
                            player.containerMenu.setCarried(oldContainer);
                        }
                    } else {
                        // === 容器槽位操作 ===
                        if (currentContainer.isEmpty()) return;
                        if (!(currentContainer.getItem() instanceof SecureContainerItem sci)) return;

                        SecureContainerType type = sci.getType();
                        ItemStackHandler handler = inv.getInventory(type);

                        if (packet.slotIndex < 0 || packet.slotIndex >= handler.getSlots()) return;

                        ItemStack slotStack = handler.getStackInSlot(packet.slotIndex);

                        if (carried.isEmpty()) {
                            // 玩家空手点击容器槽位 → 取出槽位物品到鼠标指针
                            if (!slotStack.isEmpty()) {
                                player.containerMenu.setCarried(slotStack.copy());
                                handler.setStackInSlot(packet.slotIndex, ItemStack.EMPTY);
                            }
                        } else if (slotStack.isEmpty()) {
                            // 玩家手上有物品，槽位为空 → 放入物品
                            handler.setStackInSlot(packet.slotIndex, carried.copy());
                            player.containerMenu.setCarried(ItemStack.EMPTY);
                        } else {
                            // 玩家手上有物品，槽位有物品 → 交换
                            handler.setStackInSlot(packet.slotIndex, carried.copy());
                            player.containerMenu.setCarried(slotStack.copy());
                        }
                        inv.markDirty();
                    }
                });

                // 同步执行结果回客户端
                syncAll(player);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 同步结果包（服务端 → 客户端）
     * 包含安全箱库存数据和当前鼠标指针物品
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