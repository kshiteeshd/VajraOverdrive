# The Seven Astras: forged, charged, loosed

Status: PLAN (nothing below is built yet). One astra per loka, seven in all - five elemental astras for the first five lokas, the **Pashupatastra**
for Rasatala and the **Brahmastra** for Patala. The Akasha Astra is the reference for the *quality* of the look (additive light, one signature head,
a tapered trail, a yantra circle); **every other astra is a different design and a different way of being loosed**, not a recolour of it.

## 1. The rules

An astra is a **weapon you forge once, then charge before every use**. Shards are spent twice and leave the pile as they go:

| step | what | cost | where |
|---|---|---|---|
| **FORGE** | the astra exists for this run | **5 shards**, once | the hangar, ASTRA tab |
| **CHARGE** | fill its 5 cells | **1 shard a cell**, up to 5 | the hangar, and in flight with **C** |
| **LOOSE** | HOLD F, aim, let go | **empties the 5 cells** | in flight |

- Not forged = cannot be invoked (the card is a locked silhouette, like the hulls). A full ring (5/5) is needed to loose; a part charge is kept.
- A faltered mantra (let go too early, nothing to hit) costs nothing, as now. Cooldown 6 s after a release.
- Whose shards pay for it: the loka's own, so an astra can only be forged once its loka is reached and its element is falling:

| # | astra | loka | pays | forged and charged with |
|---|---|---|---|---|
| 1 | **Akasha** - the hunter | Atala | Akasha | 5 + 5 Akasha |
| 2 | **Agni** - the pyre | Vitala | Agni | 5 + 5 Agni |
| 3 | **Jala** - the flood | Sutala | Jala | 5 + 5 Jala |
| 4 | **Prithvi** - the mountain | Talatala | Prithvi | 5 + 5 Prithvi |
| 5 | **Vayu** - the gale | Mahitala | Vayu | 5 + 5 Vayu |
| 6 | **Pashupata** - the third eye | Rasatala | Agni + Prithvi (the two it pays) | 5 + 5 shards, any mix of those two |
| 7 | **Brahma** - the last arrow | Patala | all five | 5 + 5 shards, at least one of every element to forge |

(Today's data has Vayu second and Jala fourth; the loka table's own element order - Atala Akasha, Vitala Agni, Sutala Jala, Talatala Prithvi, Mahitala
Vayu - is the one to follow, and it matches the loka each astra is *found* in.)

## 2. The seven, one by one: design, and how each is loosed

All seven are invoked the same way - **hold F, a yantra opens round the hull, the charge builds, let go** - because that is the rite. What is
different is the yantra's *shape*, what the pilot **aims**, and what **happens at the release**. Each has one verb.

| astra | its verb | yantra | what you aim | the release | the pilot's gain |
|---|---|---|---|---|---|
| **Akasha** (built) | **hunt** | circle, two turning triangles | the nose picks the foes it locks | a cage of star-diamond cubes flies, one at each locked foe, homing **through rock** | shield refilled |
| **Agni** | **rain** | upward triangle ringed with small flames | a **ground target** ahead of the nose; its circle grows while you hold | after a 0.4 s whistle, **12 fireballs fall** in the circle over 2 s; each bursts (splash), ignites, and leaves a burning patch | extra coin from foes that die burning |
| **Jala** | **sweep** | crescent moon over rippling rings | the nose: a wave starts there | a **wall of water** a screen wide travels forward ~900 px: it **pushes foes and bullets back**, damages, and every foe it hits mends the hull | hull mended per hit |
| **Prithvi** | **quake** | a square (bhupura) with gates | none: it is centred on the hull | the hull **slams the ground**: a shock ring expands to 450 px, cracks run to the nearest foes, **stone pillars rise** as cover for 6 s; stone skin 2 s | stone skin |
| **Vayu** | **dash** | a spiral of six turning arcs | the nose: a line ahead of the hull | the hull **rockets 700 px along the line in 0.3 s**, invulnerable, **cutting every foe on it** with gale blades, and **chains** to two more foes beyond the end | boost refilled |
| **Pashupata** | **gaze** | a trident over a closed eye | the nose, **and it keeps turning with the nose** | a **third eye opens** over the hull and a vast beam **burns for 1.8 s**, swept by the nose: it **ignores armour, passes through rock**, and leaves ash that does not burn the pilot | none: it is the one that wins fights |
| **Brahma** | **ending** | a lotus mandala, five petals, five colours | none: it takes everything in view | the world **dims and slows**; one **colossal five-coloured arrow** flies the length of the screen and **detonates in a five-colour nova** that clears the view; each element's trait lands once (shield, hull, boost, coin, stone skin) | all five gains at once |

Why they are distinct: Akasha is *homing volley*, Agni *area bombardment*, Jala *a front that moves things*, Prithvi *a burst around you*, Vayu *you are
the projectile*, Pashupata *a sustained beam you steer*, Brahma *a single screen-clearing event*. No two share a release shape.

### Agni - the pyre (first to build)
- **Hold F**: the red-gold yantra opens (an upward triangle in a ring of small flames). A reticle circle appears **ahead of the nose** and **grows** with the
  charge (80 px at the shortest invocation, 220 px at full). Move the nose to move it.
- **Let go**: a rising whistle for 0.4 s, then **12 fireballs fall** at random points in the circle over 2 s (a shadow grows under each first, so the
  player can see them coming and the *enemy brains* can too).
- **Each fireball**: 70 damage and a 90 px burst (60% splash), **ignites** everything touched for 3 s (12 damage a second, a second hit refreshes it), and
  leaves a **burning patch** (the Vitala `Hazards` patch: 4 s, hurts foes in it, never the pilot). Foes that panic out of a patch break formation.
- **Pyre spread**: a foe that dies burning ignites foes within 100 px. A tight court goes up as one.
- **Rock** is not a barrier to this astra (it falls from above), which is why it is the answer to a court on the far bank of a river - while Akasha is
  the answer to a lone caster behind a wall.
- **Look**: 32x32 sprites from the fire sheet - `hearth` fire for the falling balls, the `ground` fire for the patches, a new 8-frame burst sheet; the
  yantra, reticle and shadows are drawn shapes in the Akasha style (additive).

### Art for every astra
Each astra gets a 32x32 generated sheet (like `fire.py`, deterministic): *held shard*, *head/body of the release*, *impact*, *status on a foe*, 8 frames each,
plus its yantra drawn as shapes. A preview contact sheet comes first, you choose, then I wire it. Generator: `tools/enemy_art/astra.py`, one function per astra.

### Built so far
- **Mandalas** (`screen/Mandala.java`, drawn by `AstraFx`): one per astra, the seven styles included (Pashupata's waits for its astra). Each is a list of elements
  that form **one by one** with the charge - rings sweep round, polygons run along their edges, petals open in turn, each with a bright head while it forms - and
  once formed an **energy wave pulses through it** (outward and round), with runners circling the rim; the whole tightens as it charges and flares and spills
  light at a full charge. `MandalaShotTest` writes `build/mandala-all.png` (all seven at 25 / 50 / 80 / full).
- **The release always fires.** Letting go of F looses the astra even with nothing locked: the cubes fly along the nose, a cube with nothing to chase looks
  ahead (650 px) for a foe, and the shards are spent. (It used to falter, and cost nothing, unless a foe was in view.)
- **Slower invocation**: `WIND_FULL` 1.5 s -> 3.4 s (`WIND_MIN` 0.9 s): the mandala has time to form, element by element.
- **Lines first, light later**: the first half of the invocation is plain lines and shapes growing (no glow, no bright tips, no pulsing, no sparks, no halo); the light of the
  charge (`Mandala.light`) comes in from 50% to 95%: the glow round the shards and their trails, the bright heads, the pulse through each element, circling lights, sparks and the halo.
- **Held shards wear their shape** (star-diamond, arrowhead with wisps, fireball, teardrop, tumbling stone), the same heads the loosed bolts have, not plain circles.
- **The Pashupatastra** (kind 6, `Astra.PASHUPATA`): paid in five shards of Agni + Prithvi in any mix; HOLD F, let go, and the third eye opens: a beam steered by the
  nose for 2.2 s, 420 damage a second to everything on the line, **ignoring armour, rock and smoke** (`Enemy.hurtPiercing`). Seven cards in the hangar, an eye on the dial.
- **Grandeur by loka** (`Astra.TIER`: Akasha 0 Atala, Agni 1, Jala 2, Prithvi 3, Vayu 4, Pashupata 5, Brahma 6): the mandala grows x1.12 a tier and gains layers - rays,
  a ring of diamonds, an outer ring with ticks, four wing arcs, satellites, a crown of spikes, a pillar of light with the five elements wheeling - the shards grow 10% a tier
  and the opening ring of a release 15% a tier. `build/mandala-by-loka.png` shows them side by side.
- Agni's art sheet and the mandala proof (`tools/enemy_art/astra.py`, `tools/enemy_art/mandala.py`).

## 3. The UI

### Hangar ASTRA tab (forge and charge)
**Seven cards**, in loka order. Each shows the element's **stock** (gem and number) and three states:
1. **UNFORGED** - the astra's emblem as a black silhouette with an element-coloured rim, five empty cells, `FORGE  5 SHARDS` (or `SHORT BY 2`).
   Cards for a loka the pilot has not reached read `OPENS RASATALA`, as the hulls do.
2. **FORGING** - ENTER sends the five shards flying from the stock number into an anvil ring under the emblem, one every 0.15 s; the ring flares, the
   silhouette fills with colour; `AGNI ASTRA FORGED`. The stock number falls by five.
3. **FORGED / CHARGING** - the coloured emblem over 5 cells. **ENTER pours one shard** (hold to pour on); each cell lights and the stock ticks down. A full
   ring glows; the card then shows its **verb** in one line (`RAIN`, `SWEEP`, `QUAKE`, `DASH`, `GAZE`, `ENDING`) and a small diagram of how it is aimed.

Footer: `FORGE 5 SHARDS ONCE. CHARGE 1 SHARD A CELL. A FULL RING LOOSES IT.`

### Flight HUD (the astra dial, bottom-right)
The vedic dial keeps its gem in a ring of five cells, but the ring now shows the **astra's charge** (a lit cell per charge, the whole ring glowing when
full; an unforged astra is a dashed silhouette). **C** pours one shard of the selected astra's element, anywhere in a run (holding C pours on); a pickup
flashes `AGNI 3/5` beside the gem. **X** still chooses; it skips what is unforged or uncharged.

### Saves, pause, controls
Forged flags and charges ride in `RunSave`, show on the pause screen beside the shard counts, and the ControlScreen gets a C line.

## 4. How it is built

- **Rules** (`Hangar`, `Astra`): `astraForged[7]`, `astraCharge[7]`; `Astra.KINDS = 7`; `FORGE_COST = 5`, `CELLS = 5`; `forge`, `pour`, `canLoose`, `pay`.
  Pashupata pays from Agni and Prithvi in any mix; Brahma from any element and needs one of each to forge. `releases()` becomes 0 or 1.
- **One rite per astra** (`world/astra/`): a small `Rite` class each - `begin`, `hold(dt, charge)`, `aim(...)`, `loose(power)`, `update(dt)`, `done()` - because the
  releases are genuinely different programs. `AkashaRite` is today's cube code moved over untouched; `AgniRite` (reticle, falling balls, burn status,
  patches), `JalaRite` (a moving front with knockback on foes and enemy bullets), `PrithviRite` (a ring, cracks, temporary rock tiles as pillars),
  `VayuRite` (a hull dash with invulnerability and a hit line), `PashupataRite` (a swept beam that ignores armour and rock), `BrahmaRite` (a slow-mo
  focus, the arrow, the nova). `World` holds the active rite and calls it; the shared parts (yantra, the charge, the lock lines) stay in `AstraFx`.
- **Rendering**: `AstraFx` dispatches per kind (shapes for light and yantras, the 32x32 sheets for bodies and impacts).
- **Co-op**: the host runs the rite and the wire sends `kind, power, aim, seed`; guests replay the visuals from those (the existing `astraWire` carries the
  kind and the targets; it gains the aim point and a seed). A guest's charge is the host's hangar, like coins.
- **Balance**: astra damage is flat today while enemy hulls climb x5.5, so deeper astras would fade. Each astra's damage scales with the loka it is loosed
  in (the enemies' own hull scale, softened), and `BalanceTest` checks that a full release removes about the same share of a court at every loka.
  The seven prices are the same 5 + 5; what differs is the loka that pays them.
- **Tests**: forging costs five and only once; a pour moves exactly one shard; a part charge cannot loose; a loose empties it; Pashupata's mixed payment;
  Brahma's one-of-each rule; one test per rite for its signature (Agni: twelve fall inside the circle and burn; Jala: a bullet in the wave's path is
  pushed back; Prithvi: pillars exist for six seconds; Vayu: the hull ends 700 px on, unhurt; Pashupata: an armoured foe behind rock takes damage;
  Brahma: everything in view is hit); saves round-trip.

## 5. Order of work

1. **Agni** - the art preview (the fire, burst, reticle, yantra), you choose; then `AgniRite`.
2. **The shared rules and UI** (forge, charge, C, the seven-card hangar tab, the dial) - built once, with Akasha and Agni as the first two forgeable.
3. **Jala**, **Prithvi**, **Vayu** (each: art preview, then rite).
4. **Pashupata**, then **Brahma** (these two come last because their lokas do).
Each step ends with a green build and a screenshot of the release.

## 6. Questions that change the build

1. **Full charge per use** (5 cells = one loose), or power that grows with the cells (1 cell = weak, 5 = full)? I assumed a full charge.
2. **Pour in flight (C)?** Yes keeps the pilot in the fight; no keeps shards a shop decision.
3. **Charged cells survive death?** I assumed yes (a death costs a life, not the astra).
4. **Pashupata and Brahma costs**: both 5 + 5 like the rest, with the mixed-element rules above. Or dearer, as the last rewards?
5. **Order of elements**: I propose following the loka table (Vayu is Mahitala's, the fifth astra), which moves Vayu later than the code has it today.
