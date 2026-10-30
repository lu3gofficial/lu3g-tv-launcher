#!/usr/bin/env python3
"""Genera le clip vocali dell'assistente con la voce neurale italiana "Paola" (Piper).

Le frasi vengono lette da app/src/main/java/it/lu3g/tvlauncher/voice/VoiceCategories.kt
(liste `phrases = listOf(...)`, campi `confirm = "..."` e la costante `SNOOZE_PHRASE`), così il
file Kotlin resta l'unica fonte: si aggiunge una frase lì e si rilancia questo script.

Ogni frase diventa app/src/main/res/raw/voice_<hash>.m4a, dove <hash> sono i primi 10 caratteri
esadecimali dello SHA-1 del testo in UTF-8. L'app calcola lo stesso hash (VoiceAssistant.clipId) e,
se la clip manca, ripiega sulla sintesi vocale di Android. Sulla TV non gira nessun modello:
si riproduce solo un file audio, a costo zero per la CPU.

Uso:  python3 -I tools/gen_voice_clips.py [--force] [--dry-run]
  --force    rigenera anche le clip già presenti
  --dry-run  mostra solo cosa farebbe

Al primo avvio crea tools/.piper/ (ignorata da git): un venv con piper-tts e il modello
it_IT-paola-medium (circa 65 MB, scaricato da Hugging Face). Serve `afconvert` (macOS) o `ffmpeg`.
"""
import hashlib
import json
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "app/src/main/java/it/lu3g/tvlauncher/voice/VoiceCategories.kt"
RAW = ROOT / "app/src/main/res/raw"
WORK = ROOT / "tools/.piper"
VENV = WORK / "venv"
MODELS = WORK / "models"
PIPER_VERSION = "1.8.0"
MODEL = "it_IT-paola-medium"
MODEL_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/it/it_IT/paola/medium/"
# Parametri di sintesi (gli stessi del campione approvato): un po' più lenta e pulita del default.
SYNTH = {"length_scale": 1.05, "noise_scale": 0.55, "noise_w_scale": 0.7}

STRING = re.compile(r'"((?:[^"\\]|\\.)*)"')

# Programma eseguito nel venv di Piper: carica il modello una volta sola e sintetizza tutte le frasi.
CHILD = r'''
import json, sys, wave
from piper import PiperVoice, SynthesisConfig
model, espeak, cfg = sys.argv[1], sys.argv[2], json.loads(sys.argv[3])
voice = PiperVoice.load(model, espeak_data_dir=espeak)
syn = SynthesisConfig(**cfg)
for job in json.load(sys.stdin):
    with wave.open(job["wav"], "wb") as w:
        voice.synthesize_wav(job["text"], w, syn_config=syn)
    print("  sintetizzata:", job["text"], flush=True)
'''


def unescape(s: str) -> str:
    """Scioglie gli escape delle stringhe Kotlin (\\" \\' \\\\ \\n \\$)."""
    return re.sub(r"\\(.)", lambda m: {"n": "\n", "t": "\t"}.get(m.group(1), m.group(1)), s)


def sha(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:10]


def phrases_from_kotlin(src: str) -> list[str]:
    out: list[str] = []
    for m in re.finditer(r"phrases\s*=\s*listOf\(", src):
        # fino alla parentesi che chiude listOf, ignorando quelle dentro le stringhe
        i, depth, in_str = m.end(), 1, False
        while i < len(src) and depth:
            c = src[i]
            if in_str:
                if c == "\\":
                    i += 1
                elif c == '"':
                    in_str = False
            elif c == '"':
                in_str = True
            elif c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            i += 1
        out += [unescape(s) for s in STRING.findall(src[m.end():i - 1])]
    out += [unescape(s) for s in re.findall(r'confirm\s*=\s*"((?:[^"\\]|\\.)*)"', src)]
    out += [unescape(s) for s in re.findall(r'SNOOZE_PHRASE\s*=\s*"((?:[^"\\]|\\.)*)"', src)]
    return list(dict.fromkeys(out))  # senza doppioni, nell'ordine del file


def ensure_piper() -> Path:
    py = VENV / "bin/python"
    if not py.exists():
        print("Creo il venv di Piper in", VENV)
        subprocess.run([sys.executable, "-m", "venv", str(VENV)], check=True)
        subprocess.run([str(VENV / "bin/pip"), "install", "-q", f"piper-tts=={PIPER_VERSION}"], check=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    for name in (f"{MODEL}.onnx", f"{MODEL}.onnx.json"):
        dest = MODELS / name
        if not dest.exists() or dest.stat().st_size < 1000:
            print("Scarico", name)
            urllib.request.urlretrieve(MODEL_URL + name, dest)
    return py


def espeak_dir(py: Path) -> Path:
    out = subprocess.run(
        [str(py), "-I", "-c", "import os, piper; print(os.path.dirname(piper.__file__))"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    d = Path(out) / "espeak-ng-data"
    if len(str(d)) <= 150:
        return d
    # espeak-ng scarta i percorsi lunghi e ripiega su quello compilato: usa un collegamento corto
    link = Path(tempfile.gettempdir()) / "lu3g-piper" / "espeak-ng-data"
    link.parent.mkdir(parents=True, exist_ok=True)
    if link.is_symlink() or link.exists():
        link.unlink()
    link.symlink_to(d)
    return link


def to_m4a(wav: Path, m4a: Path) -> None:
    if shutil.which("afconvert"):
        base = ["afconvert", "-f", "m4af", "-d", "aac"]
        try:
            subprocess.run(base + ["-b", "48000", str(wav), str(m4a)], check=True, capture_output=True)
        except subprocess.CalledProcessError:
            subprocess.run(base + [str(wav), str(m4a)], check=True)
    elif shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "aac", "-b:a", "48k", str(m4a)], check=True)
    else:
        sys.exit("serve afconvert (macOS) o ffmpeg nel PATH")


def main() -> None:
    force, dry = "--force" in sys.argv, "--dry-run" in sys.argv
    phrases = phrases_from_kotlin(KT.read_text(encoding="utf-8"))
    if not phrases:
        sys.exit(f"nessuna frase trovata in {KT}")
    wanted = {f"voice_{sha(t)}": t for t in phrases}
    stale = [p for p in RAW.glob("voice_*.m4a") if p.stem not in wanted]
    todo = {n: t for n, t in wanted.items() if force or not (RAW / f"{n}.m4a").exists()}
    print(f"{len(phrases)} frasi, {len(todo)} da generare, {len(stale)} clip da togliere")
    for n, t in wanted.items():
        print(f"  {n}  {'nuova  ' if n in todo else 'c’è già'}  {t}")
    for p in stale:
        print("  tolgo", p.name)
    if dry:
        return
    for p in stale:
        p.unlink()
    if todo:
        py = ensure_piper()
        espeak = espeak_dir(py)
        with tempfile.TemporaryDirectory() as tmp:
            jobs = [{"wav": str(Path(tmp) / f"{n}.wav"), "text": t} for n, t in todo.items()]
            subprocess.run(
                [str(py), "-I", "-c", CHILD, str(MODELS / f"{MODEL}.onnx"), str(espeak), json.dumps(SYNTH)],
                input=json.dumps(jobs), text=True, check=True,
            )
            for j in jobs:
                to_m4a(Path(j["wav"]), RAW / (Path(j["wav"]).stem + ".m4a"))
    clips = sorted(RAW.glob("voice_*.m4a"))
    print(f"Clip in {RAW.relative_to(ROOT)}: {len(clips)} file, {sum(p.stat().st_size for p in clips) / 1024:.0f} KB in totale")


if __name__ == "__main__":
    main()
