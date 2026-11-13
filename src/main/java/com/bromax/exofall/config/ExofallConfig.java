package com.bromax.exofall.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config (config/exofall-common.toml). Defaults are tuned for Sovereign 202's late game; other packs can scale
 * them. Changes apply live (no restart) unless a comment says otherwise. Meteor spacing, loot and recipes are datapack
 * files (data/exofall/worldgen/structure_set/meteors.json, loot_table/, recipe/), not config.
 */
public final class ExofallConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------- gear
    static { BUILDER.comment("Exofall weapons and armour").push("gear"); }

    public static final ModConfigSpec.DoubleValue GEAR_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier on Exofall weapon damage. 1.0 = Sovereign 202 balance.")
            .defineInRange("damageMultiplier", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue GEAR_ARMOR_MULTIPLIER = BUILDER
            .comment("Multiplier on Exofall armour and toughness values (player armour and bug armour). 1.0 = Sovereign 202 balance.")
            .defineInRange("armorMultiplier", 1.0, 0.1, 10.0);

    static { BUILDER.comment("Legendary weapon perks").push("weapons"); }
    public static final ModConfigSpec.IntValue QUADBLADE_SLOW_TICKS = BUILDER
            .comment("Quadblade of the Sands: how long (ticks) a hit leaves the target Sandbound (slowed).")
            .defineInRange("quadbladeSandboundTicks", 60, 0, 1200);
    public static final ModConfigSpec.DoubleValue GRANDHAMMER_LIFESTEAL = BUILDER
            .comment("Royal Grandhammer: share of the damage dealt that heals the wielder.")
            .defineInRange("grandhammerLifesteal", 0.10, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue SCYTHE_LAUNCH_SPEED = BUILDER
            .comment("Glasswing Scythe: upward speed of the launch (0.54 = about 2 blocks). 0 = no launch.")
            .defineInRange("scytheLaunchSpeed", 0.54, 0.0, 3.0);
    public static final ModConfigSpec.IntValue SCYTHE_LAUNCH_COOLDOWN = BUILDER
            .comment("Glasswing Scythe: ticks between launches.")
            .defineInRange("scytheLaunchCooldownTicks", 80, 0, 1200);
    public static final ModConfigSpec.IntValue LONGSWORD_BLEED_STACKS = BUILDER
            .comment("Serrated Longsword: most Bleeding stacks one target can have.")
            .defineInRange("longswordMaxBleedStacks", 3, 1, 10);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Meteor armour set perks (per piece unless it says full set)").push("armour"); }
    public static final ModConfigSpec.DoubleValue MIRE_SIGHT = BUILDER
            .comment("Mireweave: each piece cuts how far mobs see you by this fraction.")
            .defineInRange("mireweaveSightPerPiece", 0.125, 0.0, 0.25);
    public static final ModConfigSpec.DoubleValue MIRE_AMBUSH = BUILDER
            .comment("Mireweave: each piece adds this much damage on mobs that aren't targeting you (0.075 = +7.5%).")
            .defineInRange("mireweaveAmbushPerPiece", 0.075, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue CARAPACE_BLOCK = BUILDER
            .comment("Reinforced Carapace: each piece blocks this fraction of projectile damage.")
            .defineInRange("carapaceProjectilePerPiece", 0.10, 0.0, 0.25);
    public static final ModConfigSpec.DoubleValue HONEY_SPEED = BUILDER
            .comment("Crystallized Honey, full set: extra movement speed (0.15 = +15%).")
            .defineInRange("honeyFullSetSpeed", 0.15, 0.0, 2.0);
    public static final ModConfigSpec.BooleanValue HONEY_REGEN = BUILDER
            .comment("Crystallized Honey, full set: constant Regeneration I.")
            .define("honeyFullSetRegeneration", true);
    public static final ModConfigSpec.DoubleValue ANCIENTSTONE_SLOW = BUILDER
            .comment("Ancientstone: each piece slows you by this fraction (0.025 = -2.5%).")
            .defineInRange("ancientstoneSlowPerPiece", 0.025, 0.0, 0.25);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Baubles").push("baubles"); }
    public static final ModConfigSpec.DoubleValue MOTH_RANGE = BUILDER
            .comment("Lantern Moth Pendant: hostile mobs within this many blocks get outlined.")
            .defineInRange("lanternMothRange", 8.0, 1.0, 32.0);
    public static final ModConfigSpec.IntValue MOTH_DARK = BUILDER
            .comment("Lantern Moth Pendant: works where the light level is below this.")
            .defineInRange("lanternMothBelowLight", 7, 1, 16);
    static { BUILDER.pop(); }

    static { BUILDER.pop(); }

    // ---------------------------------------------------------------- mobs and bosses
    static { BUILDER.comment("Meteor mobs (everything Exofall spawns except the four bosses)").push("mobs"); }
    public static final ModConfigSpec.DoubleValue MOB_HEALTH_MULTIPLIER = BUILDER
            .comment("Multiplier on meteor mob max health. Tamed pets don't get it (they use pets.tamedHealthMultiplier).")
            .defineInRange("healthMultiplier", 1.0, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue MOB_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier on all damage meteor mobs deal (melee, stings, spit, boulders...). Tamed pets don't get it.")
            .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
    static { BUILDER.pop(); }

    static { BUILDER.comment("The four meteor bosses").push("bosses"); }
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Multiplier on boss max health.")
            .defineInRange("healthMultiplier", 1.0, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier on all damage bosses deal. Minions they summon use mobs.damageMultiplier.")
            .defineInRange("damageMultiplier", 1.0, 0.1, 20.0);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Bug nests (the spawner blocks in the Warrens)").push("nests"); }
    public static final ModConfigSpec.IntValue NEST_RANGE = BUILDER
            .comment("A nest only spawns while a player is within this many blocks.")
            .defineInRange("activationRange", 16, 4, 64);
    public static final ModConfigSpec.IntValue NEST_CAP = BUILDER
            .comment("A nest stops spawning while this many meteor bugs are within 10 blocks of it.")
            .defineInRange("mobCap", 6, 1, 32);
    public static final ModConfigSpec.IntValue NEST_DELAY_MIN = BUILDER
            .comment("Shortest wait (ticks) between spawns.")
            .defineInRange("spawnDelayMinTicks", 160, 20, 72000);
    public static final ModConfigSpec.IntValue NEST_DELAY_MAX = BUILDER
            .comment("Longest wait (ticks) between spawns (raised to the minimum if lower).")
            .defineInRange("spawnDelayMaxTicks", 360, 20, 72000);
    static { BUILDER.pop(); }

    // ---------------------------------------------------------------- pets
    static { BUILDER.comment("Pets: the eight meteor bugs you can tame with treats").push("pets"); }

    public static final ModConfigSpec.IntValue PET_TAME_ODDS = BUILDER
            .comment("A treat tames a wild bug 1 time in this many. 1 = always.")
            .defineInRange("tameOdds", 3, 1, 100);
    public static final ModConfigSpec.IntValue PET_LOVE_TICKS = BUILDER
            .comment("How long (ticks) a fed tamed adult looks for a mate.")
            .defineInRange("loveTicks", 600, 20, 72000);
    public static final ModConfigSpec.IntValue PET_BREED_COOLDOWN = BUILDER
            .comment("Ticks before a pet can breed again. 6000 = 5 minutes.")
            .defineInRange("breedCooldownTicks", 6000, 0, 720000);
    public static final ModConfigSpec.IntValue PET_GROW_TICKS = BUILDER
            .comment("Ticks for a baby to grow up. 24000 = one Minecraft day. Each treat takes off a tenth.")
            .defineInRange("growTicks", 24000, 20, 720000);
    public static final ModConfigSpec.DoubleValue PET_HEALTH_MULTIPLIER = BUILDER
            .comment("Tamed bugs get this many times their wild max health.")
            .defineInRange("tamedHealthMultiplier", 1.5, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue PET_DAMAGE_MULTIPLIER = BUILDER
            .comment("Tamed fighters deal this many times their wild melee damage.")
            .defineInRange("tamedDamageMultiplier", 1.0, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue PET_FOLLOW_DISTANCE = BUILDER
            .comment("A following pet walks or flies back to you when it's further than this (blocks).")
            .defineInRange("followDistance", 6.0, 2.0, 32.0);
    public static final ModConfigSpec.DoubleValue PET_TELEPORT_DISTANCE = BUILDER
            .comment("A following pet teleports next to you when it's further than this (blocks). 0 = never teleport.")
            .defineInRange("teleportDistance", 24.0, 0.0, 128.0);
    public static final ModConfigSpec.BooleanValue PET_OWNER_FRIENDLY_FIRE = BUILDER
            .comment("Can owners hurt their own pets (and can pets hurt their owner's other pets)?")
            .define("ownerFriendlyFire", false);
    public static final ModConfigSpec.BooleanValue PET_DEATH_MESSAGE = BUILDER
            .comment("Tell the owner in chat when a pet dies.")
            .define("deathMessage", true);

    static { BUILDER.comment("Shieldback (fighter, tank)").push("shieldback"); }
    public static final ModConfigSpec.DoubleValue PET_TAUNT_RADIUS = BUILDER
            .comment("Monsters this close to a tamed Shieldback that are after its owner switch to the Shieldback. 0 = no taunt.")
            .defineInRange("tauntRadius", 8.0, 0.0, 32.0);
    public static final ModConfigSpec.IntValue PET_TAUNT_INTERVAL = BUILDER
            .comment("Ticks between taunts.")
            .defineInRange("tauntIntervalTicks", 60, 10, 1200);
    public static final ModConfigSpec.BooleanValue PET_TAUNT_BOSSES = BUILDER
            .comment("Can the taunt pull bosses (any mob in the c:bosses tag, and Exofall's own bosses)?")
            .define("tauntBosses", false);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Reedmantis and Bloommantis (fighters, ambush)").push("mantis"); }
    public static final ModConfigSpec.DoubleValue PET_AMBUSH_MULTIPLIER = BUILDER
            .comment("A tamed mantis's first hit on each new target deals this many times normal damage.")
            .defineInRange("ambushMultiplier", 2.0, 1.0, 10.0);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Nursemaid (helper, healer)").push("nursemaid"); }
    public static final ModConfigSpec.DoubleValue PET_HEAL_RANGE = BUILDER
            .comment("How far (blocks) a tamed Nursemaid looks for her owner and the owner's pets to heal.")
            .defineInRange("healRange", 16.0, 2.0, 64.0);
    public static final ModConfigSpec.IntValue PET_HEAL_INTERVAL = BUILDER
            .comment("Ticks between heals while she's next to a patient.")
            .defineInRange("healIntervalTicks", 40, 5, 1200);
    public static final ModConfigSpec.DoubleValue PET_HEAL_PLAYER = BUILDER
            .comment("Health per heal on a player (2 = one heart).")
            .defineInRange("healPlayer", 1.0, 0.0, 40.0);
    public static final ModConfigSpec.DoubleValue PET_HEAL_PET = BUILDER
            .comment("Health per heal on a pet.")
            .defineInRange("healPet", 4.0, 0.0, 40.0);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Mudgrub (helper, digs up swamp loot: loot table exofall:gameplay/mudgrub_dig)").push("mudgrub"); }
    public static final ModConfigSpec.IntValue PET_DIG_INTERVAL = BUILDER
            .comment("Average ticks between digs (each dig is 75% to 125% of this). 6000 = 5 minutes. 0 = never digs.")
            .defineInRange("digIntervalTicks", 6000, 0, 720000);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Golden Scarab (helper, finds ores; digs up loot table exofall:gameplay/scarab_dig)").push("golden_scarab"); }
    public static final ModConfigSpec.IntValue PET_SENSE_INTERVAL = BUILDER
            .comment("Ticks between ore searches. 0 = never searches.")
            .defineInRange("senseIntervalTicks", 600, 0, 72000);
    public static final ModConfigSpec.IntValue PET_SENSE_RADIUS = BUILDER
            .comment("Search radius in blocks (any block in the c:ores tag; gold ores win).")
            .defineInRange("senseRadius", 10, 2, 24);
    public static final ModConfigSpec.IntValue PET_NUGGET_INTERVAL = BUILDER
            .comment("Average ticks between digs (75% to 125% of this). 0 = never digs.")
            .defineInRange("digIntervalTicks", 6000, 0, 720000);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Bug armour perks").push("bug_armour"); }
    public static final ModConfigSpec.DoubleValue PET_MIREWEAVE_DETECTION = BUILDER
            .comment("Mireweave: monsters only notice the pet within this fraction of their follow range (unless it hits them).")
            .defineInRange("mireweaveDetection", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.IntValue PET_HONEY_REGEN_INTERVAL = BUILDER
            .comment("Crystallized Honey: the pet heals 1 health every this many ticks. 0 = off.")
            .defineInRange("honeyRegenIntervalTicks", 60, 0, 1200);
    public static final ModConfigSpec.DoubleValue PET_CARAPACE_PROJECTILE = BUILDER
            .comment("Reinforced Carapace: the pet takes this fraction of projectile damage.")
            .defineInRange("carapaceProjectileTaken", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue PET_ANCIENTSTONE_KNOCKBACK = BUILDER
            .comment("Ancientstone: extra knockback resistance on top of the armour's own 0.2.")
            .defineInRange("ancientstoneExtraKnockbackResistance", 0.3, 0.0, 1.0);
    static { BUILDER.pop(); }

    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ExofallConfig() {}
}
