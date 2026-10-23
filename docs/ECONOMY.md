# The economy, hull scaling, and weapon scaling

One design for the seven-loka game. The numbers below are in code (`ShipDef`, `WeaponDef`,
`Hangar`, `LokaTable`) and pinned by `EconomyTest`; change a number there and the test says what
you broke.

## 1. What a pilot earns

A thoroughly cleared Atala pays about **1,250 coins**: ~227 enemies at 2 / 4 / 6 coins by rank
(plus a few shards), five hidden caches at 100, the ruler's purse of 150. Every payment is
multiplied by `sqrt(hpScale)` of the loka, so deeper lokas pay more but not as fast as they hurt:

| loka | hp x | fire x | clear pays | a pilot keeps (65%) |
|---|---|---|---|---|
| Atala | 1.00 | 1.00 | ~1,250 | ~810 |
| Vitala | 1.35 | 1.20 | ~1,450 | ~940 |
| Sutala | 1.75 | 1.45 | ~1,650 | ~1,070 |
| Talatala | 2.30 | 1.70 | ~1,900 | ~1,235 |
| Mahitala | 3.00 | 2.00 | ~2,170 | ~1,410 |
| Rasatala | 4.00 | 2.35 | ~2,500 | ~1,625 |
| Patala | 5.50 | 2.80 | ~2,930 | ~1,900 |

Cache and ruler purses now follow the same `sqrt(hpScale)` as drops, so they stay the same share of a
loka's pay. **Everything that can be bought, in total, costs about 23,000 coins; a full-clear run of
all seven lokas pays about 13,850.** So a pilot affords roughly 40-60% of the whole hangar: a
build, not a checklist.

## 2. Hulls - one per loka, each a role

Mk-1 is the hull you wake in. One more hull opens per loka (`LokaTable.unlockShip`), and a hull is
priced so that hull + x.2 + x.3 is roughly one loka's keep (`EconomyTest` allows 0.6x-2.6x).

| hull | role | opens | price | x.2 | x.3 (+shards) | hull pts | notes |
|---|---|---|---|---|---|---|---|
| Mk-1 Vajra | all-rounder | start | - | 120 | 300 | 100 | the baseline |
| Mk-2 Kite | scout: fast, thin | Atala | 260 | 140 | 300 (+3) | 90 | speed 435, turn 200 |
| Mk-3 Aegis | the wall | Vitala | 520 | 260 | 520 (+4) | 170 | 10% mitigation |
| Mk-4 Hive | fire rate | Sutala | 700 | 350 | 700 (+4) | 140 | x.2 reload x0.80 |
| Mk-5 Lance | precision | Talatala | 900 | 450 | 900 (+5) | 150 | x.2 damage x1.30 |
| Mk-6 Mortar | slow, heavy | Mahitala | 1,150 | 580 | 1,150 (+5) | 230 | 8% mitigation |
| Mk-7 Vajra-7 | the best of them | Rasatala | 1,500 | 750 | 1,500 (+5) | 240 | 5% mitigation |

- **x.3 needs x.2**, and takes shards: `3 + minLoka/2` of any element. A tier you have not bought is
  a locked silhouette in the bay - nobody sees what a ship or its upgrade looks like until it is theirs.
- **Hull growth keeps pace with fire.** Effective hull points go from 100 to ~253 (2.5x) against
  enemy fire that goes 1.0x to 2.8x; the rest of the gap is skill and the other two roads below.

## 3. Weapons - three roads to power

Enemy hulls climb 5.5x by Patala. A pilot's damage climbs the same road three ways, each opened by
a different loka:

1. **A better pattern.** Fifteen patterns, offered five / three / two / two / two / one / none by
   loka (Atala: Single Bolt free, Light MG 80, Double 90, Buckshot 110, Burst 120; Vitala: Aim 160,
   Piercer 170, Twin Burst 200; Sutala: Heavy 240, Wallbore 260; Talatala: Missile 340, Charge 320;
   Mahitala: Bomb 380, Laser 460; Rasatala: Tracking 560). A pattern the pilot's loka does not offer
   yet is a lock on the wheel.
2. **Rank.** Each owned pattern can be *tempered*: **rank II** (60% of the price, never under 60c)
   hits **25% harder**; **rank III** (130% of the price, never under 150c, plus **2 shards**) hits
   **50% harder and gains one ability**. ENTER on the pattern already in the guns buys the next rank.
3. **A hull tier.** Damage tiers on Kite, Hive, Lance, Mortar and Vajra-7 add x1.25-x1.35.

Rank III abilities - always a trait the pattern did not have, from flags every weapon already uses:

| pattern | ability | pattern | ability |
|---|---|---|---|
| Single Bolt | true bolt (aim assist 12 deg) | Heavy Shell | concussive (splash 55) |
| Double Bolt | lancing pair (pierces enemies) | Light MG | seeking stream (homing) |
| Burst | chasing burst (homing) | Charge Shot | piercing charge |
| Twin Burst | reads the gap (aim assist 14 deg) | Laser | long beam (+300 range) |
| Buckshot | shredder (pierces enemies) | Bomb | wide blast (+45 splash) |
| Missile | hunter (+120 homing) | Tracking Missiles | a third head (+1 missile) |
| Aim Shot | bends to its mark (homing) | Piercer | wall-piercer |
| Wallbore | total bore (pierces enemies) | | |

**Does it keep pace?** `EconomyTest.aPilotsBestDamageKeepsPaceWithTheEnemysHull`: best offered
pattern at rank III with a damage tier, over the starter's damage per second, must be at least 70% of
that loka's hp multiplier. It is, loka by loka.

## 4. The astras

Astras are not bought with coins. Five shards of a loka's own element forge that loka's Astra once, five
more buy each release. You carry every Astra on; `X` picks the one `F` spends. A loka's shards are the
only road to its Astra, which is why you hurry toward the loka whose element you are short of.

## 5. What to tune first when playing

1. `World`'s hostile quota and the gaze stages (`GAZE_NOTICED`, `GAZE_WATCHING`).
2. The 65% keep rate in `EconomyTest` - if real pilots keep 80%, raise hull prices ~20%.
3. Rank price factors (0.6 / 1.3) if tempering outcompetes buying the next pattern.
4. Shard drop rates (`LootSystem.LOOT_SHARD`) - rank III and x.3 both draw on them.
