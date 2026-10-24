# Atala enemy bible

Atala is the household of **Bala**, son of Maya: a palace of pearl and gold under the earth,
lit by jewels, ruled by a man whose piece of the bolt tells him it is all his. Nobody here is
cruel. They are retainers, and every one of them has a job.

## The colour code (taken from the shipped Atala art, not invented)

| role | colour | where it shows |
|---|---|---|
| lacquer | pearl / ivory `#E8E6E0` `#FFFCF1`, cool mist `#CFCFCF` | hulls |
| warm shade | cream `#FFF4D8`, `#E6DFD0` | the lit side, belly |
| metal | gold `#FFD700`, `#FEC23D`, bronze `#B8860B`, amber `#854D00` | trim, filigree, eyes, rank |
| aperture | near-black `#1A1A1E` | eyes, vents, outline |
| Akasha | pale lavender `#D9D0E8` | illusion work: Mayavi, lamps |
| rage only | ember `#FF5622` | rage cracks and flames, never at rest |

Rank is read in gold alone: a **leader** wears a lotus crest, an **elite** adds flank vents and a
thin halo. Rage turns the pearl cream-hot, the gold white-hot, and opens ember cracks.

## Animation rules, all nine

- **idle (4):** a slow hover, 1 px up and down, one part moving with a purpose (fins, ring, flame,
  scale pans), the eye brightening and dimming once per loop.
- **rage (4):** the same shape in the rage skin, ember cracks, a flame flicker above, and a
  1 px shudder left-right. Plays when badly hurt, hunting, or betrayed.
- **hit (2):** frame 0 is the silhouette struck white and thrown back 2 px; frame 1 settles with a
  scorch. Reads at a glance, over in 0.12 s.
- **death (6):** Atala dies as gold glass. The hull flashes and cracks along its own seams, splits
  into four pieces whose edges catch gold, the pieces drift apart and dissolve, and four-point
  glints are all that is left. Nothing is random: the cuts, the glints and the dissolve are fixed.

## Size by hierarchy

A body is drawn smaller than its 64 px cell, by kind and then by rank (`Enemy.baseDraw`,
`Enemy.rankScale`): scout 34, saucer 42, spear 38, crescent / ram / mirror 46, lamp 42, bay-master 52,
door-ward 58 px. A **leader** is 12% larger than its court and an **elite** 25% larger. Hitboxes are the
body's core and grow with the same factor. Sheets are filtered Linear so the downscale averages.

## Space

Enemies are aware of their surroundings: placement runs a symmetric relaxation pass after the world is
laid out (nobody starts on anybody, and the mirrored halves stay mirrors), separation is sized to the
two bodies, a unit pressed against rock for over a second steps into open air, and one found inside
rock is lifted out (`SpaceTest`).

## The nine

### AURIS - the listening orb (scout)
*Job:* first contact. Drifts ahead of a court and listens; when it hears the pilot it kites and
pings. *Feel:* nervous, quick, never closes. *Shape:* a pearl sphere with one black slit-eye and a
gold iris, two filigree ear-fins swept back, six small gold gems at the equator. *Motion:* the
fins beat out of phase with the hover; the gems chase around the ring.

### HATAKA - the gold-draught saucer (turncoat)
*Job:* a pleasant face that lures, then bites at half hull. *Feel:* smiling, wrong. *Shape:* a flat
pearl saucer, a gold band round its rim, a wide gold smile of three fangs, a domed black eye.
*Motion:* the band's lights rotate; the smile widens by a pixel at the low point of the hover.

### LANCE - the spear sentry
*Job:* holds a post and taps one fast, aimed round. *Feel:* patient, formal. *Shape:* a long
pearl spear-head, gold spine, a black slot with a gold bead, two short blade-fins. *Motion:* the
bead tracks down the slot; the fins hold still - a sentry does not fidget.

### VAKRA - the crescent weaver (skirmisher)
*Job:* weaves and fans fire, wounded it weaves wider. *Feel:* graceful, restless. *Shape:* a pearl
crescent, horns swept up, a gold eye in the cup, a gold lip below. *Motion:* the horns sway, the
eye rolls to look where it is going.

### BHAVI - the ram (rusher)
*Job:* closes and rams. *Feel:* heavy, stubborn. *Shape:* a stout pearl shell with a gold brow
mask, a black visor, two bone tusks. *Motion:* the tusks dip on the hover's low point; the visor
glows through the brow.

### MAYAVI - the mirror-bearer (artillery)
*Job:* the slow marked lob, the illusion. *Feel:* theatrical, serene. *Shape:* an egg of pearl with
a split face - two eyes - and a ring of lavender mirror shards. *Motion:* the shards turn around
it; the eyes blink in turn.

### DVARAPALA - the door-ward (gatekeeper)
*Job:* stands a gate. Heavy slow shell. Tougher than anything near it. *Feel:* immovable, dutiful.
*Shape:* an arched pearl door-shield, a gold panel with a black keyhole eye, gold pauldrons, a
mace head at the shoulder. *Motion:* the keyhole eye brightens; the pauldrons settle on the
hover; nothing else moves - a door-ward does not.

### DIPAKA - the lamp-keeper (checkpoint keeper)
*Job:* tends the checkpoint and mends its comrades every five seconds. *Feel:* gentle, anxious.
*Shape:* a teardrop lamp of pearl, a lavender flame in its belly, a gold collar and a three-tongue
gold flame crown. *Motion:* the flames lick up and down out of step; the belly glows on the mend.

### VANIJA - the bay-master (shop keeper)
*Job:* runs a hangar bay and never leaves it. *Feel:* shrewd, welcoming, armed. *Shape:* a broad
pearl hull, a gold scale-beam above with two pans, a ring of coins below, a lavender-gold eye.
*Motion:* the pans tip alternately; the eye takes in the pilot.

## HOLD YOUR POST (a rule for every loka, after the pilot found the garrison "coming crazy")

An enemy belongs to the band it was placed in and to the place it was put. What the analysis found (a hull parked in each band for 30 s, `PostsTest`) before the
rules: **42,000 px of drift** - whole courts from the top of Vitala flying down the world to the hull - and units in Bhu Loka. The causes, and the rules that replaced
them:

| cause | rule now |
|---|---|
| a **dive** landed beside the hull from anywhere and moved the unit's post there (Atala's scouts and skirmishers), so a court migrated with the pilot | a dive is only taken when the hull is within 900 px of the post, only lands within 380 px of it, and never inside another band; Vitala's units do not dive at all (they have Dhuma's smoke) |
| a **vent's flame** hurt sleeping enemies, and a hurt unit wakes its neighbours (call to arms): the whole Hearth Deep woke at the first frame | vents burn the pilot only; sleepers are not carried by the river either |
| an awake unit had **no reason to stop**: it fought wherever the hull was | **stand down**: when the hull is farther than 1.7 wake ranges from a unit's own spawn it flies home (round the rock), is set down on its post, and **sleeps there, its formation restored** |
| nothing kept a unit **inside its band** | every unit has `bandMin/bandMax` (its region's floor and ceiling, a margin in from the gates): it cannot cross a gate or enter Bhu Loka |
| formations and leashes were loose | a hard **tether** of 760 px from the spawn for everyone; Vitala leaders stalk within 480 px of the post and followers within 600; a court takes the fight to the hull only while the hull is within 900 px of its post (`VitalaBrain.ENGAGE`), otherwise it holds its formation at the post (a pincer or a circle round a hull that is not there falls back to a wedge) |

After: drift never above 760 px, **no unit in Bhu Loka, none outside its band**, and a court that woke is home and asleep 30 s after the hull leaves.

**Placement audit** (`PostsTest.thePlacementOfTheGarrisonIsClean`): per region 0 / 45 / 51 / 53 / 62 / 16 in Atala and 57 / 88 / 86 / 97 / 13 in Vitala (Bhu Loka none);
no spawn in rock, none outside its band, none on top of another, none crowding a gate: all zero.
