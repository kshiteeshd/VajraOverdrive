# The Build Plan - a pseudo-3D world seen from above

`WORLD_BIBLE.md` says what the world IS (Bhagavata research). This says how we BUILD it, in order, after the
first slice (a painted ground) read as "a 2D picture behind the game".

## 0. What the pilot asked for

1. **Depth.** Parallax, a world that feels 3D from the top: things stand up out of the ground and lean as you pass.
2. **No walls** (for now). The old rock tiles go: no drawing, no collision, no map of them. Terrain is the world.
3. **Bhu Loka to Atala goes through mountains and caves**, not through a gate in a void.
4. **Bala's mansion is a building you ENTER**: a threshold, pillars, halls, a throne - seen from above in depth.
5. **Maya Vana is full of magic.**
6. **Readability.** Enemy bullets and the ground are nearly the same colour. Fix it.
7. **The map must match the world and be opaque.**
8. Plan first, then build step by step.

## 1. The depth model (REVISED: a raycast over flat cut planes)

The first pass drew props as stacks of slices (read as 3D models, and the pilot did not want models). It is replaced.

The land is ONE HEIGHT MAP of flat planes: each texel holds a colour and a height; a tree is three stacked discs, a pillar a
plinth, a shaft and a capital, a boulder five steps, a mountain a terraced stack. A fragment shader raycasts it: every pixel
marches a ray from a camera `D = 900` above the flight plane, which is `fly` above the ground, down through the map to the
first plane it strikes. A point at height `z` projects at `D / (D + fly - z)` relative to the flight plane (so the plane
itself is scale 1 and gameplay does not change). Parallax, occlusion, shaded sides and cast shadows all fall out of it.

**Near and far from the ground** is `fly`, set per place (`Altitude.fly`): ~430 over the meadow, ~120 in the pass, ~300
across Svarna Dvara, ~90 in the mirror-wood, ~380 over the dream-circle, ~200 in the throat, ~90 in the mansion. High: the
ground is small and slow and hazed. Low: it is large and quick and the crowns stand above the hull.

Two passes: A everything up to the flight plane (under the hull), B only what stands above it (translucent, over the hull).
Not changed: collision, aiming, enemy AI, the lights' mapping for things on the flight plane.

## 2. Walls off

`World.WALLS` (`-Dvajra.walls=true` turns them back on). After `WorldGen` has placed everything (placements still
use the old open spots, so nothing lands in a place that is now silly), the tile maps are emptied and the wall-anchored
plants and torches dropped. Nothing draws them, nothing collides with them (hull, enemies, bullets), the minimap
ignores them. Gates and seals stop being geometry and become **places** (see 4).

## 3. Readability: bullets beat the ground

Enemy rounds must be the loudest thing on screen, on every ground we will ever paint.
1. **Every hostile round gets a dark core ring and a bright rim** (a two-tone halo): visible on light ground by the
   dark ring, on dark ground by the bright rim.
2. **A hot colour the ground never uses**: the enemy bullet hue is chosen against the band's mean ground colour (the
   biome row carries a `bulletHue`); on green ground bullets are red-magenta, on violet they are lime-white, etc.
3. **The ground is quieter than the bullets**: ground saturation and contrast are capped (the painted soil stays within a
   luminance window), and a soft dark "vignette" pool sits under every hostile round (0.25 alpha) so it separates.
4. **A test** measures contrast (luminance and hue distance) between each band's mean ground and the bullet palette and
   fails if it drops below a floor.

## 4. The world, place by place

World is 5200 wide, 20600 tall; the hull enters at the bottom and climbs. Bands (`World.regionStart`): Bhu Loka 0-1600,
Svarna Dvara 1600-4100, Maya Vana 4100-7600, Svapna Mandala 7600-12000, Bala Mukha 12000-17600, Bala's court 17600-20600.

### 4.1 Bhu Loka and THE DESCENT (y 0-2100)
The sunlit meadow (built). It narrows into a valley and the land rises.
- **y 600-1300 - the foothills:** low rounded hills (stacked terraces, 40-70 high), a river crossing under a bridge,
  groves thinning, the road from the surface climbing toward the pass.
- **y 1000-1500 - the mountains:** two ridgelines (left and right, x < 1500 and x > 3700 leaving a pass 2200 wide)
  as stacked-contour relief: rock terraces, snow-white pearl caps, a waterfall tinted gold. The pass floor is the road.
- **y 1400-1900 - THE CAVE (the bila):** the pass runs into a mountain: the hull flies under an overhanging **brow of
  rock** (an overhead layer that closes in from both sides), the light falls from sun to gem-light across the threshold
  (ambient eases over 500 px, already built), stalactites hang overhead (above the plane, leaning), gem veins glow in the
  rock, the road becomes a gold-paved causeway. The grand gate stands in the cave mouth (the Dvarapala parley).
- **y 1900-2100 - emergence:** the cave opens into the jewel-lit land of Atala: Svarna Dvara.

### 4.2 Svarna Dvara (1600-4100) - the golden door
White-soil plain under a vault; the gold road from the cave runs north through **three gate-arches** (extruded gold
frames above the plane) and past **gem-lamp posts** (110 high, glowing heads). Pale gem-gardens (teal-lavender trees),
shallow pools, a shrine of lamps at the first checkpoint. Marble paving patches round the shops (flat decals), low walls
as plinths.

### 4.3 Maya Vana (4100-7600) - the mirror-wood, FULL OF MAGIC
Dark glassy trees (60-110 high, with a slow sway), and:
- **Mirror pools** that reflect a sky that is not there (animated drifting stars and a pale sun that should not exist).
- **Wisps**: hundreds of drifting light-motes in two layers; they gather round the hull and scatter when it boosts.
- **Illusion trees**: a third of the trees flicker, turn translucent, and some resolve into a *different* tree when the
  hull comes near (the mirage). Later: some are enemies' decoys.
- **Lantern-blossoms**: tall flowers (150) hanging glowing heads over the plane; they pulse in waves along the road.
- **Giant mushrooms** (90): stems extruded, caps lit from below, spores drifting up.
- **Mirror shards**: floating lavender glass planes (Mayavi's) that drift at several heights and catch light.
- **Spirit-path**: a winding ribbon of light on the ground, and a ring-arch (above the plane) every 700 px along it.
- The ground has fireflies-as-glints and webs of silver light between trees.

### 4.4 Svapna Mandala (7600-12000) - the dream-circle
Violet ground with the golden mandala rings (built) now **raised**: the rings are low gold ridges (extruded, 10 high),
lotus lakes with large pads (flat) and lotus buds (extruded), **dream-pillars** (floating pale columns, 160) at the
mandala's twelve spokes, pools of *hataka* gold that shimmer.

### 4.5 Bala Mukha (12000-17600) - the yawn
Maroon ridges become real **ribs**: long curved extruded ridges (30-60 high) like the inside of a throat, with amber
light along their crests; hanging **uvula-stalactites** overhead; the ground pulses slowly (ambient breathes). At its end
the throat narrows to the mansion's causeway.

### 4.6 Bala's Mansion (17600-20600) - you ENTER it
Seen from above, in depth (centre x = 2600):
- **The causeway (y 17200-17700):** a raised road (plinth 20 high) lined by **gold lotus lamps**, two enormous
  **pylons** (190 high, a lintel across at 260) at y 17700 - the threshold; the roof begins here.
- **The great hall (y 17800-19200, x 1900-3300):** the mosaic floor (built). **Two rows of six pillars** (x = centre +- 380,
  every 230 px, 170 high, lotus capitals, gold bands), **side aisles** with shorter pillars, **outer walls** (extruded
  prisms 150 high with arched windows, light spilling in), a **coffered roof** (beams across at 250 every 230 px) drawn
  translucent: **opaque from outside, 80% clear once the hull is under it**.
- **The throne court (y 19200-20400):** three concentric steps (extruded, 12/24/36 high), a **gold canopy** on four
  pillars over the throne, braziers at the corners (light + flame), two *hataka* fountains, hanging lamps.
- **The back wall (y 20500):** a great gold door - the seal; Bala's own sprite fights in front of it.
- **Gameplay stays as is:** pillars do not block (yet); the hull and rounds pass between them; the roof never hides the hull.

### 4.7 Not yet (planned, see the bible)
Vitala onward: one `Biome` row each plus its props; Garuda's shadow; the Sudarshana omen; the vault of gem-lights.

## 5. The map

The minimap is **opaque** and **is the world**: a disc filled from a baked top-down thumbnail of the ground
(`GroundMap` sampled once per 32 px), so the dial shows the meadow, the mountains' pale caps (when built), the cave's
dark throat, the gold causeway, the throat's ribs, the court. On top: the band's name when it changes, landmark glyphs
(gate, cave mouth, mansion, shrines), then the contacts (built). Walls are gone from it.

## 6. The steps (each ends with screenshots and a green test run)

| # | step | done when |
|---|---|---|
| 1 | **[built]** **Walls off + the depth model**: `World.WALLS`, strip maps/plants/torches; `Depth`; ground, mist and motes at their own scales; shadows follow | no wall anywhere; ground drifts slower than the hull; tests pass with walls off |
| 2 | **[built]** **Bullets beat the ground**: ring + rim + per-biome hue + vignette; contrast test | hostile rounds read on all six grounds in the screenshots |
| 3 | **[built]** **The opaque world map**: baked thumbnail, opaque disc, band name, glyphs | the dial shows the real land, opaque |
| 4 | **[built as planes in the height map: trees, mushrooms, lamps, dream-pillars, boulders, terraced mountains]** **The prop engine**: `Prop` kinds (disc-stack, column, prism, ridge), seeded placement by band, painter order, face shading | trees/pillars/mushrooms lean and shade; 60 fps |
| 5 | **[mountains, the boulder belt and the pass built; the cave brow and the gate in the mouth next]** **The descent**: foothills, mountains, the cave brow, the gate in the cave mouth | the flight from meadow into Atala goes through them |
| 6 | **Maya Vana's magic**: pools with reflections, wisps, illusion trees, lantern-blossoms, mushrooms, shards, spirit-path | the band reads as magic in motion |
| 7 | **Bala's mansion**: causeway, pylons, hall, pillars, walls, roof cutaway, throne court | you enter a building |
| 8 | **Svapna and Bala Mukha relief**, Svarna Dvara arches and lamps | every Atala band has depth |
| 9 | The rest of the lokas, one `Biome` + props each | later |
