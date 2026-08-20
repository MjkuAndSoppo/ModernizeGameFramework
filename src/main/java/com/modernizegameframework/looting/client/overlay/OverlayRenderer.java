package com.modernizegameframework.looting.client.overlay;

import com.modernizegameframework.looting.client.Constants;
import com.modernizegameframework.looting.client.Utils;
import com.modernizegameframework.looting.client.core.pipeline.VisualItemEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * 负责渲染"更好拾取"模组客户端界面的核心渲染器。
 * 当前仅用于物品栏掉落列表，全部使用原版纯色矩形绘制，不依赖任何外部皮肤贴图。
 */
public class OverlayRenderer {
    private final Minecraft mc;

    public OverlayRenderer(Minecraft mc) {
        this.mc = mc;
    }

    /**
     * 渲染物品列表中的单行条目。
     * 包括背景色块、物品图标、数量、名称以及 "NEW" 标签。
     */
    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew) {
        renderItemRow(gui, x, y, width, entry, selected, bgAlpha, textAlpha, isNew, false);
    }

    /**
     * 渲染物品列表中的单行条目。
     * 包括背景色块、物品图标、数量、名称以及 "NEW" 标签。
     *
     * @param useSkin 兼容参数：始终为 false，仅使用原版纯色矩形背景。
     */
    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew, boolean useSkin) {
        ItemStack stack = entry.getItem();
        int count = entry.getCount();

        // 渲染条目背景（选中状态会有不同的颜色高亮）
        int bgColor = selected ? Constants.COLOR_BG_SELECTED : Constants.COLOR_BG_NORMAL;
        renderRoundedRect(gui, x, y, width, Constants.ITEM_HEIGHT, Utils.applyAlpha(bgColor, bgAlpha));

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int alpha255 = (int) (textAlpha * 255);

        // 绘制基于物品稀有度或自定义颜色的左侧指示条
        gui.fill(x + 2, y + 3, x + 3, y + Constants.ITEM_HEIGHT - 3,
                Utils.colorWithAlpha(Utils.getItemStackDisplayColor(stack), alpha255));

        // 渲染物品模型及数量
        gui.renderItem(stack, x + 3, y + 3);
        String countText = (count > 1) ? compactCount(count) : null;
        gui.renderItemDecorations(mc.font, stack, x + 3, y + 3, countText);

        // 当透明度过低时跳过文本渲染以优化性能
        if (alpha255 <= 10) return;

        var pose = gui.pose();
        int baseTextColor = selected ? Constants.COLOR_TEXT_WHITE : Constants.COLOR_TEXT_DIM;
        int textColor = Utils.colorWithAlpha(baseTextColor, alpha255);

        // 使用 PoseStack 进行缩放，使文本适应 UI 比例
        pose.pushPose();
        pose.translate(x + 26, y + 8, 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        // 特殊处理附魔书：如果物品是附魔书，优先显示第一个附魔的名称而不是统一的"附魔书"
        Component displayName = stack.getHoverName();
        if (stack.getItem() instanceof EnchantedBookItem) {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (!enchants.isEmpty()) {
                Map.Entry<Enchantment, Integer> first = enchants.entrySet().iterator().next();
                displayName = first.getKey().getFullname(first.getValue());
            }
        }

        gui.drawString(mc.font, displayName, 0, 0, textColor, false);
        pose.popPose();
    }

    /**
     * 将超过 10000 的数量格式化为 k 单位（例如 12000 -> 12k）。
     */
    private String compactCount(int count) {
        if (count >= 10000) return (count / 1000) + "k";
        return String.valueOf(count);
    }

    /**
     * 渲染滚动条轨道与滑块。
     */
    public void renderScrollBar(GuiGraphics gui, int total, float maxVis, int x, int y, int h, float alpha, float scroll) {
        gui.fill(x, y, x + 2, y + h, Utils.applyAlpha(Constants.COLOR_SCROLL_TRACK, alpha));
        float ratio = maxVis / total;
        int thumbH = Math.max(10, (int) (h * ratio)); // 滑块最小高度限制为 10px
        float progress = (total - maxVis > 0) ? Mth.clamp(scroll / (total - maxVis), 0f, 1f) : 0f;

        renderRoundedRect(gui, x, y + (int) ((h - thumbH) * progress), 2, thumbH,
                Utils.applyAlpha(Constants.COLOR_SCROLL_THUMB, alpha));
    }

    /**
     * 使用原版矩形渲染拼凑出一个简单的圆角矩形。
     */
    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }
}