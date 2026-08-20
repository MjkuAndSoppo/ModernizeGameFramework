package com.modernizegameframework.looting.client.overlay;

import com.modernizegameframework.looting.client.Core;
import com.modernizegameframework.looting.client.KeyInit;
import com.modernizegameframework.looting.client.core.pipeline.VisualItemEntry;
import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.sighs.apricityui.element.Item;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.render.Base;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 负责控制战利品悬浮窗 (Overlay) 的核心渲染逻辑与状态管理。
 * <p>采用单例模式。本版本使用 MGF 内嵌的 AUI (ApricityUI) 渲染页面
 * {@code screens/looting_hud.html} 呈现物品列表，替代原先的 Java 直接绘制
 * ({@code OverlayRenderer}/{@code OverlayLayout})。保留原有的激活条件、
 * 弹出动画进度与滚动状态走 OverlayState，仅将结果同步到 AUI 文档。</p>
 */
public class Overlay {
    public static final Overlay INSTANCE = new Overlay();

    /** AUI 模板路径（相对 apricity 资源根目录） */
    private static final String AUI_TEMPLATE = "screens/looting_hud.html";

    private final OverlayState state = new OverlayState();
    private Document auiDocument;
    private boolean isOverlayToggled = false;

    private Overlay() {}

    /**
     * 处理客户端 Tick 逻辑，主要用于捕获按键状态以切换 UI 显示模式。
     */
    public void onTick(Minecraft mc) {
        if (mc.level == null) return;

        if (BetterLootingConfig.get().activationMode == BetterLootingConfig.ActivationMode.KEY_TOGGLE) {
            while (KeyInit.SHOW_OVERLAY.consumeClick()) {
                isOverlayToggled = !isOverlayToggled;
            }
        }
    }

    /**
     * 惰性创建 AUI 渲染文档。仅在首次渲染时执行。
     */
    private void ensureDocument() {
        if (auiDocument != null) return;
        auiDocument = Document.create(AUI_TEMPLATE);
        if (auiDocument != null) {
            auiDocument.applyViewport(false);
        }
    }

    /**
     * UI 渲染的主入口。检查显示条件、驱动状态机，并将最新状态同步到 AUI 文档。
     */
    public void render(GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // 隐藏 GUI (F1) 或打开了其他菜单时暂停渲染
        if (mc.options.hideGui || mc.screen != null) return;

        Core core = Core.INSTANCE;
        List<VisualItemEntry> nearbyItems = core.getNearbyItems();

        // 当周围没有战利品时，快速收起动画并清空渲染
        if (nearbyItems == null || nearbyItems.isEmpty()) {
            state.tick(false, 0, 0, rowCount());
            renderFlow(gui, mc, core, List.of());
            return;
        }

        boolean conditionMet = checkActivationCondition(mc);
        boolean shouldShow = !core.isAutoMode() && conditionMet;

        // 更新动画与滚动状态（回调保持与旧实现一致的参数结构）
        float rowCount = rowCount();
        state.tick(shouldShow, core.getTargetScrollOffset(), nearbyItems.size(), rowCount);

        renderFlow(gui, mc, core, nearbyItems);
    }

    /**
     * 当前可视行数（来自配置）。
     */
    private float rowCount() {
        return BetterLootingConfig.get().visibleRows;
    }

    /**
     * 将最新状态同步到 AUI 文档并绘制。文档为空或未成功创建时安全退出。
     */
    private void renderFlow(GuiGraphics gui, Minecraft mc, Core core, List<VisualItemEntry> nearbyItems) {
        ensureDocument();
        if (auiDocument == null) return;

        boolean shown = state.popupProgress > 0.01f;
        // 无战利品时隐藏面板
        if (!shown || (nearbyItems != null && nearbyItems.isEmpty())) {
            setPanelAttribute("data-shown", "0");
            return;
        }

        // 定位面板：居中 + 配置偏移 + 缩放
        var cfg = BetterLootingConfig.get();
        float w = mc.getWindow().getGuiScaledWidth();
        float h = mc.getWindow().getGuiScaledHeight();
        float panelW = 220f;
        float scale = cfg.uiScale;
        float left = w / 2f + cfg.xOffset - panelW * scale / 2f;
        float top = h / 2f + cfg.yOffset;

        Element hud = auiDocument.getElementById("loot-hud");
        if (hud != null) {
            hud.setInlineStyleProperty("left", left + "px");
            hud.setInlineStyleProperty("top", top + "px");
            hud.setInlineStyleProperty("transform", "scale(" + scale + ")");
            hud.setInlineStyleProperty("width", panelW + "px");
        }

        // 标题与数量
        Element titleEl = auiDocument.getElementById("hud-title");
        if (titleEl != null) {
            String customTitle = cfg.customOverlayTitle;
            titleEl.setTextContent(customTitle == null || customTitle.isEmpty() ? "战利品拾取" : customTitle);
        }
        Element countEl = auiDocument.getElementById("hud-count");
        if (countEl != null) countEl.setTextContent(String.valueOf(nearbyItems.size()));

        // 同步物品列表行：清空后按可见窗口重建，滚动由内层容器的 translateY 位移实现
        rebuildItemList(core, nearbyItems);

        setPanelAttribute("data-shown", "1");

        // 交由 AUI 渲染文档
        Base.drawOverlayDocument(gui.pose(), auiDocument);
    }

    /**
     * 重建 AUI 物品列表的内容行。仅渲染当前可见窗口内的物品，并施加滚动位移。
     */
    private void rebuildItemList(Core core, List<VisualItemEntry> nearbyItems) {
        if (nearbyItems.isEmpty()) return;

        var cfg = BetterLootingConfig.get();
        float rowCount = rowCount();
        float listH = rowCount * 26f;

        Element list = auiDocument.getElementById("loot-list");
        if (list != null) {
            list.setInlineStyleProperty("height", Math.round(listH) + "px");
        }
        Element inner = auiDocument.getElementById("loot-list-inner");
        if (inner == null) return;

        // 滚动起始与可见窗口
        int startIdx = (int) Math.floor(state.currentScroll);
        int count = Math.max(0, nearbyItems.size());
        float maxScroll = Math.max(0, count - rowCount);
        int visible = Math.max(0, (int) Math.ceil(Math.min(rowCount, count - startIdx)));

        // 内层容器位移（每行行高 + 间隙 = 28px）
        inner.setInlineStyleProperty("transform", "translateY(" + (-(Math.min(state.currentScroll, maxScroll)) * 28f) + "px)");

        inner.clearChildren();

        for (int i = startIdx; i < startIdx + visible; i++) {
            if (i < 0 || i >= count) continue;
            VisualItemEntry entry = nearbyItems.get(i);
            boolean isSelected = (i == core.getSelectedIndex());

            Element row = auiDocument.createElement("div");
            row.setAttribute("class", "loot-row");
            row.setAttribute("data-selected", isSelected ? "1" : "0");

            // 图标：通过注册表将基础元素升级为 Item，并驱动对应的物品栈
            Element icon = Element.init(auiDocument.createElement("item"));
            icon.setAttribute("class", "loot-icon");
            if (icon instanceof Item itemEl) {
                ItemStack stack = entry.getItem();
                itemEl.setDrivenState(stack, String.valueOf(entry.getTotalCount()), false, false, Item.Source.NONE);
            }
            row.appendChild(icon);

            // 名称（依据背包内是否已有该物品着色）
            Element nameEl = auiDocument.createElement("span");
            nameEl.setAttribute("class", "loot-name");
            nameEl.setTextContent(entry.getItem().getHoverName().getString());
            nameEl.setAttribute("data-in-inventory", core.isItemInInventory(entry.getItem().getItem()) ? "1" : "0");
            row.appendChild(nameEl);

            // 数量角标
            Element countEl = auiDocument.createElement("span");
            countEl.setAttribute("class", "loot-count");
            countEl.setTextContent("x" + entry.getTotalCount());
            row.appendChild(countEl);

            inner.appendChild(row);
        }
    }

    /**
     * 设置面板元素的一个数据属性（用于控制 CSS 显隐/高亮）。
     */
    private void setPanelAttribute(String name, String value) {
        Element hud = auiDocument.getElementById("loot-hud");
        if (hud != null) hud.setAttribute(name, value);
    }

    /**
     * 判断当前玩家的状态是否满足触发悬浮窗的条件（基于配置文件）。
     */
    private boolean checkActivationCondition(Minecraft mc) {
        var cfg = BetterLootingConfig.get();
        if (mc.player == null) return false;

        return switch (cfg.activationMode) {
            case LOOK_DOWN -> mc.player.getXRot() > cfg.lookDownAngle;
            case STAND_STILL -> {
                double dx = mc.player.getX() - mc.player.xo;
                double dz = mc.player.getZ() - mc.player.zo;
                // 利用位移平方差判断玩家是否几乎处于静止状态
                yield (dx * dx + dz * dz) < 0.0001;
            }
            case KEY_HOLD -> !KeyInit.SHOW_OVERLAY.isUnbound() && KeyInit.SHOW_OVERLAY.isDown();
            case KEY_TOGGLE -> isOverlayToggled;
            case ALWAYS -> true;
        };
    }
}