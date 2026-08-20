package com.modernizegameframework.looting.network.S2C;

import com.modernizegameframework.looting.config.BetterLootingConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端下发给客户端的同步配置数据包 (Server -> Client)
 */
public class PacketSyncConfig {
    private final float scanRangeXZ;
    private final float scanRangeY;

    // 构造器 (发送端使用)
    public PacketSyncConfig(float scanRangeXZ, float scanRangeY) {
        this.scanRangeXZ = scanRangeXZ;
        this.scanRangeY = scanRangeY;
    }

    // 反序列化构造器 (接收端使用)
    public PacketSyncConfig(FriendlyByteBuf buf) {
        this.scanRangeXZ = buf.readFloat();
        this.scanRangeY = buf.readFloat();
    }

    // 序列化
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.scanRangeXZ);
        buf.writeFloat(this.scanRangeY);
    }

    // 数据包处理逻辑 (在客户端执行)
    public static void handle(PacketSyncConfig msg, Supplier<NetworkEvent.Context> ctx) {
        // 将任务推送到客户端主线程执行策安全操作
        ctx.get().enqueueWork(() -> {
            BetterLootingConfig.get().serverScanRangeXZ = msg.scanRangeXZ;
            BetterLootingConfig.get().serverScanRangeY = msg.scanRangeY;
        });
        ctx.get().setPacketHandled(true);
    }
}