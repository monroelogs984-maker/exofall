package com.bromax.exofall.item;

import com.bromax.exofall.entity.BoatWood;
import com.bromax.exofall.entity.ExofallBoat;
import com.bromax.exofall.entity.ExofallChestBoat;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Places an Exofall boat. Same rules as vanilla's BoatItem, which can't spawn modded boat entities. */
public class ExofallBoatItem extends Item {
    private final BoatWood wood;
    private final boolean chest;

    public ExofallBoatItem(BoatWood wood, boolean chest, Properties props) {
        super(props);
        this.wood = wood;
        this.chest = chest;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);

        Vec3 view = player.getViewVector(1.0F);
        List<Entity> near = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0),
                EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
        Vec3 eye = player.getEyePosition();
        for (Entity e : near) {
            AABB box = e.getBoundingBox().inflate(e.getPickRadius());
            if (box.contains(eye)) return InteractionResultHolder.pass(stack);
        }

        Vec3 at = hit.getLocation();
        Boat boat = chest ? new ExofallChestBoat(ModEntities.CHEST_BOAT.get(), level) : new ExofallBoat(ModEntities.BOAT.get(), level);
        boat.setPos(at.x, at.y, at.z);
        boat.xo = at.x; boat.yo = at.y; boat.zo = at.z;
        ((BoatWood.Holder) boat).setExofallWood(wood);
        boat.setYRot(player.getYRot());
        if (level instanceof ServerLevel server) EntityType.<Boat>createDefaultStackConfig(server, stack, player).accept(boat);
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
