# The test arena (Settings > TEST)

Settings has a TEST tab; **OPEN THE TEST ARENA** lists 159 scenarios in 13 categories. Pick one and fly it with the full armoury (all hulls and variants, all 36 patterns at rank III, shards of every element, coins, five lives). Nothing flown in the arena is saved, and it never touches the pilot's book. ESC returns to the list.

| Category | What it covers |
|---|---|
| SANDBOX | free flight, as a new pilot, empty sky, target range (foes that cannot die), Vitala versions |
| STORY AND SCENES | every cutscene cue (opening, gate, the voices, the gaze, first lamp, first bay, Vitala, the rulers waking), the player's death with and without lives |
| FOES | each of the nine kinds, all at once, elites, ranks, a horde, armour, the lamp-keeper's mending, the weakling's betrayal, sleeping foes, Vitala's reserves and dodgers |
| WEAPONS | one scenario per pattern (36) at rank III against targets, rank I-III, the wheel, one pattern owned, heated hull |
| ASTRAS | each of the seven, the base hull that cannot forge, no foes, partial shards, Brahmastra only, selling shards |
| RULERS AND BOSSES | Bala and Bhairava: full fight, phase two, phase three, about to yield; the three Maya-astra modes; the ash cloud |
| WORLD AND LOKAS | every Atala band and Vitala region, the gate, the sealed arena, bays, a lamp |
| LOOT AND SHOP | coins, shards, health and repairs, a piece of the Vajra, shops (poor, rich, everything owned), kill loot |
| HUD AND UI | low and critical hull, one life, combo chain, a busy dial, seven pieces, the shop screen |
| HULLS | each of the seven hulls, the base hull alone, a shield and boost drill |
| HAZARDS AND STATUS | ash cloud, the Jala flood, foes burning, slowed, frozen, heavy fire |
| TUTORIALS | the tutorial of a new game, the first lessons |
| STRESS | sixty foes, a bullet storm, astras on a crowd |

## Keys in flight
F4 hide/show the bar, F5 god mode, F6 next pattern (Shift: previous), F7 pattern rank, F8 spawn three of the next foe kind, F9 clear foes, F10 refill (hull, shield, lives, repairs, shards, coins), F12 reset the scenario, ESC back to the list. The usual keys (hull numbers, V/B, F astra) work as in a real run.

## Where it lives
`world/Arena.java` (the catalogue and the setups), `screen/ArenaScreen.java` (the menu), `GameScreen.arena(...)` (a never-saved flight), `Enemy.dummy` (the targets). `ArenaTest` builds and flies every scenario for two seconds.

## The tutorial of a new game
After the opening of every new game, the HUD tour (every instrument) and then the controls (four small cards on the hull itself, with the keys as the pilot has set them) play. There is no page of keys. It plays every time while "TUTORIAL HINTS" is on.
