package com.bromax.exofall.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Exofall's baubles (Curios). Passive effects run here in {@link #curioTick}; the ones that react to damage, food or
 * attacks are in {@link BaubleEvents}. Slots come from the item tags in data/curios/tags/item.
 */
public class BaubleItem extends Item implements ICurioItem {
    public enum Kind { DIVEBELL_LOCKET, EMBER_BEETLE_BROOCH, LEAFCUTTER_CHARM, STINGER_FANG_RING, NURSEMAIDS_WAX_CHARM, LANTERN_MOTH_PENDANT }

    /** Lantern Moth Pendant: hostiles this close are outlined while the wearer stands in the dark. */
    // Lantern Moth range and light level: config/ExofallConfig "gear.baubles"

    public final Kind kind;

    public BaubleItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    public String id() {
        return kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity e = ctx.entity();
        if (!(e.level() instanceof ServerLevel sl)) return;
        switch (kind) {
            case EMBER_BEETLE_BROOCH -> {
                if (e.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.SMALL_FLAME, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 1, 0.25, 0.3, 0.25, 0.002);
            }
            case NURSEMAIDS_WAX_CHARM -> {
                if (e.tickCount % 40 == 0 && e.getHealth() < e.getMaxHealth()
                        && e.getBlockStateOn().is(com.bromax.exofall.registry.ModTags.WAX_BLOCKS)) {
                    e.heal(1.0F);
                    sl.sendParticles(ParticleTypes.FALLING_HONEY, e.getX(), e.getY() + 1.2, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                }
            }
            case LANTERN_MOTH_PENDANT -> {
                if (e.tickCount % 10 == 0 && sl.getMaxLocalRawBrightness(e.blockPosition().above()) < com.bromax.exofall.config.ExofallConfig.MOTH_DARK.get()) {
                    double mothRange = com.bromax.exofall.config.ExofallConfig.MOTH_RANGE.get();
                    for (Mob m : sl.getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(mothRange),
                            m -> m instanceof Enemy && m.distanceToSqr(e) <= mothRange * mothRange)) {
                        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, false, false));
                    }
                }
            }
            default -> { }
        }
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext ctx, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        if (kind == Kind.DIVEBELL_LOCKET) {
            // vanilla underwater mining is x0.2; this brings it back to normal
            map.put(Attributes.SUBMERGED_MINING_SPEED, new AttributeModifier(id, 0.8, AttributeModifier.Operation.ADD_VALUE));
        }
        return map;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.exofall." + id() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
