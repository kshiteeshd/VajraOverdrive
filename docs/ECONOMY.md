# The economy, the difficulty curve, and the plan for seven lokas

One design for the whole game. The numbers live in code - `world/Balance` (income and the in-loka ramp), `world/LokaTable` (hull and fire
multipliers), `ship/ShipDef` and `weapon/WeaponDef` (what is for sale) - and are pinned by `BalanceTest` and `EconomyTest`: change a number and
the test says what you broke. `BalanceTest` MEASURES the built lokas (it walks the generated world enemy by enemy and chest by chest) instead of
trusting a model, because the first model was wrong (below).

## 0. What was wrong, and what changed

- **Vitala's garrison was graded as Atala.** Enemies were scaled by the *deepest loka the pilot had reached*, and the static garrison is placed at
  world generation, when that is always Atala: Vitala's 341 units had hull x1.0 and fire x1.0, not x1.35 / x1.2. Every unit is now graded by the loka
  it **stands in** (`World.add`), and pays by it (`LootSystem`).
- **Income was assumed, not measured.** The old table said a Vitala clear pays ~1,450; measured, it paid ~4,280 (its 29 chests alone were ~2,650).
  Income is now a **budget per loka** (`Balance.gross`) split into shares, and each earner pays its share of it.
- **No grade inside a loka.** Atala's first band was as hard as its last. A loka now teaches before it tests (section 2).
- Chests, caches and the ruler's purse paid `sqrt(hull scale)` of a hand-picked number; they now pay their share of the loka's budget.

## 1. Income: a budget per loka

A **thorough clear** of loka `L` pays `gross(L) = 1700 x 1.28^L` coins. A **typical pilot** (most of it, not all) banks 45% of that.

| loka | hull x | fire x | thorough clear | typical pilot keeps | chests | caches | purse |
|---|---|---|---|---|---|---|---|
| Atala | 1.00 | 1.00 | 1,700 | 765 | 10 | 5 x 90 | 240 |
| Vitala | 1.35 | 1.20 | 2,176 | 979 | 19 | 5 x 115 | 305 |
| Sutala | 1.75 | 1.45 | 2,785 | 1,253 | 16 | 5 x 145 | 390 |
| Talatala | 2.30 | 1.70 | 3,565 | 1,604 | 16 | 5 x 185 | 500 |
| Mahitala | 3.00 | 2.00 | 4,563 | 2,054 | 16 | 5 x 235 | 640 |
| Rasatala | 4.00 | 2.35 | 5,841 | 2,629 | 16 | 5 x 305 | 820 |
| Patala | 5.50 | 2.80 | 7,477 | 3,365 | 16 | 5 x 390 | 1,045 |

Shares of a clear: **kills 34%** (2 / 4 / 6 coins by rank x `sqrt(hull scale)`, paid by `LootSystem`), **chests 26%** (spread over the chests the
loka is built with: `Balance.CHESTS`), **caches 26%** (five, 100 -> 390), **ruler's purse 14%**. Measured today (`BalanceTest`): Atala 1,652 of
1,700 (x0.97), Vitala 2,322 of 2,176 (x1.07).

**Shards and coins.** A shard sells for 20 coins (`Hangar.SHARD_SELL`) but is worth more as an astra release or a rank III / x.3 tier, so the pilot
is meant to *keep* them. Chests pay shards too (Atala 3, Vitala 5).

## 2. Difficulty: three layers

1. **The loka's grade** (`LokaTable`): hull x1.0 / 1.35 / 1.75 / 2.30 / 3.0 / 4.0 / 5.5 (about x1.3 a loka), fire x1.0 / 1.2 / 1.45 / 1.7 / 2.0 / 2.35 / 2.8
   (about x1.2 a loka). Fire grows slower than hulls on purpose: the enemy gets tougher faster than it gets deadly, so bullets stay readable.
2. **The band's grade** (`Balance.ramp`): inside a loka the hull runs x0.88 at its first band to x1.12 at its last; fire runs half as far (x0.94 ->
   x1.06). The average of a loka is still its table number. `BalanceTest` pins that difficulty rises band by band **and never steps back at a loka's
   door** (Atala's last band 1.12 < Vitala's first 1.19).
3. **Rank** (`Enemy.rankScale/RANK_DAMAGE`): leaders and elites are bigger bodies and fire x1.45 / x1.9 in their courts, in every loka.

The pilot answers with three roads of power (section 4), priced so a typical pilot affords a *build*, not everything.

## 3. What is for sale

`BalanceTest.thePlanForTheSevenLokasHolds`: a typical pilot banks about **12,650 coins** over seven lokas against **23,450** of things to buy
(**54%**). Prices stay the hand-tuned ladder below; the test fails if either side drifts out of 40-80%.

| hull | role | opens | price | x.2 | x.3 (+shards) | hull pts |
|---|---|---|---|---|---|---|
| Mk-1 Vajra | all-rounder | start | - | 120 | 300 | 100 |
| Mk-2 Kite | scout | Atala | 260 | 140 | 300 (+3) | 90 |
| Mk-3 Aegis | the wall (10% mitigation) | Vitala | 520 | 260 | 520 (+4) | 170 |
| Mk-4 Hive | fire rate | Sutala | 700 | 350 | 700 (+4) | 140 |
| Mk-5 Lance | precision | Talatala | 900 | 450 | 900 (+5) | 150 |
| Mk-6 Mortar | slow, heavy | Mahitala | 1,150 | 580 | 1,150 (+5) | 230 |
| Mk-7 Vajra-7 | the best | Rasatala | 1,500 | 750 | 1,500 (+5) | 240 |

A hull with both tiers costs about one loka's *typical keep* (`EconomyTest`: 0.6x - 2.6x). Weapons: 15 patterns, offered 5 / 3 / 2 / 2 / 2 / 1 / 0 by loka,
80 -> 560 coins, tempered to rank II (60% of the price, +25% damage) and III (130% + 2 shards, +50% damage and an ability).

## 4. Power keeps pace

Enemy hulls climb x5.5 by Patala. A pilot's damage climbs the same road three ways: a better **pattern**, a higher **rank**, a **hull tier** (x1.25-1.35).
`EconomyTest.aPilotsBestDamageKeepsPaceWithTheEnemysHull`: best offered pattern at rank III with a damage tier, over the starter's damage per second,
is at least 70% of the loka's hull multiplier. Hull points climb 100 -> ~250 against fire x2.8.

## 5. The astras

Not bought with coins: five shards of a loka's own element forge its Astra, five more buy each release (`world/Astra`).

## 6. Checklist for building a new loka (Sutala next)

1. A `LokaTable` row exists (hull / fire grade, ruler, shard element, hull that opens, seed).
2. Pick its **content**: enemies, chests (`Balance.CHESTS[l]`), five caches, a ruler. Kill coins at the loka's pay should come to **34%** of
   `Balance.gross(l)` (BalanceTest tolerates +-30% on the share, +-18% on the total).
3. Give it six bands in the region list and make sure `Balance.BAND_FIRST/BAND_LAST` name them (they are already planned: 12-17, 18-23, ...).
4. Add the loka to `BalanceTest.measure` (it measures the built lokas; today two) and run it. If it is rich, cut chest coins by adding chests or
   lowering a share; if poor, add a chest or two. Never raise a single payout without touching the budget.
5. Its hull (`ShipDef.minLoka`) and patterns (`WeaponDef.minLoka`) are offered when the pilot reaches it.

## 7. What to tune first when playing

1. `Balance.GROSS_BASE` / `GROWTH` (how rich a clear is), `TYPICAL` (how much of it a real pilot banks).
2. `Balance.RAMP_LOW/HIGH` (how steep a loka is inside).
3. Rank price factors (0.6 / 1.3) if tempering outcompetes buying the next pattern.
4. Shard drop rates (`LootSystem.LOOT_SHARD`): rank III and x.3 both draw on them.
