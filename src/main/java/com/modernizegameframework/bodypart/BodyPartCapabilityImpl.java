package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * 肢节血量能力的默认实现
 * 将玩家总血量按配置比例分配到各部位，并支持 NBT 持久化
 */
public class BodyPartCapabilityImpl implements BodyPartCapability {

    private final Player player;
    private final Map<BodyPartType, Float> health = new EnumMap<>(BodyPartType.class);
    private final Map<BodyPartType, Float> maxHealth = new EnumMap<>(BodyPartType.class);
    private final Map<BodyPartType, Integer> bleedingTicks = new EnumMap<>(BodyPartType.class);
    private boolean dirty = true;
    private boolean initialized = false;

    public BodyPartCapabilityImpl(Player player) {
        this.player = player;
        for (BodyPartType type : BodyPartType.values()) {
            health.put(type, 0.0f);
            maxHealth.put(type, 0.0f);
            bleedingTicks.put(type, 0);
        }
    }

    @Override
    public float getHealth(BodyPartType type) {
        return health.get(type);
    }

    @Override
    public void setHealth(BodyPartType type, float value) {
        float max = getMaxHealth(type);
        float newValue = Math.max(0.0f, Math.min(value, max));
        if (newValue != health.get(type)) {
            health.put(type, newValue);
            markDirty();
        }
    }

    @Override
    public float getMaxHealth(BodyPartType type) {
        return maxHealth.get(type);
    }

    @Override
    public void setMaxHealth(BodyPartType type, float value) {
        if (value != maxHealth.get(type)) {
            maxHealth.put(type, value);
            markDirty();
        }
    }

    @Override
    public boolean isDestroyed(BodyPartType type) {
        return getHealth(type) <= 0.0f && getMaxHealth(type) > 0.0f;
    }

    @Override
    public float getTotalHealth() {
        float total = 0.0f;
        for (float value : health.values()) {
            total += value;
        }
        return total;
    }

    @Override
    public float getTotalMaxHealth() {
        float total = 0.0f;
        for (float value : maxHealth.values()) {
            total += value;
        }
        return total;
    }

    @Override
    public void recalculateMaxHealth(float totalMaxHealth) {
        Map<BodyPartType, Float> ratios = new EnumMap<>(BodyPartType.class);

        // 从配置读取各部位比例，余数补给躯干
        ratios.put(BodyPartType.HEAD, (float) (totalMaxHealth * Config.BODYPART_HEAD_RATIO.get()));
        ratios.put(BodyPartType.BODY, (float) (totalMaxHealth * Config.BODYPART_BODY_RATIO.get()));
        ratios.put(BodyPartType.LEFT_ARM, (float) (totalMaxHealth * Config.BODYPART_LEFT_ARM_RATIO.get()));
        ratios.put(BodyPartType.RIGHT_ARM, (float) (totalMaxHealth * Config.BODYPART_RIGHT_ARM_RATIO.get()));
        ratios.put(BodyPartType.LEFT_LEG, (float) (totalMaxHealth * Config.BODYPART_LEFT_LEG_RATIO.get()));
        ratios.put(BodyPartType.RIGHT_LEG, (float) (totalMaxHealth * Config.BODYPART_RIGHT_LEG_RATIO.get()));

        float assigned = 0.0f;
        for (float value : ratios.values()) {
            assigned += value;
        }

        // 余数补给躯干
        float remainder = totalMaxHealth - assigned;
        ratios.put(BodyPartType.BODY, ratios.get(BodyPartType.BODY) + remainder);

        for (BodyPartType type : BodyPartType.values()) {
            float oldMax = getMaxHealth(type);
            float newMax = ratios.get(type);
            setMaxHealth(type, newMax);

            if (!initialized) {
                setHealth(type, newMax);
            } else if (oldMax > 0.0f) {
                float ratio = getHealth(type) / oldMax;
                setHealth(type, newMax * ratio);
            } else {
                setHealth(type, newMax);
            }
        }

        initialized = true;
    }

    @Override
    public void healAll() {
        for (BodyPartType type : BodyPartType.values()) {
            setHealth(type, getMaxHealth(type));
        }
    }

    @Override
    public void heal(BodyPartType type, float amount) {
        setHealth(type, getHealth(type) + amount);
    }

    @Override
    public void applyDamage(BodyPartType type, float amount) {
        setHealth(type, getHealth(type) - amount);
    }

    @Override
    public void setBleedingTicks(BodyPartType type, int ticks) {
        int clamped = Math.max(0, ticks);
        if (clamped != bleedingTicks.get(type)) {
            bleedingTicks.put(type, clamped);
            markDirty();
        }
    }

    @Override
    public int getBleedingTicks(BodyPartType type) {
        return bleedingTicks.get(type);
    }

    @Override
    public void tickBleeding() {
        int interval = Config.BODYPART_BLEED_INTERVAL.get();
        float damage = Config.BODYPART_BLEED_DAMAGE.get().floatValue();
        boolean changed = false;

        for (BodyPartType type : BodyPartType.values()) {
            int ticks = bleedingTicks.get(type);
            if (ticks > 0) {
                ticks--;
                if (interval > 0 && ticks % interval == 0) {
                    applyDamage(type, damage);
                }
                bleedingTicks.put(type, ticks);
                changed = true;
            }
        }

        if (changed) {
            markDirty();
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (BodyPartType type : BodyPartType.values()) {
            tag.putFloat(type.getId() + "_health", health.get(type));
            tag.putFloat(type.getId() + "_max", maxHealth.get(type));
            tag.putInt(type.getId() + "_bleed", bleedingTicks.get(type));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        for (BodyPartType type : BodyPartType.values()) {
            health.put(type, tag.getFloat(type.getId() + "_health"));
            maxHealth.put(type, tag.getFloat(type.getId() + "_max"));
            bleedingTicks.put(type, tag.getInt(type.getId() + "_bleed"));
        }
        initialized = true;
        markDirty();
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public boolean pollDirty() {
        boolean result = dirty;
        dirty = false;
        return result;
    }
}
