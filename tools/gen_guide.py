#!/usr/bin/env python3
"""Writes the Exofall Field Guide (a Patchouli book) from the content below.

    python3 tools/gen_guide.py      (from anywhere; re-run after editing)

Output: data/exofall/patchouli_books/guide/book.json and assets/exofall/patchouli_books/guide/en_us/{categories,
entries,templates}. Old category/entry files are deleted first. 5x5 recipes use the "exofall:shellrock" template
(compat/patchouli/ShellrockProcessor reads the real recipe). Text pages hold ~280 characters, entity pages ~110.
"""
import json, os, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
BOOK = os.path.join(RES, "assets/exofall/patchouli_books/guide/en_us")

# ---------------------------------------------------------------- page helpers
def text(t, title=None):
    p = {"type": "patchouli:text", "text": t}
    if title: p["title"] = title
    return p

def spot(item, t, title=None):
    p = {"type": "patchouli:spotlight", "item": item, "text": t}
    if title: p["title"] = title
    return p

def craft(recipe, t=None):
    p = {"type": "patchouli:crafting", "recipe": recipe}
    if t: p["text"] = t
    return p

def mob(entity, t, scale=0.7, name=None):
    p = {"type": "patchouli:entity", "entity": entity, "scale": scale, "text": t}
    if name: p["name"] = name
    return p

def table(item):
    return {"type": "exofall:shellrock", "recipe": f"exofall:shellrock/{item}"}

CATS = []      # (id, name, description, icon, sortnum, parent)
ENTRIES = []   # (category, id, name, icon, pages, sortnum)

def cat(cid, name, desc, icon, sort, parent=None):
    CATS.append((cid, name, desc, icon, sort, parent))

def entry(c, eid, name, icon, pages, sort=0):
    ENTRIES.append((c, eid, name, icon, pages, sort))

E = "exofall:"

# ================================================================ Getting started
cat("basics", "Getting Started", "Meteors, the pick that cracks them and the table that crafts their gear.", E + "crustbreaker", 0)

entry("basics", "welcome", "Welcome to Exofall", "minecraft:writable_book", [
    text("Huge meteors have fallen across the world. Each one is a sealed shell with a living world inside: alien "
         "plants, alien woods and swarms of giant bugs. There are four kinds: $(l:meteors/swamp)Swamp$(/l), "
         "$(l:meteors/hive)Hive$(/l), $(l:meteors/desert)Desert$(/l) and $(l:meteors/rainforest)Rainforest$(/l).", "Welcome"),
    text("Every meteor holds the same three dangers: the bugs of its interior, the $(l:warrens/warrens)Warrens$(/l) "
         "dug under its floor, and a boss sealed in the dome at its heart.$(br2)The meteors don't come in any order. "
         "Each one gives different gear: pick the one that suits how you play."),
    text("A tip before you go in: every bug that ambushes has a $(thing)tell$(), a sound or sight that gives it away "
         "a moment before it strikes. Learn them and the meteors get a lot fairer. The "
         "$(l:creatures/swamp/glasswing)creature pages$(/l) list them all."),
], 0)

entry("basics", "finding", "Finding a Meteor", "minecraft:compass", [
    text("Meteors land on dry ground, never in lakes or the sea. They sit about half buried in a wide crater: "
         "levelled ground, a raised rim and a scatter of gravel, tuff and coarse dirt. They're big (55 to 78 blocks "
         "across the middle) and about 550 blocks apart on average.", "Finding a Meteor"),
    text("Look for the crater rim from a hill, or the pale shell rising out of the ground. The shell is "
         "$(l:basics/stones)Shellrock$(/l): nothing but a $(l:basics/crustbreaker)Crustbreaker$(/l) can mine it, "
         "and explosions don't touch it.$(br2)Ops can find the nearest one with "
         "$(thing)/locate structure exofall:swamp_meteor$() (or hive, desert, rainforest)."),
], 1)

entry("basics", "crustbreaker", "The Crustbreaker", E + "crustbreaker", [
    text("The only tool that mines $(item)Shellrock$(), so it's your key into every meteor. It's also a strong "
         "pickaxe in its own right, a step past Netherite.$(br2)You need it before you can reach anything meteoric, so "
         "it's made from late-game Overworld, Nether and Deep Dark materials on a normal crafting table.", "The Crustbreaker"),
    craft(E + "crustbreaker", "Netherite pick, Netherite, echo shards, a nether star and a breeze rod."),
], 2)

entry("basics", "stones", "Meteor Stones", E + "chitinstone", [
    spot(E + "shellrock", "The crust. Pearly, blast-proof and harder than obsidian. Only a "
         "$(l:basics/crustbreaker)Crustbreaker$(/l) can mine it.", "Shellrock"),
    spot(E + "chitinstone", "The main stone inside, made of fossil shell plates. Polish it, brick it or carve "
         "pillars from it (stonecutter or crafting). Stairs, slabs and walls too.", "Chitinstone"),
    spot(E + "glowmarl", "Soft glowing soil-stone. Alien plants and saplings only grow on Glowmarl, so take some "
         "home if you want a meteor garden.", "Glowmarl"),
], 3)

entry("basics", "table", "Shellrock Crafting Table", E + "shellrock_crafting_table", [
    text("Meteor gear is too big for a normal crafting table. The Shellrock Crafting Table has a $(thing)5x5$() grid "
         "for armour, legendary weapons and baubles, and it still crafts everything a normal table does.$(br2)With "
         "JEI installed, its recipes show up there too, with a button that fills the grid for you.", "The 5x5 Table"),
    craft(E + "shellrock_crafting_table", "Four Shellrock around a crafting table."),
], 4)

# ================================================================ Meteors
cat("meteors", "The Meteors", "The four kinds of meteor and what grows inside them.", E + "glowmarl", 1)

entry("meteors", "inside", "Inside a Meteor", E + "glowmarl", [
    text("Inside, the shell is lined with Chitinstone and lit by glowing plants and stone, not the sun, so it's "
         "never fully dark and never fully day. No Overworld mobs spawn here, only the meteor's own bugs.$(br2)The "
         "bugs hunt each other: you'll see dragonflies chasing grubs and mantises eating moths.", "Inside a Meteor"),
    text("At the very centre stands a sealed $(l:bosses/arenas)boss dome$(/l) on a paved plaza. Under the floor run "
         "the $(l:warrens/warrens)Warrens$(/l), and shacks dot the ground above them.$(br2)Alien plants only grow on "
         "$(item)Glowmarl$(). You can carry them out, but they won't spread on normal ground."),
], 0)

entry("meteors", "swamp", "Swamp Meteor", E + "lanternpod", [
    text("A bug swamp: mud floors, shallow pools under Driftmoss and Bladderpads, and Glowmarl banks. "
         "$(item)Stiltreed$() trees stand on arching roots with frond caps. Lanternpods and Weepvines hang from the "
         "ceiling, and green froglights glow in the crust.", "Swamp Meteor"),
    spot(E + "lanternpod", "Glowing bulb that hangs from the ceiling. Bright light (12).", "Lanternpod"),
    spot(E + "bladderpad", "A lily pad full of gas. Step on it and it pops into a slowing cloud.", "Bladderpad"),
    spot(E + "snaretongue", "Pitcher plant. Slows and nips anything that walks into it, like a berry bush.", "Snaretongue"),
    spot(E + "sapcane", "Tall sweet reed. Mudgrubs love it, and Reedmantises hide in it. Watch for rustling.", "Sapcane"),
    spot(E + "weepvine", "Ceiling vine you can climb.", "Weepvine"),
], 1)

entry("meteors", "hive", "Hive Meteor", E + "royal_amber", [
    text("A giant wasp nest. Parallel comb sheets run from floor to ceiling, torn open by holes and low doorways. "
         "Honey patches and Glowmarl beds cover the floor. Thick $(item)Combwood$() trunks grow up through the comb "
         "with hollow, larva-filled cores.", "Hive Meteor"),
    spot(E + "nectar_bell", "Soft-glowing flower on the Glowmarl beds.", "Nectar Bell"),
    spot(E + "waxcap", "A waxy orange fungus.", "Waxcap"),
    spot(E + "royal_amber", "Glowing amber crystals (light 9). A crafting block for the Crystallized Honey armour.", "Royal Amber"),
], 2)

entry("meteors", "desert", "Desert Meteor", E + "rattlegrass", [
    text("Beetle desert: sand and red-sand dunes over Chitinstone with buried bone. The dips between dunes are Glowmarl "
         "flats where everything grows, including the pale, spiralling $(item)Ribwood$() trees. Banded Chitinstone "
         "pillars hold up the dome.", "Desert Meteor"),
    spot(E + "rattlegrass", "Rattles when you walk through it, and the noise alerts nearby beetles. Sneak to stay quiet.", "Rattlegrass"),
    spot(E + "dunespine", "A hollow cactus that nips like a berry bush.", "Dunespine"),
    spot(E + "nightbloom", "A pale blue glowing flower.", "Nightbloom"),
], 3)

entry("meteors", "rainforest", "Rainforest Meteor", E + "glowmoss_curtain", [
    text("A misty conifer forest. Glowmarl floors with Umbrella Ferns and Mistcaps, tall blue-grey $(item)Veilpine$() "
         "trees and curtains of glowing lichen hanging from the ceiling.", "Rainforest Meteor"),
    spot(E + "umbrella_fern", "A tall broad fern.", "Umbrella Fern"),
    spot(E + "mistcap", "A pale blue mushroom with a faint glow.", "Mistcap"),
    spot(E + "glowmoss_curtain", "Glowing lichen curtain (light 7). You can climb it.", "Glowmoss Curtain"),
], 4)

entry("meteors", "woods", "Alien Woods", E + "stiltreed_planks", [
    text("Each meteor has its own wood with the full set: logs, planks, stairs, slabs, fences, gates, doors, "
         "trapdoors, buttons, pressure plates and boats. Strip logs with an axe.$(br2)Saplings only grow on "
         "$(item)Glowmarl$().", "Alien Woods"),
    spot(E + "stiltreed_log", "Swamp. Segmented olive reed-trees on stilt roots.", "Stiltreed"),
    spot(E + "combwood_log", "Hive. Wax-coated amber wood with a comb pattern.", "Combwood"),
    spot(E + "ribwood_log", "Desert. Pale, leafless and spiralling like a ribcage.", "Ribwood"),
    spot(E + "veilpine_log", "Rainforest. Blue-grey conifer.", "Veilpine"),
], 5)

# ================================================================ Creatures
cat("creatures", "Creatures", "Every bug in the meteors, and the tell that gives each one away.", E + "glasswing_spawn_egg", 2)
cat("creatures/swamp", "Swamp Creatures", "Bugs of the Swamp meteor.", E + "glasswing_spawn_egg", 0, "creatures")
cat("creatures/hive", "Hive Creatures", "Bugs of the Hive meteor.", E + "combwasp_spawn_egg", 1, "creatures")
cat("creatures/desert", "Desert Creatures", "Bugs of the Desert meteor.", E + "shieldback_spawn_egg", 2, "creatures")
cat("creatures/rainforest", "Rainforest Creatures", "Bugs of the Rainforest meteor.", E + "bloommantis_spawn_egg", 3, "creatures")

def creature(c, eid, name, scale, short, detail, sort):
    entry(c, eid, name, E + f"{eid}_spawn_egg", [mob(E + eid, short, scale), text(detail, name)], sort)

# swamp
creature("creatures/swamp", "mudgrub", "Mudgrub", 0.8, "A slow, harmless larva.",
         "Pale, slow and harmless. It panics when hurt. Everything in the swamp eats Mudgrubs.$(br2)Breed them with "
         "$(item)Sapcane$(). It's also a $(l:pets/pets)pet parent$(/l), paired with the Glasswing.", 0)
creature("creatures/swamp", "glasswing", "Glasswing", 0.5, "Giant dragonfly. Hovers, then dives.",
         "Hovers to the side and above you. $(thing)Tell:$() a high swooping cry and a curled abdomen, then it dives in "
         "a straight line at where you stood. Sidestep it. Hitting it during the wind-up breaks the dive off.$(br2)Drops a "
         "$(l:bosses/arenas)Swamp Sigil Fragment$(/l) when you kill it.", 1)
creature("creatures/swamp", "bloodgnat", "Bloodgnat", 0.9, "Mosquito swarms with a constant whine.",
         "Comes in swarms of 3 to 5. Each bite heals it and weakens you. After 3 bites a gnat is full and leaves for 30 "
         "seconds, so a swarm wears itself out.$(br2)Smoke from a lit campfire below them scatters them.", 2)
creature("creatures/swamp", "divebell_beetle", "Divebell Beetle", 0.7, "A neutral diving beetle.",
         "Swims and dives in the pools. It leaves you alone unless you hit it, and then it alerts the bugs around "
         "it.$(br2)Drops $(item)Bubble Sacs$(), used for the Divebell Locket.", 3)
creature("creatures/swamp", "reedmantis", "Reedmantis", 0.6, "Ambush mantis hiding in Sapcane.",
         "Hides in Sapcane. $(thing)Tell:$() the reeds rustle and shed bits, then it rocks back with its arms up for a "
         "second before it strikes. Eats Mudgrubs.$(br2)A $(l:pets/pets)pet parent$(/l), paired with the Bloommantis.", 4)
creature("creatures/swamp", "bogspinner", "Bogspinner", 0.6, "Fishing spider that spits silk.",
         "Strings webs across gaps. From range it rears up ($(thing)tell$()) and spits silk that leaves a web where it "
         "lands. Its bite is half again as strong on anything stuck in a web. Webs vanish after 30 s.$(br2)Drops "
         "$(item)Bogsilk Spool$().", 5)
creature("creatures/swamp", "mire_leech", "Mire Leech", 0.7, "Lurks in water and latches on.",
         "Only hunts things in water. $(thing)Tell:$() ripples on the surface above it. Its bite latches on and drains "
         "you. Get out of the water, or hit it, to knock it off.$(br2)Drops $(item)Leech Fang$().", 6)
# hive
creature("creatures/hive", "combwasp", "Combwasp", 0.7, "Swarming worker wasp.",
         "Comes in swarms. Its sting poisons briefly. Hurt one hive fighter and every fighter within 16 blocks "
         "comes for you.$(br2)A $(l:pets/pets)pet parent$(/l), paired with the Nursemaid.", 0)
creature("creatures/hive", "stinger", "Stinger", 0.6, "Soldier with a slow, deadly sting.",
         "$(thing)Tell:$() a long wind-up of more than a second. Then it hits hard and poisons badly. Step back out of "
         "reach when it winds up.$(br2)Drops a $(l:bosses/arenas)Hive Sigil Fragment$(/l) when you kill it.", 1)
creature("creatures/hive", "nursemaid", "Nursemaid", 0.7, "Heals hurt hive bugs.",
         "Never attacks. It flies to hurt hive bugs and heals them, and it runs from you. Kill it first in a big "
         "fight.$(br2)A $(l:pets/pets)pet parent$(/l), paired with the Combwasp.", 2)
creature("creatures/hive", "waxmite", "Waxmite", 0.9, "Scavenger that eats dropped food.",
         "Scuttles around eating food items you drop. It leaves you alone, unless you fall below half health.", 3)
creature("creatures/hive", "larva_cocoon", "Larva Cocoon", 0.8, "Hatches into a wasp if left alone.",
         "Ripens over about 2 minutes while a player is near, then hatches a Combwasp, or sometimes a Stinger. Break "
         "cocoons before they hatch.", 4)
creature("creatures/hive", "honeyguard", "Honeyguard", 0.5, "Slow wasp behind a comb shield.",
         "Blocks most of the damage from the front, and every blocked hit wears its shield down. Heavy hits wear it "
         "fast (the Royal Grandhammer breaks it in one). It reseals after 30 s. It hits hard and knocks you "
         "back.$(br2)Drops $(item)Honeyguard Plate$().", 5)
creature("creatures/hive", "propolis_spitter", "Propolis Spitter", 0.6, "Flying wasp that spits sticky resin.",
         "Keeps its distance. $(thing)Tell:$() its belly glows and pumps, dripping honey. The spit slows you and leaves "
         "sticky puddles that last 10 s.$(br2)Drops $(item)Propolis Gland$().", 6)
# desert
creature("creatures/desert", "sanddiver", "Sanddiver", 0.7, "Burrows in sand and bursts out.",
         "Hides buried in sand. $(thing)Tell:$() a puff of sand just before it bursts out and bites. It burrows again "
         "after 5 calm seconds.", 0)
creature("creatures/desert", "shieldback", "Shieldback", 0.6, "Armoured from the front.",
         "Its front takes only a fifth of the damage, with a clang. It turns slowly, so get behind it.$(br2)A "
         "$(l:pets/pets)pet parent$(/l), paired with the Gold-Encrusted Scarab.", 1)
creature("creatures/desert", "ember_beetle", "Ember Beetle", 0.9, "Glowing, harmless prey.",
         "A small glowing beetle the hunters feed on. Harmless.", 2)
creature("creatures/desert", "roller", "Roller", 0.6, "Rolls boulders down at you.",
         "Keeps 5 to 16 blocks away. $(thing)Tell:$() it rears up for a second, then rolls a heavy boulder at you. It "
         "takes 8 s to make another. Dodge sideways.", 3)
creature("creatures/desert", "golden_scarab", "Gold-Encrusted Scarab", 0.7, "Rare, armoured, runs away.",
         "Rare and heavily armoured, and it flees from you. Catch it: a wild one drops the "
         "$(l:bosses/arenas)Desert Sigil$(/l) that opens the Dozen-legged Wrath's arena.$(br2)Tamed and bred ones drop "
         "nothing. A $(l:pets/pets)pet parent$(/l), paired with the Shieldback.", 4)
creature("creatures/desert", "antlion", "Antlion", 0.6, "Digs a pit trap in the sand.",
         "Digs a funnel in sand and sits buried at the bottom. $(thing)Tell:$() the pit and the trickling sand. It drags "
         "everything nearby toward it, then bites and roots you in place. Off sand it's just a slow bug.$(br2)Drops "
         "$(item)Antlion Mandible$().", 5)
creature("creatures/desert", "tomb_scorpion", "Tomb Scorpion", 0.5, "Pincers and a glowing sting.",
         "Its pincers slow you. Up close it raises its glowing turquoise sting ($(thing)tell$()), then strikes with "
         "long reach, hitting hard and leaving you Sandbound.$(br2)Drops $(item)Tomb Stinger$().", 6)
# rainforest
creature("creatures/rainforest", "twigwalker", "Twigwalker", 0.6, "A stick insect.",
         "Neutral. It freezes like a twig when you come within 8 blocks, and kicks if you bother it.", 0)
creature("creatures/rainforest", "bloommantis", "Bloommantis", 0.6, "Flower-mimic mantis.",
         "Looks like a flower. $(thing)Tell:$() petals shiver loose and it chimes before it strikes. Eats Lantern "
         "Moths.$(br2)Drops a $(l:bosses/arenas)Rainforest Sigil Fragment$(/l). A $(l:pets/pets)pet parent$(/l), paired "
         "with the Reedmantis.", 1)
creature("creatures/rainforest", "leafcutter", "Leafcutter", 0.8, "Marches in columns carrying leaves.",
         "Columns of ants marching with leaves. They ignore you unless you block the column. Stand in the way for 2 s "
         "($(thing)tell:$() angry chittering at 1 s) and the whole column attacks.", 2)
creature("creatures/rainforest", "lantern_moth", "Lantern Moth", 0.8, "Drawn to your light.",
         "Harmless. It circles players holding a light and sheds glowing dust.", 3)
creature("creatures/rainforest", "canopy_assassin", "Canopy Assassin", 0.5, "Hangs from the ceiling and drops.",
         "Hangs upside down under ceilings and leaves. $(thing)Tell:$() a silk thread and drips. When you pass close "
         "underneath, it drops on you and makes you bleed. Shoot it down first.$(br2)Drops $(item)Assassin Hook$().", 4)
creature("creatures/rainforest", "pouncer", "Pouncer", 0.6, "Jumping spider.",
         "$(thing)Tell:$() it crouches with its front legs raised and its eyes glint. Then it leaps at where you stood, "
         "so move.$(br2)Drops $(item)Pouncer Shell$().", 5)

# ================================================================ Warrens
cat("warrens", "The Warrens", "The dungeons dug under every meteor.", E + "brood_comb", 3)

entry("warrens", "warrens", "The Warrens", E + "brood_comb", [
    text("Under every meteor's floor runs a Warren: mineshaft-style tunnels with wooden supports and old rails, "
         "joining rooms on two levels. Store rooms hold chests and barrels, nest rooms hold spawners, and pillared "
         "halls hold whatever's living there.", "The Warrens"),
    text("$(thing)Getting in:$() look for the large wooden entrance huts with doors. A spiral stair inside goes down "
         "to the first level. Another spiral stair leads to the deep level and its $(thing)vault$(), which has the best "
         "loot and nests in its corners.$(br2)Small huts without stairs have a nest inside, and a chest."),
    text("Side tunnels end in a chest, a nest or a cave-in. The tunnels are dark, so the meteor's bugs spawn there on "
         "their own too. Bring light.$(br2)The Warrens are where the $(l:gear/materials)crafting drops$(/l) come "
         "from: the dungeon bugs spawn from the nests here."),
], 0)

entry("warrens", "nests", "Nests", E + "mire_nest", [
    text("Nests are lumps riddled with holes, with something always stirring inside. While you're within 16 blocks, "
         "a nest lets out 1 or 2 of its meteor's bugs every 10 to 25 seconds, until 6 are around it. Break a nest to "
         "stop it. They drop a good amount of experience.", "Nests"),
    spot(E + "mire_nest", "Swamp: Bogspinners, Glasswings, Bloodgnats and Reedmantises.", "Mire Nest"),
    spot(E + "brood_comb", "Hive: Combwasps, Stingers, Propolis Spitters, Honeyguards and Waxmites.", "Brood Comb"),
    spot(E + "sand_burrow", "Desert: Tomb Scorpions, Sanddivers, Shieldbacks and Antlions.", "Sand Burrow"),
    spot(E + "rotwood_hollow", "Rainforest: Pouncers, Canopy Assassins, Twigwalkers, Bloommantises and Lantern Moths.", "Rotwood Hollow"),
], 1)

entry("warrens", "loot", "Warren Loot", "minecraft:chest", [
    text("$(thing)Chests$() hold iron, gold, food, arrows, experience bottles, Glowmarl, saplings and things from "
         "their meteor. Now and then there's a diamond or an enchanted book. One chest in five holds a "
         "$(l:bosses/arenas)sigil fragment$(/l).$(br2)$(thing)Barrels$() hold supplies: torches, coal, string, rails.", "Warren Loot"),
    text("$(thing)Vault$() chests (deep level) are much richer: diamonds, emeralds, golden apples, netherite scrap, "
         "echo shards, enchanted books and gear. Each holds one or two sigil fragments (gold blocks in the Desert, whose "
         "sigil comes from the Scarab)."),
], 2)

# ================================================================ Bosses
cat("bosses", "Bosses", "The four bosses, their arenas and the sigils that wake them.", E + "swamp_sigil", 4)

entry("bosses", "arenas", "Arenas and Sigils", E + "desert_sigil", [
    text("Each meteor's centre holds a sealed dome. Its door is shut by an unbreakable $(item)Sigil Seal$(), with a "
         "$(item)Sigil Socket$() in the door post. Use the meteor's sigil on the socket to open the door.$(br2)The "
         "dome is warded: its walls and floor can't be mined or blown up.", "Arenas"),
    text("Inside, the boss lies dormant until you step in. Then the door seals behind you and the fight begins (with a "
         "short introduction the first time). If everyone leaves for 10 seconds, the boss heals, goes back to sleep and "
         "the door opens. Beat it and the door opens. Another sigil wakes it again.", "The Fight"),
    spot(E + "desert_sigil", "Dropped whole by a wild $(l:creatures/desert/golden_scarab)Gold-Encrusted Scarab$(/l) "
         "when you kill it. Opens the Dozen-legged Wrath's arena.", "Desert Sigil"),
    text("The other three sigils are crafted from $(thing)4 fragments$(). Fragments drop from every "
         "$(l:creatures/swamp/glasswing)Glasswing$(/l), $(l:creatures/hive/stinger)Stinger$(/l) and "
         "$(l:creatures/rainforest/bloommantis)Bloommantis$(/l) you kill, and from Warren chests.", "Sigil Fragments"),
    craft(E + "swamp_sigil"),
    craft(E + "hive_sigil"),
    craft(E + "rainforest_sigil"),
], 0)

def boss(eid, name, scale, short, pages_text, drops, sort):
    pages = [mob(E + eid, short, scale)]
    for i, t in enumerate(pages_text):
        pages.append(text(t, name if i == 0 else None))
    pages.append(text(drops, "Drops"))
    entry("bosses", eid, name, E + f"{eid}_spawn_egg", pages, sort)

boss("terror_of_the_bog", "Terror of the Bog", 0.25, "A giant water scorpion. Swamp.", [
    "Every attack has a wind-up. It $(thing)snaps$() up close and $(thing)grabs$(): if it catches you, it holds you "
    "for a few seconds. Hit it hard enough while held and it lets go, stunned.",
    "It $(thing)submerges$() and bursts up under you: watch for the bubbles boiling up. It spits mud, three at a time "
    "when angry. At half health it calls Bloodgnats.",
], "$(item)Lethal Proboscis$() and 2 $(item)Bog Eyes$(), always. They make the "
   "$(l:gear/weapons)Glasswing Scythe$(/l) and the $(l:gear/armour)Mireweave$(/l) armour.", 1)
boss("waxen_queen", "Waxen Queen", 0.25, "Mother of the hive. Hive.", [
    "She flies. Her $(thing)sting dive$() leaves her dazed on the floor for a moment: that's your opening. She sprays "
    "wax in a fan, lays cocoons that are nearly ripe, and buffets you with her wings if you stand under her.",
    "Hive bugs defend her and Nursemaids heal her, so kill the Nursemaids first. When angry she calls Stingers.",
], "4 $(item)Royal Honey Drops$() and 2 $(item)Crystallized Wings$(), always. They make the "
   "$(l:gear/weapons)Royal Grandhammer$(/l) and the $(l:gear/armour)Crystallized Honey$(/l) armour.", 2)
boss("dozen_legged_wrath", "Dozen-legged Wrath", 0.2, "A giant red centipede. Desert.", [
    "About 11 blocks long, and every body segment can be hit. It bites, and it $(thing)charges$(): if it hits a wall "
    "it's stunned, so stand in front of one and step aside. It $(thing)burrows$() (sand puffs show where it'll come up) "
    "and sweeps its tail at anyone beside or behind it.",
    "Below 40% health it speeds up and charges twice in a row.",
], "16 $(item)Terror Legs$() and a $(item)Venomjaw$(), always. They make the "
   "$(l:gear/weapons)Quadblade of the Sands$(/l) and the $(l:gear/armour)Ancientstone$(/l) armour.", 3)
boss("apex_mantis", "Apex Mantis", 0.25, "A great green mantis. Rainforest.", [
    "It swings a 3-hit blade combo and leaps at you. Hit it 3 times quickly and it $(thing)guards$(): it parries hits "
    "from the front and strikes back, so back off or get behind it.",
    "When angry it vanishes into camouflage. Listen for rustling and chimes: a loud chime comes half a second before it "
    "strikes. Hitting it reveals it.",
], "A $(item)Mantis Eye$() and the $(item)Mantis Greatclaw$(), always. The Greatclaw is a weak blade on purpose: "
   "it's the base of the $(l:gear/weapons)Serrated Longsword$(/l).", 4)

# ================================================================ Gear
cat("gear", "Meteor Gear", "Armour, legendary weapons and baubles, crafted in the Shellrock table.", E + "mireweave_chestplate", 5)

entry("gear", "materials", "Crafting Materials", E + "bogsilk_spool", [
    text("Meteor gear is made from what the bugs drop. Each $(l:warrens/warrens)Warren$(/l) bug drops a crafting part "
         "about 1 kill in 3 (more with Looting) when a player kills it. Bosses always drop their parts.", "Crafting Materials"),
    spot(E + "bogsilk_spool", "From Bogspinners (swamp).", "Bogsilk Spool"),
    spot(E + "leech_fang", "From Mire Leeches (swamp).", "Leech Fang"),
    spot(E + "honeyguard_plate", "From Honeyguards (hive).", "Honeyguard Plate"),
    spot(E + "propolis_gland", "From Propolis Spitters (hive).", "Propolis Gland"),
    spot(E + "antlion_mandible", "From Antlions (desert).", "Antlion Mandible"),
    spot(E + "tomb_stinger", "From Tomb Scorpions (desert).", "Tomb Stinger"),
    spot(E + "assassin_hook", "From Canopy Assassins (rainforest).", "Assassin Hook"),
    spot(E + "pouncer_shell", "From Pouncers (rainforest).", "Pouncer Shell"),
], 0)

def armour(eid, name, perk, sort):
    pages = [text(perk, name)] + [table(f"{eid}_{p}") for p in ("helmet", "chestplate", "leggings", "boots")]
    entry("gear", f"armour_{eid}", name, E + f"{eid}_chestplate", pages, sort)

entry("gear", "armour", "Armour Sets", E + "ancientstone_chestplate", [
    text("Four sets, one per meteor. They aren't steps on a ladder: each suits a different way to play. All of them "
         "are around Netherite or better, can't burn, take enchantments and trims, and repair with their meteor's "
         "main crafting drop.", "Armour Sets"),
], 1)
armour("mireweave", "Mireweave", "Swamp stealth cloth. $(thing)Each piece$(): mobs notice you 12.5% closer, and you "
       "deal 7.5% more damage to mobs that aren't targeting you. A full set halves how far mobs see you and adds 30% "
       "ambush damage. Bosses aren't fooled.$(br2)Armour like Netherite, toughness 2.5.", 2)
armour("crystallized_honey", "Crystallized Honey", "Amber glass from the hive. Protects like diamond, lasts like "
       "Netherite.$(br2)$(thing)Full set$(): +15% speed and a Regeneration that never runs out.", 3)
armour("reinforced_carapace", "Reinforced Carapace", "Mantis-green chitin. A little tougher than Netherite, with some "
       "knockback resistance.$(br2)$(thing)Each piece$() blocks 10% of projectile damage, so a full set blocks 40%.", 4)
armour("ancientstone", "Ancientstone", "Carved desert stone: the tank set. The most armour and toughness, strong "
       "knockback resistance and twice Netherite's durability.$(br2)$(thing)Each piece$() makes you 2.5% slower.", 5)

entry("gear", "weapons", "Legendary Weapons", E + "glasswing_scythe", [
    text("One legendary weapon per meteor, made from its boss's drops. Each has its own effect.", "Legendary Weapons"),
    spot(E + "glasswing_scythe", "Swamp. Fast, and each hit launches the target 2 blocks up (4 s cooldown, not bosses).", "Glasswing Scythe"),
    table("glasswing_scythe"),
    spot(E + "royal_grandhammer", "Hive. Slow and crushing, and it heals you for 10% of the damage it deals.", "Royal Grandhammer"),
    table("royal_grandhammer"),
    spot(E + "quadblade_of_the_sands", "Desert. Four blades. Hits slow the target by 10% for 3 s.", "Quadblade of the Sands"),
    table("quadblade_of_the_sands"),
    spot(E + "serrated_longsword", "Rainforest. The fastest. Hits stack Bleeding: 1 damage a second per stack, up to 3.", "Serrated Longsword"),
    table("serrated_longsword"),
], 6)

entry("gear", "baubles", "Baubles", E + "divebell_locket", [
    text("Trinkets worn in Curios slots (necklace, body, charm, ring). The two necklaces share one slot, and so do "
         "the two charms, so pick.", "Baubles"),
    spot(E + "divebell_locket", "Necklace. Breathe underwater and mine at normal speed underwater.", "Divebell Locket"),
    table("divebell_locket"),
    spot(E + "lantern_moth_pendant", "Necklace. In the dark, hostile mobs within 8 blocks get outlined.", "Lantern Moth Pendant"),
    table("lantern_moth_pendant"),
    spot(E + "ember_beetle_brooch", "Body. Fire damage is halved.", "Ember Beetle Brooch"),
    table("ember_beetle_brooch"),
    spot(E + "leafcutter_charm", "Charm. Cobwebs and berry bushes don't slow or hurt you; leaves, vines and webs break instantly.", "Leafcutter Charm"),
    table("leafcutter_charm"),
    spot(E + "nursemaids_wax_charm", "Charm. Food fills 50% more, and you heal while standing on honey or honeycomb.", "Nursemaid's Wax Charm"),
    table("nursemaids_wax_charm"),
    spot(E + "stinger_fang_ring", "Ring. Fully charged hits poison for 2 s.", "Stinger Fang Ring"),
    table("stinger_fang_ring"),
], 7)

# ================================================================ Pets
cat("pets", "Pets", "Taming and breeding the meteors' bugs.", E + "sapcane_candy", 6)

entry("pets", "pets", "Taming and Breeding", E + "sapcane_candy", [
    text("Eight bugs can be tamed, in four pairs. Each pair shares a $(thing)treat$().$(br2)Feed a wild one its treat: "
         "each treat has a 1 in 3 chance to tame it (hearts) or not (smoke). Tamed bugs never attack you or other tamed "
         "bugs, and they never despawn.", "Taming"),
    text("Feed two tamed adults of the same kind and they'll find each other and have a tamed baby. Babies are half "
         "size and grow up in one day (treats speed that up). Parents rest for 5 minutes before breeding "
         "again.$(br2)Crossing the two kinds in a pair is coming in a later version.", "Breeding"),
    craft(E + "sapcane_candy", "Swamp: Glasswing and Mudgrub."),
    craft(E + "nectar_drop", "Hive: Combwasp and Nursemaid."),
    craft(E + "dune_pellet", "Desert: Gold-Encrusted Scarab and Shieldback."),
    craft(E + "mistcap_morsel", "Rainforest: Bloommantis and Reedmantis."),
], 0)

# ================================================================ write
TEMPLATE = {
    "processor": "com.bromax.exofall.compat.patchouli.ShellrockProcessor",
    "components": [
        {"type": "patchouli:header", "text": "#title", "x": -1, "y": -1},
        {"type": "patchouli:image", "image": "exofall:textures/gui/patchouli/shellrock_grid.png", "x": 0, "y": 12,
         "u": 0, "v": 0, "width": 128, "height": 128, "texture_width": 128, "texture_height": 128},
    ] + [{"type": "patchouli:item", "item": f"#s{i}", "x": 14 + (i % 5) * 18, "y": 13 + (i // 5) * 18} for i in range(25)]
      + [{"type": "patchouli:item", "item": "#out", "x": 50, "y": 118}],
}

def main():
    for sub in ("categories", "entries", "templates"):
        shutil.rmtree(os.path.join(BOOK, sub), ignore_errors=True)
    def w(path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    w(os.path.join(RES, "data/exofall/patchouli_books/guide/book.json"), {
        "name": "Exofall Field Guide",
        "landing_text": "Notes on the meteors: how to get in, what lives inside, what it drops and what to make "
                        "from it.$(br2)Start with $(l:basics/welcome)Getting Started$(/l).",
        "use_resource_pack": True,
        "creative_tab": "exofall:main",
        "book_texture": "patchouli:textures/gui/book_green.png",
        "filler_texture": "patchouli:textures/gui/page_filler.png",
        "show_progress": False,
        "i18n": False,
    })
    w(os.path.join(BOOK, "templates/shellrock.json"), TEMPLATE)
    for cid, name, desc, icon, sort, parent in CATS:
        c = {"name": name, "description": desc, "icon": icon, "sortnum": sort}
        if parent: c["parent"] = f"exofall:{parent}"
        w(os.path.join(BOOK, "categories", cid + ".json"), c)
    for c, eid, name, icon, pages, sort in ENTRIES:
        w(os.path.join(BOOK, "entries", c, eid + ".json"),
          {"name": name, "icon": icon, "category": f"exofall:{c}", "sortnum": sort, "pages": pages})
    # the book's recipe (only loads when Patchouli is installed)
    w(os.path.join(RES, "data/exofall/recipe/guide_book.json"), {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"item": "minecraft:flint"}],
        "result": {"id": "patchouli:guide_book", "count": 1, "components": {"patchouli:book": "exofall:guide"}}})
    print(f"guide: {len(CATS)} categories, {len(ENTRIES)} entries, {sum(len(e[4]) for e in ENTRIES)} pages")

main()
