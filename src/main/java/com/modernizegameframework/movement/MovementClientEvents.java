package com.modernizegameframework.movement;

import com.modernizegameframework.ModernizeGameFramework;
import com.modernizegameframework.stamina.StaminaHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3;

/**
 * 惯性移动系统客户端事件处理
 *
 * 核心思想：客户端权威移动
 * - 客户端直接修改 LocalPlayer 的 deltaMovement
 * - 服务端跟随客户端的速度（MC 原版设计就是客户端权威）
 * - 零时序冲突，手感最佳
 *
 * 时序设计（关键）：
 * ClientTickEvent 在 aiStep() 之后触发，所以原版跳跃已经执行完毕
 * - END 阶段处理：
 *   1. 连跳保速 + 速度累加：刚落地 + 按住空格 → 原版已施加 Y=0.42，我们恢复水平速度并累加奖励
 *   2. 空中加速、地面惯性
 *
 * 三大模块：
 * 1. 地面惯性：松键滑行 + 按键渐进加速
 * 2. 空中加速：起源引擎 AirAccelerate（向量叠加）
 * 3. 连跳保速：落地瞬间保持水平速度 + 速度累加奖励按当前朝向刷新
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MovementClientEvents {

    /**
     * 上一 tick 的 onGround 状态，用于检测落地瞬间
     */
    private static boolean prevOnGround = true;

    /**
     * 上一 tick 的水平速度，用于连跳时恢复
     */
    private static Vec3 prevHorizontalDelta = Vec3.ZERO;

    /**
     * 连跳累加奖励速度（标量 m/s）
     * 每次连跳落地时，落地前速度的 10% 累加到此标量
     * 下次连跳时此速度按玩家当前按键朝向（wishDir）施加
     * 非连跳的普通落地会清零
     * 
     * 效果：转弯时奖励速度跟着朝向走，不会卡在死角方向
     */
    private static double bhopBonusSpeed = 0.0;

    /**
     * 上一 tick 的跳跃按键状态，用于检测按跳落地（非长按场景）
     */
    private static boolean prevJumpKey = false;

    /**
     * 客户端每 tick 处理惯性移动
     * 在 END 阶段执行（此时原版 aiStep 已完成）
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 未启用时重置状态
        if (!MovementConfig.ENABLED.get()) {
            prevOnGround = true;
            prevHorizontalDelta = Vec3.ZERO;
            bhopBonusSpeed = 0.0;
            prevJumpKey = false;
            return;
        }

        // 读取体力状态
        boolean staminaDepleted = StaminaHelper.getStamina(player)
                .map(s -> s.isDepleted())
                .orElse(false);

        boolean onGround = player.onGround();
        Vec3 delta = player.getDeltaMovement();
        boolean jumpKey = mc.options.keyJump.isDown();
        boolean justLanded = onGround && !prevOnGround;

        // 更新上次跳跃键状态
        prevJumpKey = jumpKey;

        // === 场景 1：刚落地 + 按住空格 → 连跳保速 + 速度累加 ===
        // 原版 aiStep() 已经检测到 onGround + jumpKey 触发了跳跃（Y=0.42）
        // 我们恢复落地前水平速度，累加奖励按当前按键朝向施加，跳过原版地面摩擦衰减
        if (justLanded && MovementConfig.AUTO_BHOP.get() && jumpKey && !staminaDepleted) {
            if (MovementConfig.BHOP_KEEP_SPEED.get()) {
                // 累加落地前速度的 10% 到奖励标量，达到上限后不再增加
                double prevSpeed = prevHorizontalDelta.length();
                double maxBonus = MovementConfig.BHOP_BONUS_MAX_SPEED.get();
                bhopBonusSpeed = Math.min(bhopBonusSpeed + prevSpeed * MovementConfig.BHOP_ACCUMULATE_RATE.get(), maxBonus);
                // 奖励速度按当前按键朝向施加（wishDir），转弯时跟着朝向刷新方向
                Vec3 wishDir = MovementHelper.getWishDir(player);
                if (wishDir.lengthSqr() < 1.0E-8) {
                    // 无输入时按原速度方向施加奖励
                    wishDir = prevSpeed > 0.001 ? prevHorizontalDelta.scale(1.0 / prevSpeed) : Vec3.ZERO;
                }
                // 新水平速度 = 落地前速度 + 奖励标量 × 当前按键朝向
                Vec3 newHorizontal = prevHorizontalDelta.add(wishDir.scale(bhopBonusSpeed));
                player.setDeltaMovement(newHorizontal.x, delta.y, newHorizontal.z);
            }
            // 连跳不消耗体力，发送标记包防止 LivingJumpEvent 重复扣费
            MovementNetwork.CHANNEL.sendToServer(new MovementNetwork.BhopConsumePacket(0));
            prevOnGround = false; // 跳起来了
            prevHorizontalDelta = new Vec3(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            return;
        }

        // === 场景 2：普通落地（非连跳）→ 清零累加奖励 ===
        if (justLanded) {
            bhopBonusSpeed = 0.0;
        }

        // === 场景 3：体力归零 + 在地面 → 强摩擦 ===
        if (onGround && staminaDepleted) {
            double friction = MovementConfig.DEPLETED_FRICTION.get();
            player.setDeltaMovement(delta.x * friction, delta.y, delta.z * friction);
            prevOnGround = onGround;
            prevHorizontalDelta = new Vec3(delta.x, 0, delta.z);
            return;
        }

        // === 场景 4：空中 → 起源引擎 AirAccelerate ===
        if (!onGround) {
            applyAirAccelerate(player, delta);
        } else {
            // 地面惯性：松键时减少摩擦，滑行感
            applyGroundInertia(player, delta);
        }

        prevOnGround = onGround;
        prevHorizontalDelta = new Vec3(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
    }

    /**
     * 空中加速：起源引擎 AirAccelerate
     */
    private static void applyAirAccelerate(LocalPlayer player, Vec3 delta) {
        Vec3 wishDir = MovementHelper.getWishDir(player);
        if (wishDir.lengthSqr() < 1.0E-8) return;

        double wishSpeed = MovementConfig.AIR_WISH_SPEED.get() * MovementConfig.AIR_CONTROL.get();
        double airAccel = MovementConfig.AIR_ACCEL.get();

        // 当前水平速度
        Vec3 horizontalVel = new Vec3(delta.x, 0, delta.z);
        // 空中加速
        Vec3 newHorizontalVel = MovementHelper.airAccelerate(horizontalVel, wishDir, wishSpeed, airAccel);

        player.setDeltaMovement(newHorizontalVel.x, delta.y, newHorizontalVel.z);
    }

    /**
     * 地面惯性：松键时滑行，按键时渐进加速
     * 注意：只在松键时施加摩擦，按键时让原版处理加速
     */
    private static void applyGroundInertia(LocalPlayer player, Vec3 delta) {
        // 如果玩家没有方向输入，减少摩擦实现滑行
        boolean hasInput = player.zza != 0.0F || player.xxa != 0.0F;
        if (!hasInput) {
            // 计算当前水平速度
            double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (speed > 0.01) {
                // 应用自定义摩擦（比原版更滑）
                double friction = MovementConfig.GROUND_FRICTION.get();
                player.setDeltaMovement(delta.x * friction, delta.y, delta.z * friction);
            }
        }
        // 有输入时不干预，让原版处理加速
    }
}
