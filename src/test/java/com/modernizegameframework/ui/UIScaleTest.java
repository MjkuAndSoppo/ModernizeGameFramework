package com.modernizegameframework.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UIScale 单元测试
 * 验证基于屏幕尺寸的缩放系数计算与像素转换。
 */
class UIScaleTest {

    @Test
    void byHeight_以1080p为基准计算高度缩放() {
        assertEquals(1.0f, UIScale.byHeight(1080), 0.0001f);
        assertEquals(0.7111f, UIScale.byHeight(768), 0.0001f);
        assertEquals(1.3333f, UIScale.byHeight(1440), 0.0001f);
    }

    @Test
    void byHeight_缩放系数受上下限约束() {
        // 低于下限 0.5 时取 0.5
        assertEquals(0.5f, UIScale.byHeight(200), 0.0001f);
        // 高于上限 2.0 时取 2.0
        assertEquals(2.0f, UIScale.byHeight(3000), 0.0001f);
    }

    @Test
    void byWidth_以1920为基准计算宽度缩放() {
        assertEquals(1.0f, UIScale.byWidth(1920), 0.0001f);
        assertEquals(0.7114f, UIScale.byWidth(1366), 0.0001f);
        assertEquals(1.3333f, UIScale.byWidth(2560), 0.0001f);
    }

    @Test
    void byMinSide_取宽高缩放较小值() {
        // 1920×1080：x=1.0, y=1.0，min=1.0
        assertEquals(1.0f, UIScale.byMinSide(1920, 1080), 0.0001f);
        // 1366×768：x≈0.711, y≈0.711，min≈0.711
        assertEquals(0.7111f, UIScale.byMinSide(1366, 768), 0.0001f);
        // 2560×1440：x≈1.333, y≈1.333，min≈1.333
        assertEquals(1.3333f, UIScale.byMinSide(2560, 1440), 0.0001f);
        // 1920×1440：x=1.0, y≈1.333，min=1.0
        assertEquals(1.0f, UIScale.byMinSide(1920, 1440), 0.0001f);
    }

    @Test
    void byMinSide_缩放系数受上下限约束() {
        assertEquals(0.5f, UIScale.byMinSide(640, 480), 0.0001f);
        assertEquals(2.0f, UIScale.byMinSide(3840, 2160), 0.0001f);
    }

    @Test
    void px_将基准像素按缩放系数转换() {
        assertEquals(100, UIScale.px(100, 1.0f));
        assertEquals(50, UIScale.px(100, 0.5f));
        assertEquals(200, UIScale.px(100, 2.0f));
    }

    @Test
    void pxByMinSide_根据屏幕尺寸直接转换基准像素() {
        assertEquals(100, UIScale.pxByMinSide(100, 1920, 1080));
        assertEquals(71, UIScale.pxByMinSide(100, 1366, 768));
        assertEquals(133, UIScale.pxByMinSide(100, 2560, 1440));
    }
}
