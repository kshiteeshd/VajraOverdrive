# Vitala (and Atala) redesign: the bestiary and its brains, Bhairava, the river, the look

*Written after a walk through every Vitala region and the ruler's fight. The findings come first, then the plan.*

## 0. What was wrong (found by looking)
- **Bhairava died unexpectedly.** His hull fell in one continuous slide: a burst (an astra, the Pashupata beam, a rank-III heavy weapon) could skip a whole phase, and a ruler at 3% still yielded the moment he touched zero. Phases were *read* from his hull, never *gated*.
- **His brain was thin.** Two to four acts in a fixed order (charge, fan, rain, eye), hovering on an ellipse over the hearth, no reading of the pilot, no reaction to being hurt, no pose before a blow.
- **The smoke was a screen of 150 soft discs** turning on circles: pretty as a still, wrong as a motion. It did not react to anything.
- **The river** started only at the end of the Plunge (a gold stripe appearing from nowhere), was a flat band with a few chevrons slid over it, and had nothing in it.
- **Both lokas look like game assets**: the stepped pixel walls with a bright orange rim, a visible grid and mosaic lines in the floor, props in flat saturated colour. The caverns are rectangles.
- **Fire hazards appeared without a reading** (a hairline igniting, an ember patch, a vent breathing) so the "trial of fire" felt random.
- The HUD called Vitala "ATALA LOKA" for its first band.

## 1. The bestiary and a brain for each
Every brain is the same four-part shape: **notice** (a wake radius, and it tells you with a sound and a flare), **position** (where it likes to be, as a role), **act** (a short list of attacks, each with a *telegraph* a pilot can read, a *commitment* and a *recovery* in which it is open), **react** (what it does when hurt, when its leader falls, when its mates fall). Nothing fires without a tell; nothing flees (Vitala's units close ranks or turn on the hull, see VITALA_ENEMIES.md).

| Unit (kind) | Role in a fight | Notice | Position | Acts (tell -> commit -> open) | React |
|---|---|---|---|---|---|
| **Agneya**, the Ember (VAKRA) | the striker | wakes at 700 px with a flare | circles the hull at ~340 px, in its leader's formation | **Dart run**: a gold line drawn to the hull, 0.5 s, then a straight fast run leaving burning ground; open for 0.8 s after. **Ember bolts** between runs (a leader fires a fan) | at 30% hull it runs twice as often; leader falls -> enraged |
| **Jvala**, the Cinder (MAYAVI) | the area denier | wakes at 900 px | hangs back, 480 px, keeps behind its allies | **Seed the ground**: a ring marker where the hull is *going* (velocity-led), then a patch for 7 s; never more than 4 patches of its own | when approached, lays three patches between itself and the hull and backs off |
| **Tamra**, the Ore (BHAVI) | the shield wall | wakes at 600 px | steps in front of the nearest shooter, always on the line to the hull | **Ram** (a low hum, a 0.4 s lean, a shoulder), **Slam** (a ring when touched) | front plate rings off bolts; only its rear and sides take damage; wounded -> berserk ram |
| **Kila**, the Nail (LANCE) | the sniper | wakes at 1000 px | holds a far arc at ~540 px | **Needle burst**: a hairline laser tell for 0.7 s along the aim, then three needles; open 1.0 s | a hit spoils the next shot's aim; it repositions to the far side |
| **Dhuma**, the Smoke (AURIS) | the flanker | wakes at 800 px | slips close at 300 px | **Fade and return**: goes smoke (untargetable) for 1.5 s, drifts to a point beside the hull, shows a flash, then two bolts | a hit while visible makes it fade at once |
| **Rekha**, the Line (HATAKA) | the controller | wakes at 800 px | parks wide, draws lines at the leader's angle | **Draw, wait, ignite**: the line grows (0.7 s), stands as a gold thread (1 s), fires (0.45 s); several draw parallel lines (a grid) | half hull: the turncoat ring |
| **Court elites** (gold, summoned by Bhairava) | his hands | called with a flare | in his formation | the leader's formation acts (wedge, pincer, ring) | enrage when he falls |
| **Keepers**: Dvarapala (gate), Dipaka (lamp), Vanija (bay) | hold a post | wake close | never leave the post | Dvarapala: slow shell; Dipaka: mends mates, sparks; Vanija: toll shots | never flee |
| **Bhairava** | the ruler | wakes on the crossing | rules the hearth | see section 2 | see section 2 |

**The shared brain** (one place, `VitalaBrain`): a *threat model* each unit keeps of the pilot (position, velocity, the aim the hull holds, whether the hull is shielding or boosting), so tells are aimed *ahead* of the hull; a *telegraph registry* (every danger spawns a visible and audible tell with a duration); an *engage budget* per band (no more than 4 units in their commit at once: the rest orbit and tease), so crowds stay readable.

## 2. Bhairava, Herald of Cinders
**Gates, not slopes.** His hull is divided into four phases at 75%, 50% and 25%. At each threshold his hull *stops* (damage past it is lost), he is untouchable for a beat, and the phase *turns*: a pose, a roar that shoves the hull, a word, a change of the hall. Nothing can skip a phase. A single blow can take at most 6% of his hull. The last phase ends in a yield, not a death.

| Phase | Name | Hull | What changes in the hall | Acts |
|---|---|---|---|---|
| I | **Stoking** | 100-75% | the ash ring wide | *Ash charge* (rears, flares, runs the line through the hull, ash behind); *ember fan* x2; *hearth burst* (a ring of twelve); *ash prowl* (circles slowly, dropping ash) |
| II | **Kindling** | 75-50% | the rim vents open; the court is called | + *cinder rain* (a closing ring), *gold spit* (three arcing globs that leave patches), the **ash cloud** (a fluid smoke simulation: it slows every hull and he is hard to see in it, but never untargetable) |
| III | **The Flood** | 50-25% | the Hataki floods the hall and carries every hull east | + the *third eye* (a long tell, a sweeping beam), *pillars of fire* (marked, then erupting at the hull's next positions) |
| IV | **The Last Fire** | 25-0% | the ring closes to its smallest; the hall is lit only by the fire | all of it, faster and in chains; the *immolation* (he draws the ash in, the hall goes dark but for his eye, then a nova of rings with a gap to fly through); then the yield |

**How he reads the pilot.** He keeps a model of the hull: if it runs, he leads shots; if it stays still, he places ash under it; if it hides behind a pillar, he sweeps the eye. He never repeats an act twice in a row. He answers being hurt: a sharp hit makes him flinch (a pose), a long barrage makes him *enrage the next act*. A hull that keeps its distance is punished by the fan; one that hugs him by the ring.

**How he is alive** (the animation, procedural on the sprite): a slow breathing swell and a hover sway in every phase; he *leans into his movement*; a **wind-up** before each act (a rear-back and squash, 0.4-0.7 s, sounded), a **stretch** in a charge with a trail of afterimages, a **roar** at each phase turn (a pop, a shake, a shockwave ring), a **cast** pose with the eye flaring, a **flinch** on a heavy hit, a **stagger** between phases; embers stream from his shoulders and the eye; the aura deepens by phase (gold, ember, white-hot, red).

**His bar.** One thin line (3 px), no pips, no notches; a hairline tick at each gate; the colour of the phase; an echo of the last hit that drains behind it.

## 3. The smoke, as a simulation
A real 2D stable-fluids grid (96 x 96 over the cloud's bounds): density and a velocity field, semi-Lagrangian advection, a pressure projection, vorticity confinement so it curls. It is *fed* by Bhairava (a burst at the cloud's start, a stream from his motion), *stirred* by every hull and round that moves through it (the pilot's wake cuts a channel in it), and it *drains* when the cloud ends. Drawn as one soft-edged texture, grey and warm at the fire's edge. The mandala at its heart stays.

## 4. The Hataki, born from the unknown
- The river exists from the **first screen** of Vitala: it comes up out of a dark mouth a short way in front of where the hull arrives, a well of black water-like gold with mist rolling off it, narrow at its source and widening over the first 500 px.
- It is **liquid, with things in it**: hundreds of moving motes (bright gold glints, darker slag flecks, little rising sparks) carried by the same flow the physics uses (faster in the middle and in the rapids, eddying at the bends); there is a gradient from a white-gold core to a dark, cooling edge; no chevrons.
- The carved cavern follows the river from its source, so the river never "begins" in empty floor.

## 5. The look: both lokas, simplified and made majestic
**Principle.** The lokas are *places the Purana praises*: bila-svarga, "heavens in holes", sunless, lit by jewels. So: darker, calmer floors with *one* idea each; big shapes, soft light, little noise; colour only where the lore puts it.

| | **Atala** (the lands of illusion and wind; sunless, jewel-lit) | **Vitala** (Hara-Bhava's gold and ash) |
|---|---|---|
| Floor | deep blue-black basalt with a faint teal strata and misted pools; no grid; the hood-gem stars overhead as a slow far light | black basalt dusted with ash; a warm black; the river the only gold |
| Walls | smooth, heavy stone masses with soft edges and a thin cool rim light; jewel veins (teal, violet) in the rock, not outlines | the same stone, ember-lit from below, gold veins only along the river; no bright orange outline |
| Air | mist banks at the floor, slow; far chimes of light | rising embers and ash flakes, heat shimmer near fire, smoke banks |
| Props | fewer, larger, in a restrained palette | fewer; the lamps and bays keep their own colours (they are the elements the eye should find) |

**Walls and placements.** The caverns are redrawn as *long galleries and wide halls* with columns and shelves (a spine of passage with side chambers) instead of rectangles; each band gets a composition: a gate with its two wards, a colonnade of sentries along one wall, an ambush pocket, a patrol along the river's banks, a court at a pool. Units stand where a pilot would *expect* guards and where the geometry gives cover, not on a scatter.

## 6. Fire that is read, not sprung
Every fire hazard now has a visible, audible tell and a fixed home: a **vent** shows a glow in its wall 1.2 s before it breathes (and breathes only in the bands that teach it); a **Rekha line** is a visible gold thread for a full second; an **ember patch** is born from a unit's visible act; **no hazard appears at a random place**. Hazards of units that are not awake are inert.

## 7. Order of work
1. Boss: gates, phases, the new brain, damage cap. 2. The bar and the animation. 3. The smoke. 4. The river. 5. The floors and walls. 6. The caverns and placements. 7. The fire's tells. 8. A walk of every band and the fight, with the arena.

---

## 8. What has been built (status)

| Part | State |
|---|---|
| Bhairava: four gated phases, a damage cap per blow and a refilling bucket (a burst cannot empty him), a brain that reads the hull and never repeats an act, ground marked before it burns, an immolation with a gap to fly through | done (`BhairavaBrain`, `Enemy.hurt`); `BhairavaGatesTest` |
| His poses and aura (breathing, leaning, wind-up, stretch with afterimages, roar, cast, stagger, flinch; embers off the shoulders and eye) and the thin bar with a hairline per gate | done (`BossPose`, `EnemyImages.drawRuler`, `WorldRenderer`) |
| The smoke as a fluid simulation, stirred by hulls and bodies, in the world's time | done (`world/Smoke`, `AshFx`); `SmokeTest` |
| The Hataki: it exists from the first screen, is born out of a black well with mist, and is full of moving glints, sparks and slag | done (`Hataki`, `RiverFx`, `GroundMap`) |
| Vitala's nine creatures, round, each with an idle loop and three action clips done now and then, out of step from unit to unit | done (`tools/enemy_art/roundkit.py`, `vitala_round.py`, `EnemyImages.cellV`); `VitalaRoundArtTest` |
| Floors: no grid, no slabs, no speckle; calm mottled stone; each loka graded by its own light (Atala cool and jewel-lit, Vitala ash and basalt with the river its one gold) | done (`GroundMap`) |
| Walls: Atala's ivory read as pale stone, its gold worn to bronze; Vitala's basalt with dull ember seams instead of a bright outline; soft filtering | done (`TileImages`) |
| Fire read, not sprung: vents are a stream of soft fire with a low red warning glow; patches cover rather than add up to white | done (`VentImages`, `HazardFx`) |
| Gate-wardens at three gates; the HUD says VITALA | done |

**Not yet:** a hand-built redrawing of the cave outlines themselves (the rock still has the tile grid's silhouette: the colouring and light are new, the shapes are the old ones), columns and architecture as props, a full re-placement of every court, the same round-and-alive treatment for Atala's creatures and the keepers' Atala sheets, and the dedicated brains of section 1 for Vitala's six units beyond what `VitalaBrain` already does (their telegraphs and formations exist; the *threat model* and the *engage budget* are not built).
