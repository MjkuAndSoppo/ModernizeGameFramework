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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 藏身处工作方块网络通道
 * 负责控制箱 UI 的打开请求与数据同步
 */
public class HollowHouseWorkBlockNetwork {

    /**
     * 网络协议版本号
     */
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 网络通道实例
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "hollow_house_work_block"),
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
        CHANNEL.registerMessage(packetId++, OpenWorkBlockScreenPacket.class,
                OpenWorkBlockScreenPacket::encode, OpenWorkBlockScreenPacket::decode, OpenWorkBlockScreenPacket::handle);
        CHANNEL.registerMessage(packetId++, UpgradeWorkBlockPacket.class,
                UpgradeWorkBlockPacket::encode, UpgradeWorkBlockPacket::decode, UpgradeWorkBlockPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncWorkBlockDataPacket.class,
                SyncWorkBlockDataPacket::encode, SyncWorkBlockDataPacket::decode, SyncWorkBlockDataPacket::handle);
    }

    /**
     * 向客户端发送打开控制箱 UI 的数据包
     */
    public static void openScreen(ServerPlayer player, Map<String, Integer> levels) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenWorkBlockScreenPacket(levels));
    }

    /**
     * 向客户端同步工作方块等级数据
     */
    public static void syncToClient(ServerPlayer player, Map<String, Integer> levels) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncWorkBlockDataPacket(levels));
    }

    /**
     * 打开控制箱 UI 数据包
     */
    public static class OpenWorkBlockScreenPacket {

        private final Map<String, Integer> workBlockLevels;

        public OpenWorkBlockScreenPacket(Map<String, Integer> workBlockLevels) {
            this.workBlockLevels = new HashMap<>(workBlockLevels);
        }

        public static void encode(OpenWorkBlockScreenPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.workBlockLevels.size());
            for (Map.Entry<String, Integer> entry : packet.workBlockLevels.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeInt(entry.getValue());
            }
        }

        public static OpenWorkBlockScreenPacket decode(FriendlyByteBuf buffer) {
            Map<String, Integer> levels = new HashMap<>();
            int size = buffer.readInt();
            for (int i = 0; i < size; i++) {
                String id = buffer.readUtf();
                int level = buffer.readInt();
                levels.put(id, level);
            }
            return new OpenWorkBlockScreenPacket(levels);
        }

        public static void handle(OpenWorkBlockScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> openClientScreen(packet.workBlockLevels));
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private static void openClientScreen(Map<String, Integer> levels) {
            Minecraft.getInstance().setScreen(new HollowHouseWorkBlockScreen(levels));
        }
    }

    /**
     * 升级/解锁工作方块请求数据包
     */
    public static class UpgradeWorkBlockPacket {

        private final String workBlockId;

        public UpgradeWorkBlockPacket(String workBlockId) {
            this.workBlockId = workBlockId;
        }

        public static void encode(UpgradeWorkBlockPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.workBlockId);
        }

        public static UpgradeWorkBlockPacket decode(FriendlyByteBuf buffer) {
            return new UpgradeWorkBlockPacket(buffer.readUtf());
        }

        public static void handle(UpgradeWorkBlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                HollowHouseWorkBlockManager.tryUpgradeWorkBlock(player, packet.workBlockId);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 同步工作方块等级数据包
     */
    public static class SyncWorkBlockDataPacket {

        private final Map<String, Integer> workBlockLevels;

        public SyncWorkBlockDataPacket(Map<String, Integer> workBlockLevels) {
            this.workBlockLevels = new HashMap<>(workBlockLevels);
        }

        public static void encode(SyncWorkBlockDataPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.workBlockLevels.size());
            for (Map.Entry<String, Integer> entry : packet.workBlockLevels.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeInt(entry.getValue());
            }
        }

        public static SyncWorkBlockDataPacket decode(FriendlyByteBuf buffer) {
            Map<String, Integer> levels = new HashMap<>();
            int size = buffer.readInt();
            for (int i = 0; i < size; i++) {
                String id = buffer.readUtf();
                int level = buffer.readInt();
                levels.put(id, level);
            }
            return new SyncWorkBlockDataPacket(levels);
        }

        public static void handle(SyncWorkBlockDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().screen instanceof HollowHouseWorkBlockScreen screen) {
                    screen.updateLevels(packet.workBlockLevels);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
