# Vitala enemy bible - six hulls and a capital ship

**The names and roles are the original Vajra Overdrive bestiary's** (`C:\Dev\VajraOverdrive\docs\reboot\BESTIARY.md`, `src_reboot/docs/LOKA-LAWS.md` in that folder):
three regulars (*agneya, jvala, tamra*), three smalls (*kila, dhuma, rekha*) and the boss *bhairava*. The ship art below was drawn for them; the
sheet files keep the kind names (`auris.png` ...), `Enemy.nameIn(kind, loka)` gives the Vitala name.

| name | the original's role (LOKA-LAWS) | sheet / Kind slot | the ship drawn for it |
|---|---|---|---|
| **AGNEYA** - the Ember | straight burning dart that leaves a fading trail (regular) | `vakra.png` / VAKRA | the **boomerang**: a swept, dart-like burner |
| **JVALA** - the Cinder | never fires; drops a burning patch where it hovers, patches pile up into ground you cannot stand on (regular) | `mayavi.png` / MAYAVI | the **mortar gunship**: it hangs back over its target |
| **TAMRA** - the Ore | copper-plated, bolts clink off its flanks, slow, heavy, body-blocks for the shooters behind it (regular) | `bhavi.png` / BHAVI | the **ram**: blunt, armoured prow |
| **KILA** - the Nail | fires the thin fast `SPIN` needle, hard to see in the ember field (small) | `lance.png` / LANCE | the **needle**: long, thin, a gold spike for a nose |
| **DHUMA** - the Smoke | goes translucent for 1.5 s at a time, cannot be targeted while smoked, reappears aiming at you (small) | `auris.png` / AURIS | the **skull** wisp: a small ghost |
| **REKHA** - the Line | draws a single hairline across the arena, waits, then ignites the whole line at once (small) | `hataka.png` / HATAKA | the **wraith**, arms wide: the line runs between its hands |
| **BHAIRAVA** - Herald of Cinders | the boss: an **ash trail**, burning ground you cannot cross, the safe arena shrinks over the fight | `bala.png` (128 px) / BALA | the capital ship |

*Status:* **all seven behave as the original bestiary says** (`enemy/VitalaBrain`, `enemy/BhairavaBrain`) - see 'The brain' below. Rank is kept.

Vitala is **Hara-Bhava's** loka: black basalt, a river of liquid gold (the Hataki), fire that smelts it, and the *bhutas* who attend the god. Its
ruler **Bhairava** - Hara in his burning aspect, *Herald of Cinders*, Lord of Burning Gold - holds the piece of the bolt that tells him the fire is his. Atala's people were round pearl
creatures; Vitala's are **ships** - nose down, engines streaming up, like the pilot's own hulls - but never *only* ships: two are ghosts, and
the rest carry a mythic shape.

## The colour code (a second palette for the same kit)

| role | colour | where it shows |
|---|---|---|
| basalt | charcoal `#42383E`, shade `#2C232C`, deep `#1C151F`, warm rim `#AC8E82` | hulls (never pure black: the floor is black too, so the **rim light** carries the silhouette) |
| gold | hataka gold `#FFC828` / `#F0961E`, bronze `#B46A14`, amber `#6E320A` | seams, trim, wing edges, eyes, rank |
| ember | `#FF6E1E` (dim `#BE3C16`) | glow at rest (Atala kept ember for rage only); **rage** goes white-hot |
| ghost | `#E8F6FF` / `#CDE4FF` / `#96CDFF` / `#4074CE` | the two bhuta alone: the only cold colour in the loka |
| outline | dark ember-brown `#28100A` | not black, so a hull still separates from the floor |

Rank is still read in gold alone: a **leader** wears a *three-tongue flame crown* (the three sacrificial fires), an **elite** adds flank vents and
the thin broken halo. **Rage** turns the stone red-hot (basalt to cherry, gold to white). **Death is cinders, not glass:** the hull flashes
white, its seams blow open, the stone goes ember-red and rises as sparks on a fixed dither; four-point glints are what is left.

## The contract with the game

Same six *roles* as Atala, so every brain, gun, quota and test keeps working: the roles are the `Enemy.Kind` slots and Vitala gives each a new
skin (`assets/enemies/vitala/<kind>.png`, the same 16x4 sheet of 64 px cells: idle x8 + rage x8, hit x2 + death x6, leader, elite). Draw sizes are
the kind's (`Enemy.baseDraw`). The **wraith** (HATAKA's slot) also *phases through walls* in Vitala (`Enemy.phasing`): inside rock it has no
body - it cannot be hit or rammed and it does not fire. Reserves and flank-dives from the depth system stay.

## The six ships (the pilot kept the first two; the other four are simple, ship-shaped, and BANK)

All four ships roll over their eight-frame idle (`roll_of`): one wing tip rises as the other falls, the tip lights blink in turn, the engines flicker
out of step - and a hit throws the silhouette white, then scalds it.

### DHUMA, the skull (scout; Kind AURIS) - *kept*
A ghost-white **skull** on a ragged tail of mist, blue fire in the sockets, a wide many-toothed grin, a ring of hataka gold on the brow. Smoke
rises past it; the jaw drops a pixel at the low point of the hover. Quick, small, cold.

### REKHA, the wraith (turncoat; Kind HATAKA) - *kept*
A hooded **wraith** with its arms held wide in welcome, long sleeves, a hem of torn mist, a dark face with a wide white grin and blue-fire eyes,
gold bangles on its wrists. It embraces - and bites at half hull - and it **walks through the walls** to do it.

### KILA, the needle (sentry; Kind LANCE)
A long thin fuselage with a gold **nose-spike**, short swept stub wings far aft with a gun pod on each tip, little canards at the nose, a tall
ember canopy and one big engine. Simplest hull in the loka: it holds a line and fires straight down it. A light runs down the hull; the canopy
eye tracks.

### AGNEYA, the boomerang (skirmisher; Kind VAKRA)
Two broad **swept-back** wings meeting in a chevron at a small ember canopy, gold serpent-scale ticks along each, twin engines on the wing roots,
two gold fangs at the nose. Wide and fast; it rolls as it weaves.

### TAMRA, the ram (rusher; Kind BHAVI)
A blunt wide body, short thick wings, an armoured **prow-plate** across the nose with a molten strip (a hot spot runs along it) and gold tusks, a
visor-slit canopy and two big thruster pods astern with heavy flames. All of its weight is at the front, and it flies at you.

### JVALA, the mortar gunship (artillery; Kind MAYAVI)
A stout hull with a **great cannon** in the nose, its muzzle a molten ring, and wings that sweep **forward** to a pod of molten gold at each tip
(the opposite stance to the boomerang), a gold canopy, twin engines. It hangs back and lobs.

## The brain (Vitala flies its own: `enemy/VitalaBrain`)

Atala's units park at a post or ring a leader and tap single rounds. Vitala's **fly formations and attack in patterns**, and each kind carries the
character the original bestiary gave it. **Rank is kept**: a unit with a commander obeys it - it takes the slot its leader's formation gives it, fires on
its leader's clock, routs when the leader falls (2.5 s of flight, then it regroups under the nearest senior, or goes home), and a leader or an elite hits
harder (`Enemy.RANK_DAMAGE`, the flame crown and the halo on the sprite).

**Formations** (each kind flies two, a leader switches every two volleys, 6.8 s; a volley is every 3.4 s):

| formation | where the followers go | when they fire (the stagger) |
|---|---|---|
| LINE | abreast, across the leader | a ripple down the line, 0.14 s a slot |
| WEDGE | a V behind the leader | all together |
| COLUMN | a file along the leader's TRAIL (the river road), five samples apart | a relay, 0.22 s a slot |
| PINCER | two flanks that close on the HULL from 560 px to 290 px | alternate flanks, half a volley apart |
| CIRCLE | a ring round the HULL (Atala's rings turn round the leader), 340 px, turning | all together: a radial |

| kind | formations | the character | its attack on the volley |
|---|---|---|---|
| **Agneya** (VAKRA) | wedge, pincer | the Ember: a burning dart | a **dart run**: a 0.5 s tell (a flash along the line), then a straight 780 px/s run through the hull's place leaving **burning ground** (a patch every 70 ms); ember bolts between runs |
| **Jvala** (MAYAVI) | column, circle | the Cinder: never fires | drops **burning patches where the hull is going** (its velocity x 0.9 s); in a column or circle it drops three; they pile up (cap 60) |
| **Tamra** (BHAVI) | line, circle | the Ore: copper-plated, slow, heavy | **armour**: a round from the front rings off at 40%, from the flank 70%, from behind whole (`Enemy.armourFactor`); it **body-blocks** - it steps in front of the nearest shooter on the hull's side - rams when close; a slag shell, and a **ring of eight** if the hull is touching it |
| **Kila** (LANCE) | line, wedge | the Nail: a sniper at 540 px | a laser tell, then a burst of **three thin fast needles** |
| **Dhuma** (AURIS) | column, pincer | the Smoke | **goes to smoke for 1.5 s** (`Enemy.smoked`: no body, cannot be targeted, drawn at 28%), drifts to a point 230-290 px from the hull, **reappears aiming** with two bolts |
| **Rekha** (HATAKA) | line, column | the Line | **draws a hairline** (it grows in 0.7 s, waits armed 1.0 s as a thin thread, ignites for 0.45 s); a Rekha leader sets the angle and the followers draw **parallel lines**: a grid. Walks through rock (`Enemy.phasing`) |

The ground they lay is `world/Hazards` (patches and lines; scalds the hull, hushed by a shrine) and is drawn by `screen/HazardFx`.

## The seventh: BHAIRAVA, Herald of Cinders (boss; the BALA slot, a 128 px sheet) - REDESIGNED

*Fiction:* Hara (Shiva) at the cremation ground, the Lord of Burning Gold - not a villain: the fire he tends changed everything it touched and he has
forgotten it was ever anything else.

*Shape (the first design, a gold-masked winged ship, was thrown out):* he is built as a capital ship out of **bone and ash** - the one pale thing in a world
of black and gold. The **skull** is the prow: horned, a gold **crescent moon** on the brow, two blazing sockets, a **third eye** opening in the forehead, a
jaw whose two fangs are cannons. Behind it a **ribcage** is the hull with a furnace burning in it, a gold spine with a vertebra every nine pixels, a
**garland of small skulls** across the chest. Two long bone **arms** reach out and down: the left holds a **trishula**, the right a **flaming skull on a pole**.
Streaming up behind the whole ship, his matted hair - **five locks of fire**, which are the engines. Ash falls over him; embers climb past it.

*Phases (one idle row each, 8 frames):* **Stoking** (third eye shut, hair low, furnace dull), **Kindling** (eye half open, hair high, furnace red),
**The Flood** (eye wide and white-hot, hair blazing gold, sockets white). Then hit x2 and death x6 (cinders). Sheet: 1024x512 = 8x4 cells of 128.

*The fight (`enemy/BhairavaBrain`):* his signature from the original is the **ash**: burning ground you cannot cross, and **a safe arena that shrinks**.
A ring of ash stands on the edge of the safe floor and closes in as his hull falls (radius 800 down to 340 px: `Hazards.ring`, drawn as a wall of flame).
Phase by hull: **Stoking** (above two thirds) - the **ash charge** (a tell, then a 700 px/s run through the hull's place laying a trail of ash that stands
seven seconds) and the **ember fan** (five bolts, twice); **Kindling** (to one third) - adds the **cinder rain** (ash dropped in a closing ring of six round
the hull) and **calls his court** (a pair of Vitala elites); the Altar's six rim vents open; **The Flood** (below) - adds the **third eye**, a long tell and
then a sweeping beam, the ring breathes and closes faster, and the Hataki floods the hall and carries every hull east. His retinue obeys him and routs
when he falls; his fall lifts the gold seal and ends the run.

## The keepers (their own sheets now)

The three people with a post are drawn in the same ship language (`tools/enemy_art/vitala_keepers.py`): **DVARAPALA** the **gate-bull** (a heavy shield of a hull,
pauldrons, two horns sweeping out and down to gold points, an ember visor that scans), **DIPAKA** the **fire-priest** (a lamp-post hull, curved lamp-arms with
burning bowls, a brazier at the nose with the three fires), **VANIJA** the **assayer** (a barge with a balance on its back, docking claws, a lens eye and a
ring of coins that hop in a wave). They are in `assets/enemies/vitala/` next to the six, so `EnemyImages` finds them with no recolour.

## Production

`tools/enemy_art/vitala.py` holds the designs (the kit, a second palette, a flame crown, a cinder death), `vitala_boss.py` the boss;
`python tools/gen_enemy_sheets.py vitala` writes `assets/enemies/vitala/*.png`; `tools/enemy_art/vpreview.py` / `bpreview.py` make the contact
sheets. `EnemyImages(loka)` loads the loka's folder, so Atala is untouched.

## Tougher, and no flight (after playtest)
- **Hull:** every Vitala unit (not the ruler) carries `Balance.VITALA_HULL` (x2.1) on top of its loka's 1.35 and its band's grade; a leader (rank 1) a further x1.3. A Vakra goes from about 90 to about 190 hull, a leader to about 250. Coins are unchanged (they follow the loka's table).
- **No rout:** when a leader falls its followers close ranks under the nearest senior, or, with none left, are **enraged**: x1.35 speed, x1.3 damage, and they no longer hold to their post's engage range. (A ruler yielding still dismisses his retinue: that is the story's beat, not a flight.)
