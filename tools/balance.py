#!/usr/bin/env python3
"""Exofall balance sheet: every mob, boss move and weapon against the player Exofall is built for.

Exofall arrives in Sovereign 202 at late Act 3 / Act 4 and is an ATTRITION mod: no single bug should kill a geared
player, but the meteors keep sending them, so the fights grind you down and feel slightly overwhelming even in the
right gear. This file holds the numbers the code uses (keep them in sync) and prints what they mean in play.

    python3 tools/balance.py

Damage maths is vanilla 1.21: armour  = clamp(armor - dmg / (2 + toughness / 4), armor / 5, 20) / 25 off the hit,
then Protection EPF (capped at 20) / 25 off what's left.
"""

# ---------------------------------------------------------------- reference players
# ENTERING: what a late-Act-3 S202 player has on arrival: netherite-class armour (20 / 12), Protection IV-ish
# total (EPF 12, S202's Apothic enchanting is nerfed), ~35 hearts of the 50-heart budget, and a late BMM weapon
# (Aeternium / Flaming Ruby / Forgotten tiers: 13.6-16 base DPS) hitting ~13.5 per swing at ~1.3 swings/s.
# GEARED: the same player in a meteor armour set (Reinforced Carapace numbers) with a legendary.
PLAYERS = {
    "entering": dict(armor=20, tough=12, epf=12, hp=70, hit=13.5, rate=1.3),
    "geared":   dict(armor=22, tough=14, epf=12, hp=80, hit=15.0, rate=1.3),
}

def taken(dmg, p):
    a = min(max(p["armor"] - dmg / (2 + p["tough"] / 4), p["armor"] / 5), 20)
    after = dmg * (1 - a / 25)
    return after * (1 - min(p["epf"], 20) / 25)

def dealt(hit, armor, tough=0):
    a = min(max(armor - hit / (2 + tough / 4), armor / 5), 20)
    return hit * (1 - a / 25)

# ---------------------------------------------------------------- targets (per hit, % of the entering player's health)
# fodder / swarm 1-2%      many at once, the chip that wears you down
# standard       3-5%      3-5 hits to kill; a pack of four hurts if you stand still
# elite          6-10%     telegraphed big hits, 6-9 hits to kill
# boss light     8-12%  /  boss heavy (telegraphed) 18-30%; a fight lasts 2-3 minutes
# Exofall mobs never one-shot: the worst telegraphed boss hit, enraged, stays under 40%.

# ---------------------------------------------------------------- lesser mobs: (role, health, melee, armour, special hits)
MOBS = {
    # swamp
    "bloodgnat":        ("fodder",   12,  6, 0, {}),
    "glasswing":        ("standard", 50, 20, 2, {}),                       # dive is its only attack, telegraphed
    "reedmantis":       ("standard", 60, 18, 4, {}),
    "bogspinner":       ("standard", 50, 12, 2, {"bite, webbed (x1.5)": 18, "silk glob": 6}),
    "mire_leech":       ("fodder",   24,  4, 0, {"drain / 2 s, latched": 6}),
    "divebell_beetle":  ("standard", 50, 14, 6, {}),
    "mudgrub":          ("prey",     16,  0, 0, {}),
    # hive
    "combwasp":         ("standard", 30, 10, 2, {}),                       # comes in 2-4, poisons
    "stinger":          ("elite",    70, 28, 4, {}),                       # 1.2 s wind-up
    "nursemaid":        ("support",  34,  0, 0, {"heal / 0.5 s": 5}),
    "waxmite":          ("fodder",   16,  7, 2, {}),
    "larva_cocoon":     ("support",  30,  0, 2, {}),
    "honeyguard":       ("elite",   100, 16, 10, {"wax shield": 60}),
    "propolis_spitter": ("standard", 44, 8, 2, {"propolis glob": 12}),
    # desert
    "sanddiver":        ("standard", 55, 18, 4, {}),
    "shieldback":       ("elite",   100, 20, 8, {}),
    "roller":           ("standard", 60, 12, 6, {"boulder": 24}),
    "antlion":          ("standard", 70, 20, 6, {}),
    "tomb_scorpion":    ("elite",    70, 12, 8, {"sting (wind-up)": 26}),
    "ember_beetle":     ("prey",     16,  0, 2, {}),
    "golden_scarab":    ("prey",     45,  0, 10, {}),
    # rainforest
    "bloommantis":      ("standard", 60, 18, 3, {}),
    "canopy_assassin":  ("standard", 45, 10, 2, {"drop from the ceiling (+bleed)": 24}),
    "pouncer":          ("standard", 45, 10, 2, {"leap": 20}),
    "twigwalker":       ("standard", 40, 12, 2, {}),
    "leafcutter":       ("fodder",   18,  8, 4, {}),                       # neutral, columns of 4-6
    "lantern_moth":     ("prey",      8,  0, 0, {}),
}

# ---------------------------------------------------------------- bosses: health, armour 12-14 / toughness 6, moves
BOSSES = {
    "waxen_queen":        (900, 10, {"wax glob x5-7 (each)": 20, "slam (radius 5.5)": 22, "buffet (radius 2.2)": 28,
                                     "dive (telegraphed)": 50}),
    "terror_of_the_bog":  (1000, 12, {"snap (cone)": 42, "mud spit": 26, "bite": 20, "grab (every 0.5 s, 3.5 s)": 8,
                                      "eruption (telegraphed)": 50}),
    "apex_mantis":        (950, 12, {"combo slash x2": 26, "combo finisher": 42, "leap slam": 36, "guard counter": 48,
                                     "camouflage reveal (telegraphed)": 55}),
    "dozen_legged_wrath": (1000, 14, {"bite (+Poison II)": 40, "body sweep (each)": 28, "charge (telegraphed)": 50,
                                      "burrow eruption (telegraphed)": 44}),
}
BOSS_UPTIME = 0.55   # share of a fight you can actually swing (it moves, guards, submerges, burrows)
ENRAGED = 1.15

# ---------------------------------------------------------------- legendary weapons (BMM rule: dmg = 17.6 * mod / speed)
# 17.6 base DPS = Flaming Ruby (16, S202's best BMM tier) + 10%: boss-made legendaries top the pack's crafted gear.
LEGENDARY_DPS = 17.6
WEAPONS = {   # name: (speed, budget modifier, damage)
    "quadblade_of_the_sands": (0.7, 1.00, 25.0),   # slow on hit
    "royal_grandhammer":      (0.5, 0.90, 32.0),   # 10% lifesteal = sustain, the strongest perk in an attrition mod
    "glasswing_scythe":       (1.2, 1.00, 14.5),   # launch (utility)
    "serrated_longsword":     (1.7, 0.90, 9.5),    # stacking bleed adds damage
}

def main():
    E, G = PLAYERS["entering"], PLAYERS["geared"]
    print(f"{'mob':18} {'role':9} {'hp':>4} {'hits to kill':>12} {'melee':>5} {'% hp/hit':>9}  specials")
    for name, (role, hp, melee, armor, specials) in MOBS.items():
        hits = hp / dealt(E["hit"], armor)
        pct = taken(melee, E) / E["hp"] * 100 if melee else 0
        sp = ", ".join(f"{k} {v} ({taken(v, E) / E['hp'] * 100:.0f}%)" for k, v in specials.items()
                       if "shield" not in k and "heal" not in k)
        print(f"{name:18} {role:9} {hp:4} {hits:12.1f} {melee:5} {pct:8.1f}%  {sp}")
    print()
    for name, (hp, armor, moves) in BOSSES.items():
        per_hit = dealt(E["hit"], armor, 6)
        secs = hp / (per_hit * E["rate"] * BOSS_UPTIME)
        print(f"{name}: {hp} hp, ~{secs / 60:.1f} min with a late BMM weapon (entering), "
              f"~{hp / (dealt(G['hit'], armor, 6) * G['rate'] * BOSS_UPTIME) / 60:.1f} min geared")
        for m, d in moves.items():
            e, g = taken(d, E) / E["hp"] * 100, taken(d * ENRAGED, E) / E["hp"] * 100
            print(f"    {m:34} {d:3} -> {e:4.1f}% of health ({g:4.1f}% enraged), geared {taken(d, G) / G['hp'] * 100:4.1f}%")
    print()
    for name, (speed, mod, dmg) in WEAPONS.items():
        want = LEGENDARY_DPS * mod / speed
        print(f"{name:24} {dmg:5.1f} dmg x {speed} = {dmg * speed:5.1f} DPS (rule says {want:.1f} dmg)")

if __name__ == "__main__":
    main()
