package com.modernizegameframework.ui;

/**
 * 统一的缩放控制工具
 * 负责根据参考设计尺寸和当前屏幕尺寸计算全局缩放系数，
 * 并提供像素到缩放后像素的转换方法，确保背包面板与底边栏缩放同步。
 */
public final class UIScale {

    /** 默认参考设计宽度（基准分辨率 1920×1080） */
    public static final int BASE_WIDTH = 1920;
    /** 默认参考设计高度（基准分辨率 1920×1080） */
    public static final int BASE_HEIGHT = 1080;
    /** 最小缩放系数，防止 UI 过小 */
    public static final float MIN_SCALE = 0.5f;
    /** 最大缩放系数，防止 UI 过大 */
    public static final float MAX_SCALE = 2.0f;

    private UIScale() {
    }

    /**
     * 计算基于高度的缩放系数
     * 以 1080p 为基准，窗口越高缩放越大
     *
     * @param screenHeight 当前窗口高度
     * @return 缩放系数
     */
    public static float byHeight(int screenHeight) {
        return UIUnit.clamp((float) screenHeight / BASE_HEIGHT, MIN_SCALE, MAX_SCALE);
    }

    /**
     * 计算基于宽度的缩放系数
     * 以 1920 宽度为基准
     *
     * @param screenWidth 当前窗口宽度
     * @return 缩放系数
     */
    public static float byWidth(int screenWidth) {
        return UIUnit.clamp((float) screenWidth / BASE_WIDTH, MIN_SCALE, MAX_SCALE);
    }

    /**
     * 计算基于最小边的缩放系数，保证 16:9 面板整体比例
     *
     * @param screenWidth  当前窗口宽度
     * @param screenHeight 当前窗口高度
     * @return 缩放系数
     */
    public static float byMinSide(int screenWidth, int screenHeight) {
        float scaleX = (float) screenWidth / BASE_WIDTH;
        float scaleY = (float) screenHeight / BASE_HEIGHT;
        return UIUnit.clamp(Math.min(scaleX, scaleY), MIN_SCALE, MAX_SCALE);
    }

    /**
     * 将基准像素值转换为缩放后的像素值
     *
     * @param baseValue 基准像素值（按 1920×1080 设计）
     * @param scale     当前缩放系数
     * @return 缩放后的像素值
     */
    public static int px(int baseValue, float scale) {
        return Math.round(baseValue * scale);
    }

    /**
     * 根据当前屏幕尺寸直接转换基准像素值
     */
    public static int pxByMinSide(int baseValue, int screenWidth, int screenHeight) {
        return px(baseValue, byMinSide(screenWidth, screenHeight));
    }
}
