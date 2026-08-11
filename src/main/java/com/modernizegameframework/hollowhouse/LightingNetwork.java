package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 照明工作方块网络通道
 * 负责打开面板与处理等级选择
 */
public class LightingNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "lighting"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenLightingScreenPacket.class,
                OpenLightingScreenPacket::encode, OpenLightingScreenPacket::decode,
                OpenLightingScreenPacket::handle);
        CHANNEL.registerMessage(packetId++, SelectLevelPacket.class,
                SelectLevelPacket::encode, SelectLevelPacket::decode,
                SelectLevelPacket::handle);
    }

    /**
     * 向客户端发送打开照明面板数据包
     */
    public static void openScreen(ServerPlayer player, int unlockedLevel) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        int selectedLevel = data != null ? data.getLightingData().getSelectedLevel() : 1;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenLightingScreenPacket(unlockedLevel, selectedLevel));
    }

    /**
     * 打开照明面板数据包
     */
    public static class OpenLightingScreenPacket {

        private final int unlockedLevel;
        private final int selectedLevel;

        public OpenLightingScreenPacket(int unlockedLevel, int selectedLevel) {
            this.unlockedLevel = unlockedLevel;
            this.selectedLevel = selectedLevel;
        }

        public static void encode(OpenLightingScreenPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.unlockedLevel);
            buffer.writeInt(packet.selectedLevel);
        }

        public static OpenLightingScreenPacket decode(FriendlyByteBuf buffer) {
            return new OpenLightingScreenPacket(buffer.readInt(), buffer.readInt());
        }

        public static void handle(OpenLightingScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> openClientScreen(packet.unlockedLevel, packet.selectedLevel));
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private static void openClientScreen(int unlockedLevel, int selectedLevel) {
            Minecraft.getInstance().setScreen(new LightingScreen(unlockedLevel, selectedLevel));
        }
    }

    /**
     * 选择照明等级请求
     */
    public static class SelectLevelPacket {

        private final int level;

        public SelectLevelPacket(int level) {
            this.level = level;
        }

        public static void encode(SelectLevelPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.level);
        }

        public static SelectLevelPacket decode(FriendlyByteBuf buffer) {
            return new SelectLevelPacket(buffer.readInt());
        }

        public static void handle(SelectLevelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                selectLevel(player, packet.level);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 服务端处理等级选择
     */
    private static void selectLevel(ServerPlayer player, int level) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }

        int unlockedLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.LIGHTING.getId());
        if (unlockedLevel <= 0) {
            return;
        }

        int clampedLevel = Math.max(1, Math.min(unlockedLevel, level));
        data.getLightingData().setSelectedLevel(clampedLevel);
    }
}
