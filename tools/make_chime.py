#!/usr/bin/env python3
"""Generates app/src/main/res/raw/prayer_chime.ogg (neutral 6 s three-note chime, E5-G5-C6 x2).
Original work of this project, dedicated to the public domain (CC0 1.0).
Usage: python3 tools/make_chime.py && ffmpeg -y -i /tmp/chime.wav -c:a libvorbis -q:a 4 app/src/main/res/raw/prayer_chime.ogg"""
import math, struct, wave
sr = 22050; notes = [(659.25, 0.0), (783.99, 0.45), (1046.5, 0.9), (659.25, 2.6), (783.99, 3.05), (1046.5, 3.5)]
dur = 6.0; n = int(sr * dur); buf = [0.0] * n
for f, t0 in notes:
    s = int(t0 * sr)
    for i in range(int(1.8 * sr)):
        if s + i >= n: break
        t = i / sr; env = math.exp(-t * 2.2) * min(1, t * 200)
        buf[s + i] += 0.28 * env * (math.sin(2 * math.pi * f * t) + 0.3 * math.sin(2 * math.pi * 2 * f * t))
w = wave.open('/tmp/chime.wav', 'wb'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, x)) * 32000)) for x in buf)); w.close()
