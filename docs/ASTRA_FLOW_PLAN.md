# Agni and Vayu as flows: the Pyre Bloom and the Tornado

Status: PLAN, with the art built (`tools/enemy_art/flowfx.py` -> `build/astra_wind.png`, `build/astra_fireflow.png`, `build/flowfx_preview.png`, and a simulated mock of both
releases, `build/flowfx_scene.png`). This replaces the earlier idea of Agni as a rain of fireballs: **Agni is now fire flowing out of the ship, Vayu a tornado**, and both
are built as one small *simulation* - particles on a flow field - rather than animations, so they react to the enemies and the walls.

## 0. What each one is, in one breath

- **AGNI - THE PYRE BLOOM.** When the mandala is complete and you let go, fire pours out of the hull in eighteen streams, like the reference drawing: each stream runs outward,
  its tip curling over into a hook, embers drifting between them. The streams lean toward the nearest living foe, so they curl into the enemies and burn them. What they
  touch is ignited (burns after the stream has passed), and the ground under it is left scorched.
- **VAYU - THE TORNADO.** Letting go starts a tornado round the ship in three beats. (1) **DRAW IN**: wind from everywhere spirals inward; enemies and their bullets are pulled
  into the funnel and carried round it. (2) **CUT**: while they are carried, the wind takes them **one by one**, nearest first, a crescent of cutting air at each. (3) **THROW**: at the eye the
  flow reverses and everything left - foes still standing, every enemy round, even the astra's own held shards - is thrown outward, away from the ship, hurting what it
  hits and slamming into walls.

## 1. The art (built)

32x32 cells, 8 frames a row (the size of every effect sprite), flat bands with a dither, every loop closes. Sprites point right (+x), root at the left; the game turns each one
to its heading.

| sheet | row | what | used by |
|---|---|---|---|
| `astra_fireflow` | STREAM | a flame stream, wide and hot at the root, ending in a curl | the 18 streams |
| | LICK | a shorter curved tongue | lesser streams between them |
| | EMBER | sparks with short trails | drift off the streams |
| | BLOOM | a round hot core with spikes of flame all round | the fire at the ship |
| | SCORCH | char with embers breathing in it | the ground a stream has crossed |
| `astra_wind` | GUST | a long curved streak of air with light running along it | the inflow and the outflow |
| | SWIRL | two spiral arms seen from above | the funnel's inner flow |
| | WISP | a small curl of wind | the curls at the edges |
| | DEBRIS | dust and leaves tumbling | grit carried in the air |
| | FUNNEL | three arms spiralling in to a dark eye | the tornado itself (drawn big, several scaled copies, turning) |
| | BLADE | a crescent of cutting air, a one-shot | the cut on each foe |

The Agni burn flame (on a burning foe) and the burst are the earlier `astra_agni` sheet; the patch is the fire sheet's GROUND row.

## 2. The simulation core (one class, used by both): `world/AstraFlow`

A **pooled particle system** on a **flow field**, stepped in the fixed 1/60 s world tick (so it is deterministic, testable and replayable on guests).

- **Particle**: position, heading, speed, age, life, size, a sprite row, a frame offset, a stream id, an owner seat. Pooled array of 700 (cap), no allocation per frame.
- **The field** is a function `v(p, t)` made of three parts that a rite sets each tick:
  - **radial**: out (Agni, Vayu's throw) or in (Vayu's draw) with a speed that depends on radius and the rite's phase;
  - **swirl**: a tangential speed, faster nearer the centre (a vortex), signed per stream (Agni) or per rite (Vayu);
  - **curl noise**: a smooth divergence-free wobble from a value-noise potential so no two streams run alike (seeded: the same run replays the same).
- **Attractors**: the nearest living foe within reach pulls a stream's heading toward it (Agni) - a gentle steering, so the fire curls onto enemies rather than homing like a missile.
- **Walls**: a particle that meets rock slides along it (its heading is projected on the wall) - the fire creeps round corners and the wind funnels along tunnels. This is
  what makes it feel like a simulation and not a picture.
- **Bodies in the flow** (enemies, enemy bullets): each frame the same field acts on them as a *force*: they are carried, not teleported (see sections 3 and 4). A mass per
  kind decides how much (Mayavi-class fodder drifts easily, heavy kinds resist, bosses take a fraction).
- **Rendering**: additive, sprite per particle, scaled by age, turned to heading; ~700 sprites is cheap on the existing batch, and the field itself is not drawn.

## 3. Agni, in steps (the Pyre Bloom)

Timeline after release (about 3 s in all, then the embers and scorch fade):

| time | what happens |
|---|---|
| 0.0 | the mandala snaps into the hull; BLOOM sprite flares at the ship; screen punch |
| 0.0 - 0.6 | the 18 stream emitters open one after another round the ship, each spawning STREAM particles every ~0.08 s at 140-200 px/s |
| 0.6 - 2.2 | full bloom: the streams bend (swirl + curl noise) and toward foes; the tip of each ages into the hook |
| 2.2 - 3.0 | emitters close in the same order, streams thin out, EMBERs and SCORCH remain |

Rules:
1. **Reach** - streams run out to about 650 px (a screen), a little less than the Pashupata beam, so Agni covers a circle and Pashupata covers a line.
2. **Ignite**: a particle within 14 px (+ the foe's radius) of a foe ignites it: burning for 3 s at 14 damage a second (a second touch refreshes, never stacks). A burning foe wears the
   BURN flame sprite and gets 40% panic: it breaks formation (the Vitala brains read `burning`).
3. **Contact damage**: 30 damage a second while a stream is on it, so a foe in the thick of it dies fast and one at the edge only burns.
4. **Spread**: a foe that dies burning ignites the foes within 100 px of it.
5. **Ground**: where streams pass, a SCORCH sprite is laid (and a Vitala `Hazards` patch for 4 s) that keeps burning foes in it; it never hurts the pilot.
6. **Walls and armour**: fire creeps along rock; plate takes full burn (fire ignores it) but not the contact damage.
7. **Pilot's gain**: foes killed while burning drop one extra coin.
8. **Grandeur**: streams = 18 at full strength; the charge fraction sets how many open and how long they live (a short invocation is a small bloom). The loka scales the damage as the
   other astras do (`Balance`).

## 4. Vayu, in steps (the Tornado)

| time | beat | what happens |
|---|---|---|
| 0.0 - 1.6 | **DRAW IN** | the field turns inward: GUSTs spawn at 110-160 px and spiral to the eye, faster as the beat goes on; the FUNNEL sprite fades in and turns; foes and enemy bullets within 650 px are pulled: each gets an inward force and a tangential one, so they are carried **round** the funnel, spiralling closer, not dragged in a line |
| 0.55 - 1.45 | **CUT** | every 0.2 s the wind takes the nearest caught foe: a BLADE crescent flashes across it, it takes 55% of its health (kills fodder), and the next one follows. "One by one" - the pilot reads the order. |
| 1.6 - 2.0 | **EYE** | everything is at the centre of the funnel: a held breath, the flow nearly stops, the field contracts, a ring of light |
| 2.0 - 3.0 | **THROW** | the field reverses and strengthens: GUSTs burst outward from the hull; every foe still caught is thrown outward at 600+ px/s and takes impact damage on a wall (+50% if it hits rock); every enemy bullet in the funnel is blown outward and away (it no longer hurts the pilot); the astra's own held shards (the bullets round the mandala) stream outward with the wind |

Rules:
1. **Mass**: foes are pulled by `force / mass`: fodder is carried, heavier kinds are slowed, a boss is only shoved (it cannot be cut, but is thrown a little and takes the throw damage).
2. **Bullets**: enemy rounds in range are pulled in (so they cannot hit while it runs), spun, then thrown outward in the throw beat - the pilot is *safe inside the storm*.
3. **The pilot** is not pulled (the eye is the ship); the nose still aims, and the pilot's own weapons fire as normal inside it.
4. **Boost**: the astra refills the boost at the release, as now.
5. **Grandeur**: radius 450 -> 650 px by charge; Vayu lives at the fifth loka, so its tornado is the largest of the elementals.

## 5. Steps of work, in order (each ends with a green build and a picture)

| # | step | done when |
|---|---|---|
| 1 | **Sprite sheets** (this file's art) | **built**; previews and the mocked scene exist |
| 2 | **`AstraFlow` core**: pooled particles, the field (radial, swirl, curl noise), walls, a seeded step, and tests for each (a particle runs outward; curls over with age; slides along a wall; the same seed replays) | tests green |
| 3 | **Bodies in the flow**: the force on enemies and enemy bullets, mass per kind, carried-not-teleported, a `caught` state on an enemy | a caught foe spirals in; a heavy one lags; a bullet is flung out |
| 4 | **Agni rite**: emitters, steering to foes, ignite and burn (`Enemy.burning`), spread, scorch patches, coin on burning kills | a screenshot like the reference drawing; a crowd burns |
| 5 | **Vayu rite**: draw in, cut one by one, eye, throw, wall slam, bullets blown out, the held shards streaming out | a screenshot per beat; foes die in distance order |
| 6 | **Wire into the astras**: the release now starts the rite instead of the cube volley for these two (cost, cooldown, cancel on death, the HUD dial and mandala unchanged) | a hangar test: pick Agni or Vayu, hold, let go |
| 7 | **Co-op**: the host runs the flow; the wire sends kind, power and a seed; guests replay the particles from the seed and only draw (damage stays on the host) | a two-seat test |
| 8 | **Tuning and perf**: particle cap, culling off-screen, frame-time guard in `GameFrameProbeTest`, damage scaling by loka in `BalanceTest` | frame stays under 16 ms with a full bloom |

### Built (steps 2-6)
`world/AstraFlow` (pooled particles on a flow field, walls, Agni and Vayu rites), `Enemy.caught/burnT`, `Enemy.hurtPiercing`, `screen/FlowFx` (sprites) and the funnel in `AstraFx`.
Agni: 18 emitters open in order, streams run to 650 px, curl and lean toward foes, ignite (14 dps for 3 s, refreshed not stacked), contact damage 30 dps, a foe that dies burning
ignites neighbours within 100 px and pays a coin, scorch is left. Vayu: foes within 450-650 px are caught and carried round (heavier lag, bosses are not caught), cut one by one nearest
first from 0.55 s every 0.2 s for 55% of health, enemy rounds are drawn in harmless, then the throw (foes outward, +37.5% impact on a wall, rounds blown away, the held shards stream
out as seeking bolts). Guests' releases start the same flow on the hub from their hull. Not yet: foe panic while burning, scorch as a Hazards patch, a boss being shoved, other
seats seeing a guest's particles (the hub's damage is right; the picture is the hub's), and a frame-time guard with a full bloom.

## 6. Tests I will write

Agni: eighteen emitters open in order; a stream reaches 650 px; a foe within the stream ignites and burns 3 s; a second touch refreshes; a dying burning foe ignites a neighbour; a
particle slides along a wall; scorch is left; the pilot is never hurt. Vayu: a foe within 650 px is pulled and *orbits* (its angle changes as its radius shrinks); mass changes the
pull; the cut order is by distance, one every 0.2 s; an enemy bullet is flung out and does not hit the pilot; the throw sends survivors outward and a wall hit hurts more; a boss is
never cut. Both: deterministic from a seed; the particle count never exceeds the cap.

## 7. Questions

1. **Agni reach**: 650 px (a screen) as proposed, or the whole view?
2. **Vayu cutting**: 55% of health per cut as proposed (kills fodder, wounds big foes), or a kill on everything but rulers?
3. **Should the throw damage the pilot's allies in co-op?** I propose no.

## 8. The camera (built) and the other astras as flows (proposals)

**Camera.** `World.astraView()` says how far the view should pull back and `GameScreen.updateZoom` eases toward it (out in about a third of a second, back more slowly):
a little while the mantra builds (up to x1.3 at full charge), x1.9 for Agni's bloom (its 650 px reach fits), the tornado's whole radius for Vayu (x1.5-2.0), x1.9 for the
Pashupatastra's beam, x1.35 while a volley is in flight. It always pulls out, never in. `AstraZoomTest` runs the real game and checks the zoom and that nothing breaks at it.

**The same method for the rest** - a flow on a field, sprites on 32x32 sheets, the world answering - each with ONE verb that no other astra has:

| astra | verb | the simulation | art (32x32 sheets) | unique to it |
|---|---|---|---|---|
| **Jala** the flood | **surge** | a crest of water travels along the nose (900 px, 1.4 s) as a moving pressure front on the field: foes in it are *carried* forward (the Agni/Vayu carry), enemy rounds are pushed back and returned harmless, a foe carried into rock takes impact; behind the crest a shallow sheet of water lingers 4 s: foes in it are **soaked** (-40% speed) | CREST (a curved line of foam), FOAM, DROPLET, RIPPLE rings, SHEEN (a puddle that glints) | heals the hull per foe struck; soaked foes **extinguish** burning and **conduct** a Pashupata beam |
| **Prithvi** the mountain | **quake** | a shock ring leaves the hull and CRACKS run out along the ground as particles that travel straight and branch (like lightning on the floor); where a crack ends a stone pillar rises (a temporary solid tile for 6 s: it blocks bullets and walls foes in); rubble hops off every crack | CRACK segment, PILLAR (top-down stone block rising in 8 frames), PEBBLES, DUST, SHOCK ring | the only astra that **builds cover**: pilot gets stone skin 2 s; a foe crossing a crack is staggered |
| **Akasha** the hunter | **mark** | the held shards become comets; on release a CONSTELLATION draws itself between every locked foe (lines of light, nearest first, one every 0.1 s), then each comet runs its line and strikes in that order, each strike a ripple that chains to the next star | STAR (twinkling), LINE (a line of light, tiled), COMET head and tail, RIPPLE | the only **targeted, geometric** astra: more locks, more lines, a bigger shape; goes through rock; refills the shield |
| **Pashupata** the third eye | **gaze** | the beam stays, but now **burns the world as it sweeps**: ash particles drift off it, the ground it crosses is scorched white (ASH, like Agni's scorch), a foe it holds does not die but **dissolves to ash** over 0.6 s (a dissolve, not a hit flash), and a ring of heat flexes at the hull | ASH (flakes), CINDER, EYE (open/close), BEAM tiles | the one that **cannot be stopped**: armour, rock, smoke, shield; a sustained steered beam |
| **Brahma** the last arrow | **ending** | time slows to 0.35 for ~1.2 s while the five elements' flows spiral in from five points into one arrow; the arrow crosses the screen; at its end a five-stage nova: a ring of FIRE (ignite), then WIND (pull and cut), WATER (soak and push), STONE (stagger and crack), LIGHT (mark every foe) - each ring is the flow of its own astra | the five sheets plus ARROW and NOVA rings | the finale: **uses all five flows in order**; whole-screen; the camera pulls to its widest |

**Elemental reactions (the thing that would make it feel like one simulation).** Because every flow acts on the same bodies and the same ground, the astras can answer each other, at no
extra cost per astra: wind spreads fire (a burning foe inside the tornado ignites foes within 200 px, not 100); water puts fire out and, with fire, makes **steam** (a drifting cloud that hides
foes from aim and slows them); stone pillars stop wind and fire; the Pashupata beam through a soaked foe jumps to the nearest soaked foe. Each is a few lines on the shared
`burnT / soakT / staggerT / markT` timers on an enemy.

Order I would build them in: Jala, Prithvi (they need the new shared **status timers** and temporary cover), Akasha (a rework of the volley), the Pashupata ash pass, then Brahma last
(it composes the five).
