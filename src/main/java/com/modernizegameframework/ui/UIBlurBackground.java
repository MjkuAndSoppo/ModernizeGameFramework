package com.modernizegameframework.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 毛玻璃背景效果工具
 * 通过半透明灰黑色叠加层模拟毛玻璃质感
 * 后续可扩展为着色器模糊背景
 */
public class UIBlurBackground {

    /** 背景叠加颜色：深灰色半透明，模拟毛玻璃暗化效果 */
    public static final int DEFAULT_OVERLAY = 0xD8000000;
    /** 更亮的毛玻璃叠加 */
    public static final int LIGHT_OVERLAY = 0xB03A3A3A;

    private UIBlurBackground() {
    }

    /**
     * 在全屏绘制毛玻璃背景
     *
     * @param graphics 渲染上下文
     * @param width    屏幕宽度
     * @param height   屏幕高度
     * @param color    叠加颜色
     */
    public static void render(GuiGraphics graphics, int width, int height, int color) {
        graphics.fill(0, 0, width, height, color);
    }

    /**
     * 在全屏绘制默认深色毛玻璃背景
     */
    public static void render(GuiGraphics graphics, int width, int height) {
        render(graphics, width, height, DEFAULT_OVERLAY);
    }

    /**
     * 在指定区域绘制毛玻璃背景
     */
    public static void renderRegion(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }
}
