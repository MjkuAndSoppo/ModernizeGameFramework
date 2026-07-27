package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ui.UIBlurBackground;
import com.modernizegameframework.ui.UIPanel;
import com.modernizegameframework.ui.UIScrollPanel;
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
 * 使用项目自研 UI 库实现：左侧栏 + 主面板布局
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



    /** 整个界面左上角坐标 */
    private int leftPos;
    private int topPos;

    /** 左侧滚动面板 */
    private UIScrollPanel sidebar;
    /** 主面板 */
    private UIPanel mainPanel;
    /** 升级/解锁按钮 */
    private Button upgradeButton;

    /** 工作方块等级数据 */
    private final Map<String, Integer> workBlockLevels;
    /** 当前选中的工作方块 */
    private HollowHouseWorkBlockType selectedType;
    /** 左侧栏条目组件列表 */
    private final List<HollowHouseWorkBlockEntry> entryList = new ArrayList<>();

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

        // 左侧滚动面板
        sidebar = new UIScrollPanel(leftPos, topPos, SIDEBAR_WIDTH, SIDEBAR_HEIGHT);
        sidebar.setBackgroundColor(0xFF2A2A2A);
        sidebar.setBorderColor(0xFF555555);
        buildSidebarEntries();

        // 主面板：尺寸与原版工作台一致，使用半透明灰色背景
        mainPanel = new UIPanel(leftPos + SIDEBAR_WIDTH + PANEL_GAP, topPos, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        mainPanel.setBackgroundColor(0xB02A2A2A);
        mainPanel.setBorderColor(0xFF555555);

        // 升级/解锁按钮，放在主面板底部居中
        int buttonWidth = 70;
        int buttonX = mainPanel.getX() + (MAIN_PANEL_WIDTH - buttonWidth) / 2;
        int buttonY = mainPanel.getY() + MAIN_PANEL_HEIGHT - 30;
        upgradeButton = Button.builder(Component.literal("解锁"), btn -> sendUpgrade())
                .bounds(buttonX, buttonY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(upgradeButton);

        updateUpgradeButton();
    }

    /**
     * 构建左侧栏条目
     */
    private void buildSidebarEntries() {
        entryList.clear();
        sidebar.clearChildren();

        HollowHouseWorkBlockType[] types = HollowHouseWorkBlockType.values();
        int entryWidth = SIDEBAR_WIDTH - UIScrollPanel.SCROLLBAR_WIDTH - 1;
        int contentHeight = types.length * HollowHouseWorkBlockEntry.ENTRY_HEIGHT;
        sidebar.setContentHeight(Math.max(SIDEBAR_HEIGHT, contentHeight));

        for (int i = 0; i < types.length; i++) {
            HollowHouseWorkBlockType type = types[i];
            int level = workBlockLevels.getOrDefault(type.getId(), 0);
            HollowHouseWorkBlockEntry entry = new HollowHouseWorkBlockEntry(
                    sidebar.getX(), sidebar.getY() + i * HollowHouseWorkBlockEntry.ENTRY_HEIGHT,
                    entryWidth, type, level);

            entry.setSelected(type == selectedType);
            entry.setOnClick(this::onEntrySelected);

            sidebar.addChild(entry);
            entryList.add(entry);
        }
    }

    /**
     * 左侧栏条目点击回调
     */
    private void onEntrySelected(HollowHouseWorkBlockType type) {
        this.selectedType = type;
        for (HollowHouseWorkBlockEntry entry : entryList) {
            entry.setSelected(entry == findEntry(type));
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
        // 绘制毛玻璃背景
        UIBlurBackground.render(graphics, this.width, this.height, UIBlurBackground.LIGHT_OVERLAY);

        // 渲染标题
        graphics.drawString(this.font, this.title,
                leftPos + (SIDEBAR_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH) / 2 - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // 渲染 UI 组件
        sidebar.render(graphics, mouseX, mouseY, partialTick);
        mainPanel.render(graphics, mouseX, mouseY, partialTick);

        // 渲染主面板内容（原版工作台纹理背景 + 详细信息）
        renderMainPanelContent(graphics);

        // 渲染原版按钮
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染主面板内容
     */
    private void renderMainPanelContent(GuiGraphics graphics) {
        int mainX = mainPanel.getX();
        int mainY = mainPanel.getY();

        if (selectedType == null) {
            return;
        }

        int level = workBlockLevels.getOrDefault(selectedType.getId(), 0);
        boolean unlocked = level > 0;
        boolean maxed = level >= selectedType.getMaxLevel();

        int textX = mainX + 10;
        int textY = mainY + 10;
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
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先让 UI 组件处理点击
        if (sidebar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (mainPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        sidebar.mouseReleased(mouseX, mouseY, button);
        mainPanel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        sidebar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        mainPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (sidebar.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
