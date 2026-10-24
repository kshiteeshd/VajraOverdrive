# Multiplayer: effects parity, simplification, and its own story

Written after reading `GameWire`, `Session`, `World`, `Ship`, `ShipRenderer`, `Effect`, `GameScreen` and
`MULTIPLAYER.md`. Nothing here has been run on two machines; the claims below are from the code, and the
two that are inference are marked *(inferred)*.

## 1. Why you cannot see another pilot's smoke and effects

### Cause 1: the pose packet carries a position, not a ship

`SHIP` (30 Hz) sends: hull id, hp fraction, x, y, vx, vy, sender clock, iFrames. That is all.

The renderer (`ShipRenderer`) draws a hull from `facing`, `thrustX/Y`, `thrustHold`, `boosting`,
shield state and weapon charge. **None of those cross the wire.** On a mirror hull they keep their
construction defaults. Consequences, all visible in the code:

| What you expect to see on the other pilot | Why it is missing |
|---|---|
| The hull turning to aim | `Ship.facing` is only ever assigned in `World` (once) and by foes' brains; no packet sets a mirror's. It stays pointing north. |
| Engine plume | `ShipRenderer.drawPlume` returns early when `thrustX == 0 && thrustY == 0`, which is always true on a mirror. |
| Exhaust sparks | They are spawned inside `Ship.update`. A mirror is never `update`d, only glided (`partner.x += vx*dt`). |
| Critical-hull smoke | Also spawned inside `Ship.update`. A dying partner trails nothing. |
| Boost heat, shield bubble, charge glow | All driven by fields the mirror never receives. |
| Their muzzle flash | `applyShot` adds the bullet only; the flash is spawned at the *local* fire choke (`World.spawnBullet`). |

### Cause 2: effects are local state, and nothing replicates them

`World.effects` is a plain local list. Every ring, spark, smoke puff and death animation is created by gameplay
code running in the host's world. The wire carries no effects at all, so on a guest:

- A foe that dies on the host **just vanishes**: `expireMissingGhosts()` removes it with no death sheet, no
  debris, no loot pop.
- Impact sparks, shockwave rings, the boss's roar rings, wind and surge effects never appear.
- Foes jitter slightly: they glide with an exponential lerp to a 10 Hz snapshot.

### The principle that fixes it, and the one that must not be used

**Do not send effects. Send causes, and let each machine regenerate the effect.**
An exhaust plume is 100 to 166 sparks a second per ship. Sent as packets, that would be about 16 kB/s per pilot
and every dropped packet would be a visible hole. The cause ("thrusting, boosting, heading 112 degrees") is
one bit and one byte, and the effect is then drawn by exactly the code that draws it for your own ship.

## 2. The effects plan

### 2.1 SHIP v2: the mirror gets the facts a renderer needs (+6 bytes)

| Field | Size | Replaces what was missing |
|---|---|---|
| `facing` | u8 (256 steps over 2 pi) | hull turning and the aim line |
| flags | u8: thrusting, boosting, shield on, firing, charging, dead | plume, boost heat, bubble, muzzle, charge glow |
| `thrust` / `charge` | u8 + u8 | plume power level, charge ratio |

The packet goes from 25 to about 31 bytes at 30 Hz. Per pilot that is under 1 kB/s: free.

### 2.2 One emitter, two callers (the part that keeps it simple)

Pull the exhaust and smoke blocks out of `Ship.update` into `Ship.emitFx(dt, w)`.
`Ship.update` calls it for your hull; `Ship.mirrorUpdate(dt, w)` calls the **same method** for a mirror, after
setting `thrustX/Y`, `boosting` and `facing` from the last packet. There is then one definition of
what exhaust looks like, so the two can never drift apart. Smoke needs no flag: the mirror's hp fraction already
arrives, and `hp < 30%` triggers it exactly as it does for you.

The mirror's `facing` eases toward the received angle by the shortest arc, so a turn is a turn, not a jump.

### 2.3 EVENT plane (new, host to all, cosmetic and lossy by design)

For the effects the guest can never infer, a small plane of one-shot events, batched per tick, 12 bytes each:

| Event | Payload | Played as |
|---|---|---|
| `DEATH` | kind, rank, x, y | the foe's own six death frames, debris, loot burst |
| `RING` | x, y, radius, colour id | shockwaves, boss roar, wind, Astra release |
| `BOSS_BEAT` | beat id | phase-change flash and the gaze HUD cue |
| `SHAKE` | amount, x, y | screen trauma by distance |

Rules: events are sent twice (id-deduped) because they are cosmetic and must never stall; they are culled by
**interest**, so a seat only receives events within its view plus a margin; and the guest caps particles (about
1,500) and halves a partner's exhaust rate under load. Kill *credit* stays on `PAIN_KILL`; the event plane never
carries anything that changes a score.

### 2.4 Things that need no new bytes

- **Muzzle flash and impact for partner rounds**: spawn them in `applyShot` (it already has the shot's colour
  and velocity).
- **Hit sparks on foes**: the swarm's hp byte already tells the guest a foe lost armour. Spark on every
  decrease, locally.
- **Smoother foes**: swarm to 20 Hz for the nearest 16 foes, and render ghosts 100 ms in the past by
  interpolating between two snapshots, instead of chasing the newest with a lerp. Same bytes per foe, far less
  jitter.

### 2.5 Gate

`MirrorFxTest`: a mirror fed a "thrusting, boosting" packet emits the same spark colour and cadence as an owned
hull; a mirror at 25% hp emits smoke; a `DEATH` event plays the right sheet; the packet round-trips; and a
four-pilot snapshot still fits the MTU rule that `GameWire` already states.

## 3. Making multiplayer simpler

Where the complexity actually is (counted from the code, not guessed):

1. **Two kinds of seat everywhere.** `World` has a `partner` field *and* an `extras` list, so every loop over
   hulls is written twice or three times (glide at ~line 300 and ~392, targeting at ~555, contact at ~755).
   **Fix: one `seats` list** (seat 0 is the host). Every loop becomes one loop; `partner` and `extras` go away.
2. **Two worlds.** The host runs the fight, the guest runs `seatAsClient()` and `updateSeat()`, a reduced world
   of ghosts. That split is correct (the host is the one source of truth) and should stay, but it should be
   *one* class with one switch (`authority`), not branches through `World`.
3. **Hand-counted byte offsets** in `GameWire` (`out[o + 10]`, `body[r + 11]`). A tiny cursor
   (`Bin.Writer` / `Bin.Reader`, `w.u8().f32().f32()`) removes a whole class of off-by-one bug, and makes the
   SHIP v2 change a three-line edit.
4. **The connection flow is already one button**; the remaining friction is the **READY ritual**. Plan: the host
   presses START and everyone in the roster goes; late guests who connect during the first minute drop in at
   the squad's position instead of being locked out.
5. **The game mode, not just the code.** Today multiplayer is "the solo open world, shared". It is huge and
   unstructured, so a session has no beginning, middle or end. A short **sortie** structure (section 4) is the
   biggest simplification a player will feel.

Order matters: do (3) first (small, makes everything after it cheap), then (1), then FX parity.

## 4. The multiplayer story: "The Storm Host"

**It must not retell the solo story.** In the solo game Indra goes down alone and takes back the seven pieces
from the seven rulers. Multiplayer is the other half of the same night, played by other people.

### Premise (Puranic anchor)

Indra's companions are the **Maruts**, forty-nine storm-gods, born when Indra split an unborn child into pieces
so that none would be lost. They have ridden at his side ever since, and they are the only ones who were
*never* given a piece of the broken bolt to hold.

When Indra leaves Swarga to follow Trivikrama's riddle, he orders the Maruts to stay and guard heaven. They do
not stay. The **players are Maruts**, two to four of them, each in a hull (the six marks already in the game),
who go down the same road behind him.

### Why this is a different story, not a reskin

- **Different protagonists and a different question.** Solo asks *"will Indra keep faith?"*. The Host asks *"what
  is loyalty when you were told not to come?"* Trivikrama's test reaches the Maruts too, and it is a quieter
  one.
- **Different enemies.** Solo fights the **rulers** (Bala, Bhairava, Bali, Maya, Takshaka, Puloman, Vasuki).
  The Host fights their **captains**, which the world plan already names (Hiranyaksha, Kalanemi, Taraka-style
  lieutenants). No boss is shared, so neither mode spoils or duplicates the other.
- **A different job.** Indra opens each loka's door by force. The Host is the thing that **holds it open
  behind him**: the doors close, the guard returns, and the way back to Swarga must stay passable.
- **Different tone.** Solo is solemn. The Host banters: four pilots on a voice channel who know each other's
  names, which is exactly what multiplayer already gives you.

### Structure: seven short sorties

One sortie per loka, 6 to 8 minutes, one objective, one captain. That is the simplification in section 3.5.

| # | Loka | Objective | Captain |
|---|---|---|---|
| 1 | Atala | **Hold the arch** until Indra's relay passes | Bala's war-captain |
| 2 | Vitala | **Escort** the gold-tide barge through fire vents | Bhairava's smith |
| 3 | Sutala | **Recover** the doorkeeper's lantern (no hostile gate guards: a rare peaceful one, with a catch) | Bali's steward |
| 4 | Talatala | **Re-route** a shifting maze to open the door | Kalanemi |
| 5 | Mahatala | **Survive** Garuda's shadow (a timed evacuation) | Kaliya's brood-mother |
| 6 | Rasatala | **Take back** what the Panis stole (chase) | Hiranyaksha |
| 7 | Patala | **Carry the light** up the stair: everything at once | Shesha's shadow |

Between sorties the squad is in the lobby for a short briefing and a shared squad hangar.

### Four beats, in the game's own voice

1. **Defiance.** The Host breaks the order to stay. Barks: *"HE SAID STAY." / "HE SAYS A LOT OF THINGS." / "THEN WE ARE LATE."*
2. **The holders.** Each captain tells a Marut the same thing the pieces tell the rulers, in a smaller voice: *the
   piece says you are not needed.* Nobody is a villain here either.
3. **The first loss.** A pilot goes down and is lost to the squad for a sortie (not for the run). The others
   have a choice to make about whether to turn back.
4. **The parasol.** In Patala the Host finds Trivikrama's black parasol, set down and empty. The comic's
   test is explained, lightly: *he was never testing only Indra. He wanted to see who came without being asked.*
   Indra never learns which of them it was. Only the Host does.

### How it plays

- **Roles from hulls, no new systems.** The marks already differ (speed, armour, weapon ladder); the sortie
  objectives are written so each hull has a natural job (the fast one carries, the armoured one holds).
- **Revive instead of respawn.** A downed Marut can be pulled up by a partner who holds position next to them for
  3 seconds. It is the one new mechanic, and it is *the* co-op verb.
- **One shared meter: FAITH.** It starts full, drops when anyone goes down, refills at each sortie's end. At
  zero the sortie fails but the run continues. No game-over screen for four people.
- **No cutscenes in flight.** Multiplayer already settles scene cues instantly (`GameScreen` discards them), which
  is right: nobody should be frozen while three friends get shot. Story lives in the **lobby briefing** (comic
  strips plus lines) and in **non-blocking barks** (the existing speech bubbles, no camera takeover).
- **A bridge back to solo.** Finishing the Host unlocks a cosmetic for Indra's ship in solo (a Marut pennant)
  and one line in his Patala scene, so the two modes know about each other without spoiling each other.

## 5. Phases and gates

| Phase | Scope | Gate |
|---|---|---|
| **A. Wire hygiene** | `Bin.Writer/Reader`, then `seats` list replaces `partner`/`extras` | all existing `CoOpTest` / `SessionTest` / `DeterminismTest` stay green unchanged |
| **B. FX parity** | SHIP v2, `Ship.emitFx` + `mirrorUpdate`, flash and hit sparks locally, then the EVENT plane and death animations | `MirrorFxTest` (section 2.5); two-machine playtest on the PLAYTEST sheet |
| **C. Smooth foes** | 20 Hz near snapshot, 100 ms interpolation buffer | a test that measures ghost jitter under 60 ms latency against today's number |
| **D. Sortie shell** | sortie select, objective runner, FAITH, revive, lobby briefing | headless `SortieTest` for each objective's win and lose |
| **E. Story content** | seven briefings, captain scenes, barks, ending, solo bridge | a `HostScriptTest` mirroring `ScriptTest`'s rules (short, capitals, names only this story has) |

Phases A and B fix what you asked about first (seeing each other properly); D and E are the new mode.

## 6. Open questions for the owner

1. **Roster size:** designing sorties for 2 to 4 pilots (recommended) or up to 8 (the session id space allows it)?
2. **Death:** is "downed, revive in 3 s, lost for the sortie if not" the right feel, or do you want a softer
   rule?
3. **Persistence:** should the squad hangar persist between sorties inside one session (recommended), or reset
   like the solo run's hangar does?
4. **Comic:** should the multiplayer story get its own short comic page, in the same style as the main three?
