"""THE SECOND PASS: punchier shots, foe fire and death, short and distinct menu sounds, a unique tone for each element of the world, an engine you can feel, and the mantras of the astras.
Imported by render_all.py after sfx.py: every recipe here replaces the recipe of the same name (the later one wins).
"""
import numpy as np

import synth as S
import synth2 as P
import sfx as B
from raga import RATIO, LOKA_SA
from sfx import REG, sfx, deg, seq, swoosh, click, rev, SA

TAU = S.TAU


def fx(x, g=1.0):
    return S.Sig(np.asarray(x, dtype=float) * g)


# ============================================================================================================================ PLAYER SHOTS
def shot(f0, f1, dur=0.15, body=0.0, ring=None, grit=0.0, drive=1.7, harm=0.3):
    """A bolt: a tone that falls fast with a crack on the front, a thump under it, a metal ring on it."""
    x = P.zap(f0, f1, dur, harm=harm, drive=drive)
    if body:
        x = x + P.thump(body, dur + 0.06, 2.6, 0.0, 0.07) * 0.8
    if ring:
        x = x + S.ghanta(ring, 0.28)[:S.n_of(0.28)] * 0.22
    if grit:
        x = x + S.bandpass(S.noise(S.n_of(dur), 301), 2000, 7000) * np.exp(-np.arange(S.n_of(dur)) / (0.03 * S.SR)) * grit
    return x


def rise(f0, f1, dur, tau=None, harm=0.4, drive=1.4):
    """The opposite sweep, for charging, hunting and the foes' dark answers."""
    return P.zap(f1, f0, dur, tau=tau, harm=harm, drive=drive)


def burst_of(n_hits, gap, make):
    return seq([(make(i), i * gap, 1.0) for i in range(n_hits)])


def whoosh(dur, f0, f1, level=1.0, seed=311):
    return swoosh(dur, f0, f1, level, 0.6, seed)


WPN = {}


def wp(i):
    def deco(fn):
        WPN[i] = fn
        return fn
    return deco


@wp(0)
def _a(p): return shot(2100 * p, 380 * p, 0.15, 120 * p, deg('S', 3) * p)
@wp(1)
def _a(p): return seq([(shot(2000 * p, 400 * p, 0.13, 110 * p), 0, 1), (shot(2600 * p, 500 * p, 0.13, 110 * p), 0.05, 0.9)])
@wp(2)
def _a(p): return seq([(shot((1900 + 250 * i) * p, 420 * p, 0.09, 100 * p), i * 0.045, 0.95) for i in range(3)])
@wp(3)
def _a(p): return seq([(shot((1800 + 300 * (i % 2)) * p, 400 * p, 0.09, 100 * p), (i // 2) * 0.085 + (i % 2) * 0.012, 0.9) for i in range(4)])
@wp(4)
def _a(p): return fx(P.blast(0.2, 150 * p, 1.0, 321, 0)) * 0.8 + S.kartal(0.12, 322) * 0.9 + P.thump(95 * p, 0.2, 2.5, 0.0, 0.07)
@wp(5)
def _a(p): return P.thump(52 * p, 0.45, 3.2, 0.8, 0.15) + P.zap(1100 * p, 110 * p, 0.34, harm=0.5, drive=2.2) * 0.8 + S.gong(deg('S', -1) * p, 0.8)[:S.n_of(0.8)] * 0.12
@wp(6)
def _a(p): return P.zap(3000 * p, 1100 * p, 0.05, tau=0.02, harm=0.1, drive=1.2) * 0.9 + S.kartal(0.03, 323) * 0.5
@wp(7)
def _a(p): return rise(500 * p, 2600 * p, 0.28) * 0.9 + fx(P.blast(0.45, 85 * p, 0.9, 324, 3)) + S.bell(deg('P', 2) * p, 0.6) * 0.3
@wp(8)
def _a(p):
    t = np.arange(S.n_of(0.7)) / S.SR
    beam = np.sin(S.phase_of(np.linspace(2800 * p, 1400 * p, len(t)) * (1 + 0.015 * np.sin(TAU * 38 * t)))) * np.exp(-t / 0.5)
    return fx(beam, 0.7) + whoosh(0.7, 5000, 9000, 0.25) + P.thump(70, 0.25, 2.0, 0.0, 0.08) * 0.5
@wp(9)
def _a(p): return seq([(P.zap(1500 * p, 200 * p, 0.45, tau=0.3, harm=0.1, drive=1.0), 0, 0.7), (P.thump(46 * p, 0.35, 2.4, 0.4, 0.14), 0, 1.0)])
@wp(10)
def _a(p): return seq([(whoosh(0.4, 350, 2400, 0.8), 0, 1.0), (P.thump(85 * p, 0.25, 2.2, 0.5, 0.09), 0, 0.9), (P.zap(1800 * p, 500 * p, 0.08), 0, 0.6)])
@wp(11)
def _a(p): return seq([(whoosh(0.38, 350, 2400, 0.8, 331), 0, 1.0), (whoosh(0.38, 420, 2800, 0.8, 332), 0.07, 0.9), (P.thump(85 * p, 0.2, 2.2, 0.4, 0.08), 0, 0.8), (S.bell(deg('R', 2) * p, 0.4), 0.05, 0.2)])
@wp(12)
def _a(p): return P.zap(3200 * p, 1700 * p, 0.11, tau=0.05, harm=0.15) * 0.9 + S.bell(deg('S', 3) * p, 0.5) * 0.4
@wp(13)
def _a(p): return P.zap(4200 * p, 800 * p, 0.1, tau=0.04, harm=0.05, drive=1.0) + S.sarod(deg('N', 2) * p, 0.2, 0.12) * 0.5
@wp(14)
def _a(p): return P.zap(280 * p, 110 * p, 0.3, tau=0.22, harm=0.9, drive=3.2) + S.bandpass(S.noise(S.n_of(0.3), 333), 150, 1200) * 0.8 + P.thump(60, 0.25, 2.0, 0.3, 0.1) * 0.6
@wp(15)
def _a(p):
    a = shot(2000 * p, 420 * p, 0.14, 110 * p)
    return seq([(a, 0, 1.0), (rev(np.asarray(a)), 0.05, 0.5)])
@wp(16)
def _a(p): return S.bansuri(np.linspace(deg('S', 2), deg('P', 2), 40) * p, 0.4, breath=0.9, attack=0.03, release=0.2) * 0.8 + whoosh(0.4, 1500, 6000, 0.55, 335)
@wp(17)
def _a(p): return seq([(shot(2400 * p, 600 * p, 0.12, 0), 0, 0.8), (S.santoor(deg('G', 1) * p, 0.5), 0, 0.7), (S.santoor(deg('D', 1) * p, 0.5), 0.05, 0.6)])
@wp(18)
def _a(p): return shot(1500 * p, 280 * p, 0.12, 90) + S.highpass(S.noise(S.n_of(0.2), 337) * np.exp(-np.arange(S.n_of(0.2)) / (0.05 * S.SR)), 2500) * 0.6
@wp(19)
def _a(p): return S.bandpass(S.noise(S.n_of(0.18), 339), 1500, 8000) * np.hanning(S.n_of(0.18)) * 1.0 + P.thump(120, 0.1, 2, 0.0, 0.04) * 0.5
@wp(20)
def _a(p): return P.thump(75 * p, 0.35, 3.0, 0.7, 0.13) + P.zap(500 * p, 130 * p, 0.2, harm=0.5) * 0.7
@wp(21)
def _a(p): return seq([(S.jaltarang(deg('R', 1) * p * np.linspace(1.3, 1.0, 1)[0], 0.4), 0, 0.8), (P.zap(1600 * p, 700 * p, 0.1, harm=0.1), 0, 0.8), (S.bandpass(S.noise(S.n_of(0.06), 341), 500, 3000) * np.hanning(S.n_of(0.06)), 0.03, 0.5)])
@wp(22)
def _a(p): return seq([(S.bowl(deg('S', 1) * p, 0.8), 0, 0.6), (P.thump(90, 0.2, 2.0, 0.3, 0.07), 0, 0.7)] + [(S.jaltarang(deg(n, 2) * p, 0.25), 0.03 + i * 0.04, 0.35) for i, n in enumerate('SRGPD')])
@wp(23)
def _a(p): return seq([(P.zap(500 * p, 1100 * p, 0.18, harm=0.2, drive=1.1), 0, 0.8), (S.veena(deg('D', 1) * p, 0.45, 0.45), 0.02, 0.8)])
@wp(24)
def _a(p): return P.zap(2900 * p, 1900 * p, 0.1, tau=0.045, harm=0.5) + S.bell(deg('G', 2) * p, 0.35, [(1, 1, 1), (2.76, 0.6, 0.5), (5.4, 0.4, 0.3)]) * 0.5
@wp(25)
def _a(p):
    n = S.n_of(0.16)
    t = np.arange(n) / S.SR
    crackle = S.highpass(S.noise(n, 343), 3000) * (np.sin(TAU * 90 * t) > 0) * np.exp(-t / 0.07)
    return P.zap(700 * p, 3500 * p, 0.14, harm=0.7, drive=2.5) * 0.8 + crackle * 0.9
@wp(26)
def _a(p): return P.thump(120 * p, 0.2, 2.0, 0.6, 0.05) + S.bowl(deg('S', 1) * p, 0.4) * 0.5 + click(4, 3000) * 0.5
@wp(27)
def _a(p): return S.sarangi(np.linspace(deg('S', 0), deg('G', 0), 30) * p, 0.4, vib=0.004, attack=0.02, release=0.2) * 0.7 + P.zap(900 * p, 300 * p, 0.2, harm=0.9, drive=2.0) * 0.5 + whoosh(0.35, 3000, 6000, 0.3, 345)
@wp(28)
def _a(p): return P.zap(5000 * p, 2000 * p, 0.08, tau=0.03, harm=0.05, drive=1.0) + S.highpass(S.noise(S.n_of(0.14), 347), 5000) * np.hanning(S.n_of(0.14)) * 0.6
@wp(29)
def _a(p): return S.bandpass(S.noise(S.n_of(0.35), 349), 500, 3200) * np.hanning(S.n_of(0.35)) * 0.9 + P.thump(110 * p, 0.2, 1.8, 0.0, 0.08) * 0.6
@wp(30)
def _a(p): return rise(120 * p, 600 * p, 0.6, tau=0.4, harm=0.9) * 0.9 + P.thump(40, 0.5, 1.6, 0.0, 0.25) * 0.8
@wp(31)
def _a(p): return P.thump(40 * p, 0.55, 3.0, 0.9, 0.22) + S.ghatam(190 * p, 0.4) * 0.9 + S.lowpass(S.noise(S.n_of(0.5), 351), 300) * np.hanning(S.n_of(0.5)) * 0.8
@wp(32)
def _a(p): return S.ghatam(320 * p, 0.25) + seq([(S.santoor(f * p, 0.25), 0.015 * i, 0.4) for i, f in enumerate([2200, 3100, 2700, 3900])]) + P.zap(2500 * p, 900 * p, 0.08) * 0.5
@wp(33)
def _a(p): return seq([(S.ghanta(deg(n, 3) * p, 0.5), i * 0.035, 0.5) for i, n in enumerate('SGP')]) + P.zap(3500 * p, 1500 * p, 0.1, harm=0.2) * 0.6
@wp(34)
def _a(p): return seq([(S.gong(deg('S', -1) * p, 1.2), 0.0, 0.9), (P.thump(48, 0.5, 3.0, 0.9, 0.2), 0.0, 1.0), (P.zap(900 * p, 150 * p, 0.2), 0.0, 0.6)])
@wp(35)
def _a(p): return seq([(S.tanpura(deg('S', -1) * p, 1.4), 0, 0.4), (S.voice(deg('S', -2) * p, 1.2, 'a', attack=0.5, release=0.5), 0, 0.5), (rise(300 * p, 1400 * p, 1.0, tau=0.7, harm=0.3), 0.1, 0.6), (S.bowl(deg('S', 1) * p, 1.2), 0.2, 0.4)])


for _i, _fn in WPN.items():
    REG['w%d_a' % _i] = (lambda fn=_fn: fn(1.0))
    REG['w%d_b' % _i] = (lambda fn=_fn: fn(1.04))


# ============================================================================================================================ HITS AND BLASTS
@sfx('hit_tick')
def _ht(): return P.zap(2600, 900, 0.06, tau=0.025, harm=0.1) + click(5, 5000, 351) * 0.8
@sfx('hit_bell')
def _hb(): return S.bell(deg('P', 3), 0.5) * 0.6 + P.zap(3000, 1500, 0.05, tau=0.02) * 0.8
@sfx('hit_thud')
def _hth(): return P.thump(95, 0.22, 2.6, 0.6, 0.07)
@sfx('hit_wet')
def _hw(): return S.jaltarang(deg('S', 3), 0.2) * 0.5 + S.bandpass(S.noise(S.n_of(0.1), 353), 500, 2500) * np.hanning(S.n_of(0.1)) * 0.6 + P.thump(130, 0.1, 2.0, 0.0, 0.04) * 0.5
@sfx('hit_glass')
def _hg(): return seq([(S.santoor(f, 0.2), 0.012 * i, 0.4) for i, f in enumerate([2500, 3300, 4100])]) + S.cymbal(0.1, 8000, 0.03, 355) * 0.5
@sfx('hit_stone')
def _hs(): return S.ghatam(250, 0.15) * 0.9 + S.lowpass(S.noise(S.n_of(0.1), 357), 900) * np.hanning(S.n_of(0.1)) * 0.5 + click(4, 2500, 358) * 0.5
@sfx('hit_burn')
def _hbn(): return S.highpass(S.noise(S.n_of(0.22), 359) * np.exp(-np.arange(S.n_of(0.22)) / (0.06 * S.SR)), 2000) * 0.8 + P.zap(1300, 300, 0.1, harm=0.8, drive=2.5) * 0.6
@sfx('boom_small')
def _bs(): return P.blast(0.5, 75, 0.7, 361, 5)
@sfx('boom_big')
def _bb(): return seq([(P.blast(1.1, 55, 0.8, 363, 10), 0, 1.0), (S.gong(deg('S', -2), 2.0), 0.02, 0.35)])


# ============================================================================================================================ FOES: dark, angry, and unlike the pilot's own bolts
def foe_shot(f_hi, f_lo, dur=0.18, body=0.0, rasp=0.5, rising=False):
    x = (P.zap(f_lo, f_hi, dur, harm=0.9, drive=3.0) if rising else P.zap(f_hi, f_lo, dur, harm=0.9, drive=3.0))
    x = x + S.bandpass(S.noise(S.n_of(dur), 371), 400, 2500) * np.exp(-np.arange(S.n_of(dur)) / (0.05 * S.SR)) * rasp
    if body:
        x = x + P.thump(body, dur + 0.05, 2.0, 0.0, 0.07) * 0.8
    return x


FOE_FIRE = {
    'mayavi': lambda: foe_shot(900, 280, 0.34, 0, 0.3, True) * 0.8 + S.bell(deg('G', 2), 0.5) * 0.35,
    'hataka': lambda: foe_shot(1500, 700, 0.1, 0, 0.5) * 0.9 + S.bell(deg('N', 3), 0.25, [(1, 1, 1), (2.9, 0.8, 0.5)]) * 0.35,
    'auris': lambda: S.tabla('tak') * 1.1 + foe_shot(2200, 900, 0.08, 0, 0.3),
    'lance': lambda: foe_shot(650, 260, 0.14, 80, 0.4) + S.ghatam(420, 0.15) * 0.7,
    'vakra': lambda: seq([(foe_shot(1900, 800, 0.05, 0, 0.3), i * 0.05, 0.9) for i in range(3)]),
    'bhavi': lambda: foe_shot(380, 110, 0.28, 70, 0.5) + P.thump(60, 0.3, 2.4, 0.4, 0.12),
    'dvarapala': lambda: seq([(S.shankha(deg('S', -1), 0.5), 0, 0.5), (P.thump(55, 0.45, 2.8, 0.6, 0.16), 0, 1.0), (foe_shot(300, 100, 0.25, 0, 0.6), 0, 0.7)]),
    'dipaka': lambda: foe_shot(3200, 1600, 0.06, 0, 0.2) + S.ghanta(deg('P', 3), 0.15) * 0.4,
    'vanija': lambda: S.tabla('na', 400) * 0.8 + foe_shot(1700, 700, 0.08, 0, 0.3) + S.ghanta(deg('S', 3), 0.1) * 0.3,
    'bala': lambda: P.thump(60, 0.45, 3.0, 0.7, 0.17) + foe_shot(500, 130, 0.3, 0, 0.7) + S.sarangi(deg('S', 0), 0.3, attack=0.02, release=0.15) * 0.35,
}
for _k, _f in FOE_FIRE.items():
    REG['foe_%s_fire' % _k] = _f


def foe_death(i, k):
    body = 120 - 6 * i
    base = P.blast(0.5 + 0.02 * i, body, 0.8, 381 + i, 4 + i % 4)
    flavour = {
        'mayavi': S.santoor(2600, 0.5) * 0.4 + S.cymbal(0.3, 8000, 0.08, 391) * 0.5,
        'hataka': S.bell(deg('N', 3), 0.5, [(1, 1, 1), (2.9, 0.8, 0.5)]) * 0.4 + S.kartal(0.15, 392) * 0.7,
        'auris': S.tabla('tak') * 0.9 + S.tabla('dha', 300) * 0.6,
        'lance': S.ghatam(300, 0.3) * 0.8,
        'vakra': S.kartal(0.2, 393) * 0.9 + P.zap(1500, 300, 0.15, harm=0.8) * 0.5,
        'bhavi': S.gong(deg('S', -1), 1.0) * 0.3 + S.ghatam(150, 0.4) * 0.9,
        'bala': S.gong(deg('S', -2), 2.0) * 0.5,
        'dvarapala': S.gong(deg('S', -1), 1.4) * 0.4 + P.thump(40, 0.6, 3.0, 0.5, 0.2),
        'dipaka': S.ghanta(deg('P', 3), 0.6) * 0.5 + S.highpass(S.noise(S.n_of(0.3), 394), 3000) * np.hanning(S.n_of(0.3)) * 0.3,
        'vanija': S.ghungroo(0.5, 12, 395) * 0.8 + S.ghanta(deg('S', 3), 0.3) * 0.3,
    }[k]
    return base + flavour


for _i, _k in enumerate(B.KINDS):
    REG['foe_%s_die' % _k] = (lambda i=_i, k=_k: foe_death(i, k))
    REG['foe_%s_wake' % _k] = (lambda i=_i: P.zap(400 + 40 * i, 900 + 60 * i, 0.12, harm=0.6, drive=2.0) * 0.5 + S.ghanta(deg('S', 2) * 2.0 ** (i / 12.0), 0.3) * 0.25)
for _i in range(3):
    REG['foe_hurt_%d' % _i] = (lambda i=_i: P.zap(900 + 150 * i, 300, 0.07, harm=0.7, drive=2.2) * 0.8 + S.ghatam(300 + 60 * i, 0.1) * 0.6)


# ============================================================================================================================ INTERFACE: short, each its own
@sfx('ui_move')
def _um(): return S.ghatam(deg('S', 3), 0.045)[:S.n_of(0.045)] * 0.9 + P.zap(2200, 1700, 0.03, tau=0.012, harm=0.0, drive=1.0) * 0.5
@sfx('ui_select')
def _us(): return seq([(S.pluck(deg('S', 3), 0.12, 0.9, 0.08), 0, 0.9), (S.pluck(deg('P', 3), 0.16, 0.9, 0.12), 0.055, 1.0)])
@sfx('ui_back')
def _ub(): return P.zap(900, 520, 0.1, tau=0.05, harm=0.2, drive=1.1) * 0.9 + S.pluck(deg('S', 2), 0.12, 0.6, 0.08) * 0.5
@sfx('ui_adjust')
def _uad(): return P.zap(1500, 1900, 0.04, tau=0.016, harm=0.1, drive=1.0) * 0.8 + click(3, 6000, 401) * 0.4
@sfx('ui_tab')
def _utab(): return whoosh(0.08, 800, 3500, 0.5, 403) + S.ghatam(deg('R', 3), 0.04)[:S.n_of(0.04)] * 0.5
@sfx('ui_toggle')
def _utg(): return seq([(click(3, 3000, 405) * 1.2, 0, 1.0), (click(3, 4500, 406) * 1.2, 0.035, 0.9)])
@sfx('ui_page')
def _upg(): return S.highpass(S.noise(S.n_of(0.08), 407), 2500) * np.hanning(S.n_of(0.08)) * 0.5
@sfx('ui_error')
def _uer(): return seq([(P.thump(130, 0.08, 1.4, 0.3, 0.035), 0, 1.0), (P.thump(110, 0.1, 1.4, 0.3, 0.04), 0.07, 0.9)])
@sfx('ui_tour')
def _utr(): return S.bowl(deg('G', 2), 0.35) * 0.7 + P.zap(2800, 2400, 0.03, tau=0.012, harm=0.0) * 0.4
@sfx('ui_tick')
def _utk(): return click(3, 4000, 409) * 0.9
@sfx('ui_wheel_open')
def _uwo(): return rise(500, 1400, 0.18, tau=0.1, harm=0.2) * 0.7 + S.bowl(deg('S', 2), 0.3) * 0.3
@sfx('ui_wheel_tick')
def _uwt(): return S.ghatam(deg('P', 3), 0.035)[:S.n_of(0.035)] * 0.7
@sfx('ui_popup')
def _upp(): return S.jaltarang(deg('P', 2), 0.25) * 0.5 + P.zap(1800, 2400, 0.05, tau=0.02, harm=0.0) * 0.3
@sfx('ui_combo')
def _ucb(): return seq([(S.jaltarang(deg(n, 2), 0.2), i * 0.05, 0.5) for i, n in enumerate('GP')])
@sfx('ui_newgame')
def _ung(): return S.shankha(deg('S', 0), 1.2) * 0.8 + S.ghanta(deg('S', 2), 1.0) * 0.3
@sfx('ui_codex')
def _ucx(): return seq([(S.highpass(S.noise(S.n_of(0.08), 411), 2500) * np.hanning(S.n_of(0.08)), 0, 0.3), (S.ghanta(deg('P', 2), 0.5), 0.03, 0.5)])


# the bay: a purchase, a refusal, a fitting, a tempering, a sale: each a different short gesture
@sfx('buy')
def _buy(): return seq([(S.ghungroo(0.2, 5, 421), 0, 0.5), (S.ghanta(deg('S', 3), 0.3), 0.0, 0.5), (S.ghanta(deg('P', 3), 0.45), 0.07, 0.6)])
@sfx('deny')
def _dny(): return seq([(S.ghatam(150, 0.1), 0, 0.9), (S.ghatam(130, 0.12), 0.08, 0.8)])
@sfx('fit')
def _fit(): return click(3, 2500, 423) * 1.0 + P.zap(1300, 900, 0.05, tau=0.02, harm=0.1) * 0.4 + S.bowl(deg('S', 3), 0.25) * 0.3
@sfx('temper')
def _tmp(): return seq([(S.ghatam(650, 0.1), 0, 0.9), (S.ghanta(deg('S', 2), 0.9), 0.0, 0.7)])
@sfx('temper3')
def _tmp3(): return seq([(S.ghatam(650, 0.1), 0, 0.9), (S.ghanta(deg('S', 2), 1.4), 0.0, 0.7), (S.bowl(deg('P', 2), 1.6), 0.06, 0.4), (S.ghanta(deg('S', 3), 1.4), 0.12, 0.4)])
@sfx('sell')
def _sell(): return S.ghungroo(0.5, 14, 425) * 0.9


# ============================================================================================================================ THE WORLD'S ELEMENTS: each its own tone
# the lamp is flame and bell (Sa, a rising arpeggio of Yaman); the bay is a bowl and a gong at the fourth (Ma), a mechanical latch; the chest is wood and coin; the gate is a vast gong; the seal is stone and a chord
@sfx('lamp_bind')
def _lamp(): return seq([(S.highpass(S.noise(S.n_of(0.4), 431), 700) * np.hanning(S.n_of(0.4)), 0, 0.3)] + [(S.ghanta(deg(n, 2), 1.6), 0.08 + i * 0.13, 0.6) for i, n in enumerate(['S', 'G', 'P', 'S'])] + [(S.bowl(deg('S', 1), 2.5), 0.1, 0.35)])
@sfx('bay_dock')
def _dock(): return seq([(click(5, 2000, 433) * 1.2, 0, 1.0), (S.gong(deg('M', -1), 2.2), 0.05, 0.6), (S.bowl(deg('M', 0), 2.0), 0.1, 0.5), (S.bowl(deg('S', 1), 1.8), 0.45, 0.35), (P.thump(70, 0.25, 2.0, 0.5, 0.08), 0.02, 0.6)])
@sfx('bay_close')
def _bcl(): return seq([(S.bowl(deg('M', 0), 0.9), 0, 0.4), (click(5, 2000, 435) * 1.2, 0.2, 0.9), (P.thump(60, 0.2, 2.0, 0.4, 0.07), 0.2, 0.6)])
@sfx('chest')
def _chest(): return seq([(S.lowpass(S.noise(S.n_of(0.25), 437), 600) * np.hanning(S.n_of(0.25)), 0, 0.7), (S.ghungroo(0.6, 10, 438), 0.12, 0.7), (S.santoor(deg('G', 3), 0.5), 0.2, 0.5), (S.santoor(deg('D', 3), 0.6), 0.3, 0.5)])
@sfx('gate')
def _gate(): return seq([(S.gong(deg('S', -2), 5.0), 0, 0.9), (P.thump(38, 1.0, 2.0, 0.0, 0.5), 0, 0.9), (S.lowpass(S.noise(S.n_of(3.0), 439), 350) * np.hanning(S.n_of(3.0)), 0, 0.7), (S.shankha(deg('S', 0), 3.0), 0.6, 0.4)])
@sfx('seal_open')
def _seal(): return seq([(S.lowpass(S.noise(S.n_of(2.5), 441), 500) * np.hanning(S.n_of(2.5)), 0, 0.8)] + [(S.bowl(deg(n, 1), 3.0), 0.8 + i * 0.1, 0.35) for i, n in enumerate(['S', 'P', 'S'])] + [(S.ghanta(deg('P', 2), 2.5), 1.0, 0.4)])
@sfx('health')
def _health(): return seq([(S.veena(deg(n, 1), 0.8, 1.0), i * 0.07, 0.7) for i, n in enumerate(['S', 'G', 'P', 'S'])])
@sfx('medkit')
def _mk(): return S.ghanta(deg('P', 2), 0.4) * 0.6 + P.zap(1200, 1800, 0.06, tau=0.025, harm=0.1) * 0.5
@sfx('repair')
def _rep(): return seq([(S.veena(np.linspace(deg('S', 1), deg('S', 2), 30), 0.7, 0.8), 0, 0.8), (S.jaltarang(deg('P', 2), 0.4), 0.3, 0.5)])
@sfx('piece_hum')
def _ph(): return S.voice(np.linspace(deg('S', 0), deg('S', 1), 40), 3.0, 'a', attack=1.5, release=0.6) * 0.5 + S.bowl(deg('S', 2), 3.0) * 0.3


def _hum_loop(parts, dur):
    """A bed that loops: every component is a whole number of cycles of its own frequency over `dur` seconds."""
    n = S.n_of(dur)
    t = np.arange(n) / S.SR
    x = np.zeros(n)
    for f, a, wob in parts:
        f = round(f * dur) / dur
        x += a * np.sin(TAU * f * t) * (1.0 + wob * np.sin(TAU * (round(0.7 * dur) / dur) * t))
    return S.Sig(x)


@sfx('lamp_hum')
def _lamphum():
    n = S.n_of(6.0)
    t = np.arange(n) / S.SR
    flick = 0.8 + 0.2 * np.sin(TAU * 3 / 6.0 * t) * np.sin(TAU * 11 / 6.0 * t)
    return _hum_loop([(deg('S', 1), 0.5, 0.15), (deg('P', 1), 0.28, 0.2), (deg('S', 2), 0.16, 0.3), (deg('G', 2), 0.08, 0.3)], 6.0) * flick


@sfx('bay_hum')
def _bayhum():
    return _hum_loop([(deg('M', -1), 0.55, 0.1), (deg('M', 0), 0.3, 0.1), (deg('S', 1), 0.14, 0.2), (deg('M', 1), 0.07, 0.25), (50.0, 0.25, 0.0)], 6.0)


# ============================================================================================================================ THE ENGINE
@sfx('engine_low')
def _elow(): return P.engine_rumble(34.0, 2.0)
@sfx('engine_whine')
def _ewhine(): return P.engine_whine(210.0, 2.0)
@sfx('thruster_loop')
def _thr(): return P.thruster(2.0)
@sfx('boost_loop')
def _bl(): return P.thruster(2.0, 451) * 0.9 + P.engine_rumble(46.0, 2.0) * 0.6
@sfx('boost')
def _boost(): return seq([(whoosh(0.6, 400, 4200, 0.9, 453), 0, 1.0), (rise(80, 380, 0.5, tau=0.3, harm=0.8, drive=2.0), 0, 0.8), (P.thump(55, 0.3, 2.5, 0.5, 0.1), 0, 0.7)])
@sfx('engine_loop')
def _eloop(): return P.engine_rumble(34.0, 2.0)


# ============================================================================================================================ THE MANTRAS: each astra's own words, sung
def S_(on, v, co, d, r): return (on, v, co, d, r)


MANTRA = {
    # Om Ham Akashaya Namah (ether): long open vowels, the voice drawn out over a shimmer
    'akasha': dict(base=1.0, voices=(1.0, 1.5), gap=0.01,
                   words=[S_('', 'o', 'm', .5, 1), S_('h', 'a', 'm', .45, 3 / 2), S_('', 'a', '', .26, 5 / 4), S_('k', 'a', '', .26, 3 / 2), S_('sh', 'a', '', .3, 5 / 3), S_('y', 'a', '', .26, 3 / 2),
                          S_('n', 'a', '', .3, 5 / 4), S_('m', 'a', 'h', .42, 1)]),
    # Om Yam Vayave Namah (wind): quick, light, high
    'vayu': dict(base=1.5, voices=(1.0, 2.0), gap=0.0,
                 words=[S_('', 'o', 'm', .32, 1), S_('y', 'a', 'm', .32, 9 / 8), S_('v', 'a', '', .18, 3 / 2), S_('y', 'a', '', .18, 5 / 3), S_('v', 'e', '', .24, 3 / 2), S_('n', 'a', '', .2, 5 / 4),
                        S_('m', 'a', 'h', .3, 9 / 8)]),
    # Om Ram Agnaye Namah (fire): staccato, rising, hard consonants
    'agni': dict(base=1.0, voices=(1.0, 1.0), gap=0.02,
                 words=[S_('', 'o', 'm', .36, 1), S_('r', 'a', 'm', .32, 5 / 4), S_('', 'a', '', .18, 3 / 2), S_('g', 'a', '', .2, 5 / 3), S_('n', 'a', '', .18, 3 / 2), S_('y', 'e', '', .22, 2),
                        S_('n', 'a', '', .2, 5 / 3), S_('m', 'a', 'h', .3, 3 / 2)]),
    # Om Vam Varunaya Namah (water): flowing, legato, undulating
    'jala': dict(base=0.95, voices=(1.0, 1.25), gap=0.0,
                 words=[S_('', 'o', 'm', .5, 1), S_('v', 'a', 'm', .45, 9 / 8), S_('v', 'a', '', .28, 4 / 3), S_('r', 'u', '', .28, 9 / 8), S_('n', 'a', '', .28, 1), S_('y', 'a', '', .28, 9 / 8),
                        S_('n', 'a', '', .3, 1), S_('m', 'a', 'h', .45, 8 / 9)]),
    # Om Lam Prithvyai Namah (earth): slow, low, heavy
    'prithvi': dict(base=0.5, voices=(1.0,), gap=0.04,
                    words=[S_('', 'o', 'm', .7, 1), S_('l', 'a', 'm', .62, 1), S_('p', 'i', '', .34, 6 / 5), S_('t', 'i', '', .34, 1), S_('v', 'e', '', .4, 6 / 5), S_('n', 'a', '', .4, 1),
                           S_('m', 'a', 'h', .6, 1)]),
    # Om Brahmastraya Phat (the Last Arrow): grand, a choir of five rising to the shout
    'brahma': dict(base=1.0, voices=(1.0, 5 / 4, 3 / 2, 2.0, 1.0), gap=0.0,
                   words=[S_('', 'o', 'm', .6, 1), S_('b', 'a', '', .26, 3 / 2), S_('r', 'a', '', .26, 5 / 3), S_('m', 'a', '', .26, 2), S_('s', 'a', '', .24, 5 / 3), S_('t', 'a', '', .22, 3 / 2),
                          S_('y', 'a', '', .26, 5 / 3), S_('ph', 'a', 't', .4, 2)]),
    # Om Pashupataye Namah (Shiva's weapon): slow, ominous, on the low
    'pashupata': dict(base=0.5, voices=(1.0, 1.0), gap=0.02,
                      words=[S_('', 'o', 'm', .75, 1), S_('p', 'a', '', .3, 1), S_('sh', 'u', '', .3, 9 / 8), S_('p', 'a', '', .3, 1), S_('t', 'a', '', .3, 6 / 5), S_('y', 'e', '', .3, 1),
                             S_('n', 'a', '', .4, 1), S_('m', 'a', 'h', .62, 1)]),
}
ASTRA = ['akasha', 'vayu', 'agni', 'jala', 'prithvi', 'brahma', 'pashupata']


def _phrase(el):
    m = MANTRA[el]
    base = LOKA_SA[0] * m['base']
    syl = [(w[0], w[1], w[2], w[3], 1.0 if i not in (0,) else 1.0) for i, w in enumerate(m['words'])]
    notes = [base * w[4] for w in m['words']]
    return P.chant(syl, notes, gap=m['gap'], voices=m['voices'], low=(el not in ('vayu',)))


def _bed(el):
    """What sounds under the mantra while the astra forms: its element's instrument, swelling."""
    d = 3.4
    if el == 'akasha': return S.overtone_voice(deg('S', -1), d, 1500.0) * 0.5 + S.bowl(deg('S', 2), d) * 0.3
    if el == 'vayu': return whoosh(d, 400, 3800, 0.45, 461) + S.bansuri(np.linspace(deg('P', 1), deg('P', 2), 30), d, breath=0.8, attack=1.5, release=0.4) * 0.3
    if el == 'agni': return seq([(S.damaru(2, 0.07, 170), i * (0.5 - 0.035 * i), 0.4 + 0.05 * i) for i in range(11)]) + S.highpass(S.noise(S.n_of(d), 463), 3000) * np.linspace(0, 0.1, S.n_of(d))
    if el == 'jala': return seq([(S.jaltarang(deg('R', 2) * (1 + 0.12 * (i % 3)), 0.4), i * 0.17, 0.35) for i in range(20)])
    if el == 'prithvi': return seq([(S.ghatam(150 + 10 * (i % 2), 0.3), i * 0.34, 0.6) for i in range(10)] + [(P.thump(50, 0.6, 2.0, 0.3, 0.2), i * 0.68, 0.8) for i in range(5)])
    if el == 'brahma': return S.shankha(deg('S', 0), d) * 0.3 + S.bowl(deg('S', 2), d) * 0.3 + S.voice(deg('S', -1), d, 'o', attack=1.5, release=1.0) * 0.3
    return S.overtone_voice(deg('S', -2), d, 700.0) * 0.6 + S.damaru(2, 0.07, 150) * 0.3


for _i, _el in enumerate(ASTRA):
    REG['astra_%d_chant' % _i] = (lambda el=_el: _phrase(el))
    REG['astra_%d_invoke' % _i] = (lambda el=_el: _bed(el))


def _shout(el):
    """The word that looses it: one syllable, shouted."""
    f = LOKA_SA[0] * MANTRA[el]['base'] * 2.0
    word = {'akasha': ('h', 'a', 'm'), 'vayu': ('y', 'a', 'm'), 'agni': ('r', 'a', 'm'), 'jala': ('v', 'a', 'm'), 'prithvi': ('l', 'a', 'm'), 'brahma': ('ph', 'a', 't'), 'pashupata': ('r', 'u', 'm')}[el]
    return P.chant([(word[0], word[1], word[2], 0.5, 1.0)], [f], voices=(1.0, 1.25), low=True)


@sfx('astra_0_loose')
def _l0(): return seq([(_shout('akasha'), 0, 0.9), (S.bowl(deg('S', 1), 3.0), 0.05, 0.8), (S.cymbal(1.0, 5000, 0.4, 471), 0.05, 0.4), (S.ghanta(deg('P', 2), 2.0), 0.05, 0.4)])
@sfx('astra_1_loose')
def _l1(): return seq([(_shout('vayu'), 0, 0.9), (whoosh(1.2, 400, 7000, 1.0, 473), 0, 1.0), (S.shankha(deg('P', 1), 1.0), 0.05, 0.3)])
@sfx('astra_2_loose')
def _l2(): return seq([(_shout('agni'), 0, 0.9), (P.blast(1.6, 70, 0.9, 475, 14), 0, 1.0), (S.nagara(65, 1.4), 0, 0.8), (S.highpass(S.noise(S.n_of(2.2), 477), 400) * np.hanning(S.n_of(2.2)), 0.1, 0.5)])
@sfx('astra_3_loose')
def _l3(): return seq([(_shout('jala'), 0, 0.9), (S.gong(deg('S', -1), 3.0), 0, 0.8), (S.lowpass(S.noise(S.n_of(3.0), 479), 1600) * np.hanning(S.n_of(3.0)), 0, 0.9), (S.santoor(deg('S', 3), 1.0), 0.2, 0.4)])
@sfx('astra_4_loose')
def _l4(): return seq([(_shout('prithvi'), 0, 0.9), (P.thump(42, 1.2, 3.0, 0.8, 0.3), 0, 1.0), (S.gong(deg('S', -2), 3.0), 0, 0.7), (S.lowpass(S.noise(S.n_of(2.0), 481), 500) * np.hanning(S.n_of(2.0)), 0.1, 0.8)])
@sfx('astra_5_loose')
def _l5(): return seq([(_shout('brahma'), 0, 1.0), (S.morph_voice(deg('S', -1), 4.0, ['o', 'm'], attack=0.1, release=2.0), 0.35, 0.8), (S.cymbal(3.0, 3500, 1.2, 483), 0.1, 0.7), (S.gong(deg('S', -1), 5.0), 0.1, 0.8), (S.shankha(deg('S', 1), 3.0), 0.35, 0.4)])
@sfx('astra_6_loose')
def _l6(): return seq([(_shout('pashupata'), 0, 1.0), (S.overtone_voice(deg('S', -2), 4.0, 900.0), 0.2, 0.8), (S.gong(deg('S', -3), 5.0), 0, 0.7), (P.thump(36, 1.2, 2.5, 0.5, 0.35), 0, 0.9)])
@sfx('astra_0_live')
def _v0(): return S.bowl(deg('S', 1), 3.0) * 0.5 + S.overtone_voice(deg('S', -1), 3.0, 1500.0) * 0.5
@sfx('astra_1_live')
def _v1(): return S.bandpass(S.noise(S.n_of(3.0), 485), 300, 3500) * 0.5 + whoosh(3.0, 600, 2400, 0.2, 486)
@sfx('astra_2_live')
def _v2(): return S.lowpass(S.noise(S.n_of(3.0), 487), 1800) * 0.7 * (0.7 + 0.3 * np.sin(S.TAU * 0.7 * np.arange(S.n_of(3.0)) / S.SR)) + S.highpass(S.noise(S.n_of(3.0), 488), 3500) * 0.12
@sfx('astra_3_live')
def _v3(): return S.bandpass(S.noise(S.n_of(3.0), 489), 200, 1200) * 0.6 + S.jaltarang(deg('S', 2), 3.0) * 0.2
@sfx('astra_4_live')
def _v4(): return S.lowpass(S.noise(S.n_of(3.0), 491), 200) * 1.0 + S.tanpura(deg('S', -2), 3.0) * 0.4
@sfx('astra_5_live')
def _v5(): return S.bowl(deg('S', 1), 3.0) * 0.4 + S.voice(deg('S', -1), 3.0, 'o', attack=1.0, release=1.0) * 0.4 + S.shankha(deg('S', 0), 3.0) * 0.2
@sfx('astra_6_live')
def _v6(): return S.overtone_voice(deg('S', -2), 3.0, 1000.0) * 0.9
