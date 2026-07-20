package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 体力值系统客户端事件监听器
 * 检测疾跑按键状态并向服务端发送同步包
 * 体力耗尽时强制释放疾跑按键，阻止玩家疾跑
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StaminaClientEvents {

    /**
     * 上一 tick 的疾跑按键状态，用于检测变化
     */
    private static boolean lastSprintKeyState = false;

    /**
     * 客户端每 tick 检测按键状态变化，体力耗尽时强制释放疾跑键
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 体力耗尽时强制释放疾跑按键，阻止玩家疾跑
        StaminaHelper.getStamina(mc.player).ifPresent(stamina -> {
            if (stamina.isDepleted()) {
                mc.options.keySprint.setDown(false);
            }
        });

        // 检测按键状态变化并同步到服务端
        boolean currentSprintKeyState = mc.options.keySprint.isDown();
        if (currentSprintKeyState != lastSprintKeyState) {
            lastSprintKeyState = currentSprintKeyState;
            StaminaNetwork.CHANNEL.sendToServer(new StaminaNetwork.SprintKeyPacket(currentSprintKeyState));
        }
    }
}
