# BROMAX's Exofall

A Minecraft 1.21.1 NeoForge RPG mod. Huge procedurally generated meteors lie half-buried across the Overworld. Each one is a self-contained biome with its own stone, woods, plants, insect mobs, sub-structures, bosses and legendary gear.

Breaking through a meteor's crust takes the Crustbreaker, a pickaxe forged from late-game materials (a nether star, echo shards, netherite, a breeze rod). The gear inside starts around Netherite level and goes beyond it: four armour sets, four legendary weapons and six baubles, made at the 5x5 Shellrock Crafting Table from the drops of the bugs in the Warrens (dungeons under each meteor) and the four bosses.

Built for the late game of the **Sovereign 202** modpack, but it works on its own. Gear power can be scaled in the config (`gear.damageMultiplier`, `gear.armorMultiplier`).

**Status:** 1.1.0, first public release. Requires GeckoLib and Curios; Patchouli (Field Guide) and JEI (5x5 recipe pages) are optional. See [CHANGELOG.md](CHANGELOG.md).

## Credits
Inspired by **The Erebus** by vadis365 and team. Exofall is an original mod: no Erebus code, assets or names are used.

## Building
```bash
./gradlew build     # jar in build/libs/
./gradlew deploy    # build + copy into the "Exofall Testing" CurseForge instance (-PdeployInstance=... to change)
```
Requires Java 21.

## License
MIT. See [LICENSE](LICENSE).
