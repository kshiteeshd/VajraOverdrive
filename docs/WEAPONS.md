# Weapons: thirty-six patterns

Fifteen **standard** patterns (indices 0-14) and **twenty-one loka patterns** (15-35), three to each loka in loka order. A loka's three are offered once the pilot has reached that loka. Every pattern tempers to rank III, which adds one trait (`WeaponDef.applyAbility`, text in the `ability` list).

| Loka | Pattern | The one idea |
|---|---|---|
| Atala | Mirror Shot | a weaker copy leaves by the tail |
| Atala | Wind Scythe | flies out, turns, comes home; cuts both ways |
| Atala | Maya Weave | two rounds sway in opposite sines |
| Vitala | Ember Dart | sets the foe alight (burn over time) |
| Vitala | Ash Spray | short cone of piercing grit |
| Vitala | Cinder Bomb | bursts into eight embers on a hit or at the end |
| Sutala | Tide Bolt | slows the foe |
| Sutala | Ripple Ring | a ring of twelve round the hull |
| Sutala | Boon Shot | each hit mends the hull |
| Talatala | Ricochet | bounces off rock three times |
| Talatala | Arc Chain | jumps to three nearby foes |
| Talatala | Sigil Mine | settles where it stops, goes off when a foe comes near |
| Mahitala | Serpent Coil | sways and hunts |
| Mahitala | Venom Fang | fast piercing poison |
| Mahitala | Naga Cloud | leaves ground that burns whoever stands in it |
| Rasatala | Gravity Well | draws foes in, then bursts |
| Rasatala | Quake Shell | shoves and freezes the foe |
| Rasatala | Shatter Shard | breaks into piercing shards on a hit |
| Patala | Jewel Prism | splits into a fan after a moment |
| Patala | Executioner | three times the damage on a foe under 40% |
| Patala | Last Light | a slow round that swells as it flies |

## Where it lives
- Data and rank III abilities: `weapon/WeaponDef.java` (new fields, builders, `shardOf`).
- Flight and hit riders: `weapon/Bullet.java` (`stepSpecial`, `onHit`, `damageTo`, `hits`); mirror shot in `Weapon.fire`; clouds in `World.zones`.
- Foe statuses: `Enemy.ignite / slowBy / freeze` (`dotT`, `slowT`, `stunT`, separate from the Agni astra's `burnT`).
- Art: `tools/enemy_art/loka_weapons.py` writes the 16x16 bullet (`atala/bullets`) and card (`weapon_display_images`) for each.
- Shop: the ARMS rail is a grid (standard block, then one row per loka) with a page on the right; the in-flight wheel takes a second ring above fourteen owned patterns.

## Astras
Only a hull's **2nd and 3rd variants** can forge and loose an astra, on every ship (`Hangar.canForge`, enforced in `Astra.releases`). The base variant's shards are only for selling; the shop says so and F shows a short notice.
