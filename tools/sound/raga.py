"""Swaras, tonics and the notation of the scores (docs/SOUND_PLAN.md 1.1-1.2).

A phrase is written as text: swaras separated by spaces, each  NAME[octave][:beats][~]
  NAME    S r R g G m M P d D n N      (lower case = komal, m = shuddha Ma, M = tivra Ma, as in the usual notation)
  octave  '.' after the name is one octave down, "'" one up (repeat for more)
  beats   duration in matras (default 1)
  ~       glide (meend) into the next note
  _       a rest ( _:2 is two beats )
  *       a gamaka: the note leans into its neighbours and back (Darbari's andolan)
The bar lines '|' are for the reader and are ignored.
"""
import re

RATIO = {'S': 1.0, 'r': 16 / 15, 'R': 9 / 8, 'g': 6 / 5, 'G': 5 / 4, 'm': 4 / 3, 'M': 45 / 32, 'P': 3 / 2, 'd': 8 / 5, 'D': 5 / 3, 'n': 9 / 5, 'N': 15 / 8}

# Sa of each loka: D3 and a semitone down per loka (the world descends, so the tonic descends)
D3 = 146.83
LOKA_SA = [D3 * 2 ** (-i / 12.0) for i in range(7)]
LOKAS = ['Atala', 'Vitala', 'Sutala', 'Talatala', 'Mahitala', 'Rasatala', 'Patala']

SWARAS = {
    'yaman': 'S R G M P D N',
    'bhoopali': 'S R G P D',
    'marwa': 'S r G M D N',
    'bhairav': 'S r G m P d N',
    'desh': 'S R m P N',
    'malkauns': 'S g m d n',
    'darbari': 'S R g m P d n',
    'shree': 'S r G M P d N',
    'todi': 'S r g M P d N',
}

_TOKEN = re.compile(r"^(?P<n>[SrRgGmMPdDnN_])(?P<o>[.']*)(?::(?P<d>[0-9.]+))?(?P<m>[~*]*)$")


def hz(name, octave, sa):
    return sa * RATIO[name] * (2.0 ** octave)


class Note:
    __slots__ = ('f', 'start', 'dur', 'glide', 'gamaka', 'name', 'octave')

    def __init__(self, f, start, dur, glide, gamaka, name, octave):
        self.f, self.start, self.dur, self.glide, self.gamaka, self.name, self.octave = f, start, dur, glide, gamaka, name, octave


def parse(text, sa, start=0.0):
    """Text -> list of Note (f is None for a rest), start and dur in beats."""
    out = []
    t = start
    for tok in text.replace('|', ' ').split():
        m = _TOKEN.match(tok)
        if not m:
            raise ValueError('bad token %r' % tok)
        n = m.group('n')
        o = m.group('o').count("'") - m.group('o').count('.')
        d = float(m.group('d')) if m.group('d') else 1.0
        flags = m.group('m')
        f = None if n == '_' else hz(n, o, sa)
        out.append(Note(f, t, d, '~' in flags, '*' in flags, n, o))
        t += d
    return out


def length(text):
    return sum(n.dur for n in parse(text, 1.0))
