# The tutorial of a new game

A new game starts with an EMPTY screen. Each instrument comes on the first time the flight gives it a reason, with a holographic callout (corner brackets close on it, a line runs to a small panel that unfolds, types a line or two and fades after 8 s). The flight HOLDS STILL while a callout is up; ENTER, SPACE, ESC or a click reads it on. Code: `screen/HudReveal.java`, gated in `VedicHud` and `Hud`; wired in `GameScreen.newReveal()`.

| Instrument | Appears when |
|---|---|
| Weapon pattern | the pilot has flown and found the trigger (the coach's two key-cap prompts, THRUST and FIRE, stay) |
| Hull tube + lives | the first hit |
| Repair cells | the first repair is picked up |
| Radar | a foe is within 1000 px (or before the shield / boost) |
| Shield | the first hostile bolt within 340 px |
| Boost | 900 px of travel |
| Score and chain | the first kill |
| Astra | the first shard |
| Loka and depth | 1800 px of travel, or the first region |
| Vajra | the first piece of the bolt is held |

Continued runs, shared runs and pilots with hints off see everything from the start. The test arena's TUTORIAL scenario plays every callout in turn.
