# Treasure chests - every loka hides its own

> Status: **built for Atala and Vitala** (`world/Chest`, `World.openChest`, `screen/ChestImages`); the other five lokas are planned below.

## The idea

A chest is the game's reward for *leaving the road*. The road through a loka is a fight; the chests are in the places off it - the flanks of a band, the
far edge of a wood, the side caverns of a river. You open one by **flying into it**. It pays **coins** (which fly out as pickups) and **one useful
thing**, rolled from the loka's own table - something a pilot in that loka can really use, not a number that goes up.

Rules that hold in every loka:

1. **Off the road.** Never on the centre line; never inside a gate or a shop compound. Reachable (a flood-fill test, `VitalaTest`, reaches every chest of
   both lokas from the Atala spawn).
2. **Its own look.** Each loka has a chest skin in that loka's palette, painted in code like the shrines (`ChestImages`): idle (a glint crosses the lid, the
   jewel breathes), opening (the lid lifts, the light comes out), open (an empty chest that stays where it was).
3. **One useful thing**, never the same table twice: the loka's element, the loka's problem.
4. **Pays once, forever.** `Chest.open` is saved in `RunSave` (`chestOpen`), so a continued run does not refill them.
5. **Deterministic.** The roll is seeded from the chest's position: a restored run, a shared race and a test all roll the same treasure.
6. **Guarded ones are earned.** A *den's* chest does not open while its ambush lives (`Chest.guards`); the last guard falling releases it.

## What a chest gives (built)

| | coins (x sqrt of the loka's hull scale) | the one useful thing, weights out of 100 |
|---|---|---|
| **Atala** - the pearl chest, bound in gold, a lavender clasp-jewel | 40-70 | 45 **Mend** (hull and shield full) / 40 **Shards** (3 Akasha, a step toward an astra) / 15 **A life** |
| **Vitala** - the basalt chest, bound in molten gold, an ember clasp | 90-150 | 35 **Mend** / 35 **Shards** (5 Agni: a whole release of the Pyre) / 18 **Surge** (boost and shield brimming) / 12 **A life** |

## Where they are (built)

- **Atala:** ten pearl chests (`WorldGen.placeAtalaChests`), two per band on the flanks (x near 700-900 and 4300-4500), the mirage wood's far edge, and
  the arena's rim.
- **Vitala:** *every side cavern holds something* (`MapGen.pockets`, `VitalaGen.placePockets`), which is what gives the pockets in the walls their meaning:
  - a **VAULT** holds a chest, quiet;
  - a **DEN** holds a chest *and its ambush* - a leader (Tamra, Agneya or Rekha) and two followers waiting at the chest; the chest stays shut until they fall;
  - a **WELL** holds an **ember well** (`World.wellUsed`): fly into it hurt and the hull, the shield and the boost are made whole - once.
  The pockets repeat vault, well, den, vault, den, well along the river, about thirty of them across ~27,000 px.

## Planned for the other five lokas (not built)

| loka | chest | the one useful thing (the loka's own problem) | where |
|---|---|---|---|
| Sutala (water, Jala) | a **conch** chest, pearl-blue | a **tide**: a few seconds in which the current carries your shots (the water loka's flow field is *your* friend) | on islands of the lake, behind a current you must cross |
| Talatala (crystal mirage, Prithvi) | a **geode** that is only real from one side | a **true mirror**: one hit reflected as a shot of your own | one real chest among mirage ones; the others open on nothing |
| Mahatala (serpents, Vayu) | a **coil** chest that must be uncoiled by touching its three loops | **hoop-slip**: a short pass through the serpents' rings without damage | inside a coil of the cave, guarded by the ring-firing kind |
| Rasatala (the abyss) | a **blood-iron** chest | **sight**: the blind kind cannot be hidden from for a minute | in the abyss's dark pockets, where the light does not reach |
| Patala (jewel and serpent light) | a chest that is a **cut jewel** | **all five shards** at once (it pays every element: the loka does) | at the heart of the throne's approach |

**Markers (built):** every unopened chest inside the compass dial's window is a gold coffer on it; once the pilot has met the person who knows the loka's hoards -
the **Scout** for Atala's, the **Gold-Seller** for Vitala's - the *nearest* unopened chest of the loka the hull is in is marked even beyond the window, resting on
the dial's edge like a gate (`HudCompass.chests`).

Not yet in: co-op ownership (today a chest is the host's world and opens for whoever touches it first), and a Codex page per chest skin.

## Tests

`VitalaBrainTest.treasureChestsOpenOnTouchPayCoinsAndOneUsefulThing`, `aDensChestStaysShutWhileItsGuardsLiveAndAWellMendsOnce`; `VitalaTest.vitalaIsTen
ThousandYojanasLongAndItsPocketsAreAllUsed` (every vault and den holds a chest, every den's is guarded) and `everyLandmarkOfBothLokasIsReachable...`.
