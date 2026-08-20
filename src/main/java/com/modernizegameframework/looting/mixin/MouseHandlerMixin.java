package com.modernizegameframework.looting.mixin;

import com.modernizegameframework.looting.client.Core;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鼠标处理器的全局 Mixin（高优先级）。
 * 拦截游戏内的鼠标滚轮，将其交给 Core 处理列表参考滚动（选择索引）。
 */
@Mixin(value = MouseHandler.class, priority = 500)
public class MouseHandlerMixin {

    /**
     * 拦截全局鼠标滚轮事件。
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        // 当模组需要接管滚动（HUD/列表参考选定）时，交予 Core 处理
        if (!Core.INSTANCE.shouldIgnoreScroll()) {
            Core.INSTANCE.performScroll(yOffset);
            ci.cancel();
        }
    }
}