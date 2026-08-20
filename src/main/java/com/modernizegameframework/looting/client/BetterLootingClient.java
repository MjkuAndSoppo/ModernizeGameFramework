package com.modernizegameframework.looting.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端主初始化类（Forge 版）。
 * 负责注册客户端专属的 Tick 事件。
 * 按键绑定由 {@link KeyInit#register(net.minecraftforge.client.event.RegisterKeyMappingsEvent)}
 * 在模组 MOD 事件总线的 RegisterKeyMappingsEvent 中注册（Forge 1.20.1 标准做法）。
 */
public class BetterLootingClient {

    public static void init() {
        // 1. 初始化核心逻辑
        Core.INSTANCE.init();

        // 2. 注册 Forge 客户端事件（Tick）
        MinecraftForge.EVENT_BUS.register(BetterLootingClient.class);
    }

    /**
     * 客户端 Tick 事件：驱动战利品核心逻辑与悬浮窗状态更新。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Core.INSTANCE.onClientTick(mc);
        }
    }
}