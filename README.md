# Vajra Overdrive — Reboot

A 2D open-world exploration shooter on libGDX. **Version 1.0.0 is Atala and Vitala**, one continuous world: a bare Bhu Loka at
the bottom, a grand gate, gated Atala above it (Bala), then Vitala's river of gold and fire on its roof (Bhairava), and past his gold
door a closed hall marked *Sutala - coming soon*. 7 ships, 15 weapons, co-op multiplayer.

**Right now:** a run starts with the base ship (Vajra Mk-1) and the Single Bolt only; every other hull shows in the hangar as a locked
silhouette until it is bought. The side story - the people of Patala and the jobs they give - is held back (`World.sideStory`, off in the game, on in
the tests); Vitala's roster is thinned to about 210 enemies.

## Download and install

Grab the installer for your system from the [Releases page](../../releases/latest). Each one carries its own Java runtime, so nothing else needs installing.

| system | file | how |
|---|---|---|
| Windows | `VajraOverdrive-setup.exe` | double-click. Windows may say *unknown publisher* (the installer is self-signed): **More info -> Run anyway**. |
| Mac, Apple silicon (M1 and later) | `VajraOverdrive-mac-apple-silicon.dmg` | open it, drag the game onto Applications. First launch: **right-click the app -> Open** (it is not notarized). |
| Mac, Intel | `VajraOverdrive-mac-intel.dmg` | same as above. |

Each file has a `.sha256` beside it to verify the download. Saves live in your user folder (on a Mac: `~/Library/Application Support/Vajra Overdrive`).

**The antagonist is Trivikrama** — the giant who humbled Indra and trisected the three worlds into
Vayu, Brahma and himself. He broke the Vajrayudha and hid one band in each of the seven lokas of
Patala. You climb to take them back. Atala is the first loka and its warden is **Bala** (the
Puranas make him Atala's ruler, son of Maya). Only Atala exists in code today; the other six are
specified in `world/LokaTable.java` and not yet built. Everything below describes what runs.

**A run owns nothing on exit.** Coins, hulls, tiers, patterns and shards are all
built inside a run and die with it — a new game resets the hangar to its defaults. The only thing
that persists is the best score, which is a record rather than progress.

## Run

```
.\gradlew.bat lwjgl3:run        # play — 1920×1080 windowed, vsync, 60 fps cap
 .\gradlew.bat core:test         # 295 core tests
```

Requires JDK 17. Gradle 9.7 wrapper, libGDX 1.14.2, JUnit 5.11.4. Game assets live in `/assets`
(wired in as `lwjgl3` resource dir), procedural art generators in `/tools`.

## Boot

The game opens **fullscreen** on the **studio card**, then goes to the main menu.

`SplashScreen` draws the SAHAH logo centred on the field, with the name **THE BURNING SHELL**
beneath it. The beat:

| Phase | Time | |
|---|---|---|
| `DELAY` | 1.2 s | black, nothing drawn — the window settles before the logo |
| `FADE_IN` | 0.7 s | the logo rises 0 → 1 |
| `HOLD` | 5.0 s | held, long enough to read |
| `FADE_OUT` | 2.0 s | the logo falls 1 → 0 |

8.9 s in all, and any key or click skips to the end. The field is the menu's own **ink**
(`13, 13, 26`), not pure black: `HomeScreen` draws an opaque field of exactly that colour, so
the splash ending on it makes the stack swap byte-identical and the handover cannot flash.

**The viewport.** Only `GameScreen` ever had a `FitViewport`. Every UI-only state — the studio
card, the title, pause, settings — draws through the shared `Ui` camera with no viewport of its
own, so it kept whatever `glViewport` the last world render left behind. After the boot-time
switch to fullscreen that was stale, and the frame came up short of the top by a black strip.
It cleared itself the moment you pressed PLAY, because `GameScreen` applies its own viewport
every frame — which made it look like a splash-only bug when it was every UI screen.

`Ui.applyViewport()` is now called once a frame by `Flow.paint()`, before anything draws, so
every state is covered and none of them has to remember. `ViewportFillTest` resizes the window
under a UI-only state and checks all four edges of the frame are filled.

`logoAlpha` is a pure function of time, so `SplashScreenTest` pins the whole beat headless;
`SplashScreenshotTest` renders it for real and checks the field never leaves ink.

**KNOWN: the logo wordmark does not show.** The logo's "SAHAH" text and the vajra's outline are
black, and the field is black, so only the orange flame and the grey vajra are visible — see
`build/splash-3-hold.png`. The game name "THE BURNING SHELL" is drawn in gold beneath the logo
instead. Re-keying the dark pixels of `assets/brand/studio-logo.png` to cream would bring the
logo wordmark back; that is an edit to the art and has not been made.

## Controls

| Key | Action |
|---|---|
| W A S D / arrows | Thrust, **screen-relative**: W is always up, D always right. Never turns the ship |
| Q / E | Turn the nose by hand — Q counter-clockwise, E clockwise. **Only** the nose: Q no longer cycles hulls |
| SPACE / LMB | Fire. SPACE fires along the nose; LMB also swings the nose onto the cursor (twin-stick). A pilot who has not been taught to fly is held to the first flight (see Tutorials) |
| SHIFT | Boost, while thrusting — drains the energy meter |
| **F** | **Invoke an astra: HOLD F, let go to loose it.** Cubes gather round the hull and foes in front of the nose are locked; the longer you hold, the more cubes and the harder they hit. Five shards of its element a release, paid at the loosing |
| **X** | Choose which astra F invokes (Akasha, Vayu, Agni, Jala, Prithvi, or the Brahmastra of all five) |
| G / RMB | Shield, drains while held. Its cost scales with what actually lands |
| V / B | Next / previous owned weapon |
| 1–7 / Z | Fit that hull / the next hull you own — a hull this run has not bought is a no-op |
| ESC | The way out, everywhere: pause in flight, close the hangar, back from a page |
| R | New run, after a run's victory card (a click no longer does it) |
| F11 / F12 | Fullscreen / swap the window between 1280×720 and 1920×1080 |

Docking is automatic: fly into a shop bay and hold still.

Bay keys (while open): `TAB` next rail (HULLS / ARMS / ASTRA), `A`/`D` move along the strip, wheel or cards, `W`/`S` between the hull strip and its tier cards, `ENTER` buys or fits, `1-7` jump to a hull, `X` sells every shard, `ESC` closes. See *The bay* below.

**Dynamic zoom — speeding pulls the view BACK, never in.** libGDX's `OrthographicCamera.update()`
builds its projection as `setToOrtho(zoom * -viewportWidth/2, zoom * +viewportWidth/2, ...)`, so the
**visible world span is `zoom × viewportWidth`**. That makes `zoom > 1` the direction that shows
*more* world and `zoom < 1` the direction that *magnifies* — the opposite of the usual zoom
intuition, and the reason an earlier version zoomed IN when asked to pull back. The camera rests at
`ZOOM = 1.0` (1:1) and eases out to `ZOOM_OUT = 1.3` (1664 × 936 px visible), driven by whichever
asks for more: ship speed normalized against 520 px/s, or pressure (6+ hostile rounds within
620 px). It eases over ~1/3 s and can only ever move up.

Nose precedence is **cursor > Q/E > hold heading**: while LMB is held the nose tracks the cursor and
Q/E does nothing; otherwise Q/E turns it; let go and it keeps the heading it had. Thrust never
contributes to turning.

## Story, scenes and the ruler's gaze

The pilot is **Indra**, king of the gods, come down from **Swarga**. **Trivikrama**'s third step broke
the **Vajrayudha** - the bolt made of the sage **Dadhichi**'s bones - into seven pieces and hid one in
each loka. The ruler of each holds a piece, and a piece tells its holder that the world around him is his.
Nobody is a villain. The pilot beats each ruler, takes the piece and, in the end, forges the weapon whole.
The whole script is in `docs/SCRIPT.md`; the plan for the world is `docs/WORLD_PLAN.md`.

**The people** (`docs/NPC_BIBLE.md`): the blue figures on the map are Uparichara's Scout, the Ninth Maya
and Kaliya - people with sheets of their own (sky blue, lamp eyes, a halo), a gold `!` until met, a first
meeting that is a short cutscene, jobs kept on a **tracker on the HUD** (a panel of diamonds), a **toast**
when a job opens or pays, and a speech bubble over their hull for what they say after. Nothing floats
over the pilot's own hull any more.

**Cutscenes** (`scene/`, `screen/Director.java`): `World` only *names* a story moment
(`World.cue`); `GameScreen` stages it. While one plays the sim does not step and the HUD hides; two
black **letterbox bands** slide on, top and bottom. The **narrator speaks in the bottom band**, plain,
no box. Everyone else gets a **temple-panel bubble** beside them - chamfered corners, a double
hairline, rangoli diamonds in the cuts, a lotus-and-arch finial, a name tab, kolam dots on the sill -
sized to its line, so a longer line is a bigger box; the pilot's is a thought, tailed with stepping
diamonds. Text types at 24 characters a second. ESC skips, any other key reads on. In a shared race
scenes are skipped and only their effects are settled. Scenes: the opening, the grand gate (four
guards ring the pilot and ask who they are - **once, on the first entry to a new loka**, and only
then do they fight), and the gaze stages below.

**The ruler's gaze** (`World.gaze()`, `HudGaze`): the kill quota is no longer hidden. A bar under
the score fills as the pilot cuts down the ruler's people. At 25% his piece warms (a scene sweeps
to the empty last band), at 60% he speaks to the empty hall, at 100% he walks into the pit and
waits. Keepers (below) are not in the quota.

## The bay (the hangar, redesigned)

Temple panels, not menus (`BayUi`, `BayController`). Three rails, `TAB` between them: **HULLS** - a
strip of seven hull cards and, under it, the three tier cards of the selected hull. **Nothing you do
not own shows its look**: an unbought hull is a lock and its mark (MK-3), an unbought tier is a lock
and ???, until you pay. **ARMS** - the weapon wheel; a pattern the pilot's loka does not offer yet is
a lock, and the hub shows the chosen pattern's bars and its three **ranks** (ENTER on the pattern in
your guns tempers it: II +25% damage, III +50% and an ability). **ASTRA** - one card per loka. Gold is
yours, the accent is focus, dim is not yet. Pinned by `BayTest` and `EconomyTest`; prices, scaling
and the reasoning are in `docs/ECONOMY.md`.

## One UI

Every page wears the same kit (`Ornament`, `Template`, `KeyCaps`): the corner-bracket frame, a
wide-tracked gold heading flanked by diamonds with one divider under it, the **temple panel**
(`Ornament.panel`) for every box, W/S to choose, ENTER to confirm, **ESC always the way out**, and the
same footer key strip. The title menu says MULTIPLAYER and so does the page. `AllScreensTest` renders
each page into `build/screen-*.png` and checks the frame.

## Vitala, the second loka, on the roof of Atala (docs/VITALA_PLAN.md, docs/VITALA_ENEMIES.md, docs/TREASURE_CHESTS.md)

The game's world is the **campaign world** (`World(seed, World.CAMPAIGN)`): Atala, and **Vitala on its roof**, one continuous map 47,600 px tall. Bala's
gold door at the top of Atala opens straight into Vitala's Plunge; the pilot flies up into it and can fly back down. (`new World()` is still Atala alone, the
unit tests' world; the menu's `VITALA` row starts a campaign run with the pilot already in Vitala, `World.vitala()`.) Nothing swaps and nothing replays: the
opening scene plays once, at the start of a campaign run.

- **Size:** Vitala is 5,200 x 27,000 px (Atala is 5,200 x 20,600). Its yojana meter runs 0-10,000 over its own length (`Hataki.YOJANA_PX`); Atala's runs 0-10,000
  over its. `Hud.yojanaAt` and the depth tape follow whichever loka the hull is in.
- **The Hataki** (`world/Hataki`): a river of liquid gold, one meandering centre line (pinned to the axis at every gate), a width, a speed - pure functions
  of the world height. `World.tickVitala`: a drift in the pools, a hard pull in the Hearth Deep's rapids; enemies are carried too (not the boss).
- **Fire vents** (`world/Vents`, `screen/VentImages`): a jet across the river on a 3 s beat; the Altar's rim vents open in the boss's second phase. A lit
  shrine hushes the current and every vent for 6 s.
- **The side caverns** (`MapGen.pockets`, `VitalaGen.placePockets`): about thirty, each a **vault** (a treasure chest), a **den** (a chest and the ambush that
  guards it) or a **well** (an ember well that mends the hull once).
- **Treasure chests** (`world/Chest`, `World.openChest`, `screen/ChestImages`): every loka hides its own (Atala's pearl chests, Vitala's basalt ones); you fly into
  one and it pays coins and one useful thing. See `docs/TREASURE_CHESTS.md` for the table and the plan for the other lokas.
- **Enemies** (original bestiary names): Agneya, Jvala, Tamra, Kila, Dhuma, Rekha and the boss Bhairava, ships drawn in a second palette. They fly **their own brain**
  (`enemy/VitalaBrain`): formations (line, wedge, column, pincer, circle) that obey rank, and patterns - Agneya's dart run, Jvala's burning ground, Tamra's armour and
  body-block, Kila's needles, Dhuma's smoke, Rekha's hairline - with the ground they lay in `world/Hazards`.
- **Bhairava** (`enemy/BhairavaBrain`): a bone-and-ash capital ship (skull prow, third eye, ribcage hull, flame hair). An ash ring closes on the safe arena as he
  falls; the ash charge, the ember fan, the cinder rain, the third eye; the Flood carries every hull east. His fall lifts the gold seal and ends the run.
- **Stages:** `World.stage` (0 Bala, 1 Bhairava). Bala's fall opens his door and makes Vitala's ruler the quota's business instead of ending the run.
- **The script and the people:** `VITALA_ENTER` plays once as the hull crosses Bala's door; the three gaze scenes and the wake and fall scenes speak in Bhairava's
  words when `World.stage` is 1; two people stand in Vitala - the **Gold-Seller** (by the river in the Mouth; he is why the compass points at treasure) and **Gauri's
  Maid** (in the Bhuta Court; she tells of the six-second hush and the wells) - each with a first-meeting scene, a sheet and a Codex page (five new pages).
- **Hold your post** (docs/ENEMY_BIBLE.md, `PostsTest`): every enemy belongs to the band it was placed in. It wakes when the hull comes near, fights from its post or
  formation within a 760 px tether, and when the hull goes (1.7 wake ranges) it flies home and sleeps there, formation restored. Nothing crosses a gate or enters Bhu Loka;
  dives are short flanks within reach of the post; vents and the river no longer wake or float sleepers.
- **Heat** (`Ship.heated`): past the Plunge, boost drains x0.7, the guns cycle 10% faster with 3 degrees of scatter, the boost tube glows hot; a lit shrine cools it.
- **Layers:** each vent's flame stands up the shaft as a column on the three nearest layers, and the Hataki shows as a gold stripe on the nearest four.
- **Keepers:** the gate-bull, the fire-priest and the assayer have their own sheets; **chest markers** are on the compass dial.
- Tests: `VitalaTest` (the whole campaign is one connected road from the Atala spawn to Bhairava's sealed door; every shrine, shop, cache and chest is on it),
  `VitalaMechanicsTest` (beat, current, hush, phasing, names, the two stages and the boss), `VitalaBrainTest` (each character, formations and rout, the hazards,
  the chests, the wells, the opening not replayed, a soak flight through every awake court), `VitalaShotTest` (a picture per band and place, a frame guard).

## Enemies: the Atala bible and sheets

`docs/ENEMY_BIBLE.md` says what each of the nine Atala enemies is *for*, how it feels and how it
moves. They are drawn by hand (`tools/enemy_art/`, run `python tools/gen_enemy_sheets.py`) on
64×64 sheets, `assets/enemies/atala/<kind>.png`, in Atala's real colour code — pearl, gold, black
apertures, lavender for illusion, ember only in rage. Each sheet holds idle ×4, rage ×4, hit ×2,
death ×6 (gold glass) and the same for a leader (crest) and an elite (vents, halo). Beside the six
soldiers stand three **keepers**, enemies with a job: the **Dvarapala** (gatekeeper) leads every
interior gate's post, the **Dipaka** (lamp-keeper, mends comrades every 5 s) every checkpoint's, the
**Vanija** (bay-master) every shop's. `EnemyShowcaseTest` renders them all in the real game.

## Coordinates

The world is **y-up**: increasing y is screen-up. Bhu Loka sits at the bottom (y 0–1600), the boss
arena at the top (y 17600–20600), and you spawn at (2600, 800) facing up (π/2) and climb — up
through the grand `ATALA LOKA` gate at y 1600. `TileMap.row 0` is the bottom row of every map. The
UI camera uses the same y-up convention.

## World

`World.width = 5200`, `World.height = 20600`; `World.lokaName()` reports `Bhu` below the grand gate
and `Atala` beyond it.

| Region | Name | y start | y end | Tile map (ox, oy, cols × rows) |
|---|---|---|---|---|
| R0 | Avatarana — Bhu Loka | 0 | 1600 | none — open sky, no tiles |
| R1 | Svarna Dvara | 1600 | 4100 | 24, 1600, 161 × 79 |
| R2 | Maya Vana | 4100 | 7600 | 24, 4100, 161 × 110 |
| R3 | Svapna Mandala | 7600 | 12000 | 24, 7600, 161 × 138 |
| R4 | Bala Mukha | 12000 | 17600 | 24, 12000, 161 × 175 |
| R5 | Boss: Bala | 17600 | 20600 | 24, 17600, 161 × 94 |

`regionStart[]` / `regionEnd[]` / `regionNames[]` are plain arrays in `World` and are the source of
truth: the HUD reads `currentRegion()` from them, and death respawns you at your most recent
collected checkpoint, or at `regionStart[currentRegionIndex()] + 120` when none is collected. All
five maps are 161 tiles wide and centred (`ox = (width - 161 * TILE) / 2 = 24`), each runs from its
band's start at or past its band's end (no daylight gaps at the seams), and the boss map runs
17600→20608 exactly.

**Gates.** Every band boundary carries a full-width rock gate wall (`MapGen.gateWall`, x −8..5208,
so nothing slips past the edges) with one doorway on the centre column — the only place both
neighbouring caverns open — open for the wall's full height (a lintel row would seal it; the rock
above an arch can only be decoration, not structure). The two thresholds are grand: the Bhu→Atala
gate at y 1600 (7-tile half-door stepping in over the top two rows for a stepped gold arch, gold
pillars, `ATALA LOKA` in world font over it, LANCE + HATAKA honor guard hovering beneath) and the
boss gate at y 17600 (6-tile half-door); the three interior gates are straight 4-tile half-doors.
Each interior doorway is watched by a stationed three-guard post (`Enemy.guard`, not mirrored);
the grand entry is watched by the honor guard instead.

**Bala.** The band holds the boss: a 96 px, 1500 hp BALA parked at the pit centre (2600, 19104).
He is **sealed** until the run fills a per-kind kill quota — `World.quota[i] = min(placed, max(3, placed/8))`
sized off the roster itself, so it is always obtainable; kills anywhere count. The seal shows no
numbers anywhere — while it is unmet he is simply not in the world (`World.boss` stays null, not on
the roster), and the last required kill summons him silently into the pit (`trySpawnBoss` runs every
frame). The crossing (or a shot) then wakes him and `World.orderRetinue` pulls the whole band into
his ring. He runs his own state
machine (`BossBrain`): **the commander** (above 2/3 hp) holds the pit and only fires slow 5-shot
volleys, each release ordering his ring to fire together; **the duel** (below 2/3) stands the
survivors down — they walk out the nearest arena mouth and retire — then circles at 420 px cycling
volley → spiral → mortar → shockwave (the shockwave leaves two dodge lanes); **berserk** (below 1/3)
blinks to a new berth every few seconds, each vanish dropping an elite pair at the old post (cap 6,
they rejoin the ring) and reappearing 350–850 px out, and below 15% the spiral never stops. Phase
changes get a 1.5 s stagger (white strobe, hits ignored), a gold ring and screen trauma. His bar
rides **above his hull** like every other enemy's — appearing the moment you enter region 5 — but
bigger: a 240 px gold-framed, segmented fill tinted by phase (gold/ember/blood), white notches at
both thresholds. Killing him sets `bossDead`, starts the `ATALA CONQUERED` banner, dismisses what
is left of the retinue, pays 150 coins and opens the gold gate across the arena's north corridor.
Atala leads on only when Bala falls.

A scaled world map — 1 px = 8 world px, drawn from the live `World` with the real brick art — is
generated by `WorldMapImageTest` to `tools/atala_map.png`.

### Checkpoints and lives

Each Atala band (regions 1-5 — Bhu Loka has none) carries **one checkpoint**, placed on open ground at 60% of the band's depth
(spiral scan out of the band centre, clamped to its band). It is a **deepa-stambha, a stone lamp-altar** (`CheckpointImages`, painted
pixel by pixel at start-up, no art files): an octagonal cut-stone plinth with a gold inlay ring broken by eight spokes, eight gold
petals and a dark bowl with a gold lip, drawn at 72 px with a soft shadow. **Unspent, the lamp is cold:** a pink-white flame-seed hangs
over the empty bowl on a thread of light, bobbing, a slow ring of twelve pink diamonds turns round the altar and pink sparks spiral up -
a thing you can see from far off in the dark. **Fly through it** (48 px) and the seed drops in and the lamp **catches**: a gold ring
and sixteen embers burst, and from then on a steady, flickering gold flame with a warm halo burns in the bowl and gold embers lift off
it. A lit lamp is where you wake if you fall. The lamps are also the world's light: a warm 250 px pool from a lit one, a faint pink one
from a cold one (in the lightmap), and a glow at the lamp's spot on each of the six nearest layers of the stack, so you see one
shining down the shaft long before you reach it. `lwjgl3/.../CheckpointShotTest` writes `build/checkpoint-0-cold.png` and
`-1-lit.png`.

You have **3 lives** (`MAX_LIVES` 5 is the most you can hold). A death costs one and drops you at the most recent collected checkpoint
(the last one wins across bands); with none collected it falls back to the region entrance.
There is no game over: the counter falls to zero and stays there, and the hull still wakes at the last lamp.

### Caches

Each Atala band (1–5, none in Bhu) carries **one cache**, placed **just inside the band's entrance
gate** — 400 px past it, on open ground (spiral scan). **The cache is never drawn**: no crate, no
label, nothing on screen tells the player one exists — you only learn something is there when the
squad jumps you. *(Development-phase note: this is deliberate; a production build must keep caches
invisible — re-add the shapes pass only to eyeball placement while building.)*

Cache state is **per player seat** (`World.cacheState[SLOTS][5]`, `SLOTS = 4`) — seat 0 is the
solo player; in live co-op the caches stay host-seat places for now (see MULTIPLAYER.md's debt
register).

Touch a sealed cache and every living unit of that band within `AMBUSH_R` (1000 px) wakes and
**hunts**: straight at your ship at cruise speed, no ring, no orbit, no leash — only the local
squad hears the call; everything past the radius is left alone. **Shop and checkpoint guards are
exempt** (`Enemy.siteGuard`) — they hold their sites and never answer a cache. If nobody qualifies,
the cache is an empty pocket and opens straight away. Clear the hunters
(`World.resolveCaches` checks the band each frame) and the cache flips to OPEN; touch it then for
the payout — **100 coins and +1 life** (once; touching again does nothing).

### Shops

**Vanija's lotus berth.** Each Atala band 1-4 carries **one shop** (none in Bhu, none in the boss band) at 40% of the band's depth, always
above the checkpoint at 60%, alternating right/left of centre (bands 1,3 on the right at x~3700, bands 2,4 on the left at x~1500), with
the door approach kept clear of rock. It is the bay-master's quay, cut in the walls' own ivory stone and gold and PAINTED IN CODE
(`ShopImages`, no art files): a chamfered stone lintel with a gold border and cornice hangs on two lotus-capped pillars; its face is
Vanija's own emblem, the **tula** (the merchant's balance - two pans, a coin in one, a pearl in the other); two lamps hang from it, and
two gold lamp-posts carry six lamp-pips each. Under it, in the marked dock square, is the **berth**: a round stone dais with eight
studs and a lotus of eight gold petals round a dark mandala. The sign is the same 128x128 footprint as before, so the board and its legs
are still solid (you cannot fly through them), and the whole sign stands inside a **rock compound** (a 12x11 tile ring wall with gold
corners, carved out of any region or gate rock behind it, a four-tile door directly under the bay; the floor under the sign stays open
and you rise in through the door).

**A hull is drawn INTO the berth.** Enter the marked square and the berth pulls the hull gently to its centre (its speed bleeds off and
it slides home, never leaving the square, so the pull cannot cancel the dock) while the six lamp-pips light one by one and the eight
petals take the light in turn; two **gold clamps** close on the hull's flanks (drawn over it) and a steady warm bloom settles over
the berth. Leave, and the clamps let go and the lights go out. The sign's two lamps are amber lights in the lightmap, the berth a warm
250 px pool once a hull is seated, and every shop shines amber down the six nearest layers of the stack so you can find one from
far away. `lwjgl3/.../ShopShotTest` writes `build/shop-0-parked-out.png`, `-1-settling.png` and `-2-docked.png`.

**Docking is automatic.** Get the *whole* hull between the two dock-light posts — the marked
square below the sign (`World.inDockBox` — a graze does not count) — and the dock sequence runs: `shop_doc1`…`shop_doc6` fill the bay lights over 0.6 s,
then **the hangar opens by itself**. The sign swaps to `shop_undocked` the moment you leave the bay;
leaving mid-sequence cancels it. ESC / ENTER closes the overlay (the world is paused while it is
open), which parks the bay as `SITTING` — it stays lit until you fly out, so it cannot reopen in
your face. Direct controls (`1-7` ship, `V/B` weapon) keep working everywhere.

**Site guards.** Every shop and checkpoint (Atala bands 1–5 — nine sites) holds three
posted guards: a rank-1 leader standing on the site with two sentries flanking below it
(`World.guards`), all `sentry` with a 700 px wake, kinds graded by band (`World.GUARD`: vakra /
auris in Svarna Dvara up to mayavi / bhavi in the boss band). Bhu Loka — where you spawn — holds no
shop, no checkpoint, no guard and no wall: bare sky up to the grand gate.

### Loot

Every death pays (`World.dropLoot`, called from `Enemy.hurt` — retires and stand-downs pay
nothing). The whole purse leaves as **one fanned burst**: coins, rolled health and the shard
(or a boss's piece) each take an even slice of a ring at 0.5–1.0× the corpse's width (kept
clear of rock), and the fan rotates kill to kill — nothing piles on anything. Drops
decelerate under exponential drag to a stop, then float until the hull touches them
(collect radius 20 px = hull half 12 + coin half 8). The five `assets/coins/coin_1..5.png`
variants all pay 1 each, rolled per coin:

| Source | Coins | Health | Shard |
|---|---|---|---|
| rank 0 regular | 2 | 8% | 4% |
| rank 1 commander / posted guard | 4 | 15% | 10% |
| OrnamentTest | 3 | 3 | 3 | 3 | 3 || 3 || 3 || 3 || 3 || 3 | **the shared ornament resolves**: the class the title and pause screens both call actually exists and offers every method they use, and the geometry constants moved out of HomeScreen with it — a deleted helper fails here instead of at the next compile |
| KeyHintsTest | 7 | **the control strip**: centred, and it CLEARS the weapon plate and the shard block (if either neighbour grows into it this fails rather than the two quietly overlapping), it sits low and under the depth tape, **the F cap reads the real Astra state** - not forged / ready / banking / cooling - **charging outranks everything**, the track shows what it is filling toward, and it reads the loka's own element so the cap agrees with the corner || ShardGaugeTest | 7 | 7 | 7 | 7 | 6 | **the shard block draws a row per element the LOKA pays, not one per tatva** - Atala pays Akasha alone so it is a single row, and it grows to two at Rasatala and five at Patala; every paid element has a colour and art; the block stays clear of the depth tape and fits the screen; the charge track is exactly as wide as a release costs, so one segment is one shard; and it reads forge progress - 3 of 5 fills three segments, forging does NOT spend the pile, and selling is the only way to lose ground |
| SplashScreenTest | 7 | 7 | 7 | 7 | 7 || 7 || 7 || 7 | **the studio card's beat**: nothing is drawn through the 1.2 s delay, the logo is at 0.5 halfway up the rise and 1 at the top, holds at full alpha and falls back through 0.5 to 0, the hold is 5 s and the outro 2 s and slower than the rise, the logo is drawn 1.5x smaller (373 px), the curve never leaves 0..1, and it is not done a frame early | the field runs ink -> pale -> ink so the handover is not a flash; both curves stay inside 0..1; it is not done a frame early; and any input skips to the end |
| elite | 6 | 30% | 20% |
| Bala | 25 | 100% | the Vajrayudha band, 100% |

Health (`health_drop/health.png`, drawn at 2×) repairs 25% of max hull, capped — at a full
hull it waits on the spot. Shards key off the **loka, not the foe** (`World.LOKA_SHARDS`, from
LOKA-LAWS §3): Atala pays Akasha, Vitala Agni, Sutala Jala, Talatala Prithvi, Mahitala Vayu.
Shard art: `assets/shards/shard_1..5.png` in pancha order (1 Akasha, 2 Vayu, 3 Agni, 4 Jala,
5 Prithvi), 16×16 drawn at 2.5× (40 px) so the drop reads off the rock.

**The Vajrayudha.** `assets/vajrayudha/vj.png` (32×32) is cut into seven horizontal bands —
band *i* = loka *i*, top band Atala, bottom Patala (`VajraImages`). Each loka's architect drops
his band as a world pickup; all seven → `World.vajraDone`: the world freezes on "THE VAJRA IS
WHOLE", `R` starts a new run. (The HUD progress plate that showed the blade forming was cut
with the top-right pass; the pickup band itself stays.)

**Pickups have a lifetime.** Nothing stays on the ground forever: `Pickup.LIFE` = 40 s, and the
last `FADE` = 4 s are drawn dimming via `Pickup.alpha()`. A band you cleared and walked away from
does not leave a floor of coins behind.

**Magnetism.** Loot inside 90 px tugs toward the hull (`World.PULL_R`, accel `PULL_G` scaled to
500 px/s at full power), force scaled by `Hangar.magnet` — fixed at **15** for dev builds so
loot reaches the hull. A purchase row can land later when the hangar is redesigned. Coins,
health, shards and bands all feel it.

**Shard economy.** x.3 tiers cost their coin price **+ 3 shards** (`Hangar.SHARD_COST`, spent
from the deepest stack first); `[X]` in the hangar dumps the whole stock flat at 20c per shard
(5 shards went for 100c). x.2 stays coins-only, and switching marks is still a free hotkey
until the per-loka unlock rules land.

**What the shards are FOR.** Coins buy stats (tiers). Shards are **the fuel of the astras**, and nothing else:
five shards of an element pay for one release of its astra. There is no forge to buy and no stored charge - the shard
pile *is* the fuel, which makes selling shards a real choice against keeping them (`X` in the hangar sells the lot).

### The Astra rite

An astra is a divine weapon called down by an invocation, aimed, and loosed. The rite has three beats and one price
(`vajra.world.Astra` holds the numbers; `World.astraBegin / astraLoose / astraCancel` run it):

1. **Invoke - HOLD F.** A cage of cubes gathers round the hull (a third of them at the start, all of them at full
   strength after `WIND_FULL` 1.5 s) and the hull goes heavy (`WIND_THRUST` 0.4 on the stick). Foes *in view, within
   900 px, nearest the line of the nose first* are **locked**: four corner brackets round each, as many locks as cubes.
2. **Aim.** The nose is the aim - there is no second cursor to manage.
3. **Loose - LET GO.** Each cube flies at a locked foe, round and round the locks; they steer and **go through rock**.
   Let go before `WIND_MIN` 0.4 s and the **mantra falters**: nothing flies and nothing is spent. Let go with nothing in
   front to answer and it falters too.

**The price** is `COST` = 5 shards of the astra's own element, paid *at the loosing, not at the start* - a hull that dies
(or pauses, or docks) mid-invocation loses nothing. A short `COOLDOWN` of 6 s follows a release (0.5 s after a faltered
one, so a stuttering key cannot flicker the cage). **Power** runs from 0 at the shortest invocation that counts to 1 at
full strength: cubes `0.35 + 0.65 k` of the kind's full count, damage `0.7 + 0.6 power` of the listed.

**The astras** - five elements, each its own character, and a sixth:

| Astra | Epithet | Cubes | Hit | What it does |
|---|---|---|---|---|
| Akasha | the Hunter | 16 | 70 | seeks its prey through rock; **the shield is refilled** |
| Vayu | the Gale | 10 | 55 | fast (1500 px/s); **chains** to up to 3 further foes; **the boost is refilled** |
| Agni | the Pyre | 8 | 85 | **bursts** on impact, 60% of the hit to everything within 120 px |
| Jala | the Flood | 12 | 50 | **every hit mends the hull** by 3 |
| Prithvi | the Mountain | 6 | 150 | few, heavy blows; **2 s of stone skin** (no damage) |
| Brahmastra | the Last Arrow | 26 | all five | needs **5 shards of every element**; half of each astra's cubes, all five at once, shield and boost refilled and a second of stone skin |

There is **no free repair and no extra life** any more: the old release mended the hull whole and handed back a life
every time, which made it a panic button you could farm. Boons are of the element, and Jala's is its blows.

**Choosing.** `X` steps `World.astraSel` to the next astra you can pay (it wraps); a choice that cannot be paid falls
back to the first that can (`loadedAstra()`). The hangar's **ASTRA** rail is six cards - the element's own gem (the same
art as the shard pickups and the HUD; the Brahmastra wears all five), its epithet, the releases you can pay for, what
it does - and ENTER on a card chooses it.

**In a shared flight** the guest pays its own shards and keeps its own boons; the `ASTRA` plane carries
`[kind][power][count][foe ids]` so the host looses the same volley from that seat's hull, and the kills pay the guest.

**How it looks** (`AstraFx`, drawn additively so it burns): the cubes are gone. Holding F opens a slow gold **yantra**
(two counter-turning triangles in a ring) round the hull that tightens as the charge builds and, once full, spills a pulse
ring; the held shards are glowing **orbs** on the shrinking circle, each dragging an arc of light. Loosed, each shard is a
**bolt of its element** with a tapered trail drawn from the positions it flew through (`AstraCube.mark`): Akasha a
spinning star-diamond, Vayu a thin gale with two wind-wisps, Agni a guttering fireball, Jala a teardrop with a rippling
wake, Prithvi a stone shard shedding dust. A strike throws two rings and a burst of sparks in the element's colour.
`AstraCube.hold/arm` is still one entity in both states, the cage and the volley.

`AstraTest` (32 tests) pins the fuel, the invocation (holding builds the cage, letting go too soon is free, a dead hull
loses nothing, a pause cancels for free, the cooldown), the aim (locks follow the nose, out-of-view foes are never
locked, nothing locked means nothing spent), each astra's character, the Brahmastra, the choice and the wire.
`AstraScreenshotTest` renders the cage, the locks and the volley on the real Flow.

### Save

`vajra.save` holds exactly two fields: `version` and `best`. That is deliberate — the save file is
**VERSION 2**, and v1 carried the whole hangar. Nothing else survives a run, so a new game resets
coins, hulls, tiers, patterns and shards to their defaults and the only thing
carried forward is the best score.

`SaveGame.deserialize` ignores a corrupt file or a foreign version rather than failing, and clamps
`best` into `[0, Score.MAX]`. It sets `JSON.setIgnoreUnknownFields(true)` in a static block, because
libGDX's `Json` throws on an unknown key and a v1 file read as v2 would otherwise be fatal.

### Profiles

`profiles.json` is the roster: each pilot holds a name, their own settings, and two career books —
`campaign` (runs, kills, points, best, seconds) and `multiplayer` (carried, written the day that
game exists). `vajra/profile/Profiles` owns it: `endRun(world)` is THE way a run ends, folding the
run into the active pilot and calling `SaveGame.finish` in the same breath so the two files can
never drift; the run-end code in GameScreen and PauseScreen both call it, nothing else calls
SaveGame directly. Settings moved out of their own `settings.json` into the profile — each pilot
keeps their own shake and popup scale, changed values persist at once, and a stale settings.json
from an older build is simply ignored. The PROFILE row on the title screen opens the pilot's page
(name, ledger, switch / rename via the platform text dialog / new pilot); the roster wraps and is
capped at 8, corrupt files fall back to one fresh PILOT, and rename sanitizes (uppercase, safe
characters, 12 chars).

### Netplay and voice

**One button hosts. One panel joins. Nothing is installed, nothing is forwarded, no address
is read off a screen.** Phase 1 of `docs/MULTIPLAYER.md` is built: a home MULTIPLAYER row
opens the netplay gate, and the run is a host-hub UDP session — one envelope for all control
packets (JSON, tag 0), 20 ms μ-law voice frames (tag 1), heartbeats every 2 s with a 6 s
silence sweep, and a host-locked run seed as the handoff handshake. The voice channel is
push-to-talk (hold V) with graceful degradation — no mic means listen-only, no speakers mean
talk-only — and each lobby roster shows a lit diamond per audibly-talking pilot.

**HOST A RUN** does three things at once: it binds the game's UDP port, it **shouts the room
on the LAN** (`LanBeacon` — a one-second beacon carrying the room code, the pilot's name and
the port, so a friend on the same network finds the room from a list instead of typing an
IPv4), and it **opens the door on the router in the background** (`Nat` — UPnP SSDP + SOAP
`AddPortMapping`, then NAT-PMP, then STUN for the public address). The lobby prints one
code, `ABCD@203.0.113.9:26000`: the room and the door in one string.

**FIND A RUN** lists the rooms that can be found and takes one
link for either network. The joiner knocks **directly** on the address the link carries; if
nobody answers it looks the room **up by its four letters on the network it is on** and
knocks at the LAN address the room shouted, and only then falls back to the relay — so
**one link serves both networks** and the friend never has to know which kind of door the
host opened. If the router refuses every mapping, the host's lobby offers **R** and the game
starts the **bundled relay** on the host's own machine — no .bat file is ever asked of
anybody. The whole protocol is headless-testable:
`SessionTest` walks the handshake over a paired `LoopbackTransport` with a fake clock,
`VoiceCodecTest` pins the μ-law roundtrip, and a single `UdpTransportTest` smokes the real
localhost wire.

Phase 2, shadow races, is built too: when the host starts, every pilot's lobby hands
off into the **same seeded run** (`World`'s orb and loot chaos grow from the session
seed; the sim advances in fixed 1/60 s steps, so the race flies identical frames
everywhere). Each pilot's flight stays local; the session's book keeps every pilot's
best flight and posts it in the lobby roster, and pause-quit returns a pilot to the
still-running lobby to ready up for the next race. `DeterminismTest` is the gate: two
worlds, one seed, one scripted pilot — the same score after 60 flight-seconds.

Phase 3, **live co-op**, is built: the host flies into the seeded run and its `World`
runs the whole fight; the guest's hull arrives as a live mirror both pilots can see
(`SHIP` planes at 30 Hz, pose birth-stamped and aged on the wire), the fight broadcasts
as 10 Hz `SWARM` snapshots (the foes and loot **nearest any live seat**, with the foe's
armour, so the fight in front of you is whole — the guest never sees a foe twice or
resolve one itself), rounds announce themselves as `SHOT` events, the host's verdicts on a
guest hull relay home as `PAIN` the guest's own hull answers, and a guest's **Astra
release is armed by the host** on the same foes (the `ASTRA` plane) so the signature
weapon is a real weapon on every seat. Books stay per-pilot; the guest's shop, magnet and
score are its own; everything else is the host's word. **Four pilots fly a room now**:
the seats are coloured pink (host), cyan, yellow, white — a pilot's hull wears their
colour's bar beneath it, and the lobby table lists every pilot
with a green READY / red NOT READY status, their round-trip ping and their best flight. A
ready solo host can START alone; the latch on unready rosters stays. In flight the HUD
carries the crew block under the vitals line (left): one row per pilot, their name in
their seat colour and their round-trip ping in the OS's own Arial — green at/below 90 ms,
red above, `-` until measured (and `-` on your own row: a delay to yourself has no
meaning). The ships themselves show only the colour bar, no name; `SessionPingTest` is the
ping gate. `CoOpTest` and `CoOpStreamTest` are the flight gates: two loopback
sessions with 60 ms per-hop latency, mirrors within half a tile of each other, three
seats seeing each other, and one round of `SHOT`/touch/kill-pay/sweep/run-over
convergence.

Phase 4, **the internet relay**, is built: `HOST ONLINE` registers a room on a relay box
and the invite names the room *and* the server it lives on, `ABCD@<host>:27040`, so the
joiner dials the host's machine directly. A hand-run public relay box stays the
no-forwarding alternative: aim both sides' defaults at it (relay.txt/-Dvajra.relay, default
127.0.0.1:27040) and share plain `ROOM:XXXX`.
`RelayTest` walks a real localhost server end to end (join, packets both
ways, refuses, the invite dial); text chat and cross-session career sync stay in the
debt register.
**For a step-by-step play — LAN and over the internet — follow `docs/PLAYTEST.md`.**
**Updating in place:** upgrades never need a delete-first — the setup stops the
running game and its auto-relay of that install before copying, and the home
screen's UPDATE row does the whole swap from inside the game: it reads
`updates.txt` beside the game (one line — an http(s) URL or a plain path; this
dev tree's own `build/installer/VajraOverdrive-setup.exe` works as a source),
fetches the new setup, compares its build stamp (`app/gamever.txt`, stamped by
gradle, answerable via the setup's `--version`) with the installed one, and
when they differ installs silently over this install and relaunches. A setup
that won't state its stamp is refused rather than installed. A hosted room
dies with the upgrade; the relay restarts with the next session.
**The multiplayer law, whole (read this before touching NetScreen/Net/Nat/LanBeacon):**
everything runs on ONE link and ONE screen with exactly two verbs. **HOST A RUN** binds the
game port, shouts the room on the LAN every second (`LanBeacon.Host`), and opens
the router's door in the background (`Nat`: UPnP, then NAT-PMP, then STUN). The
lobby prints one code — `ABCD@<public>:<port>` when the door opened (the typed
invite works the same on every network), `ABCD@<this-machines-LAN-ip>` when it
did not; pressing **R** in the lobby moves the live room onto the bundled relay
(the relay's UDP 27040 is now the door being opened) and the printed code
follows without any new hosting steps. **FIND A RUN** lists the rooms the wire
hears — and, when a link is set, the rooms that link's relay holds
(`RelayServer` answers `LIST` with its live-room table; the panel merges both
lists). ONE typed row does the rest, by shape:
- `ABCD@ip:port` — the invite (joins directly, falls back to the relay at the
  same address — never mind which door the host opened);
- a bare `ROOM:ABCD` — the code is looked up on the LAN first, then the linked
  server is dialled (an UNLINKED machine is told what to type instead of
  dialling its own loopback);
- a bare `ip:port` — joins the GAME door at the game's port (26000): the address
  itself is the room, and the pilot walks in when the host answers; any other
  port sets the link (`relay.txt` beside the game, applied live), and that
  server's rooms list. There is no separate SERVER LINK row.
The link lives **in the game**. Friends never run `run-relay.bat` — that bat
starts a SERVER; only the host's machine needs one, and the game starts it by
itself when its dial finds silence and the target is this machine.

To hand the game to a friend, `gradlew installer` builds **`build/installer/VajraOverdrive-setup.exe`**
— a self-contained Windows **setup wizard** (welcome → terms of play → install path →
options → install → finish; compiled with the C# compiler that ships inside Windows,
so no .NET download is needed on the friend's machine). The whole game rides inside
the exe **and so does Java**: a jlink-built Temurin 17 runtime (`tools/jre`, ~50 MB)
ships with every install, so the friend's machine needs nothing at all — no Java
search, no adoptium.net trip. (Builders without `tools/jre/win` staged simply get the
old behaviour: the installer asks for Java 17. They double-click it, walk Next-Next —
the install folder defaults to
`%LOCALAPPDATA%\Programs\Vajra Overdrive` (no admin rights), the shortcut choices are
ticked, and — for online play — the relay address goes in the options step. Shortcuts
open `play.bat`, which prefers the bundled runtime, `javaw` window included. The
desktop and start-menu icons wear the mk1 base ship. The build Authenticode-signs the
exe (self-signed: valid signature = untampered; the trusted-root chain needs a paid
cert) and drops a SHA-256 sidecar (`...-setup.exe.sha256`) the friend can verify with
`certutil -hashfile VajraOverdrive-setup.exe SHA256`. `setup.exe --silent` runs the
same install with no UI, for scripts. `gradlew makeSetup` also still builds the plain
zip (`build/setup/VajraOverdrive-setup.zip`) for Mac or offline carrying: unzip, run
`install.bat` (Windows) or `sh install.sh` (Mac). On a Mac, `gradlew macDmg` (or
`sh tools/setup/make-mac-dmg.sh` on the unzipped package) builds the native
**`VajraOverdrive.dmg`** — drag Vajra Overdrive onto Applications, done; built through
`gradlew macDmg` it also carries the matching Mac runtimes (arm and Intel, ~50 MB
each) and answers Java questions itself. The game's
window and taskbar carry the mk1 ship too, and the title reads **Vajra Overdrive**.

### Tiles

32 px grid (`TileMap.TILE`), IDs: `0` EMPTY open sky, `1` ROCK, `2` GOLD trim, `3` DARK depth.
Outside the grid is open. All wall art is the user's hand-drawn set — sixteen 16 px images
`assets/atala/walls/Wall_Tiles/tile_00..15.png` (cut from `Wall_new.png`), drawn at 32 px,
nearest-filtered, loaded directly (no derived sheet). The set is a DUAL-GRID, not a per-cell
autotile: each tile encodes the 2 x 2 pattern of cells around a grid VERTEX
(NW/NE/SW/SE), `(NW?8)|(NE?4)|(SW?2)|(SE?1)`, and is drawn centred on that vertex — one
solid, one open, four one-land rounded corners, four three-land inner corners, four straight
edges, two diagonal connectors, exactly sixteen. `tools/fix_wall_tiles.py` decodes the tile →
pattern table, verifies every neighbour seam against the shared cell column/row (canonical
8/8 half-split), snaps deviant border pixels to match (`Wall_Tiles_handdrawn/` holds the
originals) and prints the analysis; `TileImages` walks camera-visible vertices, maps pattern
→ file with `P2FILE` and draws every tile at its NATIVE colours — no tint; the hand-drawn art
itself carries gold trim and dark depth. A missing file falls back to flat squares per id,
never a crash.

Collision is push-out, not damage: `pushOut` runs 4 passes and ejects a rect along the smallest
penetration axis; walls slide, nothing hurts you.

Generation (`MapGen.cavern`) runs in order:

1. Bilinear value-noise lattice + flank bias → solid toward the edges, open along the centre
2. Force outer columns solid; force a centre gap on the top and bottom row
3. `smooth` ×2 (3×3 majority), then `removeSpecks` (delete unanchored rock blobs under 8 cells)
4. `carveChambers` — `rows/16` bands, each an ellipse chamber (first and last widened ×1.5),
   joined by 2–3 tile necks, with a mirrored side pocket on each side; optional gold seams across
   necks that look solid but hide two notches (Maya's lie)
5. `trim` — rock touching sky becomes GOLD, rock buried under a 5×5 window becomes DARK

Per-region parameters (seed, fill, vertical stretch, seams):

| Map | seed | fill | stretchY | seams |
|---|---|---|---|---|
| Svarna Dvara | 11 | 0.78 | 1.0 | 0 |
| Maya Vana | 22 | 0.78 | 2.2 | 1 |
| Svapna Mandala | 33 | 0.80 | 1.5 | 2 |
| Bala Mukha | 44 | 0.74 | 1.2 | 2 |
| Bala arena | 55 | — | — | — |

The arena is not a cavern: an open pit ringed by a noise-wobbled rock annulus
(`baseR = min(cols, rows) * 0.38`, ±3 wobble), with the cardinal axes left open so there are four
passages through it.

## The seven lokas

`world/LokaTable.java` is the spine: **one row per loka**, and it is what a new loka is. Each row
carries the loka's name, the warden who holds its Vajrayudha band, which Pancha Tatva it pays, the
difficulty multipliers it grades its roster with, how many hulls it opens for purchase, and the
seed its bands generate from.

Scripture: the Vishnu Purana puts the seven nether worlds 70,000 yojanas below the surface, each
10,000 deep — Atala, Vitala, Nitala, Garbhastimat, Mahatala, Sutala, Patala. The Bhagavata and Padma
Puranas, whose ordering this game uses, call them Atala, Vitala, Sutala, Talatala, Mahitala,
Rasatala, Patala. Below all seven lies Naraka, and the seven rest on the head of Shesha.

| # | Loka | Warden | Pays | Hull × | Fire × | Buys hulls 0… | Seed |
|---|---|---|---|---|---|---|---|
| 0 | Atala | Bala | Akasha | 1.00 | 1.00 | 1 | 11 |
| 1 | Vitala | Bhairava | Agni | 1.35 | 1.20 | 3 | 21 |
| 2 | Sutala | Takshaka | Jala | 1.75 | 1.45 | 5 | 31 |
| 3 | Talatala | Kalanemi | Prithvi | 2.30 | 1.70 | 6 | 41 |
| 4 | Mahitala | Hiranyaksha | Vayu | 3.00 | 2.00 | 6 | 51 |
| 5 | Rasatala | Puloman | Agni, Prithvi | 4.00 | 2.35 | 6 | 61 |
| 6 | Patala | **Trivikrama** | all five | 5.50 | 2.80 | 6 | 71 |

`hpScale` / `dmgScale` are the **difficulty curve**: they ride the enemy at construction
(`World.add`), so a deeper loka's roster is meaner with no new behaviour code. The player answers
them on the two axes the hangar already owns - coins (hulls, tiers) and shards (the
Astra). Atala is the easy loka (1.00× hull, 1.00× fire); each step down the curve is harder.

Only **Atala** is built — it is the easy loka, the tutorial climb. Each of the other six needs,
beyond its row here: a `MapGen` cavern, a `WorldGen` placement pass, art, and a boss.

**Bands are not lokas.** Atala owns every region in the current build, so
`World.lokaOfRegion = { 0, 0, 0, 0, 0, 0 }` maps all six to loka 0. Treating the band index as the
loka index would switch Atala's shard economy to Vitala's element halfway up the world — that was a
real bug.

### What the scriptures give Atala to build with

- **Bala rules it** — the Puranas make him Atala's ruler, son of Maya. The band is named for him and
  he is the boss, which is already what the game does.
- **Two named cities** — the Vayu Purana puts the daitya **Namuchi** and the naga **Kaliya** here.
  Two of the five bands could carry these names, with their gates as the approach.
- **It is not a hell.** The Puranas describe the Patalas as sunless but *rich* — palaces studded with
  jewels, rivers and lakes, metals in the rock. Atala's current read is bare wet cave; the scripture
  supports jewel-veined walls and standing water, which is a `TileMap` id and a plant pass, not a
  redesign.
- **It belongs to the nagas and the daityas.** The garrison is six abstract kinds; the lore wants
  serpent-kin and asura-kin in the same band, which is the cheapest route to a *named* bestiary.
- **Below Atala is more of the same, then Naraka.** That is the whole seven-loka ladder: one realm's
  character at a time, sinking, with the floor of the world getting worse rather than the enemies
  just getting bigger.

## Ship

24×24 collision box, centre-based coordinates. Starting hull, boost and shield are all
per-frame numbers in `Ship`:

| Constant | Value |
|---|---|
| Friction | 9/s |
| Boost | ×`boostMult` on accel and top speed, drains `BOOST_DRAIN` 24/s, regens `BOOST_REGEN` 7/s — **4.2 s of use off a full bar, ~14 s back**, cap 100, only while thrusting |
| Shield | drains 18/s while held, regens 10/s, cap 50 + upgrades. A block costs **`SHIELD_PER_DAMAGE` × damage**, floor `SHIELD_MIN_COST` 5 — so a Hataka betray pellet is cheap to eat and a Bhavi shell is not. The meter prices the threat instead of charging a flat toll for anything |
| Hit | 0.6 s invulnerability, +0.6 camera trauma; shield block adds +0.3 |
| Death | a life spent; full hull, trauma 1, respawn at the most recent checkpoint (else the region entrance), 1.5 s invulnerability. Zero lives changes nothing: there is no game over |

Trauma decays at 1.6/s and drives camera shake (`±16 px × trauma²`). dt is clamped to 1/30 s.
Shield hits cost no hull. There is no dash. There is no ammo, no reload, no resource other than these meters.

**The dip.** There is no halo or state ring around the hull any more. When the ship is hit,
or shielding, the sprite is drawn `DIP_PX` = 5 px **low** and eases back up over ~1/16 s
(`Ship.dip`, held for `DIP_TIME` 0.35 s). Feedback without an outline.

**The plume is anchored, not eyeballed.** `ShipRenderer.tailPoint` puts the flame `PLUME_ANCHOR` =
23 px back along the exhaust axis from the hull's *drawn* position. Three things are easy to get
wrong here, and were:

- the anchor clears the drawn **art** (SIZE 40, half 20), not just the **collision box** (24, half
  12). Anchoring on the collision half tucked the flame *underneath* the hull sprite;
- the offset sits **inside** the rotation term (`centre − (cos, sin) × anchor`), so it turns with the
  ship. A separate screen-space nudge subtracted from `y` regardless of heading and shoved the flame
  back into the hull whenever the nose pointed down;
- it reads `drawnY(s)`, so a hull dipped 5 px by a hit or a shield takes its flame down with it
  instead of leaving the two sliding apart.

`PlumeAnchorTest` pins all of it headless: the anchor is exactly `PLUME_ANCHOR` back at every
heading, always on the exhaust side of the nose, above the hull when the nose points down, and it
follows the dip vertically only.

### Ships (7 — you start with one)

Every hull except Mk-1 is **purchased**, and each loka opens more of them (`LokaTable.unlockShip`
gives the highest ship index buyable there). A run starts in Mk-1 with 0 coins and nothing else:
`1-7` on an unbought hull does nothing, and `Q`/`Z` cycle only through hulls you actually own.

| # | Name | Cost | Opens at | Flavour | HP | Accel | Max spd | Turn °/s | Boost | Colour |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Vajra Mk-1 | free | — | the first blade | 100 | 1850 | 375 | 180 | 1.8 | gold |
| 2 | Kite Mk-2 | 250c | Atala | fast as rumor | 85 | 2250 | 435 | 200 | 2.0 | cyan |
| 3 | Aegis Mk-3 | 320c | Vitala | the wall that walks | 150 | 1550 | 330 | 160 | 1.6 | green |
| 4 | Hive Mk-4 | 380c | Vitala | swarm logic | 100 | 1850 | 365 | 180 | 1.8 | orange |
| 5 | Lance Mk-5 | 460c | Sutala | reads the gap | 95 | 2000 | 405 | 220 | 1.8 | pale cyan |
| 6 | Mortar Mk-6 | 560c | Sutala | arrives late, loudly | 120 | 1700 | 350 | 170 | 1.7 | rust |
| 7 | Vajra-7 | 700c | Talatala | the seventh halo | 110 | 2350 | 470 | 240 | 2.0 | white |

Each ship has three tiers: **x.1 is the hull itself** (you bought it), x.2 and x.3 are bought on top
— x.3 wants coins **and** 3 shards. Multipliers (cooldown, damage) multiply together;
bonuses (speed, turn, hull, shield) add. Derived stats: `maxHp = def + hullBonus`,
`maxSpeed = def + speedBonus`, `turnRate = radians(def + turnBonus)`, `shieldCap = 50 + shieldBonus`.

**Power scaling.** Tier magnitudes are set against the loka curve, not in a vacuum. Tier 3 is worth
roughly +40–60 hull or ×1.25–1.35 damage — enough to matter against the next loka's `hpScale`, not
enough to skip it. A hull you have not bought and tiered falls behind band by band; that is what the
coin ladder is for.

**Pace.** Base accel and top speed are deliberately low (`FRICTION` 9/s against a 1850 accel puts
Mk-1's cruise near 205 px/s). The game is tuned around a hull that takes a moment to cross a
chamber, because every enemy fires **one** bullet and the fight is about reading it rather than
outrunning it. Tier speed bonuses stay small so nothing on sale turns this into a different game.

Art comes in three variations per ship — `assets/vajra_ships/mk_<n>/ship_<1|2|3>.png`, painted from
the silhouettes in `assets/ship_mask/` (regenerate: `python tools/gen_ships.py`). The variation you
fly is the highest tier you own, so buying an upgrade changes the look. Primary/secondary colours are
the loka palettes: mk_1 Atala pearl/gold, mk_2 Vitala ember/gold, mk_3 Sutala violet/lilac,
mk_4 Talatala amber/cyan, mk_5 Mahatala sand/green, mk_6 Rasatala slate/crimson, mk_7 Patala gold/ruby.

Every mark flies a **forged sheet** (`mk_<n>/mark<n>_sheet.png`, **128x128** cells, 34 frames per tier row: idle 12,
hit 6, **boost 8**, bank left 4, bank right 4), made by `ship_forge/ShipForge_8.java`
(`java ship_forge/ShipForge_8.java ship_forge/out8 [--mark N]`). The three tiers of a mark are three different
colourways, not one hull recoloured: e.g. Mk-1 cyan / gold-lavender / ivory-gold, Mk-2 obsidian / steel-brass /
gilded with white-hot veins, Mk-7 black-gold-red / silver-sapphire / void-ruby. While the boost is held the hull plays
its **own boost form**: Mk-1 folds its wings back, Mk-2 sweeps armour and tusks, Mk-3 shuts its lotus into a bud, Mk-4
pulls its cities into a tight orbit, Mk-5 tucks its heads into a comet, Mk-6 folds its blade fan, Mk-7 closes every
floating piece on the core (two frames close in, six loop). Mark 6 is the asura war-throne; Mark 7 is the thin
matte-black needle with a gold halo, red flaps and one huge exhaust. The sheets load with mipmaps (the hull draws at
about a third of its texel size). The `ship_<n>.png` files remain as the fallback.

**Enemy idles** are eight-frame loops (sheets are `assets/enemies/atala/<kind>.png`, 16x4 cells of 64: idle x8 + rage x8
on rows 0/2/3, hit x2 + death x6 on row 1; regenerate with `python tools/gen_enemy_sheets.py`). Beyond the eye, each
body now does something: Auris' fins beat and two motes orbit it, Hataka drips gold draught and a glint rides its
dome, Lance's bead rides the slot while a pulse runs down the spine and the finial burns, Vakra throws lightning
between its horns and trails tassels, Bhavi vents steam and sweeps its eyes, Mayavi's shards twinkle and light runs
down the seam, Dvarapala's studs chase and a scan line crosses the door, Dipaka sheds embers and swirls its flame,
Vanija's coins hop in a wave under a swaying beam.

| Ship | Tier 2 | Tier 3 |
|---|---|---|
| Vajra Mk-1 | Rapid Cycler 120c — cooldown ×0.82 | Aegis Plate 300c — hull +40, shield +15 |
| Kite Mk-2 | Light Frames 180c — speed +50, turn +10 | Twin Link 360c — cooldown ×0.80, damage ×1.25 |
| Aegis Mk-3 | Reinforced Hull 220c — hull +60 | Bulwark 440c — shield +55 |
| Hive Mk-4 | Swarm Lens 240c — cooldown ×0.80 | Seeker Chips 480c — damage ×1.35, hull +25 |
| Lance Mk-5 | Sharpened 280c — damage ×1.30 | Overdrive 560c — cooldown ×0.85, speed +45, turn +15 |
| Mortar Mk-6 | Heavy Bands 320c — damage ×1.35 | Quickload 640c — cooldown ×0.76 |
| Vajra-7 | Angel Protocol 400c — cooldown ×0.82, damage ×1.25 | Seventh Halo 800c — hull +50, shield +50 |

A run starts with **0 coins** and dies with them: coins, hulls, tiers, patterns and
shards are all built inside the run. Rank 0/1/elite pay 2/4/6 coins plus rolled health
(8/15/30%) and loka-keyed tatva shards (6/14/28%); Bala pays 25 coins, guaranteed health and the
Atala band of the Vajrayudha; caches pay 100c and a life, the kill purse 150.

## Weapons (15 patterns — you start with one)

Patterns are data (`WeaponDef` rows), not classes. Defaults: speed 700, damage 10, size 5,
life 2.5 s, cooldown 0.3 s, colour gold `(1, 0.9, 0.3)`.

| # | Name | Cost | Cooldown | Damage | Pattern |
|---|---|---|---|---|---|
| 1 | Single Bolt | free | 0.30 | 10 | one shot |
| 2 | Double Bolt | 90c | 0.30 | 10 | 2 bolts, 10° spread |
| 3 | Burst | 120c | 0.60 | 9 | 3 shots, 0.09 s gap |
| 4 | Twin Burst | 170c | 0.65 | 9 | 2×2 bolts, 8° spread, 0.1 s gap |
| 5 | Buckshot | 110c | 0.50 | 8 | 5 bolts, 45° spread |
| 6 | Heavy Shell | 180c | 0.60 | 34 | slow, 10 px shell |
| 7 | Light MG | 80c | 0.08 | 4 | rapid, 3 px |
| 8 | Charge Shot | 220c | 0.20 | 14 | hold to charge 0.8 s, power scales with charge |
| 9 | Laser | 320c | 0.70 | 30 | instant beam, 900 px range, stops at the first wall |
| 10 | Bomb | 260c | 0.80 | 26 | 90 px splash |
| 11 | Missile | 240c | 0.50 | 18 | homing, 160°/s |
| 12 | Tracking Missiles | 380c | 0.70 | 14 | 2 homing, 380°/s |
| 13 | Aim Shot | 150c | 0.20 | 12 | 20° aim assist inside 900 px |
| 14 | Piercer | 140c | 0.32 | 14 | pierces enemies until it kills |
| 15 | Wallbore | 200c | 0.42 | 10 | pierces rock |

A run starts owning **only the Single Bolt** (`Hangar.weaponOwned[0] = true`); the other fourteen are
bought on the PATTERN tab. `V` / `B` cycle only through patterns you own. The price ladder rises with
what the pattern does — a Laser and a Missile are late-run buys, an 80c Light MG is an early one.

Firing (`Weapon`): cooldown is multiplied by the ship's `cooldownMult`; damage by `power ×
damageMult`; bullet speed by `power` alone — a damage tier never moves the round. Muzzle sits 22 px
along the aim. Burst rounds keep the first shot's aim, fire exactly `def.burst` shots and **stop the
moment you release the trigger**. Charge weapons only fire on release if the charge cleared 0.12 s
**and the weapon's cooldown is clear** — tapping cannot outrun it.
Aim assist (`World.assistAim`) snaps to the nearest enemy inside the cone, within 900 px.

## Combat

Hit resolution lives in `World`, not in `Bullet`:

- Player bullet vs enemy: AABB overlap; splash weapons detonate on contact, pierce continues to the
  next body, plain shots die on the first hit
- Enemy bullet vs ship: AABB against the 24×24 box, then `Ship.damage` — skipped entirely while
  `iFrames > 0`, so a freshly respawned hull lets shots fly straight through
- Ship vs enemy contact: both get a `World.KNOCK` (260 px/s) velocity kick along the contact line,
  in opposite directions — a small shove apart, no damage to the ship; the enemy also eats
  `World.RAM` (10) damage per touch at most once per 0.5 s (`contactCd`), and starts a 2 s
  `Enemy.RECOVER` window. Friction and Brain steering bleed the knock off.
- Splash: full damage inside the radius, **half in the overlap fringe**
- Beams: distance marched in 16 px samples until rock, every enemy within `w/2 + 6` of the segment
- Rock stops both sides — enemy fire is blocked too, so cover works both ways
- Homing steers at most `homingTurn` rad/s toward the nearest enemy inside 700 px (player shots only)
- Player shots are pooled (`Pool<Bullet>`, 64 initial / 512 max) and capped at 400 live; enemy fire
  is capped at 300 so the player always keeps slots

## Enemies

Six kinds, all placed in the world at construction — there is no wave spawning:

| Kind | HP | Fire cd | Size | Cruise | Behaviour in `Brain.think` |
|---|---|---|---|---|---|
| MAYAVI | 90 | 4.2 s | 48 | 105 | orbits its post |
| HATAKA | 45 | 3.8 s | 32 | 82 | static |
| AURIS | 35 | 3.0 s | 32 | 100 | **kites** — backs off 300 px/s when inside 240 px |
| LANCE | 50 | 2.4 s | 32 | 90 | static sentry |
| VAKRA | 65 | 3.4 s | 48 | 118 | **weaves** ±220 px/s when inside 420 px |
| BHAVI | 85 | 2.6 s | 48 | 130 | **closes** 340 px/s when inside 520 px |

Cruise speeds are deliberately low (82–130) and fire cooldowns 25–50% longer than they were. Both
halves of the same change: with one bullet per attack, an enemy that arrives fast and fires often
turns a readable tell into noise. `BALA` is 1500 hp and never auto-fires — `BossBrain` runs every
attack itself.

Shared rules:

- **Sleep until approached.** Inactive enemies do not move or fire; they wake when the player comes
  within `wakeRange` of their home (courts 900, sentries/elites 700, hataka 900, boss pit 1200).
- **Patrol** is a sine orbit about home at its kind's **cruise speed** (the table's Cruise column —
  every tier moves at its own rate), closing on the orbit point with gain 2.2
  and steering with a 6/s exponential rate. Rank 1 runs 1.15× its kind's cruise.
- **Whiskers** at 46 px probe left/ahead/right along the intended direction and bend velocity around
  rock; **mates** keep 46 px apart; the **leash** is 300 px and does not apply once routed or
  hunting.
- **Fire** requires line of sight and range ≤ 620 px. **Atala fires ONE bullet.** Every kind's
  attack is a single round (`count(1)`), so the screen fills with fewer, readable threats and the
  difficulty lives in *what one round costs* rather than how many arrive. Damage is graded by who
  is firing — `RANK_DAMAGE = { 1, 1.6, 2.4 }` for regular / rank-1 commander / elite — so a court
  still reads as a court without filling the screen. The per-kind signatures (speed, silhouette,
  tell) are the feature; the damage dials come up in later lokas through `LokaTable.dmgScale`.
- **Hata** does not double its ring: `BETRAY_COUNT` = 3 pellets of `BETRAY_DAMAGE` = 4 are still
  vented around the ring, because **the ring is the tell** — it is how you read the shot. What was
  cut is the volleys of rings.
- **Rank and formation.** Rank 1 (court commanders, elites) can lead; everyone else follows one.
  A unit with a living commander rides a **ring formation** around it: n living members take n
  slots spread `2π/n` apart under a slow 0.4 rad/s spin, ring radius `max(70, n·46/2π)` so it opens
  as the count grows, mate separation (46 px) still applies. The unit's post syncs to its leader's,
  so leash and ring never pull against each other. Sentries hold their posts; backups join the ring
  like anyone else.
- **Rout → regroup.** When a commander dies every follower runs outward from the post at up to
  180 px/s for 2.5 s and does not fire. Then it searches within `Brain.FIND` (700 px) for the
  nearest living unit of **higher rank** and joins its formation. With no senior in range it drops
  back to normal behaviour — patrol its own post and attack as usual.
- **Knockback recovery.** For 2 s after a ram contact the unit flies straight back to its
  post/slot with its kind flair (kite, weave, close) paused, then fights normally again.
- **Call to arms (top three regions only).** In regions 3–5 (y ≥ 6100) a hit — damage or kill —
  wakes every sleeping unit within `Enemy.ARMS` (600 px). The bottom three regions never hear
  fights: there only player proximity wakes anything.
- **Wound response (below 30% hp).** AURIS kites from 520 px instead of 240; LANCE, HATAKA and
  MAYAVI break rank and back off the player inside 500 px; VAKRA and BHAVI go berserk — 1.5×
  cruise speed, weaving from 560 px and closing from 700 px.
- **HP bars.** Every enemy wears a 2 px bar above its hull: green when full, red when nearly
  dead (by real hp). The fill is the eased `dispHp` (12/s exponential onto hp), so a hit burns a
  white-hot slice at the bar's leading edge that cools away as the bar drains.
- **Elites** have double hull and tint gold; dormant units draw dimmed grey; a hit flashes white
  for 0.12 s; death leaves a 70 px gold ring, nearest-filtered and gone in 0.25 s.

### Order of battle (227 units: 184 in the mirrored order about x = 2600, plus 43 stationed posts — Bala joins when the seal lifts)

| Region | Units | Placements |
|---|---|---|
| R0 Bhu | 0 | bare sky: no enemies, no shop, no checkpoint, no wall |
| R1 | 45 | gateway honor 4 @ y1790 (LANCE @ 2536/2664, HATAKA @ 2450/2750); sentry pair @ (2600,2150); vakra+auris courts 6+4 @ (2600,3000) and 6+4 @ (1500,3550) + (3700,3550); 6 site guards |
| R2 | 51 | gate post @ (2600,4300); sentry pair @ (2600,4650); bhavi+vakra 4+2 @ (2600,5200) + hataka pair; vakra+auris 6+4 @ (1500,6100) + (3700,6100); court 4+2 @ (2600,6800); sentry pair @ (2600,7100); 6 site guards |
| R3 | 53 | gate post @ (2600,7800); mayavi+bhavi 6+4 @ (2600,8500) + hataka pair; vakra+auris 6+4 @ (1500,9600) + (3700,9600); mayavi+bhavi 4+2 @ (2600,10800); sentry pair @ (2600,11500); 6 site guards |
| R4 | 62 | gate post @ (2600,12200); sentry pair @ (2600,12550); mayavi+bhavi 6+4 @ (2600,13300) + hataka pair; bhavi+vakra 6+4 @ (1500,14400) + (3700,14400); mayavi+bhavi 4+2 @ (2600,15400) + 2 gold elites @ (1200/4000,15400); mayavi+bhavi 4+2 @ (2600,16300) + hataka pair; sentry pair @ (2600,17200); 6 site guards |
| R5 Arena | 16 | (Bala joins when the seal lifts — pit centre 2600,19104); gate post @ (2600,17800); sentry pair @ (2600,18100); 4 mayavi honor @ (3000/2200,19084), (2600,19584), (2600,18584); 4 lance sentries @ (3512/1688,19084), (2600,19900), (2600,18300); 3 site guards (arena checkpoint) |

Stationed posts: 4 gateway honor, 4 gate posts × 3, 9 sites × 3 = 43. Site guards are hand-placed
(`Enemy.guard`), so the mirror rule does not apply to them; same for the gate and gateway posts.

Court geometry: commander on the anchor (orbit 25 px), `n` ring members evenly spaced on a 110 px
radius, `m` backups in a line 60 px apart, 200 px below the anchor. Backups drop to 0 if the spot is
under 30% open and halve under 50 — R4's table total is 5 slots short of nominal for exactly that
reason. `World.add` carves a padded 16 px box around every home so no
one spawns inside rock.

## The land (flying over a world, not through space)

`docs/WORLD_BIBLE.md` is the research (Bhagavata Purana 5.16-5.26, the Vishnu Purana's soil colours, the Mahabharata's
Hiranyapura) and the render plan. The short version: the lower lokas are *bila-svarga*, earth-wide lands with cities,
gardens, lakes and soil, lit by jewels because they have no sun - so the open space is **ground**, the rock is **raised
ground**, and the ship is **in the air**.

- **`Biome`** (one row per band of `World.regionStart`): soil ramp, water, trees, road, a pattern style (mandala rings,
  throat ribs, mosaic), the frame's ambient light and its mist. Band 0 (Bhu Loka, "Avatarana") is real sunlit green
  earth that cracks toward the gate; then Svarna Dvara (white soil, a gold-paved road), Maya Vana (a dark mirror-wood
  with silver pools), Svapna Mandala (violet ground drawn with golden rings, lotus lakes), Bala Mukha (maroon ridges
  like a throat), Bala's court (an ivory-and-gold mosaic).
- **`GroundMap`** paints it: a pure function of the world (soil by a height field, patterns, water with shores, ripples
  and lotus pads, a winding road, trees that cast shadows, glints); the same seed makes the same bytes, so chunk seams
  cannot show. One texel = 4 world px; a 128x128-texel chunk paints in ~4 ms.
- **`GroundLayer`** streams chunks (512 world px, an LRU of 96) as the camera nears; a jump paints the whole view at once.
- **Lighting is per biome** (`Biome.ambient`, eased over 500 px at a boundary): the multiply pass is no longer one black.
- **Altitude cues:** the hull and every body cast a shadow down-right; the walls cast theirs (`TileImages.drawShadow`);
  the mist (the old nebula, now added light, never a dark overlay) drifts at 0.95 against the ground; and 90 motes of
  jewel-dust slide past at 1.2-1.7x. The stars are gone (they would have been under the ground).
- `lwjgl3/.../WorldShotTest` writes `build/world-0..7.png`: one real frame per band, plus the Svapna pillars and the
  Svarna lamp road, each with a ring of hostile rounds in the air.

**ATALA, STEP A (docs/ATALA_PLAN.md): the wall stack in the real game.** The old rock tiles are the play layer again
(`World.walls` defaults to true; `-Dvajra.walls=false` sweeps them away after generation). Below the meadow the renderer stacks
the same walls behind the play layer (`WorldRenderer.stackLayers`, the maths in `LayerStack`): `Biome.layers(y)` says how many -
none on the open meadow, opening from y 1000 to a whole shaft by y 1500 (the descent), then 12 in the Golden Door, 15 in the Mirage
Wood and the Dream Circle, 12 in the Yawn, 20 in the Hall of Belief, eased over 500 px at each boundary, the farthest layer fading
in so the count never pops. Each layer is `0.96^k` the size of the one in front about the screen centre (so it slides by that
fraction of the camera's move) and tinted toward the band's own deep colour (`Biome.deepTint`: warm in the Golden Door, teal in
the Mirage Wood, violet, maroon, gold). Under the stack the raycast land (below) drops away to a far, hazed, double-size floor as
the shaft opens. While a stack is up the lamp/mushroom light pools and the above-the-hull raycast pass are off (they would not
line up with the far floor; they come back on the layers in step B), and the minimap draws the wall tiles over the land again.
The old gate walls are real gates again (the boulder belt remains as the lip of the hole).

**STEP A.2 (faster, alive).** (1) *(the test build's everything-unlocked purse was retired: the game plays the real economy from the first frame).* (2) **Ten layers everywhere** (it was 10-20 by band). (3) **Speed** - the whole game frame at 1920x1080 went from about 11-13 ms to
about 4.5-7 ms (`PerfProbeTest` times every render pass with `Probe`; `GameFrameProbeTest` times update + render + HUD and fails above 16 ms): the walls are
**baked in 512 px chunks** (`TileChunks`: each chunk holds its soft shadow, side face and tiles in one premultiplied texture, so a layer is a handful of
quads instead of thousands of tile sprites; mirage tiles stay live; a frame that lacks a chunk falls back to the per-tile path), the **far floor is raycast at half
resolution** into a buffer and stretched (`TerrainLayer.drawHalf`), the **minimap's rock is baked once** with its land (`WorldThumb.rock`, built while the game
loads) instead of a `blockedPx` per cell per frame, and the bullet filter only copies the frame when a bullet exists. (4) **Animated sprite sheets**: the
checkpoint's altar is 8 frames (its gold chakra ring turns - fast while cold, slow once lit - and its petals breathe), with a 6-frame winged flame-seed that
flutters on a figure-of-eight round the empty bowl while cold, a 6-frame pixel flame that licks in the bowl once lit, and four 4-frame lamp-bearers that circle
the lit altar; the shop's sign is a 16-frame sheet for each of the 7 dock-light states (the tula's beam SEE-SAWS, its chains and pans ride the ends, a gold coin
HOPS from pan to pan, a pearl rests in the other, the hanging lamps gutter, a pip runs up each idle dock post), and the berth's dais is 8 frames (a wave of light
walks round the lotus petals). (5) **The shaft has a floor and a second population** (`Enemy.depth`): below the play layer's walls, a body's DEPTH is 0 on the
layer and 1 at the floor; below 0.4 it has no body (no fire, no AI, nothing can touch it, rounds and the astra's locks pass it) and is drawn small and dark in a
pit UNDER the walls. A **leader's call** (`EnemyDirector.tickBackup`): an awake rank-1 body with the hull within 1100 px has two calls (an elite three), one every
7-11 s, and each raises two reserves of its own kind from the floor beside it (depth 1 to 0 over a second, a grit ring where they break the surface), following
it; no more than four at once. **Scouts and skirmishers (Auris, Vakra) FLANK by diving**: every 9-15 s an awake rank-0 one sinks (0.8 s), runs beneath you
(1.8 s) and surfaces 320-440 px from the hull at a clear spot. `EnemyDepthTest` pins all of it; `RiseShotTest` writes `build/rise.png`.

**ATALA, STEP A.1 (the floor made stone, the world made dark and calm).** (1) **The far floor follows the stack:** it used to slide
at a different speed from the last wall layer (a camera zoom and the shader's ground scale were set separately); now one number
(`LayerStack.floorSpeed`, a half layer in front of the last layer, eased in from the meadow's own ground speed as the shaft opens)
sets both the camera's zoom (`floorZoomMul`) and the lean, and a test proves the screen speed equals it. (2) **Closer layers, calmer
motion:** the shrink per layer is 0.98 (was 0.96: the shaft is shallower, the last of 15 layers is 0.74 of the play layer), the lean
is a quarter of what it was (22 px at most), and the far floor's own flight plane is 150 so its tall things do not swing against the
layers. (3) **No greenery anywhere, not even on the surface:** every band's floor is stone in the wall tiles' own palette (sandstone,
ivory, cold grey, violet, maroon, ivory and gold), laid as 64 px FLAGSTONES, each slab a shade of its own with dark seams; the
trees became stone outcrops (stalagmites with the same cast shadows), the lotus pads and the blue water are gone (only the silver
mirror pools of the Mirage Wood and the violet-gold hataka pools remain). (4) **Much darker:** every band's ambient is roughly
half of what it was (the meadow 0.62, the Golden Door 0.30, the Mirage Wood 0.13-0.25), the floor is dimmed by 55% once the shaft
is open, and the stacks' deepest colours are darker - so a fight's light (bullets, flashes, pools) is what you see.

**ATALA, STEP B: the stack gets depth cues.** (1) **Contact shadows:** each of the five nearest layers throws a soft dark copy
(9 px down-right) onto the layer behind it, and the play layer does the same. (2) **A leaning vanishing point:** the stack's centre
rides toward where the hull is going (the hull's velocity x 0.12, at most 60 px, eased over about a quarter of a second), so the
far layers drift by `lean x (1 - scale)` (`LayerStack.leanShift`) and the far land by half of it - the shaft tilts as you steer.
(3) **Extruded tiles:** the play layer and the nearest six layers draw a thin dark copy (3 px down-right, 50% black) under the
tiles: a wall has a side face and reads as thick. (4) **Motes between layers:** 48 soft additive lights per layer on the nearest
eight, plus the air between the first layer and the play layer, each pinned to its own layer so it slides at that layer's speed,
tinted by the band's accent. A frame in the Mirage Wood costs about 9 ms here.

**Pseudo-3D from above, by raycasting flat planes** (`docs/WORLD_BUILD_PLAN.md`; no models anywhere):
- **The land is one height map** (`TerrainBake`, 4 world px a texel, ~27 MB, baked on all cores in ~0.3 s while the game
  loads; the flat ground shows until it lands). Every texel holds a colour and a height in **cut planes**: a tree is three
  stacked discs, a mushroom two, a lamp a post and a head, a pillar a plinth, shaft and capital, a boulder five steps, a
  mountain a terraced stack (a plane every 13 units), the throat's ribs, the mandala's rings and the court's dais low steps.
- **`TerrainLayer` raycasts it** in a fragment shader: each pixel marches a ray from a camera hanging 900 above the flight
  plane down through the map to the first plane it meets (44 steps + 5 of bisection), so parallax is exact, ridges hide
  what is behind them, the sides of a plane are shaded by the slope the ray finds, and shadows are thrown toward a light
  up-left. Pass A draws everything up to the flight plane (under the hull); pass B draws only what stands above it (pillar
  crowns, crests), translucent, over the hull. A frame costs about 2 ms.
- **Near and far from the ground** (`Altitude.fly(y)`, eased between stops): high over the meadow (430) and the dream-circle
  (380) - the land small and slow, hazed - and low in the pass (120), the mirror-wood (90-105) and the mansion (90-100) -
  the land large and quick, the crowns over the hull. Aim, collision and AI live on the flight plane and do not care.
- **No walls.** `World.walls` (`-Dvajra.walls=true` brings the rock back) is off: the world is still generated with the
  rock so placements keep their open spots, then the maps are emptied and the wall-rooted plants and torches dropped.
- **The boundary of rocks and boulders between Bhu Loka and Atala** (`Boundary.belt`): three rows of 34-68 px boulders
  across the whole world at y 1600 with a 520 px pass at the centre (the old grand door), solid to hulls, bodies and rounds
  (`World.blockedPx`/`collide`), and the road is pinned through the pass. The same list is the picture (stepped, paling
  stone) and the collision.
- **Darkness.** Each band's ambient is much lower now (the meadow stays daylit; Atala is dim), and the light comes from
  things: the hull's pool, enemies, and now **lamps, glowing mushrooms and pillar crowns** (`PropIndex.glows`, projected for
  the current altitude).
- **Bullets are the negative of what is behind them, nothing else about them changes.** Each bullet keeps its own art at its
  own size; a small shader (`BulletImages.begin/end`) reads the finished frame behind it (one copy of the frame per draw run)
  and paints the sprite's silhouette in the inverted colour, pushed away from mid-grey so it still reads over mid-tones, the
  art's own light and dark riding on top. Over dark ground it is bright, over light ground dark, over green magenta, over violet
  lime. Drawn once, over the finished frame (the foreground pass); without the shader it falls back to the plain
  `GL_ONE_MINUS_DST_COLOR` negative.
- **The map is the world, and opaque**: both minimaps fill an ink disc and then the land from `WorldThumb` (the whole
  world sampled once per 32 px), and the vedic dial names the band.
- `lwjgl3/.../WorldShotTest` writes `build/world-0..7.png` (one real frame per band, the mountains, the pillars, the boulder
  belt, each with a ring of hostile rounds in the air) and times the raycaster.

## Presentation

Draw order per frame (the ground, mist and shadow passes now sit under what follows, and the motes over it;
see **The land**): nebula (10% alpha, 2048 px tile, slow scroll) → stars + checkpoint
particles (shapes) → plants (four variants — ping-pong sway over 3/3/12/8-frame sheets, grow/shrink pulse, all 1x, grass in side-by-side runs of 1-3, base-anchored on any rock
face and rotated off it, roots tucked 4 px behind the face so the wall draws over them, never on a corner block, viewport-culled) →
tiles (dual-grid wall art, native colours) → torch flames (rotated onto their wall face, 3-frame loops, 2× scale,
viewport-culled) →
enemies (viewport-culled) → orbs → shop signs → checkpoints →
bullets (16×16
sprites) → sparks, enemy hp bars and the hull fallback (shapes) → thrust plume, ship, laser beams and
shockwave rings (sprites) → charge glow and shield ring (shapes) → the lightmap dim
(`Lights`: world × ambient-with-pools) → bullet sprites redrawn over the dim at full
brightness so shots read in the dark → hangar / game-over dimmer → HUD text. All UI text uses two OFL fonts from `assets/fonts/`, generated through FreeType at
startup: **Rajdhani** SemiBold for the headings (wide-tracked caps, 30) and the numerals, and
**Oxanium** for everything read at length (15 body, 16 subtitles, 11 captions);
centered lines are measured with `GlyphLayout`, not hardcoded offsets. In-world text was cut
by the UI design pass — `SHOP` / `CHECKPOINT` / gate arch / `SEALED` / boss phase labels are
gone, their art (sign, checkpoint stack, tinted gate, phase-tinted boss bar) carries the
meaning — so the Oxanium signage face is no longer loaded; its five A/B candidates sit
unused in `assets/fonts/` for future signage (Cardonas under its personal-use-only license).

Every shape pass starts by re-enabling GL blend — `SpriteBatch.end()` disables it and
`ShapeRenderer` never turns it back on, so without that the dimmer, sparks, checkpoint motes,
charge glow, shield ring and hp-bar backing would all render opaque.

- **Stars** — 1640 motes in 3 parallax layers (0.3/0.6/0.85 of camera motion, 1/2/3 px), culled to
  the viewport, wrapped over the world
- **Orbs** — 6 ambient orbs, visible 2.3 s (8 frames at 10 fps, 25°/s spin, fade in 0.2 s / out
  0.3 s), hidden 1–4 s, then reappear at a random open spot at 1×, 1.5× or 2× size. Visual only
- **Lights** — Atala is dim: after the shield ring, `Lights` multiplies the frame by a lightmap
  built in a window-sized frame buffer — ambient floor 0.15 (cool), plus additive radial pools:
  warm white r340 around the ship, red 2.6× hull around every enemy, gold 3× for the boss, and a
  flickering r90 pool at each torch's mid-flame point. The falloff texture (128² smoothstep
  grayscale) is generated in code, the region is flipped for SpriteBatch. No wall line-of-sight
  occlusion yet; the HUD and overlays draw after the pass and stay full-bright.
- **Checkpoints** — one per Atala band: three concentric crops drawn at 2× (64×64) — pink collector
  at the back, gold globe over it, gold frame on top — same size, aligned on the centre. Both
  orbs take the
  exact same transform (one shared breath scale ±15% on a ~3 s sine, one shared spin 45°/s about
  their own centres) while the frame stays static, and 12 tiny glow motes stream left and right
  from the centre (70 px, sine fade, drawn behind the stack; gone once spent). Spent checkpoints keep frame and globe at
  30% alpha. Crops live in
  `assets/checkpoint/`, nearest-filtered; missing files draw gold / pink placeholders
- **Enemies** — every `idle_N` a kind ships with plays in sequence at 4 fps, staggered per unit by
  its placement phase (auris carries 6 frames of ring rotation, the other kinds 2)
- **Bullets** — one 16×16 sprite per weapon (laser has none, it draws as a beam) plus one for enemy
  fire, in `assets/atala/bullets/`, generated by `tools/gen_bullets.py`. Drawn scaled to the hit box
  and turned along its heading; the weapon name is the file key (`Single Bolt` → `single-bolt.png`)
- **Beam, rings, plume** — the laser draws as a 16×8 beam sprite stretched from the muzzle to
  wherever it cut off at a wall; death and bomb splash draw 16×16 shockwave rings — nearest-filtered
  so the ring stays crisp as it expands, gone in 0.25 s; the engine plume
  is 16 thrust frames (4 power levels × 4 animation frames) from the pack's orange set, trimmed to
  their ink so they centre exactly on the tail and nearest-filtered to stay crisp at 2×. All live in
  `assets/atala/fx/`, cropped out of the Space Shooter pack sheets by `tools/gen_fx.py` and whitened
  so any colour tints them (`FxImages` falls back to a drawn bar / circle outline; missing plume
  frames simply drop the plume)
- **Ship** — 32×32 sprite from `assets/vajra_ships/`, one folder per ship (`mk_1`…`mk_7`) with three
  variations painted in that ship's loka colours; the variation flown follows the highest bought
  hangar tier. Missing art falls back to the 24×24 polygon hull, nose wedge and canopy. The plume
  sits exactly on the tail, anchored against `facing` and turning with it, while a thrust key is
  held — its level ramps with hold time (0 / 0.1 / 0.3 / 0.6 s → all four pack levels), boost runs
  that same ramp while shifting the hue crimson and doubling the plume length, drawn at half scale,
  animated over 4 frames; the hull is drawn along the push key's
  world   axis — W up, S down, A left, D right, bare burst follows the nose — anchored on the
  trailing edge and squeezed across it, full from the first frame and easing out with the burst,
  with no particle trail; charge glow sits over the hull and
  the ship blinks while invulnerable
- **HUD** — the bar block, drawn entirely in code (no image assets, no frame): three thin
  the mockup over a dark track, each clipped to its value; lives pips under the bars. Hull/shield/boost only - panel, portrait, coin and the
  shard block are elsewhere.
  * **CONTROL STRIP (KeyHints)** - the two run-scoped keys, bottom centre, as `[V] WEAPON` and
    `[F] ASTRA`. **Bottom centre because it is the only free stretch of the screen**: the weapon
    plate owns the bottom-left corner and the shard block owns the bottom-right, and the band
    between them is empty, so one centred strip reads as "your controls" in one glance instead
    of scattering two labels to opposite edges. It also stays clear of the depth tape (right
    wall, y 120 up), and `KeyHintsTest` fails if either neighbour grows into it.

    **V is static** - it is always live, so a stateful version would only flicker. **F is not**: a
    plain "[F]" says the key exists and nothing about whether it will do anything, so the cap
    reads the same state the corner block reads, in the same language. A **hollow cap** means
    not forged, and the track is filling toward forging; a **dim cap** means forged with nothing
    in hand; a **solid gold cap** means a release is in hand; and it **pulses with `windK()`**
    while the charge runs, so the strip you have been reading all run is the one that tells you
    the Astra went off. The track is in the loka's own element, so the centre of the screen and
    the corner agree.

    The strip's width is measured off the real font and the pixel test reads `KeyHints`' own
    geometry, so neither can drift from what is drawn.

- **Shard block, bottom right** - `ShardGauge`. **One row per element the loka pays**, not one
  per Pancha Tatva: Atala pays Akasha alone, so it is a single row, and the block only grows
  where the world does - Rasatala two, Patala five. **A row is one thing, read left to right:
  the charge track, the count, the gem.** The track is progress toward the next Astra release in
  five segments of one shard each, and the segment you are part-way through fills partially, so
  "3 of 5" needs no second number. It is dimmer until the weapon is forged and solid once it is -
  that difference is the whole "have I got it yet" answer. The count is what you hold, kept
  because they sell for 20c. The gem is **untinted**: its own art colour, matching the pickup on
  the floor rather than `TATVA_RGB`. **A gold hairline round the gem means a release is in hand:**
  **press F** - the one loud state, and there is exactly one action behind it.

  The track reads the shard PILE, not the banked counter: forging banks shards and never spends
  them, so the pile is exactly "how many releases this harvest is worth". Selling is the only
  thing that pulls it back down.

  This replaced five temper pips plus a separate forge bar beneath - two tracks asking two
  questions, with no way to tell which mattered, for one weapon.

- **Compass** - top-right ring showing bearing to gates (`G`), shops (`S`) and checkpoints
  (`C`), plus a north marker. The ring spins so the ship's facing is up on the compass — turn
  toward a marker to head for it. Replaces the minimap (parked). Reads live `World` data
  (gates, shops, checkpoints) and is hidden in Bhu Loka where there is nothing to point at.

- **Control screen** - `ControlScreen`, pushed from the home menu (CONTROLS row) and from
  pause. Lists every flight, hangar and system binding: W A S D thrust, Q / E turn, SPACE /
  LMB fire, SHIFT boost, F Astra, G / RMB shield, V / B weapon cycle, 1–7 hull select,
  Q / Z hull cycle, H / ENTER hangar, ESC, TAB, wheel zoom, F11 fullscreen, F12 window swap,
  T debug damage, R new run.



Missing art never crashes: every image loader falls back to a generated placeholder (coloured tile,
gold diamond, gold circle, black nebula, white bar or ring), to a drawn shape, or to nothing at all.

## Architecture

```
build.gradle, settings.gradle, gradle.properties   Java 17, gdx 1.14.2, junit 5.11.4
assets/        atala/walls, atala/bullets, atala/fx, atala/enemies/*, atala/energy_orb_1, atala/space,
               ship_mask/ (source silhouettes), vajra_ships/mk_1..mk_7/ (painted ship art),
               coins/coin_1..5, health_drop/, shards/shard_1..5 (art wanted), vajrayudha/vj.png
               (the seven-band blade), checkpoint/ (checkpoint holder + orbs), brand/ (studio + game
               logos), fonts/ (Rajdhani + Courbe Sans in use,
               + CinzelDecorative / YatraOne / RuslanDisplay / BungeeInline / Monoton / Oxanium —
               the in-world signage set, unloaded after the no-in-world-text pass — plus Cardonas,
               personal-use-only, needs a paid license to ship), effects/ (smoke-low-hull, small-flame),
               SpaceShooterAssetPack_{Miscellaneous,Projectiles}.png
               brand/studio-logo.png (the studio card - NO SPACES in a nested asset path: libGDX's
               FileHandle falls back to a classpath lookup for Internal files, and that lookup
               404s on a space inside a nested path. "sahah/SAHAH LOGO.png" was invisible.)
tools/         fix_wall_tiles.py, gen_enemy.py, gen_nebula.py, gen_bullets.py, gen_ships.py, gen_fx.py
core/src/main/java/vajra/
  VajraGame.java              composition root, owns the SpriteBatch, sets the Flow. Boot is
                              SplashScreen, which hands itself over to HomeScreen
  flow/                       Flow, FlowState — run-state machine: one screen stack, only the
                              top state updates and receives input
  world/                      logic — never imports rendering classes
    World.java                world size, regions, runtime: hit resolution, bullets + effects,
                              aim assist, LOS, spawn helpers, checkpoints + lives, docking,
                              per-seat cacheState[SLOTS][5], astraBegin / astraLoose (the Astra rite)
    LokaTable.java            the seven lokas: warden, paid tatva, hp/dmg scales, hull unlocks,
                              seeds - one row per loka is what a new loka is
    WorldGen.java             build once: 5 maps + gates, order of battle, compounds, shops +
                              caches + checkpoints, torches + plants, kill quota
    TileMap.java              tile grid: get/set, solidPx, edge mask, open fraction, push-out
    MapGen.java               worldgen factories: 5 caves + 5 gate walls, smooth/specks/trim
    Checkpoint.java           one per Atala band: orbit, collector touch, respawn anchor
    Orb.java, Effect.java     ambient orb cycle; spark/ring/flash effects
    Pickup.java               loot pops: coins, health, tatva shards, the Vajrayudha band —
                              drag-to-float physics, no gravity
    Score.java                points + combo: kill values (loot tiers + kind hp), the timed
                              chain and its multipliers, flat milestone bonuses, saturating total
  ship/
    Ship.java                 flight, meters, damage/respawn
    ShipControls.java         interface GameScreen implements (keyboard/mouse → axes)
    ShipDef.java              7 ships × 3 tiers; Hangar.java coins + shards + pieces + magnet
                              (fixed at 15) + owned tiers + stat multipliers
  enemy/
    Enemy.java, Brain.java    placed enemy + the steering brain (dismissed units walk out on their own)
    BossBrain.java            Bala's phase machine: wake, retinue ring, patterns, blinks, victory
  weapon/
    Weapon.java, WeaponDef.java   fire controller + the 15 pattern rows
    Bullet.java               pooled projectile, homing, detonation; Bullet.HOSTILE is enemy fire
  screen/                     rendering only
    GameScreen.java           loop, cameras, input mapping
    Ui.java                   fonts, UI camera + its viewport (applied every frame by Flow,
                              so UI-only states are never left on a stale glViewport),
                              ShapeRenderer, text + blend helpers
    Icons.java                coin + shard faces read at menu size: the wallet, the price lines
                              and the forge meter
    SplashScreen.java         the studio card: the SAHAH logo centred on the menu's own ink,
                              with "THE BURNING SHELL" beneath it; a beat of black, then up,
                              held and out on a slow fade. The field is the menu's exact colour
                              so the handover cannot flash. Any key or click skips it.
    UiMenu.java               list-menu component: cursor state and hit boxes only, each screen
                              draws its own rows
    Ornament.java             the title screen's temple ornament - hairline frame with corner
                              brackets, line-diamond-line divider, diamond row markers. Shared
                              by HomeScreen and PauseScreen; drawn into an open shape pass like
                              the kit. A Filled ShapeRenderer cannot stroke, so rings and diamonds
                              are built from primitives.
    ControlScreen.java        full key/mouse reference - every flight, hangar and system
                              binding. Pushed from home (CONTROLS row) and pause.

    Hud.java                  HUD: dimmer, bar block call, score + combo, yojana depth tape,
                              compass (gates/shops/checkpoints bearing), game-over and shop
                              text; suppresses the flight HUD while the hangar is open
    ShopUi.java               THE HANGAR - one opaque screen, three tabs (HULL / PATTERN / ASTRA).
                              Owns all layout, hit boxes and labels. Draws directly; the
                              component kit (`UiPanel`/`UiButton`/`UiBadge`) was removed —
                              no use for now.
    ShopController.java       purchases only. Split from ShopUi on purpose: draw and hit-test
                              used to be two hand-synced copies of the same magic coordinates.
    HudBars.java              hull/shield/boost bars, drawn in shapes (no image, no frame)
    ShardGauge.java           the bottom-right shard readout: one row per element the loka
                              pays (Atala = one row), each a 16px gem, a dim count and five
                              a five-segment charge track, the count and the untinted gem.
                              Read-only.
    WorldRenderer.java        world draw passes + image ownership
    Backdrop.java             stars, parallax
    TileImages, EnemyImages, BulletImages, OrbImages, NebulaImages, ShipImages, FxImages,
    CheckpointImages, ShopImages, PickupImages, VajraImages, ShipRenderer
lwjgl3/src/main/java/vajra/desktop/DesktopLauncher.java
core/src/test/java/vajra/...  43 classes, 295 headless tests
lwjgl3/src/test/java/vajra/desktop/  8 GL tests (HudScreenshotTest a scene per shop tab, SplashScreenshotTest the studio card, ViewportFillTest the resize, ShardGaugeScreenshotTest the shard block's hollow-vs-solid symbols, AstraScreenshotTest the charge cage and the volley, TransitionScreenshotTest the PLAY handover, the prologue skip and the boss spawn)
```

Deliberate absences: `DescentGate`, `Fragment`, `Sever` — nothing in code for them.
(The Vajrayudha band is `Pickup.PIECE`, not that `Fragment`.)

Working rules held by the tests and the code: logic never touches rendering classes; hot paths
(bullets, collision, stars) allocate nothing per frame — shared float buffers are written and
read immediately; broadphase is brute force (fine at 227 enemies); worldgen is seeded and
reproducible; the camera follows the ship and clamps to world bounds.

## Tests

```
.\gradlew.bat core:test --offline
```

295 core tests across 43 classes, all headless (no GL, no natives), plus 8 GL tests in
`lwjgl3` — **250 total**:

| Class | Tests | Covers |
|---|---|---|
| BackdropTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | star wrap tiling |
| BossTest | 14 | 14 | 14 | 14 | 14 || 14 || 14 || 14 || 14 || 14 | quotas fit the roster and a fresh run starts sealed, Bala does not exist until the quota is full, one Bala in the pit, dormant below the crossing, phase beats (stagger ignores hits, stand-down, berserk blink scheduled), a blink drops an elite pair and reappears 350-850 px out, elite pairs cap at 6, the kill starts victory and pays 150, the north loka gate is sealed behind gold until that kill, volleys release only with line of sight, the first volley orders his ring, stood-down units walk out and retire without firing |
| BrainTest | 11 | 11 | 11 | 11 | 11 || 11 || 11 || 11 || 11 || 11 | wall whiskers, mate separation, persistent position, auris kiting, rout follows commander, rout regroups or restores the spawn post, a second rout still flees, rout on the post still flees, formation slots spread by count, joiner settles into the ring, wounded hataka breaks cover |
| BulletImagesTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | every non-beam weapon and enemy fire has a 16x16 sprite on disk |
| CacheTest | 11 | 11 | 11 | 11 | 11 || 11 || 11 || 11 || 11 || 11 | one cache per band just inside its entrance gate on open ground, only the local squad within the radius answers, shop/checkpoint site guards never answer, touch arms the squad and only the last fall pays out (100 coins + 1 life, no double pay), an empty pocket opens on touch, hunters converge and leave the leash behind, **cache state is per player seat**, every tier has its own speed and rank 1 runs hotter, every torch's stored sample is solid rock within 6 px behind its base, every plant's stored ledge sample is solid and clear of checkpoints, 1065 plants with Maya Vana holding the majority, grass tufts stand in side-by-side runs |
| CheckpointImagesTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | all three 32x32 checkpoint crops exist on disk |
| CheckpointTest | 4 | 4 | 4 | 4 | 4 || 4 || 4 || 4 || 4 || 4 | one per Atala band on open ground, collector spends it, death anchors at the most recent checkpoint, game over at zero lives |
| CombatTest | 25 | 25 | 25 | 25 | 25 || 25 || 25 || 25 || 25 || 25 | damage and kills, pierce, splash crowd, enemy fire, rock cover both ways, auris firing, line of sight, shield vs live fire, beam sweep, i-frame pass-through, sparks fly off every hit, a long run holds together |
| DropTest | 10 | 10 | 10 | 10 | 10 || 10 || 10 || 10 || 10 || 10 | the loot table pays more the higher the rank (2/4/6 coins, Bala 25), one death fans every drop, coins settle then float (no gravity), health repairs a quarter of the hull capped at max, **shard rolls are 6/14/28% by rank**, shard rolls key off the loka with the architect's band at 100%, the magnet tugs loot at a fixed 15 power, **pickups expire after 40 s and fade over the last 4**, the seventh Vajrayudha band ends the run |
| EnemyArtTest | 2 | 2 | 2 | 2 | 2 || 2 || 2 || 2 || 2 || 2 | every kind's idle_N files are gapless on disk; the animation walks every frame of a 1-6 frame cycle |
| EnemyPlacementTest | 11 | 11 | 11 | 11 | 11 || 11 || 11 || 11 || 11 || 11 | 227-unit order of battle, nothing stationed in Bhu Loka, mirror symmetry (site guards excluded), homes carved clear of rock, sleep until approached then patrol, call to arms wakes the top three regions only, **every band grades through the loka curve, and all six bands are still Atala** |
| FlowTest | 4 | 4 | 4 | 4 | 4 || 4 || 4 || 4 || 4 || 4 | the run-state machine's transitions |
| WallsOffTest | 2 | **the rock is off by default**: no solid tile anywhere, no wall-rooted plants or torches, the hull flies through where the gate wall stood; the switch brings the old rock back |
| PropFieldTest | 3 | **the props are a pure function of the world**: the same chunk gives the same props, every tree on land and none on water or road, each band has its own things (trees, mushrooms, lamps, pillars), a pillar rises above the flight plane and a tree does not |
| LayerStackTest | 4 | **the layered walls**: every layer behind is smaller than the one in front, the further back the darker and bluer, each peeks out down-left of the one before, and a far layer slides by a smaller fraction of the camera's move |
| AltitudeTest | 3 | **how high the hull flies**: far over the meadow and the dream-circle, near in the pass, the mirror-wood and the mansion, never a jump between, and the projection scale is 1 at the flight plane |
| TerrainBakeTest | 4 | **the height map**: the world at 4 px a texel, a pass through the boulder belt with every boulder raised, mountains flanking it in cut planes, trees standing up out of the meadow and none in the middle of a lake |
| GroundMapTest | 5 | **the land is a pure function of the world**: a chunk paints the same bytes twice and is just the function sampled (no seams), every band reads as its own place (distinct mean colours, neither black nor white; the surface green, the mirror-wood cool, the throat warm), the light holds mid-band and eases across a boundary without a jump, a band is the last start below the point, a chunk paints fast enough to stream |
| AstraTest | 32 | **the Astra rite**: five shards pay a release and nothing is forged, paid at the loosing, hold to invoke and let go to loose, a faltered mantra and a dead hull are free, the cage grows with the hold, locks follow the nose and ignore what is out of view, each astra's boon and character (shield, boost, stone skin, the Flood's mending, the Pyre's splash, the Gale's chain), the Brahmastra, X and the fallback, the guest's volley on the hub |
| FramesTest | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | frame pacing helpers |
| FxImagesTest | 5 | 5 | 5 | 5 | 5 || 5 || 5 || 5 || 5 || 5 | beam, ring and all 16 thrust frames cropped from the pack, white, opaque at the core and trimmed to their ink; the plant sway ping-pongs over any sheet length |
| GateTest | 4 | 4 | 4 | 4 | 4 || 4 || 4 || 4 || 4 || 4 | Bhu Loka is bare, the five gate walls stand solid except at their centre doors, the hull crosses every doorway, hull-sized spawn->arena BFS inside the world bounds |
| GuardTest | 2 | 2 | 2 | 2 | 2 || 2 || 2 || 2 || 2 || 2 | the compound is a ring wall with a four-tile door over a courtyard carved clear of rock; every shop + checkpoint holds a rank-1 leader and two sentries, Bhu Loka holds none |
| HangarTest | 17 | 17 | 17 | 17 | 16 | a fresh run starts with only Mk-1 and 0 coins, **a hull this run has not bought cannot be fitted or cycled to**, buying a hull deducts coins and opens its tiers, `1-7` on an unbought hull is a no-op, `Q`/`Z` cycle owned hulls only, tier x.3 wants 3 shards on top of coins (spent deepest stack first), selling shards pays 20c each, magnet is fixed at 15, **every shard you hold is forging the Astra**, **shards the loka does not pay never forge**, **the forge banks shards per loka** |
| HomeTest | 4 | 4 | 4 | 4 | 4 || 4 || 4 || 4 || 4 | the menu cursor wraps both ways, the rows stay PLAY / SETTINGS / QUIT, **the logo is drawn at 2x (800x480 off the 500x300 asset)**, and **the bottom row clears the footer hint** |
| HudTest | 3 | 3 | 3 | 3 | 3 || 3 || 3 || 3 || 3 || 3 | the yojana tape falls as you climb and rises as you drop, and every pip stays inside its column |
| OrbTest | 3 | 3 | 3 | 3 | 3 || 3 || 3 || 3 || 3 || 3 | hidden/visible cycle, spin + animation, open spots avoid walls |
| PauseScreenTest | 2 | 2 | 2 | 2 | 2 || 2 || 2 || 2 || 2 || 2 | the three rows are RESUME / SETTINGS / QUIT TO TITLE, and the run-state block stays gone (it duplicated the HUD, with the run still visible behind the ink) |
| PlumeAnchorTest | 6 | 6 | 6 | 6 | 6 || 6 || 6 || 6 || 6 || 6 | **the flame sits PLUME_ANCHOR back along the exhaust axis at every heading**, always behind the nose, above the hull when the nose points down, **clearing the drawn art and not just the collision box**, **riding the dip down with the hull**, and the dip never shifts it sideways |
| PopupArtTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | the popup art on disk |
| PopupsTest | 5 | 5 | 5 | 5 | 5 || 5 || 5 || 5 || 5 || 5 | popup content and lifetime |
| SaveTest | 6 | 6 | 6 | 6 | 6 || 6 || 6 || 6 || 6 || 6 | **only `best` persists**: a run's coins, hulls and tiers do not survive, a fresh Hangar is at defaults, a corrupt or foreign-version file is ignored rather than fatal, and `best` clamps into range |
| ScoreTest | 10 | 10 | 10 | 10 | 10 || 10 || 10 || 10 || 10 || 10 | kill value = loot tier + kind hp (every kind's own hp), combo multiplier steps at 5/10/15/20 capped at x5, the new tier pays on the bumping kill, window expiry drops the chain, death breaks the chain, total saturates at the ten digits, formation fall pays break then wipe, region entry pays once per band, checkpoint pays once, Bala's fall pays the flat 5000 |
| SettingsTest | 3 | 3 | 3 | 3 | 3 || 3 || 3 || 3 || 3 || 3 | the settings screen |
| ShipArtTest | 3 | 3 | 3 | 3 | 3 || 3 || 3 || 3 || 3 || 3 | every ship has three 32x32 variations on disk; variation follows the bought tier; plume levels ramp 0->3 with hold time and boost stretches the plume x2 on the same ramp |
| ShipTest | 22 | 22 | 22 | 22 | 22 || 22 || 22 || 22 || 22 || 22 | screen-relative thrust/strafe, WASD never turns, Q CCW / E CW, **the **the shield costs damage x1 with a floor of 5**, boost drain/regen, invulnerability, respawn at region start, the thrust hold ramp, exhaust trail and hull smoke, **the hull dips on hit and eases back** |
| ShopTest | 6 | 6 | 6 | 6 | 6 || 6 || 6 || 6 || 6 || 6 | one sign per Atala band 1-4 (none in Bhu, none in the boss band), alternating sides above the checkpoint, full-hull bay docking runs to completion, leaving/closing parks the bay, the hull must fit entirely, sign board + legs are solid, 32x32 undocked + doc1..doc6 art on disk |
| ShopUiTest | 14 | 14 | 14 | 14 | 14 | **the hangar**: the tab strip and its three sections, the opaque background, every hit box matches what is drawn, row baselines are single-sourced, owned/unowned/locked hull states, prices and afford states, **the ASTRA tab is one row and says what a release does**, the forge label tells forged-but-spent apart from not forged, and the footer hint |
| TileMapTest | 8 | 8 | 8 | 8 | 8 || 8 || 8 || 8 || 8 || 8 | symmetry, south-north connectivity, open-space ratio, trim rule, push-out bounds, arena passages, maya vana corridor, edge mask over buried / west-exposed / off-grid / concave shapes |
| VajraImagesTest | 2 | 2 | 2 | 2 | 2 || 2 || 2 || 2 || 2 || 2 | the seven bands split the 32px blade with no gap, no overlap, no short band; `assets/vajrayudha/vj.png` is on disk |
| WeaponCardsTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | the weapon card art on disk |
| WeaponTest | 11 | 11 | 11 | 11 | 11 || 11 || 11 || 11 || 11 || 11 | single/double/buckshot counts, burst scheduling, charge power and release gating, damage tier leaves bullet speed alone, laser vs beam, homing, wallbore, bomb ring |
| WorldContactTest | 4 | 4 | 4 | 4 | 4 || 4 || 4 || 4 || 4 || 4 | contact shoves both apart along the contact line, no contact means no shove, ram damage with its cooldown and the recovery window, hp bar eases onto the new value |
| WorldMapImageTest | 1 | 1 | 1 | 1 | 1 || 1 || 1 || 1 || 1 || 1 | writes `tools/atala_map.png` from the live World |
| HudScreenshotTest *(lwjgl3)* | 3 | renders one scene per shop tab plus the flight HUD and pixel-checks the frameless bar block, the three fill colours, the dark track regions, the life pips, the score and combo, and the yojana tape, against the public layout constants |

## Not built

Region routing flags, landmarks, haze, named chambers, the magnet purchase row (deferred;
magnetism is fixed at 15), ammo/refill, the other six lokas and their bands, audio,
minimap, objective markers, a non-empty Bhu Loka opening, wall line-of-sight light occlusion, the
home and settings screens still drawing raw text loops instead of the component kit, and art
beyond the placeholder set.

## The story

Choosing **NEW GAME** plays the story before the run begins: 19 panels over three pages, each with the narrator's
line typed in a black band beneath (SPACE finishes the line, then turns the panel; A goes back; ESC skips the whole
story straight into the run). The title screen comes first and is never the story; CONTINUE never replays it. The art
is `assets/story/pN-K.png`, cut and cleaned from the three source pages in `story_panels/` by
`python tools/story/split_panels.py`; the lines are in `core/.../screen/StoryScript.java`. With no art on disk NEW GAME
goes straight to the run. In the story, Trivikrama is Vishnu, who asks Indra to lend him the Vajra, breaks it in seven
and sends a piece to each loka, to test Indra's faith. (Tests run with `-Dvajra.nostory=true` so a menu test can press
NEW GAME and land in the run.)

## Continue

The title opens on **CONTINUE** when the pilot left a solo run mid-flight, then NEW GAME, MULTIPLAYER and the rest;
with no save it opens on NEW GAME. A run is parked by SAVE AND QUIT in the pause menu, by closing the window, and
by a quiet autosave every 20 seconds (never during a cutscene or the hangar). The park is the whole run: the hull
and where it hangs, the purse and everything bought, score and kills, the foes already fallen, checkpoints, caches
and wrecks taken, the people already met and the jobs they gave. CONTINUE does not replay the opening or any first
meeting. Foes still standing wake at full armour. NEW GAME over a save asks once (press ENTER again) before it
erases it; dying with no lives left, or winning, ends the run and clears it. Multiplayer flights are never parked.
The save lives in the pilot's `profiles.json` (`run`); tests write theirs under `build/testdata/`.

## The flight HUD (the vedic instrument set)

This is the game's one HUD (the classic set and its F9 / SETTINGS switch are gone): `core/.../screen/VedicHud.java`. It follows the pilot's own sketch, in a quiet muted gold, every instrument hanging from one 18-unit
margin, drawn straight onto the game with no tinted plate behind them:

- **Top-left:** a round **weapon medallion** (the pattern's card, the weapon key under it), and out of it a long glass
  **health tube** - a capsule on its side, the hull a liquid flowing in it (jade, amber, vermilion and pulsing under 30%),
  ringed in gold bands. The **lives** are small rings under the tube (dashed once spent), the pattern's name beside them.
- **Bottom-left:** the **minimap** dial with two curved **glass tubes** round it: the **shield** (sky) is the left half and
  the **boost** (saffron) the right, each a gold-railed bore the liquid fills from the bottom up. The liquid has a darker
  body and a brighter top, light bands drift along it (faster while the shield is up, much faster with streaks while you
  boost), bubbles rise, and the front is a sloshing meniscus; a spent shield shows a thin red edge. On the dial itself:
  a radar sweep that makes each body ping as it passes, **awake** foes red and **sleeping** ones grey, leaders ringed
  pale and elites ringed gold, post-keepers amber squares, **people** blue, the **commander** a pulsing diamond held on
  the edge when far, **loot** (coins as specks, health as green plus, shards in their tatva, the Vajrayudha piece as a
  pulsing star), **incoming rounds**, the other hulls of a shared flight in their seat colours, Maya's wrecks while she
  asks for them, and the gate, bay and checkpoint markers as before.
- **Top-centre:** **Bala's gaze** as an eye whose lid and iris open as he notices you - and only then: it stays shut away
  while he is unaware of you (gaze stage 0) and fades in once he has noticed.
- **Bottom-right:** **one small astra gem** in a ring of five cells, and nothing else (no counts, no labels): the
  loaded astra's own element gem, a cell lit for each shard of the five a release costs (the Brahmastra's five are the
  five elements), a whole glowing ring meaning a release is in hand, a thin gold line draining while it recovers, and
  while you HOLD F the ring fills from the top in the astra's colour with a gold tick where the mantra starts to count.
  A small gold number appears only when more than one release is in hand. `X` chooses.
- **Right edge:** the **world meter** - the depth tape and the loka's name and yojana.

The score, the combo, the hull-and-purse line, the crew, the jobs and the toasts are drawn by `Hud` (the hull-and-purse
line and the crew sit below the medallion).

**The weapon key:** **tap V** for the next pattern you own (B for the previous); **hold V** and a solo flight **pauses** with
the picture **blurred** behind it (`Blur`: a two-pass gaussian over a quarter-size copy; a shared flight cannot pause, so it
crawls instead) and the **weapon wheel** opens round the screen centre - point the mouse at a pattern, let go to fit it (the dead
centre keeps what you have). While a session owns V for push-to-talk, B is the weapon key instead.

**There is no dash.** The dash, its cooldown, its i-frames, its hull smear and its upgrade multipliers have been removed
from the ship, the hangar and every HUD (id 4 of the primer, the old dash lesson, is retired and never reused).
