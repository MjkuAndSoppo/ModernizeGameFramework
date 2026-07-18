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
 * 速度与连跳统计 HUD
 * 显示位置：屏幕右下角，物品栏上方
 * 四行独立显示：当前速度、平均速度(a)、峰值速度(max)、最远跳跃距离(d)
 */
@Mod.EventBusSubscriber(modid = com.modernizegameframework.ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpeedHudOverlay {

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("speed_hud", SPEED_HUD);
    }

    private static final IGuiOverlay SPEED_HUD = (gui, guiGraphics, partialTicks, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !MovementConfig.ENABLED.get()) return;

        Vec3 delta = player.getDeltaMovement();
        double currentSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z) * 20.0;

        Font font = mc.font;
        int lineHeight = font.lineHeight + 2;

        // 四行独立显示
        String line1 = String.format("%.2f m/s", currentSpeed);
        String line2 = String.format("a: %.2f", MovementClientEvents.bhopChainAvgSpeed);
        String line3 = String.format("max: %.2f", MovementClientEvents.bhopChainPeakSpeed);
        String line4 = String.format("d: %.2f m", MovementClientEvents.bhopChainLongestJump);

        // 计算最长行宽度
        int maxWidth = font.width(line1);
        maxWidth = Math.max(maxWidth, font.width(line2));
        maxWidth = Math.max(maxWidth, font.width(line3));
        maxWidth = Math.max(maxWidth, font.width(line4));

        // 渲染位置：右下角，物品栏上方
        int x = screenWidth - maxWidth - 4;
        int y = screenHeight - 40 - lineHeight * 3; // 往上挪给四行腾空间

        // 绘制半透明背景
        int totalHeight = lineHeight * 4 + 4;
        RenderSystem.enableBlend();
        guiGraphics.fill(x - 3, y - 2, x + maxWidth + 3, y + totalHeight, 0x80000000);

        // 绘制四行文字
        guiGraphics.drawString(font, line1, x, y, 0xFFFFFF, false);
        y += lineHeight;
        guiGraphics.drawString(font, line2, x, y, 0xAAAAAA, false);
        y += lineHeight;
        guiGraphics.drawString(font, line3, x, y, 0xAAAAAA, false);
        y += lineHeight;
        guiGraphics.drawString(font, line4, x, y, 0xAAAAAA, false);
    };
}