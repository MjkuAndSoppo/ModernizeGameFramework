package com.modernizegameframework.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 移动系统工具函数
 * 提供起源引擎风格的空中加速、期望方向计算等
 */
public class MovementHelper {

    private MovementHelper() {}

    /**
     * 根据玩家输入（前/后/左/右）与水平视角计算期望移动方向（归一化）
     * 算法与原版 LivingEntity.getInputVector 一致，仅取水平分量
     *
     * @param player 玩家
     * @return 归一化的水平期望方向，无输入时返回 Vec3.ZERO
     */
    public static Vec3 getWishDir(Player player) {
        float yaw = player.getYRot();
        float forward = player.zza; // 前后输入
        float strafe = player.xxa;  // 左右输入

        if (forward == 0.0F && strafe == 0.0F) return Vec3.ZERO;

        float len = Mth.sqrt(forward * forward + strafe * strafe);
        float normForward = forward / len;
        float normStrafe = strafe / len;

        float yawRad = (float) Math.toRadians(yaw);
        float sinY = Mth.sin(yawRad);
        float cosY = Mth.cos(yawRad);

        // 与原版 getInputVector 公式一致
        double dx = normStrafe * cosY - normForward * sinY;
        double dz = normStrafe * sinY + normForward * cosY;

        return new Vec3(dx, 0.0, dz);
    }

    /**
     * 起源引擎 AirAccelerate 算法
     * 核心原理：鼠标转向时，原速度在新方向的投影变小，于是能继续加速
     * 速度作为向量参与计算，方向改变但大小可以保持甚至增加
     *
     * @param velocity  当前水平速度向量（x, 0, z）
     * @param wishDir   期望方向（归一化）
     * @param wishSpeed 期望速度上限
     * @param airAccel  空中加速度系数
     * @return 加速后的新水平速度向量
     */
    public static Vec3 airAccelerate(Vec3 velocity, Vec3 wishDir, double wishSpeed, double airAccel) {
        if (wishDir.lengthSqr() < 1.0E-8) return velocity;

        // 当前速度在期望方向上的投影
        double currentSpeed = velocity.x * wishDir.x + velocity.z * wishDir.z;
        // 还能加多少速度
        double addSpeed = wishSpeed - currentSpeed;

        if (addSpeed <= 0) return velocity;

        // frametime 近似 0.05（20 tick/s）
        double accelSpeed = Math.min(airAccel * wishSpeed * 0.05, addSpeed);

        return new Vec3(
                velocity.x + wishDir.x * accelSpeed,
                0.0,
                velocity.z + wishDir.z * accelSpeed
        );
    }

    /**
     * 渐进式接近目标速度（用于地面加速）
     *
     * @param current 当前速度
     * @param target  目标速度
     * @param maxStep 单步最大变化量
     * @return 接近后的速度
     */
    public static double approach(double current, double target, double maxStep) {
        if (current < target) {
            return Math.min(current + maxStep, target);
        } else if (current > target) {
            return Math.max(current - maxStep, target);
        }
        return current;
    }

    /**
     * 检测玩家主手或副手是否持有基岩
     * 手持基岩时所有体力消耗行为均豁免
     *
     * @param player 玩家
     * @return 是否手持基岩
     */
    public static boolean isBedrockInHand(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(Blocks.BEDROCK.asItem()) || offHand.is(Blocks.BEDROCK.asItem());
    }
}
