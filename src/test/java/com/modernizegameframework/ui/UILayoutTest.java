package com.modernizegameframework.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UILayout 单元测试
 * 验证背包面板 16:9 约束、底边栏高度 clamp、子面板布局以及快捷栏居中计算。
 */
class UILayoutTest {

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void backpackPanel_保持16比9宽高比(int width, int height) {
        UILayout.Rect panel = UILayout.backpackPanel(width, height);
        float aspect = (float) panel.width() / panel.height();
        assertEquals(16.0f / 9.0f, aspect, 0.02f,
                "分辨率 " + width + "x" + height + " 下背包面板应保持 16:9");
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void backpackPanel_居中显示(int width, int height) {
        UILayout.Rect panel = UILayout.backpackPanel(width, height);
        assertEquals((width - panel.width()) / 2, panel.x(),
                "分辨率 " + width + "x" + height + " 下面板应水平居中");
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768, 60",
            "1920, 1080, 60",
            "2560, 1440, 60",
            "800, 600, 48",
            "640, 480, 40"
    })
    void bottomBar_高度为窗口高度8百分比且限制在40到60像素(int width, int height, int expectedHeight) {
        UILayout.Rect bar = UILayout.bottomBar(width, height);
        assertEquals(0, bar.x());
        assertEquals(width, bar.width());
        assertEquals(height - expectedHeight, bar.y());
        assertEquals(expectedHeight, bar.height());
    }

    @Test
    void backpackPanel_至少有两边紧贴可用区域边缘() {
        // 1920×1080：底边栏高 60，可用高度 1020
        UILayout.Rect panel = UILayout.backpackPanel(1920, 1080);
        UILayout.Rect bar = UILayout.bottomBar(1920, 1080);
        int availableHeight = 1080 - bar.height();

        // 面板高度应等于可用高度（上下两边贴紧）
        assertEquals(availableHeight, panel.height());
        // 面板宽度应小于窗口宽度，左右留边
        assertTrue(panel.width() <= 1920);
    }

    @Test
    void panelPoint_将基准坐标映射到当前面板内() {
        UILayout.Rect panel = new UILayout.Rect(100, 50, 1600, 900);
        UILayout.Point point = UILayout.panelPoint(800, 450, panel);
        assertEquals(900, point.x());
        assertEquals(500, point.y());
    }

    @Test
    void panelSize_将基准尺寸映射到当前面板尺寸() {
        UILayout.Rect panel = new UILayout.Rect(0, 0, 800, 450);
        assertEquals(267, UILayout.panelSize(533, panel));
        assertEquals(267, UILayout.panelSize(533, panel));
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void 子面板_左中右三个面板宽高比均为16比27(int width, int height) {
        UILayout.Rect left = UILayout.leftPanel(width, height);
        UILayout.Rect middle = UILayout.middlePanel(width, height);
        UILayout.Rect right = UILayout.rightPanel(width, height, true);

        float expected = 16.0f / 27.0f;
        assertEquals(expected, (float) left.width() / left.height(), 0.03f,
                "左侧面板宽高比应为 16:27");
        assertEquals(expected, (float) middle.width() / middle.height(), 0.03f,
                "中部面板宽高比应为 16:27");
        assertEquals(expected, (float) right.width() / right.height(), 0.03f,
                "右侧面板宽高比应为 16:27");
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void leftPanel_位于背包面板左侧并在顶部留出边栏空间(int width, int height) {
        UILayout.Rect panel = UILayout.backpackPanel(width, height);
        UILayout.Rect left = UILayout.leftPanel(width, height);

        assertEquals(panel.x(), left.x());
        assertTrue(left.y() >= panel.y());
        assertTrue(left.width() > 0);
        assertTrue(left.height() > 0);
        assertTrue(left.x() + left.width() <= panel.x() + panel.width());
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void middlePanel_位于背包面板中部(int width, int height) {
        UILayout.Rect left = UILayout.leftPanel(width, height);
        UILayout.Rect middle = UILayout.middlePanel(width, height);

        assertEquals(left.x() + left.width(), middle.x());
        assertTrue(middle.width() > 0);
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void rightPanel_存在容器时位于背包面板右侧(int width, int height) {
        UILayout.Rect middle = UILayout.middlePanel(width, height);
        UILayout.Rect right = UILayout.rightPanel(width, height, true);

        assertEquals(middle.x() + middle.width(), right.x());
        assertTrue(right.width() > 0);
    }

    @Test
    void rightPanel_无容器时返回零尺寸() {
        UILayout.Rect right = UILayout.rightPanel(1920, 1080, false);
        assertEquals(0, right.width());
        assertEquals(0, right.height());
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 2",
            "1920, 2",
            "2560, 2"
    })
    void hotbarStartX_快捷栏在底边栏内水平居中(int screenWidth, int slotGap) {
        int slotSize = 16;
        int hotbarWidth = 9 * slotSize + 8 * slotGap;
        int expectedX = (screenWidth - hotbarWidth) / 2;
        assertEquals(expectedX, UILayout.hotbarStartX(slotSize, slotGap, screenWidth));
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void hotbarStartY_快捷栏在底边栏内垂直居中(int width, int height) {
        int slotSize = 16;
        UILayout.Rect bar = UILayout.bottomBar(width, height);
        int expectedY = bar.y() + (bar.height() - slotSize) / 2;
        assertEquals(expectedY, UILayout.hotbarStartY(slotSize, width, height));
    }

    @ParameterizedTest
    @CsvSource({
            "1366, 768",
            "1920, 1080",
            "2560, 1440"
    })
    void scaled_按屏幕高度线性缩放(int width, int height) {
        // 以 1080p 为基准，100 像素在不同高度下的缩放值
        int expected = Math.round(100 * ((float) height / 1080f));
        assertEquals(expected, UILayout.scaled(100, height));
    }
}
