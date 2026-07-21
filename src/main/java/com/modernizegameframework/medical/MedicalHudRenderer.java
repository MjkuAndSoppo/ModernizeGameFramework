package com.modernizegameframework.medical;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 医疗读条 HUD 渲染器
 * 在屏幕中央显示当前医疗物品读条进度
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MedicalHudRenderer {

    /**
     * 读条条宽度
     */
    private static final int BAR_WIDTH = 120;

    /**
     * 读条条高度
     */
    private static final int BAR_HEIGHT = 6;

    /**
     * 客户端当前读条会话进度，范围 [0, 1]
     */
    private static float progress = 0.0f;

    /**
     * 当前是否处于医疗读条
     */
    private static boolean active = false;

    /**
     * 当前读条物品名称
     */
    private static String itemName = "";

    /**
     * 当前读条阶段：true 表示使用读条（绿色），false 表示启用读条（蓝色）
     */
    private static boolean usagePhase = false;

    private MedicalHudRenderer() {
    }

    /**
     * 更新客户端读条状态
     *
     * @param progressValue 进度 0-1
     * @param activeValue   是否激活
     * @param name          物品名称
     * @param usage         是否为使用阶段
     */
    public static void setProgress(float progressValue, boolean activeValue, String name, boolean usage) {
        progress = progressValue;
        active = activeValue;
        itemName = name;
        usagePhase = usage;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        Player player = mc.player;
        if (player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2 + 20;

        int barLeft = centerX - BAR_WIDTH / 2;
        int barRight = barLeft + BAR_WIDTH;
        int fillRight = barLeft + (int) (BAR_WIDTH * progress);

        // 背景
        graphics.fill(barLeft, centerY, barRight, centerY + BAR_HEIGHT, 0xFF333333);
        // 填充：启用阶段蓝色，使用阶段绿色
        int fillColor = usagePhase ? 0xFF00FF00 : 0xFF00CED1;
        graphics.fill(barLeft, centerY, fillRight, centerY + BAR_HEIGHT, fillColor);
        // 边框
        graphics.renderOutline(barLeft, centerY, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF);

        // 物品名称
        if (!itemName.isEmpty()) {
            int nameWidth = mc.font.width(itemName);
            graphics.drawString(mc.font, itemName, centerX - nameWidth / 2, centerY - 12, 0xFFFFFF, false);
        }
    }
}
