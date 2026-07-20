package com.modernizegameframework.bodypart;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 肢节血量系统自定义效果注册
 */
public class BodyPartEffects {

    /**
     * 效果注册器
     */
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ModernizeGameFramework.MODID);

    /**
     * 手无力效果
     * 臂黑时触发，降低攻击速度作为视觉和状态标记
     */
    public static final RegistryObject<MobEffect> HAND_WEAKNESS = EFFECTS.register("hand_weakness", () -> {
        MobEffect effect = new MobEffect(MobEffectCategory.HARMFUL, 0x8B4513) {};
        // 攻击速度降低，作为状态标记
        effect.addAttributeModifier(Attributes.ATTACK_SPEED, "c3d4e5f6-a7b8-9012-cdef-123456789012", -0.2D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        return effect;
    });

    /**
     * 疼痛效果
     * 受伤时触发，降低移动与攻击速度
     */
    public static final RegistryObject<MobEffect> PAIN = EFFECTS.register("pain", () -> {
        MobEffect effect = new MobEffect(MobEffectCategory.HARMFUL, 0x8B0000) {};
        // 移动速度降低 15%
        effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, "d4e5f6a7-b8c9-0123-defa-456789012345", -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 攻击速度降低 10%
        effect.addAttributeModifier(Attributes.ATTACK_SPEED, "e5f6a7b8-c9d0-1234-efab-567890123456", -0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        return effect;
    });

    /**
     * 止痛药效果
     * 屏蔽疼痛，不附加 debuff
     */
    public static final RegistryObject<MobEffect> PAINKILLER = EFFECTS.register("painkiller", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x00CED1) {});
}
