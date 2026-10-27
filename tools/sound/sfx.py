"""THE SOUND EFFECTS, each a written recipe of the instruments in synth.py (docs/SOUND_PLAN.md sections 5-14). Tuned to Atala's Sa so combat is harmonic with the music.

Run from the repo root:  python tools/sound/sfx.py [prefix ...]      (no prefix = everything)
Writes assets/audio/sfx/<name>.ogg (mono).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import synth as S
from raga import LOKA_SA, RATIO

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'audio', 'sfx')
SA = LOKA_SA[0] * 2.0          # D4: the pitch the effects are tuned to
REG = {}


def sfx(name):
    def deco(fn):
        REG[name] = fn
        return fn
    return deco


def deg(name, octave=0):
    return SA * RATIO[name] * 2.0 ** octave


def seq(parts, total=None):
    """parts: [(array, start_seconds, gain)] -> one array."""
    n = max(int((s * S.SR)) + len(x) for x, s, g in parts)
    out = np.zeros(n)
    for x, s, g in parts:
        i = int(s * S.SR)
        out[i:i + len(x)] += x * g
    return out


def swoosh(dur, f0, f1, level=1.0, q=0.7, seed=5):
    """Noise carried across a sweeping band: a thrown rope, a gale, a wing."""
    n = S.n_of(dur)
    t = np.arange(n) / S.SR
    fc = np.geomspace(f0, f1, n)
    ph = S.phase_of(fc)
    x = S.noise(n, seed) * np.sin(ph) + 0.5 * S.noise(n, seed + 1) * np.sin(ph * 0.5)
    x = S.lowpass(x, max(f0, f1) * 1.6) if max(f0, f1) < 8000 else x
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** (1.0 / q)
    return S.Sig(x * env * level)


def click(n_ms=6, bright=4000.0, seed=2):
    n = S.n_of(n_ms / 1000.0)
    t = np.arange(n) / S.SR
    return S.Sig(S.highpass(S.noise(n, seed) * np.exp(-t / (n_ms / 3000.0)), bright) * 0.5)


def tap(f, d=0.35):
    """A temple bell struck with a metal rod: short, bright."""
    return S.ghanta(f, d)[:S.n_of(d)] + click(5, 5000) * 0.4


def fade_out(x, ms=20):
    n = min(len(x), S.n_of(ms / 1000.0))
    x = x.copy()
    x[len(x) - n:] *= np.linspace(1, 0, n)
    return x


def rev(x):
    return x[::-1].copy()


# ============================================================================================================================ WEAPONS (fire)
W = {}


def w(i):
    def deco(fn):
        W[i] = fn
        return fn
    return deco


@w(0)
def _w0(p): return tap(deg('S', 1) * p, 0.32)
@w(1)
def _w1(p): return seq([(tap(deg('S', 1) * p, 0.3), 0, 1), (tap(deg('P', 1) * p, 0.3), 0.045, 0.9)])
@w(2)
def _w2(p): return seq([(tap(deg(n, 1) * p, 0.25), i * 0.05, 1.0) for i, n in enumerate('SRG')])
@w(3)
def _w3(p): return seq([(tap(deg(n, 1) * p, 0.25), (i // 2) * 0.09 + (i % 2) * 0.012, 0.9) for i, n in enumerate(['S', 'P', 'R', 'D'])])
@w(4)
def _w4(p): return seq([(S.cymbal(0.08, 5000, 0.03, 11), 0, 1.0), (S.kartal(0.1, 12), 0.01, 0.8), (S.membrane(180 * p, 0.12, 1.4, 0.04, 0.8, 0, 13), 0, 0.6)])
@w(5)
def _w5(p): return seq([(S.pakhawaj(70 * p, 0.55, 0.5), 0, 1.0), (S.ghanta(deg('S', 0) * p, 0.7), 0, 0.35)])
@w(6)
def _w6(p): return S.kartal(0.07, 14) * 1.2 + S.tabla('tak') * 0.3
@w(7)
def _w7(p): return seq([(S.cymbal(0.5, 6000, 0.18, 15), 0, 0.6), (S.nagara(80 * p, 0.6), 0, 0.9), (S.bell(deg('P', 1) * p, 0.8), 0, 0.5)])
@w(8)
def _w8(p):
    n = S.n_of(0.7)
    f = np.linspace(deg('S', 1) * 1.5, deg('S', 1) * 1.52, n) * p
    return S.sarangi(f, 0.7, vib=0.001, attack=0.03, release=0.3, rosin=0.3) * 1.2 + swoosh(0.7, 3000, 7000, 0.3)
@w(9)
def _w9(p): return seq([(S.gong(deg('S', -1) * p, 1.0), 0, 0.8), (swoosh(0.5, 900, 300, 0.3), 0.05, 1.0)])
@w(10)
def _w10(p): return seq([(swoosh(0.35, 400, 1800, 0.7), 0, 1.0), (S.membrane(120 * p, 0.25, 1.4, 0.07, 0.7, 0.1, 17), 0, 0.7)])
@w(11)
def _w11(p):
    a = np.stack([swoosh(0.35, 400, 1800, 0.7, seed=3), swoosh(0.35, 450, 2000, 0.7, seed=4)], 1)
    return a.mean(axis=1) + S.bell(deg('R', 2) * p, 0.4) * 0.25
@w(12)
def _w12(p): return S.bell(deg('S', 2) * p, 0.45) * 0.9 + S.bell(deg('P', 2) * p, 0.3) * 0.25
@w(13)
def _w13(p): return S.sarod(deg('N', 2) * p, 0.25, 0.14) + click(3, 7000) * 0.5
@w(14)
def _w14(p): return S.sarangi(deg('S', -1) * p, 0.4, vib=0.0, attack=0.02, release=0.2, rosin=0.9) + S.lowpass(S.noise(S.n_of(0.4), 21), 600) * 0.4
@w(15)
def _w15(p):
    a = tap(deg('R', 1) * p, 0.3)
    return seq([(a, 0, 1.0), (rev(a), 0.04, 0.55)])
@w(16)
def _w16(p): return S.bansuri(np.linspace(deg('S', 2), deg('P', 2), 40) * p, 0.4, breath=0.9, attack=0.03, release=0.2) * 0.9 + swoosh(0.4, 2500, 5000, 0.2)
@w(17)
def _w17(p): return seq([(S.santoor(deg('G', 1) * p, 0.6), 0, 0.8), (S.santoor(deg('D', 1) * p, 0.6), 0.04, 0.7)])
@w(18)
def _w18(p): return seq([(S.damaru(1, 0.05, 220), 0, 0.8), (S.highpass(S.noise(S.n_of(0.15), 31) * np.exp(-np.arange(S.n_of(0.15)) / (0.04 * S.SR)), 2500), 0, 0.5)])
@w(19)
def _w19(p): return S.bandpass(S.noise(S.n_of(0.2), 33), 1500, 7000) * np.hanning(S.n_of(0.2)) * 0.8
@w(20)
def _w20(p): return S.membrane(95 * p, 0.35, 1.5, 0.12, 0.6, 0.1, 35)
@w(21)
def _w21(p): return seq([(S.jaltarang(deg('R', 1) * p, 0.5), 0, 0.9), (S.jaltarang(deg('G', 2) * p, 0.4), 0.06, 0.4)])
@w(22)
def _w22(p): return seq([(S.bowl(deg('S', 1) * p, 0.9), 0, 0.7)] + [(S.jaltarang(deg(n, 2) * p, 0.3), 0.04 + i * 0.045, 0.3) for i, n in enumerate('SRGPD')])
@w(23)
def _w23(p): return S.veena(deg('D', 1) * p, 0.5, 0.5) * 0.9
@w(24)
def _w24(p): return S.bell(deg('G', 2) * p, 0.4, [(1, 1, 1), (2.76, 0.5, 0.5), (5.4, 0.3, 0.3)]) * 0.9 + click(4, 6000) * 0.4
@w(25)
def _w25(p): return S.tabla('tin', deg('P', 1) * p) * 0.9 + swoosh(0.12, 5000, 9000, 0.25)
@w(26)
def _w26(p): return S.bowl(deg('S', 1) * p, 0.5) * 0.8 + S.ghanta(deg('P', 1) * p, 0.25) * 0.4
@w(27)
def _w27(p): return S.sarangi(np.linspace(deg('S', 0), deg('G', 0), 30) * p, 0.45, vib=0.002, attack=0.05, release=0.2) * 0.8 + swoosh(0.4, 4000, 6000, 0.25)
@w(28)
def _w28(p): return S.highpass(S.noise(S.n_of(0.18), 41), 4500) * np.hanning(S.n_of(0.18)) * 0.6 + S.sarod(deg('S', 3) * p, 0.1, 0.1) * 0.3
@w(29)
def _w29(p): return S.bandpass(S.noise(S.n_of(0.4), 43), 600, 3000) * np.hanning(S.n_of(0.4)) * 0.7 + S.bowl(deg('S', 0) * p, 0.4) * 0.3
@w(30)
def _w30(p): return S.overtone_voice(np.linspace(110, 220, 30) * p, 0.7, 900.0) * 1.4
@w(31)
def _w31(p): return seq([(S.ghatam(180 * p, 0.4), 0, 1.0), (S.pakhawaj(65 * p, 0.6, 0.2), 0, 0.9)])
@w(32)
def _w32(p): return seq([(S.ghatam(300 * p, 0.3), 0, 0.9), (S.cymbal(0.15, 7000, 0.05, 45), 0.03, 0.4)])
@w(33)
def _w33(p): return S.ghanta(deg('S', 3) * p, 0.7) * 0.7 + S.bell(deg('P', 3) * p, 0.5) * 0.3
@w(34)
def _w34(p): return seq([(S.gong(deg('S', -1) * p, 1.2), 0.03, 1.0), (S.membrane(60, 0.4, 1.3, 0.2, 0.8, 0.1, 47), 0.03, 0.8)])
@w(35)
def _w35(p):
    return seq([(S.tanpura(deg('S', -1) * p, 1.4), 0, 0.5), (S.voice(deg('S', -2) * p, 1.2, 'a', attack=0.6, release=0.5), 0, 0.5), (S.bowl(deg('S', 1) * p, 1.2), 0.2, 0.4)])


for _i, _fn in W.items():
    def _mk(i=_i, fn=_fn):
        return lambda: [fn(1.0), fn(1.035)]
    REG['w%d_a' % _i] = (lambda i=_i, fn=_fn: fn(1.0))
    REG['w%d_b' % _i] = (lambda i=_i, fn=_fn: fn(1.035))


# ============================================================================================================================ IMPACTS (by material)
@sfx('hit_tick')
def _hit_tick(): return click(8, 4500, 51) * 2 + S.ghanta(deg('S', 3), 0.1) * 0.2
@sfx('hit_bell')
def _hit_bell(): return S.bell(deg('P', 3), 0.5) * 0.7 + click(5, 6000) * 0.3
@sfx('hit_thud')
def _hit_thud(): return S.membrane(110, 0.25, 1.4, 0.08, 0.7, 0.1, 53)
@sfx('hit_wet')
def _hit_wet(): return S.jaltarang(deg('S', 3), 0.25) * 0.6 + S.bandpass(S.noise(S.n_of(0.1), 55), 600, 2500) * np.hanning(S.n_of(0.1)) * 0.5
@sfx('hit_glass')
def _hit_glass(): return S.santoor(deg('G', 3), 0.3) * 0.6 + S.cymbal(0.1, 8000, 0.04, 57) * 0.5
@sfx('hit_stone')
def _hit_stone(): return S.ghatam(260, 0.18) * 0.8 + S.lowpass(S.noise(S.n_of(0.1), 59), 900) * 0.3
@sfx('hit_burn')
def _hit_burn(): return S.damaru(1, 0.04, 200) * 0.4 + S.highpass(S.noise(S.n_of(0.25), 61) * np.exp(-np.arange(S.n_of(0.25)) / (0.07 * S.SR)), 2000) * 0.6
@sfx('boom_small')
def _boom_small(): return seq([(S.nagara(80, 0.8), 0, 0.9), (S.cymbal(0.4, 4000, 0.12, 63), 0, 0.4)])
@sfx('boom_big')
def _boom_big(): return seq([(S.gong(deg('S', -2), 2.0), 0, 0.8), (S.nagara(60, 1.4), 0, 1.0), (S.cymbal(0.8, 3500, 0.3, 65), 0, 0.5)])
@sfx('charge_loop')
def _charge_loop():
    n = S.n_of(1.2)
    return S.overtone_voice(np.linspace(deg('S', -1), deg('S', 0), 20), 1.2, 1200.0) * 1.2 + S.bowl(deg('S', 1), 1.2) * 0.2
@sfx('mine_arm')
def _mine_arm(): return click(4, 3000) * 1.2 + S.ghanta(deg('R', 3), 0.1) * 0.3
@sfx('burn_loop')
def _burn_loop():
    n = S.n_of(1.0)
    x = S.highpass(S.noise(n, 67), 1500) * (0.5 + 0.5 * np.abs(S.noise(n, 68)))
    return S.lowpass(x, 5000) * 0.4
@sfx('freeze')
def _freeze(): return S.bell(deg('S', 4), 0.6) * 0.5 + click(3, 8000) * 0.5
@sfx('execute_kill')
def _execute_kill(): return S.membrane(55, 0.6, 1.5, 0.28, 0.7, 0.2, 69) + S.gong(deg('S', -1), 1.2) * 0.4


# ============================================================================================================================ THE PILOT'S HULL
@sfx('hull_hit_light')
def _hhl(): return seq([(S.ghatam(240, 0.2), 0, 0.9), (S.highpass(S.noise(S.n_of(0.12), 71) * np.exp(-np.arange(S.n_of(0.12)) / (0.03 * S.SR)), 800), 0, 0.5)])
@sfx('hull_hit_heavy')
def _hhh(): return seq([(S.pakhawaj(65, 0.6, 0.1), 0, 1.0), (S.ghatam(150, 0.35), 0, 0.8), (S.cymbal(0.3, 3000, 0.1, 73), 0, 0.4)])
@sfx('shield_up')
def _shu(): return S.bowl(deg('P', 0), 1.0) * 0.9 + S.ghanta(deg('P', 2), 0.5) * 0.3
@sfx('shield_loop')
def _shl():
    t = np.arange(S.n_of(1.0)) / S.SR
    return (np.sin(S.TAU * deg('P', 0) * t) + np.sin(S.TAU * deg('P', 0) * 1.012 * t)) * 0.3
@sfx('shield_hit')
def _shh(): return S.gong(deg('P', 1), 0.8) * 0.9
@sfx('shield_down')
def _shd(): return S.bowl(deg('S', 0), 0.8) * 0.6
@sfx('boost')
def _boost(): return seq([(S.shankha(deg('S', 0), 0.9), 0, 0.6), (swoosh(0.7, 500, 3000, 0.6), 0, 1.0)])
@sfx('repair')
def _repair(): return seq([(S.veena(np.linspace(deg('S', 1), deg('S', 2), 30), 0.8, 0.9), 0, 0.8), (S.jaltarang(deg('P', 2), 0.5), 0.35, 0.5)])
@sfx('death_burst')
def _dburst(): return seq([(S.ghatam(200, 0.5), 0, 0.9), (S.cymbal(1.0, 3500, 0.35, 75), 0, 0.6), (S.pakhawaj(55, 1.0, 0.1), 0, 0.9)])
@sfx('heartbeat')
def _hb(): return seq([(S.damaru(1, 0.0, 120), 0, 0.9), (S.damaru(1, 0.0, 100), 0.22, 0.6)])
@sfx('ram')
def _ram(): return seq([(S.pakhawaj(90, 0.4, 0.1), 0, 0.8), (S.ghatam(200, 0.25), 0.02, 0.6)])
@sfx('astra_locked')
def _alock(): return seq([(S.membrane(140, 0.2, 1.1, 0.07, 0.8, 0, 77), 0, 0.9), (S.sarangi(deg('S', -1) * 0.9, 0.3, attack=0.02, release=0.2), 0.05, 0.4)])
@sfx('engine_loop')
def _engine():
    n = S.n_of(2.0)
    t = np.arange(n) / S.SR
    f = deg('S', -2)
    x = np.zeros(n)
    for h in range(1, 14):
        x += np.sin(S.TAU * f * h * t + h) / h ** 1.1
    x += 0.25 * S.lowpass(S.noise(n, 79), 700)
    return (x * 0.25 * (1.0 + 0.08 * np.sin(S.TAU * 3 * t)))
@sfx('mud_loop')
def _mud():
    n = S.n_of(1.5)
    return S.lowpass(S.noise(n, 81), 400) * 0.6


# ============================================================================================================================ FOES
KINDS = ['mayavi', 'hataka', 'auris', 'lance', 'vakra', 'bhavi', 'bala', 'dvarapala', 'dipaka', 'vanija']


@sfx('foe_mayavi_fire')
def _fm(): return S.bell(deg('G', 2), 0.45) * 0.7 + swoosh(0.4, 500, 300, 0.4)
@sfx('foe_hataka_fire')
def _fh(): return S.bell(deg('N', 3), 0.3, [(1, 1, 1), (2.9, 0.6, 0.5)]) * 0.5 + click(4, 7000) * 0.3
@sfx('foe_auris_fire')
def _fa(): return S.tabla('tak') * 1.0 + S.tabla('tin', 600) * 0.3
@sfx('foe_lance_fire')
def _fl(): return S.ghatam(420, 0.18) * 0.8
@sfx('foe_vakra_fire')
def _fv(): return seq([(S.kartal(0.05, 83 + i), i * 0.05, 0.7) for i in range(3)])
@sfx('foe_bhavi_fire')
def _fb(): return S.membrane(90, 0.4, 1.3, 0.15, 0.6, 0.3, 85) + S.ghanta(deg('S', 1), 0.4) * 0.15
@sfx('foe_dvarapala_fire')
def _fd(): return seq([(S.shankha(deg('S', -1), 0.6), 0, 0.4), (S.pakhawaj(60, 0.5, 0.2), 0, 0.9)])
@sfx('foe_dipaka_fire')
def _fdi(): return click(4, 5000) * 1.0 + S.ghanta(deg('P', 3), 0.15) * 0.3
@sfx('foe_vanija_fire')
def _fvn(): return S.tabla('na', 400) * 0.7 + S.kartal(0.06, 87) * 0.8 + S.ghanta(deg('S', 3), 0.1) * 0.2
@sfx('foe_bala_fire')
def _fbala(): return S.pakhawaj(70, 0.5, 0.4) * 0.9 + S.sarangi(deg('S', 0), 0.35, attack=0.02, release=0.2) * 0.4
for _k in KINDS:
    REG['foe_%s_die' % _k] = (lambda k=_k: seq([(S.ghatam(150 + 40 * KINDS.index(k), 0.4), 0, 0.9), (S.cymbal(0.3, 4500, 0.1, 90 + KINDS.index(k)), 0, 0.4), (S.membrane(80, 0.3, 1.4, 0.1, 0.5, 0.1, 100 + KINDS.index(k)), 0, 0.5)]))
    REG['foe_%s_wake' % _k] = (lambda k=_k: S.ghanta(deg('S', 2) * 2.0 ** (KINDS.index(k) / 12.0), 0.5) * 0.4 + S.kartal(0.08, 120 + KINDS.index(k)) * 0.4)
for _i in range(3):
    REG['foe_hurt_%d' % _i] = (lambda i=_i: S.ghatam(300 + 60 * i, 0.15) * 0.8 + click(4, 3500, 130 + i) * 0.3)
@sfx('foe_hurt_armour')
def _fha(): return S.gong(deg('S', 1), 0.4) * 0.5 + S.membrane(220, 0.12, 1.1, 0.04, 0.5, 0.4, 135) * 0.6
@sfx('foe_mend')
def _fmend(): return S.veena(deg('P', 2), 0.5, 0.6) * 0.6 + S.jaltarang(deg('S', 3), 0.5) * 0.4
@sfx('foe_dodge')
def _fdodge(): return swoosh(0.18, 1500, 4000, 0.8)
@sfx('foe_dive')
def _fdive(): return S.jaltarang(deg('S', 2), 0.3) * 0.4 + S.lowpass(S.noise(S.n_of(0.3), 137), 800) * np.hanning(S.n_of(0.3)) * 0.4
@sfx('foe_surface')
def _fsurf(): return S.jaltarang(deg('P', 3), 0.4) * 0.5 + swoosh(0.25, 300, 1500, 0.4)
@sfx('betray')
def _betray(): return S.bell(deg('N', 3), 0.8, [(1, 1, 1), (2.9, 0.7, 0.6), (4.2, 0.5, 0.4)]) * 0.7 + S.cymbal(0.4, 6000, 0.12, 139) * 0.5
@sfx('alert_ring')
def _alert(): return seq([(S.ghanta(deg('S', 2), 1.0), 0, 0.5), (S.nagara(90, 0.7), 0.1, 0.7), (S.nagara(90, 0.7), 0.4, 0.6)])


# ============================================================================================================================ THE RULERS
@sfx('boss_telegraph')
def _bt():
    n = S.n_of(0.6)
    return S.sarangi(np.linspace(deg('M', 0), deg('P', 1), 30), 0.6, vib=0.01, attack=0.2, release=0.1) * 0.8
@sfx('boss_hit')
def _bh(): return S.gong(deg('S', 0), 0.5) * 0.4 + S.membrane(100, 0.2, 1.3, 0.07, 0.6, 0.1, 141) * 0.6
@sfx('boss_wake')
def _bwake(): return seq([(S.pakhawaj(55, 1.4, 0.3), 0, 1.0), (S.voice(deg('S', -2), 2.0, 'a', attack=0.5, release=1.0), 0.4, 0.6), (S.gong(deg('S', -2), 3.0), 0.2, 0.5)])
@sfx('maya_forge')
def _mforge():
    n = S.n_of(4.0)
    x = S.bandpass(S.noise(n, 143), 2000, 9000) * (np.arange(n) / n) ** 2
    return rev(S.bell(deg('S', 3), 2.0) * 0.5)[:n] if False else (x * 0.5 + S.sarangi(np.linspace(deg('S', 1), deg('S', 2), 60), 4.0, vib=0.01, attack=3.0, release=0.5) * 0.4)
@sfx('mirror_shatter')
def _shatter():
    g = np.random.default_rng(145)
    parts = []
    for i in range(46):
        t = g.uniform(0, 1.4) ** 1.4
        f = g.uniform(1800, 7500)
        parts.append((S.santoor(f, 0.9) * 0.4 * (1.0 - t / 2.0), t, 1.0))
        parts.append((S.cymbal(0.12, g.uniform(5000, 9000), 0.04, 146 + i), t, 0.4))
    parts.append((S.cymbal(1.4, 4000, 0.5, 147), 0, 0.9))
    parts.append((S.pakhawaj(60, 1.0, 0.1), 0, 0.8))
    return seq(parts)
@sfx('mirror_turn')
def _mturn(): return S.bell(deg('G', 3), 0.9) * 0.6 + swoosh(0.35, 800, 3000, 0.3)
@sfx('door_open')
def _door(): return seq([(S.gong(deg('S', -2), 4.0), 0, 0.9), (S.lowpass(S.noise(S.n_of(2.0), 149), 400) * np.hanning(S.n_of(2.0)), 0, 0.7)])
@sfx('ash_loop')
def _ash():
    n = S.n_of(3.0)
    x = S.bandpass(S.noise(n, 151), 150, 1800) * (0.6 + 0.4 * np.sin(S.TAU * 0.3 * np.arange(n) / S.SR))
    return x * 0.8
@sfx('ash_start')
def _ashs(): return S.lowpass(S.noise(S.n_of(2.0), 153), 900) * np.hanning(S.n_of(2.0)) * 0.9 + S.ghanta(deg('S', -1), 3.0)[:S.n_of(2.0)] * 0.3
@sfx('yield_piece_fly')
def _ypf():
    n = S.n_of(2.5)
    t = np.arange(n) / S.SR
    return (S.voice(np.linspace(deg('S', 0), deg('S', 2), 30), 2.5, 'a', attack=1.0, release=0.8) * 0.5 + S.bowl(deg('S', 2), 2.5) * 0.4)


# ============================================================================================================================ ASTRAS
ASTRA = ['akasha', 'vayu', 'agni', 'jala', 'prithvi', 'brahma', 'pashupata']
BIJA = {'akasha': ('h', 'a', 'm'), 'vayu': ('y', 'a', 'm'), 'agni': ('r', 'a', 'm'), 'jala': ('v', 'a', 'm'), 'prithvi': ('l', 'a', 'm'), 'brahma': ('', 'o', 'm'), 'pashupata': ('r', 'u', 'm')}


def syllable(f, dur, cons, vowel, end):
    """One chanted bija: a consonant onset, the vowel held, the nasal close."""
    v = S.morph_voice(f, dur, [vowel, end], attack=0.05, release=0.12, vib=0.003)
    n = len(v)
    onset = S.n_of(0.09)
    c = np.zeros(n)
    if cons == 'h':
        c[:onset] = S.bandpass(S.noise(onset, 155), 600, 3500) * np.linspace(1, 0, onset) * 0.8
    elif cons == 'r':
        t = np.arange(onset) / S.SR
        c[:onset] = S.lowpass(S.noise(onset, 156), 700) * (0.6 + 0.4 * np.sin(S.TAU * 28 * t)) * 0.7
    elif cons in ('l', 'v', 'y'):
        c[:onset] = S.voice(f, 0.09, 'u' if cons != 'y' else 'i', attack=0.01, release=0.04)[:onset] * 0.7
    return v + c


def chant_loop(el, f):
    cons, vowel, end = BIJA[el]
    s = syllable(f, 0.62, cons, vowel, end)
    gap = np.zeros(S.n_of(0.08))
    return np.concatenate([s, gap])


for _i, _el in enumerate(ASTRA):
    base = {'akasha': deg('S', -1), 'vayu': deg('P', -2), 'agni': deg('S', -1) * 1.0, 'jala': deg('S', -1), 'prithvi': deg('S', -2), 'brahma': deg('S', -2), 'pashupata': deg('S', -2)}[_el]
    REG['astra_%d_chant' % _i] = (lambda el=_el, b=base: chant_loop(el, b))


@sfx('astra_0_invoke')
def _a0i(): return S.overtone_voice(np.linspace(deg('S', -1), deg('S', 0), 30), 2.0, 1300.0) * 0.8 + S.bowl(deg('S', 1), 2.0) * 0.3
@sfx('astra_1_invoke')
def _a1i(): return S.bansuri(np.linspace(deg('P', 1), deg('P', 2), 40), 2.0, breath=0.9, attack=0.5, release=0.3) * 0.6 + swoosh(2.0, 500, 3500, 0.35)
@sfx('astra_2_invoke')
def _a2i(): return seq([(S.damaru(2, 0.07, 170), i * (0.5 - 0.03 * i), 0.5 + 0.05 * i) for i in range(10)]) + S.highpass(S.noise(S.n_of(4.0), 157), 2500)[:0].sum() * 0
@sfx('astra_3_invoke')
def _a3i(): return seq([(S.jaltarang(deg('R', 2) * (1 + 0.1 * (i % 3)), 0.4), i * 0.18, 0.5) for i in range(14)])
@sfx('astra_4_invoke')
def _a4i(): return seq([(S.ghatam(150 + 10 * (i % 2), 0.3), i * 0.3, 0.7) for i in range(8)] + [(S.pakhawaj(60, 0.5, 0.2), i * 0.6, 0.6) for i in range(4)])
@sfx('astra_5_invoke')
def _a5i(): return S.morph_voice(deg('S', -2), 3.0, ['o', 'm'], attack=1.0, release=0.5) * 0.6 + S.shankha(deg('S', 0), 3.0) * 0.3
@sfx('astra_6_invoke')
def _a6i(): return S.overtone_voice(deg('S', -2), 3.0, 700.0) * 0.8 + S.damaru(2, 0.07, 150) * 0.4
@sfx('astra_0_loose')
def _a0l(): return seq([(S.bowl(deg('S', 1), 3.0), 0, 0.8), (S.cymbal(1.0, 5000, 0.4, 159), 0, 0.5), (S.ghanta(deg('P', 2), 2.0), 0, 0.4)])
@sfx('astra_1_loose')
def _a1l(): return seq([(swoosh(1.2, 400, 6000, 1.0), 0, 1.0), (S.shankha(deg('P', 1), 1.2), 0, 0.4)])
@sfx('astra_2_loose')
def _a2l(): return seq([(S.nagara(70, 1.6), 0, 1.0), (S.highpass(S.noise(S.n_of(2.5), 161), 400) * np.hanning(S.n_of(2.5)), 0, 0.6), (S.cymbal(1.5, 4000, 0.6, 162), 0, 0.5)])
@sfx('astra_3_loose')
def _a3l(): return seq([(S.gong(deg('S', -1), 3.0), 0, 0.9), (S.lowpass(S.noise(S.n_of(3.0), 163), 1500) * np.hanning(S.n_of(3.0)), 0, 0.8), (S.santoor(deg('S', 3), 1.0), 0.2, 0.5)])
@sfx('astra_4_loose')
def _a4l(): return seq([(S.pakhawaj(50, 1.2, 0.2), 0, 1.0), (S.gong(deg('S', -2), 3.0), 0, 0.7), (S.lowpass(S.noise(S.n_of(2.0), 165), 500) * np.hanning(S.n_of(2.0)), 0.1, 0.8)])
@sfx('astra_5_loose')
def _a5l(): return seq([(S.morph_voice(deg('S', -1), 4.0, ['o', 'm'], attack=0.1, release=2.0), 0.3, 0.9), (S.cymbal(3.0, 3500, 1.2, 167), 0.3, 0.7), (S.gong(deg('S', -1), 5.0), 0.3, 0.8), (S.shankha(deg('S', 1), 3.0), 0.3, 0.4)])
@sfx('astra_6_loose')
def _a6l(): return seq([(S.overtone_voice(deg('S', -2), 4.0, 900.0), 0, 0.9), (S.gong(deg('S', -3), 5.0), 0, 0.7)])
@sfx('astra_0_live')
def _a0v(): return S.bowl(deg('S', 1), 3.0) * 0.5 + S.overtone_voice(deg('S', -1), 3.0, 1500.0) * 0.4
@sfx('astra_1_live')
def _a1v(): return S.bandpass(S.noise(S.n_of(3.0), 169), 300, 3500) * 0.5
@sfx('astra_2_live')
def _a2v(): return S.lowpass(S.noise(S.n_of(3.0), 171), 1800) * 0.7 * (0.7 + 0.3 * np.sin(S.TAU * 0.7 * np.arange(S.n_of(3.0)) / S.SR))
@sfx('astra_3_live')
def _a3v(): return S.bandpass(S.noise(S.n_of(3.0), 173), 200, 1200) * 0.6 + S.jaltarang(deg('S', 2), 3.0) * 0.2
@sfx('astra_4_live')
def _a4v(): return S.lowpass(S.noise(S.n_of(3.0), 175), 200) * 1.0 + S.tanpura(deg('S', -2), 3.0) * 0.4
@sfx('astra_5_live')
def _a5v(): return S.bowl(deg('S', 1), 3.0) * 0.4 + S.voice(deg('S', -1), 3.0, 'o', attack=1.0, release=1.0) * 0.4
@sfx('astra_6_live')
def _a6v(): return S.overtone_voice(deg('S', -2), 3.0, 1000.0) * 0.9
@sfx('astra_cube')
def _acube(): return S.bell(deg('S', 3), 0.3) * 0.5 + click(3, 6000) * 0.3
@sfx('astra_hit')
def _ahit(): return S.membrane(100, 0.3, 1.4, 0.1, 0.6, 0.2, 177) + S.cymbal(0.3, 4500, 0.1, 178) * 0.5
@sfx('astra_petal')
def _apetal(): return S.jaltarang(deg('S', 4), 0.2) * 0.5
@sfx('astra_cancel')
def _acancel(): return S.sarangi(np.linspace(deg('S', 0), deg('S', 0) * 0.9, 20), 0.5, attack=0.02, release=0.3) * 0.5


# ============================================================================================================================ PICKUPS, LAMPS, BAYS
for _i, _n in enumerate('SRGPD'):
    REG['coin_%d' % _i] = (lambda n=_n: S.ghungroo(0.3, 5, 181) * 0.6 + S.ghanta(deg(n, 3), 0.25) * 0.5)
SHARD = [lambda: S.bowl(deg('S', 1), 1.2) * 0.9, lambda: S.bansuri(deg('P', 2), 0.7, breath=0.4) * 0.8, lambda: S.damaru(1, 0, 190) * 0.8 + click(4, 3500) * 0.4,
         lambda: S.jaltarang(deg('R', 2), 0.9), lambda: S.ghatam(200, 0.4)]
for _i, _f in enumerate(SHARD):
    REG['shard_%d' % _i] = _f
@sfx('shard_release')
def _srel(): return S.morph_voice(deg('S', -1), 1.0, ['o', 'm'], attack=0.1, release=0.4) * 0.7 + S.bowl(deg('S', 2), 1.2) * 0.4
@sfx('health')
def _health(): return seq([(S.veena(deg('S', 1), 1.0, 1.2), 0, 0.8), (S.veena(deg('P', 1), 1.0, 1.2), 0.12, 0.6), (S.veena(deg('S', 2), 1.0, 1.2), 0.25, 0.7)])
@sfx('medkit')
def _medkit(): return S.ghanta(deg('P', 2), 0.5) * 0.7 + click(4, 3000) * 0.3
@sfx('piece_hum')
def _phum(): return S.voice(np.linspace(deg('S', 0), deg('S', 1), 40), 3.0, 'a', attack=1.5, release=0.6) * 0.5 + S.bowl(deg('S', 2), 3.0) * 0.3
@sfx('lamp_bind')
def _lamp(): return seq([(S.highpass(S.noise(S.n_of(0.5), 183), 600) * np.hanning(S.n_of(0.5)), 0, 0.4), (S.ghanta(deg('S', 1), 3.0), 0.05, 0.7), (S.bowl(deg('S', 1) * 1.0, 3.0), 0.1, 0.4)])
@sfx('bay_dock')
def _dock(): return S.bowl(deg('S', 0), 2.0) * 0.6 + S.ghanta(deg('S', -1), 2.0) * 0.5 + S.lowpass(S.noise(S.n_of(1.0), 185), 500) * np.hanning(S.n_of(1.0)) * 0.3
@sfx('bay_close')
def _bclose(): return S.lowpass(S.noise(S.n_of(0.5), 187), 700) * np.hanning(S.n_of(0.5)) * 0.6 + S.bowl(deg('S', 0), 1.0) * 0.3
@sfx('buy')
def _buy(): return S.ghungroo(0.4, 6, 189) * 0.6 + S.ghanta(deg('P', 2), 0.6) * 0.5 + S.ghatam(500, 0.1) * 0.3
@sfx('deny')
def _deny(): return S.membrane(160, 0.15, 1.0, 0.05, 0.9, 0.1, 191) * 0.9 + S.sarangi(deg('S', -1) * 0.85, 0.2, attack=0.01, release=0.1) * 0.4
@sfx('temper')
def _temper(): return seq([(S.ghatam(600, 0.15), 0, 0.9), (S.ghanta(deg('S', 2), 1.5), 0.02, 0.8)])
@sfx('temper3')
def _temper3(): return seq([(S.ghatam(600, 0.15), 0, 0.9), (S.ghanta(deg('S', 2), 2.0), 0.02, 0.8), (S.bowl(deg('P', 2), 2.5), 0.1, 0.5), (S.voice(deg('S', 0), 2.0, 'a', attack=0.4, release=1.0), 0.15, 0.3)])
@sfx('sell')
def _sell(): return S.ghungroo(0.8, 14, 193) * 0.8
@sfx('fit')
def _fit(): return click(5, 2500) * 1.0 + S.bowl(deg('S', 2), 0.4) * 0.5
@sfx('chest')
def _chest(): return S.lowpass(S.noise(S.n_of(0.3), 195), 500) * np.hanning(S.n_of(0.3)) * 0.6 + S.ghanta(deg('R', 3), 0.8) * 0.5
@sfx('gate')
def _gate(): return seq([(S.gong(deg('S', -2), 5.0), 0, 0.9), (S.lowpass(S.noise(S.n_of(3.0), 197), 350) * np.hanning(S.n_of(3.0)), 0, 0.8), (S.shankha(deg('S', 0), 3.0), 0.6, 0.4)])
@sfx('seal_open')
def _seal(): return seq([(S.lowpass(S.noise(S.n_of(2.5), 199), 500) * np.hanning(S.n_of(2.5)), 0, 0.8), (S.bowl(deg('S', 1), 3.0), 0.8, 0.5), (S.ghanta(deg('P', 2), 2.5), 1.0, 0.4)])
@sfx('veil_part')
def _veil(): return seq([(swoosh(3.0, 200, 1500, 0.5), 0, 1.0), (S.morph_voice(deg('S', -1), 4.0, ['a', 'o'], attack=1.5, release=1.5), 1.0, 0.5)])
@sfx('rock_tick')
def _rock(): return S.ghatam(300, 0.1) * 0.5 + S.lowpass(S.noise(S.n_of(0.1), 201), 1200) * 0.2


# ============================================================================================================================ INTERFACE
@sfx('ui_move')
def _uim(): return S.ghatam(deg('S', 1), 0.1) * 0.6 + S.bowl(deg('S', 2), 0.4) * 0.15
@sfx('ui_select')
def _uis(): return S.ghanta(deg('S', 2), 0.7) * 0.8
@sfx('ui_back')
def _uib(): return S.ghanta(deg('S', 1), 0.6) * 0.6
@sfx('ui_newgame')
def _uing(): return S.shankha(deg('S', 0), 2.0) * 0.9 + S.ghanta(deg('S', 2), 2.0) * 0.3
@sfx('ui_toggle')
def _uit(): return click(5, 2500) * 1.0 + S.bowl(deg('P', 2), 0.25) * 0.3
@sfx('ui_tick')
def _uitick(): return S.ghatam(deg('S', 2), 0.06) * 0.4
@sfx('ui_tour')
def _uitour(): return S.bowl(deg('S', 2), 0.8) * 0.6
@sfx('ui_page')
def _uipage(): return S.lowpass(S.noise(S.n_of(0.15), 203), 3000) * np.hanning(S.n_of(0.15)) * 0.5
@sfx('ui_popup')
def _uipop(): return S.jaltarang(deg('P', 2), 0.4) * 0.6
@sfx('ui_combo')
def _uicombo(): return S.jaltarang(deg('G', 2), 0.3) * 0.5 + S.jaltarang(deg('P', 2), 0.3) * 0.3
@sfx('ui_wheel_open')
def _uiwo(): return S.bowl(deg('S', 1), 1.0) * 0.5
@sfx('ui_wheel_tick')
def _uiwt(): return S.ghatam(deg('P', 2), 0.05) * 0.4
@sfx('ui_error')
def _uierr(): return S.membrane(150, 0.15, 1.0, 0.05, 0.9, 0.1, 205) * 0.7
@sfx('ui_codex')
def _uicodex(): return seq([(S.lowpass(S.noise(S.n_of(0.12), 207), 3500) * np.hanning(S.n_of(0.12)), 0, 0.4), (S.ghanta(deg('P', 2), 1.0), 0.05, 0.6)])


# ============================================================================================================================ AMBIENCE (loops)
def _loop(x):
    """Cross-fade the end into the start so the file repeats with no click."""
    f = S.n_of(0.4)
    out = x[:len(x) - f].copy()
    out[:f] = out[:f] * np.linspace(0, 1, f) + x[len(x) - f:] * np.linspace(1, 0, f)
    return out


@sfx('amb_atala')
def _amb_atala():
    n = S.n_of(22.0)
    t = np.arange(n) / S.SR
    wind = S.bandpass(S.noise(n, 209), 100, 900) * (0.5 + 0.5 * np.sin(S.TAU * 0.09 * t + 1.0) ** 2) * 0.6
    chime = np.zeros(n)
    for i, (tt, nm) in enumerate([(2.0, 'S'), (7.5, 'P'), (11.0, 'G'), (15.5, 'S'), (19.0, 'R')]):
        c = S.bell(deg(nm, 4), 3.0) * 0.1
        s = int(tt * S.SR)
        chime[s:s + len(c)] += c[:max(0, n - s)] if s + len(c) > n else c
    return _loop(wind + chime)


@sfx('amb_vitala')
def _amb_vitala():
    n = S.n_of(22.0)
    t = np.arange(n) / S.SR
    furnace = S.lowpass(S.noise(n, 211), 260) * (0.6 + 0.4 * np.sin(S.TAU * 0.13 * t)) * 1.0
    ash = S.highpass(S.noise(n, 212), 4000) * 0.04
    drum = np.zeros(n)
    for i in range(11):
        d = S.damaru(2, 0.08, 120) * 0.2
        s = int((i * 2.0 + 1.0) * S.SR)
        drum[s:s + len(d)] += d[:max(0, n - s)] if s + len(d) > n else d
    return _loop(furnace + ash + drum)


@sfx('amb_flood')
def _amb_flood():
    n = S.n_of(6.0)
    return _loop(S.bandpass(S.noise(n, 213), 150, 2500) * 0.7)


# ============================================================================================================================ VOICES: sung dialogue, seven notes of each speaker
SPEAKERS = {
    'indra': lambda f: S.shankha(f * 0.5, 0.35) * 1.0,
    'bala': lambda f: S.sarangi(f * 0.5, 0.2, vib=0.01, attack=0.02, release=0.08, rosin=0.3),
    'bhairava': lambda f: S.overtone_voice(f * 0.25, 0.25, f * 3.0),
    'scout': lambda f: S.bansuri(f * 2.0, 0.16, breath=0.3, attack=0.02, release=0.06),
    'maya': lambda f: S.santoor(f * 2.0, 0.3) + S.bell(f * 2.0 * 1.0 / 1.0, 0.3) * 0.15,
    'kaliya': lambda f: S.jaltarang(f * 2.0, 0.3),
    'seller': lambda f: S.ghanta(f * 4.0, 0.3) * 0.6 + S.ghungroo(0.15, 3, 215) * 0.4,
    'maid': lambda f: S.veena(f, 0.25, 0.4),
    'dipaka': lambda f: S.bell(f * 4.0, 0.25) * 0.7,
    'vanija': lambda f: S.kartal(0.05, 217) * 0.7 + S.tabla('na', f) * 0.5,
    'dvarapala': lambda f: S.pakhawaj(f * 0.25, 0.3, 0.2) + S.shankha(f * 0.5, 0.25) * 0.4,
    'voice': lambda f: S.bowl(f, 0.3) * 0.7,
}
BHOOPALI = ['S', 'R', 'G', 'P', 'D', 'S', 'G']     # seven degrees: the speaker's register is the instrument's
for _sp, _fn in SPEAKERS.items():
    for _d, _nm in enumerate(BHOOPALI):
        REG['v_%s_%d' % (_sp, _d)] = (lambda fn=_fn, nm=_nm, d=_d: fn(SA * RATIO[nm] * (2.0 if d == 5 else 1.0)))


# ============================================================================================================================ the runner
def render(name):
    x = REG[name]()
    x = np.asarray(x, dtype=float)
    if x.ndim > 1:
        x = x.mean(axis=1)
    x = fade_out(S.trim_silence(x, 0.0008), 25)
    loopy = name.endswith('_loop') or name.startswith('amb_') or name in ('engine_loop',)
    S.save(os.path.join(OUT, name + '.ogg'), x, quality=3, peak=0.8 if not loopy else 0.6, mono=True)


if __name__ == '__main__':
    pre = sys.argv[1:]
    names = [k for k in REG if not pre or any(k.startswith(p) for p in pre)]
    os.makedirs(OUT, exist_ok=True)
    for k in names:
        render(k)
    print('rendered', len(names))
