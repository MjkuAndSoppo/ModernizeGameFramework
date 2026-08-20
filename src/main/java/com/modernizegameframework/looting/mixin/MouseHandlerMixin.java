package com.modernizegameframework.looting.mixin;

import com.modernizegameframework.looting.client.Core;
import com.modernizegameframework.looting.client.inventory.LootListInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鼠标处理器的全局 Mixin（高优先级）
 * 用于接管游戏内的鼠标滚动、点击和移动事件，实现快捷过滤操作及自定义面板的交互。
 */
@Mixin(value = MouseHandler.class, priority = 500)
public class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;

    /**
     * 获取考虑了 GUI 缩放比例后的实际鼠标 X 坐标
     */
    @Unique
    private double better_looting$getScaledMouseX() {
        if (this.minecraft.getWindow() == null) return this.minecraft.mouseHandler.xpos();
        return this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
    }

    /**
     * 获取考虑了 GUI 缩放比例后的实际鼠标 Y 坐标
     */
    @Unique
    private double better_looting$getScaledMouseY() {
        if (this.minecraft.getWindow() == null) return this.minecraft.mouseHandler.ypos();
        return this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();
    }

    /**
     * 拦截全局鼠标滚轮事件
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        // 1. 获取玩家是否按下了潜行键 (Shift)
        boolean isShiftDown = this.minecraft.options.keyShift.isDown();

        // 2. 处理模组 Core 逻辑层面的滚动需求
        if (!Core.INSTANCE.shouldIgnoreScroll()) {
            Core.INSTANCE.performScroll(yOffset);
            ci.cancel();
            return;
        }

        // 仅在打开容器界面时处理过滤面板和物品栏列表的滚动
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 物品栏掉落物列表滚动
        if (screen instanceof InventoryScreen) {
            if (LootListInteraction.INSTANCE.isMouseOverList(mouseX, mouseY)) {
                LootListInteraction.INSTANCE.handleScroll(yOffset);
                ci.cancel();
            }
        }
    }

    /**
     * 拦截全局鼠标按键（点击）事件
     */
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        // 物品拖拽释放（无论当前屏幕，释放拖拽状态）
        if (action == 0 && LootListInteraction.INSTANCE.isDraggingItem()) {
            if (this.minecraft.screen instanceof InventoryScreen invScreen) {
                LootListInteraction.INSTANCE.onItemRelease(invScreen);
            }
            ci.cancel();
            return;
        }

        // 滚动条拖拽释放
        if (action == 0 && LootListInteraction.INSTANCE.isDraggingScrollbar()) {
            LootListInteraction.INSTANCE.onScrollbarRelease();
            ci.cancel();
            return;
        }

        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 物品栏列表：滚动条按下优先
        if (action == 1 && containerScreen instanceof InventoryScreen) {
            if (LootListInteraction.INSTANCE.isMouseOverScrollbar(mouseX, mouseY)) {
                ci.cancel();
                LootListInteraction.INSTANCE.onScrollbarPress(mouseX, mouseY);
                return;
            }
        }

        // 物品栏列表：物品按下（仅左键，玩家手上没有已拿起的物品时）
        if (action == 1 && button == 0 && containerScreen instanceof InventoryScreen
                && containerScreen.getMenu().getCarried().isEmpty()) {
            if (LootListInteraction.INSTANCE.onItemPress(mouseX, mouseY)) {
                ci.cancel();
            }
        }
    }

    /**
     * 拦截鼠标移动事件，驱动物品栏掉落物列表的滚动条拖拽。
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    private void onMouseMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (this.minecraft.getWindow() == null) return;
        double mouseX = xpos * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
        double mouseY = ypos * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();

        if (LootListInteraction.INSTANCE.isDraggingItem()) {
            LootListInteraction.INSTANCE.onItemDrag(mouseX, mouseY);
        }
        if (LootListInteraction.INSTANCE.isDraggingScrollbar()) {
            LootListInteraction.INSTANCE.onScrollbarDrag(mouseX, mouseY);
        }
    }
}
