package com.modernizegameframework.medical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 医疗系统客户端事件处理
 * 负责本地预测读条进度并驱动 HUD 显示
 */
@Mod.EventBusSubscriber(modid = com.modernizegameframework.ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MedicalClientHandler {

    /**
     * 当前客户端医疗会话
     */
    private static MedicalClientSession session = null;

    private MedicalClientHandler() {
    }

    /**
     * 开始客户端本地读条会话
     */
    public static void startSession(LocalPlayer player, InteractionHand hand, MedicalItem item, ItemStack stack) {
        session = new MedicalClientSession(player, hand, item, stack);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (session == null) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            session = null;
            MedicalHudRenderer.setProgress(0.0f, false, "");
            return;
        }

        if (!session.tick()) {
            session = null;
            MedicalHudRenderer.setProgress(0.0f, false, "");
            return;
        }

        MedicalHudRenderer.setProgress(session.getProgress(), true, session.getItemName());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        stopSession();
    }

    /**
     * 是否正在本地读条
     */
    public static boolean isInSession() {
        return session != null;
    }

    /**
     * 强制停止本地读条会话
     */
    public static void stopSession() {
        session = null;
        MedicalHudRenderer.setProgress(0.0f, false, "");
    }

    /**
     * 客户端医疗会话
     */
    private static class MedicalClientSession {
        private final LocalPlayer player;
        private final InteractionHand hand;
        private final MedicalItem item;
        private final ItemStack stack;
        private final Vec3 startPosition;
        private final int startSlot;
        private final int totalTicks;
        private int elapsedTicks = 0;

        MedicalClientSession(LocalPlayer player, InteractionHand hand, MedicalItem item, ItemStack stack) {
            this.player = player;
            this.hand = hand;
            this.item = item;
            this.stack = stack;
            this.startPosition = player.position();
            this.startSlot = player.getInventory().selected;
            this.totalTicks = item.getActivationTicks() + item.getUsageTicks();
        }

        boolean tick() {
            elapsedTicks++;
            if (elapsedTicks >= totalTicks) return false;

            // 被打断条件：移动、切换物品、物品变更
            if (player.getInventory().selected != startSlot) return false;
            ItemStack current = player.getItemInHand(hand);
            if (current.getItem() != item) return false;
            if (player.isSprinting() || player.isSwimming() || player.isFallFlying()) return false;
            if (player.getVehicle() != null) return false;
            if (player.position().distanceToSqr(startPosition) > 0.001) return false;

            return true;
        }

        float getProgress() {
            return totalTicks > 0 ? (float) elapsedTicks / totalTicks : 1.0f;
        }

        String getItemName() {
            return stack.getHoverName().getString();
        }
    }
}
