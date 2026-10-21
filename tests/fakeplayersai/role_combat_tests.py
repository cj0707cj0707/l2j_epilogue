#!/usr/bin/env python3
"""Structure88: real act/poll/public hooks + source wiring; no live server/DB."""
from pathlib import Path
import subprocess, sys
R=Path(__file__).resolve().parents[2]
S=R/'game/data/scripts/custom/fakeplayersai'
t=(S/'BotTactics.java').read_text();m=(S/'BotManager.java').read_text()
p=(S/'RoleCombatPolicy.java').read_text();sp=(S/'BotSpawner.java').read_text();guard=(S/'BotRecoveryRules.java').read_text()
for signature in ['private static void combatTick(', 'private static void olympiadChaseTick(']:
    part=m[m.index(signature):]
    assert part.index('BotTactics.tickInvulnerableTarget(')<part.index('BotTactics.tickRoleCombat(')<part.index('BotTactics.tickBluffRear(')
part=t[t.index('public static synchronized boolean tick('):]
assert part.index('tickInvulnerableTarget(')<part.index('tickRoleCombat(')<part.index('tickBluffRear(')
assert 'charges < CHARGE_MIN && !(bot.isInOlympiadMode() && bot.isOlympiadStart() && TacticsPolicy.chargeMeleeClass(preset.getClassId()))' in m
assert 'RoleCombatPolicy.buildersLast(options)' in t and 'time>=s.chargePursuitUntil' in t and 's.chargePursuitUntil=time+1200' in t
assert 'RoleCombatPolicy.builder(id)?1:0' in sp
assert 'tankMirrorDefenseAllowed(actor,enemy,event.getSkill())' in guard
assert 'RoleCombatPolicy.candidate(actor,enemy,event.getSkill(),victim==actor)' in guard
assert 'RoleCombatPolicy.needsCharge(actor)' in guard and 'RoleCombatPolicy.rangedDominator(event.getAttacker()' in guard
assert 'drains>=4' in p and 'dx*((double)e.getXdestination()-e.getX())' in p
assert 'SleepRetreat.ownSleep(p,e)!=null' in p
ini=dict(line.split('=',1) for line in (R/'game/config/Custom/FakePlayersAIOverrides.ini').read_text().splitlines() if line.startswith('rotation.'))
for cid,rage in [(88,345),(114,346)]:
    ids=list(map(int,ini['rotation.'+str(cid)].split(',')))
    assert ids[-1]==rage and all(x not in (8,50,345,346) for x in ids[:-2])
print('PASS: role-hook order in all3 entry paths, no active ranked refill, builder order, native guards and pursuit window',flush=True)
subprocess.run([sys.executable,str(Path(__file__).with_name('passive_barrier_bluff_tests.py')),'--role-only'],cwd=R,check=True)
