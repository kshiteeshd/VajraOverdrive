"""THE SCORES. Every piece of music in the game is written out here as phrases of swaras (raga.py), a tala and an instrumentation, then rendered by the instruments of synth.py.
Nothing is generated at random: change a note here and the piece changes; run it twice and it is the same.

Run from the repo root:  python tools/sound/music.py [title menu atala vitala bala bhairava rest stings]
Writes assets/audio/music/*.ogg (loops are folded so they repeat with no seam).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import synth as S
from raga import LOKA_SA, parse

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'audio', 'music')
LOOP = 60.0


def line(text, beats):
    """A cycle of notation that must add up to the tala's length."""
    total = sum(n.dur for n in parse(text, 1.0))
    assert abs(total - beats) < 1e-6, 'cycle is %s beats, wanted %s: %s' % (total, beats, text)
    return text


# ============================================================================================================================ the talas
# a theka: one bol per beat. Bols: dha dhin tin na ge tak tun, PK = pakhawaj dha, _ = silent (khali)
TALAS = {
    'teental': dict(beats=16, bols='dha dhin dhin dha dha dhin dhin dha dha tin tin na na dhin dhin dha'.split(), sam='gong', khali=8, kind='tabla'),
    'ektal': dict(beats=12, bols='dhin dhin dha ge tin na kat ta dha ge dhin na'.split(), sam='gong', khali=6, kind='tabla'),
    'chautal': dict(beats=12, bols='PK PK dhin tak PK tak PK PK dhin tak PK tak'.split(), sam='gong', khali=-1, kind='pakhawaj'),
    'jhaptal': dict(beats=10, bols='dhin na dhin dhin na tin na dhin dhin na'.split(), sam='gong', khali=5, kind='tabla'),
    'rupak': dict(beats=7, bols='tin tin na dhin na dhin na'.split(), sam=None, khali=0, kind='tabla'),
    'dhamar': dict(beats=14, bols='PK tak PK tak dha tun PK tak dha PK tak dhin dhin na'.split(), sam='gong', khali=-1, kind='pakhawaj'),
}


def bol_sound(bol, sa):
    f = sa * 2.0
    if bol == 'PK':
        return S.pakhawaj(sa * 0.55, 0.9)
    if bol in ('dha',):
        return S.tabla('dha', f * 2)
    if bol == 'dhin':
        a, b = S.tabla('tin', f * 2), S.tabla('ge', f * 2)
        out = np.zeros(max(len(a), len(b)))
        out[:len(a)] += a * 0.6
        out[:len(b)] += b
        return out
    if bol in ('tin', 'ta'):
        return S.tabla('tin', f * 2)
    if bol in ('na', 'tun'):
        return S.tabla('na' if bol == 'na' else 'tun', f * 2)
    if bol == 'ge':
        return S.tabla('ge', f * 2)
    if bol in ('tak', 'kat'):
        return S.tabla('tak')
    raise ValueError(bol)


def pulse(mix, tala, sa, beat_s, cycles, start=0.0, gain=1.0, density=1.0, accent_sa=None):
    """The theka over `cycles` cycles; density < 1 thins it (only the first beat of each group and the sam)."""
    T = TALAS[tala]
    cache = {}
    for c in range(cycles):
        for b, bol in enumerate(T['bols']):
            t = start + (c * T['beats'] + b) * beat_s
            if bol not in cache:
                cache[bol] = bol_sound(bol, sa)
            sam = (b == 0)
            if density < 1.0 and not sam and (b % 4 != 0 and b != T['khali']):
                continue
            g = gain * (1.0 if sam else 0.72 if b == T['khali'] else 0.6)
            if b == T['khali'] and T['khali'] >= 0:
                g *= 0.55                                           # the empty beat: the open hand, the quiet stroke
            mix.add(cache[bol], t, g, pan=-0.2 if b % 2 else 0.2)
        if T['sam'] and accent_sa:
            mix.add(S.gong(sa * 0.5, 4.0), start + c * T['beats'] * beat_s, accent_sa, pan=0.0)


# ============================================================================================================================ the drone
def tanpura_bed(mix, sa, dur, gain=0.55):
    """Pa Sa Sa Sa. (low) over six seconds, repeating, alternately panned."""
    strings = [(sa * 0.75, -0.35), (sa, 0.1), (sa, 0.3), (sa * 0.5, -0.1)]
    cache = {}
    t, i = 0.0, 0
    while t < dur:
        f, p = strings[i % 4]
        if f not in cache:
            cache[f] = S.tanpura(f, 7.0)
        mix.add(cache[f], t, gain, p)
        t += 1.5
        i += 1


# ============================================================================================================================ melody
def play(mix, notes, beat_s, inst, start=0.0, gain=1.0, pan=0.0, glide=0.1, rep=1, span=None, grace=None, **kw):
    """Lay a list of Notes on the mix with `inst(freq_or_curve, dur, **kw)`. A glided note carries its pitch into the next in a curve; a gamaka leans and returns.
    rep repeats the whole phrase `span` beats apart (the same composition, again). grace: a short neighbour note before each note (the ornament of the fast layas)."""
    total = max(n.start + n.dur for n in notes)
    span = span or total
    for r in range(rep):
        off = start + r * span * beat_s
        for i, n in enumerate(notes):
            if n.f is None:
                continue
            t = off + n.start * beat_s
            d = n.dur * beat_s
            f = n.f
            nxt = notes[i + 1].f if (n.glide and i + 1 < len(notes)) else None
            if nxt is not None:
                gl = min(d * 0.5, 0.5)
                k = max(2, int(gl * 100))
                curve_ = np.concatenate([np.full(max(2, int((d - gl) * 100)), f), np.linspace(f, nxt, k)])
                f = curve_
            elif n.gamaka:
                m = max(8, int(d * 100))
                x = np.arange(m) / 100.0
                f = n.f * (1.0 + 0.035 * np.sin(2 * np.pi * 2.2 * x) * np.clip(x / 0.5, 0, 1))
            if grace is not None and n.f is not None:
                gf = n.f * grace
                mix.add(inst(gf, 0.07, **kw), t - 0.07, gain * 0.5, pan)
            x = inst(f, d + 0.25, **kw)
            mix.add(x, t, gain, pan)


def echo(notes, shift_beats, octave=1):
    """The response: the same phrase answered later, an octave away (a device of the form, not a random one)."""
    out = []
    for n in notes:
        out.append(type(n)(None if n.f is None else n.f * octave, n.start + shift_beats, n.dur, n.glide, n.gamaka, n.name, n.octave))
    return out


# ============================================================================================================================ the explore pieces
def explore(name, sa, tala, cycles_text, raga_voice='a'):
    """Render the six stems of a loka's explore music: drone, drift, combat, crisis, threat, voice.
    One written composition (`cycles_text`, one string per cycle) is played at three layas that double: the same notes, twice and four times as fast."""
    T = TALAS[tala]
    B = T['beats']
    C = len(cycles_text)
    comp = ' '.join(line(c, B) for c in cycles_text)
    notes = parse(comp, sa)
    beats = B * C
    drift_beat = LOOP / beats
    stems = {}

    # drone: tanpura only, with a low bowl that breathes
    m = S.Mix(LOOP)
    tanpura_bed(m, sa, LOOP)
    m.add(S.bowl(sa, 12.0), 0.0, 0.18)
    m.add(S.bowl(sa * 1.5, 12.0), 30.0, 0.1)
    m.reverb(0.35, 3.0)
    stems['drone'] = m.loop_fold()

    # DRIFT (vilambit): a bansuri breathes the composition with long glides; the veena answers it far off; one pakhawaj stroke on each sam
    m = S.Mix(LOOP)
    play(m, [type(n)(n.f, n.start, n.dur, True if i % 2 == 0 else n.glide, n.gamaka, n.name, n.octave) for i, n in enumerate(notes)], drift_beat, S.bansuri, gain=0.7, pan=-0.1, breath=0.2)
    play(m, echo(notes, 3.0, 2.0), drift_beat, S.veena, gain=0.22, pan=0.4, decay=2.4)
    for c in range(C):
        m.add(S.pakhawaj(sa * 0.55, 1.2), c * B * drift_beat, 0.5)
    m.reverb(0.4, 3.2)
    stems['drift'] = m.loop_fold()

    # COMBAT (madhya): the sarod plays the gat, the bansuri doubles it softly, the tabla keeps the theka, the santoor echoes a beat later
    beat_c = drift_beat / 2.0
    m = S.Mix(LOOP)
    play(m, notes, beat_c, S.sarod, gain=0.72, pan=-0.15, rep=2, span=beats, decay=0.9)
    play(m, notes, beat_c, S.bansuri, gain=0.28, pan=0.1, rep=2, span=beats, breath=0.12)
    play(m, echo(notes, 1.0, 2.0), beat_c, S.santoor, gain=0.18, pan=0.45, rep=2, span=beats)
    pulse(m, tala, sa, beat_c, C * 2, gain=0.7, density=0.6, accent_sa=0.3)
    m.reverb(0.28, 2.4)
    stems['combat'] = m.loop_fold()

    # CRISIS (drut): the ornamented gat at four times the pace, the whole theka, kartal on every beat, a nagara on each sam, the sarangi cries above
    beat_x = drift_beat / 4.0
    m = S.Mix(LOOP)
    play(m, notes, beat_x, S.sarod, gain=0.7, pan=-0.2, rep=4, span=beats, decay=0.6, grace=1.0595)
    play(m, notes, beat_x, S.sarangi, gain=0.3, pan=0.2, rep=4, span=beats)
    pulse(m, tala, sa, beat_x, C * 4, gain=0.85, density=1.0, accent_sa=0.35)
    kart = S.kartal()
    for i in range(beats * 4):
        m.add(kart, i * beat_x, 0.5 if i % 2 == 0 else 0.3, pan=0.5 if i % 2 else -0.5)
    for c in range(C * 4):
        m.add(S.nagara(sa * 0.5, 1.2), c * B * beat_x, 0.55)
    m.reverb(0.2, 1.8)
    stems['crisis'] = m.loop_fold()

    # THREAT: a low bowed string and a bowl a hair out of tune: the beating is the pressure; bowed metal rises across the minute
    m = S.Mix(LOOP)
    for k in range(6):
        t = k * 10.0
        m.add(S.sarangi(sa * 0.5, 11.0, vib=0.003, attack=3.0, release=3.0), t, 0.5, pan=-0.2)
        m.add(S.bowl(sa * 0.5 * 1.0058, 11.0), t, 0.3, pan=0.2)
    m.add(S.gong(sa * 0.25, 12.0), 0.0, 0.5)
    m.reverb(0.4, 3.5)
    stems['threat'] = m.loop_fold()

    # VOICE: the low chant, a vowel to each cycle of the drift: it enters only in the crisis
    m = S.Mix(LOOP)
    seq = [(sa * 0.5, raga_voice), (sa * 0.75, raga_voice), (sa * 0.5, raga_voice), (sa * 0.5 * 2.0 ** (-2 / 12.0) * 2, raga_voice)]
    cyc = LOOP / 6.0
    for i in range(6):
        f, v = seq[i % 4]
        m.add(S.voice(f, cyc * 0.9, v, attack=1.2, release=1.4), i * cyc, 0.7)
    m.add(S.overtone_voice(sa * 0.5, 30.0, 1100.0), 0.0, 0.2)
    m.add(S.overtone_voice(sa * 0.5, 30.0, 1500.0), 30.0, 0.2)
    m.reverb(0.45, 3.5)
    stems['voice'] = m.loop_fold()

    for k, v in stems.items():
        S.save(os.path.join(OUT, '%s_%s.ogg' % (name, k)), v, quality=4)
    print('explore', name, 'ok')


ATALA = [  # Yaman, Sa = D, Teental (16): the sthayi, the antara and the return
    line("N.:1 R:1 G:2 | G:1 M:1 P:2 | P:1 M:1 G:1 R:1 | G:1 R:1 S:2", 16),
    line("P:1 D:1 N:2 | N:1 D:1 N:1 S':1 | R':1 S':1 N:1 D:1 | P:1 M:1 G:1 R:1", 16),
    line("N.:1 R:1 G:1 M:1 | P:2 G:1 M:1 | G:1 R:1 N.:1 R:1 | S:4", 16),
]

VITALA = [  # Bhairav, Sa = C#, Ektal (12)
    line("S:2 r:1 G:1 m:2 P:2 G:1 m:1 r:1 S:1", 12),
    line("G:1 m:1 P:2 d:2 P:1 m:1 G:1 r:2 S:1", 12),
    line("P:1 d:1 N:2 S':2 N:1 d:1 P:1 m:1 G:1 m:1", 12),
    line("S':2 N:1 d:1 P:2 m:1 G:1 r:1 G:1 r:1 S:1", 12),
]


# ============================================================================================================================ the rulers
def ruler(name, sa, tala, cycles_text, lead, pulse_gain=0.9, voice_f=None, vowel='a', beats=None):
    """A boss phase: one loop at a fixed tempo. Lead on the sarangi (growling) with the rudra-veena style low pluck beneath, the theka heavy, a chant over it."""
    T = TALAS[tala]
    B = T['beats']
    comp = ' '.join(line(c, B) for c in cycles_text)
    notes = parse(comp, sa)
    total = B * len(cycles_text)
    beat = LOOP / total
    m = S.Mix(LOOP)
    tanpura_bed(m, sa, LOOP, gain=0.5)
    play(m, notes, beat, lead, gain=0.8, pan=-0.1, vib=0.007) if lead is S.sarangi else play(m, notes, beat, lead, gain=0.8, pan=-0.1)
    play(m, notes, beat, S.veena, gain=0.35, pan=0.3, decay=1.4)                                  # the low plucked line under it
    bass = [type(n)(None if n.f is None else n.f / 2.0, n.start, n.dur, n.glide, n.gamaka, n.name, n.octave) for n in notes]
    play(m, bass, beat, S.veena, gain=0.4, pan=0.0, decay=1.8)
    pulse(m, tala, sa, beat, len(cycles_text), gain=pulse_gain, accent_sa=0.4)
    for c in range(len(cycles_text)):
        m.add(S.nagara(sa * 0.5, 1.5), c * B * beat, 0.5)
    f0 = voice_f or sa * 0.5
    for i in range(len(cycles_text)):
        m.add(S.voice(f0 if i % 2 == 0 else f0 * 1.5, B * beat * 0.9, vowel, attack=1.0, release=1.0), i * B * beat, 0.45)
    m.reverb(0.3, 2.6)
    S.save(os.path.join(OUT, name + '.ogg'), m.loop_fold(), quality=4)
    print('ruler', name, 'ok')


BALA_P1 = [  # Marwa (no Sa, no Pa: it never rests), Chautal (12)
    line("D.:2 N.:1 r:1 G:2 M:2 G:1 r:1 N.:2", 12),
    line("r:2 G:1 M:1 D:2 N:2 D:1 M:1 G:2", 12),
    line("M:2 D:1 N:1 r':2 N:1 D:1 M:2 G:1 r:1", 12),
    line("D:2 M:1 G:1 r:2 G:2 M:1 D:1 N:1 D:1", 12),
    line("N:2 D:2 M:2 G:2 r:2 N.:2", 12),
]
BALA_P2 = [  # the shattering: Jhaptal (10, 2+3+2+3), the accents slip off the beat
    line("D.:1 M.:0.5 G.:0.5 r.:1 G.:1.5 M.:0.5 D.:2 N.:1 D.:1.5 M.:0.5", 10).replace('D.:1.5 M.:0.5', 'D.:2'),
    line("r:1 G:0.5 M:0.5 D:1.5 N:0.5 D:1 M:1 G:0.5 r:0.5 G:1 M:2", 10),
    line("N:1 r':0.5 N:0.5 D:1 M:1 G:0.5 M:0.5 D:2 N:1 D:1 M:1", 10),
    line("M:1.5 D:0.5 N:1 r':1 N:0.5 D:0.5 M:1 G:1 r:0.5 G:0.5 M:2", 10),
    line("D:1 N:1 r':2 G':1 r':1 N:1 D:1 M:2", 10),
    line("M:1 G:0.5 r:0.5 G:1 M:1 D:2 M:1 G:1 r:1 N.:1", 10),
]
BALA_P3 = [  # the doors: Rupak (7), which begins on the empty beat
    line("_:1 D:1 M:1 G:1 r:2 G:1", 7),
    line("_:1 M:1 D:1 N:1 D:2 M:1", 7),
    line("_:1 N:1 r':1 N:1 D:1 M:1 G:1", 7),
    line("_:1 r:1 G:1 M:1 D:2 N:1", 7),
    line("_:1 D:1 N:1 r':2 N:1 D:1", 7),
    line("_:1 G':1 r':1 N:1 D:1 M:2", 7),
    line("_:1 M:1 G:1 r:1 G:2 M:1", 7),
    line("_:1 D:1 M:1 G:1 r:1 N.:2", 7),
]
BHAIRAVA = [  # Bhairav, Dhamar (14 = 5+2+3+4)
    line("S.:3 r.:1 G.:1 m.:2 P.:3 d.:2 P.:2", 14),
    line("d.:2 P.:1 m.:1 G.:2 r.:1 S.:2 r.:1 G.:1 m.:1 P.:2", 14),
    line("P.:2 d.:1 N.:1 S:3 N.:1 d.:1 P.:2 m.:1 G.:1 r.:1", 14),
    line("S:2 r:1 S:1 N.:2 d.:2 P.:2 m.:1 G.:1 r.:1 S.:1", 14),
]


# ============================================================================================================================ the title, the menus, the rests, the stings
def title():
    sa = LOKA_SA[0]
    m = S.Mix(LOOP)
    tanpura_bed(m, sa, LOOP, gain=0.6)
    m.add(S.morph_voice(sa * 0.5, 9.0, ['o', 'm'], attack=2.0, release=3.0), 1.0, 0.8)                      # Om
    m.add(S.shankha(sa * 2, 3.0), 12.0, 0.55)                                                               # the VAJRA motif: Sa Ma Pa Sa', struck
    for i, (r, t) in enumerate([(1.0, 13.0), (4 / 3, 14.2), (3 / 2, 15.4), (2.0, 16.6)]):
        m.add(S.ghanta(sa * 4 * r, 2.5), t, 0.5)
        m.add(S.nagara(sa * 0.5 * r, 1.0), t, 0.5)
    pilot = parse("S:1 R:1 G:2 P:2 G:1 R:1 S:3 | G:1 P:1 D:2 P:2 G:1 R:1 S:3 | R:1 G:1 P:2 D:2 P:1 G:1 R:3", sa)   # the pilot's tune (Bhoopali)
    play(m, pilot, 1.1, S.bansuri, start=19.0, gain=0.8, pan=-0.1, breath=0.2)
    play(m, echo(pilot, 3.0, 2.0), 1.1, S.veena, start=19.0, gain=0.2, pan=0.4)
    play(m, pilot, 1.1, S.sarod, start=42.0, gain=0.45, pan=0.2)
    m.add(S.voice(sa, 8.0, 'a', attack=2.5, release=3.0), 44.0, 0.35)
    m.add(S.bowl(sa * 2, 8.0), 55.0, 0.4)
    m.reverb(0.4, 3.6)
    S.save(os.path.join(OUT, 'title.ogg'), m.loop_fold(), quality=4)
    print('title ok')


def menu():
    sa = LOKA_SA[0]
    m = S.Mix(LOOP)
    tanpura_bed(m, sa, LOOP, gain=0.5)
    jt = parse("S:4 G:4 P:4 G:2 R:2 S:8 | R:4 G:4 D:4 P:2 G:2 R:8 | S:2 R:2 G:4 P:4 D:4 S':8 | D:4 P:4 G:4 R:4 S:8", sa)
    play(m, jt, 1.0, S.jaltarang, start=2.0, gain=0.3, pan=0.3)
    m.reverb(0.5, 3.8)
    S.save(os.path.join(OUT, 'menu.ogg'), m.loop_fold(), quality=4)
    print('menu ok')


def rest(name, sa, text, tala_beats):
    """A lamp or a bay: a veena alone over the drone, unhurried."""
    m = S.Mix(LOOP)
    tanpura_bed(m, sa, LOOP, gain=0.5)
    notes = parse(text, sa)
    total = max(n.start + n.dur for n in notes)
    play(m, notes, LOOP / total, S.veena, gain=0.8, pan=0.05, decay=3.0)
    m.reverb(0.45, 3.4)
    S.save(os.path.join(OUT, name + '.ogg'), m.loop_fold(), quality=4)
    print('rest', name, 'ok')


def stings():
    sa = LOKA_SA[0]
    # the Seven: the nth swara of the scale of Yaman, a struck bowl with a halo of voice; the seventh completes the scale
    scale = ['S', 'R', 'G', 'M', 'P', 'D', 'N']
    from raga import hz
    for i, n in enumerate(scale):
        m = S.Mix(4.0)
        f = hz(n, 1, sa)
        m.add(S.bowl(f, 4.0), 0.0, 0.9)
        m.add(S.ghanta(f * 2, 3.0), 0.0, 0.35)
        m.add(S.voice(f * 0.5, 3.0, 'a', attack=0.5, release=1.5), 0.2, 0.3)
        m.reverb(0.4, 2.8)
        S.save(os.path.join(OUT, 'piece_%d.ogg' % (i + 1)), m.out(), quality=5)
    m = S.Mix(9.0)
    for i, n in enumerate(scale + ['S']):
        f = hz(n, 1 if n != 'S' or i == 0 else 2, sa) if i < 7 else hz('S', 2, sa)
        m.add(S.bowl(f, 5.0), i * 0.45, 0.8)
        m.add(S.ghanta(f * 2, 3.0), i * 0.45, 0.3)
    m.add(S.morph_voice(sa, 5.0, ['o', 'm'], attack=1.0, release=2.0), 3.2, 0.5)
    m.add(S.gong(sa, 6.0), 3.6, 0.5)
    m.reverb(0.4, 3.2)
    S.save(os.path.join(OUT, 'piece_all.ogg'), m.out(), quality=5)

    # death: the drone detunes a quarter-tone down, one sarangi note held, then silence
    m = S.Mix(5.0)
    for i, f in enumerate([sa * 0.75, sa, sa, sa * 0.5]):
        m.add(S.tanpura(f * 2.0 ** (-0.5 / 12.0), 5.0), i * 0.3, 0.5)
    m.add(S.sarangi([sa * 2, sa * 2, sa * 2 * 0.98, sa * 2 * 0.96], 4.0, attack=0.4, release=2.0), 0.2, 0.8)
    m.reverb(0.5, 3.0)
    S.save(os.path.join(OUT, 'death.ogg'), m.out(), quality=5)
    # respawn: a single call of the conch
    m = S.Mix(3.5)
    m.add(S.shankha(sa * 2, 3.0), 0.0, 0.8)
    m.add(S.tanpura(sa, 4.0), 0.5, 0.4)
    m.reverb(0.35, 2.6)
    S.save(os.path.join(OUT, 'respawn.ogg'), m.out(), quality=5)
    # a new loka: the tonic slides down a semitone over six seconds under the new raga's first phrase
    for a, b in [(0, 1)]:
        m = S.Mix(9.0)
        n = S.n_of(8.0)
        fcur = np.linspace(LOKA_SA[a], LOKA_SA[b], n)
        m.add(S.sarangi(fcur * 0.5, 8.0, vib=0.002, attack=1.5, release=2.0), 0.0, 0.6)
        m.add(S.bowl(LOKA_SA[b], 6.0), 5.0, 0.5)
        m.add(S.shankha(LOKA_SA[b], 3.0), 0.0, 0.3)
        m.reverb(0.45, 3.4)
        S.save(os.path.join(OUT, 'descend_%d.ogg' % (a + 1)), m.out(), quality=5)
    # a ruler yields: a sarangi weeping, a low horn answering (Indra); the second, a damaru that stops and one bell
    m = S.Mix(30.0)
    weep = parse("S':2~ N:1 D:1 P:2 G:2~ R:2 S:4 | _:2 R:2 G:2~ M:2 P:3~ G:2 R:2 S:6", sa)
    play(m, weep, 1.1, S.sarangi, gain=0.8, pan=-0.2, vib=0.007)
    play(m, echo(weep, 3.0, 0.5), 1.1, S.shankha, gain=0.2, pan=0.3) if False else None
    m.add(S.shankha(sa, 4.0), 6.0, 0.4)
    m.add(S.shankha(sa * 1.5, 4.0), 16.0, 0.4)
    tanpura_bed(m, sa, 30.0, gain=0.5)
    m.reverb(0.45, 3.4)
    S.save(os.path.join(OUT, 'yield_atala.ogg'), m.out(), quality=5)
    sc = LOKA_SA[1]
    m = S.Mix(20.0)
    for i in range(14):
        m.add(S.damaru(2, 0.07, 170), i * 0.9 * (1.0 + i * 0.03), 0.6 * (1 - i / 16.0))
    m.add(S.overtone_voice(sc * 0.5, 16.0, 1200.0), 3.0, 0.35)
    m.add(S.ghanta(sc * 2, 4.0), 15.0, 0.5)
    m.reverb(0.45, 3.2)
    S.save(os.path.join(OUT, 'yield_vitala.ogg'), m.out(), quality=5)
    print('stings ok')


def opening():
    """Indra's court: a long Yaman opening in the nadaswaram's place (bowed and bright), a vow, the broken bolt: one stroke and ringing silence."""
    sa = LOKA_SA[0]
    m = S.Mix(75.0, tail=5.0)
    tanpura_bed(m, sa, 75.0, gain=0.5)
    court = parse("S:2 G:2 P:4 M:2 G:2 R:4 | G:2 P:2 N:2 S':2 R':4 S':2 N:2 D:2 P:4 | P:2 M:2 G:2 R:2 S:8", sa)
    play(m, court, 1.2, S.sarangi, start=2.0, gain=0.7, pan=-0.1, vib=0.006)
    play(m, court, 1.2, S.shankha, start=2.0, gain=0.14, pan=0.2)
    for i in range(6):
        m.add(S.pakhawaj(sa * 0.55, 1.5), 4.0 + i * 9.6, 0.7)
    m.add(S.voice(sa * 0.5, 10.0, 'a', attack=3.0, release=3.0), 20.0, 0.4)
    m.add(S.gong(sa * 0.5, 6.0), 60.0, 0.9)                   # the stroke
    m.add(S.ghanta(sa * 2, 5.0), 60.0, 0.5)
    pilot = parse("S:1 R:1 G:2 P:2 G:1 R:1 S:4", sa)
    play(m, pilot, 1.2, S.bansuri, start=64.0, gain=0.8, pan=0.0, breath=0.22)
    m.reverb(0.45, 3.6)
    S.save(os.path.join(OUT, 'opening.ogg'), m.out(), quality=4)
    print('opening ok')


REST_ATALA = "S:2 R:2 G:4 | M:2 P:2 G:4 | R:2 G:2 R:2 S:2 | N.:2 R:2 S:4 | G:2 M:2 P:4 | D:2 P:2 M:2 G:2 | R:2 G:2 R:2 N.:2 | S:8 _:8"
REST_VITALA = "S:2 r:2 G:4 | m:2 P:2 G:4 | r:2 G:2 r:2 S:2 | N.:2 d.:2 P.:4 | G:2 m:2 P:4 | d:2 P:2 m:2 G:2 | r:2 G:2 r:2 S:2 | S:8 _:8"


TARGETS = {
    'title': title,
    'menu': menu,
    'opening': opening,
    'atala': lambda: explore('atala', LOKA_SA[0], 'teental', ATALA),
    'vitala': lambda: explore('vitala', LOKA_SA[1], 'ektal', VITALA),
    'bala': lambda: [ruler('bala_1', LOKA_SA[0], 'chautal', BALA_P1, S.sarangi, vowel='a'),
                     ruler('bala_2', LOKA_SA[0], 'jhaptal', BALA_P2, S.sarangi, vowel='a'),
                     ruler('bala_3', LOKA_SA[0], 'rupak', BALA_P3, S.sarangi, vowel='a')],
    'bhairava': lambda: ruler('bhairava', LOKA_SA[1], 'dhamar', BHAIRAVA, S.sarangi, vowel='o'),
    'rest': lambda: [rest('rest_atala', LOKA_SA[0], REST_ATALA, 16), rest('rest_vitala', LOKA_SA[1], REST_VITALA, 12)],
    'stings': stings,
}

if __name__ == '__main__':
    todo = sys.argv[1:] or list(TARGETS)
    os.makedirs(OUT, exist_ok=True)
    for k in todo:
        TARGETS[k]()
