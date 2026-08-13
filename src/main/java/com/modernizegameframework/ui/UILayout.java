package com.modernizegameframework.ui;

/**
 * 响应式屏幕布局工具
 * 负责计算背包面板、底边栏等核心区域的位置与尺寸，
 * 并统一维护缩放系数，确保所有 UI 元素同步缩放。
 *
 * 设计规范：
 * - 背包面板保持 16:9 宽高比，居中显示，尽可能大且至少有两边紧贴窗口边缘
 * - 底边栏宽度等于窗口宽度，高度为窗口高度的 8%（限制在 40~60 像素之间）
 * - 面板内部使用基于面板尺寸的百分比进行相对定位
 * - 三段式布局：左装备区、中主仓库区、右容器区，均基于背包面板计算
 */
public class UILayout {

    /** 背包面板基准宽度（按 1600×900 设计） */
    public static final int PANEL_BASE_WIDTH = 1600;
    /** 背包面板基准高度（按 1600×900 设计） */
    public static final int PANEL_BASE_HEIGHT = 900;
    /** 面板基准宽高比 */
    public static final float PANEL_ASPECT_RATIO = 16.0f / 9.0f;

    /** 底边栏高度占窗口高度的比例 */
    public static final float BOTTOM_BAR_HEIGHT_RATIO = 0.08f;
    /** 底边栏最小高度（像素） */
    public static final int BOTTOM_BAR_MIN_HEIGHT = 40;
    /** 底边栏最大高度（像素） */
    public static final int BOTTOM_BAR_MAX_HEIGHT = 60;

    /** 面板外边缘留白（基准像素） */
    public static final int PANEL_MARGIN_BASE = 20;
    /** 顶部边栏基准高度 */
    public static final int TOP_BAR_BASE_HEIGHT = 40;

    /** 屏幕边缘留白基准值，供顶部/底部边栏计算使用 */
    public static final int MARGIN = 20;
    /** 顶部边栏基准高度 */
    public static final int TOP_BAR_HEIGHT = 40;
    /** 区域之间垂直间距基准值 */
    public static final int SECTION_GAP = 12;

    /**
     * 三段式子面板基准宽度。
     * 背包面板为 1600×900（16:9），将其横向三等分后每个子面板的宽度约为 533，
     * 高度为 900，因此每个子面板的宽高比为 533:900 ≈ 16:27。
     */
    private static final int LEFT_PANEL_BASE_WIDTH = 533;
    private static final int MIDDLE_PANEL_BASE_WIDTH = 533;
    private static final int RIGHT_PANEL_BASE_WIDTH =
            PANEL_BASE_WIDTH - LEFT_PANEL_BASE_WIDTH - MIDDLE_PANEL_BASE_WIDTH;

    /** 中部面板在背包面板内的起始 X 坐标 */
    private static final int MIDDLE_PANEL_BASE_X = LEFT_PANEL_BASE_WIDTH;
    /** 右侧面板在背包面板内的起始 X 坐标 */
    private static final int RIGHT_PANEL_BASE_X = MIDDLE_PANEL_BASE_X + MIDDLE_PANEL_BASE_WIDTH;

    private UILayout() {
    }

    /**
     * 计算当前屏幕下的全局缩放系数
     * 使用最小边适配，确保 16:9 面板完整显示
     */
    public static float scale(int screenWidth, int screenHeight) {
        float scaleX = (float) screenWidth / PANEL_BASE_WIDTH;
        float scaleY = (float) screenHeight / PANEL_BASE_HEIGHT;
        return Math.min(scaleX, scaleY);
    }

    /**
     * 按屏幕高度将基准像素值线性缩放。
     * 以 1080p 为基准，适合顶部边栏、玩家模型偏移等需要跟随窗口高度的元素。
     *
     * @param baseValue    基准像素值（按 1920×1080 设计）
     * @param screenHeight 当前窗口高度
     * @return 缩放后的像素值
     */
    public static int scaled(int baseValue, int screenHeight) {
        return Math.round(baseValue * ((float) screenHeight / 1080f));
    }

    /**
     * 计算单个槽位步进（槽位 16 + 默认间隙 2），按屏幕高度缩放。
     *
     * @param screenHeight 当前窗口高度
     * @return 缩放后的槽位步进
     */
    public static int slotStep(int screenHeight) {
        return scaled(18, screenHeight);
    }

    /**
     * 计算底边栏矩形
     */
    public static Rect bottomBar(int screenWidth, int screenHeight) {
        int height = Math.max(BOTTOM_BAR_MIN_HEIGHT,
                Math.min(BOTTOM_BAR_MAX_HEIGHT,
                        Math.round(screenHeight * BOTTOM_BAR_HEIGHT_RATIO)));
        return new Rect(0, screenHeight - height, screenWidth, height);
    }

    /**
     * 计算 16:9 背包面板矩形
     * 面板居中，尽可能大，保持比例
     */
    public static Rect backpackPanel(int screenWidth, int screenHeight) {
        int availableHeight = screenHeight - bottomBar(screenWidth, screenHeight).height();
        int availableWidth = screenWidth;

        // 按可用高度计算面板宽度
        int heightByHeight = availableHeight;
        int widthByHeight = Math.round(availableHeight * PANEL_ASPECT_RATIO);

        // 按可用宽度计算面板高度
        int widthByWidth = availableWidth;
        int heightByWidth = Math.round(availableWidth / PANEL_ASPECT_RATIO);

        int panelWidth;
        int panelHeight;
        if (widthByHeight <= availableWidth) {
            // 高度是限制因素，面板高度等于可用高度，宽度按比例
            panelWidth = widthByHeight;
            panelHeight = heightByHeight;
        } else {
            // 宽度是限制因素，面板宽度等于可用宽度，高度按比例
            panelWidth = widthByWidth;
            panelHeight = heightByWidth;
        }

        int x = (screenWidth - panelWidth) / 2;
        int y = (availableHeight - panelHeight) / 2;
        return new Rect(x, y, panelWidth, panelHeight);
    }

    /**
     * 计算左侧装备区面板矩形。
     * 位于背包面板左侧，顶部留出顶部边栏高度，避免与标签栏重叠。
     *
     * @param screenWidth  当前窗口宽度
     * @param screenHeight 当前窗口高度
     * @return 左侧面板矩形
     */
    public static Rect leftPanel(int screenWidth, int screenHeight) {
        Rect panel = backpackPanel(screenWidth, screenHeight);
        int topBarH = scaled(TOP_BAR_HEIGHT, screenHeight);
        int leftW = panelSize(LEFT_PANEL_BASE_WIDTH, panel);
        return new Rect(panel.x(), panel.y() + topBarH, leftW, panel.height() - topBarH);
    }

    /**
     * 计算中部主仓库区面板矩形。
     *
     * @param screenWidth  当前窗口宽度
     * @param screenHeight 当前窗口高度
     * @return 中部面板矩形
     */
    public static Rect middlePanel(int screenWidth, int screenHeight) {
        Rect panel = backpackPanel(screenWidth, screenHeight);
        int leftW = panelSize(LEFT_PANEL_BASE_WIDTH, panel);
        int middleW = panelSize(MIDDLE_PANEL_BASE_WIDTH, panel);
        return new Rect(panel.x() + leftW, panel.y(), middleW, panel.height());
    }

    /**
     * 计算右侧容器区面板矩形。
     * 当没有外部容器时返回零尺寸矩形。
     *
     * @param screenWidth   当前窗口宽度
     * @param screenHeight  当前窗口高度
     * @param hasContainer  是否打开外部容器
     * @return 右侧面板矩形
     */
    public static Rect rightPanel(int screenWidth, int screenHeight, boolean hasContainer) {
        if (!hasContainer) {
            return new Rect(0, 0, 0, 0);
        }
        Rect panel = backpackPanel(screenWidth, screenHeight);
        int leftW = panelSize(LEFT_PANEL_BASE_WIDTH, panel);
        int middleW = panelSize(MIDDLE_PANEL_BASE_WIDTH, panel);
        int rightW = panelSize(RIGHT_PANEL_BASE_WIDTH, panel);
        return new Rect(panel.x() + leftW + middleW, panel.y(), rightW, panel.height());
    }

    /**
     * 将基准坐标（按 PANEL_BASE_WIDTH×PANEL_BASE_HEIGHT 设计）转换为当前面板内的实际像素坐标
     *
     * @param baseX   基准 X 坐标
     * @param baseY   基准 Y 坐标
     * @param panel   当前面板矩形
     * @return 转换后的坐标
     */
    public static Point panelPoint(int baseX, int baseY, Rect panel) {
        float scaleX = (float) panel.width() / PANEL_BASE_WIDTH;
        float scaleY = (float) panel.height() / PANEL_BASE_HEIGHT;
        int x = panel.x() + Math.round(baseX * scaleX);
        int y = panel.y() + Math.round(baseY * scaleY);
        return new Point(x, y);
    }

    /**
     * 将基准尺寸（按 PANEL_BASE_WIDTH×PANEL_BASE_HEIGHT 设计）转换为当前面板内的实际像素尺寸
     */
    public static int panelSize(int baseSize, Rect panel) {
        return Math.round(baseSize * ((float) panel.width() / PANEL_BASE_WIDTH));
    }

    /**
     * 将基准尺寸转换为当前屏幕缩放后的像素值
     */
    public static int screenSize(int baseSize, int screenWidth, int screenHeight) {
        return Math.round(baseSize * scale(screenWidth, screenHeight));
    }

    /**
     * 计算快捷栏在底边栏中的居中起始 X 坐标
     *
     * @param slotSize 单个槽位尺寸
     * @param slotGap  槽位间隙
     * @param screenWidth 当前窗口宽度
     */
    public static int hotbarStartX(int slotSize, int slotGap, int screenWidth) {
        int hotbarWidth = 9 * slotSize + 8 * slotGap;
        return (screenWidth - hotbarWidth) / 2;
    }

    /**
     * 计算快捷栏在底边栏中的居中起始 Y 坐标
     *
     * @param slotSize    单个槽位尺寸
     * @param screenWidth 当前窗口宽度
     * @param screenHeight 当前窗口高度
     */
    public static int hotbarStartY(int slotSize, int screenWidth, int screenHeight) {
        Rect bar = bottomBar(screenWidth, screenHeight);
        return bar.y() + (bar.height() - slotSize) / 2;
    }

    /**
     * 矩形数据结构
     */
    public record Rect(int x, int y, int width, int height) {
    }

    /**
     * 点数据结构
     */
    public record Point(int x, int y) {
    }
}
