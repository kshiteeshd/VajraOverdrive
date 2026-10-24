# Atala, built as a 2D game with a touch of 3D

Decision this plan makes: **the playing space is the old wall world, kept flat and readable; the 3D is a stack of parallax
layers behind it and a far lit land under it.** The raycast terrain stops being the thing you fly over and becomes the floor
you see at the bottom of the shaft. Aim, collision, enemies and bullets stay exactly 2D.

Six regions, same count and same order as today (`World.regionStart`), renamed where the new idea asks for it:

| # | band y | old name | new name | one-line idea |
|---|---|---|---|---|
| 0 | 0-1600 | Avatarana (Bhu Loka) | **The Mouth of the Earth** | daylight meadow, then the ground opens into a shaft |
| 1 | 1600-4100 | Svarna Dvara | **The Golden Door** | the first gem-lit hall; a stair of gate-arches going down |
| 2 | 4100-7600 | Maya Vana | **The Mirage Wood** | a forest that is half illusion, in layers of glass |
| 3 | 7600-12000 | Svapna Mandala | **The Dream Circle** | rings of light; the layers turn |
| 4 | 12000-17600 | Bala Mukha | **The Yawn** | a throat; the walls breathe |
| 5 | 17600-20600 | Bala's court | **The Hall of Belief** | a mansion with pillars; you fly into it |

## 1. What "a touch of 3D" is made of (the toolbox, all 2D-safe)

1. **Layer stack** (built, `LayerStack`): the same walls again behind the play layer, smaller by `shrink^k`, darker and bluer,
   sliding by a fraction of the camera's move. This is the parallax.
2. **Vanishing point that follows the hull.** The stack's centre is not the screen centre but the hull plus a lean toward
   where it is going (velocity and aim): far layers drift opposite to the flight, like looking down a tunnel as you turn.
3. **Contact shadows.** The play layer's walls throw a soft dark copy onto layer 1 (a few px down-right); each layer does the
   same onto the next. The hull and every enemy throw a small shadow on layer 1. Depth is mostly shadows.
4. **Depth light.** The lightmap already pools light on the hull and enemies; pools also shrink and dim with the layer they
   lie on, and gem-lights live ON layers (a gem on layer 6 lights layer 6 and below, not the play layer).
5. **Depth haze.** Tint and a little blur-by-alpha with distance (built as tint); each region sets the haze colour.
6. **Air between layers.** Drifting motes, spores, embers, mirror shards, each pinned to a layer so they slide at that layer's
   speed. Between the play layer and layer 1 they cross in front of the far world; they sell "space" more than any wall.
7. **Extruded tiles.** Each wall tile also draws a thin darker side face on the side away from the light (the same sprite,
   offset 3 px and 55% black), so a wall reads as thick.
8. **The far floor** (built, `TerrainLayer`): the raycast land behind the stack, at its own altitude, big (x2) and hazed.
9. **Living layers.** A layer can move on its own (rotate slowly, breathe, slide), so the stack is not frozen between camera moves.
10. **Pillars as stacks.** A column is the same pillar sprite on several layers at the same x, shrinking: a shaft going down.

## 2. Region by region

Numbers are starting points; each is a field of the region's `Biome` row and eases over 500 px at a boundary like the light does.

### 0. The Mouth of the Earth (y 0-1600)
- **Play layer:** open meadow, no walls until y~1000; then the foothill rock closes in from both sides (the old rock, not boulders),
  leaving the 760 px pass; the boulder belt stays at y 1600 as the lip of the hole.
- **Stack:** 0 layers at the spawn (pure daylight, the raycast land underneath as today), growing to 15 by y 1500: the stack
  *appears* as the ground opens, so the first time you feel depth is the moment you enter the earth.
- **Floor:** the sunlit meadow raycast at fly 430, hazed; as layers appear it becomes the far floor, dimming as the sun is lost.
- **Living:** grass sways (sprites), birds cross high (above-layer sprites with shadow), a river runs through the pass and falls
  into the hole (animated layer).
- **Set-piece:** the descent. A one-way camera pull: the zoom eases out 1.0 -> 1.15 across the lip while the ambient goes from
  daylight to gem-light. No cutscene; the world does it.

### 1. The Golden Door (y 1600-4100)
- **Play layer:** the old walls: broad pale halls, gold trim; the three old gate walls return as real gates (doors 450 px).
- **Stack:** 12 layers, shrink 0.96, a faint warm tint, peek 0. Layers carry **gate-arches**: golden frames on layers 2, 5, 8
  that read as a stair of doors going down the shaft.
- **Floor:** white-soil land with the gold road, very far.
- **Living:** gem-lamps on layers (they light their layer), slow gold motes, doorkeeper statues on layer 3 whose eyes follow the
  hull (cheap: a sprite rotation).
- **Gameplay hook:** sentries stand in front of the arches; honour-guard parley stays.

### 2. The Mirage Wood (y 4100-7600)
- **Play layer:** thick dark tree-walls with gaps; *some walls are mirages*: they fade out as the hull nears and are not solid
  until then (a flag on a tile run; the minimap shows them dim).
- **Stack:** 15 layers, tint teal-black. Layers are **glass**: each shows the layer behind it slightly reflected, mirrored in x and
  drifting. The far floor is the mirror-wood land with silver pools.
- **Living:** wisps (the biggest cast of motes: 150, on 4 layers, they gather round the hull and scatter when it boosts),
  lantern-blossoms (tall glowing flowers on near layers, pulsing in waves along the road), giant mushrooms (lit from beneath),
  drifting lavender mirror shards at several depths (the same shard sprite per layer: they catch the hull's light).
- **Light:** ambient very low; the wisps and blossoms are the light.
- **Gameplay hook:** Mayavi's decoys come out of the mirage walls; a mirrored hull ghost appears in layer 1 and mimics you (harmless,
  distracting, readable).

### 3. The Dream Circle (y 7600-12000)
- **Play layer:** ring-shaped walls (concentric arcs with gaps) in violet stone.
- **Stack:** 15 layers, **each layer rotates** slowly (alternating directions, 0.5-2 deg/s) about the circle's centre: you
  look down a kaleidoscope. Gold ring ridges on every layer.
- **Floor:** lotus lakes, very large (the mandala as the bottom of the well).
- **Living:** slow petals falling through the layers, gold *hataka* pools that shimmer on layer 1, dream-pillars standing as
  stacked columns on the twelve spokes.
- **Gameplay hook:** rotating gaps in the nearest ring are the timing puzzle between waves.

### 4. The Yawn (y 12000-17600)
- **Play layer:** curved rib-walls like the inside of a throat; amber light along their crests.
- **Stack:** 12 layers; the layers **breathe**: shrink oscillates 0.94 <-> 0.97 on a 6 s cycle and the ribs open and close with it
  (the play layer's rib gaps follow the same clock, so the world and the stack agree).
- **Floor:** a red glow far below, the ground pulsing.
- **Living:** uvulas hang from above (sprites on near layers), warm air makes motes rise, Bala's gaze reddens the haze as the quota fills.
- **Gameplay hook:** the breath clock is the rhythm of the band: waves arrive on the exhale.

### 5. The Hall of Belief (y 17600-20600)
- **Play layer:** the hall's outer walls with a doorway; inside, the open floor. The mansion's **roof** is a front overlay that
  fades once the hull is under it.
- **Stack:** 20 layers - the deepest in Atala - and almost no tint: the hall is bright and rich. Pillars are **stacked columns**
  (the pillar sprite on layers 0-8 at each position: two rows of six down the nave), so flying between them you pass in front of
  columns that go down into the dark.
- **Floor:** the ivory-and-gold mosaic, raycast, right below the pillars' feet (low altitude, 90): the one place the land is close.
- **Living:** braziers (real light pools), hanging lamps swaying on layer 1, hataka fountains, a throne canopy on four pillars.
- **Set-piece:** the entry. At the doorway the stack snaps from the Yawn's 12 breathing layers to 20 still ones over 600 px:
  the world stops breathing and goes quiet - you've walked into Bala's house.

## 3. Transitions and the cost budget

- Layer count, shrink, tint, haze, far-floor altitude and ambient are all per-region and **eased over 500 px** at each boundary,
  so there are no pops. The count itself changes by fading the nearest/farthest layer's alpha, not by adding one suddenly.
- **Budget:** 8 ms a frame at 1080p on this machine (the test area is 7 ms for 15 layers). Tools if we exceed it:
  (a) merge layers k >= 8 into one texture re-rendered only when the camera has moved 48 px, (b) skip a layer whose tiles are
  empty in view, (c) the far floor at half resolution.
- **Automatic quality:** if the frame average exceeds 12 ms for 2 s, drop the layer count by 3 (and tell nobody).

## 4. What changes in the code

1. `Biome` gets `layers, shrink, haze, floorFly, lean, living[]` and the existing ambient/mist; `Biome.blend` generalises to all.
2. `LayerStack` + the test area's draw loop move into `WorldRenderer` (the game draws the stack when walls are on, behind the
   play layer, over the far floor). Walls come back on (`World.walls` default true for Atala), boulders stay at the first lip.
3. The ground/raycast stays as the far floor only; `PropLayer`-style stamping stays for the floor (trees, pillars).
4. New small systems: `LayerSprites` (a sprite or particle pinned to a layer: lamp, wisp, shard, pillar column),
   `LayerMotion` (rotate/breathe/slide per layer), `ContactShadows`.
5. Mirage walls: a `solidWhen` flag on tile runs + a fade in `TileImages` + collision check by hull distance.
6. Tests: layer maths (done for the stack), per-region biome blend continuity, mirage solidity, a screenshot per region, the
   frame-time guard, and a flight test through all six regions with the real game loop.

## 5. Order of work (each ends with screenshots and a green run)

| step | what | you see |
|---|---|---|
| A | **[built]** stack into the game, walls back on, per-band layer counts and deep tints, far land under it, minimap walls | the descent, the doors going down |
| B | **[built]** contact shadows, hull-following vanishing point, extruded tiles, motes between layers | the 3D feel gets stronger without touching play |
| C | the Mirage Wood: glass layers, wisps, blossoms, mushrooms, mirage walls | the magical band |
| D | the Dream Circle: rotating layers, rings, pillars | the kaleidoscope |
| E | the Yawn: the breathing stack + wall clock | the living throat |
| F | the Hall of Belief: stacked pillars, roof fade, throne court | entering a house |
| G | budget pass: merge far layers, auto-quality | smooth everywhere |
