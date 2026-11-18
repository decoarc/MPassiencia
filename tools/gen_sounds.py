"""Gera os efeitos sonoros do jogo (sons próprios do projeto, sem direitos de terceiros).

Uso: python -I tools/gen_sounds.py app/src/main/res/raw
Saída: flip.wav, place.wav, foundation.wav, win.wav (44,1 kHz, 16 bits, mono).
Só usa a biblioteca padrão; a semente é fixa para o resultado ser sempre o mesmo.
"""
import math
import random
import struct
import sys
import wave
from pathlib import Path

RATE = 44_100
rng = random.Random(20261117)


def envelope(i: int, n: int, attack_ms: float = 2.0, decay: float = 6.0) -> float:
    attack = int(RATE * attack_ms / 1000)
    if i < attack:
        return i / max(1, attack)
    return math.exp(-decay * (i - attack) / max(1, n - attack))


def noise_lowpass(n: int, alpha: float) -> list[float]:
    out, prev = [], 0.0
    for _ in range(n):
        prev = prev + alpha * (rng.uniform(-1, 1) - prev)
        out.append(prev)
    return out


def tone(freqs: list[float], n: int) -> list[float]:
    return [sum(math.sin(2 * math.pi * f * i / RATE) for f in freqs) / len(freqs) for i in range(n)]


def samples_ms(ms: float) -> int:
    return int(RATE * ms / 1000)


def flip() -> list[float]:
    n = samples_ms(60)
    raw = noise_lowpass(n, 0.35)
    return [raw[i] * envelope(i, n, decay=7) * 1.6 for i in range(n)]


def place() -> list[float]:
    n = samples_ms(80)
    raw = noise_lowpass(n, 0.2)
    body = tone([180], n)
    return [(0.6 * raw[i] + 0.5 * body[i]) * envelope(i, n, decay=8) for i in range(n)]


def foundation() -> list[float]:
    n = samples_ms(120)
    body = tone([880, 1320], n)
    return [body[i] * envelope(i, n, decay=5) * 0.7 for i in range(n)]


def win() -> list[float]:
    out: list[float] = []
    for f in (523, 659, 784, 1047):
        n = samples_ms(120)
        body = tone([f, f * 2], n)
        out += [body[i] * envelope(i, n, decay=3) * 0.6 for i in range(n)]
    return out


def write(path: Path, data: list[float]) -> None:
    peak = max(1e-9, max(abs(x) for x in data))
    scale = 0.85 / peak
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, x * scale)) * 32767)) for x in data))


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    for name, gen in {"flip": flip, "place": place, "foundation": foundation, "win": win}.items():
        write(out / f"{name}.wav", gen())
    total = sum((out / f"{n}.wav").stat().st_size for n in ("flip", "place", "foundation", "win"))
    print(f"ok: 4 arquivos, {total} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
