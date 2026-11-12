package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.effect.BleedingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects used by Exofall's weapons. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Exofall.MOD_ID);

    /** Serrated Longsword: 1 damage per second per stack (amplifier + 1 stacks, max 3). */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    /** Quadblade of the Sands: -10% movement speed. */
    public static final DeferredHolder<MobEffect, MobEffect> SANDBOUND = EFFECTS.register("sandbound",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xC8A060) {}
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, Exofall.id("effect.sandbound"), -0.10,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Bleed damage: ignores the post-hit immunity window (so it never eats the next sword hit), no knockback. */
    public static final ResourceKey<DamageType> BLEED_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Exofall.id("bleed"));

    private ModEffects() {}
}
