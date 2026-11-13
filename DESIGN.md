# Exofall design notes

## Locked (2026-11-09)
- 1.21.1 NeoForge, MIT, modid `exofall`
- Start small: **one complete meteor type per version**, then add more
- Meteors are about **50% buried in terrain** (not water), with a crater ring
- Crust needs a new pickaxe (nether stars, echo shards, netherite ingots, ...). Crust must be blast-proof and sealed
- Gear starts around Netherite, tuned against Sovereign 202's late game (Astral Sorcery, Ars Nouveau, Gaia legendaries, Better Nether flaming ruby, Lycanites). Multipliers in config
- Legendary weapons are standalone items (not BMM/BHB)
- In S202: introduced late Act 3 or in Act 4
- Bio-themed fantasy, no tech or guns. Mostly self-contained in the meteors
- Erebus is inspiration only: no code, assets, Erebus-specific names or mob concepts

## Long-term scope (aspirational)
25-40 mobs, 3 stone types, 5-7 meteor biomes, 6 woods, 30+ plants, 10 sub-structures,
~1 meteor per 850 blocks, 9-12 legendary weapons, 3 general materials, legendary armour sets,
at least 1 boss per meteor type, dungeon-like design, 5x5 crafting table.

## MVP (0.1): one meteor
1 stone type, 1 wood, ~6 plants, 5-6 mobs + 1 boss, 2 sub-structures, crust-breaker pickaxe,
5x5 table, 2 legendaries, 1 armour set.

## Step 1: technical prototype (no art)
A plain stone sphere that spawns half-buried, with a placeholder interior biome (own fog + one test plant).
- Meteor = custom Structure type. Sample ground height over the footprint, sink to the median, carve the shell
  in chunk-local pieces (like ocean monuments / strongholds), then place NBT sub-structures inside.
- Interior biome = real biome. Structure starts are known before the BIOMES step, so a hook there returns the
  meteor biome for 4x4x4 cells inside the interior (3D biomes, like Lush Caves under surface biomes).
  Gives fog/sky/music/particles, natural spawns, F3 name, and floor/ceiling plant decoration via `environment_scan`.
- Risks to prove: (1) meteor biomes must be added to the dimension's possible-biomes list or their features
  are dropped/crash at decoration; (2) compatibility with S202 biome-injection mods hooking the same step.
- Fallback: structure `spawn_overrides` + client-side "inside a meteor" check for fog/sky/music.

## First meteor: swamp (locked 2026-11-09)
Buggy swamp meteor: dragonflies, mosquitoes and similar insects. Mob roster, plants and names not decided yet.

## Meteor kinds (locked 2026-11-09)
Four kinds to start, one structure each, all in structure set `exofall:meteors` (equal weight; spacing 34 / separation 16
since 2026-11-10, see below). Interior generators in `world/interior/`, picked by the `style` field.
1. **Swamp** (bugs: dragonflies, mosquitoes...): mud/moss floor, pools, lily pads, bog plants, verdant froglights
2. **Wasp hive**: parallel honeycomb sheets floor to ceiling, noise holes + regular doorways, honey floor patches, bee nests, shroomlights
3. **Desert beetle**: sand/red-sand dunes over sandstone with buried bone, banded sandstone pillars, cactus, dead bushes, ochre froglights
4. **Hemlock rainforest**: grass/podzol/moss, pools, ferns, tiered conifers (placeholder spruce, built block by block), glow-berry vines, pearlescent froglights
All currently spawn in any overworld biome. Spawns are vanilla stand-ins.

## Prototype result (2026-11-09)
Working in a dev server: `exofall:swamp_meteor` structure (radius 26-38, spacing 53 / separation 24 chunks),
centre at the median ground height, sealed placeholder crust, mud/moss floor with pools, lily pads, test plant,
froglight ceiling lights. Interior biome `exofall:swamp_meteor` is written into the chunk's 4x4x4 cells from
`MeteorPiece.postProcess` (like /fillbiome). The BIOMES-step hook turned out unnecessary: decoration only runs
features for biomes the biome source knows, so the piece decorates the interior itself, and the overwritten
biome cells keep biome-filtered overworld features out. Not done yet: crater ring, tool-tier gating, sub-structures.

## Crater + crust gating (2026-11-10)
- **Crater ring** (`MeteorPiece.craterColumn`): inside ~1.15 r the ground is levelled to a bowl floor 12% of r below the
  centre (uphill terrain cut away, downhill filled in), fading out by 1.45 r; a raised rim (12% of r high) peaks at 1.45 r
  and ends at 1.85 r. Ring radius is noise-warped ±10%. Ejecta (coarse dirt, gravel, tuff, a little cobbled deepslate)
  covers ~75% of the bowl and thins out to nothing at the crater edge. Columns under water are skipped.
- **Water check** now samples 17 points (centre, 8 at 0.8 r, 8 at 1.5 r) and allows at most one wet one. The old
  centre-only check let meteors land half in lakes. This rejects about two thirds of attempts, so spacing went from
  53/24 to 34/16. Measured on seed 20261109 (25 random points): median distance to the nearest meteor ~550 blocks.
- **Crust gating**: `exofall:needs_meteoric_tool` (crust) is in every vanilla `incorrect_for_*_tool` tag, netherite
  included. Tier `ModTiers.METEORIC`: 3000 uses, speed 10, +4 damage, enchant 15, repaired with echo shards (placeholder
  numbers). Crust hardness 60 (obsidian 50), still blast-proof.
- **Crustbreaker** pickaxe (2026-11-12, real: id `crustbreaker`, EPIC, 32x32 sprite from
  textures-src/item/gen_crustbreaker.py: pearl Shellrock head with hammer face + spike, echo-shard collar). Vanilla
  crafting table (you need it to get into a meteor): echo shard / nether star / echo shard, netherite ingot /
  netherite pickaxe / netherite ingot, breeze rod under.
- Caveat: modded pickaxes whose tier uses its own empty `incorrect_for` tag can still mine crust. Check S202's
  above-netherite tools when the pack integration happens.

## Size (locked 2026-11-10, option 2)
Radius 55-75 (hive 57-77, rainforest 60-78), round (vertical = horizontal radius), about 10x the old interior volume:
room for stacked dungeon levels, sub-structures, a boss room and open spawning areas. Hard limit: a piece can't reach
more than ~135 blocks from its centre (chunks only reference starts 8 chunks away), so `MAX_REACH` = 132 squeezes the
crater on the biggest meteors. Separation raised to 18 so craters can't overlap.

## Interior content (locked 2026-11-10; names locked as written)
Shared rules: no Overworld mobs spawn inside; light from glowing plants/stone, not the sun; alien plants only grow on
meteor soil (can be carried out, won't spread); mobs hunt each other (food chains); every ambush mob has a visible or
audible tell.

**Stones (shared):** Shellrock (crust, Crustbreaker only, dark fused glass with oil-slick sheen), Chitinstone (main interior
stone, layered fossil-shell plates, polished/brick/pillar variants for dungeons, tinted per meteor), Glowmarl (soft
glowing soil-stone the alien plants grow on).

**Woods:** Swamp = Stiltreed (segmented reed-trees on arching stilt roots, frond caps); Hive = Combwood (wax-coated,
hollow larva-nest cores, amber planks); Desert = Ribwood (pale leafless spiralling ribcage trunks); Rainforest = Veilpine
(blue-grey conifer with glowing lichen curtains; replaces placeholder spruce).

**Plants:** Swamp: Lanternpod (glowing hanging bulb, drops lantern jelly), Bladderpad (lily pad, pops into slowing gas),
Snaretongue (pitcher plant, eats small bugs, nips players), Sapcane (tall reed, sweet sap = food + mosquito bait),
Driftmoss (spreading water carpet), Weepvine (climbable ceiling vine). Hive: nectar bells, waxcap fungus, royal amber
crystals. Desert: dunespine (hollow cactus, beetle nests), nightbloom, rattlegrass (rattles when walked through, alerts
beetles). Rainforest: umbrella fern, mistcap mushroom, glowmoss curtains.

**Mobs:**
- Swamp: Glasswing (dragonfly, hover then dive), Bloodgnat (mosquito swarm 3-5, drains + Weakness, smoke scatters),
  Divebell Beetle (neutral diver, drops bubble sacs), Reedmantis (ambush in Sapcane, reeds rustle first), Mudgrub
  (passive larva, prey, farmable). **Boss: Terror of the Bog** (giant water scorpion).
- Hive: Combwasp workers, Stinger soldiers (slow visible wind-up), Nursemaids (heal others), Waxmites (scavengers),
  larva cocoons that hatch if left. **Boss: Waxen Queen.**
- Desert: Sanddivers (burrow ambush, sand-puff tell), Shieldbacks (armoured front), Ember beetles (light, prey), Rollers
  (push boulders down corridors). **Boss: Dozen-legged Wrath** (red centipede), summoned via a sigil dropped by a
  golden-encrusted scarab.
- Rainforest: Twigwalkers (stick insects), Bloommantis (flower mimic), Leafcutter columns (neutral unless blocked),
  Lantern moths (drawn to the player's light). **Boss: Apex Mantis** (large green mantis with blade-arms).

**Boss rooms** are sealed and opened with a sigil in a sigil socket. Sigils come from the dungeons; exactly how is not
decided yet.

## Interior blocks built (2026-11-10, placeholder textures)
80 blocks, all in the Exofall creative tab. Resources come from `tools/gen_placeholder_assets.py`, which clones vanilla
look-alikes (mangrove->Stiltreed, acacia->Combwood, birch->Ribwood, warped->Veilpine, tuff->Chitinstone, obsidian->Shellrock,
calcite->Glowmarl, amethyst cluster->Royal Amber) and keeps vanilla textures. Re-run it after adding blocks; it owns
the files in `tools/.generated`. Real textures replace the vanilla texture refs later.
- Shell: 3 layers Shellrock outside, 2 layers Chitinstone lining. Everything over 3 blocks under a floor is Chitinstone.
- Woods: log, wood, stripped log/wood (axe-strippable), planks, stairs, slab, fence, gate, door, trapdoor, button,
  pressure plate; leaves for Stiltreed and Veilpine. No saplings, signs or boats yet (trees are built by the
  generator, not grown).
- Plants only survive on `#exofall:alien_soil` (Glowmarl). Pads sit on water. Behaviours: Snaretongue/Dunespine slow
  and nip like berry bushes; Bladderpad pops into a slowness cloud; Rattlegrass rattles + emits a vibration (silent
  when sneaking); Weepvine and Glowmoss Curtain are climbable; glowing plants: Lanternpod 12, Royal Amber 9,
  Glowmoss 7, Glowmarl 6, Nightbloom 5, Nectar Bell 4, Mistcap 3.
- Generation: swamp = Glowmarl banks + Stiltreed on stilt roots, Driftmoss/Bladderpads on pools, Lanternpods and
  Weepvines from the ceiling; hive = Glowmarl beds with Nectar Bells/Waxcaps, Royal Amber, 3x3 hollow Combwood
  trunks floor to ceiling; desert = Glowmarl flats in dune troughs with Rattlegrass/Dunespine/Nightbloom and twin-helix
  Ribwood, Chitinstone pillars; rainforest = Glowmarl floor with Umbrella Ferns/Mistcaps, Veilpines, Glowmoss
  Curtains. All vanilla plants removed from the interiors.

## Textures, saplings, boats (2026-11-10)
- All textures are Exofall's own (no vanilla placeholders left). Sources in `textures-src/` (gen_stones, gen_woods,
  gen_plants, entity/gen_boats, item/gen_boat_items + hand .px); rebuild with `python3 textures-src/build_textures.py`,
  then `python3 tools/gen_placeholder_assets.py` so models pick up new textures. Palettes: `textures-src/texlib.py`.
- Colours: Shellrock iridescent white (animated pearl shimmer), Chitinstone brown, Glowmarl mint (Glenn's picks);
  woods/plants picked from the design text (Stiltreed olive reed, Combwood amber + hex comb, Ribwood bone, Veilpine
  slate blue + cyan lichen).
- Saplings for all 4 woods (alien soil only, bone meal or random ticks, grow the same shapes as world gen).
  Sources: Stiltreed/Veilpine leaves 5%, Nectar Bell 8% -> Combwood, Rattlegrass 8% -> Ribwood (they have no leaves).
  `/exofall grow <pos>` (ops) grows one instantly for testing.
- Boats + chest boats for all 4 woods: own entities (`exofall:boat`, `exofall:chest_boat`, wood stored by name),
  own renderer; skins use only vanilla's boat UV mask, painted with our planks. No dispenser behaviour yet.

## Open questions
- Locator item for finding a specific meteor type (1 per 850 blocks split across 5-7 types is a long hunt)
- Ways past the crust to block: TNT, creepers, ender pearls, chorus fruit, mods in S202

## First mobs: swamp lesser mobs (2026-11-11)
GeckoLib 4.9.3 (S202 ships it; jar in git-ignored `libs/`, required dependency in mods.toml). Models, textures and
animations come from `tools/mobs/gen_mobs.py` (builder `mobkit.py` and `preview.py` adapted from Pestilence's tools);
rerun it after edits, preview with `python3 tools/mobs/preview.py <geo> <png> <out.png>`. Textures are first-pass
generated, not /pixelart-polished. 2026-11-11 later: rebuilt at 2x size with more 3D detail (Glenn's call, Erebus
art as a detail-level reference only): legs/antennae/abdomens are bone chains (`chain_x`/`chain_z`; +rz bends a +X limb
down, +rx bends a +Z segment down). Hitboxes: Mudgrub 1.0x0.9, Glasswing 1.6x1.2, Bloodgnat 0.9x1.1. Sounds are pitched vanilla stand-ins. All stats are placeholders, not tuned yet.
- **Mudgrub** (creature, 8 HP): slow pale larva, panics, bred and tempted with Sapcane, babies half size. Prey for both
  flyers. Spawns 2-4 on Glowmarl/mud/moss at any light. No drop yet (what it's farmed for is undecided).
- **Glasswing** (monster, 20 HP, 7 dmg, 2 armour): hovers ~6 out and ~4 up from its target, winds up for 1 s
  (high-pitched phantom swoop + abdomen curl = the tell), then dives in a straight line at where the target was.
  Sidestep to dodge; hitting it during the wind-up breaks it off. Climbs away 1.5 s, repeats (~5 s per dive). Also
  hunts Bloodgnats and Mudgrubs. Spawns 1-2.
- **Bloodgnat** (monster, 4 HP, 2 dmg): swarms of 3-5 with a constant high whine. Each bite heals it 2 and gives
  Weakness I (5 s). After 3 bites it is sated and leaves for 30 s, so a swarm wears out. A lit campfire up to 10 below
  / 3 to the side scatters it sideways for 5 s. Also drinks from Mudgrubs.
- Swamp meteor spawners now hold only these three (spider/cave spider/slime/frog/bat stand-ins removed). The other three
  meteors still use vanilla stand-ins.
- Verified on the dev server (no player): Glasswing dive loop kills Mudgrubs, swarm drains + sates, smoke scatters.
  Not verified yet: against a player, natural spawning inside a real meteor, client rendering/animations.
- Remaining swamp mobs: Divebell Beetle, Reedmantis; boss Terror of the Bog.

## All lesser mobs (2026-11-11, Glenn: "do the rest except for the bosses with these style models")
16 more GeckoLib mobs in the 2x detailed style, 4 per meteor, generated by `tools/mobs/gen_mobs.py` (body plans in
`mobs_templates.py`: mantis + wasp; one module per meteor; legged models auto-grounded). Java: `entity/mob/<meteor>/`,
shared bases `Bug` (main + action controllers), `FlyingBug`, `AmbushMantis`, `HiveMob` (alarm), `Hideable`.
Stats, sounds (vanilla stand-ins) and textures are placeholders. Each meteor now spawns only its own mobs.
- **Swamp:** Divebell Beetle (neutral swimmer, alerts others, drops Bubble Sac = placeholder 30 s Water Breathing),
  Reedmantis (hides in Sapcane; tell = reeds rustle + shed bits, 1 s rock with arms raised; eats Mudgrubs).
- **Hive:** Combwasp (swarm, short poison), Stinger (1.2 s wind-up then 12 + Poison II; step back to dodge),
  Nursemaid (never attacks, heals hurt hive mobs +3/0.5 s, flees players, glowing wax drop), Waxmite (eats dropped
  FOOD only, attacks players under half health), Larva Cocoon (ripens 2 min while a player is within 24, hatches a
  Combwasp or 1-in-5 Stinger unless 8+ fighters nearby). Hurting any hive fighter alarms all within 16.
- **Desert:** Sanddiver (buried in sand, 0.8 s sand-puff tell, bursts out and bites; reburrows after 5 calm s),
  Shieldback (front 120 deg takes 20% damage + clang, turns slowly), Ember Beetle (glowing prey), Roller (keeps
  5-16 blocks away, 1 s rear-up then rolls a boulder 10 dmg, 8 s to regrow), Gold-Encrusted Scarab (placeholder
  name; rare, armour 10, flees; meant to drop the Dozen-legged Wrath sigil, drops gold nuggets for now).
- **Rainforest:** Twigwalker (neutral, freezes when a player is within 8, kicks), Bloommantis (flower mimic; tell =
  petals shiver loose + chime; eats Lantern Moths), Leafcutter (columns carrying leaves; block one for 2 s and the
  column attacks, chitter at 1 s), Lantern Moth (harmless, circles players holding a light, glow dust).
- Dev-server verified (no player): every mob spawns; Sanddiver ambush; Shieldback front 1.4 vs back 8.8 from a 10 hit;
  Roller boulder hits for ~10; hive alarm; Nursemaid heals; Waxmite eats bread; Reedmantis hide -> tell -> kill;
  Leafcutters march; Divebell doesn't drown. Not verified (need a player or a client): Cocoon hatching, Twigwalker
  freeze, Lantern Moth light-seeking, Scarab fleeing, Stinger wind-up timing, every model/animation/glow in game.
- Gotchas found: goal canUse()/tick() is polled every other tick, so `tickCount % N` there never fires for odd
  entity ids (use countdowns); a plain Entity collides with pushable mobs like a boat (boulder shattered on its own
  Roller; override canCollideWith); NeoForge makes canBreatheUnderwater final (override canDrownInFluidType).

## Warrens (dungeons, 2026-11-12)
world/dungeon: Dungeon (layout from the meteor seed), DungeonGen (blocks, priority rule), DungeonCheck
(`/exofall dungeon check`: flood-fill walk test, chests/nests reachable, shack doors lead out, no water/gaps; writes
top-down maps to the run folder). Level 0 at baseFloor-12, level 1 at baseFloor-24 with one vault; spiral stair
shafts; 3 entrance shacks over level-0 rooms, 4-6 nest shacks. Nest blocks (Mire Nest, Brood Comb, Sand Burrow,
Rotwood Hollow) spawn the meteor's bugs (block/NestBlockEntity). Loot: loot_table/chests/warren_*. All 4 meteor types
pass the check on seed 20261109.

## Dungeon-era mobs (2026-11-12, Glenn: "Add all those, they passed"; 2 more per meteor for dungeon variety)
Same 2x GeckoLib style (`tools/mobs`; shared `spider_legs`/`chevrons` + wasp `kind='spitter'` in mobs_templates.py).
Stats/sounds are placeholders. Each has a readable tell. `/exofall mobtest` (entity/mob/MobSelfTest) runs every
mechanic on a pad 40 blocks up with a damageable fake player (added to the level so mobs can find it).
- **Bogspinner** (swamp): fishing spider. Idle: strings `exofall:bogsilk_web` across 1-block gaps (max 3 within 6).
  Fight: 4-14 blocks, 0.75 s rear-up tell, spits a silk `SpitGlob` (cobweb look) -> 2 dmg + a web. Bites webbed
  targets x1.5. Webs (BogsilkWebBlock) vanish after 30 s; Leafcutter Charm passes/cuts them.
- **Mire Leech** (swamp, spawns in water): hunts only things in water; tell = FISHING ripples on the surface above
  it. A landed bite latches (follows the host's legs, synced host id), drains 2 hp / 2 s and heals itself; lets go
  when the host is out of water 0.5 s, when hit (knocked off), or after 30 s.
- **Honeyguard** (hive): slow shield wasp, 50 hp, armour 10, KB res 1, attack knockback 2.5. Front hits do 15% and
  wear a 24-point shield by the damage (x2 for hits >= 12): Grandhammer breaks it in one. Reseals after 30 s.
  Renderer hides the `shield` bone when broken.
- **Propolis Spitter** (hive, flying): holds 6-12 blocks out, 1 s belly-pump tell (glowmask belly + honey drips),
  propolis `SpitGlob` -> 4 dmg + Slowness II 3 s + `exofall:propolis` puddles (honey-like speed 0.35, 10 s).
- **Antlion** (desert): on first settling on sand digs a funnel (r 3, 2 deep) and sits buried (misc.buried anim).
  Tell = visible pit + falling-sand particles. Drags everything within 4.5 toward it (players on their own client,
  mobs on the server), bites 8 + Slowness IV 1 s in reach. Off sand it's a plain slow melee bug.
- **Tomb Scorpion** (desert): pincer 5 + Slowness I 1 s; within 3.4 a 0.75 s raised-sting tell (glowing turquoise
  sting, rattle) then a 3.8-reach sting: 8 + Sandbound 3 s. Black lacquer, gold trim, linen-wrapped tail.
- **Canopy Assassin** (rainforest): hangs under ceilings/leaves (3-6 up), rendered flipped (AssassinRenderer negates
  yaw under the Z flip). Tell = silk thread + drips while a player is within 10. Drops 0.5 s after a player passes
  within 2.5 sideways: 8 + a Bleeding stack. Climbs back after 10 calm s. Shooting it down knocks it off.
- **Pouncer** (rainforest): jumping spider. From 3-7 blocks: 0.75 s crouch (front legs raised, eye-glint sparks),
  then a ballistic leap aimed at where you stood -> 7 dmg on contact. 3 s between leaps, bites 4.
- Loot: Bogspinner string, Honeyguard/Spitter honeycomb, Tomb Scorpion gold nuggets. Biome spawn weights 15-30.
- Verified (dev server, mobtest): every mechanic above; dev client loads all 8 models/animations with no errors.
  Not verified: how they look/animate in game, natural spawning, the Assassin flip and Leech ride on a real client.
- Test gotcha: ServerLevel increments tickCount, not Entity.tick(); hand-ticked mobs need `tickCount++` or their
  goals (run on alternate ticks) never select.

## Pets (locked 2026-11-11, not built yet)
Glenn: "very trimmed down". One pet per meteor. Each pet is ridden, flown or neither, depending on the pet. Some pets
have a gear-making job (feed them X, after a while they drop Y). They come BEFORE each meteor's boss, but you get one by
capturing and breeding that meteor's existing bugs (not by taming a rare wild spawn). Which bugs, the capture item and
each pet's stats are still open. Keep them below Ice and Fire dragon power but still strong.

**Step 1 built 2026-11-12 (Glenn: "only the 8 parents breed")** — entity/mob/pet (PetPair, Pets, PetState):
pairs Swamp = Glasswing x Mudgrub, Hive = Combwasp x Nursemaid, Desert = Golden Scarab x Shieldback (Shieldback my
pick, Glenn didn't name the beetle), Rainforest = Bloommantis x Reedmantis. One treat per pair (vanilla table, 4 per
craft): Sapcane Candy (sapcane + sugar), Nectar Drop (nectar bell + honeycomb), Dune Pellet (dunespine + bone meal),
Mistcap Morsel (mistcap + umbrella fern); names are working names. Treat on a wild parent: 1 in 3 tames it (passive to
players and other tamed pets, never despawns, scarab/nursemaid stop fleeing). Treat on two tamed adults: tamed baby,
half size, grows up in 24000 ticks (treats speed it up), 5 min cooldown. Mudgrub keeps vanilla breeding (sapcane or
candy). Tamed/bred Golden Scarabs drop nothing (no Desert Sigil farm). Cross-breeding (PetPair.CROSS_TREATS) waits for
the hybrids. `/exofall pettest` passes (runs at world spawn: entity lookups need entity-ticking chunks).

**Step 2 built 2026-11-13 (Glenn: hybrids SCRAPPED, own baubles scrapped for v1; picked my recommended options):**
pet/PetGoals adds the AI on join (wild-only goals are gated off once tamed, so the mob classes barely change), pet/PetEvents
the event rules. Owner = whoever tamed/bred it (older ownerless pets: first clicker claims). Empty hand cycles
Follow/Stay/Wander; Follow teleports past 24 blocks. Tamed = x2 health, fighters x1.5 damage (config). Fighters
(Glasswing, Combwasp, Shieldback, Reedmantis, Bloommantis) take the owner's attacker and target; Shieldback taunts
monsters off its owner every 3 s (not bosses); mantis first hit on each new target x2. Helpers: Nursemaid heals owner
1 / pets 2 every 2 s; Mudgrub digs `gameplay/mudgrub_dig` from soft ground every ~5 min (this is its farm product);
scarab senses c:ores (gold first) every 30 s within 10 blocks (action-bar text + glint trail) and digs
`gameplay/scarab_dig` gold. No friendly fire, golems ignore pets, owner gets a death message. Bug armour
(item/BugArmorItem, body slot like wolf armour, never wears out, 5x5 recipe P+S+K, no boss drop): Mireweave = noticed
at half range, Honey = 1 hp / 3 s, Carapace = 60% projectile damage, Ancientstone = +0.3 knockback resistance. Drawn by
client/PetArmourLayer as inflated shells over the body bones with a 32x32 plate per set (tools/armor/gen_armor.py also
makes the icons). Every number is in the config `pets` section. `/exofall pettest` covers all of it.

## Config (2026-11-13)
exofall-common.toml: gear (+ weapons/armour/baubles perk values), mobs and bosses (health via a permanent modifier
refreshed on load, damage via LivingIncomingDamageEvent on any hit sourced from an Exofall mob; tamed pets skipped),
nests, pets. exofall-client.toml: boss cinematic, pet armour drawing. Meteor spacing/loot/recipes stay datapack.

## Release 1.0.0 (2026-11-12)
Config gear multipliers now applied (config/GearScaling, ItemAttributeModifierEvent; tested x2 armour). CHANGELOG.md, mod logo, 17 advancements. CF project not created yet (Glenn).

## Field Guide (2026-11-12, Patchouli, optional)
`tools/gen_guide.py` writes the whole book (11 categories, 55 entries, 182 pages: basics, meteors, every creature
with an entity render, Warrens, bosses + sigils, gear with real 5x5 recipe pages, pets). 5x5 pages = template
`exofall:shellrock` + compat/patchouli/ShellrockProcessor. Crafted from book + flint (recipe only loads with
Patchouli). Checked in a dev client: every spread screenshotted, no text overflow. Gotcha: /open-patchouli-book needs
the full entry id (exofall:basics/welcome); without it Patchouli 93 disconnects the client.

## Bosses, arenas, sigils (2026-11-11; Glenn: "detailed and have a short cinematic after you encounter them. Worry about the arenas too and how to unlock them; then make them spawn in the middle of each meteor (we can later build AROUND them)")
- **Arena** (`world/ArenaLayout`, `world/ArenaGen`): sealed dome at every meteor's centre, inner 15 x 12 tall, on a
  paved plaza (radius 24, cleared 16 up) that blends back into the ground by 32; trees/pillars are kept out to 38.
  One door (3x4, direction from the seed) shut by an unbreakable **Sigil Seal**, with the **Sigil Socket** in the
  right-hand post. Themed inside: swamp = mud + 2-deep pools, driftmoss, lanternpods, verdant froglights; hive =
  honeycomb floor + honey patches, 6 comb pillars, royal amber, shroomlights; desert = 2-deep sand (the Wrath
  burrows), polished ring path, 4 bone pillars; rainforest = moss, grass/ferns, 5 Veilpine trees, glowmoss curtains.
  Old meteors in already-generated chunks have no arena.
- **Unlocking (my proposal, not locked):** each meteor has a sigil. Desert Sigil drops from the Gold-Encrusted
  Scarab (Glenn's design). The others are crafted from 4 fragments (shapeless); every Glasswing / Stinger /
  Bloommantis killed by a player drops one. Meant to move into dungeon loot once dungeons exist.
- **Fight flow** (`entity/boss/ExofallBoss`): sigil on socket -> seal opens, boss lies dormant (invulnerable) in the
  centre -> a player walks 2+ blocks inside -> seal closes behind them, 5 s intro cinematic for players who
  haven't seen this boss's (camera swings in on the boss while it roars, letterbox, name + subtitle; player frozen
  and invulnerable; client `client/BossCinematic`, packet `network/CinematicPayload`) -> fight with a boss bar.
  Nobody left for 10 s -> boss heals, goes home, dormant, seal reopens. Death -> seal opens; another sigil wakes a
  new boss. 500 XP, no loot yet. Debuffs on bosses are capped at 5 s / level II.
- **Bosses** (stats placeholder; every attack has a wind-up tell; enraged = cooldowns x0.65, damage x1.15):
  - Terror of the Bog (420 HP): snap, grab (held 2.5 s, 30 damage frees you and stuns it), submerge then burst up
    under a bubble boil, mud spit (3 when enraged), enraged at 50% calls Bloodgnats.
  - Waxen Queen (380 HP, flies): sting dive then 1.5 s dazed on the floor, wax spray fan, lays nearly-ripe Larva
    Cocoons, wing buffet if you stand under her, enraged calls 2 Stingers; hive mobs defend her, Nursemaids heal her.
  - Dozen-legged Wrath (450 HP, 12 legs, ~11 blocks long): bite, charge (stunned if it hits a wall; double when
    enraged), burrow + sand-puff tell, tail sweep if you're beside/behind; enraged at 40% = faster. Body segments
    follow the head's path (yaw history); 7 hittable PartEntity segments.
  - Apex Mantis (400 HP): 3-hit blade combo, leap, guard after 3 quick hits (front melee parried + riposte),
    enraged camouflage (rustle/chime tells, loud chime 0.5 s before the strike; hitting it reveals it).
- Models in `tools/mobs/mobs_bosses.py`. Test commands: `/exofall arena socket` (inside a meteor),
  `/exofall arena wake <socket>`, `/exofall boss fight`.
- Dev-server verified (no player, iron golem as the opponent): all four arenas generate with socket + seal +
  theme; wake opens the seal; all four bosses fight (Queen dives/lays cocoons, Wrath/Mantis kill the golem) and
  reset to dormant after 10 s. **Not verified (needs a client):** the cinematic, intro trigger, boss bars, every
  model/animation in game, Wrath body-follow direction (sign of the segment turn is reasoned, not seen; flip it in
  `MobRenderers.WrathModel` if the body bends the wrong way), Queen flight feel, grab teleporting a real player.

## Warded arena blocks (2026-11-11; Glenn: "make unbreakable versions of all the blocks in and encasing each of the boss arenas so they arent easily cheesable")
- 20 `warded_*` twins (`ModBlocks.WARDED`, assets from `tools/gen_warded.py`): same look/behaviour as the source (honey
  sticks, mud sinks), unbreakable (-1 hardness, 3.6M blast), no loot, piston-proof, wither/dragon immune, no mob spawns.
- `ArenaGen.apply` wards everything in the porch, the 5-deep floor and the dome (walls, lamps, pillars, Veilpine trees).
  Plants, water and the plaza stay normal (the plaza is where Glenn builds around the arena). Verified: TNT leaves the
  dome intact, craters the plaza.

## Gear (2026-11-11, structure locked by Glenn; specifics open)
- Per meteor: **1 gear tier, 2 unique baubles (5x5 table), 2-3 unique boss gear pieces (5x5 table, from boss drops)**.
- **Shellrock Crafting Table** (`block/ShellrockTableBlock`, `crafting/ShellrockTableMenu`, `crafting/ShellrockRecipe`):
  recipe = shellrock in the 4 corners + crafting table in the middle (3x3). 5x5 grid; crafts `exofall:shellrock_crafting`
  recipes (shaped, up to 5x5, mirrorable, JSON `pattern`/`key`/`result`) and every vanilla crafting recipe.
  Verified with a fake-player test: vanilla 2x2 off-centre, a 5x5 + its mirror, wrong ingredient rejected, cake
  returns buckets. No 5x5 recipes exist yet. No JEI category yet.
- **Boss drops** (always, fixed counts): Terror of the Bog = Lethal Proboscis + 2 Bog Eye; Waxen Queen = 4 Royal Honey
  Drop + 2 Crystallized Wing; Dozen-legged Wrath = 16 Terror Leg + Venomjaw; Apex Mantis = Mantis Eye + Mantis
  Greatclaw (sword, 8 damage, 1.3 speed, deliberately weak: base for a greater weapon). "Bog Eye" is my name.

## Armour sets (2026-11-12; perks locked by Glenn, stat numbers my proposal)
Each piece = tier material + 1 dungeon loot item, crafted in the Shellrock Crafting Table (loot item = stand-in until
dungeons exist). Armour may or may not end up separate from the weapons. Meteors are NOT done in order (Glenn,
2026-11-12): no progression ladder; the sets differ in role, not in unlock order.
- **Mireweave** (swamp): mobs notice you at half range (bosses ignore it; check the Lycanites port's targeting),
  ambush damage bonus on mobs that haven't noticed you. Diamond-to-Netherite protection, Netherite durability.
  Proposed 20 armour / 2.5 toughness / 1x Netherite durability.
- **Crystallized honey** (hive, name open): diamond protection, Netherite durability. FULL SET only: +15% speed
  (attribute) + permanent Regen I.
- **Reinforced Carapace** (rainforest): "9-damage-sword tier". Projectile protection. Proposed 22 armour / 4
  toughness / 0.1 KB / ~1.15x Netherite durability.
- **Ancientstone** (desert): "10-damage-sword tier" tank, -2.5% speed per piece. Proposed 24 armour / 6 toughness /
  0.2 KB / 2x Netherite durability.

**Built 2026-11-12** (item/ArmorSet, ExofallArmorItem, ArmorEvents; `/exofall armortest` passes): numbers as proposed
above, defence per piece boots/legs/chest/helmet: Mireweave 3/6/8/3, Honey 3/6/8/3 (2 toughness), Carapace 4/7/8/3,
Ancientstone 4/7/9/4; toughness and KB are per piece (vanilla convention). Mireweave scales per piece (12.5% notice
range, +7.5% ambush each), Carapace 10% projectile block per piece. Repair items are placeholders (Bog Eye, Royal Honey
Drop, Mantis Eye, Terror Leg); recipes come with the 5x5 table recipes. EPIC, fire resistant, trimmable, enchantable.
Textures: tools/armor/gen_armor.py (Netherite silhouette + shading remapped onto each set's ramp, details painted in
box-UV face space; preview on tools/armor/stand.geo.json with tools/mobs/preview.py).

## Recipes and dungeon-mob drops (2026-11-12)
Each dungeon mob drops one crafting material, 1 in 3 per player kill (+10%/Looting level): Bogspinner = Bogsilk
Spool, Mire Leech = Leech Fang, Honeyguard = Honeyguard Plate, Propolis Spitter = Propolis Gland, Antlion = Antlion
Mandible, Tomb Scorpion = Tomb Stinger, Canopy Assassin = Assassin Hook, Pouncer = Pouncer Shell. All armour, the 4
legendary weapons and the 6 baubles are crafted in the Shellrock table from these + boss drops (tools/gen_recipes.py
writes data/exofall/recipe/shellrock; it trims empty pattern edges, which the table requires). Armour per set: 18
main drop, 9 second drop, 3 boss drop (helmet/chest/legs), meteor block filler (Sapcane, Royal Amber, Shellrock,
Polished Chitinstone). Armour repairs with the set's main drop. `/exofall recipetest` checks every recipe in a real
5x5 grid and the drop rates. JEI page with "+" transfer (compat/jei, optional dep; dev NeoForge bumped to 21.1.265 because JEI 19.64 needs 21.1.238+).

## Legendary weapons (2026-11-12, Glenn's designs + numbers; built, uncraftable for now)
One per meteor, standalone `ExofallWeaponItem` (SwordItem on the METEORIC tier, 3000 uses placeholder), EPIC, in the
creative tab. 3D in hand, flat 32x32 sprite in inventory/ground/item frame (NeoForge `separate_transforms`, like the
trident). Models: `tools/weapons/gen_weapons.py` (boxes on the vanilla sword diagonal + per-texel painted atlas,
software preview renderer in `model3d.py`); icons: `tools/weapons/gen_icons.py` -> material-ID maps ->
`textures-src/item/weapon_icon/gen_weapon_px.py` (+ `<name>.fix` hand touches).
- **Quadblade of the Sands** (desert): 15 dmg / 0.7 speed (10.5 DPS), hit = Sandbound (-10% speed, 3 s). Four bronze
  estoc blades in a + on a gold-trimmed bronze mount, turquoise gems.
- **Royal Grandhammer** (hive): 20 / 0.5 (10 DPS), 10% lifesteal of damage dealt (LivingDamageEvent.Post), no sweep.
  Honeycomb core in a gold frame, ends dip toward the haft, wax drips.
- **Glasswing Scythe** (swamp): 11 / 1.2 (13.2 DPS), hit launches ~2 blocks up (vy 0.54), 4 s cooldown shown on the
  item, bosses (ExofallBoss or c:bosses) immune. Translucent wing blade, glowing (fullbright) Bog Eye, reed haft.
- **Serrated Longsword** (rainforest): 9 / 1.7 (15.3 DPS), stacking Bleeding: 1 dmg/s per stack, max 3 (up to ~18 DPS
  with full stacks); a hit tops the timer to >= 2 s but keeps its 1 s rhythm so combos bleed. Bleed damage type
  `exofall:bleed` bypasses the hit cooldown (never eats a sword hit), no knockback, armour still applies.
- Verified: `/exofall weapontest` (dev server) checks every effect; dev client loads all models with no warnings.
  Not verified: how they look/feel in hand in game.

## Baubles (2026-11-12, Glenn: "I do really like all of those; add them"; ~6 total planned, Glenn adds his own later)
Curios (now a required dependency, 9.5.1 like S202; jar in libs/). `item/BaubleItem` (ICurioItem) + `item/BaubleEvents`.
Uncraftable for now, RARE, 32x32 icons (`textures-src/item/bauble/gen_baubles.py`). Slots via curios item tags;
`data/exofall/curios/entities` gives players necklace/body/charm/ring. Two necklaces and two charms compete for
the single default slot of each (a choice, easy to retag).
- **Divebell Locket** (necklace, swamp): breathe underwater (LivingBreatheEvent), submerged mining speed +0.8 (= normal).
- **Ember Beetle Brooch** (body, desert): fire damage x0.5; ember particles (no real light; Gloaming hook possible later).
- **Leafcutter Charm** (charm, rainforest; replaced the Golden Scarab Ward 2026-11-12, Glenn: the Ward was "effectively an
  infinite totem of undying"): cobwebs and berry bushes don't slow you (`mixin/EntityMixin` cancels `makeStuckInBlock`
  for `exofall:leafcutter_passable`, both sides), berry bushes don't hurt, `exofall:leafcutter_cuttable` (leaves, vines,
  webs, bushes, weepvine) breaks instantly (BreakSpeed 1000). Leaves stay solid (passing through them was dropped).
- **Stinger Fang Ring** (ring, hive): fully charged hits that land add Poison I for 2 s.
- **Nursemaid's Wax Charm** (charm, hive): food gives +50% hunger/saturation; heal 1 every 2 s on `exofall:wax_blocks`
  (honey/honeycomb blocks + warded twins).
- **Lantern Moth Pendant** (necklace, rainforest): wearer in light < 7 -> hostiles within 8 get Glowing (server-side,
  so other players see the outline too).
- Verified (`/exofall baubletest`, damageable fake player): locket breathing + mining speed, brooch 4 -> 2 fire damage,
  leafcutter (berry damage cancelled, web slowdown skipped, leaves speed 1000), wax charm +3 food from bread. Not verified: ring, pendant, icons in game.

## Balance pass (2026-11-13, Glenn: "balanced to the planned progression level... based on attrition and should feel slightly overwhelming even to appropriately geared players")
The model and every number live in **tools/balance.py** (run it; keep the code in sync). Reference player = late
Act 3 S202: netherite-class 20 armour / 12 toughness, EPF 12, ~35 hearts, a late BMM weapon (13.6-16 base DPS,
~13.5 per swing). Before this pass the bugs had vanilla-overworld stats (a Combwasp died to one hit and did 0.4 hp
through that armour) and bosses died in ~25 s with heavy hits worth ~5% health.
- Lesser mobs ~2-2.5x health, ~2.5x damage: fodder 1-2% of health a hit, standard 3-5% (3-5 hits to kill), elites
  6-10% telegraphed (6-9 hits). Special attacks scaled the same (sting 26, drop 24, leap 20, boulder 24, globs 12/6,
  leech drain 6 / 2 s, Nursemaid heal 5, Honeyguard shield 60 / heavy 28).
- Bosses 900-1000 hp (2-3 min fights), light hits 5-9%, telegraphed heavies 18-26% (33% max enraged), never a one-shot.
- Nests 8-18 s between spawns (was 10-25 s), cap 6.
- Legendaries re-based to 17.6 DPS (Flaming Ruby +10%, BMM trait-tax rule): Quadblade 25/0.7, Grandhammer 32/0.5,
  Scythe 14.5/1.2, Longsword 9.5/1.7. These were GLENN's numbers (15/20/11/9) -> flagged to him; at 10-15 DPS they were
  below a crafted Aeternium BMM weapon.
- Armour (my numbers): Carapace toughness 4 -> 3.5/piece, Ancientstone 24 armour/6 toughness -> 22/4 per piece
  (armour above 20 + toughness 24 made it take ~20% of every mob hit). Still above netherite ("9/10-damage-sword tier").
- Pets: wild stats are late-game now, so tamed multipliers dropped to x1.5 health / x1.0 damage; pet heal 4.
