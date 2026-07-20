package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 肢节血量系统客户端事件处理
 * 负责腿黑时禁止跳跃（客户端即时拦截，避免服务端修正带来的回弹）
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BodyPartClientEvents {

    private BodyPartClientEvents() {
    }

    /**
     * 腿黑时禁止跳跃
     * LivingJumpEvent 不可取消，通过撤销 Y 速度实现禁止跳跃
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!Config.BODYPART_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Player)) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getId() != event.getEntity().getId()) return;

        BodyPartHelper.getBodyPartCapability(player).ifPresent(cap -> {
            if (BodyPartPenaltyHandler.isLegDestroyed(cap)) {
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(delta.x, 0.0, delta.z);
            }
        });
    }
}
