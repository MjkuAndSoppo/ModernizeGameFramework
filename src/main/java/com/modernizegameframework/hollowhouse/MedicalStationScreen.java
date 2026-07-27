package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ui.UIBlurBackground;
import com.modernizegameframework.ui.UIPanel;
import com.modernizegameframework.ui.UIScrollPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 医疗站界面
 * 左侧为配方列表，中间为配方详情与制作控制，右侧为当前生产任务列表
 */
public class MedicalStationScreen extends Screen {

    /** 主面板尺寸 */
    private static final int MAIN_PANEL_WIDTH = 176;
    private static final int MAIN_PANEL_HEIGHT = 166;

    /** 左侧面板宽度 */
    private static final int LEFT_PANEL_WIDTH = 88;
    /** 右侧面板宽度 */
    private static final int RIGHT_PANEL_WIDTH = 120;
    /** 面板间隙 */
    private static final int PANEL_GAP = 4;

    /** 配方列表条目高度 */
    private static final int RECIPE_ENTRY_HEIGHT = 16;
    /** 任务列表条目高度 */
    private static final int TASK_ENTRY_HEIGHT = 18;

    /** 界面整体左上角坐标 */
    private int leftPos;
    private int topPos;

    private final int medicalLevel;
    private final List<MedicalTask> tasks;
    private final List<MedicalRecipe> availableRecipes;

    private MedicalRecipe selectedRecipe;

    private UIScrollPanel recipePanel;
    private UIPanel detailPanel;
    private UIScrollPanel taskPanel;

    private EditBox amountEditBox;
    private Button craftButton;
    private Button increaseButton;
    private Button decreaseButton;

    private final List<RecipeListEntry> recipeEntries = new ArrayList<>();
    private final List<TaskListEntry> taskEntries = new ArrayList<>();

    public MedicalStationScreen(int medicalLevel, List<MedicalTask> tasks) {
        super(Component.literal("医疗站"));
        this.medicalLevel = medicalLevel;
        this.tasks = new ArrayList<>(tasks);

        this.availableRecipes = new ArrayList<>();
        for (MedicalRecipe recipe : MedicalRecipe.values()) {
            if (recipe.isAvailable(medicalLevel)) {
                availableRecipes.add(recipe);
            }
        }

        this.selectedRecipe = availableRecipes.isEmpty() ? null : availableRecipes.get(0);
    }

    @Override
    protected void init() {
        int totalWidth = LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH;
        this.leftPos = (this.width - totalWidth) / 2;
        this.topPos = (this.height - MAIN_PANEL_HEIGHT) / 2;

        // 左侧配方列表面板
        recipePanel = new UIScrollPanel(leftPos, topPos, LEFT_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        recipePanel.setBackgroundColor(0xFF2A2A2A);
        recipePanel.setBorderColor(0xFF555555);
        buildRecipeList();

        // 中间详情面板
        detailPanel = new UIPanel(leftPos + LEFT_PANEL_WIDTH + PANEL_GAP, topPos, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        detailPanel.setBackgroundColor(0xB02A2A2A);
        detailPanel.setBorderColor(0xFF555555);

        // 右侧任务列表面板
        taskPanel = new UIScrollPanel(leftPos + LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP, topPos, RIGHT_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        taskPanel.setBackgroundColor(0xFF2A2A2A);
        taskPanel.setBorderColor(0xFF555555);
        buildTaskList();

        // 数量编辑框与按钮
        int centerX = detailPanel.getX() + MAIN_PANEL_WIDTH / 2;
        int editBoxWidth = 40;
        int editBoxX = centerX - editBoxWidth / 2;
        int editBoxY = detailPanel.getY() + MAIN_PANEL_HEIGHT - 55;

        amountEditBox = new EditBox(this.font, editBoxX, editBoxY, editBoxWidth, 16, Component.literal("1"));
        amountEditBox.setValue("1");
        amountEditBox.setFilter(s -> s.matches("\\d*"));
        amountEditBox.setMaxLength(3);
        this.addRenderableWidget(amountEditBox);

        decreaseButton = Button.builder(Component.literal("-"), btn -> adjustAmount(-1))
                .bounds(editBoxX - 22, editBoxY, 20, 16)
                .build();
        this.addRenderableWidget(decreaseButton);

        increaseButton = Button.builder(Component.literal("+"), btn -> adjustAmount(1))
                .bounds(editBoxX + editBoxWidth + 2, editBoxY, 20, 16)
                .build();
        this.addRenderableWidget(increaseButton);

        // 制作按钮
        craftButton = Button.builder(Component.literal("制作"), btn -> startCraft())
                .bounds(centerX - 35, editBoxY + 22, 70, 20)
                .build();
        this.addRenderableWidget(craftButton);

        updateCraftButton();
    }

    /**
     * 构建左侧配方列表
     */
    private void buildRecipeList() {
        recipeEntries.clear();
        recipePanel.clearChildren();

        int entryWidth = LEFT_PANEL_WIDTH - UIScrollPanel.SCROLLBAR_WIDTH - 1;
        int contentHeight = Math.max(MAIN_PANEL_HEIGHT, availableRecipes.size() * RECIPE_ENTRY_HEIGHT);
        recipePanel.setContentHeight(contentHeight);

        for (int i = 0; i < availableRecipes.size(); i++) {
            MedicalRecipe recipe = availableRecipes.get(i);
            RecipeListEntry entry = new RecipeListEntry(
                    recipePanel.getX(), recipePanel.getY() + i * RECIPE_ENTRY_HEIGHT,
                    entryWidth, RECIPE_ENTRY_HEIGHT, recipe);
            entry.setSelected(recipe == selectedRecipe);
            entry.setOnClick(r -> selectRecipe(r));
            recipePanel.addChild(entry);
            recipeEntries.add(entry);
        }
    }

    /**
     * 构建右侧任务列表
     */
    private void buildTaskList() {
        taskEntries.clear();
        taskPanel.clearChildren();

        int entryWidth = RIGHT_PANEL_WIDTH - UIScrollPanel.SCROLLBAR_WIDTH - 1;
        int contentHeight = Math.max(MAIN_PANEL_HEIGHT, tasks.size() * TASK_ENTRY_HEIGHT);
        taskPanel.setContentHeight(contentHeight);

        for (int i = 0; i < tasks.size(); i++) {
            MedicalTask task = tasks.get(i);
            TaskListEntry entry = new TaskListEntry(
                    taskPanel.getX(), taskPanel.getY() + i * TASK_ENTRY_HEIGHT,
                    entryWidth, TASK_ENTRY_HEIGHT, task);
            taskPanel.addChild(entry);
            taskEntries.add(entry);
        }
    }

    /**
     * 选中配方
     */
    private void selectRecipe(MedicalRecipe recipe) {
        this.selectedRecipe = recipe;
        for (RecipeListEntry entry : recipeEntries) {
            entry.setSelected(entry.getRecipe() == recipe);
        }
        updateCraftButton();
    }

    /**
     * 调整制作数量
     */
    private void adjustAmount(int delta) {
        try {
            int current = Integer.parseInt(amountEditBox.getValue());
            int next = Math.max(1, current + delta);
            amountEditBox.setValue(String.valueOf(next));
        } catch (NumberFormatException e) {
            amountEditBox.setValue("1");
        }
    }

    /**
     * 发送制作请求
     */
    private void startCraft() {
        if (selectedRecipe == null) {
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(amountEditBox.getValue());
        } catch (NumberFormatException e) {
            amount = 1;
        }
        amount = Math.max(1, amount);
        MedicalStationNetwork.CHANNEL.sendToServer(
                new MedicalStationNetwork.StartMedicalProductionPacket(selectedRecipe.name(), amount));
    }

    /**
     * 更新制作按钮状态
     */
    private void updateCraftButton() {
        if (craftButton != null) {
            craftButton.active = selectedRecipe != null;
        }
    }

    /**
     * 服务端同步任务列表后刷新界面
     */
    public void updateTasks(List<MedicalTask> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);
        buildTaskList();
    }

    @Override
    public void tick() {
        super.tick();
        if (amountEditBox != null) {
            amountEditBox.tick();
        }
        // 本地倒计时刷新显示
        for (TaskListEntry entry : taskEntries) {
            entry.updateDisplay();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制毛玻璃背景
        UIBlurBackground.render(graphics, this.width, this.height, UIBlurBackground.LIGHT_OVERLAY);

        // 标题
        graphics.drawString(this.font, this.title,
                leftPos + (LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH) / 2
                        - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // 渲染三个面板
        recipePanel.render(graphics, mouseX, mouseY, partialTick);
        detailPanel.render(graphics, mouseX, mouseY, partialTick);
        taskPanel.render(graphics, mouseX, mouseY, partialTick);

        // 渲染中间详情面板内容
        renderDetailPanel(graphics);

        // 渲染原版组件（按钮、编辑框）
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染中间详情面板
     */
    private void renderDetailPanel(GuiGraphics graphics) {
        if (selectedRecipe == null) {
            return;
        }

        int x = detailPanel.getX() + 8;
        int y = detailPanel.getY() + 8;
        int lineHeight = 12;

        // 配方名称
        graphics.drawString(this.font, Component.literal("§n" + selectedRecipe.getDisplayName()), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 4;

        // 产出物品
        ItemStack output = selectedRecipe.getOutput();
        graphics.renderItem(output, x, y);
        graphics.renderItemDecorations(this.font, output, x, y);
        graphics.drawString(this.font, output.getHoverName(), x + 18, y + 4, 0xFFFFFFFF, false);
        y += 22;

        // 消耗物品
        graphics.drawString(this.font, Component.literal("§7消耗："), x, y, 0xFFFFFFFF, false);
        y += lineHeight;
        for (ItemStack ingredient : selectedRecipe.getIngredients()) {
            String text = "  " + ingredient.getHoverName().getString() + " * " + ingredient.getCount();
            graphics.drawString(this.font, Component.literal(text), x, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }

        // 经验消耗与生产时间
        y += 2;
        graphics.drawString(this.font, Component.literal("§7经验消耗：§e" + selectedRecipe.getExperienceCost()), x, y, 0xFFFFFFFF, false);
        y += lineHeight;
        graphics.drawString(this.font, Component.literal("§7生产时间：§e" + selectedRecipe.getProductionSeconds() + " 秒"), x, y, 0xFFFFFFFF, false);

        // 数量标签
        int editBoxY = detailPanel.getY() + MAIN_PANEL_HEIGHT - 55;
        graphics.drawString(this.font, Component.literal("制作数量"),
                detailPanel.getX() + MAIN_PANEL_WIDTH / 2 - this.font.width("制作数量") / 2,
                editBoxY - 12, 0xFFFFFFFF, false);

        // 右侧面板标题
        graphics.drawString(this.font, Component.literal("生产队列"),
                taskPanel.getX() + 4, taskPanel.getY() + 4, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (recipePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (taskPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        recipePanel.mouseReleased(mouseX, mouseY, button);
        taskPanel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        recipePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        taskPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (recipePanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (taskPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 配方列表条目组件
     */
    private class RecipeListEntry extends UIPanel {

        private final MedicalRecipe recipe;
        private boolean selected;
        private java.util.function.Consumer<MedicalRecipe> onClick;

        public RecipeListEntry(int x, int y, int width, int height, MedicalRecipe recipe) {
            super(x, y, width, height);
            this.recipe = recipe;
            setBackgroundColor(0xFF333333);
            setBorderColor(0xFF555555);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            setBackgroundColor(selected ? 0xFF3A5A8A : 0xFF333333);
        }

        public void setOnClick(java.util.function.Consumer<MedicalRecipe> onClick) {
            this.onClick = onClick;
        }

        public MedicalRecipe getRecipe() {
            return recipe;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            // 绘制配方名称
            String name = recipe.getDisplayName();
            if (font.width(name) > width - 4) {
                name = font.plainSubstrByWidth(name, width - 8) + "...";
            }
            graphics.drawString(font, Component.literal(name), x + 2, y + (height - 8) / 2, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.accept(recipe);
                return true;
            }
            return false;
        }
    }

    /**
     * 任务列表条目组件
     */
    private class TaskListEntry extends UIPanel {

        private final MedicalTask task;

        public TaskListEntry(int x, int y, int width, int height, MedicalTask task) {
            super(x, y, width, height);
            this.task = task;
            setBackgroundColor(0xFF333333);
            setBorderColor(0xFF555555);
        }

        public void updateDisplay() {
            // 由 Screen.tick 调用，触发重绘即可
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            String text = task.getDisplayText();
            if (font.width(text) > width - 4) {
                text = font.plainSubstrByWidth(text, width - 8) + "...";
            }
            graphics.drawString(font, Component.literal(text), x + 2, y + (height - 8) / 2, 0xFFFFFFFF, false);
        }
    }
}
