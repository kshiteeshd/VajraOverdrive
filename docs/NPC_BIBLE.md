# The people of Atala

The blue figures on the map are **people, not enemies**. Before this pass they were Lance hulls tinted
cyan, they spoke in floating sentences you could miss in a fight, and finishing a job meant a line of
text drifting up over your own ship. Now they are planned characters with art of their own, a first
meeting that is a scene, and jobs that live on the HUD.

## What a person looks like

A soldier is pearl and gold with a black slit for an eye. A person is the opposite in every way you
can read at a glance:

- **lit from within in sky blue**, never slit-eyed - their eyes are lamps;
- a **thin halo ring** over the head (the diya the people of the lokas keep lit);
- they **hail**: until you have met them a gold `!` bobs over their head and the halo widens; near
  them their name shows underneath in blue;
- their speech bubble is **sky blue** (Indra's thoughts are lavender, soldiers ivory, the ruler gold).

Sheets: `assets/npcs/atala/<name>.png`, 512x128, 64x64 cells, hand-drawn in `tools/enemy_art/npcs.py`,
run `python tools/gen_npc_sheets.py`. Row 0 idle x4, talk x4. Row 1 work x4, hail x4. Drawn at 48 px.

## The three

### Uparichara's Scout - the watch
*Story:* King Uparichara Vasu flew a crystal chariot that **Indra himself gave him**. His watch-pilots
were the first to fly such craft. This one fell above the third band, and his beacon still burns.
*Shape:* a pearl watch-craft, one wing whole and one broken with a crack through it, smoke rising from
the wound, a beacon lamp on his back that breathes, a lens on a stalk for a face.
*Job:* **raise his beacon** (the band-2 cache). It pays 100c and a life, and puts every camp on the
compass; the brokers pin a **bounty** on Dambha the same hour.
*First meeting:* he recognises Indra - "YOU GAVE OUR KING HIS WINGS."

### The Ninth Maya - the forge
*Story:* Bala is *son of Maya*, the architect of the asuras; the Ninth Maya is the last wright of that
line, her woods still bend the rock. She put the hammer down, and her son holds the door of Atala - and
holds a piece of Indra's bolt.
*Shape:* a broad pearl hull with a bronze keel and an **anvil for a prow**, a forge lit in her belly, a
hammer arm that rests up and swings when she works, tongs on the left.
*Job:* **salvage** six wrecks of her woods. A wreck taken is a gold ring and a pip on the tracker. Done:
200c and the first shard of Akasha, and her letter after Bala falls.
*First meeting:* "I AM THE NINTH MAYA. I PUT THE HAMMER DOWN." ... "I WILL TEMPER YOU."

### Kaliya the Tide-Pilot - the road
*Story:* Kaliya is a **naga** of the Kadru brood, a courier of the water roads, far from Mahatala
because **Garuda's shadow** drives his kin upward. He knows Takshaka.
*Shape:* an S of blue scales that travels as he moves, a flared hood with three gold jewels, two lamps for
eyes, a halo over his tail.
*Job:* none yet. He is the seam to **Mahatala**: "SAY MY NAME TO TAKSHAKA, BELOW." His water-road
job lands with Sutala.

## How they speak

| when | how | why |
|---|---|---|
| the **first meeting** | a short cutscene: letterbox bands, a blue bubble over them, Indra's lavender thought under his hull | this is where a person says who they are - it deserves the screen |
| a **job turns** | a bubble over their hull while the world keeps moving, plus a **toast** on the HUD | a flight is not stopped for a status line |
| a **job is open** | a panel on the left of the HUD: the giver, the task, progress as diamonds | the count belongs on the HUD, not in the sky |
| the **ruler falls** | Maya's letter read in the narrator band, only if she was met | a promise paid - an unmet voice pays nothing |

Rules that do not bend: a person is never hurt, never fires, and is never woken by a fight; they never
hold the gate; and in a shared race the scenes are skipped but the jobs still turn.
