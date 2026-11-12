package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.registry.ModItems;
import com.bromax.exofall.registry.ModTags;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class BaubleEvents {
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

    /** Leafcutter Charm: cobwebs and berry bushes don't slow you (called from the Entity mixin, both sides). */
    public static boolean leafcutterPasses(LivingEntity e, BlockState state) {
        return state.is(ModTags.LEAFCUTTER_PASSABLE) && wearing(e, ModItems.LEAFCUTTER_CHARM.get());
    }

    /** Leafcutter Charm: berry bushes don't prick you. */
    @SubscribeEvent
    public static void thorns(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SWEET_BERRY_BUSH) && wearing(event.getEntity(), ModItems.LEAFCUTTER_CHARM.get()))
            event.setCanceled(true);
    }

    /** Leafcutter Charm: leaves, vines, webs and bushes break instantly, with any tool or none. */
    @SubscribeEvent
    public static void cut(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(ModTags.LEAFCUTTER_CUTTABLE) && wearing(event.getEntity(), ModItems.LEAFCUTTER_CHARM.get()))
            event.setNewSpeed(Math.max(event.getNewSpeed(), 1000F));
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
