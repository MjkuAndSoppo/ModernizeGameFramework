package com.modernizegameframework.hollowhouse;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 藏身处控制箱工作方块管理界面
 * 使用 GuiGraphics 直接绘制，不再依赖旧 UI 组件库
 * 布局：左侧栏 + 主面板
 */
public class HollowHouseWorkBlockScreen extends Screen {

    /** 原版工作台 GUI 尺寸 */
    private static final int MAIN_PANEL_WIDTH = 176;
    private static final int MAIN_PANEL_HEIGHT = 166;

    /** 左侧栏宽度 = 主面板宽度 × 0.5 */
    private static final int SIDEBAR_WIDTH = 88;
    /** 左侧栏高度与主面板相同 */
    private static final int SIDEBAR_HEIGHT = MAIN_PANEL_HEIGHT;

    /** 左侧面板与主面板之间的间隙 */
    private static final int PANEL_GAP = 4;

    /** 滚动条宽度 */
    private static final int SCROLLBAR_WIDTH = 6;

    /** 左侧栏背景色 */
    private static final int SIDEBAR_BG = 0xFF2A2A2A;
    /** 左侧栏边框色 */
    private static final int SIDEBAR_BORDER = 0xFF555555;
    /** 主面板背景色 */
    private static final int MAIN_PANEL_BG = 0xB02A2A2A;
    /** 主面板边框色 */
    private static final int MAIN_PANEL_BORDER = 0xFF555555;

    /** 整个界面左上角坐标 */
    private int leftPos;
    private int topPos;

    /** 主面板左上角坐标 */
    private int mainPanelX;
    private int mainPanelY;

    /** 升级/解锁按钮 */
    private Button upgradeButton;

    /** 工作方块等级数据 */
    private final Map<String, Integer> workBlockLevels;
    /** 当前选中的工作方块 */
    private HollowHouseWorkBlockType selectedType;
    /** 左侧栏条目列表 */
    private final List<HollowHouseWorkBlockEntry> entryList = new ArrayList<>();

    // ===== 滚动状态 =====
    /** 当前滚动偏移量 */
    private int scrollOffset = 0;
    /** 左侧栏内容总高度 */
    private int contentHeight = 0;
    /** 是否正在拖拽滚动条 */
    private boolean draggingScrollbar = false;
    /** 拖拽开始时鼠标 Y 对应的滚动偏移量 */
    private double dragStartYOffset = 0;

    public HollowHouseWorkBlockScreen(Map<String, Integer> levels) {
        super(Component.literal("藏身处工作方块"));
        this.workBlockLevels = new HashMap<>(levels);
        this.selectedType = HollowHouseWorkBlockType.STOREHOUSE;
    }

    @Override
    protected void init() {
        // 计算整个界面居中位置
        int totalWidth = SIDEBAR_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH;
        this.leftPos = (this.width - totalWidth) / 2;
        this.topPos = (this.height - MAIN_PANEL_HEIGHT) / 2;

        this.mainPanelX = leftPos + SIDEBAR_WIDTH + PANEL_GAP;
        this.mainPanelY = topPos;

        // 升级/解锁按钮，放在主面板底部居中
        int buttonWidth = 70;
        int buttonX = mainPanelX + (MAIN_PANEL_WIDTH - buttonWidth) / 2;
        int buttonY = mainPanelY + MAIN_PANEL_HEIGHT - 30;
        upgradeButton = Button.builder(Component.literal("解锁"), btn -> sendUpgrade())
                .bounds(buttonX, buttonY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(upgradeButton);

        buildSidebarEntries();
        updateUpgradeButton();
    }

    /**
     * 构建左侧栏条目
     */
    private void buildSidebarEntries() {
        entryList.clear();

        HollowHouseWorkBlockType[] types = HollowHouseWorkBlockType.values();
        int entryWidth = SIDEBAR_WIDTH - SCROLLBAR_WIDTH - 1;
        this.contentHeight = Math.max(SIDEBAR_HEIGHT, types.length * HollowHouseWorkBlockEntry.ENTRY_HEIGHT);

        for (int i = 0; i < types.length; i++) {
            HollowHouseWorkBlockType type = types[i];
            int level = workBlockLevels.getOrDefault(type.getId(), 0);
            HollowHouseWorkBlockEntry entry = new HollowHouseWorkBlockEntry(
                    leftPos, topPos + i * HollowHouseWorkBlockEntry.ENTRY_HEIGHT,
                    entryWidth, type, level);

            entry.setSelected(type == selectedType);
            entry.setOnClick(this::onEntrySelected);

            entryList.add(entry);
        }

        // 重置滚动偏移
        scrollOffset = 0;
    }

    /**
     * 左侧栏条目点击回调
     */
    private void onEntrySelected(HollowHouseWorkBlockType type) {
        this.selectedType = type;
        for (HollowHouseWorkBlockEntry entry : entryList) {
            entry.setSelected(entry.getWorkBlockType() == type);
        }
        updateUpgradeButton();
    }

    /**
     * 根据类型查找对应条目
     */
    private HollowHouseWorkBlockEntry findEntry(HollowHouseWorkBlockType type) {
        for (HollowHouseWorkBlockEntry entry : entryList) {
            if (entry != null && entry.getWorkBlockType() == type) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 发送升级/解锁请求到服务端
     */
    private void sendUpgrade() {
        if (selectedType == null) {
            return;
        }
        HollowHouseWorkBlockNetwork.CHANNEL.sendToServer(
                new HollowHouseWorkBlockNetwork.UpgradeWorkBlockPacket(selectedType.getId()));
    }

    /**
     * 更新升级按钮文字和可用状态
     */
    private void updateUpgradeButton() {
        if (selectedType == null || upgradeButton == null) {
            return;
        }
        int level = workBlockLevels.getOrDefault(selectedType.getId(), 0);
        if (level >= selectedType.getMaxLevel()) {
            upgradeButton.setMessage(Component.literal("已满级"));
            upgradeButton.active = false;
        } else {
            String action = level == 0 ? "解锁" : "升级";
            upgradeButton.setMessage(Component.literal(action));
            upgradeButton.active = true;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制毛玻璃背景（替换 UIBlurBackground）
        graphics.fillGradient(0, 0, this.width, this.height, 0xAA0A0A0A, 0xAA0A0A0A);

        // 渲染标题
        graphics.drawString(this.font, this.title,
                leftPos + (SIDEBAR_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH) / 2 - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // 渲染左侧栏背景和边框
        graphics.fill(leftPos, topPos, leftPos + SIDEBAR_WIDTH, topPos + SIDEBAR_HEIGHT, SIDEBAR_BG);
        graphics.renderOutline(leftPos, topPos, SIDEBAR_WIDTH, SIDEBAR_HEIGHT, SIDEBAR_BORDER);

        // 渲染左侧栏条目（带滚动裁剪）
        graphics.enableScissor(leftPos, topPos, leftPos + SIDEBAR_WIDTH, topPos + SIDEBAR_HEIGHT);
        for (int i = 0; i < entryList.size(); i++) {
            HollowHouseWorkBlockEntry entry = entryList.get(i);
            int baseY = topPos + i * HollowHouseWorkBlockEntry.ENTRY_HEIGHT;
            entry.y = baseY - scrollOffset;
            entry.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();

        // 渲染滚动条
        renderScrollbar(graphics);

        // 渲染主面板背景和边框
        graphics.fill(mainPanelX, mainPanelY, mainPanelX + MAIN_PANEL_WIDTH, mainPanelY + MAIN_PANEL_HEIGHT, MAIN_PANEL_BG);
        graphics.renderOutline(mainPanelX, mainPanelY, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT, MAIN_PANEL_BORDER);

        // 渲染主面板内容
        renderMainPanelContent(graphics);

        // 渲染原版按钮
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染滚动条（参考 TarkovInventoryScreen 风格）
     */
    private void renderScrollbar(GuiGraphics graphics) {
        if (contentHeight <= SIDEBAR_HEIGHT) {
            return;
        }
        int scrollbarX = leftPos + SIDEBAR_WIDTH - SCROLLBAR_WIDTH - 1;
        int scrollbarY = topPos + 1;
        int scrollbarHeight = SIDEBAR_HEIGHT - 2;
        // 滚动条背景
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF222222);
        // 滚动条滑块
        float ratio = (float) SIDEBAR_HEIGHT / contentHeight;
        int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
        int maxOffset = contentHeight - SIDEBAR_HEIGHT;
        int thumbY = scrollbarY + (maxOffset == 0 ? 0
                : (int) ((scrollbarHeight - thumbHeight) * ((float) scrollOffset / maxOffset)));
        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF777777);
    }

    /**
     * 渲染主面板内容
     */
    private void renderMainPanelContent(GuiGraphics graphics) {
        if (selectedType == null) {
            return;
        }

        int level = workBlockLevels.getOrDefault(selectedType.getId(), 0);
        boolean unlocked = level > 0;
        boolean maxed = level >= selectedType.getMaxLevel();

        int textX = mainPanelX + 10;
        int textY = mainPanelY + 10;
        int lineHeight = 12;

        // 标题
        graphics.drawString(this.font, Component.literal("§n" + selectedType.getDisplayName()),
                textX, textY, 0xFFFFFFFF, false);
        textY += lineHeight + 4;

        // 当前等级
        String levelText = unlocked ? ("当前等级：§a" + level + " §7/ §a" + selectedType.getMaxLevel())
                : "当前等级：§c未解锁";
        graphics.drawString(this.font, Component.literal(levelText), textX, textY, 0xFFFFFFFF, false);
        textY += lineHeight;

        // 解锁/升级消耗
        if (!maxed) {
            int targetLevel = level + 1;
            int cost = selectedType.getUpgradeCost(targetLevel);
            String action = level == 0 ? "解锁消耗" : "升级消耗";
            graphics.drawString(this.font, Component.literal(action + "：§e" + cost + " §7级经验"),
                    textX, textY, 0xFFFFFFFF, false);
            textY += lineHeight;
        } else {
            graphics.drawString(this.font, Component.literal("§a已达到最高等级"), textX, textY, 0xFFFFFFFF, false);
            textY += lineHeight;
        }

        // 分隔线
        textY += 4;
        graphics.fill(textX, textY, textX + MAIN_PANEL_WIDTH - 20, textY + 1, 0xFFAAAAAA);
        textY += 6;

        // 说明文字
        graphics.drawString(this.font, Component.literal("§7说明："), textX, textY, 0xFFFFFFFF, false);
        textY += lineHeight;
        String desc = getDescription(selectedType);
        graphics.drawString(this.font, Component.literal(desc), textX, textY, 0xFFFFFFFF, false);
    }

    /**
     * 获取工作方块说明
     */
    private String getDescription(HollowHouseWorkBlockType type) {
        return switch (type) {
            case STOREHOUSE -> "藏身处仓库，可存储物品供其他工作方块消耗。";
            case MEDICAL -> "藏身处医疗站，可消耗仓库物品生产医疗物品。";
            case POWER -> "藏身处供电站，消耗燃料为需要电力工作方块供电。";
            case LIGHTING -> "藏身处照明，消耗电力点亮整个私人区域。";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击在左侧栏区域内
        if (mouseX >= leftPos && mouseX < leftPos + SIDEBAR_WIDTH
                && mouseY >= topPos && mouseY < topPos + SIDEBAR_HEIGHT) {
            // 检查是否点击在滚动条上
            if (isMouseOverScrollbar(mouseX, mouseY)) {
                draggingScrollbar = true;
                dragStartYOffset = scrollOffset;
                return true;
            }
            // 遍历条目，调整 Y 坐标以应用滚动偏移后判断点击
            for (int i = 0; i < entryList.size(); i++) {
                HollowHouseWorkBlockEntry entry = entryList.get(i);
                int baseY = topPos + i * HollowHouseWorkBlockEntry.ENTRY_HEIGHT;
                entry.y = baseY - scrollOffset;
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            // 根据鼠标拖拽计算新的滚动偏移
            int scrollbarX = leftPos + SIDEBAR_WIDTH - SCROLLBAR_WIDTH - 1;
            int scrollbarY = topPos + 1;
            int scrollbarHeight = SIDEBAR_HEIGHT - 2;
            float ratio = (float) SIDEBAR_HEIGHT / contentHeight;
            int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
            int maxOffset = contentHeight - SIDEBAR_HEIGHT;
            if (maxOffset > 0) {
                double mouseDelta = mouseY - scrollbarY - (thumbHeight / 2.0);
                double fraction = mouseDelta / (scrollbarHeight - thumbHeight);
                scrollOffset = (int) (fraction * maxOffset);
                scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 检查鼠标是否在左侧栏区域内
        if (mouseX >= leftPos && mouseX < leftPos + SIDEBAR_WIDTH
                && mouseY >= topPos && mouseY < topPos + SIDEBAR_HEIGHT) {
            int maxOffset = Math.max(0, contentHeight - SIDEBAR_HEIGHT);
            scrollOffset = (int) (scrollOffset - delta * HollowHouseWorkBlockEntry.ENTRY_HEIGHT);
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * 判断鼠标是否在滚动条上
     */
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (contentHeight <= SIDEBAR_HEIGHT) {
            return false;
        }
        int scrollbarX = leftPos + SIDEBAR_WIDTH - SCROLLBAR_WIDTH - 1;
        int scrollbarY = topPos + 1;
        int scrollbarHeight = SIDEBAR_HEIGHT - 2;
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
    }

    /**
     * 服务端同步最新等级后刷新界面
     */
    public void updateLevels(Map<String, Integer> levels) {
        this.workBlockLevels.clear();
        this.workBlockLevels.putAll(levels);

        // 重建左侧栏条目以应用新等级
        buildSidebarEntries();
        // 保持当前选中
        onEntrySelected(selectedType);
        updateUpgradeButton();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}