# The next astra work: the mandala's shield, Akasha's lightning, Prithvi's boulders, Brahma's lotus

Status: PLAN, then built in the order of section 6. Everything uses the AstraFlow method (docs/ASTRA_FLOW_PLAN.md): a pooled particle system stepped in the world tick, 32x32 sprite
sheets, the host owns every verdict, a guest's release is replayed on the hub from its hull, the camera pulls back (`World.astraView`).

## 1. The mandala is a shield (built first, small)

While an astra is being formed nothing can hurt the pilot from outside: **every enemy round that touches the mandala is destroyed** where it touches.
- The mandala's radius is a pure function of the charge and the astra's loka tier (`Astra.mandalaRadius(kind, k)`, the same number `Mandala.draw` uses).
- Each tick of the invocation, every hostile bullet whose distance from the hull is within that radius is destroyed with a small burst in the astra's colour (a spark and a short
  ring). No damage reaches the ship. The shield is exactly the visible circle, so it GROWS (tier) and SHRINKS (the mandala tightens as the charge builds): the pilot reads their cover
  off the picture.
- It ends the instant the mantra is loosed or cancelled (the flows have their own protection: Vayu's storm, the others none).
- Tests: a bullet inside the radius is destroyed while invoking and not otherwise; the pilot's hull is untouched; the radius shrinks with the charge and grows with the tier.

## 2. AKASHA - the circular lightning (verb: strike)

Indra's own weapon: on release a **burst of circular lightning** goes out from the hull and **strikes every foe near it at once**.
- **Reach**: 520 px (a tier-0 astra, so the smallest of the seven), scaled by the charge (360 px at the shortest invocation).
- **Beats**: t = 0 a white flash and a ring of lightning leaves the hull (a jagged circle that expands to the full reach in 0.3 s); 0.05 - 0.55 s every foe inside the reach is struck, nearest
  first, one every 12 ms, by a forking bolt from the hull; at 0.5 s a second, weaker ring (the echo) strikes again for half damage.
- **The strike**: 85 x dm damage, a 0.8 s **stun** (the foe's brain waits, as in the tornado), and an **arc**: from each struck foe the lightning jumps to the two nearest foes within 160 px for 50%.
- **Pilot's gain**: the shield is refilled (as now).
- **Look**: jagged bolts (a polyline with seeded midpoint displacement, re-jittered every 50 ms so it crackles) in three layers - a wide violet glow, a mid layer, a white core - with short
  side branches; the ring is a circle whose radius is displaced by noise; stunned foes wear small arcs that crawl round them for the stun's length. All drawn as light in the shape pass.
- **Simulation**: `AstraFlow.LIGHTNING`: a schedule of strikes (foe, delay) built at the release, a list of bolts to draw (points, age), the ring's radius over time.
- Tests: every foe in the reach is struck, none outside; order is nearest first; stun is applied; arcs reach a neighbour within 160 px; a second ring strikes at 0.5 s for half; the shield is refilled.

## 3. PRITHVI - the boulders (verb: hurl)

The mandala STAYS (fully formed, pulsing) for the whole rite and **boulders form out of it and are hurled**: lots of them, breaking and re-breaking.
- **Beats** (about 3.4 s): the stone skin is given at the release; for 3 s a boulder forms every 60 ms at a random point of the mandala ring (a CREATE animation: pebbles and dust
  draw together into a rock in 0.25 s), then flies out of the mandala at 520-760 px/s.
- **Aim**: about 45% of them are thrown at a living foe (with a little error); the rest fly in random outward directions, so they strike **walls and whatever is in between** - which is
  most of the spectacle - and a share of them hit enemies anyway.
- **Breaking, three generations**: a BOULDER (varieties: round, slab, veined, jagged) that meets a wall, a foe or a hostile round **breaks** into 3-5 CHUNKS flying on in a cone away from the
  hit; a chunk that meets anything or whose life ends **breaks into 6-9 PEBBLES** (much smaller, short-lived, light damage). Each generation has its own sprite and size.
- **Damage**: boulder 70 x dm x its size (a heavy blow, once), chunk 28 x dm, pebble 7 x dm; the first foe a rock touches takes it. A hostile round touching a rock is destroyed.
- **Cap**: at most 14 boulders alive, the pool never beyond its cap (a new boulder waits if the pool is near full).
- **Art**: `astra_earth.png`, 10 rows x 8 frames of 32x32 - CREATE, BOULDER A (round), B (slab), C (veined), D (jagged), BREAK (boulder into chunks, one-shot), CHUNK A, CHUNK B, PEBBLES,
  DUST (one-shot), BREAK SMALL (chunk into pebbles, one-shot). Earth palette (sand, ochre, brown, umber, shadow) with a faint vein glow.
- Tests: boulders appear on the mandala's ring; one that meets a wall breaks into chunks and a chunk into pebbles; a foe hit takes the damage once; a hostile round touching a rock dies; about
  half are aimed at foes; the pool never exceeds its cap.

## 4. BRAHMA - the lotus (verb: bloom), in small steps

Five petals of the Brahma lotus spread through the world and **end every foe**, each petal in one element's colour with that element's effect. Built in six small steps so each can be seen working:

| step | what | done when |
|---|---|---|
| L1 | **Petal art**: `astra_lotus.png` - BUD (the lotus blooming at the hull, one-shot), PETAL OPEN (a petal unfolding, one-shot), PETAL FLY (a petal fluttering, loop), PETAL SPIN (the petal edge-on, a spinning blade, loop), PETAL FADE (dissolving, one-shot), MOTES (petal flecks); near-white so the game tints it with the element's colour | a contact sheet to look at |
| L2 | **The bloom**: on release the five petals unfold from the hull in a ring (0.9 s, one per element, each tinted its element's colour), the camera pulls to its widest | five tinted petals open |
| L3 | **Seeking**: each petal picks the nearest living foe nobody else is on (no gravity, no arc: it steers straight at it at 900 px/s, turn rate 9 rad/s) and leaves petal motes | each petal reaches a different foe |
| L4 | **Circling**: on arrival the petal circles the foe (radius 60 px closing to 38, 0.48 s, spinning as a blade) and strikes three times as it circles | a foe is hit three times and the petal moves on |
| L5 | **Each petal's effect**: Akasha petal - shock (stun 0.6 s); Vayu - a cut (extra bleed damage); Agni - ignites; Jala - heals the pilot; Prithvi - a heavy blow and stagger | five different effects on the same foe |
| L6 | **Ending**: petals keep going, foe after foe, until no foe is left within 2200 px or 9 s pass; then they gather and fade at the hull; a ruler takes only 4% of its health per circling | a crowd is cleared; a boss is hurt but not slain |

Damage per circling is 60% of a foe's health (at dm = 1) and never under 220, so one petal ends a common foe in two visits and five petals together end it at once; a ruler is capped at 4%
per circling and loses at most a quarter of its health to the petals in all. Tests: five petals, five different foes at once; a petal circles (its angle round the foe changes while its radius shrinks); a circling hurts three times; the element effects apply;
everything within the range dies in a bounded time; a boss survives; a guest's Brahma blooms from its own hull.

## 5. Order of building

1. the shield (section 1); 2. Akasha's lightning; 3. the earth sheet, then Prithvi's boulders; 4. the lotus sheet, then L2-L6; each step ends with a compile; the whole ends with tests, a render of
each effect and a green build. Akasha, Prithvi and Brahma no longer fire the cube volley (Jala still does until its surge is built; the Pashupatastra is the beam; Agni and Vayu are flows).
