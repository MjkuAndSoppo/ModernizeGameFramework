package com.modernizegameframework.looting;

import com.modernizegameframework.looting.command.ModCommands;
import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.event.CommonEvents;
import com.modernizegameframework.looting.network.NetworkHandler;
import com.modernizegameframework.looting.network.S2C.PacketSyncConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 战利品提升（BetterLooting）功能的公共入口。
 * 负责初始化双端（客户端+服务端）通用的设置、网络和事件。
 * 由主模组 {@code ModernizeGameFramework} 在初始化阶段调用。
 */
public class BetterLooting {
    public static final String MODID = "better_looting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        // 1. 初始化配置文件 (自动删除旧json)
        BetterLootingConfig.init();

        // 2. 注册网络通道与数据包
        NetworkHandler.register();

        // 3. 注册通用事件监听器
        CommonEvents.init();

        // 4. 注册模组指令
        ModCommands.register();

        // 5. 注册玩家进出服务器事件
        MinecraftForge.EVENT_BUS.register(new PlayerConnectionHandler());

        LOGGER.info("Better Looting (MGF) Common Initialized.");
    }

    /**
     * 玩家加入/离开服务器事件处理器。
     */
    public static class PlayerConnectionHandler {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            if (!player.level().isClientSide()) {
                BetterLootingConfig config = BetterLootingConfig.get();
                PacketSyncConfig packet = new PacketSyncConfig(config.scanRangeXZ, config.scanRangeY);
                NetworkHandler.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, packet);
            }
        }

        @SubscribeEvent
        public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                BetterLootingConfig.get().serverScanRangeXZ = -1.0f;
                BetterLootingConfig.get().serverScanRangeY = -1.0f;
            }
        }
    }
}