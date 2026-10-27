"""The instruments of Vajra Overdrive, as written code: additive and physical-model voices of the Indian palette (docs/SOUND_PLAN.md section 2) and a stereo temple reverb.

Everything is deterministic (a seeded generator for the noise inside a drum or a breath), so a score always renders to the same sound. Nothing here composes: the notes come from
the scores (music.py, sfx.py).
"""
import os
import subprocess
import wave

import numpy as np
from scipy import signal

SR = 44100
TAU = 2.0 * np.pi
_RNG = np.random.default_rng(1108)

FFMPEG = os.environ.get('FFMPEG', 'ffmpeg')


# ---------------------------------------------------------------------------------------------------------------- basics
def n_of(dur):
    return max(1, int(round(dur * SR)))


def noise(n, seed=None):
    g = _RNG if seed is None else np.random.default_rng(seed)
    return g.standard_normal(n).astype(np.float64)


def lowpass(x, fc, order=2):
    fc = min(fc, SR * 0.45)
    b, a = signal.butter(order, fc / (SR / 2), 'low')
    return signal.lfilter(b, a, x)


def highpass(x, fc, order=2):
    b, a = signal.butter(order, min(fc, SR * 0.45) / (SR / 2), 'high')
    return signal.lfilter(b, a, x)


def bandpass(x, lo, hi, order=2):
    b, a = signal.butter(order, [min(lo, SR * 0.44) / (SR / 2), min(hi, SR * 0.45) / (SR / 2)], 'band')
    return signal.lfilter(b, a, x)


def resonator(x, fc, q=8.0):
    """A two-pole resonance (a formant, a body mode)."""
    w = TAU * fc / SR
    r = np.exp(-np.pi * fc / (q * SR))
    a = [1.0, -2.0 * r * np.cos(w), r * r]
    return signal.lfilter([1.0 - r], a, x)


def env_ar(n, a, r, curve=2.0):
    """Attack then release shaped as a power curve."""
    e = np.ones(n)
    na, nr = min(n, n_of(a)), min(n, n_of(r))
    if na > 0:
        e[:na] = np.linspace(0, 1, na) ** curve
    if nr > 0:
        e[n - nr:] *= np.linspace(1, 0, nr) ** curve
    return e


def exp_decay(n, tau):
    return np.exp(-np.arange(n) / (tau * SR))


def curve(f, n):
    """A frequency as a constant or a (time-stretched) list of values, as an n-sample array."""
    if np.isscalar(f):
        return np.full(n, float(f))
    f = np.asarray(f, dtype=float)
    return np.interp(np.linspace(0, len(f) - 1, n), np.arange(len(f)), f)


def phase_of(f):
    return TAU * np.cumsum(f) / SR


def vibrato(n, rate, depth, delay=0.0, seed=0):
    t = np.arange(n) / SR
    ramp = np.clip((t - delay) / 0.4, 0, 1) if delay else 1.0
    return 1.0 + depth * ramp * np.sin(TAU * rate * t + seed)


def normalise(x, peak=0.9):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 1e-9 else x


# ---------------------------------------------------------------------------------------------------------------- strings
def harmonic(f, dur, amps, decays, attack=0.003, release=0.05, inharm=0.0):
    """Additive tone: amps[k] and decays[k] (seconds) for harmonic k+1; f may be a pitch curve."""
    n = n_of(dur)
    fc = curve(f, n)
    ph = phase_of(fc)
    out = np.zeros(n)
    t = np.arange(n) / SR
    for k, (a, d) in enumerate(zip(amps, decays)):
        h = k + 1
        stretch = h * np.sqrt(1.0 + inharm * h * h)
        if np.max(fc) * stretch > SR * 0.45:
            break
        out += a * np.sin(ph * stretch) * np.exp(-t / d)
    return out * env_ar(n, attack, release)


def pluck(f, dur, bright=0.5, decay=1.2, inharm=0.0002):
    """A struck or plucked string: veena, sarod, santoor-like when bright."""
    k = 22
    amps = [(1.0 / (h ** (1.0 + 1.6 * (1.0 - bright)))) for h in range(1, k + 1)]
    decays = [decay / (1.0 + 0.55 * (h - 1) * (1.2 - 0.6 * bright)) for h in range(1, k + 1)]
    return harmonic(f, dur, amps, decays, attack=0.002, release=0.08, inharm=inharm)


def veena(f, dur, decay=1.6):
    x = pluck(f, dur, bright=0.55, decay=decay)
    n = len(x)
    body = resonator(x, 240.0, 6.0) * 0.4
    return x + body[:n]


def sarod(f, dur, decay=1.0):
    x = pluck(f, dur, bright=0.75, decay=decay)
    return x + 0.25 * highpass(x, 1800)


def tanpura(f, dur=6.0):
    """The drone string: a long pluck with the jawari buzz (a bridge that lets the harmonics sing)."""
    k = 36
    amps = [1.0 / (1.0 + 0.04 * (h - 6) ** 2) ** 0.5 * (1.0 / h ** 0.4) for h in range(1, k + 1)]
    decays = [dur * 0.9 / (1.0 + 0.09 * h) for h in range(1, k + 1)]
    x = harmonic(f, dur, amps, decays, attack=0.012, release=0.4, inharm=0.00005)
    return x * (0.7 + 0.3 * np.sin(TAU * 0.35 * np.arange(len(x)) / SR + 1.0)) * 0.5


def santoor(f, dur=1.4):
    n = n_of(dur)
    out = np.zeros(n)
    t = np.arange(n) / SR
    for r, a, d in [(1, 1.0, 0.9), (2.005, 0.6, 0.55), (3.02, 0.45, 0.4), (4.13, 0.3, 0.3), (5.4, 0.2, 0.2), (6.9, 0.12, 0.15)]:
        out += a * np.sin(TAU * f * r * t + 0.3 * r) * np.exp(-t / d)
    click = highpass(noise(n, 5) * np.exp(-t / 0.006), 3000) * 0.3
    return (out + click) * env_ar(n, 0.001, 0.05)


def jaltarang(f, dur=1.6):
    """Tuned bowls of water: a pure ring with a soft wobble."""
    n = n_of(dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for r, a, d in [(1, 1.0, 1.1), (2.6, 0.28, 0.5), (4.8, 0.1, 0.25)]:
        wob = 1.0 + 0.003 * np.sin(TAU * 5.5 * t)
        out += a * np.sin(TAU * f * r * t * wob) * np.exp(-t / d)
    return out * env_ar(n, 0.004, 0.08)


# ---------------------------------------------------------------------------------------------------------------- winds and bows
def bansuri(f, dur, breath=0.18, vib=0.0035, attack=0.09, release=0.14):
    n = n_of(dur)
    fc = curve(f, n) * vibrato(n, 5.2, vib, delay=0.25)
    ph = phase_of(fc)
    tone_ = np.sin(ph) + 0.32 * np.sin(2 * ph + 0.4) + 0.1 * np.sin(3 * ph + 1.0) + 0.04 * np.sin(4 * ph)
    air = bandpass(noise(n), max(200.0, float(np.mean(fc)) * 2.0), float(np.mean(fc)) * 7.0 + 800.0)
    env = env_ar(n, attack, release, 1.6)
    swell = 0.8 + 0.2 * np.sin(TAU * 0.5 * np.arange(n) / SR)
    return (tone_ + breath * air * 3.0) * env * swell * 0.5


def sarangi(f, dur, vib=0.006, attack=0.14, release=0.2, rosin=0.12):
    """A bowed string that cries: saw harmonics through two body formants, with rosin noise."""
    n = n_of(dur)
    fc = curve(f, n) * vibrato(n, 5.8, vib, delay=0.2)
    ph = phase_of(fc)
    x = np.zeros(n)
    for h in range(1, 28):
        if np.max(fc) * h > SR * 0.45:
            break
        x += np.sin(ph * h + 0.1 * h) / h ** 0.9
    x = resonator(x, 600.0, 3.0) * 1.6 + resonator(x, 1500.0, 4.0) * 1.0 + x * 0.3
    x += rosin * highpass(noise(n), 2500) * 0.25
    return x * env_ar(n, attack, release, 1.4) * 0.35


def shankha(f, dur=2.2):
    """The conch: a rich buzzing swell that bends up into the note."""
    n = n_of(dur)
    t = np.arange(n) / SR
    fc = curve(f, n) * (1.0 - 0.04 * np.exp(-t / 0.25))
    ph = phase_of(fc)
    x = np.zeros(n)
    for h in range(1, 20):
        x += np.sin(ph * h + 0.3 * h * h) / h ** 0.75
    x = lowpass(x, 2600) + 0.1 * bandpass(noise(n), 800, 3000)
    return x * env_ar(n, 0.25, 0.7, 1.5) * 0.4


# ---------------------------------------------------------------------------------------------------------------- bells, bowls, metal
def bell(f, dur=2.5, partials=None):
    partials = partials or [(1.0, 1.0, 1.0), (2.0, 0.55, 0.8), (2.42, 0.4, 0.6), (3.0, 0.3, 0.5), (4.1, 0.22, 0.35), (5.4, 0.12, 0.25)]
    n = n_of(dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for r, a, d in partials:
        out += a * np.sin(TAU * f * r * t) * np.exp(-t / (dur * 0.38 * d))
    return out * env_ar(n, 0.001, 0.2) * 0.5


def ghanta(f, dur=2.2):
    """A temple bell: struck, bright, with a beating pair."""
    n = n_of(dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for r, a, d in [(1, 1, 1.0), (1.007, 0.8, 1.0), (2.76, 0.5, 0.6), (5.4, 0.3, 0.35), (8.9, 0.18, 0.2)]:
        out += a * np.sin(TAU * f * r * t) * np.exp(-t / (dur * 0.4 * d))
    return out * env_ar(n, 0.0008, 0.2) * 0.4


def bowl(f, dur=4.0):
    """A singing bowl: a long pure ring, slow beating."""
    n = n_of(dur)
    t = np.arange(n) / SR
    out = np.sin(TAU * f * t) * np.exp(-t / (dur * 0.5)) + 0.7 * np.sin(TAU * f * 1.006 * t) * np.exp(-t / (dur * 0.5))
    out += 0.25 * np.sin(TAU * f * 2.71 * t) * np.exp(-t / (dur * 0.2))
    return out * env_ar(n, 0.02, 0.4) * 0.4


def gong(f, dur=5.0):
    n = n_of(dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for r, a, d in [(1, 1.0, 1.0), (1.47, 0.7, 0.9), (2.09, 0.5, 0.8), (2.56, 0.4, 0.7), (3.4, 0.3, 0.5), (4.3, 0.2, 0.4)]:
        out += a * np.sin(TAU * f * r * t * (1.0 + 0.002 * np.sin(TAU * 2.0 * t))) * np.exp(-t / (dur * 0.35 * d))
    out += 0.4 * bandpass(noise(n, 9) * np.exp(-t / 0.15), 300, 3000)
    return out * env_ar(n, 0.004, 0.4) * 0.35


def cymbal(dur=1.2, bright=6000.0, tau=0.35, seed=3):
    n = n_of(dur)
    t = np.arange(n) / SR
    x = highpass(noise(n, seed), bright * 0.5) * np.exp(-t / tau)
    x += 0.5 * bandpass(noise(n, seed + 1), bright, bright * 1.8) * np.exp(-t / (tau * 0.6))
    return x * 0.3


# ---------------------------------------------------------------------------------------------------------------- drums
def membrane(f, dur, sweep=1.6, tau=0.3, click=0.5, ring=0.3, seed=1):
    n = n_of(dur)
    t = np.arange(n) / SR
    fc = f * (1.0 + (sweep - 1.0) * np.exp(-t / 0.04))
    ph = phase_of(fc)
    body = (np.sin(ph) + 0.35 * np.sin(2.3 * ph + 0.5) + 0.15 * np.sin(3.6 * ph)) * np.exp(-t / tau)
    thump = lowpass(noise(n, seed) * np.exp(-t / 0.012), 1500) * click
    rg = ring * np.sin(ph * 2.76) * np.exp(-t / (tau * 1.7))
    return (body + thump + rg) * env_ar(n, 0.0006, 0.02)


def pakhawaj(f=80.0, dur=0.9, ring=0.35):
    """The royal bass drum: deep, with the black-paste ring on top."""
    return membrane(f, dur, sweep=1.5, tau=0.38, click=0.4, ring=ring, seed=21)


def tabla(kind, f=300.0):
    """Syllables: na, tin, tun, ge (bayan), dha (na + ge), tak (a flat slap)."""
    if kind == 'ge':
        return membrane(f * 0.3, 0.5, sweep=1.8, tau=0.26, click=0.3, ring=0.1, seed=31)
    if kind == 'na':
        return membrane(f, 0.35, sweep=1.04, tau=0.11, click=0.3, ring=0.5, seed=32) * 0.8
    if kind == 'tin':
        return membrane(f * 1.6, 0.32, sweep=1.03, tau=0.14, click=0.2, ring=0.6, seed=33) * 0.7
    if kind == 'tun':
        return membrane(f * 1.2, 0.55, sweep=1.05, tau=0.22, click=0.2, ring=0.4, seed=34) * 0.8
    if kind == 'tak':
        n = n_of(0.12)
        t = np.arange(n) / SR
        return (highpass(noise(n, 35), 1800) * np.exp(-t / 0.02) + 0.5 * np.sin(TAU * 900 * t) * np.exp(-t / 0.015)) * 0.6
    if kind == 'dha':
        a, b = tabla('na', f), tabla('ge', f)
        n = max(len(a), len(b))
        out = np.zeros(n)
        out[:len(a)] += a
        out[:len(b)] += b
        return out
    raise ValueError(kind)


def dholak(f=150.0, dur=0.4):
    return membrane(f, dur, sweep=1.3, tau=0.15, click=0.5, ring=0.15, seed=41)


def nagara(f=75.0, dur=1.2):
    return membrane(f, dur, sweep=1.4, tau=0.55, click=0.7, ring=0.2, seed=43)


def damaru(beats=2, gap=0.07, f=170.0):
    """The hand drum of Shiva: two rattling pellets on a pinched skin."""
    n = n_of(beats * gap + 0.25)
    out = np.zeros(n)
    for i in range(beats):
        hit = membrane(f * (1.05 if i % 2 else 1.0), 0.22, sweep=1.15, tau=0.05, click=0.9, ring=0.2, seed=50 + i)
        s = int(i * gap * SR)
        out[s:s + len(hit)] += hit[:max(0, n - s)] if s + len(hit) > n else hit
    return out * 0.8


def ghatam(f=330.0, dur=0.35):
    """The clay pot: a short hollow tone with a crack."""
    n = n_of(dur)
    t = np.arange(n) / SR
    tone_ = np.sin(TAU * f * t * (1.0 + 0.2 * np.exp(-t / 0.02))) * np.exp(-t / 0.07)
    tone_ += 0.5 * np.sin(TAU * f * 2.45 * t) * np.exp(-t / 0.04)
    crack = bandpass(noise(n, 61) * np.exp(-t / 0.008), 1500, 6000) * 0.5
    return (tone_ + crack) * 0.8


def kartal(dur=0.12, seed=70):
    n = n_of(dur)
    t = np.arange(n) / SR
    return highpass(noise(n, seed), 3500) * np.exp(-t / 0.025) * 0.5


def ghungroo(dur=0.5, count=7, seed=80):
    """A bunch of tiny bells shaken: a scatter of fast bright pings."""
    g = np.random.default_rng(seed)
    n = n_of(dur)
    out = np.zeros(n)
    for i in range(count):
        s = int(g.uniform(0, dur * 0.7) * SR)
        f = g.uniform(3200, 5200)
        m = n_of(0.12)
        t = np.arange(m) / SR
        ping = np.sin(TAU * f * t) * np.exp(-t / 0.03) * g.uniform(0.3, 0.8)
        e = min(n, s + m)
        out[s:e] += ping[:e - s]
    return out * 0.35


# ---------------------------------------------------------------------------------------------------------------- voice
_VOWELS = {
    'o': [(450, 80), (800, 90), (2830, 120)],
    'a': [(730, 90), (1090, 110), (2440, 120)],
    'u': [(325, 70), (700, 90), (2530, 120)],
    'e': [(530, 80), (1840, 100), (2480, 120)],
    'i': [(270, 60), (2290, 100), (3010, 120)],
    'm': [(250, 60), (1200, 150), (2200, 200)],
}


def voice(f, dur, vowel='o', vib=0.004, attack=0.12, release=0.2, bright=1.0, breathy=0.05):
    """A sung vowel: a glottal pulse train through formants. Male register by default (f ~ 90-200 Hz)."""
    n = n_of(dur)
    fc = curve(f, n) * vibrato(n, 5.0, vib, delay=0.3)
    ph = phase_of(fc)
    src = np.zeros(n)
    for h in range(1, 40):
        if np.max(fc) * h > SR * 0.44:
            break
        src += np.sin(ph * h) / h ** (1.0 + 0.3 / max(0.2, bright))
    src += breathy * noise(n)
    out = np.zeros(n)
    for i, (fm, bw) in enumerate(_VOWELS[vowel]):
        out += resonator(src, fm, fm / bw) * (1.0, 0.7, 0.35)[i] * 3.0
    return out * env_ar(n, attack, release, 1.5) * 0.3


def morph_voice(f, dur, vowels, **kw):
    """Several vowels in turn, crossfaded (OM = o then m)."""
    n = n_of(dur)
    parts = [voice(f, dur, v, **kw) for v in vowels]
    out = np.zeros(n)
    k = len(parts)
    for i, p in enumerate(parts):
        w = np.zeros(n)
        c = (i + 0.5) / k * n
        sp = n / k
        x = np.arange(n)
        w = np.clip(1.0 - np.abs(x - c) / sp, 0, 1)
        out += p[:n] * w
    return out


def overtone_voice(f, dur, formant=1400.0, vib=0.002):
    """Throat-singing: a low drone with one formant swept over the harmonics, whistling a melody on top."""
    n = n_of(dur)
    fc = curve(f, n)
    ph = phase_of(fc)
    src = np.zeros(n)
    for h in range(1, 60):
        if np.max(fc) * h > SR * 0.44:
            break
        src += np.sin(ph * h) / h ** 0.6
    y = resonator(src, formant, 30.0) * 4.0 + 0.3 * lowpass(src, 400)
    return y * env_ar(n, 0.3, 0.4) * 0.3


# ---------------------------------------------------------------------------------------------------------------- mixing
def pan_gains(p):
    a = (p + 1.0) * np.pi / 4.0
    return np.cos(a), np.sin(a)


class Mix:
    """A stereo canvas; sounds are laid in at a time with a gain and a pan."""

    def __init__(self, dur, tail=3.0):
        self.n = n_of(dur + tail)
        self.dur = dur
        self.buf = np.zeros((self.n, 2))

    def add(self, x, t=0.0, gain=1.0, pan=0.0):
        s = int(round(t * SR))
        if s < 0:
            x = x[-s:]
            s = 0
        if s >= self.n:
            return
        e = min(self.n, s + len(x))
        if e <= s:
            return
        l, r = pan_gains(pan)
        self.buf[s:e, 0] += x[:e - s] * gain * l
        self.buf[s:e, 1] += x[:e - s] * gain * r

    def reverb(self, wet=0.25, decay=2.2, damp=4500.0, seed=7, predelay=0.02):
        ir = reverb_ir(decay, damp, seed, predelay)
        w = np.stack([signal.fftconvolve(self.buf[:, c], ir[:, c])[:self.n] for c in range(2)], axis=1)
        self.buf = self.buf * (1.0 - wet * 0.5) + w * wet
        return self

    def loop_fold(self):
        """Fold the tail back over the start so the file loops with no seam; returns the loop (dur long)."""
        n = n_of(self.dur)
        out = self.buf[:n].copy()
        tail = self.buf[n:]
        m = min(len(tail), n)
        out[:m] += tail[:m]
        return out

    def out(self):
        return self.buf[:n_of(self.dur)]


def reverb_ir(decay=2.2, damp=4500.0, seed=7, predelay=0.02):
    n = n_of(decay * 1.5 + predelay)
    g = np.random.default_rng(seed)
    t = np.arange(n) / SR
    ir = np.zeros((n, 2))
    for c in range(2):
        x = g.standard_normal(n) * np.exp(-t / (decay / 6.9))
        x = lowpass(x, damp, 1)
        x[:int(predelay * SR)] = 0
        ir[:, c] = x
    ir /= np.sqrt(np.sum(ir ** 2)) * 0.35
    return ir


def soft_clip(x, drive=1.0):
    return np.tanh(x * drive) / np.tanh(drive)


# ---------------------------------------------------------------------------------------------------------------- writing
def write_wav(path, x, sr=SR):
    x = np.asarray(x)
    if x.ndim == 1:
        x = x[:, None]
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2')
    with wave.open(path, 'wb') as w:
        w.setnchannels(pcm.shape[1])
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())


def to_ogg(wav_path, ogg_path, quality=4, mono=False):
    cmd = [FFMPEG, '-y', '-loglevel', 'error', '-i', wav_path] + (['-ac', '1'] if mono else []) + ['-c:a', 'libvorbis', '-q:a', str(quality), ogg_path]
    subprocess.run(cmd, check=True)


def save(path_ogg, x, quality=4, peak=0.89, mono=None):
    """Normalise, write through a temporary wav, encode to ogg."""
    x = np.asarray(x, dtype=float)
    x = normalise(x, peak)
    is_mono = x.ndim == 1 if mono is None else mono
    os.makedirs(os.path.dirname(path_ogg), exist_ok=True)
    tmp = path_ogg[:-4] + '.tmp.wav'
    write_wav(tmp, x)
    to_ogg(tmp, path_ogg, quality, mono=is_mono)
    os.remove(tmp)


def trim_silence(x, thresh=0.002):
    idx = np.where(np.abs(x) > thresh)[0]
    return x[:idx[-1] + 1] if len(idx) else x


# ---------------------------------------------------------------------------------------------------------------- signals of unequal length add (padded with silence)
class Sig(np.ndarray):
    """A mono signal. Adding two of different lengths pads the shorter with silence, so a recipe can simply lay one sound over another."""

    def __new__(cls, a):
        return np.asarray(a, dtype=float).view(cls)

    def _pad(self, other):
        o = np.asarray(other)
        if o.ndim == 1 and self.ndim == 1 and o.shape != self.shape:
            n = max(len(self), len(o))
            a = np.zeros(n)
            b = np.zeros(n)
            a[:len(self)] = np.asarray(self)
            b[:len(o)] = o
            return Sig(a), b
        return self, o

    def __add__(self, other):
        a, b = self._pad(other)
        return np.add(a, b).view(Sig)

    def __radd__(self, other):
        a, b = self._pad(other)
        return np.add(b, a).view(Sig)

    def __sub__(self, other):
        a, b = self._pad(other)
        return np.subtract(a, b).view(Sig)


def _wrap(fn):
    def inner(*a, **k):
        r = fn(*a, **k)
        return Sig(r) if isinstance(r, np.ndarray) and r.ndim == 1 else r
    inner.__name__ = fn.__name__
    inner.__doc__ = fn.__doc__
    return inner


for _name in ['harmonic', 'pluck', 'veena', 'sarod', 'tanpura', 'santoor', 'jaltarang', 'bansuri', 'sarangi', 'shankha', 'bell', 'ghanta', 'bowl', 'gong', 'cymbal', 'membrane',
              'pakhawaj', 'tabla', 'dholak', 'nagara', 'damaru', 'ghatam', 'kartal', 'ghungroo', 'voice', 'morph_voice', 'overtone_voice', 'noise', 'lowpass', 'highpass', 'bandpass',
              'resonator']:
    globals()[_name] = _wrap(globals()[_name])
