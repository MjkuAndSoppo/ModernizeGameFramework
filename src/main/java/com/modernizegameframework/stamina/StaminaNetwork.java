package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 体力值网络同步通道
 * 负责将服务端体力数据同步到客户端，用于 HUD 显示
 */
public class StaminaNetwork {

    /**
     * 网络协议版本号
     */
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 网络通道实例
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "stamina"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /**
     * 消息 ID 计数器
     */
    private static int packetId = 0;

    /**
     * 注册网络消息
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncStaminaPacket.class, SyncStaminaPacket::encode, SyncStaminaPacket::decode, SyncStaminaPacket::handle);
        CHANNEL.registerMessage(packetId++, SprintKeyPacket.class, SprintKeyPacket::encode, SprintKeyPacket::decode, SprintKeyPacket::handle);
    }

    /**
     * 向指定玩家发送体力同步包
     *
     * @param player 目标玩家
     */
    public static void syncToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        player.getCapability(StaminaRegistry.STAMINA_CAPABILITY).ifPresent(stamina -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncStaminaPacket(stamina.getCurrent(), stamina.getMax()));
        });
    }

    /**
     * 体力同步数据包
     */
    public static class SyncStaminaPacket {

        private final double current;
        private final double max;

        public SyncStaminaPacket(double current, double max) {
            this.current = current;
            this.max = max;
        }

        public static void encode(SyncStaminaPacket packet, FriendlyByteBuf buffer) {
            buffer.writeDouble(packet.current);
            buffer.writeDouble(packet.max);
        }

        public static SyncStaminaPacket decode(FriendlyByteBuf buffer) {
            return new SyncStaminaPacket(buffer.readDouble(), buffer.readDouble());
        }

        public static void handle(SyncStaminaPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.getCapability(StaminaRegistry.STAMINA_CAPABILITY).ifPresent(stamina -> {
                        stamina.setCurrent(packet.current);
                    });
                }
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 疾跑按键状态数据包（客户端 -> 服务端）
     */
    public static class SprintKeyPacket {

        private final boolean held;

        public SprintKeyPacket(boolean held) {
            this.held = held;
        }

        public static void encode(SprintKeyPacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.held);
        }

        public static SprintKeyPacket decode(FriendlyByteBuf buffer) {
            return new SprintKeyPacket(buffer.readBoolean());
        }

        public static void handle(SprintKeyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Player player = context.getSender();
                if (player != null) {
                    player.getCapability(StaminaRegistry.STAMINA_CAPABILITY).ifPresent(stamina -> {
                        stamina.setSprintKeyHeld(packet.held);
                    });
                }
            });
            context.setPacketHandled(true);
        }
    }
}
