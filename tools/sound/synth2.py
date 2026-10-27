"""More instruments (docs/SOUND_PLAN.md): punch (shots, thumps, blasts), the engine, and the voice that says a mantra. Builds on synth.py; every function returns a Sig."""
import numpy as np

import synth as S
from synth import SR, TAU, n_of, noise, lowpass, highpass, bandpass, phase_of, env_ar, normalise, Sig


def zap(f0, f1, dur, tau=None, crack=0.35, harm=0.3, drive=1.6, attack=0.0006):
    """A struck, pitch-falling (or rising) tone with a crack on the front: the body of a shot."""
    n = n_of(dur)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t / (dur * 0.22))
    ph = phase_of(f)
    x = np.sin(ph) + harm * np.sin(2 * ph + 0.4) + harm * 0.5 * np.sin(3 * ph)
    x = np.tanh(x * drive)
    env = np.exp(-t / (tau or dur * 0.34)) * np.clip(t / attack, 0, 1)
    out = x * env
    c = min(n, n_of(0.006))
    out[:c] += crack * np.asarray(highpass(noise(c, 91), 2500)) * np.linspace(1, 0, c)
    return Sig(out * env_ar(n, 0.0, 0.01, 1.0))


def thump(f=60.0, dur=0.3, drop=3.0, click=0.5, tau=0.09):
    """A kick: a sub sine falling fast, with a tick."""
    n = n_of(dur)
    t = np.arange(n) / SR
    fc = f * (1.0 + (drop - 1.0) * np.exp(-t / 0.025))
    x = np.sin(phase_of(fc)) * np.exp(-t / tau)
    c = min(n, n_of(0.004))
    x[:c] += click * np.asarray(highpass(noise(c, 93), 1500)) * np.linspace(1, 0, c)
    return Sig(np.tanh(x * 1.5) * env_ar(n, 0.0, 0.01, 1.0))


def blast(dur=0.6, body=70.0, bright=0.6, seed=95, debris=6):
    """A burst: a low thump under a fast bright crack and a slower dark roar, with a scatter of ringing debris."""
    n = n_of(dur)
    t = np.arange(n) / SR
    bright_n = np.asarray(highpass(noise(n, seed), 1200)) * np.exp(-t / (dur * 0.10)) * bright
    dark_n = np.asarray(lowpass(noise(n, seed + 1), 700)) * np.exp(-t / (dur * 0.30))
    out = bright_n + dark_n + np.asarray(thump(body, dur, 3.5, 0.0, dur * 0.18))[:n] * 1.4
    g = np.random.default_rng(seed + 2)
    for _ in range(debris):
        s0 = int(g.uniform(0.02, dur * 0.55) * SR)
        f = g.uniform(900, 3800)
        m = n_of(0.09)
        tt = np.arange(m) / SR
        tink = np.sin(TAU * f * tt) * np.exp(-tt / 0.025) * g.uniform(0.08, 0.2)
        e = min(n, s0 + m)
        if e > s0:
            out[s0:e] += tink[:e - s0]
    return Sig(np.tanh(out * 1.1) * env_ar(n, 0.0, dur * 0.2, 1.4))


def _loopfade(x, ms=250):
    k = n_of(ms / 1000.0)
    x = np.asarray(x).copy()
    x[:k] = x[:k] * np.linspace(0, 1, k) + x[len(x) - k:] * np.linspace(1, 0, k)
    return x


def engine_rumble(f=34.0, dur=2.0):
    """A low engine bed that loops: stacked harmonics on a whole number of cycles, a slow throb, and a dark roar."""
    cyc = max(1, int(round(f * dur)))
    f = cyc / dur
    n = n_of(dur)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for h in range(1, 24):
        x += np.sin(TAU * f * h * t + 0.7 * h) / h ** 0.85
    x *= 1.0 + 0.25 * np.sin(TAU * max(1, cyc // 4) / dur * t)
    roar = _loopfade(lowpass(noise(n, 97), 420))
    return Sig(normalise(x * 0.5 + roar * 1.2, 0.8))


def engine_whine(f=210.0, dur=2.0):
    """A turbine's whine for the hull's speed: a saw through a low-pass with a small wobble, on a whole number of cycles so it loops."""
    cyc = max(1, int(round(f * dur)))
    f = cyc / dur
    n = n_of(dur)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for h in range(1, 12):
        x += np.sin(TAU * f * h * t) / h
    return Sig(np.asarray(lowpass(x, 2400)) * 0.6)


def thruster(dur=2.0, seed=99):
    """A rushing exhaust that loops: band-limited noise, steady."""
    return Sig(_loopfade(bandpass(noise(dur and n_of(dur), seed), 250, 2600), 300))


# ============================================================================================================================ the voice that says a mantra
S._VOWELS['n'] = [(250, 60), (1500, 120), (2500, 150)]


def _burst(kind):
    if kind in ('p', 'b'):
        m = n_of(0.035)
        x = np.asarray(lowpass(noise(m, 101), 700)) * np.exp(-np.arange(m) / (0.01 * SR))
    elif kind in ('t', 'd'):
        m = n_of(0.03)
        x = np.asarray(highpass(noise(m, 102), 3200)) * np.exp(-np.arange(m) / (0.007 * SR))
    elif kind in ('k', 'g'):
        m = n_of(0.04)
        x = np.asarray(bandpass(noise(m, 103), 1200, 2800)) * np.exp(-np.arange(m) / (0.012 * SR))
    elif kind == 's':
        m = n_of(0.09)
        x = np.asarray(highpass(noise(m, 104), 4500)) * np.hanning(m)
    elif kind == 'sh':
        m = n_of(0.1)
        x = np.asarray(bandpass(noise(m, 105), 2200, 4800)) * np.hanning(m)
    elif kind == 'h':
        m = n_of(0.08)
        x = np.asarray(bandpass(noise(m, 106), 500, 3500)) * np.linspace(1, 0.2, m)
    elif kind == 'r':
        m = n_of(0.06)
        t = np.arange(m) / SR
        x = np.asarray(lowpass(noise(m, 107), 600)) * (0.55 + 0.45 * np.sin(TAU * 32 * t)) * np.hanning(m)
    else:
        return np.zeros(1)
    return x * (0.32 if kind in ('s', 'sh', 'h') else 0.7)


def syllable(f, dur, onset='', vowel='a', coda='', accent=1.0, vib=0.004, attack=0.03, release=0.1):
    """One sung syllable: an onset, a held vowel, a closing nasal or breath."""
    n = n_of(dur)
    if coda in ('m', 'n'):
        v = S.morph_voice(f, dur, [vowel, vowel, coda], attack=attack, release=release, vib=vib)
    else:
        v = S.voice(f, dur, vowel, attack=attack, release=release, vib=vib)
    out = np.zeros(max(n, len(v)))
    out[:len(v)] += np.asarray(v)[:len(out)] * accent
    if onset:
        voiced = {'l': 'u', 'v': 'u', 'y': 'i', 'n': 'n', 'm': 'm'}
        if onset in voiced:
            a = np.asarray(S.voice(f, 0.07, voiced[onset], attack=0.01, release=0.04))
            out[:len(a)] += a[:len(out)] * 0.8 * accent
        else:
            b = _burst(onset)
            out[:len(b)] += b[:len(out)] * accent
    if coda == 'h':
        m = n_of(0.12)
        b = np.asarray(bandpass(noise(m, 108), 500, 3000)) * np.linspace(0.8, 0, m)
        s0 = max(0, n - m)
        out[s0:s0 + len(b)] += b[:len(out) - s0] * 0.6
    return Sig(out)


def chant(syllables, notes, gap=0.0, voices=(1.0,), detune=0.004, low=True):
    """A sung phrase. syllables: [(onset, vowel, coda, seconds, accent)], notes: one Hz per syllable. The choir is `voices` (pitch multipliers), each a few cents apart, with the
    octave below underneath when `low`."""
    total = sum(s[3] for s in syllables) + gap * len(syllables) + 0.4
    n = n_of(total)
    out = np.zeros(n)
    norm = max(1.0, len(voices) ** 0.5)
    for vi, mult in enumerate(voices):
        t = 0.0
        det = 1.0 + detune * (vi - (len(voices) - 1) / 2.0) * 2.0
        for (on, vw, cd, d, ac), f in zip(syllables, notes):
            ff = f * mult * det
            x = np.asarray(syllable(ff, d + 0.05, on, vw, cd, ac))
            i = n_of(t)
            e = min(n, i + len(x))
            out[i:e] += x[:e - i] / norm
            if low:
                y = np.asarray(syllable(ff * 0.5, d + 0.05, on, vw, cd, ac * 0.6))
                out[i:e] += y[:e - i] * 0.55 / norm
            t += d + gap
    return Sig(out)
