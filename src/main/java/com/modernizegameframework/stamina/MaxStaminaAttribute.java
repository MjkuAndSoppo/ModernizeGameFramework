package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 最大体力属性注册
 * 通过属性系统让装备、药水、效果等能够改变玩家的最大体力
 */
public class MaxStaminaAttribute {

    /**
     * 属性注册器
     */
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ModernizeGameFramework.MODID);

    /**
     * 最大体力属性
     * 默认值 50，最小 1，最大 1024
     */
    public static final RegistryObject<Attribute> MAX_STAMINA = ATTRIBUTES.register("max_stamina",
            () -> new RangedAttribute("attribute.modernizegameframework.max_stamina", 50.0, 1.0, 1024.0)
                    .setSyncable(true));

    private MaxStaminaAttribute() {}
}
