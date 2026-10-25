# The rulers' astras: which boss uses what, by the lore of its loka

Status: PLAN. The player gathers the Vajrayudha's seven pieces; each ruler holds one and **fights with an astra of its own loka**. The astras are the same simulation as the pilot's
(`world/AstraFlow`, the mandalas, the sprite sheets), run by the boss and aimed at the pilot, with three differences: the boss FORMS its astra with a visible mandala (the TELL, drawn at
the boss's size, the same 3.4 s), it is hostile (hurts the pilot, not the boss), and the pilot has a COUNTER for each. The mandala also shields the boss while it forms: **the pilot's rounds are
destroyed on it** (the same rule the pilot's own mandala has), so the way to stop an astra is to interrupt it, not to out-shoot it.

Lokas, rulers and elements are `world/LokaTable`; the lore is the Puranic one the game already draws on (Atala: Bala, son of Maya; Vitala: Hataka and Bhairava; Sutala: the serpent-lord,
water; Talatala: Maya's citadels, Tripura; Mahitala: Hiranyaksha, who dragged the earth under the sea; Rasatala: the Danavas, Nivatakavachas and Kalakeyas; Patala: Vasuki's jewelled
serpents, and Trivikrama, who broke the Vajra).

| loka | ruler | element | the astra he uses | the lore | what the pilot sees and must do |
|---|---|---|---|---|---|
| Atala | **Bala**, son of Maya | Akasha (space) | **Maya-astra** (the Mohana): illusions | Bala is the master of Maya's illusions; he made the Atala's phantom women by a yawn | a gold mandala forms; then **four phantoms of Bala** (one real) appear in a ring and fire in turn; only the real one casts a shadow and hurts when shot. COUNTER: shoot the one whose mandala keeps turning |
| Vitala | **Bhairava**, the Hataka-lord | Agni | **the Hataka pyre**, then the **third eye** (a Pashupata echo) | Vitala's gold is made by fire (Bhava and Bhavani) and he is Shiva's fierce form; the game already says "the third eye is opening" | phase 1: a river-wide **wave of gold fire** along the river's own current (dodge in the side caverns); phase 2: the third eye opens and a **telegraphed beam** sweeps (the pilot's Pashupata, hostile): COUNTER: hide behind the vault walls, the beam ignores nothing for the pilot |
| Sutala | **Takshaka**, serpent-king | Jala | **Naga-pasha** (the serpent noose) with a **flood** | Sutala is the water-deep, serpent country | coils of water seek the pilot and **bind** (the ship is slowed, the guns jam half a second) while a **surge** crosses the arena; COUNTER: boost out of the coil's loop, or shoot its knot |
| Talatala | **Kalanemi**, Maya's heir | Prithvi | **Tripura-astra**: three flying citadels | Talatala is Maya's country, where the three cities of the asuras (Tripura) were built; Shiva burned them in one arrow | three **stone citadels** circle the arena and hurl boulders (the pilot's quake, hostile) until the pilot **lines the three up** and one shot passes through all three. COUNTER: align them |
| Mahitala | **Hiranyaksha** | Vayu | **Mahi-karshana**: dragging the earth down | the demon who dragged the earth beneath the sea | a **reverse tornado**: everything (the pilot, the pilot's rounds, the rubble) is pulled toward the boss, then released; COUNTER: boost against the pull and fire into the eye |
| Rasatala | **Puloman** (the Danavas) | Agni + Prithvi | **Tamasa-astra** (darkness) with **Nivatakavacha armour** | Rasatala is the abyss of the Danavas; the Nivatakavachas wear armour nothing pierces | the arena goes **dark** (the pilot's light radius shrinks to a circle round the hull); the boss is **plated** until the pilot hits the glowing seam: COUNTER: carry light (the flares, the pilot's Agni) |
| Patala | **Trivikrama** | all five | **Brahmastra**, then the **Narayanastra** | the giant who took the three worlds in three steps and broke the Vajra; the Narayanastra is the arrow that grows against whoever resists it and spares whoever lays down arms | phase 1: the **lotus** (the pilot's Brahma, hostile): five petals that circle the pilot; phase 2, the Narayanastra: the arrow rains in the more the pilot **fires or moves** - the only counter is to **stop firing and hold still** while it passes: the whole game's lesson, reversed |

## How it is built

1. **Boss astra rites** are the pilot's rites with `owner = enemy`: `AstraFlow` gets a `hostile` flag, a `source` (the boss) in place of the ship, and the damage goes to the pilot's hull instead of the
   roster.
2. **The tell**: the boss's mandala (the same `Mandala` renderer, at the boss's scale) forms over 3.4 s, the camera zooms for it, and the boss is shielded by it. Interrupting is by a heavy
   hit to its weak point (each boss has one) which cancels the formation and staggers it.
3. **Phases**: a boss's brain (`BossBrain`/`BhairavaBrain`) calls an astra at fixed health thresholds (75%, 40%, 15%) and on a cooldown between, never during the pilot's own astra.
4. **Pilot counters** are tested one by one (a test per boss: its astra hurts an unprepared pilot and does not hurt one who did the counter).
5. **Order**: Bhairava first (the third eye and the Hataka pyre reuse the Agni and Pashupata flows), Bala (Maya-astra needs the phantom system), Takshaka (needs Jala), Kalanemi (needs Prithvi),
   Hiranyaksha (needs a hostile Vayu), Puloman (a darkness pass), Trivikrama last (the lotus and the Narayanastra).

Only Bala and Bhairava exist in the game today; the others are built with their lokas.

## Built: Bala's Maya-astra (world/Maya, screen/ShatterFx)
Bala casts it three times over the fight: at 85% (the glass breaks piece by piece), at the duel (all pieces at once) and at the berserk (piece by piece, then DOORS). He stands still and silent while it runs, and the cracks' heart is
wherever he stands. In DOORS each piece opens on another part of Atala (24 spots of open ground, far from him), shown as a window drawn from there; the piece the hull is over at the count (19 s) carries it through (20 s) and the way back
to Bala is the fight. `ShatterFx.LINES` draws the crack lines (off).
