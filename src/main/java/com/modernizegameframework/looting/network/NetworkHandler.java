package com.modernizegameframework.looting.network;

import com.modernizegameframework.looting.BetterLooting;
import com.modernizegameframework.looting.network.C2S.PacketBatchPickup;
import com.modernizegameframework.looting.network.C2S.PacketPlaceIntoSlot;
import com.modernizegameframework.looting.network.S2C.PacketSyncConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;

/**
 * 基于 Forge SimpleChannel 的网络数据包注册中心。
 * 负责在客户端和服务端之间建立通信频道。
 */
public class NetworkHandler {
    // 网络协议版本，用于客户端与服务端握手校验
    private static final String PROTOCOL_VERSION = "1";

    // 创建模组专属的主网络通道
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BetterLooting.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有自定义的网络数据包
     * 必须在模组初始化阶段调用
     */
    public static void register() {
        // 注册客户端向服务端发送的：批量拾取数据包 (C2S)
        INSTANCE.registerMessage(0, PacketBatchPickup.class,
                PacketBatchPickup::toBytes,
                PacketBatchPickup::new,
                PacketBatchPickup::handle);

        // 注册客户端向服务端发送的：拖拽放入指定槽位数据包 (C2S)
        INSTANCE.registerMessage(1, PacketPlaceIntoSlot.class,
                PacketPlaceIntoSlot::toBytes,
                PacketPlaceIntoSlot::new,
                PacketPlaceIntoSlot::handle);

        // 注册服务端向客户端同步的：配置更新数据包 (S2C)
        INSTANCE.registerMessage(2, PacketSyncConfig.class,
                PacketSyncConfig::toBytes,
                PacketSyncConfig::new,
                PacketSyncConfig::handle);
    }

    /**
     * 便捷方法：从客户端向服务端发送数据包
     */
    public static void sendToServer(Object msg) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }

    /**
     * 便捷方法：向指定玩家发送数据包
     */
    public static void sendToPlayer(ServerPlayer player, Object msg) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * 便捷方法：向多位玩家广播数据包
     */
    public static void sendToPlayers(Collection<ServerPlayer> players, Object msg) {
        for (ServerPlayer player : players) {
            sendToPlayer(player, msg);
        }
    }
}