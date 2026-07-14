package com.modernizegameframework.movement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 速度显示 HUD
 * 在屏幕右下角显示当前水平速度（m/s）
 * MC 中 1 方块 = 1 米，每 tick 速度 × 20 = m/s
 */
@Mod.EventBusSubscriber(modid = com.modernizegameframework.ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpeedHudOverlay {

    /**
     * 注册速度 HUD 叠层
     */
    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("speed_hud", SPEED_HUD);
    }

    /**
     * 速度 HUD 渲染逻辑
     * 显示位置：屏幕右下角，物品栏上方
     */
    private static final IGuiOverlay SPEED_HUD = (gui, guiGraphics, partialTicks, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 未启用移动系统时不显示
        if (!MovementConfig.ENABLED.get()) return;

        // 计算水平速度（方块/tick → m/s，乘以 20）
        Vec3 delta = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z) * 20.0;

        // 格式化显示文本
        String text = String.format("%.2f m/s", horizontalSpeed);

        // 渲染位置：右下角，物品栏上方
        Font font = mc.font;
        int textWidth = font.width(text);
        int x = screenWidth - textWidth - 4;  // 距右边 4 像素
        int y = screenHeight - 40;             // 物品栏上方

        // 绘制阴影背景 + 文字
        RenderSystem.enableBlend();
        guiGraphics.fill(x - 3, y - 2, x + textWidth + 3, y + font.lineHeight + 2, 0x80000000);
        guiGraphics.drawString(font, text, x, y, 0xFFFFFF, false);
    };
}
