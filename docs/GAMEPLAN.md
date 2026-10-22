# The Vajra Overdrive World Plan

Living document, like PLAN.md before it. This is the plan for the push that starts now: make
Patala a *place* with voices in it, not a corridor of blood. The premise never moves —
**Trivikrama** broke the Vajrayudha and hid one band in each of the seven lokas — everything
below grows out of scripture, not around it.

---

## 1. Where the game stands today — the honest audit

Measured against the code, not the dream.

| system | state |
|---|---|
| the premise (Trivikrama, seven lokas, seven bands, a run owns nothing) | **shipped** |
| `LokaTable` — all 7 lokas named, wardens named, elements, hp/dmg scales, ship gates, seeds | **shipped** |
| Atala worldgen — 6 bands, gates, quota seal, caches, shops, checkpoints, boss arena | **shipped** |
| Bala — 3 phases (commander / duel / berserk), retinue ring, blink, 4 patterns, tells | **shipped** |
| the roster — 6 kinds + ranks, formation rings, rout on the commander's death, stand-down | **shipped** |
| Hataka's betrayal at half-hp | **shipped** |
| economy — coins √hpScale, shards, pieces, the Astra forge, prizes | **shipped** |
| ship `damageReduction`, `RANK_DAMAGE` capped {1, 1.45, 1.9} | **shipped** |
| hangar / shop / HUD / compass / controls / settings / profiles / netplay | **shipped** |
| **the marks & variants redesign** (5×3 sails, 21 hulls, coin retune, `densityScale`) | **idle** — planned and costed in SCALING_PLAN, phases 4–6 remain |
| **Vitala…Patala** — the other six worlds | **table rows only** — no cavern, no roster, no fight |
| **voices** — NPCs that talk, want things, ask things, remember you | **none** |
| **side work** — missions that are not the gate chain | **none** |
| **ambient life** — traffic that is not out to kill you | **none** |
| **the codex** — a place where the world speaks in peace | **none** |
| **boss memory** — a boss that learns from how you fight *him* | **none** |

Atala as a *circuit* is done — descent, seals, shops, checkpoint, boss, victory card. Atala as
a *world* is roughly a third built: it has no one in it who was not born to kill you. Seven lokas
exist on the ladder; one exists on the map. If the whole seven-world game is 100%, the run today
is **~15%**.

**Why it feels straightforward — said plainly.** Every run is a tunnel: descend, kill until a
silent quota fills, kill the warden, repeat. Nothing in the world wants anything from you except
your hull. The fix is not more length or more damage — it is *breadth per band* and *voices*,
and both reuse machinery the game already has (popups, the compass, shops, the shard economy,
the netplay session).

---

## 2. Design pillars for the liveliness push

1. **A loka is a place, not a gauntlet.** Places to go that are not the gate; people to meet who
   are not enemies; names on the map that are worth reading.
2. **Every fight tells you who these people were.** The warden's phases are already his best
   characterization — keep going the same way (Bala's "STAND DOWN. THIS ONE IS MINE." is the
   standard all content must meet).
3. **Two doors, both true.** Side content pays the *same* economy as the spine — missions are
   not a second-class currency.
4. **The seven elements drive travel.** What a loka pays is what your forge needs; what your
   forge needs decides which world you hurry to.
5. **Solo by default, co-op never an afterthought.** Every mission template must survive the
   two-seat session (`Session` is authoritative-host; the systems below take their state from
   the host and echo everywhere).

---

## 3. The living loka — five systems, built once, reused by all seven worlds

### 3.1 The voices — NPC pilots with story

An NPC is an `Enemy` hull that never fights you (faction `NEUTRAL` in `Enemy.Kind` terms — a new
kind whose `Brain.think` idles, orbits, and gets out of your bullet line), plus three things the
game already has: **`Popups`** for speech, **the compass** for finding them, and the **profile
campaign** for remembering. One new file (`Npc.java`) + one (`Chatter.java`) carry the layer.

Each voice has: a name, a loka, a want, three scripted beats, and a payoff.

**Atala's roster (the pilot-living slice):**

| voice | who | the want | the payoff |
|---|---|---|---|
| **The Ninth Maya** | Maya's last workshop-wright, half-dead beside her forge in Maya Vana | salvage of `wall` tiles to retemper her tools | teaches the forge a second output band; her letter re-frames Bala |
| **Kaliya the Tide-Pilot** | naga courier running the sunless water-roads | escort through bands 1–3 while his caravan floats the channel | opens a Jala shortcut gate — a swimlane beneath band 3 (new `MapGen` carve) |
| **Uparichara's Scout** | a wounded watch-ship grinding his own repose at the band-2 rock | find and raise his crashed beacon (a `cache`-shaped job) | marks enemy camps on the compass; his report starts the quota story |
| **The Renegade Hataka** | the betrayer who hasn't betrayed yet | immunity from his own kind — walk him out | he fights for you in the Bala pit; dies or endures depending on how you met |
| **The Gold Brokers** | the shops' managers, split personalities per band | your business — they buy shards, gossip about later lokas | their gossip is the codex's living page |

Speech is event-keyed, not chatter-spam: `Chatter` fires on first sight, on band crossing, on
quest-state change, on the warden falling. Two lines per event, written like Bala's.

**Story walls, kept:** NPCs never hold the gate. The premise chain (quota seal → warden → band)
stays on the spine permanently; voices add *why*, never *whether*.

### 3.2 Side missions — seven templates, not seven scripts

A mission = a trigger (a voice, a shop bulletin, a murmur at a checkpoint) + a `Mission` row in
the run (state, timer, reward) + one of seven shapes. The shapes are systems; the content is
Asana-named and two lines of story each.

| template | the shape | what it chases | reward band |
|---|---|---|---|
| **Salvage** | N wrecks scattered in a marked pocket | exploration + aim | coins + 1 piece |
| **Escort** | walk a slow caravan ship to a gate, enemies probe in | protection | coins + shard |
| **Bounty** | one named rogue elite with its own brain tag roams a band | duel | big coins + 1 life |
| **The Hold** | wave-defense at a compass point, timed, no healing | stamina | Astra charge |
| **The Covert Way** | a hidden door answers only to a compass clue | curiosity | cache |
| **The Poacher's License** | a kill-quota that names ONE kind only | knowledge of the roster | discount at shops |
| **The Courier** | carry a live spark: hull slows and glows, all aggro rises | nerve | shard + piece |

 proliferation rule: a template earns its place in a loka by texture, not repetition. Atala
launches with **four**: Salvage, Bounty, the Covert Way, and the Courier (Kaliya drives both — his
caravan is the Escort in multiplayer, where passenger convoys niches like this belong to the two-seat
sim). Multiplayer: every template keeps its progress on the host (`Mission` state rides the same
message route the room roster does); a joiner mid-mission sees the board in one line.

### 3.3 Ambient life (two systems, not ten)

- **Traffic lanes** — non-hostile clearways at band edges: a naga caravan every ~40s, lights on,
  they hoot at gunfire. Cost: one `Traffic.java` + reuse of the bullet-line-carving rule.
- **Tide of Shards** — a slow drifting bio-swarm (the loka's element, rendered from `Orb`
  sprockets) settles in one pocket per band; you may *harvest calmly* there — the risk
  equivalence is that guards circle the pocket. This teaches the economy's feel with no fight.

### 3.4 Breadth per band — the pocket structure

Each band stays a descent corridor, but grows **two side pockets and one murmur**:

- **pocket** — a carved dead-end room built at worldgen from the same cave rules: a **supply
  grove** (pothos of coins/shards, lightly guarded) or a **ruin** (a mural + a cache door, word
  worked into the lore). Two named on the compass per band. `MapGen` grows a `carvePocket(band)`
  pass; the compass lists pocket labels.
- **murmur** — a one-line `Popups` whisper keyed to a *place*, unlocked by *being somewhere*
  (writing that rewards exploring, not fighting). Ten for Atala at first cut.

Chase notes: pocket loot must be worse than the mission reward and better than nothing — the
breadth ladder is exploration < mission < boss.

### 3.5 The codex — where the world speaks in peace

A pause-screen tab next to CONTROLS. One page per: loka, warden, roster kind, voice, place
opened. Entries unlock from: meeting voices, murals, the Deepening texts, letters. Storage is a
`boolean[64]` on the profile campaign. It is the single highest-leverage *cheap* item in this
plan — pure strings and one screen, it turns every system above into remembered world.

### 3.6 The boss with more brain

`BossBrain` today is one state machine, phase-keyed, fixed cycle. Deepen it in three taste-able
steps — all throwable at every future warden:

1. **Memory.** The boss tracks *punish per pattern* — damage you did to him while a pattern was
   active (dodged patterns score 0). Every four cycles he retires his two weakest patterns
   against *you* and leans toward the ones that hurt you. Player-feel: "he stopped doing the
   mortar I dodged easily and started the thing I keep failing." Cost: a weight array + `pick()`
   rewrite (~40 lines + a test).
2. **Arena response.** He uses his own room: breaks the gold ring (Bala's retinue forms
   *behind* you instead of around him), claims the compass of the pit, summons by *arc* — the
   field drains toward him. Keeps the feel of talking to a living person at 1500 hp.
3. **The tell gets louder before the pattern, quieter before the lie.** A 0.4s pre-shape flash
   (ring color per pattern) before some *real* patterns, and one *fake* tell (Volley, then
   actually Shock) only in berserk. This is where audio will amplify once §5 audio lands.

**The seven wardens as seven brains** (brain sketches; full specs land as each loka opens):

- **Bala (Atala)** — the honorable commander, today's build + memory in step 1. His duel phase
  stays sacred — no cheap shots at the "stand down" beat, that is the characterization.
- **Agnihotra (Vitala)** — the ember-walled: walls of fire lanes grow as HP falls; the counter is
  the loka itself — he cools blind in the deep water pockets, so the arena's geography bites.
- **Takshaka (Sutala)** — the coiling: he hangs the fight *on* terrain, pillars and lake-lines;
  venom tide rises the longer you let him keep the lake.
- **Kalanemi (Talatala)** — the weight: mirrors your movement; the counter is timing, hitting
  the mirror *at* its own attack.
- **Hiranyaksha (Mahitala)** — the crowded court: he fights with the roster rule, no solo
  patterns at all until the court is emptier than his shadow.
- **Puloman (Rasatala)** — the blood pool: drains your yojana HUD as a resource; he can be
  starved of his own floor by capping the drains.
- **Trivikrama (Patala)** — the finisher: three arenas in one (he fights you as the three worlds
  he trisected — earth, air, and the fifth step that never fell). One fight, three floors, and the
  last floor is a run the pilot should understand by then.

---

## 4. The story, deepened — the same spine, more spine

**Base kept:** Trivikrama broke the Vajrayudha; one band lies in each of the seven lokas beneath
the earth; the pilot descends, re-forges, and takes them back. Changing none of that.

**The deepening — the pilot's name.** The one who lost the bolt to Trivikrama is Indra. The
pilot does not remember this at the start. As bands come home (the build order: the pieces he
carries are the memory), the lokas' voices begin to call him what he is. By Patala, the
warden's of the cosmos *knows who he is*, and so does Trivikrama. Three delivery-weights:

1. **The verses** — scripture popups on phase turns (the Bala pattern).
2. **The letters** — hand-written pieces, one per loka-arc, delivered as a codex page + one popup
   in-situ. First ten written into the plan appendix below.
3. **The mirror** — the warden-murals in ruins: the previous lokas' wardens, and what each one
   asked for. Bosses are readings, not obstacles.

**Atala's arc (three beats, first-run-speak, staged):**

- **Opening (band 1):** the verse popups on the Deepening tell the pilot *down*, not *home*.
  Uparichara's Scout sees something in him.
- **Mid (bands 2–4):** The Ninth Maya's letter — Maya Vana — names Bala without the title:
  *son of mine, who will hold the door too long.* Kaliya's channel makes the whole band readable
  as *his* home, not a stage.
- **Payoff (band 5):** Bala's fight is a duel of honor corroborated by the letter; when he falls,
  the pause screen's first codex page unlocks truthfully. The victory card reads ATALA CONQUERED,
  and underneath, one new line: *the bolt remembers its wielder.*

**The other six arcs** are parked here in one line each, and are written before each world's
wave lands (see §5): Vitala — a forge that refuses its own fire; Sutala — a lake that keeps its
own oath; Talatala — a pillar that holds a kingdom up; Mahitala — a court that has forgotten its
king; Rasatala — a pool that remembers its blood; Patala — the fifth step.

---

## 5. The build order — waves that each ship playable

Every wave keeps the suite green, and content lands *with* its tests. Order is chosen so each
wave makes the next cheaper, and nothing rides on unimplemented economy.

**WAVE 0 — ground truth economy, before new content.** SCALING_PLAN phases 4–6: retune the 15
weapons onto the 70/154/322 ladder, `SailDef` 5×3, 21 hulls with `damageReduction` variants,
`densityScale` per loka, coin base raise. *Why first:* new rewards (missions, pockets, bounties)
have to mean something against an economy that currently caps at 80 dps; also every later wave's
numbers need this floor.

**WAVE 1 — Atala alive.** The template slice, once: `Npc.java` + `Chatter.java` (voice layer),
3 Atala voices, 4 mission templates (Salvage / Bounty / Covert Way / Courier), pockets + murmurs
on bands 1–3, the codex with its first 12 pages, Bala's memory (step 1). *The single week of
proof* that the "straightforward" diagnosis was right.

**WAVE 2 — Vitala (the second world).** One `MapGen` cavern, the six kinds reskinned through
`LokaTable` scales, traffic + shard-tide, Agnihotra's brain, two new voices, the full template
set on hand. This wave is where the world-building *machine* is proven reproducible — that
"add a loka" is a row, a cavern, art and a brain, as the PLAN says.

**WAVE 3 — Sutala + Talatala.** Kaliya's channel system (water nav), Takshaka's coiling,
Kalanemi's mirror, the roster finishing its rank/catwork variety, first leak of the codex.

**WAVE 4 — Mahitala, Rasatala, Patala.** The court of Hiranyaksha, Puloman's blood-floor,
Trivikrama's three-floor finisher. Roster ranks roll to their full spread (2.8x density).

**WAVE 5 — the amp.** Audio beds per loka, boss voice SFX, tells get sound, the HudTheme
seven-palette set. (Sections 5.2–5.3 of PLAN.md, arriving once content exists to carry it.)

**Alongside, always:** netplay missions tests (co-op carry of every template), the "no carries
between runs" law never bends, and the codex stays cheap to extend (strings + a boolean[]).

---

## 6. If everything stops after Wave 1

The run is: a tunnel with speakers and side doors — which is the measured fix for the diagnosis.
If nothing else ever lands, the game is *more alive* with exactly five systems built. That is the
bar for the next milestone.

---

## Appendix — the first ten letters and murmur texts (drafted as delivered copy)

(kept in this file so the texts live in the repo; delivery goes through PopupArt ensuite)

1. **THE SCOUT'S REPORT** — *"His hull is scratched like a shore. He has gone down further than
the quota asked. Whoever you were before the door, be careful what you love."*
2. **THE MAYA LETTER I** — *"Son of mine held the door with a light in his hand that took it for
a lamp. He will stand you down with honor and put his back in front of it until the world comes
through him anyway."*
3. **MURMUR, BAND 1 POCKET** — *"Nobody who built this wall expected the water to rise here. They
built anyway. That is the local religion."*
4. **THE KALIYA DEED** — *"I will run the sunless water road with your name if you walk ahead and
let me die in warmth."*
5. **MURMUR, THE MAYA RUIN** — *The mural is a plan of the Vajrayudha. It has seven bands and
eight names. One name is struck out with a steady hand.*
6. **THE BROKER'S GOSSIP** — *"Deeper loka coin is heavier. You can hear it. Everybody tells you
the rocks are harder. Only the brokers tell you the sounds."*
7. **THE SCOUT'S BEACON NOTE** — *"Beacon down. Not the ship: the EAR."*
8. **THE MAYA LETTER II** — *"Forge slowly. What you make of shards, you make of your own
memory."*
9. **MURMUR, THE GATE'S FOOT** — *The gate has been painted over six times, each a different
century. Under it all the door still says the same sentence nobody can read aloud anymore.*
10. **THE RENEGADE'S PACT** — *"Don't save me from them. Save me from how I'd have betrayed."*
