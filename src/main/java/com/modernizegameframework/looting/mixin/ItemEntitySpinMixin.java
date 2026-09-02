package com.modernizegameframework.looting.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 掉落物动画控制 Mixin。
 * 原版 ItemEntityRenderer 通过 getSpin(float) 的返回值选择渲染分支：
 * 返回 0 时走"静态平铺"分支（renderFlatItem），既不绕 Y 轴旋转，也不做 bob 上下浮动；
 * 返回非 0 时才进入"旋转 + 浮动"分支。本 Mixin 将该值固定为 0，
 * 由此同时去除掉落物的旋转与上下浮动动画，使其静态平放。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntitySpinMixin {

    /**
     * 固定为常量 0：让 ItemEntityRenderer 走入静态平铺渲染分支，
     * 掉落物既不再绕 Y 轴旋转，也不再上下浮动。
     */
    @Inject(method = "getSpin", at = @At("HEAD"), cancellable = true)
    private void modernizegameframework$noSpin(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }
}