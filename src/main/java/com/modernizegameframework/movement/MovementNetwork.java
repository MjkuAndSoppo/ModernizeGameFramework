package com.modernizegameframework.movement;

import com.modernizegameframework.ModernizeGameFramework;
import com.modernizegameframework.stamina.StaminaRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 移动系统网络通道
 * 客户端通知服务端执行体力扣除（连跳消耗等）
 *
 * 关键设计：客户端权威移动
 * - 客户端检测跳跃并直接修改速度
 * - 通过 packet 通知服务端扣除体力
 * - JUMP_CONSUMED_THIS_TICK 标记防止 LivingJumpEvent 重复扣费
 */
public class MovementNetwork {

    /**
     * 网络协议版本号
     */
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 网络通道实例
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "movement"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /**
     * 消息 ID 计数器
     */
    private static int packetId = 0;

    /**
     * 本 tick 已通过 packet 扣过跳跃体力的玩家集合
     * 防止 LivingJumpEvent 重复扣除
     */
    public static final Set<UUID> JUMP_CONSUMED_THIS_TICK = new HashSet<>();

    /**
     * 注册网络消息
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, BhopConsumePacket.class,
                BhopConsumePacket::encode, BhopConsumePacket::decode, BhopConsumePacket::handle);
    }

    /**
     * 连跳体力消耗包（客户端 -> 服务端）
     * 客户端检测到跳跃成功后发送，服务端扣除体力并标记 JUMP_CONSUMED_THIS_TICK
     */
    public static class BhopConsumePacket {

        /**
         * 消耗类型：0=连跳，1=普通跳跃
         */
        private final int type;

        public BhopConsumePacket(int type) {
            this.type = type;
        }

        public static void encode(BhopConsumePacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeInt(packet.type);
        }

        public static BhopConsumePacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new BhopConsumePacket(buffer.readInt());
        }

        public static void handle(BhopConsumePacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
            net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                // 手持基岩时豁免
                if (MovementHelper.isBedrockInHand(player)) return;

                player.getCapability(StaminaRegistry.STAMINA_CAPABILITY).ifPresent(stamina -> {
                    if (packet.type == 0) {
                        // 连跳不消耗体力，只标记防止 LivingJumpEvent 重复扣费
                    } else {
                        // 普通跳跃消耗（体力耗尽时不扣）
                        if (!stamina.isDepleted()) {
                            stamina.onActionConsume(com.modernizegameframework.stamina.StaminaConfig.JUMP_COST.get());
                        }
                    }
                    // 标记本 tick 已处理过跳跃体力，防止 LivingJumpEvent 重复扣
                    JUMP_CONSUMED_THIS_TICK.add(player.getUUID());
                });
            });
            context.setPacketHandled(true);
        }
    }
}
