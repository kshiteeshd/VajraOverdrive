# Vitala - the second loka (PLAN, nothing built yet)

> Status: **built**, and changed from the first plan after the pilot played it:
>
> - **One continuous world.** Vitala is not a separate world you are carried to: it stands **on the roof of Atala** (`World.CAMPAIGN`, 47,600 px tall;
>   Atala 0-20,600, Vitala 20,600-47,600). Bala's gold door at the top of Atala opens straight into the Plunge; you fly up, and you can fly back down.
>   The first plan's "swap the world" (`descend`) is gone, and with it the replayed opening scene.
> - **Bigger, and still ten thousand yojanas.** Vitala is 5,200 x 27,000 px, the same width as Atala and 1.3x its length; its yojana meter runs 0-10,000 over
>   its own 27,000 px (`Hataki.YOJANA_PX = 2.7`; Atala's runs 0-10,000 over its 19,000 at 1.9).
> - **The pockets mean something.** About thirty side caverns, each a **vault** (a treasure chest), a **den** (a chest guarded by an ambush) or a **well** (mends the
>   hull once): `docs/TREASURE_CHESTS.md`.
> - **A second ruler, a second stage.** Bala's fall lifts his seal and makes **Bhairava** (the original bestiary's Herald of Cinders) the quota's business
>   (`World.stage`, `beginStage`); his fall ends the run.
> - **A new brain.** Vitala's units fly formations and attack in patterns with the characters of the original bestiary, rank kept (`docs/VITALA_ENEMIES.md`).
>
> | step | status | where |
> |---|---|---|
> | V1 loka as data | done (superseded by the one-world design) | `World(seed, CAMPAIGN)`, `World.stage`, `World.vitala()`, `Biome.ALL` (12 rows), `Altitude` (one run of stops), `TileMap.skin` |
> | V2 shape | done, bigger | `Hataki` (world coordinates from `Y0`), `MapGen.vitala()` (161 columns, typed pockets, shop halls, a 900 x 780 Altar), `VitalaGen` (appends to Atala) |
> | V3 look | done | basalt tiles per map, both enemy and shrine palettes at once, ten tinted layers, gold river, glow, chevrons |
> | V4 mechanics | done (no layer columns, no heat effect yet) | current, vents, shrine hush, **burning ground and lines** (`Hazards`, `HazardFx`) |
> | V5 enemies | done, new designs and a new brain | six ships + redesigned boss; `VitalaBrain`, `BhairavaBrain` |
> | V6 places | done | shrine, Gold Assay, chests, wells |
> | V7 boss and flow | done | Bhairava (ash ring, ash charge, ember fan, cinder rain, third eye, the Flood) |
> | V8 polish | **done** (the Agni astra still open) | **the script**: `VITALA_ENTER`, the gaze scenes in Bhairava's words (`Scenes` reads `World.stage`), `BHAIRAVA_WAKES/FALLS`, two people (the Gold-Seller, Gauri's Maid: first-meeting scenes, sheets in `assets/npcs/vitala/`, five Codex pages); **flame columns** on layers 1-3 behind each vent and the river as a gold stripe on layers 1-4 (`VentImages.columns`, `WorldRenderer.forgeLayer`); **heat** (`Ship.heated`: boost drains x0.7, guns cycle 10% faster with 3 degrees of scatter, the boost tube glows hot, a shrine's hush cools it); **the keepers' own sheets**; **chest markers** on the compass dial; **balance** (below) |

## 0. What the scriptures give us

Bhagavata Purana 5.24.17 (read in full for `WORLD_BIBLE.md`): Vitala is **below Atala**, 10,000 yojanas down (each lower loka lies 10,000 yojanas
beneath the last, and each is as wide as the earth). Its master is **Hara-Bhava - Lord Shiva - with Gauri**, the **master of gold mines**, with his
**ghostly associates (the bhutas)**. From the couple's union flows the river **Hataki**; **fire**, drinking it, spits it out as **hataka gold**, and
the demons of Vitala wear that gold as ornaments and live happily. Vishnu Purana: its soil is **black**. The game's own `LokaTable` row:
Vitala, ruler **Bhairava** (holder of the second piece of the Vajrayudha), element **Agni**, enemy hull x1.35, fire x1.2, opens the third hull.
`WORLD_PLAN.md` set the theme as "gold and fire": the Hataki river as a current, fire vents on a beat, Hara-Bhava's shrine as the place that goes quiet.

So the design is built on four facts: **a black stone world, a river of liquid gold, fire that smelts it, and a god's shrine in the middle of it.**

## 1. The shape: a descent, then a sideways world

- **The fall (10,000 yojanas).** Atala's boss arena ends in a gold door (the seal that lifts when Bala falls). Through it the hull does not fly on: it
  **falls** - a vertical shaft section, about 2,400 px, in which the layered walls rush upward past you, the stack stretches deep (the floor far
  below, lit by a gold glow growing out of the dark) and the ambient goes from Atala's ivory-dim to **ember-red**. No cutscene: the world does it, the
  hull steers in the shaft. It ends in a splash into the first gold pool.
- **Vitala itself is a SIDEWAYS world.** Hull enters at the **west** and works **east**. The band is about **16,000 px long and 3,200 px tall** (Atala
  is 5,200 wide and 20,600 tall): five bands left to right, one boss arena at the east end. It is a *place you explore*, not a corridor you climb: the
  Hataki river runs through all of it, there are side caverns off the river (a cache in each), and one optional deep branch.
- **Same depth feel as Atala:** the walls are still the PLAY layer, with **ten stacked layers** behind it, the raycast land far below as the floor. The
  difference is what the layers and the floor are made of (section 3).

## 2. The five bands (west to east) - same count as Atala's bands after the gate

| # | name | x range | the idea |
|---|---|---|---|
| 0 | **The Plunge** | the fall shaft, y-axis | falling through a shaft into the gold; the tutorial of the current |
| 1 | **Hataki Mouth** | 0-3,200 | where the river is born: broad shallow gold, wide cavern, the first forge-fires; the safe band |
| 2 | **The Smelters** | 3,200-6,800 | fire vents and crucibles: timed flames, a gold river that narrows into channels with walls of black basalt |
| 3 | **The Bhuta Court** | 6,800-10,800 | Hara-Bhava's domain: the ghost-lit hall, the **shrine** (checkpoint of the loka), the ghostly associates |
| 4 | **The Hearth Deep** | 10,800-14,000 | Bhairava's own fires: the river goes to rapids, the floor is a lake of fire, the walls are forge-works |
| 5 | **The Altar of Bhairava** | 14,000-16,000 | the boss arena: the sacrificial hearth; the river falls in a gold cataract into the Altar's well |

(The five bands plus the fall are "six regions", like Atala. Names may change; the order and count stay.)

## 3. How it looks (the biome row)

- **Floor and walls: black basalt, gold veins.** The wall tiles keep their shape but are re-skinned in a second palette: charcoal-black stone with
  molten-gold seams and ember-orange edge-light (a second tile sheet, generated by the same pipeline as the wall palette fix, `tools/fix_wall_tiles.py`).
  The flagstone floor of the raycast land is black basalt with a gold inlay grid; *gold river* sections are a flowing, bright, rippling surface.
- **Light:** ambient very dark **warm-black**; the light comes from fire and gold: lava-glow pools along walls, forge mouths, the river's own glow
  (it is emissive: the river lights the walls above it), embers rising. The hull's pool is amber here.
- **The stack:** ten layers like Atala, tinted toward **deep ember red**, with the river glowing on layer 1-4 as a gold stripe at its own world positions
  (the same trick as the checkpoint beacons), and heat shimmer (a slow wobble of the nearest layer, a tiny sine in x).
- **Fire as a depth cue:** columns of flame on the layers behind the play layer (flat sprites pinned to a layer, like the mirage wood's blossoms),
  so you see fire down the shaft long before the vent in front of you fires.
- **Props (stone outcrops become "slag mounds", mushrooms become "gold crucibles"):** the raycast floor's props are slag piles and cooling gold
  ingots; the PropField kinds are re-skinned per loka (kind -> palette), no new code per prop.

## 4. The river: the loka's mechanic (a current)

The Hataki is a **flowing region**: a polygon band along the river's centreline (a polyline with a width) with a **flow vector** (it runs east, faster in
rapids, slower in the pools). A hull in the river is *carried* by the flow (an added velocity, not a hard push: boost or thrust against it costs
more). Enemies are carried too; bullets are not. The river is **safe gold** in the Mouth and the Court, **boiling** (damage per second to anything in it
that is not boosting - the fire has drunk it) in the Smelters and the Hearth where it passes the vents.

Why it fits: gold carries the hull *east* - toward the boss - which turns "sideways exploring" into a rhythm: ride the current, leave it to
loot the side caverns and fight, ride on. Rapids in the Hearth Deep make the river a hazard to **leave** the other way (you cannot fly west).

## 5. Fire: the second mechanic (vents on a beat)

**Vents** are 2x2-tile holes in the floor-edge of the walls that fire a **column of flame** along an axis on a fixed clock (a 3 s beat: 1.2 s warning glow,
0.8 s flame, 1 s cool). Flame damages a hull (and enemy bodies, not bullets) and is drawn as an animated sprite sheet on the play layer with a matching
column on three layers behind it. A band's vents share one clock, so you learn the beat: **the Smelters teach it, the Hearth Deep stacks two clocks.**
Hull effect, per `WORLD_PLAN` section 6: **heat** - boost drains slower, weapon rate +10% with +spread; shown in the HUD as the boost tube glowing hotter.

## 6. Hara-Bhava's shrine (checkpoint) and the loka's shop

- **Checkpoint (one per band, the Court's is the big one):** the same deepa-stambha altar redone as a **trishula-lamp**: three flames on a trident of gold
  rising out of a black stone linga plinth with a ring of **ghost-lights** circling it (the bhutas, harmless, painted sprites: they bob and offer a
  flame to the hull when it is lit). Lit: the whole band goes **quiet for 6 s** (vents hold, the current slows to nothing) and your hull rests. This
  is the "shrine quiets the band" idea from `WORLD_PLAN`.
- **Shop: the Gold Assay.** Vanija's bay re-dressed: a black stone counter with gold scales, a **crucible** instead of a lotus berth - the ship seats into a
  dais of molten-gold glass; the tula's pans hold ingots and the coin hop becomes an ingot hop. Same footprint, same docking.
- **Both are animated sprite sheets in the second palette** (the `CheckpointImages` / `ShopImages` painters take a palette).

## 7. Enemies

Same seven roles as Atala (`WORLD_PLAN` section 8), same shapes (the sprites' silhouette contract), the **ember-and-gold palette** (pearl becomes
blackened bronze with gold trim; apertures glow orange; death = sparks; rage = white-hot). One **new role** for Vitala (every loka adds one, retires none):

- **BHUTA (the ghostly associate):** a pale blue-white wisp-skull that **phases through walls** and cannot be hit while inside one; it drifts at the hull
  and fires only in the open. Flanks a hull in the walls' blind side. Rank-1 **Bhuta-nayaka** calls reserves like any leader.
- The **reserves and flank-dives** from the depth system stay: here they **rise out of the river** (from under the gold, surfacing with a splash).
- Keepers redressed: Dvarapala -> **Nandi-like gate-bull** (a slow armoured bull-shape, the same hit box), Dipaka -> **fire-priest** (tends the shrine lamp
  and mends allies), Vanija -> **assayer** (runs the Gold Assay).
- Difficulty: `LokaTable` 1.35 hp / 1.2 fire, enemy bands ramped 0.85..1.25 inside the loka (the plan in `WORLD_PLAN` section 7).

## 8. The boss: Bhairava, Herald of Cinders (Lord of Burning Gold)

- **Fiction:** Hara at the cremation ground (the name and the ash-trail signature are the original bestiary's); not a villain - he has tended the fire so long the piece in his hand says the fire is his. "I have kept it burning a long time."
- **Arena (the Altar):** a round forge-hall; at its centre a great **hearth** (a lit well); three **braziers** on the rim; the Hataki falls in a cataract
  from the north into the well. The camera does not leave the arena; the layers behind are the hearth's flues.
- **Phases:** (1) **Stoking:** he stands at the hearth and throws gold-bolts in arcs while three braziers feed him; destroy a brazier to cut a source.
  (2) **Kindling:** he rides the river's current round the arena, leaving fire in its wake; vents open along the rim on the beat; he calls reserves out
  of the well. (3) **The Flood:** he opens the cataract - the arena floods with gold (a current that carries everything one way) and he fights from the
  flame; the weak point is the hearth, exposed in the last seconds of every flood.
- Falls: "Take it. It never made me a king."; the Hearth's flame goes white, the piece drops, the **Agni astra** is forged for the pilot's stock (the
  Agni shards are already paid by this loka).

## 9. Economy, shards, items, story

- Pays **Agni shards** (element 2, `LokaTable`); the **Agni astra** (the Pyre: bursts on impact, burns the crowd) becomes fully available.
- A new hull (the 3rd, Sutala/Vitala hull `unlockShip`) is on sale in the first Gold Assay.
- The loka's gaze quota/scenes: the same four stages (unaware/noticed/watching/waiting) with Bhairava's words (a script pass, `docs/SCRIPT.md`).
- **Two people** (NPCs, `docs/NPC_BIBLE.md` style): a **gold-seller** (a bhuta who trades hataka gold for shards) and **Gauri's maid** who gives the shrine lore;
  small scenes the first time they are met.

## 10. What changes in the code (the honest list)

This is the expensive part, so it is decided before building:

1. **A loka is data.** Introduce `LokaWorld` (`width, height, axis, bands[], gates[], biomes[]`) and let `World` be built FROM a loka. Atala keeps its numbers.
   The world today assumes **progress = y** in about **90 places** (44 uses of `regionStart`, 12 of `currentRegionIndex`, the gaze, the boss wake line, the
   altitude and stack functions, the biome lookup, the minimap's band name). The fix is one abstraction, `World.progress(x, y)` (y for a vertical loka, x for
   a sideways one) and `World.bandAt(x, y)`, then replacing the calls - mechanical but wide.
2. **Generation:** a new `WorldGen`/`MapGen` pass for a *horizontal river cavern* (a meandering tunnel along x with side caverns, the river polyline, vents,
   forge niches), reusing the tile maps and the guard/cache/shop/checkpoint placement (made axis-agnostic).
3. **Render:** the stack/floor already work in world coordinates, so they carry over; the biome rows get a Vitala set; the river is a new `FlowLayer`
   (a gold animated surface drawn on the play layer with beacon glows on layers 1-4); vents are `FlameSprites` (a sprite sheet + layer columns).
4. **Physics:** `World.currentAt(x, y)` returns the river's flow vector; `Ship` and `Enemy` add it to their velocity. One new field, one new term.
5. **Assets:** a Vitala wall/tile palette (a script over the existing wall tiles), the ember-and-gold enemy sheets (the enemy pipeline already takes a palette),
   shop/checkpoint palettes, the bhuta sprite sheet, the boss sprite sheet (Bala's own is still the older art, so Bhairava is new work either way).
6. **Flow:** the gate from Atala's arena opens to the Plunge; `GameScreen` swaps the `World` for the Vitala one and carries the hangar, score and astras
   (the carry rules in `WORLD_PLAN` section 2 already exist).
7. **Tests:** a flight test through all of Vitala's bands, the current's maths, the vent clock, biome continuity along x, the bhuta's phasing, the boss'
   phases, a screenshot per band, the frame-time guard.

## 11. Order of work (each step ends in screenshots and a green run)

| step | what | you see |
|---|---|---|
| V1 | `LokaWorld` + `progress()` refactor, Atala unchanged (all tests green) | nothing changes - the base |
| V2 | The Plunge (the fall shaft) and the sideways cavern generator with the river polyline | the shape of Vitala, empty, black and gold |
| V3 | Black-basalt palette (walls, floor), ember light, ten tinted layers, the river surface | the look |
| V4 | The current (flow) and the vents (clock, flame sprites, layer columns), heat hull effect | the two mechanics |
| V5 | Enemies in the ember palette, the bhuta role, rising out of the river | the roster |
| V6 | The shrine (checkpoint) and the Gold Assay (shop), animated, quiet-the-band | the places |
| V7 | Bhairava and the Altar, the Agni astra, the carry from Atala | the ending |
| V8 | NPCs, script, the gaze scenes, balance pass, speed pass | the polish |

## 12. Questions that need an answer before V1

1. **Sideways direction:** west to east (the plan) - yes? (The minimap, the compass and the gaze tape all read "down is deeper"; sideways needs a
   "progress" strip instead of the depth tape.)
2. **Size:** 16,000 x 3,200 (about 12 minutes of flight, plus side caverns) - right, or longer/shorter?
3. **Is the river current OK to be forced** (you cannot easily fly west in the rapids)? It makes the boss approach one-way; the alternative is a gentle current only.
4. **The Plunge:** a short steerable fall (plan) or a plain loading beat?

## Balance (measured, V8)

`VitalaBrainTest.theBandsHurtMoreAsTheyClimbAndNoneIsAWall` flies a passive pilot (no shooting, no dodging, no shield) up the river through every awake court,
three seeds, and counts the hulls lost in each band:

| band | hulls lost, passive pilot |
|---|---|
| The Plunge | 0.00 |
| Hataki Mouth | 0.78 (the gentle band) |
| The Smelters | 2.41 (vents, Agneya's dart runs, Tamra's walls) |
| The Bhuta Court | 3.04 (Rekha's grids, Dhuma's packs) |
| The Hearth Deep | 2.86 (everything at once - but the rapids carry you through it faster) |
| The Altar | the boss fight: a passive pilot sits in the ash and burns |

The test pins the shape (the forges hurt more than the Mouth, the Mouth under 1.5 hulls, no river band over 6) rather than the numbers, so a retune cannot
quietly turn a band into a wall.
