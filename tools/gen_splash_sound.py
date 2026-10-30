# Jingle della splash LU3G TV (4 s, mono 44,1 kHz) con una variante per ogni tema stagionale.
# Uso: python3 -I gen_splash_sound.py CARTELLA_WAV   → scrive base + sei varianti; poi convertire con afconvert (vedi README).
import math, wave, struct, random, sys, os
SR = 44100; DUR = 4.0; N = int(SR * DUR)

MAJOR = [220.0, 277.18, 329.63, 440.0]        # La3 Do#4 Mi4 La4
MINOR = [220.0, 261.63, 329.63, 440.0]        # La3 Do4 Mi4 La4
LOW   = [164.81, 196.0, 246.94, 329.63]       # Mi3 Sol3 Si3 Mi4 (autunno)

def render(theme):
    buf = [0.0] * N
    rnd = random.Random(3)

    def tone(start, dur, freq, amp, harmonics=((1, 1.0), (2, 0.35), (3, 0.15)), attack=0.005, decay=None, vibrato=0.0):
        i0 = int(start * SR); n = int(dur * SR); dec = decay or dur / 3
        for k in range(n):
            t = k / SR
            env = min(1.0, t / attack) * math.exp(-t / dec)
            f = freq * (1 + vibrato * math.sin(2 * math.pi * 6 * t))
            v = sum(a * math.sin(2 * math.pi * f * h * t) for h, a in harmonics)
            idx = i0 + k
            if idx < N: buf[idx] += amp * env * v

    def noise_swell(start, dur, amp, lo=0.015, hi=0.09):
        y = 0.0; i0 = int(start * SR); n = int(dur * SR)
        for k in range(n):
            t = k / SR
            x = rnd.uniform(-1, 1)
            y += (lo + (hi - lo) * (t / dur)) * (x - y)
            e = math.sin(math.pi * min(1.0, t / dur)) ** 1.5
            idx = i0 + k
            if idx < N: buf[idx] += amp * e * y

    def pad(start, dur, freqs, amp, attack=0.35):
        i0 = int(start * SR); n = int(dur * SR)
        for f in freqs:
            for k in range(n):
                t = k / SR
                env = min(1.0, t / attack) * max(0.0, 1 - max(0.0, t - dur * 0.45) / (dur * 0.55))
                v = math.sin(2 * math.pi * f * t) + 0.5 * math.sin(2 * math.pi * f * 1.003 * t)
                idx = i0 + k
                if idx < N: buf[idx] += amp * env * v

    def bell(start, freq, amp, dur=0.9):
        # Campana: fondamentale + parziali non armonici
        tone(start, dur, freq, amp, harmonics=((1, 1.0), (2.76, 0.4), (5.4, 0.15)), decay=0.28)

    def sleigh(start, amp):
        # Sonaglio: rumore acuto brevissimo
        i0 = int(start * SR); n = int(0.09 * SR); prev = 0.0
        for k in range(n):
            t = k / SR
            x = rnd.uniform(-1, 1); hp = x - prev; prev = x     # passa-alto grezzo
            env = math.exp(-t / 0.025)
            idx = i0 + k
            if idx < N: buf[idx] += amp * env * hp

    notes = MINOR if theme == 'halloween' else LOW if theme == 'autunno' else MAJOR

    # 1. Soffio mentre i triangoli si compongono
    whoosh = {'halloween': 1.5, 'autunno': 1.5, 'san_valentino': 0.7}.get(theme, 1.1)
    noise_swell(0.0, 1.3 if theme not in ('halloween', 'autunno') else 2.0, whoosh)

    # 2. Quattro note, una per triangolo
    for j, f in enumerate(notes):
        tone(0.06 + 0.17 * j, 0.9, f, 0.22, decay=0.26, vibrato=0.004 if theme == 'halloween' else 0.0)

    # 3. Lettere: tocchi acuti + accordo
    if theme == 'natale':
        for j, f in enumerate([1318.5, 1568.0, 2093.0, 2637.0]): bell(1.15 + 0.12 * j, f, 0.07)
        pad(1.15, 2.6, [220.0, 329.63, 440.0, 554.37], 0.05)
    elif theme == 'halloween':
        for j, f in enumerate([880.0, 1046.5, 1318.5, 1760.0]): tone(1.15 + 0.12 * j, 0.6, f, 0.05, harmonics=((1, 1.0), (2, 0.2)), decay=0.15, vibrato=0.01)
        pad(1.15, 2.6, [220.0, 261.63, 329.63, 440.0], 0.055)             # La minore
    elif theme == 'san_valentino':
        for j, f in enumerate([523.25, 659.25, 783.99, 987.77, 1174.66]): tone(1.1 + 0.11 * j, 1.2, f, 0.06, harmonics=((1, 1.0), (2, 0.25), (3, 0.08)), decay=0.5)   # arpa Do maj7
        pad(1.15, 2.6, [261.63, 329.63, 392.0, 493.88], 0.045)
    elif theme == 'autunno':
        for j, f in enumerate([659.25, 783.99, 987.77, 1318.5]): tone(1.2 + 0.14 * j, 0.7, f, 0.05, harmonics=((1, 1.0), (2, 0.2)), decay=0.2)
        pad(1.15, 2.6, [164.81, 246.94, 329.63, 415.3], 0.06, attack=0.5)
    else:
        for j, f in enumerate([880.0, 1108.7, 1318.5, 1760.0]): tone(1.15 + 0.12 * j, 0.5, f, 0.06, harmonics=((1, 1.0), (2, 0.2)), decay=0.12)
        pad(1.15, 2.6, [220.0, 329.63, 440.0, 554.37], 0.055)

    # 4. Riflesso: scintillio (o variante del tema)
    if theme == 'halloween':
        # Glissando "da theremin" con vibrato
        i0 = int(1.9 * SR); n = int(0.9 * SR); ph = 0.0
        for k in range(n):
            t = k / SR
            f = 520 + 260 * (t / 0.9) + 14 * math.sin(2 * math.pi * 6 * t)
            ph += 2 * math.pi * f / SR
            env = min(1.0, t / 0.25) * max(0.0, 1 - max(0.0, t - 0.5) / 0.4)
            idx = i0 + k
            if idx < N: buf[idx] += 0.07 * env * math.sin(ph)
    elif theme == 'pasqua':
        for st in (1.95, 2.25, 2.55):                                       # cinguettii: due blip rapidi
            for d, f in ((0.0, 2200.0), (0.07, 2800.0)): tone(st + d, 0.08, f, 0.06, harmonics=((1, 1.0),), decay=0.03)
    else:
        boost = 1.8 if theme == 'estate' else 1.0
        for st, f in [(1.95, 2637.0), (2.15, 3136.0), (2.35, 3520.0)]:
            tone(st, 0.6, f, 0.05 * boost, harmonics=((1, 1.0), (1.5, 0.3)), decay=0.18)

    # 5. Respiro del logo: tocco basso (o bordone/onda)
    if theme == 'halloween':
        i0 = 0; n = int(3.4 * SR)
        for k in range(n):                                                 # bordone cupo con tremolo
            t = k / SR
            env = min(1.0, t / 0.8) * max(0.0, 1 - max(0.0, t - 2.4) / 1.0)
            trem = 0.6 + 0.4 * math.sin(2 * math.pi * 4.5 * t)
            idx = i0 + k
            if idx < N: buf[idx] += 0.11 * env * trem * math.sin(2 * math.pi * 55.0 * t)
    elif theme == 'estate':
        noise_swell(2.3, 1.4, 0.9, lo=0.01, hi=0.03)                       # onda che si infrange
    else:
        tone(2.6, 0.8, 55.0, 0.28, harmonics=((1, 1.0), (2, 0.3)), attack=0.03, decay=0.3)

    # Natale: sonagli per tutta la durata, a ritmo
    if theme == 'natale':
        st = 0.3
        while st < 3.3:
            sleigh(st, 0.16); sleigh(st + 0.1, 0.09); st += 0.2

    # Master: limitatore morbido, dissolvenza finale, normalizzazione a -1,5 dB
    peak = max(abs(math.tanh(v * 1.3)) for v in buf) or 1.0
    gain = 0.84 / peak
    out = bytearray()
    for i, v in enumerate(buf):
        t = i / SR
        fade = 1.0 if t < 3.4 else max(0.0, (4.0 - t) / 0.6)
        v = math.tanh(v * 1.3) * gain * fade
        out += struct.pack('<h', int(max(-1.0, min(1.0, v)) * 32767))
    return bytes(out)

if __name__ == '__main__':
    outdir = sys.argv[1]
    for theme, name in [('base', 'splash_sound'), ('halloween', 'splash_halloween'), ('natale', 'splash_natale'),
                        ('san_valentino', 'splash_san_valentino'), ('pasqua', 'splash_pasqua'), ('estate', 'splash_estate'), ('autunno', 'splash_autunno')]:
        data = render(theme)
        with wave.open(os.path.join(outdir, name + '.wav'), 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data)
        print('ok', name)
