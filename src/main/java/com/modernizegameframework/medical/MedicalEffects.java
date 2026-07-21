package com.modernizegameframework.medical;

import com.modernizegameframework.bodypart.BodyPartEffects;
import com.modernizegameframework.bodypart.BodyPartHelper;
import com.modernizegameframework.bodypart.BodyPartType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 医疗物品具体效果实现
 */
public final class MedicalEffects {

    private MedicalEffects() {
    }

    /**
     * 绷带：停止一个非黑色出血部位，任意情况下都能使用且不会被伤害打断
     */
    public static final MedicalEffect BANDAGE = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return true;
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findBleedingNotDestroyedPart(player);
                if (target == null) return false;
                cap.setBleedingTicks(target, 0);
                return false; // 一次性效果，结束读条
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }

        @Override
        public boolean isAnytime() {
            return false;
        }

        @Override
        public boolean isUnbreakable() {
            return true;
        }
    };

    /**
     * 大绷带：停止一个非黑色出血部位，仅在出血时可用且不会被伤害打断
     */
    public static final MedicalEffect BIG_BANDAGE = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return true;
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findBleedingNotDestroyedPart(player);
                if (target != null) {
                    cap.setBleedingTicks(target, 0);
                }
                return false;
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }

        @Override
        public boolean isAnytime() {
            return false;
        }

        @Override
        public boolean isUnbreakable() {
            return true;
        }
    };

    /**
     * AI-2 急救包：循环随机非黑色部位每次回 1 血
     */
    public static final MedicalEffect AI2_MEDKIT = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return hasAnyDamagedNotDestroyed(player);
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findDamagedNotDestroyedPart(player);
                if (target == null) return false;
                cap.heal(target, 1.0f);
                return hasAnyDamagedNotDestroyed(player);
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return true;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }
    };

    /**
     * IFAK：循环随机非黑色部位每次回 1 血
     */
    public static final MedicalEffect IFAK = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return hasAnyDamagedNotDestroyed(player);
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findDamagedNotDestroyedPart(player);
                if (target == null) return false;
                cap.heal(target, 1.0f);
                return hasAnyDamagedNotDestroyed(player);
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return true;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }
    };

    /**
     * CMS 手术包：恢复一个黑色部位到 1 血
     */
    public static final MedicalEffect CMS_KIT = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return hasAnyDestroyedPart(player);
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findDestroyedPart(player);
                if (target == null) return false;
                cap.setHealth(target, 1.0f);
                return false;
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }
    };

    /**
     * 大手术包：循环恢复黑色部位到 1 血
     */
    public static final MedicalEffect BIG_SURGICAL_KIT = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return hasAnyDestroyedPart(player);
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
                BodyPartType target = findDestroyedPart(player);
                if (target == null) return false;
                cap.setHealth(target, 1.0f);
                return hasAnyDestroyedPart(player);
            }).orElse(false);
        }

        @Override
        public boolean isLoop() {
            return true;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }
    };

    /**
     * 止痛药：清除疼痛并添加止痛药效果，任意情况下都能使用
     */
    public static final MedicalEffect PAINKILLER = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return true;
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            player.removeEffect(BodyPartEffects.PAIN.get());
            int duration = com.modernizegameframework.Config.BODYPART_PAINKILLER_DURATION.get();
            if (duration > 0) {
                player.addEffect(new MobEffectInstance(BodyPartEffects.PAINKILLER.get(), duration, 0, false, false, true));
            }
            return false;
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }

        @Override
        public boolean isAnytime() {
            return true;
        }
    };

    /**
     * 大止痛药：清除疼痛 + 止痛药效果，任意情况下都能使用
     */
    public static final MedicalEffect BIG_PAINKILLER = new MedicalEffect() {
        @Override
        public boolean canApply(Player player, ItemStack stack) {
            return true;
        }

        @Override
        public boolean apply(Player player, ItemStack stack) {
            player.removeEffect(BodyPartEffects.PAIN.get());
            int duration = com.modernizegameframework.Config.BODYPART_PAINKILLER_DURATION.get();
            if (duration > 0) {
                player.addEffect(new MobEffectInstance(BodyPartEffects.PAINKILLER.get(), duration, 0, false, false, true));
            }
            return false;
        }

        @Override
        public boolean isLoop() {
            return false;
        }

        @Override
        public int durabilityCost() {
            return 1;
        }

        @Override
        public boolean isAnytime() {
            return true;
        }
    };

    // ========== 工具方法 ==========

    private static boolean hasAnyBleedingNotDestroyed(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            for (BodyPartType type : BodyPartType.values()) {
                if (!cap.isDestroyed(type) && cap.getBleedingTicks(type) > 0) return true;
            }
            return false;
        }).orElse(false);
    }

    private static boolean hasAnyDamagedNotDestroyed(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            for (BodyPartType type : BodyPartType.values()) {
                if (!cap.isDestroyed(type) && cap.getHealth(type) < cap.getMaxHealth(type)) return true;
            }
            return false;
        }).orElse(false);
    }

    private static boolean hasAnyDestroyedPart(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            for (BodyPartType type : BodyPartType.values()) {
                if (cap.isDestroyed(type)) return true;
            }
            return false;
        }).orElse(false);
    }

    private static BodyPartType findBleedingNotDestroyedPart(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            List<BodyPartType> bleeding = new ArrayList<>();
            for (BodyPartType type : BodyPartType.values()) {
                if (!cap.isDestroyed(type) && cap.getBleedingTicks(type) > 0) bleeding.add(type);
            }
            if (bleeding.isEmpty()) return null;
            // 优先选血量最低的出血部位
            bleeding.sort((a, b) -> Float.compare(cap.getHealth(a), cap.getHealth(b)));
            return bleeding.get(0);
        }).orElse(null);
    }

    private static BodyPartType findDamagedNotDestroyedPart(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            List<BodyPartType> damaged = new ArrayList<>();
            for (BodyPartType type : BodyPartType.values()) {
                if (!cap.isDestroyed(type) && cap.getHealth(type) < cap.getMaxHealth(type)) damaged.add(type);
            }
            if (damaged.isEmpty()) return null;
            return damaged.get(ThreadLocalRandom.current().nextInt(damaged.size()));
        }).orElse(null);
    }

    private static BodyPartType findDestroyedPart(Player player) {
        return BodyPartHelper.getBodyPartCapability(player).map(cap -> {
            for (BodyPartType type : BodyPartType.values()) {
                if (cap.isDestroyed(type)) return type;
            }
            return null;
        }).orElse(null);
    }
}
