package com.modernizegameframework.movement;

import com.modernizegameframework.Config;
import com.modernizegameframework.ModernizeGameFramework;
import com.modernizegameframework.bodypart.BodyPartHelper;
import com.modernizegameframework.bodypart.BodyPartPenaltyHandler;
import com.modernizegameframework.stamina.StaminaHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
 *   1. 连跳峰值速度回归：刚落地 + 按住空格 → 水平速度回到连跳期间峰值
 *   2. 空中加速、地面惯性
 *
 * 连跳峰值速度回归（替代 10% 累加）：
 * - 连跳期间追踪最大水平速度
 * - 每次落地连跳时，速度回归到峰值大小
 * - 方向按 90% 惯性 + 10% 按键朝向 混合
 * - 普通落地清零所有统计
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MovementClientEvents {

    // === 移动状态 ===

    private static boolean prevOnGround = true;
    private static Vec3 prevHorizontalDelta = Vec3.ZERO;
    private static boolean prevJumpKey = false;

    // === 连跳统计数据（public 供 HUD 读取） ===

    /** 连跳期间峰值速度（m/s） */
    public static double bhopChainPeakSpeed = 0.0;
    /** 连跳期间平均速度（m/s） */
    public static double bhopChainAvgSpeed = 0.0;
    /** 连跳期间最远单次跳跃距离（方块） */
    public static double bhopChainLongestJump = 0.0;

    // === 统计内部追踪 ===

    private static double bhopChainSpeedSum = 0.0;
    private static int bhopChainSpeedCount = 0;
    private static Vec3 bhopTakeoffPos = null;
    private static boolean bhopChainActive = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!MovementConfig.ENABLED.get()) {
            resetAll();
            return;
        }

        boolean staminaDepleted = StaminaHelper.getStamina(player)
                .map(s -> s.isDepleted())
                .orElse(false);
        boolean legDestroyed = Config.BODYPART_ENABLED.get()
                && BodyPartHelper.getBodyPartCapability(player)
                        .map(BodyPartPenaltyHandler::isLegDestroyed)
                        .orElse(false);

        boolean onGround = player.onGround();
        Vec3 delta = player.getDeltaMovement();
        boolean jumpKey = mc.options.keyJump.isDown();
        boolean justLanded = onGround && !prevOnGround;
        boolean justTookOff = !onGround && prevOnGround;

        prevJumpKey = jumpKey;

        // === 场景 1：刚落地 + 按住空格 → 连跳峰值速度回归 ===
        // 原版 aiStep() 已触发跳跃（Y=0.42），我们恢复水平速度到峰值大小
        if (justLanded && MovementConfig.AUTO_BHOP.get() && jumpKey && !staminaDepleted) {
            if (MovementConfig.BHOP_KEEP_SPEED.get() && bhopChainActive && bhopChainPeakSpeed > 0.01) {
                // 计算本次跳跃距离
                if (bhopTakeoffPos != null) {
                    double jumpDist = Math.sqrt(
                            (player.getX() - bhopTakeoffPos.x) * (player.getX() - bhopTakeoffPos.x) +
                            (player.getZ() - bhopTakeoffPos.z) * (player.getZ() - bhopTakeoffPos.z));
                    bhopChainLongestJump = Math.max(bhopChainLongestJump, jumpDist);
                }

                // 急停惩罚：惯性方向与按键朝向夹角过大时削减
                Vec3 wishDir = MovementHelper.getWishDir(player);
                double prevSpeed = prevHorizontalDelta.length();
                if (wishDir.lengthSqr() < 1.0E-8) {
                    wishDir = prevSpeed > 0.001 ? prevHorizontalDelta.scale(1.0 / prevSpeed) : Vec3.ZERO;
                }
                Vec3 penalizedPrev = MovementHelper.applyStopSpeed(prevHorizontalDelta, wishDir,
                        MovementConfig.STOP_SPEED_THRESHOLD_ANGLE.get(), MovementConfig.STOP_SPEED_MAX_PENALTY.get());

                // 90% 惯性方向 + 10% 按键朝向 → 混合后归一化 → 峰值速度（转回 blocks/tick）
                Vec3 penalizedDir = penalizedPrev.length() > 0.001 ? penalizedPrev.normalize() : wishDir;
                Vec3 blendedDir = penalizedDir.scale(0.9).add(wishDir.scale(0.1));
                if (blendedDir.lengthSqr() > 1.0E-8) blendedDir = blendedDir.normalize();
                Vec3 newHorizontal = blendedDir.scale(bhopChainPeakSpeed / 20.0); // m/s → blocks/tick

                player.setDeltaMovement(newHorizontal.x, delta.y, newHorizontal.z);
            }
            // 记录起跳位置（用于下一跳距离计算）
            bhopTakeoffPos = new Vec3(player.getX(), 0, player.getZ());
            MovementNetwork.CHANNEL.sendToServer(new MovementNetwork.BhopConsumePacket(0));
            prevOnGround = false;
            prevHorizontalDelta = new Vec3(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            return;
        }

        // === 场景 2：普通落地 → 清零所有统计 ===
        if (justLanded) {
            resetBhopStats();
        }

        // === 场景 3：刚离地 → 记录起跳位置，激活连跳链 ===
        if (justTookOff && jumpKey) {
            bhopTakeoffPos = new Vec3(player.getX(), 0, player.getZ());
            double takeoffSpeed = prevHorizontalDelta.length() * 20.0; // 转为 m/s
            if (!bhopChainActive && takeoffSpeed > 3.0) {
                bhopChainActive = true;
                bhopChainPeakSpeed = takeoffSpeed;
                bhopChainSpeedSum = takeoffSpeed;
                bhopChainSpeedCount = 1;
                bhopChainLongestJump = 0.0;
            }
        }

        // === 场景 4：体力归零 + 在地面 → 强摩擦 ===
        if (onGround && staminaDepleted) {
            double friction = MovementConfig.DEPLETED_FRICTION.get();
            player.setDeltaMovement(delta.x * friction, delta.y, delta.z * friction);
            prevOnGround = onGround;
            prevHorizontalDelta = new Vec3(delta.x, 0, delta.z);
            return;
        }

        // === 场景 5：空中 → 更新峰值 + AirAccelerate ===
        if (!onGround) {
            double currentSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z) * 20.0; // m/s
            if (bhopChainActive) {
                bhopChainPeakSpeed = Math.max(bhopChainPeakSpeed, currentSpeed);
                bhopChainSpeedSum += currentSpeed;
                bhopChainSpeedCount++;
                bhopChainAvgSpeed = bhopChainSpeedSum / bhopChainSpeedCount;
            }
            applyAirAccelerate(player, delta);
        } else {
            applyGroundInertia(player, delta);
        }

        // === 速度上限：体力耗尽 5m/s，腿黑 2m/s ===
        applySpeedCap(player, staminaDepleted, legDestroyed);

        prevOnGround = onGround;
        prevHorizontalDelta = new Vec3(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
    }

    /**
     * 应用体力耗尽 / 腿黑的水平速度上限
     * 腿黑上限（2m/s）优先于体力耗尽上限（5m/s）
     */
    private static void applySpeedCap(LocalPlayer player, boolean staminaDepleted, boolean legDestroyed) {
        double limit = Double.MAX_VALUE;
        if (legDestroyed) {
            limit = Config.BODYPART_LEG_DESTROYED_SPEED_LIMIT.get();
        } else if (staminaDepleted) {
            limit = MovementConfig.DEPLETED_SPEED_LIMIT.get();
        }
        if (limit <= 0.0) return;

        Vec3 delta = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
        double speed = horizontal.length() * 20.0;
        if (speed > limit) {
            Vec3 clamped = horizontal.normalize().scale(limit / 20.0);
            player.setDeltaMovement(clamped.x, delta.y, clamped.z);
        }
    }

    private static void applyAirAccelerate(LocalPlayer player, Vec3 delta) {
        Vec3 wishDir = MovementHelper.getWishDir(player);
        if (wishDir.lengthSqr() < 1.0E-8) return;

        Vec3 horizontalVel = new Vec3(delta.x, 0, delta.z);
        horizontalVel = MovementHelper.applyStopSpeed(horizontalVel, wishDir,
                MovementConfig.STOP_SPEED_THRESHOLD_ANGLE.get(), MovementConfig.STOP_SPEED_MAX_PENALTY.get());

        double wishSpeed = MovementConfig.AIR_WISH_SPEED.get() * MovementConfig.AIR_CONTROL.get();
        double airAccel = MovementConfig.AIR_ACCEL.get();
        Vec3 newHorizontalVel = MovementHelper.airAccelerate(horizontalVel, wishDir, wishSpeed, airAccel);

        player.setDeltaMovement(newHorizontalVel.x, delta.y, newHorizontalVel.z);
    }

    private static void applyGroundInertia(LocalPlayer player, Vec3 delta) {
        boolean hasInput = player.zza != 0.0F || player.xxa != 0.0F;
        if (!hasInput) {
            double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (speed > 0.01) {
                double friction = MovementConfig.GROUND_FRICTION.get();
                player.setDeltaMovement(delta.x * friction, delta.y, delta.z * friction);
            }
        }
    }

    /** 清零连跳统计数据 */
    private static void resetBhopStats() {
        bhopChainActive = false;
        bhopChainPeakSpeed = 0.0;
        bhopChainAvgSpeed = 0.0;
        bhopChainLongestJump = 0.0;
        bhopChainSpeedSum = 0.0;
        bhopChainSpeedCount = 0;
        bhopTakeoffPos = null;
    }

    /** 完全重置所有状态 */
    private static void resetAll() {
        prevOnGround = true;
        prevHorizontalDelta = Vec3.ZERO;
        prevJumpKey = false;
        resetBhopStats();
    }
}