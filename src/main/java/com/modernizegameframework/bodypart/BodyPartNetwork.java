package com.modernizegameframework.bodypart;

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
 * 肢节血量网络同步通道
 * 负责将服务端各部位血量数据同步到客户端，用于 HUD 显示与状态判断
 */
public class BodyPartNetwork {

    /**
     * 网络协议版本号
     */
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 网络通道实例
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "bodypart"),
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
        CHANNEL.registerMessage(packetId++, SyncBodyPartPacket.class, SyncBodyPartPacket::encode, SyncBodyPartPacket::decode, SyncBodyPartPacket::handle);
    }

    /**
     * 向指定玩家发送肢节血量同步包
     *
     * @param player 目标玩家
     */
    public static void syncToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        player.getCapability(BodyPartCapabilityRegistry.BODY_PART_CAPABILITY).ifPresent(cap -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncBodyPartPacket(cap));
        });
    }

    /**
     * 肢节血量同步数据包
     */
    public static class SyncBodyPartPacket {

        private final BodyPartData data;

        public SyncBodyPartPacket(BodyPartCapability cap) {
            int count = BodyPartType.values().length;
            float[] health = new float[count];
            float[] maxHealth = new float[count];
            int[] bleeding = new int[count];
            for (BodyPartType type : BodyPartType.values()) {
                health[type.ordinal()] = cap.getHealth(type);
                maxHealth[type.ordinal()] = cap.getMaxHealth(type);
                bleeding[type.ordinal()] = cap.getBleedingTicks(type);
            }
            this.data = new BodyPartData(health, maxHealth, bleeding);
        }

        private SyncBodyPartPacket(BodyPartData data) {
            this.data = data;
        }

        public static void encode(SyncBodyPartPacket packet, FriendlyByteBuf buffer) {
            int count = BodyPartType.values().length;
            for (int i = 0; i < count; i++) {
                buffer.writeFloat(packet.data.health[i]);
            }
            for (int i = 0; i < count; i++) {
                buffer.writeFloat(packet.data.maxHealth[i]);
            }
            for (int i = 0; i < count; i++) {
                buffer.writeInt(packet.data.bleeding[i]);
            }
        }

        public static SyncBodyPartPacket decode(FriendlyByteBuf buffer) {
            int count = BodyPartType.values().length;
            float[] health = new float[count];
            float[] maxHealth = new float[count];
            int[] bleeding = new int[count];
            for (int i = 0; i < count; i++) {
                health[i] = buffer.readFloat();
            }
            for (int i = 0; i < count; i++) {
                maxHealth[i] = buffer.readFloat();
            }
            for (int i = 0; i < count; i++) {
                bleeding[i] = buffer.readInt();
            }
            return new SyncBodyPartPacket(new BodyPartData(health, maxHealth, bleeding));
        }

        public static void handle(SyncBodyPartPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.getCapability(BodyPartCapabilityRegistry.BODY_PART_CAPABILITY).ifPresent(cap -> {
                        for (BodyPartType type : BodyPartType.values()) {
                            cap.setMaxHealth(type, packet.data.maxHealth[type.ordinal()]);
                            cap.setHealth(type, packet.data.health[type.ordinal()]);
                            cap.setBleedingTicks(type, packet.data.bleeding[type.ordinal()]);
                        }
                    });
                }
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 肢节血量数据容器
     */
    public static class BodyPartData {
        public final float[] health;
        public final float[] maxHealth;
        public final int[] bleeding;

        public BodyPartData(float[] health, float[] maxHealth, int[] bleeding) {
            this.health = health;
            this.maxHealth = maxHealth;
            this.bleeding = bleeding;
        }
    }
}
