# Story beats: how the rulers fall, the Vajra, the veil, repairs, and the pilot's death

## The script
Every spoken line (Scenes, StoryScript, the rulers' barks, the people's lines, the Codex pages) is in the register of the epics: plain, grave, a little formal, no jest. A ruler speaks of the piece as of a voice he
obeyed too long ("the piece says Atala is mine"). Lines stay short (a bubble holds one breath, the band two) and set in capitals; `ScriptTest` holds the length and the voice.

## A ruler does not die: he yields (Enemy.yieldUp, World.victory, World.bossReleases)
The last hit leaves him on his knee where he stands, beyond harm and silent (`Enemy.yielding`). The kill is counted, the retinue stands down, the story is told as a cutscene (cue BALA_FALLS / BHAIRAVA_FALLS): the
narrator over the fallen ruler, then the ruler and the pilot speak turn about and **the camera crosses between them with every line** (`Scenes.yieldScene`), the last exchange shot nearer. When the scene is over, however
it ended, `Scenes.finish` calls `World.bossReleases()`: he lets go (rings and gold light), the purse is paid, the piece comes to the hull **by itself** (`Pickup.seek`, faster the longer it has come, collected on touch), the
gold gate opens, the next stage begins.

## The piece, shown (VajraPanel)
On collection (`World.pieceShowT`, 5.4 s) the screen dims a little and the piece is shown big at the middle on the whole blade (the bands held lit, the new one burning), with its number and the loka that gave it. Then it
shrinks and flies to the top-right corner, where a panel reads `n/7 VAJRA` with the blade in miniature (a band lit per piece). The panel stays for the run.

## The veil (CloudVeil, World.veilClear)
The way beyond each stage's seal lies in cloud (a bank of soft puffs, two layers, no hard edge) until its ruler has yielded and let go. Then `veilClear` runs 0 to 1 over seven seconds: the puffs swell, drift to the sides and up
and thin out; the camera is taken to the gate for a few seconds to see it (`World.camFocus`, `unveilT`).

## Repairs (World.medkits, key 1)
A repair drop is bigger (56 px with a glow, taken from further off, heals 30%). A hull with room takes it at once; a whole hull keeps it (up to five) and **1** spends one (40% of the hull). The pouch is shown at the end of the health
tube (a cross per repair, the key beside it, pulsing when the hull is low) and saved with the run. Hulls 2-7 moved to keys 2-7 (the first hull is Z's).

## The pilot's death (DeathSeq)
The hull breaks into light where it was struck (a burst, then a drift), the controls are dead and the hull is not drawn; the camera holds the wreck for a breath, then leaves it and travels to the lamp (1.8 s); there the
light gathers from a ring, closing on one point, and the hull is whole again (a ring and a burst), with two seconds of grace. 4.4 s in all. The life is spent and the combo broken at the moment of the strike.

## The camera (World.camFocus)
Besides the hull, the camera is drawn to: Bala while he forges his Maya-astra (it goes to him in half a second, stays while the mandala forms, and comes back over a second), the wreck-to-lamp path of a death, and the gate while the
veil parts. While Brahma is forged the view creeps out slowly (World.astraSlowView).
