# The World Plan - seven lokas, one living world

This replaces the "a run is a tunnel" reading of GAMEPLAN.md. It records the story as the
author tells it, what the Puranas give us, and how each piece lands in a 2D shooter. Items
marked **[built]** exist in code today; the rest is the order of work.

---

## 1. The story, in one breath

Nobody in Patala is a villain. **Trivikrama** took the **Vajrayudha** and broke it, and a piece
of it lies with each ruler of the seven lokas. A piece whispers to whoever holds it that the
world around them is theirs. So the rulers rule - not from malice, but because the metal tells
them they do. The pilot goes loka to loka, beats each ruler, takes the piece, forges the pieces
back into the weapon and carries it back. The pilot does not remember who they were (the
Deepening gives it back, loka by loka: the one who lost the bolt is Indra).

What follows from this:
- A ruler is never "evil" in a cutscene. Bala says "I have believed it a long time". When he
  falls: "Take it. It never made me a king."
- Soldiers are loyal, not cruel. They parley first (the gate scene) because that is their job.
- The pieces are the plot AND the economy: each is carried, so the pilot's blade grows whole.

## 2. One connected world [built in part]

All seven lokas are one map, stacked, joined by gates. Nothing resets at a gate.
- **Carried:** coins, the hulls and patterns bought, every loka's **Astra**, every piece.
- **Astra choice [built]:** each loka's forge makes its own Astra (Akasha, Agni, Jala, Prithvi,
  Vayu...). `X` picks which one `F` spends; a dry selection falls to the deepest one in hand.
- **Position is the loka:** enemy strength, shard colour, UI accent and hull effect follow the
  loka the pilot is IN (`World.currentLoka`), not the deepest reached. Unlocks (hulls, patterns)
  follow the deepest reached.
- Not yet carried across *runs*: that law ("a run owns nothing") stands for now. Revisit once
  loka 2 exists - a seven-loka journey is too long to lose in one death. Candidate: the
  **checkpoint ledger** (deepest checkpoint + pieces held) survives a run.

## 3. The ruler's gaze [built]

The ruler feels his people fall through his piece.

| stage | trigger | what the player sees |
|---|---|---|
| unaware | start | HUD bar empty, word UNAWARE |
| noticed | 25% of the kill quota | scene: the camera sweeps to the empty last band, "the piece in Bala's hand grows warm" |
| watching | 60% | scene: Bala speaks to the empty hall - "someone is emptying my house" |
| awake | quota full | scene: Bala appears in the pit, "come up, the last door is open and I am standing in it" |
| waiting | boss present | bar full, ember. The crossing (y 18000) still wakes the fight |

The quota is no longer a hidden number: the bar IS the quota. Per-kind caps stop one easy
enemy from filling it. Every loka reuses the same four stages; only the words and the ruler change.

## 4. What the Puranas give us

Sources read: Bhagavata Purana 5.24 (the Patalas), the Vishnu/Vayu/Padma variants named in
`LokaTable`, and the popular rulers. Where tradition disagrees with the game's table the
recommendation is in the last column. (The game keeps the Bhagavata order.)

| loka | what the text says | game ruler today | suggestion |
|---|---|---|---|
| **Atala** | Bala, son of Maya, who made 96 magics (maya). His yawn made three kinds of women; visitors are given a drink brewed with *hataka* gold and are bewitched | Bala | keep. Theme: **illusion and enchantment** |
| **Vitala** | Hara-Bhava (Shiva) and Gauri; the river Hataki runs gold, from which *hataka* is made | Bhairava | keep Bhairava as the *ruler*, make **Hara-Bhava's shrine** the loka's checkpoint. Theme: **gold and fire** |
| **Sutala** | Bali, richer than Indra's heaven, who gave the three steps. Vamana-Trivikrama stands at his door as doorkeeper | Takshaka | **Bali** is the stronger ruler; Takshaka belongs to the nagas below. The doorkeeper at Bali's gate is the Trivikrama echo |
| **Talatala** | Maya the danava, master of all the *mayavis*, protected by Shiva; the Tripura cities | Kalanemi | **Maya** himself - and Maya Vana in Atala makes him the father we have already met |
| **Mahatala** | Many-headed serpents of Kadru, the Krodhavasha: Kuhaka, Takshaka, Kaliya, Sushena. They fear Garuda | Hiranyaksha | **Takshaka** (with Kaliya as an ally). Hiranyaksha is a Rasatala daitya |
| **Rasatala** | Daityas, danavas, the Nivatakavachas, the Kalakeyas, Hiranyapura; the Panis who stole Indra's cows | Puloman | keep Puloman; Hiranyaksha and Kalanemi are his captains |
| **Patala** | Naga-loka: Shankha, Kulika, Mahashankha, Shveta, Dhananjaya, Dhritarashtra, Ashvatara, Devadatta... later tradition makes Vasuki lord. Jewelled hoods give it light | Trivikrama | **Vasuki** as the last ruler; Trivikrama is the *thief*, found beneath, past Shesha |

Texts also say there is no sunlight in the Patalas - the jewels of the nagas light them - and
no age or sickness. Narada called Patala lovelier than heaven. Use all three.

## 5. Making the world feel alive

Each is a small system, reused by every loka.

1. **Light comes from things.** No sun: the jewels are the day. Lamps brighten near the pilot
   and dim when an area is cleared; a ruler's gaze reddens the far sky. (Lights system exists.)
2. **Routines.** Shopkeepers open and shut their bays on a clock, gate guards change, couriers
   cross a band on a timetable. The player learns the world by watching it.
3. **Atala - the maya wood.** Mirage enemies (decoys that pop), doors that are walls, a *hataka
   draught* pickup that is sweet and costly. The illusion theme is literal.
4. **Vitala - the gold river.** Gold tides carry the hull; fire vents on a beat; Hara's shrine
   quiets the whole band while the player rests.
5. **Sutala - the open hand.** The richest shops, no hostile gate guards, a doorkeeper who
   will not let you pass until you answer him (the Trivikrama echo).
6. **Talatala - Maya's works.** Walls that rearrange between visits, turret-halls on rails,
   a map that is a puzzle. Heavy, slow hulls feel right here.
7. **Mahatala - the hooded deep.** Serpent courts, Garuda's shadow crossing overhead makes
   every naga duck and flee for a few seconds (free kills, a rhythm to the band).
8. **Rasatala - the red court.** Blood-lit, crowded. Panis steal a pickup and run; get it back.
9. **Patala - the jewelled floor.** Everything at once, and the quiet before the last stair.
10. **Rumour.** Every shop keeper tells one fact about the next loka; the codex fills.
11. **The pieces hum.** The nearer the pilot is to a piece, the louder the HUD compass pulses
    gold - the same resonance that wakes the ruler. One mechanic, two meanings.

## 6. How a loka changes how it plays and looks

A loka is not a reskin. Each one changes the **hull** a little and the **UI** a lot.

| loka | colour | hull effect | UI voice |
|---|---|---|---|
| Atala | pearl + gold (Akasha lavender for accents) | mirage: shield flickers; nothing else | jewel-cut corners, gold hairlines (today) |
| Vitala | ember + gold | heat: boost drains slower, shots run hot (+rate, +spread) | frame glows orange, bars with a flame notch |
| Sutala | teal + white | drag: glide longer, turns softer | rounded, wave-line dividers |
| Talatala | brass + stone | inertia: slower to start, harder to stop | riveted square corners, gear ticks |
| Mahitala | green + black | gusts: a drifting push you can lean into | scale-pattern borders |
| Rasatala | crimson + black | thirst: pickups heal less, kills heal a little | pulsing red border |
| Patala | prismatic | all small effects, mild | every accent in turn |

Rule: the effect must be readable in the first ten seconds and never lethal by itself.

## 7. Scaling for seven lokas

`LokaTable` carries `hpScale` and `dmgScale` per loka (1.0 to 5.5 hp, 1.0 to 2.8 dmg) and they
rise geometrically - good. What was missing is a ramp INSIDE a loka, so band 1 of loka 2 is not a wall.
- **Band ramp [planned]:** inside a loka, bands run `0.85, 0.95, 1.05, 1.15, 1.25` of the loka's
  scale, so the first band of loka N is about the same as the last of N-1.
- **Player power** comes from three places and each loka opens one more: coins (hulls/tiers),
  patterns (weapons), Astras (one per loka). Rough target: ~8 minutes of play per band, the
  economy pays for about one purchase per band.
- **Hull gates:** `LokaTable.unlockShip` today opens only two hulls in Atala. Open one more
  hull per loka so the hangar always has something to look at and want.
- **Roster growth:** every loka adds ONE new role and retires none (see 8), so fights stay
  readable while the mix grows.

## 8. The hierarchy every loka uses

Each loka's roster has the same seven roles, reskinned. A **squad leader** (rank 1) commands
a **court**; the court follows him, flees when he falls, regroups on the next senior.

| role | job in a fight | Atala |
|---|---|---|
| Scout | fast, thin, first contact | AURIS |
| Skirmisher | weaves and fans fire | VAKRA |
| Rusher | closes and rams | BHAVI |
| Artillery | slow, marked, indirect | MAYAVI |
| Sentry | holds a post, long tap | LANCE |
| Turncoat | lures, bites when hurt | HATAKA |
| Ruler | the loka's boss | BALA |
| **Gatekeeper** | stands a gate, armoured, parleys first | DVARAPALA **[sprite built]** |
| **Lamp-keeper** | tends a checkpoint, mends allies | DIPAKA **[sprite built]** |
| **Bay-master** | runs a hangar bay, never leaves it | VANIJA **[sprite built]** |

The three keepers are enemies with a job: they hold what the world needs and they fight to keep
it. Defeating one never breaks the gate/checkpoint/shop; it only removes the guard.

Ranks: regular, **leader** (crest), **elite** (gold vents). Rank is drawn on the sprite.

## 9. Enemy sprite sheets [built, hand-drawn]

64x64 frames, one sheet per archetype per loka, `assets/enemies/<loka>/<kind>.png`, 8x4 cells (designs: `docs/ENEMY_BIBLE.md`, source: `tools/enemy_art/`):

| row | cells |
|---|---|
| 0 | idle x4, rage x4 |
| 1 | hit x2, death x6 |
| 2 | leader: idle x4, rage x4 |
| 3 | elite: idle x4, rage x4 |

The colour is the loka's own code, read from its shipped art: **Atala is pearl and gold** with
black apertures, lavender for illusion and ember only in rage (the Atala violet is the *Akasha
element*, which belongs to shards, the Astra and the UI accent, not to the hulls). Each later loka
gets its own hand-drawn palette and death: sparks (Vitala), bubbles (Sutala), rubble (Talatala),
scales (Mahitala), red mist (Rasatala), prism light (Patala). Only Atala is built; its death is
gold glass. Source: `tools/enemy_art/`, run `python tools/gen_enemy_sheets.py`.

## 10. The hangar, redesigned [built]

Cards, not menus. One screen, three rails: **HULL**, **PATTERN**, **ASTRA**. The hull rail shows
the ship as it will look at every tier (x.1, x.2, x.3 - the art that already exists in
`assets/vajra_ships`), locked tiers dimmed with their price; patterns sit on a **wheel** that
mirrors V/B in flight; Astras show one card per loka forged. Gold = affordable, dim = not yet,
ink = locked by loka. ENTER buys or fits, TAB changes rail, ESC leaves.

## 11. Work order

1. [done] Keys: Q/E rotate only, Z next hull, X next Astra; ESC is the way out everywhere; a click no longer restarts a run; the Controls page lists only what exists.
2. [done] Ruler's gaze, three scenes, HUD bar; the quota is visible and keepers are excluded from it.
3. [done] Astra carried across lokas; X chooses; the bay's ASTRA rail shows one card per loka.
4. [done] Enemy bible, nine hand-drawn 64x64 sheets, rage/hit/death wired, three keepers placed.
5. [done] The bay: HULLS / ARMS (wheel) / ASTRA cards, real tier art.
6. [done] Unused files moved to `unused/` (see `unused/README.md`).
7. [done] Grand gate parley, once per new loka; speech bubbles and a box-less narrator.
8. next: `World.currentLoka` (position, not deepest) for shard colour and enemy scale; band ramp; one hull per loka unlock.
9. next: Vitala (cavern, roster reskin via a second palette in `tools/enemy_art`, Bhairava, Hara-Bhava's shrine).
10. [done] The economy, hull and weapon scaling (docs/ECONOMY.md): one hull and a few patterns per loka, weapon ranks and abilities.
11. [done] The script rewritten in the story's words (docs/SCRIPT.md); the three people redesigned (docs/NPC_BIBLE.md): own art, a scene for the first meeting, bubbles after, a job tracker and toasts on the HUD.
12. next: Bala's own sprite sheet (he still wears the older art) and Vitala's roster.
13. next: audio (none exists yet); first-time hints for the 16 lessons not yet taught in flight.
