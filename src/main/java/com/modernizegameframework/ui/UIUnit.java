package com.modernizegameframework.ui;

/**
 * 响应式相对单位工具类
 * 参考 CSS 中的 vw/vh/rem/em/percent 概念，将基准尺寸转换为当前屏幕下的像素值
 */
public final class UIUnit {

    private UIUnit() {
    }

    /**
     * 视口宽度百分比（vw），1vw = 窗口宽度的 1%
     *
     * @param value  百分比数值，例如 50 表示 50vw
     * @param width  当前窗口宽度（像素）
     * @return 转换后的像素值
     */
    public static int vw(float value, int width) {
        return Math.round(width * value / 100.0f);
    }

    /**
     * 视口高度百分比（vh），1vh = 窗口高度的 1%
     *
     * @param value  百分比数值
     * @param height 当前窗口高度（像素）
     * @return 转换后的像素值
     */
    public static int vh(float value, int height) {
        return Math.round(height * value / 100.0f);
    }

    /**
     * rem 单位，基于根元素字体大小（基准字体）的倍数
     *
     * @param value    rem 数值
     * @param baseFont 基准字体大小（像素）
     * @return 转换后的像素值
     */
    public static int rem(float value, int baseFont) {
        return Math.round(value * baseFont);
    }

    /**
     * em 单位，基于父元素字体大小的倍数
     *
     * @param value      em 数值
     * @param parentFont 父元素字体大小（像素）
     * @return 转换后的像素值
     */
    public static int em(float value, int parentFont) {
        return Math.round(value * parentFont);
    }

    /**
     * 百分比，基于给定参照尺寸的百分比
     *
     * @param value      百分比数值
     * @param reference  参照尺寸（像素）
     * @return 转换后的像素值
     */
    public static int percent(float value, int reference) {
        return Math.round(reference * value / 100.0f);
    }

    /**
     * 将数值限制在 [min, max] 范围内
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将数值限制在 [min, max] 范围内
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
