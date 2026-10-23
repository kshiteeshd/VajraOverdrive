# The lore comic: image-generation prompt pack

Three portrait pages that open the game, drawn in a **dark, painterly, manga-composed, Arcane-lit** style. Each
panel is a full piece of art that could stand alone as a wallpaper.

**Nothing in this pack asks the generator to draw text.** Image models spell badly. Generate the art with no
lettering; the captions in section 6 are laid over it afterwards (by me, in the game's own font).

## How to use this pack

1. Generate **one image per panel** (section 4), not one image per page. Per-panel generation gives far better
   faces and detail than asking for a whole comic page, and each panel stays wallpaper-sharp.
2. Paste the **GLOBAL STYLE BLOCK** (section 1) at the start of every panel prompt, then the **CHARACTER BLOCKS**
   (section 2) for whoever appears, then the panel's own text. Always add the **NEGATIVE PROMPT** (section 3).
3. For consistency across panels, generate the character sheets first (section 2). Feed them back as reference
   images (Midjourney `--cref` / `--sref`, Flux or SDXL IP-Adapter, or "reference image" in your tool).
4. Panels are generated at the aspect ratio listed. Upscale to at least 2400 px on the long side.
5. Save them as `panel-1a.png`, `panel-1b.png` ... in `assets/comic/panels/` (any size). Then either:
   - send them to me and I compose the three pages (frames, gutters, lettering), or
   - if you compose a full page yourself, save it as `assets/comic/page-1.png` .. `page-3.png`. The game already
     shows those, full-screen, before the menu the first time it opens.

## 1. GLOBAL STYLE BLOCK (paste into every prompt)

```
Cinematic dark-fantasy illustration, the look of the animated series Arcane: hand-painted textured brushwork over
3D-like volumes, dramatic chiaroscuro lighting, strong rim light on every figure, saturated coloured light
sources against deep shadow, visible painterly grain, soft atmospheric haze and volumetric god-rays, shallow
depth of field with sharp focal subject. Composed like a premium manga splash page: bold dynamic angles, heavy
ink-weight contour on the figures, confident speed and impact lines only where action demands, dramatic
foreshortening. Semi-realistic anatomy and faces with real skin texture, believable fabric and metal, but
stylised with an artist's eye: elongated elegant proportions, sharp expressive eyes. Ancient Indian sacred
architecture and iconography blended with sleek science-fiction: carved sandstone and black stone, gold filigree,
pearl-white enamel, lotus and rangoli motifs, glowing lavender energy conduits. Colour grade: deep indigo and
ink black shadows, warm molten gold highlights, lavender-violet accents. Ultra detailed, 8k, wallpaper quality,
no text, no letters, no logos, no watermark, no frame, no border, full-bleed.
```

## 2. CHARACTER BLOCKS

Generate each of these first as a turnaround sheet (front, three-quarter, profile, plus close-up of the face)
on a plain dark background, then use as the reference for every panel.

### INDRA, lord of Swarga (the player's pilot)
```
INDRA: a regal warrior-king in his late thirties, athletic and lean, broad shoulders, sun-bronzed skin with a
golden undertone, sharp noble face, strong jaw, intense dark eyes with lavender light reflected in them, black
hair swept back and tied in a topknot with a few loose strands, a short trimmed beard. A tall golden crown with
a single lavender gem. Pearl-white and gold armour fused with ceremonial robes: a layered ivory-and-gold
chestplate with lotus engraving, gold shoulder guards shaped like folded wings, a long deep-indigo sash and cape
lined in lavender, bare forearms with gold bracers, glowing lavender thread-lines running through the armour
seams like circuitry. A third-eye mark is NOT present. He looks proud, exact, certain of his power.
After the theft (page 2 onward) the same figure, but the cape is torn, gold dulled, eyes shadowed, jaw set.
```

### THE VAJRAYUDHA (Indra's weapon)
```
THE VAJRAYUDHA: an ancient thunderbolt weapon, about a forearm in length, a central grip with a carved lotus
collar, two lotus-petal prongs at each end curving in to meet at a point (the classic double-ended vajra),
material is pale bone-white ivory veined with polished gold, hairline lavender lightning crawling over it,
radiating a soft white-gold glow. It is made from the bones of the sage Dadhichi, so show faint fossil-like
grain in the ivory and tiny carved mantra lines in gold.
```

### TRIVIKRAMA, who is Vishnu (the strange one)
```
TRIVIKRAMA: unmistakably a god, but wrong in the way he presents himself, unsettling and uncanny rather than
frightening. Tall and slender with deep blue-teal skin like twilight sky, a perfectly serene, faintly amused
smile that never changes, large calm eyes that are too still, rarely blinking, with no visible pupil, only
warm gold light. Four arms. A tilted, slightly too-small golden crown. He wears magnificent yellow silk and a
lotus garland, BUT he treats them casually and wrongly: a plain cloth satchel slung over the silk, a
mundane black parasol (umbrella) held open indoors, a simple wooden walking staff in one hand, a small clay cup
of water in another, bare feet that hover one finger-width above the floor and leave no footprints. He bows
too deeply and too long, with a hand placed politely over his heart. His shadow does not match him: it is
many times his size, falls the wrong direction against the light, and has its own faint lotus-eyes.
Courteous, soft-spoken, humble in manner, impossible in presence. A gentle test, never a villain.
```

### INDRA'S SHIP (reference: assets/vajra_ships/mk_1/ship_1.png, the game's Mk-1)
```
INDRA'S SHIP, the Vajra Mk-1: a sleek single-seat spacefighter shaped like a vajra, pearl-grey enamel hull with
polished warm gold trim, a narrow central body tapering to a needle nose, two long gold prongs curving
forward on each side like the arms of a vajra (the silhouette from the game's pixel sprite: pearl body in the
middle, gold claws left and right), a bubble canopy of dark glass with a lavender glow inside, engines at the
rear burning pale gold with a lavender core, carved lotus filigree on the wings, tiny lit lavender conduit
lines along the hull. Reads as a sacred weapon that is also a machine.
```

## 3. NEGATIVE PROMPT (paste everywhere)

```
text, letters, words, captions, speech bubbles, watermark, signature, logo, frame, border, comic panel border,
pixel art, 8-bit, low resolution, blurry, jpeg artifacts, flat cartoon, chibi, cute, anime moe, western
superhero comic, photoreal photograph, extra fingers, malformed hands, deformed face, asymmetrical eyes,
duplicate limbs (except Trivikrama's four arms), cropped head, modern clothing, guns, cluttered background
```

## 4. THE NINE-PLUS PANELS

Page size when composed: portrait, about 2 : 3. Aspect ratios below are for generating the panel itself.

### PAGE 1 - "THE THRONE OF SWARGA" (4 panels)

**1A. Establishing splash, ratio 16:9, wallpaper hero shot.**
*Indra enthroned.* A vast open-air hall at the peak of Swarga, high above a sea of luminous clouds at golden hour.
Colossal sandstone columns carved with lotus and apsaras rise into a coffered ceiling; the roof is open to a
violet-indigo sky packed with stars and one enormous ringed moon. Long god-rays pour through the columns. At the
far end, on a stepped dais of seven tiers, INDRA sits upright on a monumental throne of carved gold and black
stone, whose back is a giant gold halo carved as a thousand-petalled lotus. He rests the VAJRAYUDHA across his
knees, one hand on its grip, a low arc of lavender lightning crawling from it across the floor. Camera: low
angle from the foot of the stairs looking up, the throne dead centre, symmetrical one-point perspective, tiny
robed attendants and gold-armoured guards lining both sides to show scale. Mood: absolute power and calm pride.
Palette: molten gold, ink black, deep indigo, lavender rim light.

**1B. Small inset, ratio 1:1, the origin of the bolt.**
*Dadhichi's gift.* A flashback rendered in a washed-out, warm sepia-and-gold tone with soft vignette edges. An
old, serene sage with long white hair and beard, thin and luminous, sits in lotus pose on a rock at dawn, eyes
closed, smiling, giving up his life. His body dissolves upward into drifting motes of gold light, and from the
light a clean pale-bone thunderbolt forms between his open palms. Gentle, sacred, sad. Soft rim light, mist.

**1C. Close-up, ratio 4:5 (portrait).**
*Indra's gaze.* Extreme close-up of INDRA's face from the front, lower three-quarter, chin slightly raised. Lavender
light from the Vajra lights his jaw and cheekbones from below, a gold halo blooms behind his head. Eyes sharp,
unblinking, a reflection of the thunderbolt in each pupil. Detailed skin pores, a faint scar through the
left eyebrow, stray hair lifting in a breeze. Manga-style hard-shaded eye highlights, painterly skin. Expression:
the calm of a man who has never lost anything.

**1D. Wide, ratio 21:9 (cinematic strip), the shadow.**
*The visitor arrives.* The great doors at the far end of the throne hall stand open to a blazing white-gold
sunset. In the doorway, the SILHOUETTE of a small figure holding an open parasol, but its SHADOW is impossibly
long: it stretches across the entire marble floor, up the seven steps of the dais and over INDRA's knees,
many times larger than the figure, with a faint lotus-shaped halo. Guards in the foreground turn and stare;
a single gold petal falls through the light. Camera: low behind INDRA's shoulder, so we see his back and the
shadow rising over him. Mood: uncanny, quiet dread, polite menace.

### PAGE 2 - "THE TEST" (5 panels)

**2A. Wide, ratio 16:9.**
*The bow.* TRIVIKRAMA stands before the throne, bowing impossibly low and long, parasol held open above his
head INDOORS, satchel at his hip, clay cup in one hand, three hands folded politely. His feet hover above the
marble. His giant shadow spreads behind him up the wall, its own faint eyes open. INDRA looks down from the
throne with polite confusion edging into unease. Attendants stare, frozen. Soft top-down golden light on
TRIVIKRAMA, hard cold lavender backlight on INDRA. Detailed sandstone, polished floor reflections.

**2B. Close-up, ratio 1:1.**
*The taking.* Tight on two hands and the bolt. TRIVIKRAMA's long blue fingers lift the VAJRAYUDHA out of
INDRA's grasp as easily as taking a flower from a child, while INDRA's gold-braceleted hand is open and
unresisting, his fingers still curled in the shape of the grip. Lavender lightning spits and then dies. Extreme
depth of field, background melting into gold bokeh. Behind them, faintly, TRIVIKRAMA's calm smile.

**2C. Tall vertical, ratio 1:2.**
*The three steps.* A vertical sequence in one image from top to bottom, a single immense blue-teal giant,
TRIVIKRAMA grown to cosmic scale, his silhouette filling the frame: the first foot planted on a blue-green
Earth from orbit, the second raised through the sky and stars, the third coming down upon SWARGA's palace,
which sits tiny beneath the sole of a gigantic bare foot ringed with lotus-marks. A gold crown tilted on the
sky-sized head far above, smiling serenely, as if nothing unusual is happening. Starfield, nebula light,
epic scale. Painterly, strong sense of scale, a lone tiny gold ship of light for comparison.

**2D. Full-width, ratio 16:9.**
*The seven pieces.* TRIVIKRAMA, now huge and calm against a night sky, snaps the bone-white VAJRAYUDHA into
SEVEN pieces. Seven shards of glowing bolt scatter outward like falling stars, each trailing a different
coloured light: pearl-and-gold, ember-orange, sea-teal, brass, crimson, deep-violet and prismatic
rainbow. Below, seven layered worlds seen as stacked glowing discs under a dark sky, one shard falling toward each.
Long exposure light streaks, cinematic scale, composition like a meteor shower over a stylised cosmos.

**2E. Close-up, ratio 3:2.**
*The smile.* TRIVIKRAMA, now normal-sized again, leans down to INDRA (kneeling on the dais steps, head bowed,
crown in his hand), parasol casting a soft gold circle of shade over them both. TRIVIKRAMA's serene smile, one
eye opening a hair and looking directly at the viewer, impossible and kind. INDRA's face is in shadow, jaw
clenched, a single tear tracking through the dust, but his eyes lift to meet TRIVIKRAMA's. Mood: a test is
being asked, and answered with silence. Warm gold from above, cold lavender from below.

### PAGE 3 - "THE JOURNEY" (4 panels)

**3A. Wide, ratio 21:9.**
*The empty throne.* The same hall, hours later. A long, cold blue dusk. The throne is empty; a single lotus
petal rests on the cushion where the bolt used to lie. INDRA, in a torn cape, walks away down the seven steps
toward a giant arched gate at the bottom, a lone small figure under towering columns. Rim light, long shadows
(his own, normal this time), cold mist, the gold of the hall now dim.

**3B. Reveal, ratio 3:4 (portrait), the ship.**
*The hangar.* The VAJRA Mk-1 (see the ship block) sits on a launch cradle inside a carved-stone Swarga
hangar, nose toward the viewer, lit from underneath by lavender floor light, with gold trim glinting and
vents steaming. Hero three-quarter low-angle shot, shallow depth of field, a lit runway of glowing sigils
receding into the distance behind it, INDRA a small figure on the gantry climbing toward the canopy. Product-shot
quality: polished enamel reflections, real metal, intricate lotus filigree.

**3C. Close-up, ratio 1:1.**
*The cockpit.* INDRA's face in the cockpit, helmetless, seen through the curved glass of the canopy. Lavender HUD
glow flickers across his eyes and jaw, golden star light behind him. A determined, tired face, wet eyes,
a small hint of faith. Reflections of the departing hangar lights slide across the glass. Manga-style
highlight in the eyes, painterly skin, extreme detail.

**3D. Full-bleed final wallpaper, ratio 16:9 or 2:3 (generate both).**
*Departure.* The VAJRA Mk-1 launches from Swarga's open gate, a tiny pearl-and-gold arrowhead streaking off a
cliff of white palace and cloud, its engines burning pale gold with lavender cores. Far below in a vast
indigo-black void, SEVEN glowing lokas hang in a vertical descending chain like lanterns on a thread, each
a different colour (pearl-gold, ember, teal, brass, stone, crimson, prismatic), each with a tiny falling
shard of light above it. The ship is small, the cosmos enormous, composition leading the eye from the ship
down the chain of lights. Behind the horizon, barely visible in the mist, the faint blue silhouette of
TRIVIKRAMA's parasol like a second moon, watching. A large clean area of dark sky left empty at the top for the
game title.

## 5. CONSISTENCY CHECKLIST

- INDRA: gold crown with ONE lavender gem; ivory-and-gold armour; indigo sash; lavender circuitry lines.
- Torn cape and dulled gold from 2E onward.
- TRIVIKRAMA: four arms, black parasol indoors, satchel, clay cup, hovering feet, mismatched shadow,
  gold eyes with no pupil, one continuous smile.
- VAJRAYUDHA: bone-white ivory with gold veins, double-ended with lotus-petal prongs, lavender lightning.
- SHIP: pearl-grey hull, warm gold prongs, lavender glow, gold engine flame.
- Sky: ink-indigo with warm gold light; lavender is the accent. Seven loka colours are only used in 2D and 3D.

## 6. THE CAPTIONS (laid over the art afterwards, one short line per panel)

| Panel | Caption |
|---|---|
| 1A | IN SWARGA, INDRA KEPT THE THREE WORLDS. HE KEPT THEM WITH ONE WEAPON. |
| 1B | THE VAJRAYUDHA WAS MADE FROM THE BONES OF THE SAGE DADHICHI, WHO GAVE THEM FREELY. |
| 1C | IT BROKE VRITRA. IT HELD THE SKY. IT NEVER LEFT HIS HAND. |
| 1D | THEN A SHADOW CROSSED THE SKY. |
| 2A | A GUEST CAME BEARING A PARASOL, AND ASKED FOR NOTHING AT ALL. |
| 2B | TRIVIKRAMA. THE THREE-STEPPER. INDRA DID NOT REACH FOR IT. |
| 2C | THE FIRST STEP COVERED THE EARTH. THE SECOND, THE SKY. THE THIRD FELL ON SWARGA. |
| 2D | HE BROKE THE BOLT IN SEVEN, AND SENT ONE PIECE TO EACH LOKA BELOW. |
| 2E | "FOLLOW IT, IF YOU STILL BELIEVE." |
| 3A | A KING WITHOUT HIS WEAPON IS ONLY A MAN WITH A THRONE. |
| 3B | INDRA TOOK THE ONE THING SWARGA STILL HAD: A SHIP. |
| 3C | "I AM INDRA. I AM TAKING IT BACK." |
| 3D | SEVEN LOKAS. SEVEN PIECES. THE FIRST DOOR IS ATALA. |

(Trivikrama is Vishnu, here to test Indra's loyalty and faith in him. The comic never says so outright; the
last frame leaves it as a smile.)
