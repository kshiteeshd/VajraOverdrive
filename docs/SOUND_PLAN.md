# VAJRA OVERDRIVE - THE SOUND PLAN

*Status: plan only. No audio exists in the game yet (Settings already carries four volumes: master, music, sfx, voice; the tests run with audio disabled). This document is the contract the composition, the sound design and the code will all be built against.*

---

## 0. Intent, and what this is not

**What the player should feel:** that they are flying down through the seven lower worlds of the Bhagavata Purana, carrying a broken thunderbolt, and that the world *sings* back - in the music of the land that wrote the story.

Three rules that decide every later choice:

1. **Composed, not generated.** Every piece of music is a written composition: a raga, a tala, a form, a score. Nothing is produced by a random note generator, and no sound effect is a random synth blip. Variation comes from *authored* alternatives (round-robin takes, written stems), never from randomness in the notes themselves.
2. **Everything is tuned to the story.** A pickup, a bullet, a menu click, a boss and a loka are each an *instrument or a voice from the world*, not a stock "sci-fi" sound. If a sound would fit any other shooter, it is wrong for this one.
3. **The sound is also the interface.** The player must be able to play half-blind: what is firing, what is near, what just changed, how hurt they are, all readable by ear.

**Not wanted:** laser "pew" synth stock, generic epic-orchestra trailer music, "Eastern" cliché loops (a sitar over a drum machine), procedurally-assembled music, text-to-speech, sound packs that do not belong to the world.

---

## 1. The sonic bible (the ideas everything hangs on)

### 1.1 The world descends, so the tonic descends
All music shares one reference pitch, **Sa**, held by a tanpura-like drone. Each loka drops Sa by a semitone, so going deeper is *literally* going lower, and nothing ever modulates upward until the end.

| Loka | Sa | | Loka | Sa |
|---|---|---|---|---|
| 1 Atala | D | | 5 Mahitala | B-flat |
| 2 Vitala | C# | | 6 Rasatala | A |
| 3 Sutala | C | | 7 Patala | G# |
| 4 Talatala | B | | (ending) | back to D, an octave up |

### 1.2 One raga per loka, chosen for what the loka *is*
Raga is chosen for mood (rasa), for the traditional hour it is played, and for how the loka's ruler and story behave.

| Loka | Place / ruler | Raga | Why |
|---|---|---|---|
| Atala | Bala, the maker of illusion; the grand gate | **Yaman** (Bala's fight: **Marwa**) | Yaman is the bright, beautiful raga with the *tivra Ma* - a note that is almost true and not quite: the pilot's own tune (Bhoopali) is Yaman with its two false notes removed. Marwa never rests on Sa or Pa: unresolved dread, the sound of a lie. |
| Vitala | Hara-Bhava's gold; Bhairava, ash and fire | **Bhairav** | Shiva's own raga: dawn, austerity, *komal Re and Dha*, the sound of a god who has no need of gold. |
| Sutala | Bali; water and the gift | **Desh** | The raga of monsoon and longing; rain and the generosity of a king who gave away everything. |
| Talatala | Maya Danava the architect; sorcery and craft | **Malkauns** | The pentatonic night raga of tantra and the uncanny; a craft with no ordinary notes. |
| Mahitala | The serpents (Kahuvaka and the nagas) | **Darbari Kanada** | Slow, heavy, with the wide *andolan* oscillation on Ga and Dha - the slither of a serpent in the note itself. |
| Rasatala | Daityas and Danavas; weight, earth, war | **Shree** | A grave dusk raga, solemn and fierce; the ground itself as armour. |
| Patala | Vasuki, the jewels, Shesha below | **Todi** | The jewel raga: dark, glittering, all komal; it resolves into a bare Sa drone, and the loka has *no pulse at all* when quiet (see 1.4). |
| Title / ending | The pilot, the bolt, the return | **Bhoopali** -> **Yaman** | Beginning as the plain five-note pilot's tune; ending as the full Yaman - the false notes made true. |

### 1.3 Laya is the intensity system
Indian music already has the tool a game needs. Each loka's music is one composition in one **tala**, played in three **layas** (tempi) that double:

| State | Laya | Meaning in play | Beats per minute (matra) |
|---|---|---|---|
| **Drift** | vilambit (slow) | nothing hunting the pilot | ~48 |
| **Alert / Combat** | madhya (medium) | foes awake, firing | ~96 |
| **Crisis** | drut (fast) | boss phase, low hull, or an astra loosed | ~192 |

Because the tempi are exact doubles, the music changes laya on the tala's **sam** (beat one) with no seam: the same melody, twice as fast. Layers (melody, percussion, voice) are separate stems that fade in as the state rises.

| Loka | Tala (beats) | Feel |
|---|---|---|
| Atala | Teental (16) | grand, even, ceremonial |
| Vitala | Ektal (12) | austere, Shiva's cycle |
| Sutala | Dadra (6) | rocking like water |
| Talatala | Jhaptal (10, 2+3+2+3) | intricate, clockwork |
| Mahitala | Jhumra (14, 3+4+3+4) | slow, coiling |
| Rasatala | Chautal (12) | heavy pakhawaj, war |
| Patala | Tilwada (16), *absent when quiet* | time itself thinning |

### 1.4 The leitmotifs (the story's melodic memory)
Short, fixed phrases that recur in every piece in the key and raga of where they appear.

| Motif | Meaning | Shape (in Sa = tonic) | Voice |
|---|---|---|---|
| **VAJRA** | Indra, the bolt, the pilot's purpose | Sa - Ma - Pa - Sa' with a hard strike on each; a rising fourth and fifth | shankha + nagara + nadaswaram, a pakhawaj thwack |
| **THE PILOT** | the player, small and human | Sa Re Ga Pa Ga Re Sa (Bhoopali) | solo bansuri |
| **MAYA** | illusion, anything that is not what it looks | the pilot's motif played *inverted and backwards*, detuned a quarter-tone | santoor, glass harmonica, reversed bell |
| **THE SEVEN** | the seven pieces of the Vajrayudha | the seven swaras Sa Re Ga Ma Pa Dha Ni - one added per piece collected | rising struck bowls; the seventh completes the scale into upper Sa |
| **SHESHA** | what lies beneath everything | a single sub-bass Sa sustained for the length of a breath, seven overtones fading in | rudra veena sub + low voice |

### 1.5 The five elements are five instrument families
Used for the five shards, the five elemental astras, their pickups and their impacts. Their seed-syllables (*bija*) are the real tattva mantras, and they are sung.

| Element (tatva) | Bija | Instrument family | Quality |
|---|---|---|---|
| **Akasha** (ether) | HAM | overtone voice, tanpura shimmer, singing bowl | space, a held halo |
| **Vayu** (wind) | YAM | bansuri, sarangi bow-breath | air, motion, chasing |
| **Agni** (fire) | RAM | damaru, nagara, shehnai high register | crackle, flare, burn |
| **Jala** (water) | VAM | jaltarang (tuned water bowls), santoor | droplets, flowing, soaked |
| **Prithvi** (earth) | LAM | ghatam, pakhawaj, rudra veena low | weight, stone, thud |

**Brahmastra** = all five together plus *Om*. **Pashupatastra** = Shiva: damaru, the low chant of Rudram, and a held silence before it.

### 1.6 Speech is sung
There is no voice acting; the dialogue is text. So each speaker has a **voice instrument**, and a line is *sung*: a fixed, deterministic mapping from the letters of the text to scale degrees of the speaker's raga (vowels pick the note, consonants pick the articulation). The same sentence always sounds the same. This is a composed device, not a blip generator: the mapping tables are written and checked by ear.

| Speaker | Voice instrument |
|---|---|
| Indra | low brass voice: shankha tone and nadaswaram, dignified |
| Bala | sarangi, bowed hard, growling at the bottom |
| Bhairava | overtone throat-singing with damaru ticks |
| Uparichara's Scout | bansuri, quick and light |
| The Ninth Maya | santoor and glass, doubled by its own inverted reflection |
| Kaliya, the Tide-Pilot | jaltarang, wet and rippling |
| The Gold-Seller | tiny gold bells over a ghungroo shake |
| Gauri's Maid | saraswati veena, plucked and warm |
| Dipaka (lamp-keeper) | single clay-lamp bell and a soft flame tick |
| Vanija (bay-master) | a coin-counting kartal clink and a tabla *na* |
| Dvarapala (gate-ward) | pakhawaj hit and a slow horn |

---

## 2. The palette

**Instruments (sampled or recorded, played by a human performer or a high-quality sampled player - see section 9):** tanpura, rudra veena, saraswati veena, sarod, santoor, sarangi, bansuri, shehnai / nadaswaram, harmonium (ritual, sparing), jaltarang, ghatam, pakhawaj, mridangam, tabla, dholak, kanjira, damaru, nagara, ghanta (temple bell), shankha (conch), jhanj / kartal / manjira cymbals, gong, ghungroo.
**Voices:** solo female alap (the goddess voices: Gauri's Maid, the Saraswati of the title), male Vedic-style chant on three pitches (udatta, anudatta, svarita) for astras and bosses, overtone voice (Bhairava, Shesha), whispers (Maya).
**Designed textures (made to sit with the above, never over them):** sub drones, bowed metal, glass shatter, stone and cave air, tape-slowed bells, granular rain. The "sci-fi" of the hull lives here, kept warm and metallic, never digital-clean.

---

## 3. The categories and where the code will call them

| Cat | Name | Owns |
|---|---|---|
| **A** | Music | themes, bosses, cutscene scores, stingers, the adaptive layer system |
| **B** | The pilot's hull | engines, boost, shield, hits, repair, death, respawn |
| **C** | Weapons | 36 patterns: fire and impact (+ riders) |
| **D** | Astras | seven astras, the chanted invocation, release, impacts |
| **E** | Foes | nine kinds: wake, fire, hurt, die, ambience |
| **F** | The rulers | Bala, Bhairava: phases, astras, yielding |
| **G** | The world | ambience per loka, doors, gates, seals, rock, clouds, weather |
| **H** | Pickups and checkpoints | coins, shards, the Vajra pieces, health, lamps, the shop bay |
| **I** | Interface | menus, settings, HUD tour, popups, purchases, wheel, codex |
| **J** | Voices | sung dialogue (1.6), cutscene beats |
| **K** | Co-op | partner presence, join and leave |
| **L** | Mix and technology | buses, ducking, accessibility, performance |

Hook points already in the code that the audio engine will listen to (no new gameplay code is needed to know *when*): `World.cue(Cue)`, `World.spawnBullet`, `Enemy.hurt` / death, `Pickup` collection, `World.astraBegin / astraLoose`, `Ship.damage`, `World.useMedkit`, checkpoint and shop open/close, `Popups`, the cutscene `Director` shots, `World.victory / bossReleases`, `Maya`, `AshCloud`, `AstraFlow`, `DeathSeq`, `HudTour`.

---

## 4. Category A - MUSIC (the compositions)

Each loka has the same set: **Explore** (3 laya states as stems), **Rest** (lamp and bay), **Ruler** (the boss, phased), **Yield** (the ruler gives way), **Veil** (the cloud over the next world). Plus the shared pieces.

### 4.1 Shared pieces

| ID | Moment | Spec |
|---|---|---|
| A01 | **Title** | Sa held by tanpura; Om on a low male voice; the VAJRA motif announced once on shankha; the pilot's bansuri theme in Bhoopali; ends open on Sa. ~2:30 loop. |
| A02 | **Settings / menus** | tanpura and soft jaltarang only; no melody, so the player can talk or set options in peace. |
| A03 | **Opening cutscene** (`OPENING`) | Indra's court: nadaswaram and pakhawaj in Raga Yaman, solemn, a vow. The bolt is broken: a single sharp stroke then ringing silence. The pilot's motif enters on bansuri. ~1:45, scored to the shots. |
| A04 | **HUD tour** | gentle: tanpura, bowl taps timed to each spotlight step; the final "THE KEYS" page resolves on a held Sa. |
| A05 | **Death** | the drone detunes a quartertone down, one sarangi note held, then silence (~4 s); never a "game over" jingle. |
| A06 | **Respawn** | shankha single call, tanpura returns in tune. |
| A07 | **Piece of the Vajra** (7 stings) | the *n*th swara of the Seven as a struck bowl with a chant halo; the 7th plays the whole scale up to upper Sa. |
| A08 | **New loka / veil parts** | the Sa descends a semitone: the drone *slides* down over 6 s while the new raga's first phrase fades in. |
| A09 | **Ending** | full Yaman in upper D; the Seven, the pilot, and VAJRA together; Sa'. (Written last, after the story is settled.) |
| A10 | **Co-op join / leave** | single clay bowl, tuned to the current Sa-Pa. |

### 4.2 Atala (D) - Yaman / Teental 16
| ID | Piece | Spec |
|---|---|---|
| A11 | Atala Explore | Drift: tanpura, long bansuri alap in Yaman, a distant pakhawaj every 4 cycles. Combat: tabla enters with a Teental theka, sarod picks up the *gat* (fixed composition). Crisis: drut, shehnai rides above, jhanj on every sam. Three stems + one shared drone. |
| A12 | Atala Rest | the lamp / bay: tanpura, a saraswati veena playing the Yaman theme alone, unhurried. |
| A13 | **Bala: phase 1** | **Marwa**, Chautal 12 (royal). Pakhawaj, rudra veena, a low male chant on the bija of Maya. Majestic and wrong: the melody never lands on Sa. |
| A14 | **Bala: phase 2** | Jhaptal 10; the shatter phase: the rhythm *breaks* (accents slip off the beat), glass harmonica and reversed bells. |
| A15 | **Bala: phase 3** | **Rupak 7**, the tala that begins on *khali* (the empty beat) - the doors-into-Atala astra. The music starts "on the wrong foot", santoor mirrors itself, the Maya motif everywhere. |
| A16 | Bala yields | solo sarangi weeping in Yaman, Indra answering on low brass; the two voices cross as the camera crosses. |
| A17 | Veil over Vitala | a deep conch under the new Sa sliding down; the Vitala raga's first phrase in the distance. |

### 4.3 Vitala (C#) - Bhairav / Ektal 12
| ID | Piece | Spec |
|---|---|---|
| A21 | Vitala Explore | Drift: tanpura, damaru heartbeat, a slow sarod in Bhairav. Combat: dholak and nagara, shehnai sharpens. Crisis: drut, kartal swarm, fire in the voice. Gold: a bright ghanta-and-ghungroo layer in the gold regions only. |
| A22 | Vitala Rest | Gauri's maid: solo female alap in raga Gauri-like soft Bhairav, veena. |
| A23 | **Bhairava: phases** | Raga Bhairav, **Dhamar 14**. Overtone-voice drone, damaru, low horns, nagara. Phase changes raise the laya and add the throat voice. |
| A24 | Bhairava ash-cloud passage | the music is low-passed under the grey (muffled), then clears when the player leaves the cloud; the mandala's pulse is a slow ghanta. |
| A25 | Bhairava yields | the damaru stops mid-beat; only the overtone drone remains; one bell. |
| A26 | Veil over Sutala | rain begins on jaltarang. |

### 4.4 Sutala (C) - Desh / Dadra 6
A31 Explore (rocking water: jaltarang, bansuri, tabla Dadra; santoor in Combat), A32 Rest (Bali's generosity: a plain, open veena), A33 **Ruler** (to be written when the boss exists), A34 Yield, A35 Veil.

### 4.5 Talatala (B) - Malkauns / Jhaptal 10
A41 Explore (clockwork: tight interlocking tabla and kanjira, santoor patterns that tick like a mechanism, the uncanny pentatonic), A42 Rest, A43 Ruler (Maya Danava himself - the *real* Maya motif, un-inverted for once), A44 Yield, A45 Veil.

### 4.6 Mahitala (B-flat) - Darbari Kanada / Jhumra 14
A51 Explore (slow, coiling; rudra veena with slow *andolan*, sarangi slides, jhumra's uneven 3+4+3+4 pulse; the serpent hiss as a played texture), A52 Rest, A53 Ruler, A54 Yield, A55 Veil.

### 4.7 Rasatala (A) - Shree / Chautal 12
A61 Explore (ghatam and pakhawaj foreground, rudra veena, a war-drum in Combat), A62 Rest, A63 Ruler, A64 Yield, A65 Veil.

### 4.8 Patala (G#) - Todi / Tilwada 16, pulse-less when quiet
A71 Explore (Drift has *no tala*: only the drone, the jewel-like komal Todi phrases on santoor and the Shesha sub; the pulse arrives only with enemies), A72 Rest, A73 **Vasuki / Shesha** final ruler (the largest piece: all five tatva families, the Seven, the Vajra), A74 Yield, A75 the Ending lead-in.

(Section 4.4-4.8 rulers are *placeholders for compositions to be briefed* once those bosses are designed; the raga, tala, Sa and instruments above are fixed so they can be written in advance.)

### 4.9 Form of every Explore piece
- **Length:** 4 tala cycles per section, 3 sections (A: *sthayi*, B: *antara*, A'), loop in the drone so the join is inaudible; ~3:30 unlooped.
- **Stems (all the same length and key, to be mixed live):** DRONE (always), PULSE (percussion), MELODY (raga lead), COUNTER (second voice / response), THREAT (low pressure: sub, bowed metal), VOICE (chant, enters only in Crisis).
- **State to stems:** Drift = DRONE+MELODY(soft); Alert = +PULSE; Combat = +COUNTER, laya doubles; Crisis = +THREAT+VOICE, laya doubles again.

---

## 5. Category B - THE PILOT'S HULL

| ID | Scenario | Sound |
|---|---|---|
| B01 | Engine idle (per hull, 7 hulls) | a soft tonal thrum on the loka's Sa, a different instrument family per hull; heavier hulls sit lower |
| B02 | Thrust | the thrum opens: pitch rises by a fifth with the speed, a breath of wind (bansuri overblow) |
| B03 | Boost | a conch-and-wind surge; the drone ducks 3 dB for 0.3 s |
| B04 | Shield up / hold / down | a bowl struck, a ringing sustain while held, a soft release; low shield: the ring beats (detuned) |
| B05 | Shield hit | a struck gong plate, pitched by damage |
| B06 | Hull hit (light / heavy) | cracked clay and metal; heavy adds a low drum; the drone drops a quartertone for 0.5 s |
| B07 | Low hull warning (below 30%, the smoke state) | a slow heartbeat on the damaru that quickens as hp falls; stops the moment hp recovers |
| B08 | Repair / medkit (key 1) | a rising veena glissando over a pouring water drop |
| B09 | Heated (the forge-lands scatter) | a hot metal tick and tight sizzle on the engine |
| B10 | Mud (inside the ash cloud) | the hull's sounds low-passed and slowed ~8% |
| B11 | Death (see A05) | the burst of particles: a clay vessel shattering, then the detune |
| B12 | Respawn (A06) | shankha; engine thrum restored |
| B13 | Hull variant bought / worn | the tier's own struck bowl: x.1 plain, x.2 two bowls in a fifth, x.3 a triad with a chant halo |
| B14 | Astra gate (variant 1 pressing F) | a dull wooden knock and a short low note: "not yet" |
| B15 | Ship rams a foe | a thud and a clay scrape |

---

## 6. Category C - WEAPONS (36 patterns, fire and impact)

**Design language:** the pilot's weapons are the Vajra's own children - *thunder struck on metal*. A shot is a short struck sound with the loka's Sa ringing under it; each pattern has one instrument voice. Fire sounds are 4 authored round-robin takes; impacts 3. Heavy weapons duck the music by 1-2 dB for 100 ms. Rank III adds a shimmer layer (a halo of the same note an octave up).

### 6.1 The fifteen standard patterns
| Pattern | Fire | Impact |
|---|---|---|
| Single Bolt | one dry vajra-strike: a small ghanta tapped with a metal rod, crisp | a spark: a tight cymbal tick |
| Double Bolt | two such strikes a hair apart (a fifth) | double tick |
| Burst | three quick taps rising in pitch | three ticks |
| Twin Burst | two pairs, the second pair a tone higher | pair ticks |
| Buckshot | a cymbal crash chopped to 80 ms, with a dry rattle | gritty scatter on stone |
| Heavy Shell | a pakhawaj bass hit with a metal ring | a body blow: low drum and clay crack |
| Light MG | tiny fast kartal rattles, tonally pitched to Pa | pin-sparks |
| Charge Shot | charge: a rising overtone voice; release: a cymbal flash and nagara hit | a bright ringing detonation |
| Laser | a bowed metal string drawn taut (sarangi harmonic), sustained while held | a sizzle at the end of the line |
| Bomb | a deep gong thrown, a slow whistle | a huge dull boom, a gong decay |
| Missile | a thrown rope-whoosh plus a small drum | a punchy crack |
| Tracking Missiles | two whooshes in stereo, a faint wind-chime as they turn | two cracks |
| Aim Shot | a clean single bell with a "lock" ping | a clear bell |
| Piercer | a thin high sarod string snap | a slicing tick that continues |
| Wallbore | a deep drill-like bowed stone | a crumble of rock |

### 6.2 The twenty-one patterns of the lokas
| Loka | Pattern | Fire | Impact / rider |
|---|---|---|---|
| Atala | Mirror Shot | strike + a *reversed* copy (the maya motif) from behind | a glass tick |
| Atala | Wind Scythe | a bansuri breath swept past; **return**: the pitch glides back | a cut, then the whistle on its way home |
| Atala | Maya Weave | two santoor plucks, panning left-right in a sine | glassy ticks |
| Vitala | Ember Dart | a damaru tick and a spark | a hot hiss; **burn**: a low crackle loop on the foe |
| Vitala | Ash Spray | a soft breathy shaker (dry grit) | grit on stone |
| Vitala | Cinder Bomb | a short drum thud | a muffled boom, then eight tiny embers pinging |
| Sutala | Tide Bolt | a jaltarang drip with a rising ripple | a wet slap; **slow**: a pitched-down water bubble |
| Sutala | Ripple Ring | a bowl struck, a ring of water droplets outward | quiet rain on the ring |
| Sutala | Boon Shot | a warm veena pluck | a soft chime; **mend**: a rising kindly note on the hull |
| Talatala | Ricochet | a metal ping | each bounce a pitched-up ping (a step of the raga per bounce) |
| Talatala | Arc Chain | a sharp electric tabla *tin* | each jump a rising glissando of tin-tin-tin |
| Talatala | Sigil Mine | a soft set-down: a small tuned bowl "placed" | **arm**: a click; **burst**: a gong and chant syllable |
| Mahitala | Serpent Coil | a low bowed gliss, hissing | a snap |
| Mahitala | Venom Fang | a thin hiss and high needle tick | **poison**: a sour slow-detuned drip loop |
| Mahitala | Naga Cloud | a breath of gas, a soft "pffff" bowl | **cloud**: a drone of hissing breath for its 4 seconds |
| Rasatala | Gravity Well | a low overtone note that *pulls* (pitch glides toward the foe) | **pull**: a deepening drone; burst: a stone slab dropped |
| Rasatala | Quake Shell | a ghatam smash and a pakhawaj thud | **freeze**: the foe's sound stops dead for the stun |
| Rasatala | Shatter Shard | a clay pot dropped | five crystal tinks |
| Patala | Jewel Prism | a gem struck, which then splits into three bell tones (a chord) | three simultaneous jewel notes |
| Patala | Executioner | one heavy sword-like gong strike, silence before | a *finishing* low drum when a wounded foe falls; a normal tick otherwise |
| Patala | Last Light | a long swelling tanpura-and-voice bloom as it grows | a vast bell as it passes through each foe |

### 6.3 Shared weapon rules
- **Priority:** the player's own fire is always audible (never culled); enemy fire is culled first.
- **Rate-limited:** rapid weapons (Light MG, Ash Spray, Venom Fang) play a *loop* with a start and end rather than one sound per bullet.
- **Pitch:** fire sounds are tuned to the current loka's Sa and Pa so combat is harmonic with the music instead of fighting it.

---

## 7. Category D - ASTRAS

Every astra has the same three-part structure: **INVOKE** (hold F: the mantra builds), **LOOSE** (release: the weapon), **LIVE** (its effect lasting, its impacts). The invocation is a *real chant of the tatva's bija*, layered as the charge rises.

| Astra | Bija | INVOKE (hold) | LOOSE | LIVE and impact |
|---|---|---|---|---|
| **Akasha** (the Hunter) | HAM | overtone voice and tanpura shimmer; cubes gather as bowl taps | a clear ringing vault, a vast singing bowl | each seeking cube: a bright crystal ping; shield refill: a warm swell |
| **Vayu** (the Gale) | YAM | bansuri multiphonics, a rising wind | a gale shriek and shehnai cut | chain from foe to foe: each jump a tabla *tin* and a whip; boost refill: a breath |
| **Agni** (the Pyre) | RAM | damaru roll quickening, crackle | a nagara hit and a roaring fire bloom | long burning roar for the bloom's 2.9 s; the burn: low crackle; coin-on-burn kill: a tiny gold ghungroo |
| **Jala** (the Flood) | VAM | jaltarang drops increasing to a stream | a great wave: water, a deep gong, a santoor cascade | the flood: a long surge loop; drowned foe: a bubble; hull mended: a rising chime |
| **Prithvi** (the Mountain) | LAM | ghatam and pakhawaj, low chant | a boulder-fall: stone slab and gong | each boulder: a heavy thud; stone skin: a sustained low drone |
| **Brahmastra** (the Last Arrow) | OM and all five | the five bija chanted in sequence, then together; a lotus light: a rising shruti chord; the camera zooms out slowly - the sound *opens* with it (reverb widens) | silence for one beat, then a full-orchestra Om and a cymbal swell | the petals (single): a tiny struck chime each; arrows: whooshes in all directions; a long decay |
| **Pashupatastra** (the Third Eye) | RUDRA | the world quietens; damaru, a Rudram whisper | a held, low beam: a tanpura-and-voice sustain | beam burn: a sustained saw of overtone voice; foes in the beam: the voice splits upward |

Failure states: invoking and letting go too soon = a faltering mantra (the chant breaks mid-syllable, no cost). Dying mid-mantra = the chant cuts out in the middle of a syllable (A05).

---

## 8. Category E - FOES (nine kinds)

All foes share rules: **wake** (a sting when they notice), **fire** (own timbre), **hurt** (a short, pitched tick), **die** (the foe's instrument shatters), **ambient** (only when near, low volume). Each kind's timbre says what it is.

| Kind | Role | Wake | Fire | Hurt / die |
|---|---|---|---|---|
| **Mayavi** | the lobber; mystical | a santoor shimmer | a soft glass bell and a slow whoosh (the *lob*) | glass crack; marker ring on landing: a ticking bowl |
| **Hataka** | the weakling that betrays | a cheap tin bell | a thin tin ring; at half hp, the *betray* ring: a bright metallic burst | tin crumple |
| **Auris** | snappers, fast | a quick tabla *tak* | a dry snap | a snap and a pop |
| **Lance** | the tapper | a pakhawaj tap | a lance-tap: a clay *tuk* | a thin fall |
| **Vakra** | the fan-firer | a bansuri hiss | a fan of three fast clicks | a curved crack |
| **Bhavi** | the ore-plated, slow and heavy | a low ghatam note | a copper thud | a heavy clang then a collapse of ore |
| **Dvarapala** | gate-ward, armoured | a horn, slow | a heavy shell: horn + slab | armour: a blunt "tonk" when plate blunts a hit |
| **Dipaka** | lamp-keeper; mends its comrades | a small flame tick | a spark | **mend**: a warm bell on each foe it heals; its death: a lamp going out |
| **Vanija** | bay-master, never leaves | a coin kartal | a "toll": a coin clink and a tabla *na* | a handful of coins spilled |

**Vitala variants** (the same kinds with better brains) add *dodge* and *reserve flight*: a dodge is a quick breath-whoosh; the reserves flying in from below are a rising pakhawaj roll and a shehnai call; no diving, so no submerge/emerge splash for Vitala.
**Underwater/dive (Atala foes):** submerge = a soft water close, emerge = a surfacing ring.
**Status on a foe:** burning (low crackle), slowed (pitched-down), frozen (its sound stops), poisoned (a sour drip).
**Routs / call-to-arms:** a bell and drum alert when a group wakes.

---

## 9. Category F - THE RULERS

### 9.1 Bala (Atala)
| Moment | Sound |
|---|---|
| Wake (`BALA_WAKES`) | the pakhawaj one beat late; a low chant of "Maya"; A13 starts |
| His attacks (BossBrain patterns) | each a pakhawaj-and-sarangi phrase; telegraphs are *audible* a half-second before the bullet (the Marwa phrase rises) |
| **Maya-astra** (three modes) | **forging**: the world's sounds are pulled into a bowl; a rising reversed whisper; **shatter**: a mirror breaking in three tiers (A14); **modes 1 and 2**: pieces turn one at a time with a bell per piece, then all at once; **mode 3 (doors)**: the doors open with a deep door-gong, each window a faint, distinct *sonic window* (the live door's sound a muffled copy from there); the crack origin hisses at his feet |
| Frozen Bala | a held chord; he is silent |
| Stagger / invuln | a muffled gong |
| Yield (`BALA_FALLS`) | A16; the piece flies to the pilot: A07 note 1 |
| Retinue (orderRetinue) | a drum cadence as the ring forms |

### 9.2 Bhairava (Vitala)
| Moment | Sound |
|---|---|
| Wake (`BHAIRAVA_WAKES`) | damaru roll from far away; A23 |
| His state 7: the **ash cloud** | a long breath and a rain of fine grit; the mandala draws as a slow grey ghanta; inside the cloud, all sound is slowed and low-passed (B10) |
| Barks | a growl of overtone voice |
| Yield | A25; piece 2 |

### 9.3 Later rulers
Raga, tala and Sa are fixed (4.4-4.8); their attacks are briefed when the bosses are designed.

---

## 10. Category G - THE WORLD

### 10.1 Ambience beds (always a *musical* bed, not noise)
Each loka has two beds: **air** (always) and **place** (by region).

| Loka | Air | Place beds |
|---|---|---|
| Atala | distant wind, an underworld vault with faint chimes of the hood-gem stars | the grand gate, the maya wrecks, the forest of Maya Vana (a hush, leaves of metal) |
| Vitala | low furnace breath, ash falling, a far damaru pulse | gold regions (a warm ring), the altar (a deep vault) |
| Sutala | a gentle rain, the long hum of a subterranean sea | the Plunge, waterfalls |
| Talatala | tiny clockwork, ticking of gears, a thin hum of sorcery | the architect's halls |
| Mahitala | a slow breathing hiss, stone creaking, scales sliding | the nests |
| Rasatala | deep stone-groan, distant forges, drum | the war-camps |
| Patala | near-silence and a sub drone; jewel chimes; then Shesha, the largest sound in the game | the jewel halls |

### 10.2 The world's mechanisms
| ID | Scenario | Sound |
|---|---|---|
| G01 | The grand gate opens (`ATALA_GATE`) | a long door-gong, stone sliding, a conch call |
| G02 | A seal opens after a ruler | a bright bowl cascade + stone cracking |
| G03 | The cloud veil over the next loka (`unveil`) | a sustained wind and a pad; the cloud *parts* with a breath and a rising choir |
| G04 | Crossing into a new loka (A08) | the Sa slides down |
| G05 | Rock hit by bullets / areas | stone ticks; dust |
| G06 | Doors in a rooted wall | a rumble and stone shift |
| G07 | Water flood (Jala) | surge, dripping, foam crest, spray (the three modes of the flow) |
| G08 | Ash cloud (Bhairava) | see 9.2 |
| G09 | Treasure chests (docs/TREASURE_CHESTS.md) | a lid creak and a small bright ring |
| G10 | A wreck taken (Maya's grove) | a hollow bronze note |
| G11 | The flowing particle systems (petals, ash, rain) | quiet, bed-level; part of the ambience, never a distinct sound |

---

## 11. Category H - PICKUPS, LAMPS AND BAYS

| ID | Scenario | Sound |
|---|---|---|
| H01 | **Coins** | a ghungroo shake and a single tiny bell; the pitch climbs a scale step per coin in a quick chain (a combo), resets after a second |
| H02 | **Shards** (five elements) | each its element's bowl (1.5): a singing bowl, a bansuri breath, a damaru tick, a water drop on jaltarang, a ghatam knock; the fifth of an element completes a release: a chant syllable of that bija |
| H03 | **Health drop** | a bigger, warmer veena swell |
| H04 | **Medkit stored** (the stored repair) | a clay bell tick |
| H05 | **A piece of the Vajra** | the flying piece: a rising shruti hum as it seeks the hull; arrival: A07 |
| H06 | **Checkpoint lamp bound** (`FIRST_LAMP` and every one) | a flame catching: a soft whoosh, a temple bell, the lamp's hum *tuned to the current Sa* |
| H07 | **Respawn at the lamp** | the lamp's note swells; A06 |
| H08 | **Bay (shop) approach and dock** (`FIRST_BAY`) | a hush, a low bell, the bay's own warm room tone with the **Rest** music |
| H09 | **Shop: buy** | a coin on stone, a bowl ring |
| H10 | **Shop: short / locked** | a dull wooden knock |
| H11 | **Temper** (rank II / III) | a hammer on an anvil, a bell; III adds a halo chord |
| H12 | **Sell shards** | a handful of coins poured |
| H13 | **Fit a weapon / hull** | a latch and a short bowl |
| H14 | **Close the bay** | the door sound and the music returns to Explore |

---

## 12. Category I - INTERFACE

| ID | Scenario | Sound |
|---|---|---|
| I01 | Menu move | a soft tap on a clay bowl (pitched to Sa, Pa, Sa' by position) |
| I02 | Menu select | a temple bell, short |
| I03 | Menu back | a lower, softer bell |
| I04 | New game | a conch call, the title music fades |
| I05 | Settings: slider | a tick per step, pitched with the value (the volume slider plays a reference tone) |
| I06 | Settings: toggle / rebind | a latch sound; rebinding: a "listening" faint tone then a clear note when set; a conflict swap: a short two-note exchange |
| I07 | HUD tour step | a bowl tap per spotlight; ENTER/SPACE: a page-turn; ESC skip: a soft close |
| I08 | Popups (score, combo tier, the "1/7 Vajra" popup) | combo tier: a rising scale; the Vajra panel: A07 |
| I09 | Dialogue box | open: a soft shruti; the text *sings* (1.6); advance: a tap |
| I10 | Weapon wheel (hold) | the world slows in sound: a low-pass and a ring appears as a bowl; each hover a small tick; release: the fit sound |
| I11 | Codex page unlock | a page turn and a bell |
| I12 | Chapter / loka title card | the loka's raga's first phrase |

---

## 13. Category J - VOICES AND CUTSCENES

The cutscene score is **written to picture**: each shot has a hit point (a camera cut, a speaker, a reveal) that lands on a beat of the tala.

| Cue | Content |
|---|---|
| `OPENING` | A03 + Indra's sung voice |
| `ATALA_GATE` | G01 under a pakhawaj roll; Atala's first phrase on the cross |
| `BALA_WAKES` | A13's opening; Bala's sarangi voice |
| `VOICE_SCOUT / MAYA / KALIYA` | each with their voice instrument (1.6) and their own theme in five bars |
| `GAZE_NOTICED / WATCHING / ROUSED` | the piece-in-the-ruler's-hand: a warming tone that builds across the three beats (three notes of the Seven, unresolved) |
| `BALA_FALLS` | A16 |
| `VITALA_ENTER` | the Sa slides down a semitone (G04) |
| `VOICE_SELLER / MAID` | their voices |
| `BHAIRAVA_WAKES / FALLS` | A23 / A25 |
| `FIRST_LAMP / FIRST_BAY` | a soft guiding motif (the Pilot, on bansuri), then the lamp's or bay's own hum |
| Death sequence | A05; the camera moving to the respawn is accompanied by the tanpura returning |

Dialogue is *never spoken text-to-speech*; the sung line is quiet (voice bus), and the **textSpeed** setting scales its tempo.

---

## 14. Category K - CO-OP

| ID | Scenario | Sound |
|---|---|---|
| K01 | Partner joins / leaves | A10 |
| K02 | Partner's engine and fire | the same sounds, **panned to their position** and 3 dB quieter; fire sounds tuned a third higher so the two pilots are distinguishable |
| K03 | Partner's astra | seen and heard: the chant is quieter and further |
| K04 | Push-to-talk | a small tap on keydown; their voice is the player's voice - the music ducks 4 dB while a partner speaks |
| K05 | Guest sees the host's world | rounds sent over the wire carry no weapon-pattern key (the guest draws plain discs today); sound for those is the *generic tuned tick* until the wire carries a pattern id (a to-do, section 17) |

---

## 15. Category L - MIX AND TECHNOLOGY

### 15.1 Buses (map to the existing four volumes)
`MASTER` -> `MUSIC` (stems) / `SFX` (`WEAPONS`, `FOES`, `WORLD`, `UI`) / `VOICE` (sung lines, mantras). The settings tabs already expose master, music, sfx and voice; one more slider is proposed for ambience, optionally.

### 15.2 Ducking priorities (highest wins)
1. **Mantra / astra release** - ducks MUSIC by 6 dB, FOES by 3 dB for its length.
2. **Boss telegraph** - ducks MUSIC by 3 dB for the telegraph only.
3. **Dialogue / cutscene voices** - ducks MUSIC 4 dB, WORLD 6 dB.
4. **Player hurt** - ducks MUSIC 2 dB for 400 ms.
5. Pickups sit on top and never duck.

### 15.3 Voice limits
Max 48 concurrent voices: player 12 (never stolen), astra 6, foes 14 (stolen by distance), world 8, UI 4, voice 4. Looped sounds count once.

### 15.4 Spatialisation
2D stereo pan by on-screen position; distance volume to the edge of the screen + 25%. Off-screen foes that fire are heard dull (low-passed) and panned *at the edge* - an early warning.

### 15.5 Accessibility
- **Reduce flash** has an audio sibling: *reduce sharp sounds* (caps high-frequency transients; the shatter becomes a duller ringing).
- **Gentle camera** never changes sound.
- Every key audio cue has a visual counterpart already (health, shield, astra ring); every visual alert gets an audio cue (see B07, boss telegraphs) so either alone is enough.
- A **mono** switch for the mix.

### 15.6 Performance
Music as streamed OGG (stereo, 44.1 kHz, ~160 kbps) in separate stems; SFX as OGG (mono, 44.1 kHz) loaded in banks per loka. The title, shop and hangar load their own small banks. The simulation runs without sound (tests use `disableAudio`); the audio engine listens to world events and never feeds back into the simulation.

---

## 16. Asset budget

| Category | Items | Authored takes | Notes |
|---|---|---|---|
| A Music | 5 shared stings + 7 x (3-stem Explore, Rest, Ruler phased, Yield, Veil) + title / opening / ending | ~120 stems | the heart of the work |
| B Hull | ~15 sound families (x7 hull engines) | ~60 | |
| C Weapons | 36 patterns x (fire x4, impact x3) + riders | ~280 | |
| D Astras | 7 x (invoke loop, 4 charge layers, loose, 3 impacts, live loop) | ~80 | + the five real bija recordings |
| E Foes | 9 kinds x 5 events x 3 takes | ~135 | |
| F Rulers | 2 now + 5 later | ~60 now | |
| G World | 7 lokas x (air + place beds) + ~30 mechanism sounds | ~70 | |
| H Pickups / shop | ~25 | ~70 | |
| I UI | ~30 | ~40 | |
| J Voices | 11 speakers x 12 syllable-notes + the sung-mapping tables | ~140 | |
| K Co-op | 6 | ~10 | |

About **1,000+ authored files** at full scope; the first playable slice (section 18, phase 2) needs under 300.

---

## 17. The engine work (code), in order

1. **`AudioEngine`** (core, behind an interface so tests and the headless game keep running silent): sound banks per loka, music stems with a master clock in matras, bus volumes from `Settings`.
2. **`MusicDirector`**: owns the laya state (Drift / Alert / Combat / Crisis), the stem mix, quantised transitions on sam, the Sa slide between lokas, duck handling.
3. **`SoundEvents`**: one place that listens to `World` (the hook points in section 3) and turns events into sounds, so the simulation stays audio-free.
4. **`SungVoice`**: the deterministic text-to-note mapping per speaker.
5. **A shot-pattern id on the co-op wire** so a guest hears the right weapon (K05).
6. **Tests:** every cue maps to a registered sound; the music clocks never drift (a test plays a stem for 100 cycles and checks the sample count); all four buses clamp; a missing file is silent, never a crash; the headless path is silent.

---

## 18. Build phases

| Phase | Deliverable | Gate |
|---|---|---|
| **0 - this document** | the plan, agreed | you approve it, change what you like |
| **1 - the score** | for Atala: **the full written score** (Yaman, Teental, Marwa bosses, three layas) as notation, plus the leitmotif sheet and the five bija recordings brief; the sung-voice mapping tables for all speakers | you audition the Atala pieces before more is made |
| **2 - the vertical slice** | engine (items 1-3), Atala music + SFX for hull, weapons (the starter patterns), pickups, UI, the foes in Atala, Bala | plays well on the existing Atala and Bala |
| **3 - Vitala** | Bhairav and its set; Bhairava; the other weapons | |
| **4 - the five weapon loka sets and astras** | all 36 weapons, 7 astras | |
| **5 - the descending lokas** | each remaining loka and ruler as they are built | |
| **6 - polish** | the mix pass, accessibility, performance, the ending | |

---

## 19. How the music and sounds will actually be made - the honest part

I (Claude) cannot record a bansuri or a pakhawaj. What I can do is:
- write every composition as a **real score** (notated raga phrases, tala, form, and the stem layout) in MIDI / MusicXML / LilyPond, with the leitmotifs, the transitions and the cue sheets exact;
- write the engine, the event wiring and the tests;
- write the briefs for every sound effect (source, processing chain, length, pitch, variations).

The sound itself has to come from one of these, and the choice changes the schedule and the quality, so it is **your** decision:

1. **A composer / performers** record from the scores (best result; the scores are the commission).
2. **High-quality sampled instruments** (Indian-instrument sample libraries) played from the scores through a DAW; I can render with free libraries but real libraries sound better.
3. **A hybrid:** sampled for the dense stems, live performers for the solo lines that matter (the bansuri pilot theme, the sarangi, the chant).

Everything in this plan is written so it works with any of the three.

---

## 20. Decisions I need from you

1. **Production route** (section 19): 1, 2 or 3?
2. **Voice:** is the sung-dialogue idea (1.6) right, or do you want recorded speech for the key characters later?
3. **Real Sanskrit:** the bija and Rudram chant are real; do you want a fuller Sanskrit text in the Brahmastra and boss pieces (needs a reviewer who reads Sanskrit)?
4. **Tonal descent:** happy with one semitone per loka?
5. **Phase 1 first:** may I start on the Atala score and the leitmotif sheet?

---

## 21. Implementation status (what exists now)

**Route taken (section 19):** the scores are written in `tools/sound/music.py` (raga, tala, form, instrumentation, as notation in `tools/sound/raga.py`) and rendered by the instruments of `tools/sound/synth.py`, a deterministic additive / physical-model synth of the Indian palette with a temple reverb. It is *synthesised from the written scores*, not performed: it will not sound like a human bansuri player. The scores are the commission; replacing the render with recorded performers, or with a sample library, changes only the audio files (same names), not the code.

| Part | State |
|---|---|
| Notation, tonics (Sa descends a semitone per loka), ragas, talas | done: `raga.py`, `music.py` |
| **Music** | title, menu, the opening, **Atala** (Yaman / Teental: drone, drift, combat, crisis, threat and voice stems, one composition at three doubling layas), **Vitala** (Bhairav / Ektal), **Bala** (Marwa / Chautal, Jhaptal, Rupak), **Bhairava** (Bhairav / Dhamar), the lamp and bay (veena, both lokas), the seven pieces of the Vajra and the whole, death, respawn, the descent into Vitala, the two yields |
| **Effects** (291) | the 36 weapons (two takes each), seven impact materials, the hull, the nine foe kinds (fire, die, wake), the seven astras (invoke, the chanted bija loop, loose, live), pickups, the lamp, the bay, the interface, the ruler's set pieces (Maya forge, the shattering mirror, the doors, the ash), two ambience beds, and 84 notes of sung speech (12 speakers x 7) |
| **Engine** | `vajra.audio.AudioEngine` (buses under the pilot's volumes, the music stems kept in step, crossfaded by a pace that rises at once and falls after a hold), `Audio` (the shared instance), `Snd` / `SoundSink` (what the world reports); inert with no device |
| **Wired** | world events (shots, hits, booms, foes, the hull, shield, pickups, the astra rite, cues, lamps, bays, medkit, chests, the piece and the seal), the flight's loops (engine, shield, mud, heartbeat, ash, astra live, ambience), the bay (buy, deny, fit, temper, sell), the menus' keys (move, choose, back), the tour, the wheel, the pause (lowers the music), sung dialogue for scenes and bubbles |
| Tests | `AudioTest` (every sound the engine asks for exists on disk; the sung table; the world's events; the engine inert without a device); every file was also decoded by libGDX on a real device in a probe run |

**Not yet:** music and ruler pieces for the lokas that do not exist as places yet (Sutala to Patala), the ending, the Brahmastra's long orchestral release and Pashupatastra's held silence beyond the astra effects above, the co-op sounds (K), the accessibility "reduce sharp sounds" switch, an ambience volume slider, and a human or sampled render of the scores. Run `python tools/sound/music.py` and `python tools/sound/sfx.py` to rebuild every file (needs numpy, scipy and ffmpeg).

### 21.1 Second pass (after the first listen)

What changed, and why:
- **Menu sounds** are now short and each its own: a 40 ms clay tick to move along a list, a quick two-note pluck to go on (ENTER or a click), one lower soft pluck to go back, a thin tick to change a value (A/D), a brush and tick for a tab (Q/E/TAB), a click-click for a toggle. The shop's buy, refusal, fitting, tempering and sale are separate short gestures.
- **Shots and foes** were rebuilt on a punchier voice (`synth2.py`): an instant attack, a pitch that falls fast, a crack on the front, a thump under it. The pilot's bolts fall; the foes' answers are darker and rougher (and some rise), each kind its own; foe deaths are real bursts (thump, roar, ringing debris) with each kind's own flavour on top.
- **Each element of the world has its own tone**: the lamp (flame and a rising bell arpeggio in Yaman, and a hum of Sa and Pa with a flicker that grows as the hull comes near), the bay (a latch, a gong and bowl at the fourth, and a steady machine hum), the chest (wood and coins and two santoor notes), the gate (a vast gong and a conch), the seal (stone and a chord).
- **The hull is an engine**: a looping rumble whose pitch and weight follow the speed, a turbine whine that climbs with it, an exhaust rush, and a boost roar.
- **The astras are chanted in their own words** (`sfx2.py`, `MANTRA`): *Om Ham Akashaya Namah* (ether, long open vowels), *Om Yam Vayave Namah* (wind, quick and high), *Om Ram Agnaye Namah* (fire, staccato and rising), *Om Vam Varunaya Namah* (water, flowing), *Om Lam Prithvyai Namah* (earth, slow and low), *Om Brahmastraya Phat* (a choir of five rising to the shout), *Om Pashupataye Namah* (slow, ominous). The line is sung in a loop while the astra forms (its pitch rising with the charge) over its element's own bed, and the last syllable is shouted as it looses.
- **Music**: each loka now has its own palette (Atala: bansuri, sarod, tabla; Vitala: sarangi, veena, dholak and clay pot, a darker room), each arrangement is cleaner (one lead and the time; the shadow and the cymbals only in the crisis), the title, the settings and every menu share one music, and the **pause has its own** (Raga Desh on santoor and jaltarang).
- Tools: `render_all.py` renders everything; `look.py` draws a waveform and spectrogram sheet and prints attack, length and brightness for any sound, since there is no ear in the build.
