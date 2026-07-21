package com.modernizegameframework.medical;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 医疗系统服务端事件处理
 * 管理所有玩家的医疗读条会话，处理每 tick 更新与受伤打断
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MedicalHandler {

    /**
     * 玩家 UUID 到医疗会话的映射
     */
    private static final Map<UUID, MedicalSession> SESSIONS = new HashMap<>();

    private MedicalHandler() {
    }

    /**
     * 开始一个新的医疗读条会话
     */
    public static void startSession(Player player, ItemStack stack, InteractionHand hand, MedicalItem item) {
        // 同一玩家已有会话则直接覆盖（重新右键）
        SESSIONS.put(player.getUUID(), new MedicalSession(player, hand, item, stack));
    }

    /**
     * 获取玩家当前会话
     */
    public static MedicalSession getSession(Player player) {
        return SESSIONS.get(player.getUUID());
    }

    /**
     * 每 tick 更新所有医疗会话
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        MedicalSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        boolean continuing = session.tick();
        if (!continuing) {
            session.finish(true);
            SESSIONS.remove(player.getUUID());
        }
    }

    /**
     * 玩家受伤时打断当前医疗读条
     * 拥有"无视"性质的物品不会被伤害打断
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        MedicalSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.getItem().getEffect().isUnbreakable()) {
            return;
        }

        session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.finish(false);
        }
    }

    /**
     * 判断玩家是否正在医疗读条
     */
    public static boolean isInSession(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /**
     * 强制停止某玩家的医疗会话（例如切换物品时）
     */
    public static void stopSession(Player player) {
        MedicalSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.finish(false);
        }
    }
}
