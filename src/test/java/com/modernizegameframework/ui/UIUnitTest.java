package com.modernizegameframework.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UIUnit 单元测试
 * 验证相对单位转换与 clamp 工具的正确性。
 */
class UIUnitTest {

    @Test
    void vw_返回视口宽度百分比() {
        assertEquals(960, UIUnit.vw(50, 1920));
        assertEquals(683, UIUnit.vw(50, 1366));
        assertEquals(1280, UIUnit.vw(50, 2560));
    }

    @Test
    void vh_返回视口高度百分比() {
        assertEquals(540, UIUnit.vh(50, 1080));
        assertEquals(384, UIUnit.vh(50, 768));
        assertEquals(720, UIUnit.vh(50, 1440));
    }

    @Test
    void rem_返回基准字体倍数() {
        assertEquals(32, UIUnit.rem(2, 16));
        assertEquals(24, UIUnit.rem(1.5f, 16));
    }

    @Test
    void em_返回父元素字体倍数() {
        assertEquals(36, UIUnit.em(3, 12));
        assertEquals(18, UIUnit.em(1.5f, 12));
    }

    @Test
    void percent_返回参照尺寸百分比() {
        assertEquals(250, UIUnit.percent(25, 1000));
        assertEquals(500, UIUnit.percent(50, 1000));
    }

    @Test
    void clampInt_将数值限制在范围内() {
        assertEquals(10, UIUnit.clamp(5, 10, 20));
        assertEquals(15, UIUnit.clamp(15, 10, 20));
        assertEquals(20, UIUnit.clamp(25, 10, 20));
    }

    @Test
    void clampFloat_将数值限制在范围内() {
        assertEquals(0.5f, UIUnit.clamp(0.1f, 0.5f, 2.0f), 0.0001f);
        assertEquals(1.5f, UIUnit.clamp(1.5f, 0.5f, 2.0f), 0.0001f);
        assertEquals(2.0f, UIUnit.clamp(3.0f, 0.5f, 2.0f), 0.0001f);
    }
}
