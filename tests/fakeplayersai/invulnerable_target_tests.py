#!/usr/bin/env python3
"""Structure87: real production-hook matrix; run without legacy fictional doc84 flags."""
from pathlib import Path
import subprocess, sys
R=Path(__file__).resolve().parents[2]
s=R/'game/data/scripts/custom/fakeplayersai'
manager=(s/'BotManager.java').read_text();tactics=(s/'BotTactics.java').read_text()
for signature in ['private static void combatTick(', 'private static void olympiadChaseTick(']:
    body=manager[manager.index(signature):]
    assert body.index('BotTactics.tickInvulnerableTarget(')<body.index('BotTactics.tickBluffRear(')
body=tactics[tactics.index('public static synchronized boolean tick('):]
assert body.index('tickInvulnerableTarget(')<body.index('tickBluffRear(')
assert 'final boolean offBlocked = protectedSleep || enemy.isInvul();' in tactics
assert '(movingSupport || rootedKite) && dominatorTankSleepReady' in tactics
print('PASS: mandatory slow/fast/training wiring, universal offensive veto and rooted sleep admission',flush=True)
subprocess.run([sys.executable,str(Path(__file__).with_name('passive_barrier_bluff_tests.py')),'--invulnerable-only'],check=True)
