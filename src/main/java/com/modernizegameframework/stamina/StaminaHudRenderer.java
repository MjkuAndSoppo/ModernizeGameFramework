package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 体力值 HUD 渲染器
 * 在物品栏右侧绘制多行体力条，贴住底边，支持渐变颜色与从右往左缩短
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StaminaHudRenderer {

    /**
     * 每条体力条的高度（物品栏高度 22 的四分之一）
     */
    private static final int ROW_HEIGHT = 6;

    /**
     * 每行之间的间距
     */
    private static final int ROW_SPACING = 2;

    /**
     * 每点体力对应的像素宽度
     */
    private static final int PIXELS_PER_STAMINA = 2;

    /**
     * 体力条与物品栏右侧的间隙（1 像素，紧挨着但不相连）
     */
    private static final int HOTBAR_GAP = 1;

    /**
     * 渲染体力 HUD
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!StaminaConfig.ENABLED.get()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        StaminaHelper.getStamina(player).ifPresent(stamina -> {
            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = event.getWindow().getGuiScaledWidth();
            int screenHeight = event.getWindow().getGuiScaledHeight();

            double current = stamina.getCurrent();
            double max = stamina.getMax();
            double staminaPerRow = StaminaConfig.STAMINA_PER_ROW.get();

            if (max <= 0 || staminaPerRow <= 0) return;

            int rowCount = (int) Math.ceil(max / staminaPerRow);
            double percent = max <= 0 ? 0 : current / max;
            int barColor = calculateGradientColor(percent);

            // 体力条基准位置：贴住物品栏右侧和屏幕底边
            int hotbarRightX = screenWidth / 2 + 91;
            int baseX = hotbarRightX + HOTBAR_GAP;
            int baseY = screenHeight - ROW_HEIGHT;

            for (int row = 0; row < rowCount; row++) {
                double rowStart = row * staminaPerRow;
                double rowMax = Math.min(staminaPerRow, max - rowStart);
                double rowCurrent = Math.max(0, Math.min(current - rowStart, rowMax));

                int rowWidth = (int) Math.round(rowMax * PIXELS_PER_STAMINA);
                int fillWidth = (int) Math.round(rowCurrent * PIXELS_PER_STAMINA);

                // 超出默认体力的行向上堆叠
                int rowY = baseY - row * (ROW_HEIGHT + ROW_SPACING);

                // 从右往左缩短：左端固定贴住物品栏，右端随体力减少向左缩回
                if (fillWidth > 0) {
                    graphics.fill(baseX, rowY, baseX + fillWidth, rowY + ROW_HEIGHT, barColor);
                }
            }
        });
    }

    /**
     * 根据体力百分比计算渐变颜色
     * 100% 为绿色，0% 为红色
     *
     * @param percent 体力百分比 0.0 ~ 1.0
     * @return ARGB 颜色值
     */
    private static int calculateGradientColor(double percent) {
        percent = Math.max(0, Math.min(1, percent));
        int red = (int) Math.round(255 * (1 - percent));
        int green = (int) Math.round(255 * percent);
        return 0xFF000000 | (red << 16) | (green << 8);
    }
}
