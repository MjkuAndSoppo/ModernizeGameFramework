package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 肢节血量伤害判定处理器
 * 负责将原版伤害转换为对具体部位的伤害，并支持 TAC:Z 爆头判定扩展
 */
public class BodyPartDamageHandler {

    private static final Random RANDOM = new Random();

    private BodyPartDamageHandler() {}

    /**
     * 判断伤害来源是否为 TAC:Z 爆头
     * 通过检测抛射物实体的持久化 NBT 标签实现预留接口
     *
     * @param source 伤害来源
     * @return 是否为爆头
     */
    public static boolean isTaczHeadshot(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null) return false;
        CompoundTag tag = direct.getPersistentData();
        // 预留 TAC:Z 爆头标记，具体键名需根据 TAC:Z 实际实现调整
        return tag.getBoolean("mgfHeadshot") || tag.getBoolean("isHeadshot");
    }

    /**
     * 判断是否为摔落伤害
     *
     * @param source 伤害来源
     * @return 是否摔落
     */
    private static boolean isFallDamage(DamageSource source) {
        return source.is(DamageTypes.FALL);
    }

    /**
     * 判断是否为爆炸伤害
     * 包括 TNT、苦力怕、床/重生锚等
     *
     * @param source 伤害来源
     * @return 是否爆炸
     */
    private static boolean isExplosionDamage(DamageSource source) {
        return source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
    }

    /**
     * 判断是否为无明确方向的环境伤害
     * 例如火焰、窒息、溺水、饥饿等
     *
     * @param source 伤害来源
     * @return 是否环境伤害
     */
    private static boolean isEnvironmentalDamage(DamageSource source) {
        return source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.WITHER);
    }

    /**
     * 判断是否为远程伤害
     * 直接伤害实体与攻击者实体不同（如箭矢、子弹）时视为远程
     *
     * @param source 伤害来源
     * @return 是否远程
     */
    private static boolean isRanged(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        return direct != null && attacker != null && direct != attacker;
    }

    /**
     * 判断是否为近战伤害
     * 直接伤害实体与攻击者实体相同（如玩家/僵尸挥击）时视为近战
     *
     * @param source 伤害来源
     * @return 是否近战
     */
    private static boolean isMelee(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        return direct != null && attacker != null && direct == attacker;
    }

    /**
     * 根据伤害来源和受害者位置反推命中部位
     * 远程以弹着点为准，近战以攻击者朝向和相对位置为准
     *
     * @param victim 受害者
     * @param source 伤害来源
     * @return 命中部位
     */
    public static BodyPartType resolveHitPart(Player victim, DamageSource source) {
        if (isTaczHeadshot(source)) {
            return BodyPartType.HEAD;
        }

        if (isRanged(source)) {
            // 远程：以弹射物实体位置（弹着点）作为命中参考
            Entity projectile = source.getDirectEntity();
            return resolveByPosition(victim, projectile.position());
        }

        // 近战：以实际攻击者位置与眼睛高度为准
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (attacker == null) {
            return resolveByPosition(victim, getSourcePosition(source, victim));
        }
        return resolveMelee(victim, attacker);
    }

    /**
     * 获取伤害来源位置
     *
     * @param source 伤害来源
     * @param victim 受害者
     * @return 来源位置
     */
    private static Vec3 getSourcePosition(DamageSource source, Player victim) {
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            return direct.position();
        }
        Entity entity = source.getEntity();
        if (entity != null) {
            return entity.position();
        }
        Vec3 pos = source.getSourcePosition();
        return pos != null ? pos : victim.position();
    }

    /**
     * 根据来源相对位置判断命中部位
     * 用于远程或 fallback，按攻击高度与方位分配
     *
     * @param victim    受害者
     * @param sourcePos 来源位置
     * @return 命中部位
     */
    private static BodyPartType resolveByPosition(Player victim, Vec3 sourcePos) {
        Vec3 victimPos = victim.position();
        Vec3 victimLook = victim.getLookAngle();
        victimLook = new Vec3(victimLook.x, 0.0, victimLook.z).normalize();
        Vec3 toSource = new Vec3(sourcePos.x - victimPos.x, 0.0, sourcePos.z - victimPos.z);
        if (toSource.lengthSqr() < 0.0001) {
            toSource = victimLook.scale(-1);
        } else {
            toSource = toSource.normalize();
        }

        return resolveByHeight(victim, sourcePos.y, victimLook, toSource);
    }

    /**
     * 近战判定
     * 正面（受害者面向攻击者）80% 命中手臂；背面 60% 命中腿部
     *
     * @param victim   受害者
     * @param attacker 攻击者
     * @return 命中部位
     */
    private static BodyPartType resolveMelee(Player victim, Entity attacker) {
        Vec3 victimPos = victim.position();
        Vec3 attackerPos = attacker.position();
        double hitY = attacker.getEyeY();

        Vec3 victimLook = victim.getLookAngle();
        victimLook = new Vec3(victimLook.x, 0.0, victimLook.z).normalize();
        Vec3 toAttacker = new Vec3(attackerPos.x - victimPos.x, 0.0, attackerPos.z - victimPos.z);
        if (toAttacker.lengthSqr() < 0.0001) {
            toAttacker = victimLook.scale(-1);
        } else {
            toAttacker = toAttacker.normalize();
        }

        double dot = victimLook.dot(toAttacker);
        boolean frontal = dot > 0.5;
        boolean back = dot < -0.5;

        if (frontal) {
            // 正面对砍 mostly 打手臂（举盾/格挡姿态）
            if (RANDOM.nextFloat() < 0.8f) {
                return resolveArm(victimLook, toAttacker);
            }
            return resolveByHeight(victim, hitY, victimLook, toAttacker);
        }

        if (back) {
            // 背刺更容易打到腿
            if (RANDOM.nextFloat() < 0.6f) {
                return resolveLeg(victimLook, toAttacker);
            }
            return resolveByHeight(victim, hitY, victimLook, toAttacker);
        }

        return resolveByHeight(victim, hitY, victimLook, toAttacker);
    }

    /**
     * 按攻击高度判定命中部位
     *
     * @param victim     受害者
     * @param hitY       攻击点高度
     * @param victimLook 受害者头部朝向
     * @param toSource   从受害者指向来源的方向
     * @return 命中部位
     */
    private static BodyPartType resolveByHeight(Player victim, double hitY, Vec3 victimLook, Vec3 toSource) {
        double eyeY = victim.getEyeY();
        double waist = victim.position().y + victim.getBbHeight() * 0.45;

        // 头部判定：高于眼睛或上半身顶部
        if (hitY >= eyeY - 0.1) {
            return BodyPartType.HEAD;
        }

        // 躯干/手臂区域
        if (hitY >= waist) {
            // 侧面更容易打到手臂，正面/背面以躯干为主
            boolean side = Math.abs(victimLook.dot(toSource)) < 0.5;
            boolean hitArm = side ? RANDOM.nextFloat() < 0.6f : RANDOM.nextFloat() < 0.3f;
            if (hitArm) {
                return resolveArm(victimLook, toSource);
            }
            return BodyPartType.BODY;
        }

        return resolveLeg(victimLook, toSource);
    }

    /**
     * 判定命中左手还是右手
     *
     * @param victimLook 受害者头部朝向
     * @param toSource   从受害者指向伤害来源的方向
     * @return 左手或右手
     */
    private static BodyPartType resolveArm(Vec3 victimLook, Vec3 toSource) {
        Vec3 right = victimLook.yRot(-90.0f * (float) Math.PI / 180.0f).normalize();
        double sideDot = right.dot(toSource);
        return sideDot > 0 ? BodyPartType.RIGHT_ARM : BodyPartType.LEFT_ARM;
    }

    /**
     * 判定命中左腿还是右腿
     *
     * @param victimLook 受害者头部朝向
     * @param toSource   从受害者指向伤害来源的方向
     * @return 左腿或右腿
     */
    private static BodyPartType resolveLeg(Vec3 victimLook, Vec3 toSource) {
        Vec3 right = victimLook.yRot(-90.0f * (float) Math.PI / 180.0f).normalize();
        double sideDot = right.dot(toSource);
        return sideDot > 0 ? BodyPartType.RIGHT_LEG : BodyPartType.LEFT_LEG;
    }

    /**
     * 对玩家应用肢节血量伤害
     * 取消原版伤害，改为扣除指定部位血量
     *
     * @param victim 受害者
     * @param source 伤害来源
     * @param amount 伤害量
     */
    public static void applyBodyPartDamage(Player victim, DamageSource source, float amount) {
        if (!Config.BODYPART_ENABLED.get()) return;

        if (isExplosionDamage(source)) {
            // 爆炸伤害四倍
            applyExplosionDamage(victim, source, amount * 4.0f);
        } else if (isFallDamage(source)) {
            // 肢体系统开启时摔落伤害提升为 8 倍
            applyFallDamage(victim, amount * 8.0f);
        } else if (isEnvironmentalDamage(source)) {
            applyEnvironmentalDamage(victim, amount);
        } else {
            // 近战伤害双倍
            if (isMelee(source)) {
                amount *= 2.0f;
            }
            BodyPartType part = resolveHitPart(victim, source);
            applySinglePartDamage(victim, part, amount);
        }

        BodyPartNetwork.syncToClient(victim);
    }

    /**
     * 对单个部位造成伤害并触发疼痛、出血等后续效果
     * 若命中部位已黑，则伤害按 1.7 倍均摊给所有未黑部位
     *
     * @param victim 受害者
     * @param part   命中部位
     * @param amount 伤害量
     */
    private static void applySinglePartDamage(Player victim, BodyPartType part, float amount) {
        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            if (cap.isDestroyed(part)) {
                float redistributed = amount * 1.7f;
                List<BodyPartType> alive = new ArrayList<>();
                for (BodyPartType type : BodyPartType.values()) {
                    if (!cap.isDestroyed(type)) {
                        alive.add(type);
                    }
                }

                if (alive.isEmpty()) {
                    BodyPartHelper.applyDamage(victim, BodyPartType.BODY, redistributed);
                    applySecondaryEffects(victim, BodyPartType.BODY);
                } else {
                    float perPart = redistributed / alive.size();
                    for (BodyPartType type : alive) {
                        BodyPartHelper.applyDamage(victim, type, perPart);
                    }
                    BodyPartType primary = pickLowestHealthRatioPart(cap, alive);
                    applySecondaryEffects(victim, primary);
                }
            } else {
                BodyPartHelper.applyDamage(victim, part, amount);
                applySecondaryEffects(victim, part);
            }
        });
        checkFatal(victim);
    }

    /**
     * 处理爆炸伤害
     * 命中的主部位承受 30%，其余 70% 均摊给其他未黑部位
     * 若主部位已黑，则全部按 1.7 倍均摊给其他未黑部位
     *
     * @param victim 受害者
     * @param source 伤害来源
     * @param amount 伤害量（已乘 4）
     */
    private static void applyExplosionDamage(Player victim, DamageSource source, float amount) {
        Vec3 explosionPos = source.getSourcePosition();
        if (explosionPos == null) {
            explosionPos = getSourcePosition(source, victim);
        }
        BodyPartType hitPart = resolveByPosition(victim, explosionPos);

        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            List<BodyPartType> others = new ArrayList<>();
            for (BodyPartType type : BodyPartType.values()) {
                if (type != hitPart && !cap.isDestroyed(type)) {
                    others.add(type);
                }
            }

            if (!cap.isDestroyed(hitPart)) {
                BodyPartHelper.applyDamage(victim, hitPart, amount * 0.3f);
                float perPart = (amount * 0.7f) / others.size();
                for (BodyPartType type : others) {
                    BodyPartHelper.applyDamage(victim, type, perPart);
                }
                applySecondaryEffects(victim, hitPart);
            } else {
                // 主部位已黑，全部按 1.7 倍均摊
                float redistributed = amount * 1.7f;
                if (others.isEmpty()) {
                    BodyPartHelper.applyDamage(victim, BodyPartType.BODY, redistributed);
                    applySecondaryEffects(victim, BodyPartType.BODY);
                } else {
                    float perPart = redistributed / others.size();
                    for (BodyPartType type : others) {
                        BodyPartHelper.applyDamage(victim, type, perPart);
                    }
                    applySecondaryEffects(victim, pickLowestHealthRatioPart(cap, others));
                }
            }
        });
        checkFatal(victim);
    }

    /**
     * 处理摔落伤害
     * 伤害已由调用方乘以 8 倍，先用双腿当前血量吸收；
     * 平均分配后仍有剩余的部分作为溢出伤害，均摊给全身所有部位。
     *
     * @param victim 受害者
     * @param amount 伤害量（已乘 8）
     */
    private static void applyFallDamage(Player victim, float amount) {
        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            float leftHealth = cap.getHealth(BodyPartType.LEFT_LEG);
            float rightHealth = cap.getHealth(BodyPartType.RIGHT_LEG);

            // 先用双腿血量吸收摔落伤害，尽量平均分配
            float leftDamage = Math.min(leftHealth, amount / 2.0f);
            float rightDamage = Math.min(rightHealth, amount / 2.0f);
            float remaining = amount - leftDamage - rightDamage;

            // 若一条腿先满，让还有余量的腿继续承担剩余伤害
            if (remaining > 0.0f) {
                float leftCapacity = leftHealth - leftDamage;
                if (leftCapacity > 0.0f) {
                    float extra = Math.min(remaining, leftCapacity);
                    leftDamage += extra;
                    remaining -= extra;
                }
                float rightCapacity = rightHealth - rightDamage;
                if (remaining > 0.0f && rightCapacity > 0.0f) {
                    float extra = Math.min(remaining, rightCapacity);
                    rightDamage += extra;
                    remaining -= extra;
                }
            }

            List<BodyPartType> affectedParts = new ArrayList<>();
            if (leftDamage > 0.0f) {
                BodyPartHelper.applyDamage(victim, BodyPartType.LEFT_LEG, leftDamage);
                affectedParts.add(BodyPartType.LEFT_LEG);
            }
            if (rightDamage > 0.0f) {
                BodyPartHelper.applyDamage(victim, BodyPartType.RIGHT_LEG, rightDamage);
                affectedParts.add(BodyPartType.RIGHT_LEG);
            }

            // 超出双腿总血量的溢出伤害扩散到全身
            if (remaining > 0.0f) {
                float perPart = remaining / BodyPartType.values().length;
                for (BodyPartType type : BodyPartType.values()) {
                    BodyPartHelper.applyDamage(victim, type, perPart);
                    if (!affectedParts.contains(type)) {
                        affectedParts.add(type);
                    }
                }
            }

            if (affectedParts.isEmpty()) {
                applySecondaryEffects(victim, BodyPartType.BODY);
            } else {
                applySecondaryEffects(victim, pickLowestHealthRatioPart(cap, affectedParts));
            }
        });
        checkFatal(victim);
    }

    /**
     * 处理无方向环境伤害
     * 均摊给所有未黑部位
     *
     * @param victim 受害者
     * @param amount 伤害量
     */
    private static void applyEnvironmentalDamage(Player victim, float amount) {
        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            List<BodyPartType> targets = new ArrayList<>();
            for (BodyPartType type : BodyPartType.values()) {
                if (!cap.isDestroyed(type)) {
                    targets.add(type);
                }
            }

            if (targets.isEmpty()) {
                BodyPartHelper.applyDamage(victim, BodyPartType.BODY, amount);
                applySecondaryEffects(victim, BodyPartType.BODY);
            } else {
                float perPart = amount / targets.size();
                for (BodyPartType type : targets) {
                    BodyPartHelper.applyDamage(victim, type, perPart);
                }
                BodyPartType primary = pickLowestHealthRatioPart(cap, targets);
                applySecondaryEffects(victim, primary);
            }
        });
        checkFatal(victim);
    }

    /**
     * 从目标部位中选出当前血量百分比最低的部位，用于触发次要效果
     *
     * @param cap     肢节血量能力
     * @param targets 候选部位列表
     * @return 血量百分比最低的部位
     */
    private static BodyPartType pickLowestHealthRatioPart(BodyPartCapability cap, List<BodyPartType> targets) {
        BodyPartType primary = targets.get(0);
        float primaryRatio = cap.getHealth(primary) / Math.max(1.0f, cap.getMaxHealth(primary));
        for (BodyPartType type : targets) {
            float ratio = cap.getHealth(type) / Math.max(1.0f, cap.getMaxHealth(type));
            if (ratio < primaryRatio) {
                primaryRatio = ratio;
                primary = type;
            }
        }
        return primary;
    }

    /**
     * 触发概率出血与疼痛效果
     *
     * @param victim 受害者
     * @param part   用于出血判定的主部位
     */
    private static void applySecondaryEffects(Player victim, BodyPartType part) {
        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            if (cap.isDestroyed(part)) {
                return;
            }
            if (RANDOM.nextDouble() < Config.BODYPART_BLEED_CHANCE.get()) {
                cap.setBleedingTicks(part, Config.BODYPART_BLEED_DURATION.get());
            }
        });

        if (!victim.hasEffect(BodyPartEffects.PAINKILLER.get())) {
            int painDuration = Config.BODYPART_PAIN_DURATION.get();
            if (painDuration > 0) {
                victim.addEffect(new MobEffectInstance(BodyPartEffects.PAIN.get(), painDuration, 0, false, false, true));
            }
        }
    }

    /**
     * 检查头部或躯干是否归零，若是则直接击杀玩家
     *
     * @param victim 受害者
     */
    private static void checkFatal(Player victim) {
        BodyPartHelper.getBodyPartCapability(victim).ifPresent(cap -> {
            if (cap.isDestroyed(BodyPartType.HEAD) || cap.isDestroyed(BodyPartType.BODY)) {
                victim.setHealth(0.0f);
            }
        });
    }
}
