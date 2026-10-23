# unused/

Files moved out of the live tree because nothing in the game, the tests or the tools loads
them. They keep their original relative paths, so putting one back is a plain `git mv`.

| what | why it is here |
|---|---|
| `assets/fonts/*` (ten faces, the Courbe Sans package) | the UI uses only Rajdhani SemiBold and `courbe-sans.ttf` |
| `assets/frames_borders`, `assets/hud_mask`, `assets/ship_mask` | older HUD / ship-mask art, no loader |
| `assets/atala/walls/Wall_Tiles_handdrawn`, `Wall_new.png` | backup of the hand-drawn wall tiles and a spare sheet |
| `assets/Vajra Overdrive_ Molten Pixel Emblem.png` | emblem, replaced by `brand/vajra-overdrive-logo.png` |
| `core/src/.../ShopUi.java`, `ShopController.java`, `ShopUiTest.java` | the old tabbed hangar, replaced by the card bay (`BayUi`, `BayController`, `BayTest`) |
| `code_notes/`, `docs/design-notes/` | loose notes and PDFs |

Nothing was deleted. Checked by searching every source, test and tool script for the file name
or its directory.
