package com.modernizegameframework.looting.client;

import com.modernizegameframework.looting.client.inventory.InventoryLootList;
import com.modernizegameframework.looting.client.overlay.HotbarIndicator;
import com.modernizegameframework.looting.client.overlay.Overlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端主初始化类（Forge 版）。
 * 负责注册客户端专属的按键绑定、Tick 与 HUD 渲染事件。
 * 按键绑定由 {@link KeyInit#register(net.minecraftforge.client.event.RegisterKeyMappingsEvent)}
 * 在模组 MOD 事件总线的 RegisterKeyMappingsEvent 中注册（Forge 1.20.1 标准做法）。
 */
public class BetterLootingClient {

    public static void init() {
        // 1. 初始化核心逻辑与物品栏列表
        Core.INSTANCE.init();
        InventoryLootList.INSTANCE.init();

        // 2. 注册 Forge 客户端事件（Tick 与 HUD 渲染）
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
            Overlay.INSTANCE.onTick(mc);
        }
    }

    /**
     * HUD 渲染事件：在主界面绘制战利品悬浮窗与模式指示器。
     */
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        GuiGraphics gui = event.getGuiGraphics();
        float partialTick = event.getPartialTick();
        Overlay.INSTANCE.render(gui, partialTick);
        HotbarIndicator.INSTANCE.render(gui, partialTick);
    }
}