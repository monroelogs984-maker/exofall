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
- **Placeholder Crustbreaker** pickaxe (name not final, netherite pickaxe texture). Placeholder recipe:
  echo shard / nether star / echo shard over netherite ingot / netherite pickaxe / netherite ingot.
- Caveat: modded pickaxes whose tier uses its own empty `incorrect_for` tag can still mine crust. Check S202's
  above-netherite tools when the pack integration happens.

## Open questions
- Locator item for finding a specific meteor type (1 per 850 blocks split across 5-7 types is a long hunt)
- Ways past the crust to block: TNT, creepers, ender pearls, chorus fruit, mods in S202
