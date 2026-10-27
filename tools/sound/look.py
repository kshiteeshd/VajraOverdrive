"""Look at sounds (there is no ear in the build): waveform and spectrogram sheets, and numbers (attack, length, centroid, loudness).
   python tools/sound/look.py out.png name [name ...]      (names under assets/audio/sfx, or music/<name>)"""
import os
import subprocess
import sys

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'audio')
SR = 22050


def load(name):
    path = os.path.join(ROOT, name if name.startswith('music/') else 'sfx/' + name) + '.ogg'
    raw = subprocess.run(['ffmpeg', '-loglevel', 'error', '-i', path, '-f', 'f32le', '-ac', '1', '-ar', str(SR), '-'], capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32)


def stats(x):
    a = np.abs(x)
    pk = float(a.max())
    att = int(np.argmax(a > 0.5 * pk)) / SR * 1000
    spec = np.abs(np.fft.rfft(x * np.hanning(len(x))))
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    cen = float((spec * fr).sum() / max(spec.sum(), 1e-9))
    return dict(dur=len(x) / SR, peak=pk, rms=float(np.sqrt((x ** 2).mean())), attack_ms=att, centroid=cen)


if __name__ == '__main__':
    out, names = sys.argv[1], sys.argv[2:]
    fig, axes = plt.subplots(len(names), 2, figsize=(11, 1.7 * len(names)), gridspec_kw={'width_ratios': [1, 2]})
    if len(names) == 1:
        axes = np.array([axes])
    for i, n in enumerate(names):
        x = load(n)
        s = stats(x)
        t = np.arange(len(x)) / SR
        axes[i, 0].plot(t, x, lw=0.4)
        axes[i, 0].set_title('%s  %.2fs att %.0fms cen %.0fHz rms %.2f' % (n, s['dur'], s['attack_ms'], s['centroid'], s['rms']), fontsize=7)
        axes[i, 0].set_ylim(-1, 1)
        axes[i, 1].specgram(x, NFFT=512, Fs=SR, noverlap=384, cmap='magma', vmin=-110)
        axes[i, 1].set_ylim(0, 9000)
        for a in axes[i]:
            a.tick_params(labelsize=6)
    plt.tight_layout()
    plt.savefig(out, dpi=90)
    print('saved', out)
