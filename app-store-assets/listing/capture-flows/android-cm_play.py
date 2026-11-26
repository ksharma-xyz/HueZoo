# Plays Color Memory on the tablet emulator: remembers chamber A's colour while it is shown, answers
# SAME/DIFFERENT correctly in the decision phase. usage: cm_play.py <serial> <rounds> [skip_first_answer 0|1]
import os, subprocess, sys, io, time
from PIL import Image
S, ROUNDS = sys.argv[1], int(sys.argv[2])
WRONG_FROM = int(sys.argv[3]) if len(sys.argv) > 3 else 99   # 1-based round from which to answer wrong on purpose (keeps the result out of LEGENDARY)
ADB = [os.environ.get('ADB', 'adb'), '-s', S]
# Tablet (2560x1600) defaults; for the 1080x2400 phone set CM_CLASS=phone.
if os.environ.get('CM_CLASS') == 'phone':
    PA, PB, SAME, DIFF = (300, 1500), (800, 1500), (283, 2239), (797, 2239)
else:
    PA, PB = (1000, 800), (2000, 800)
    SAME, DIFF = (286, 1341), (658, 1341)
DARK = {(25, 25, 35)}
def shot():
    raw = subprocess.run(ADB + ['exec-out', 'screencap', '-p'], capture_output=True).stdout
    return Image.open(io.BytesIO(raw)).convert('RGB')
def is_bg(c): return max(c) < 80
def tap(p): subprocess.run(ADB + ['shell', 'input', 'tap', str(p[0]), str(p[1])])
mem = None; answered = 0; t0 = time.time()
while answered < ROUNDS and time.time() - t0 < 150:
    im = shot(); a, b = im.getpixel(PA), im.getpixel(PB)
    pass
    if not is_bg(a) and is_bg(b):
        mem = a; print('memorize A', a, flush=True)
    elif is_bg(a) and not is_bg(b) and mem is not None:
        same = max(abs(x - y) for x, y in zip(mem, b)) <= 2
        if answered + 1 >= WRONG_FROM: same = not same
        print('decision B', b, 'mem', mem, '->', 'SAME' if same else 'DIFFERENT', flush=True)
        tap(SAME if same else DIFF); answered += 1; mem = None
        time.sleep(2.5)
print('answered', answered)
