package com.modernizegameframework.inventory;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 塔科夫背包系统网络通道
 * 负责打开背包/容器的请求以及装备槽、扩展格数据的同步
 */
public class TarkovInventoryNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "tarkov_inventory"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    /**
     * 注册网络消息
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenInventoryPacket.class,
                OpenInventoryPacket::encode, OpenInventoryPacket::decode, OpenInventoryPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncInventoryPacket.class,
                SyncInventoryPacket::encode, SyncInventoryPacket::decode, SyncInventoryPacket::handle);
        CHANNEL.registerMessage(packetId++, QuickMovePacket.class,
                QuickMovePacket::encode, QuickMovePacket::decode, QuickMovePacket::handle);
    }

    /**
     * 将玩家装备槽与扩展格数据同步到客户端
     */
    public static void syncAll(ServerPlayer player) {
        player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY).ifPresent(cap -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncInventoryPacket(cap.serializeNBT()));
        });
    }

    // ==================== OpenInventoryPacket ====================

    /**
     * 客户端请求打开塔科夫背包界面
     */
    public static class OpenInventoryPacket {

        public OpenInventoryPacket() {
        }

        public static void encode(OpenInventoryPacket packet, FriendlyByteBuf buf) {
        }

        public static OpenInventoryPacket decode(FriendlyByteBuf buf) {
            return new OpenInventoryPacket();
        }

        public static void handle(OpenInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                // 先同步装备槽与扩展格数据，再打开界面
                syncAll(player);

                NetworkHooks.openScreen(player, new TarkovInventoryMenuProvider(null,
                                Component.translatable("container.modernizegameframework.tarkov_inventory")),
                        TarkovInventoryMenuProvider.extraDataWriter(0, Component.empty()));
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== SyncInventoryPacket ====================

    /**
     * 服务端向客户端同步装备槽与扩展格数据
     */
    public static class SyncInventoryPacket {

        private final CompoundTag data;

        public SyncInventoryPacket(CompoundTag data) {
            this.data = data;
        }

        public static void encode(SyncInventoryPacket packet, FriendlyByteBuf buf) {
            buf.writeNbt(packet.data);
        }

        public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new SyncInventoryPacket(tag == null ? new CompoundTag() : tag);
        }

        public static void handle(SyncInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Player player = getClientPlayer();
                if (player == null) return;

                player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY).ifPresent(cap -> {
                    cap.deserializeNBT(packet.data);
                });
            });
            context.setPacketHandled(true);
        }
    }

    // ==================== QuickMovePacket ====================

    /**
     * 快捷键移动请求
     * type = 0 表示 Alt+点击（移到装备区）
     * type = 1 表示 Ctrl+点击（移到容器区）
     */
    public static class QuickMovePacket {

        private final int slotIndex;
        private final int type;

        public QuickMovePacket(int slotIndex, int type) {
            this.slotIndex = slotIndex;
            this.type = type;
        }

        public static void encode(QuickMovePacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.slotIndex);
            buf.writeInt(packet.type);
        }

        public static QuickMovePacket decode(FriendlyByteBuf buf) {
            return new QuickMovePacket(buf.readInt(), buf.readInt());
        }

        public static void handle(QuickMovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                AbstractContainerMenu menu = player.containerMenu;
                if (!(menu instanceof TarkovInventoryMenu tarkovMenu)) return;

                if (packet.slotIndex < 0 || packet.slotIndex >= menu.slots.size()) return;

                if (packet.type == 0) {
                    TarkovInventoryQuickMove.moveToEquipment(player, tarkovMenu, packet.slotIndex);
                } else if (packet.type == 1) {
                    TarkovInventoryQuickMove.moveToContainer(player, tarkovMenu, packet.slotIndex);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 获取客户端玩家（仅在客户端调用）
     */
    private static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
