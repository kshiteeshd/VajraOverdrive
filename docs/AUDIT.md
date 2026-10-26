# Audit of the game as it stands (after v1.0.2 and the story-beats work)

What was checked: the whole campaign headless from the opening to Sutala's hall (`StoryFlowTest`), every door of Bala's third astra for reachability, saves at the worst moments, kills and deaths in the middle of other
scenes, a 20,000-frame random flight through every band (0.27 ms of simulation a frame, no NaN, no leaks), the code for what is built and what is only planned, and the shipped defaults.

## Found and fixed in this pass
1. **Killing Bala in the middle of his astra left the astra running**: the glass stayed over his yielding scene and, in the third casting, a door carried the hull 6,500 px away from him after he had yielded. Now his yielding
   ends the astra.
2. **A mantra half-said when the hull died kept charging**, and letting go of F from the wreck loosed Agni's flames from the dead hull. Now the hull's death ends the invocation, and nothing can be loosed from a wreck.
3. **A save in the second the ruler's piece was flying to the hull lost the piece for good** (the stage had moved on, the piece was not in the save): the run could never reach seven. Now a piece in flight is the pilot's
   in the save; autosave also waits while a ruler is yielding.
4. `SCRIPT.md` was stale (it still held the old lines): regenerated from the code.

## Open, in the order that matters
**Before a public release**
- **The astra testing stock is still on**: `Hangar.TEST_STOCK` defaults to 100 shards of every element (the v1.0.2 installers shipped with it). Set `vajra.teststock` default to 0 when testing is over.
- **There is no sound at all**: no effects, no music, no assets, no volume setting. Everything is silent.
- **Six of seven lokas do not exist.** The campaign ends in Sutala's "coming soon" hall: there is no ending, no credits, no confrontation, and the Vajra can never reach 7/7 (the panel will read 2/7 for ever).
- **Nine of the seventeen Codex pages cannot be reached** (Scout, Maya, Kaliya, the beacon, the rogue, the retempering, Maya's letter, the Gold-Seller, Gauri's Maid): the side characters are switched off, so the pages sit locked
  for good, as do their tutorial lessons and the letter branch of Bala's scene. Either switch the side story back on or hide those pages.

**Story and content**
- Bhairava has no lines in the fight (Bala has barks at each phase and a spoken line at each astra) and no astra of his own; the plan for the rulers' astras (`BOSS_ASTRAS_PLAN.md`) has only Bala's Maya-astra built.
- Jala's astra is still the old cube volley (its surge is unbuilt); the astra forge and charge as designed (five cells, the pour key) are unbuilt - the bay has the seven cards and a release costs five shards.
- Dialogue exists only in English and only in capitals; no voice acting (there is no audio).

**Co-op** (the new systems were not run in a shared flight)
- The Maya-astra, the yielding scene, the veil, the piece panel and the death scene are local: guests do not see Bala's glass, the piece comes to the host's hull and is taken there, so a guest's Vajra count does not grow.
- Nothing carries the stage or the pieces across the swarm except the host's run-over bits.

**Accessibility and settings**
- Settings hold only fullscreen, shake and popups. No volume, no reduce-flashing option (the shatter's light bands, the white flash at a door, the death's burst), no key rebinding, no gamepad.
- The classic HUD (F9) has no stored-repairs row (the Vedic HUD, the default, has).
- Windows that are not 16:9: the shattering glass copies the whole window, so the letterbox bars turn with the shards.

**Release and tooling**
- The Mac DMGs have never been opened on a Mac by us; the in-game updater is Windows-only; the Windows setup is self-signed (SmartScreen will still warn until a certificate is bought).
- The earlier report of lag while astras work was never reproduced; the F3 readout is there for the player who can.
- Older planning docs (`GAMEPLAN.md` and others) describe an earlier game and should be marked as history.

## What stands
The story flow holds from the opening through both rulers: the yield scenes, the piece that finds the hull, the veil that parts, the stage change, the saves; every door of the third astra is open ground connected to the start;
the pilot's death, the stored repairs and the camera holds do what they say; the simulation is cheap; all tests pass.
