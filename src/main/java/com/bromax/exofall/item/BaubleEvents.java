package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.registry.ModItems;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class BaubleEvents {
    private static final String SCARAB_KEY = "exofall_scarab_ward_ready_at";
    /** Players whose current swing is fully charged (set in AttackEntityEvent, read when the hit lands). */
    private static final Map<Player, Long> CHARGED = new WeakHashMap<>();

    public static boolean wearing(LivingEntity e, Item item) {
        return CuriosApi.getCuriosInventory(e).map(h -> h.isEquipped(item)).orElse(false);
    }

    /** Divebell Locket: breathe underwater. */
    @SubscribeEvent
    public static void breathe(LivingBreatheEvent event) {
        if (!event.canBreathe() && wearing(event.getEntity(), ModItems.DIVEBELL_LOCKET.get())) event.setCanBreathe(true);
    }

    /** Ember Beetle Brooch: fire damage halved. */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && wearing(event.getEntity(), ModItems.EMBER_BEETLE_BROOCH.get()))
            event.setAmount(event.getAmount() * 0.5F);
    }

    /** Golden Scarab Ward: a killing blow leaves you at 1 heart instead, once every 5 minutes. */
    @SubscribeEvent
    public static void lethal(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (event.getNewDamage() < p.getHealth() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        Item ward = ModItems.GOLDEN_SCARAB_WARD.get();
        if (!wearing(p, ward)) return;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(SCARAB_KEY)) return;
        p.getPersistentData().putLong(SCARAB_KEY, now + BaubleItem.SCARAB_COOLDOWN);
        p.getCooldowns().addCooldown(ward, BaubleItem.SCARAB_COOLDOWN);
        event.setNewDamage(Math.max(0F, p.getHealth() - 2F));
        ServerLevel sl = (ServerLevel) p.level();
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.6, 0.4, 0.3);
        sl.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.8F);
        p.displayClientMessage(Component.translatable("item.exofall.golden_scarab_ward.saved"), true);
    }

    /** Stinger Fang Ring: remember fully charged swings. */
    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide && p.getAttackStrengthScale(0.5F) >= 0.9F) CHARGED.put(p, p.level().getGameTime());
    }

    /** Stinger Fang Ring: a fully charged hit that lands poisons for 2 s. */
    @SubscribeEvent
    public static void landed(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player p) || event.getSource().getDirectEntity() != p) return;
        Long at = CHARGED.remove(p);
        if (at == null || at != p.level().getGameTime() || event.getNewDamage() <= 0) return;
        if (wearing(p, ModItems.STINGER_FANG_RING.get()))
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), p);
    }

    /** Nursemaid's Wax Charm: food restores 50% more hunger and saturation. */
    @SubscribeEvent
    public static void ate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        FoodProperties food = event.getItem().getFoodProperties(p);
        if (food == null || food.nutrition() <= 0 || !wearing(p, ModItems.NURSEMAIDS_WAX_CHARM.get())) return;
        int extra = Math.max(1, Math.round(food.nutrition() * 0.5F));
        float modifier = food.saturation() / (2F * food.nutrition());
        p.getFoodData().eat(extra, modifier);
    }

    private BaubleEvents() {}
}
