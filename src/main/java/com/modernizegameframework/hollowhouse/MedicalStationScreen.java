package com.modernizegameframework.hollowhouse;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 医疗站界面
 * 左侧为配方列表，中间为配方详情/任务详情与操作，右侧为当前生产任务列表
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

    /** 滚动条宽度 */
    private static final int SCROLLBAR_WIDTH = 6;

    /** 界面整体左上角坐标 */
    private int leftPos;
    private int topPos;

    private final int medicalLevel;
    private final List<MedicalTask> tasks;
    private final List<MedicalRecipe> availableRecipes;

    private MedicalRecipe selectedRecipe;
    private MedicalTask selectedTask;
    private int selectedTaskIndex = -1;

    // 三个面板的坐标与尺寸（替代 UIPanel / UIScrollPanel 对象）
    private int recipePanelX, recipePanelY, recipePanelW, recipePanelH;
    private int detailPanelX, detailPanelY, detailPanelW, detailPanelH;
    private int taskPanelX, taskPanelY, taskPanelW, taskPanelH;

    // 滚动偏移量
    private int recipeScrollOffset = 0;
    private int taskScrollOffset = 0;
    private int recipeContentHeight;
    private int taskContentHeight;

    private EditBox amountEditBox;
    private Button craftButton;
    private Button increaseButton;
    private Button decreaseButton;
    private Button cancelTaskButton;

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
        this.selectedTask = null;
        this.selectedTaskIndex = -1;
    }

    @Override
    protected void init() {
        int totalWidth = LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH;
        this.leftPos = (this.width - totalWidth) / 2;
        this.topPos = (this.height - MAIN_PANEL_HEIGHT) / 2;

        // 左侧配方列表面板坐标
        recipePanelX = leftPos;
        recipePanelY = topPos;
        recipePanelW = LEFT_PANEL_WIDTH;
        recipePanelH = MAIN_PANEL_HEIGHT;

        // 中间详情面板坐标
        detailPanelX = leftPos + LEFT_PANEL_WIDTH + PANEL_GAP;
        detailPanelY = topPos;
        detailPanelW = MAIN_PANEL_WIDTH;
        detailPanelH = MAIN_PANEL_HEIGHT;

        // 右侧任务列表面板坐标
        taskPanelX = leftPos + LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP;
        taskPanelY = topPos;
        taskPanelW = RIGHT_PANEL_WIDTH;
        taskPanelH = MAIN_PANEL_HEIGHT;

        buildRecipeList();
        buildTaskList();

        // 数量编辑框与按钮（仅在选择配方时显示）
        int centerX = detailPanelX + MAIN_PANEL_WIDTH / 2;
        int editBoxWidth = 40;
        int editBoxX = centerX - editBoxWidth / 2;
        int editBoxY = detailPanelY + MAIN_PANEL_HEIGHT - 55;

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

        // 取消任务按钮（仅在选择任务时显示）
        cancelTaskButton = Button.builder(Component.literal("取消任务"), btn -> cancelSelectedTask())
                .bounds(centerX - 35, editBoxY + 22, 70, 20)
                .build();
        this.addRenderableWidget(cancelTaskButton);

        updateWidgetVisibility();
    }

    /**
     * 构建左侧配方列表
     */
    private void buildRecipeList() {
        recipeEntries.clear();
        recipeScrollOffset = 0;

        int entryWidth = recipePanelW - SCROLLBAR_WIDTH - 1;
        recipeContentHeight = Math.max(recipePanelH, availableRecipes.size() * RECIPE_ENTRY_HEIGHT);

        for (int i = 0; i < availableRecipes.size(); i++) {
            MedicalRecipe recipe = availableRecipes.get(i);
            RecipeListEntry entry = new RecipeListEntry(
                    recipePanelX, recipePanelY + i * RECIPE_ENTRY_HEIGHT,
                    entryWidth, RECIPE_ENTRY_HEIGHT, recipe);
            entry.setSelected(recipe == selectedRecipe && selectedTask == null);
            entry.setOnClick(r -> selectRecipe(r));
            recipeEntries.add(entry);
        }
    }

    /**
     * 构建右侧任务列表
     */
    private void buildTaskList() {
        taskEntries.clear();
        taskScrollOffset = 0;

        int entryWidth = taskPanelW - SCROLLBAR_WIDTH - 1;
        taskContentHeight = Math.max(taskPanelH, tasks.size() * TASK_ENTRY_HEIGHT);

        for (int i = 0; i < tasks.size(); i++) {
            MedicalTask task = tasks.get(i);
            TaskListEntry entry = new TaskListEntry(
                    taskPanelX, taskPanelY + i * TASK_ENTRY_HEIGHT,
                    entryWidth, TASK_ENTRY_HEIGHT, task, i);
            entry.setSelected(selectedTaskIndex == i);
            entry.setOnClick((t, index) -> selectTask(t, index));
            taskEntries.add(entry);
        }
    }

    /**
     * 选中配方
     */
    private void selectRecipe(MedicalRecipe recipe) {
        this.selectedRecipe = recipe;
        this.selectedTask = null;
        this.selectedTaskIndex = -1;
        for (RecipeListEntry entry : recipeEntries) {
            entry.setSelected(entry.getRecipe() == recipe);
        }
        for (TaskListEntry entry : taskEntries) {
            entry.setSelected(false);
        }
        updateWidgetVisibility();
    }

    /**
     * 选中右侧任务
     */
    private void selectTask(MedicalTask task, int index) {
        this.selectedTask = task;
        this.selectedTaskIndex = index;
        for (RecipeListEntry entry : recipeEntries) {
            entry.setSelected(false);
        }
        for (TaskListEntry entry : taskEntries) {
            entry.setSelected(entry.getIndex() == index);
        }
        updateWidgetVisibility();
    }

    /**
     * 更新控件可见性
     * 选中配方时显示制作控件，选中任务时显示取消按钮
     */
    private void updateWidgetVisibility() {
        boolean isRecipeMode = selectedTask == null;
        amountEditBox.visible = isRecipeMode;
        amountEditBox.active = isRecipeMode;
        decreaseButton.visible = isRecipeMode;
        decreaseButton.active = isRecipeMode;
        increaseButton.visible = isRecipeMode;
        increaseButton.active = isRecipeMode;
        craftButton.visible = isRecipeMode;
        craftButton.active = isRecipeMode && selectedRecipe != null;
        cancelTaskButton.visible = !isRecipeMode;
        cancelTaskButton.active = !isRecipeMode;
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
     * 取消选中的任务
     */
    private void cancelSelectedTask() {
        if (selectedTask == null || selectedTaskIndex < 0) {
            return;
        }
        MedicalStationNetwork.CHANNEL.sendToServer(
                new MedicalStationNetwork.CancelMedicalTaskPacket(selectedTaskIndex));
    }

    /**
     * 服务端同步任务列表后刷新界面
     */
    public void updateTasks(List<MedicalTask> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);

        // 如果之前选中的任务在新列表中已不存在（例如被取消），强制切回配方模式
        if (selectedTaskIndex >= 0 && selectedTaskIndex < tasks.size()) {
            this.selectedTask = tasks.get(selectedTaskIndex);
        } else {
            this.selectedTask = null;
            this.selectedTaskIndex = -1;
            // 确保回到配方选择模式，默认选中第一个可用配方
            if (!availableRecipes.isEmpty()) {
                selectedRecipe = availableRecipes.get(0);
            }
        }

        // 重新构建任务列表与配方列表，确保右侧栏目正确清算
        buildTaskList();
        buildRecipeList();
        updateWidgetVisibility();
    }

    @Override
    public void tick() {
        super.tick();
        if (amountEditBox != null) {
            amountEditBox.tick();
        }
        // 本地倒计时刷新显示（getRemainingSeconds 会基于开始时间实时校准）
        for (TaskListEntry entry : taskEntries) {
            entry.updateDisplay();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制毛玻璃背景（使用渐变填充替代 UIBlurBackground）
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC000000, 0xCC000000);

        // 标题
        graphics.drawString(this.font, this.title,
                leftPos + (LEFT_PANEL_WIDTH + PANEL_GAP + MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH) / 2
                        - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // ===== 渲染左侧配方列表面板 =====
        renderPanel(graphics, recipePanelX, recipePanelY, recipePanelW, recipePanelH, 0xFF2A2A2A, 0xFF555555);

        // 渲染配方条目（应用滚动偏移）
        for (RecipeListEntry entry : recipeEntries) {
            int visualY = entry.getBaseY() - recipeScrollOffset;
            // 裁剪：只渲染可见区域内的条目
            if (visualY + entry.getHeight() > recipePanelY && visualY < recipePanelY + recipePanelH) {
                entry.render(graphics, mouseX, mouseY, partialTick, visualY);
            }
        }

        // 渲染左侧滚动条
        renderScrollbar(graphics, recipePanelX, recipePanelY, recipePanelW, recipePanelH,
                recipeScrollOffset, recipeContentHeight);

        // ===== 渲染中间详情面板 =====
        renderPanel(graphics, detailPanelX, detailPanelY, detailPanelW, detailPanelH, 0xB02A2A2A, 0xFF555555);
        renderDetailPanel(graphics);

        // ===== 渲染右侧任务列表面板 =====
        renderPanel(graphics, taskPanelX, taskPanelY, taskPanelW, taskPanelH, 0xFF2A2A2A, 0xFF555555);

        // 渲染任务条目（应用滚动偏移）
        for (TaskListEntry entry : taskEntries) {
            int visualY = entry.getBaseY() - taskScrollOffset;
            // 裁剪：只渲染可见区域内的条目
            if (visualY + entry.getHeight() > taskPanelY && visualY < taskPanelY + taskPanelH) {
                entry.render(graphics, mouseX, mouseY, partialTick, visualY);
            }
        }

        // 渲染右侧滚动条
        renderScrollbar(graphics, taskPanelX, taskPanelY, taskPanelW, taskPanelH,
                taskScrollOffset, taskContentHeight);

        // 渲染原版组件（按钮、编辑框）
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 绘制面板背景与边框
     */
    private void renderPanel(GuiGraphics graphics, int x, int y, int w, int h, int bgColor, int borderColor) {
        graphics.fill(x, y, x + w, y + h, bgColor);
        graphics.renderOutline(x, y, w, h, borderColor);
    }

    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics graphics, int panelX, int panelY, int panelW, int panelH,
                                 int scrollOffset, int contentHeight) {
        if (contentHeight <= panelH) {
            return;
        }
        int scrollbarX = panelX + panelW - SCROLLBAR_WIDTH;
        int trackHeight = panelH;
        int thumbHeight = Math.max(16, trackHeight * panelH / contentHeight);
        int maxScroll = contentHeight - panelH;
        int thumbY = panelY + (scrollOffset * (trackHeight - thumbHeight) / maxScroll);

        // 轨道背景
        graphics.fill(scrollbarX, panelY, scrollbarX + SCROLLBAR_WIDTH, panelY + trackHeight, 0xFF333333);
        // 滑块
        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF888888);
    }

    /**
     * 渲染中间详情面板
     */
    private void renderDetailPanel(GuiGraphics graphics) {
        // 右侧面板标题统一在面板上方绘制，避免遮挡滚动内容
        graphics.drawString(this.font, Component.literal("生产队列"),
                taskPanelX + 4, taskPanelY - 12, 0xFFFFFFFF, false);

        if (selectedTask != null) {
            renderTaskDetail(graphics);
            return;
        }
        if (selectedRecipe != null) {
            renderRecipeDetail(graphics);
        }
    }

    /**
     * 渲染配方详情
     */
    private void renderRecipeDetail(GuiGraphics graphics) {
        int x = detailPanelX + 8;
        int y = detailPanelY + 8;
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
        int editBoxY = detailPanelY + MAIN_PANEL_HEIGHT - 55;
        graphics.drawString(this.font, Component.literal("制作数量"),
                detailPanelX + MAIN_PANEL_WIDTH / 2 - this.font.width("制作数量") / 2,
                editBoxY - 12, 0xFFFFFFFF, false);
    }

    /**
     * 渲染任务详情
     */
    private void renderTaskDetail(GuiGraphics graphics) {
        int x = detailPanelX + 8;
        int y = detailPanelY + 8;
        int lineHeight = 12;

        // 任务标题
        graphics.drawString(this.font, Component.literal("§n任务详情"), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 4;

        // 产出物品
        ItemStack output = selectedTask.getRecipe().getOutput();
        graphics.renderItem(output, x, y);
        graphics.renderItemDecorations(this.font, output, x, y);
        graphics.drawString(this.font, output.getHoverName(), x + 18, y + 4, 0xFFFFFFFF, false);
        y += 22;

        graphics.drawString(this.font, Component.literal("§7总量：§f" + selectedTask.getAmount()), x, y, 0xFFFFFFFF, false);
        y += lineHeight;
        graphics.drawString(this.font, Component.literal("§7已完成：§f" + selectedTask.getCompletedAmount()), x, y, 0xFFFFFFFF, false);
        y += lineHeight;
        graphics.drawString(this.font, Component.literal("§7剩余时间：§e" + selectedTask.getFormattedRemainingTime()), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 2;

        // 剩余材料
        graphics.drawString(this.font, Component.literal("§7消耗材料："), x, y, 0xFFFFFFFF, false);
        y += lineHeight;
        for (ItemStack ingredient : selectedTask.getRemainingIngredients()) {
            String text = "  " + ingredient.getHoverName().getString() + " * " + ingredient.getCount();
            graphics.drawString(this.font, Component.literal(text), x, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }

        // 经验
        y += 2;
        graphics.drawString(this.font, Component.literal("§7已消耗经验：§e" + selectedTask.getTotalExperienceCost()
                + " §7（剩余返还：§e" + selectedTask.getRemainingExperienceCost() + "§7）"), x, y, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理左侧配方列表点击
        for (RecipeListEntry entry : recipeEntries) {
            int visualY = entry.getBaseY() - recipeScrollOffset;
            if (entry.isMouseOver(mouseX, mouseY, visualY) && entry.mouseClicked(mouseX, mouseY, button, visualY)) {
                return true;
            }
        }
        // 处理右侧任务列表点击
        for (TaskListEntry entry : taskEntries) {
            int visualY = entry.getBaseY() - taskScrollOffset;
            if (entry.isMouseOver(mouseX, mouseY, visualY) && entry.mouseClicked(mouseX, mouseY, button, visualY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 处理左侧配方列表滚动
        if (mouseX >= recipePanelX && mouseX < recipePanelX + recipePanelW &&
                mouseY >= recipePanelY && mouseY < recipePanelY + recipePanelH) {
            int maxScroll = Math.max(0, recipeContentHeight - recipePanelH);
            recipeScrollOffset = Math.max(0, Math.min(maxScroll, recipeScrollOffset - (int) (delta * 10)));
            return true;
        }
        // 处理右侧任务列表滚动
        if (mouseX >= taskPanelX && mouseX < taskPanelX + taskPanelW &&
                mouseY >= taskPanelY && mouseY < taskPanelY + taskPanelH) {
            int maxScroll = Math.max(0, taskContentHeight - taskPanelH);
            taskScrollOffset = Math.max(0, Math.min(maxScroll, taskScrollOffset - (int) (delta * 10)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== 内部类定义 ====================

    /**
     * 配方列表条目组件（不继承任何 UI 组件，独立实现渲染与交互）
     */
    private class RecipeListEntry {

        private final MedicalRecipe recipe;
        private final int baseX;
        private final int baseY;
        private final int width;
        private final int height;
        private boolean selected;
        private int backgroundColor = 0xFF333333;
        private int borderColor = 0xFF555555;
        private java.util.function.Consumer<MedicalRecipe> onClick;

        public RecipeListEntry(int x, int y, int width, int height, MedicalRecipe recipe) {
            this.baseX = x;
            this.baseY = y;
            this.width = width;
            this.height = height;
            this.recipe = recipe;
        }

        public int getBaseY() {
            return baseY;
        }

        public int getHeight() {
            return height;
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

        public void setBackgroundColor(int color) {
            this.backgroundColor = color;
        }

        public void setBorderColor(int color) {
            this.borderColor = color;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int visualY) {
            // 绘制背景
            graphics.fill(baseX, visualY, baseX + width, visualY + height, backgroundColor);
            // 绘制边框
            graphics.renderOutline(baseX, visualY, width, height, borderColor);
            // 绘制文本
            String name = recipe.getDisplayName();
            if (font.width(name) > width - 4) {
                name = font.plainSubstrByWidth(name, width - 8) + "...";
            }
            graphics.drawString(font, Component.literal(name), baseX + 2, visualY + (height - 8) / 2, 0xFFFFFFFF, false);
        }

        public boolean isMouseOver(double mouseX, double mouseY, int visualY) {
            return mouseX >= baseX && mouseX < baseX + width
                    && mouseY >= visualY && mouseY < visualY + height;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button, int visualY) {
            if (isMouseOver(mouseX, mouseY, visualY) && onClick != null) {
                onClick.accept(recipe);
                return true;
            }
            return false;
        }
    }

    /**
     * 任务列表条目组件（不继承任何 UI 组件，独立实现渲染与交互）
     */
    private class TaskListEntry {

        private final MedicalTask task;
        private final int index;
        private final int baseX;
        private final int baseY;
        private final int width;
        private final int height;
        private boolean selected;
        private int backgroundColor = 0xFF333333;
        private int borderColor = 0xFF555555;
        private java.util.function.BiConsumer<MedicalTask, Integer> onClick;

        public TaskListEntry(int x, int y, int width, int height, MedicalTask task, int index) {
            this.baseX = x;
            this.baseY = y;
            this.width = width;
            this.height = height;
            this.task = task;
            this.index = index;
        }

        public int getBaseY() {
            return baseY;
        }

        public int getHeight() {
            return height;
        }

        public int getIndex() {
            return index;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            setBackgroundColor(selected ? 0xFF3A5A8A : 0xFF333333);
        }

        public void setOnClick(java.util.function.BiConsumer<MedicalTask, Integer> onClick) {
            this.onClick = onClick;
        }

        public void updateDisplay() {
            // 由 Screen.tick 调用，触发重绘即可
        }

        public void setBackgroundColor(int color) {
            this.backgroundColor = color;
        }

        public void setBorderColor(int color) {
            this.borderColor = color;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, int visualY) {
            // 绘制背景
            graphics.fill(baseX, visualY, baseX + width, visualY + height, backgroundColor);
            // 绘制边框
            graphics.renderOutline(baseX, visualY, width, height, borderColor);
            // 绘制文本
            String text = task.getDisplayText();
            if (font.width(text) > width - 4) {
                text = font.plainSubstrByWidth(text, width - 8) + "...";
            }
            graphics.drawString(font, Component.literal(text), baseX + 2, visualY + (height - 8) / 2, 0xFFFFFFFF, false);
        }

        public boolean isMouseOver(double mouseX, double mouseY, int visualY) {
            return mouseX >= baseX && mouseX < baseX + width
                    && mouseY >= visualY && mouseY < visualY + height;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button, int visualY) {
            if (isMouseOver(mouseX, mouseY, visualY) && onClick != null) {
                onClick.accept(task, index);
                return true;
            }
            return false;
        }
    }
}