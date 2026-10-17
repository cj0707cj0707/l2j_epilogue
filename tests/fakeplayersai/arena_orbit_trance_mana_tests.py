#!/usr/bin/env python3
"""Four arena rules against installed core copy; original binary is never modified."""
from pathlib import Path
import os,subprocess,tempfile,hashlib,zipfile
R=Path(__file__).resolve().parents[2];P=R/'game/core-patches';S=R/'game/data/scripts/custom/fakeplayersai'
def run(args,**kw):
    p=subprocess.run(args,capture_output=True,text=True,timeout=180,**kw)
    if p.returncode:print(p.stdout[-2000:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
    return p.stdout
core=R/'libs/GameServer.jar';before=hashlib.sha256(core.read_bytes()).hexdigest()
with tempfile.TemporaryDirectory(prefix='arena-orbit-core-') as d:
    t=Path(d);jar=t/'GameServer.jar';jar.write_bytes(core.read_bytes())
    old=['java','--class-path','.',str(P/'debuff-no-refresh/InstallDebuffGuard.java'),str(jar),str(P/'debuff-no-refresh/DebuffCategoryGuard.java')]
    new=['java','--class-path','.',str(P/'arena-boundary/InstallArenaBoundary.java'),str(jar),str(P/'arena-boundary/ArenaBoundaryGuard.java')]
    run(old,cwd=R);debuff=jar.read_bytes();run(new,cwd=R);patched=jar.read_bytes();run(new,cwd=R);run(old,cwd=R)
    assert patched==jar.read_bytes(),'both launcher verification paths idempotent'
    import io
    with zipfile.ZipFile(io.BytesIO(debuff)) as a,zipfile.ZipFile(jar) as b:
        changes={n for n in a.namelist() if a.read(n)!=b.read(n)}
        assert changes=={'org/l2jmobius/gameserver/entity/WorldObject.class','org/l2jmobius/gameserver/entity/actor/Creature.class'},changes
        assert len(set(b.namelist())-set(a.namelist()))==3
    bad=t/'unknown.jar';bad.write_bytes(core.read_bytes())
    fail=subprocess.run(new[:4]+[str(bad),new[5]],capture_output=True,text=True)
    assert fail.returncode!=0 and bad.read_bytes()==core.read_bytes(),'unprepared core fails closed'
    broken=t/'tampered.jar'
    with zipfile.ZipFile(jar) as a,zipfile.ZipFile(broken,'w') as b:
        for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n.endswith('/ArenaBoundaryGuard.class') else b''))
    fail=subprocess.run(new[:4]+[str(broken),new[5]],capture_output=True,text=True)
    assert fail.returncode!=0,'tampered patched core fails closed'
    compiler=t/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    javap=t/'Inspect.java';javap.write_text('class Inspect{public static void main(String[]a){System.exit(java.util.spi.ToolProvider.findFirst("javap").orElseThrow().run(System.out,System.err,a));}}')
    cp=os.pathsep.join([str(jar),*map(str,sorted((R/'libs').glob('*.jar')))])
    bc=run(['java',str(javap),'-classpath',cp,'-p','-c','org.l2jmobius.gameserver.entity.actor.Creature'])
    method=bc.split('public void moveToLocation(int, int, int, int);',1)[1].split('public ',1)[0]
    assert method.index('ArenaBoundaryGuard.clamp:')<method.index('boundaryOriginalMoveToLocation:')
    forms=run(['java',str(javap),'-classpath',cp,'-p','-c','org.l2jmobius.gameserver.mechanics.stats.Formulas'])
    forms=forms.split('public static boolean calcEffectSuccess(',1)[1].split('public static ',1)[0]
    assert all(x in forms for x in ('Skill.getActivateRate:','Skill.getMinChance:','Skill.getMaxChance:','Math.clamp:'))
    sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),
        *[Path(__file__).with_name(n) for n in ('LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java')]]
    run(['java',str(compiler),'-cp',cp,'-d',str(t/'classes'),*map(str,sources)],cwd=R)
    print(run(['java','-cp',str(t/'classes')+os.pathsep+cp,'custom.fakeplayersai.ArenaOrbitTranceManaTest',str(R/'game')],cwd=R/'game').strip())
    baseline=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
    print(run(['java','-cp',str(t/'classes')+os.pathsep+baseline,'custom.fakeplayersai.ArenaOrbitTranceManaTest',str(R/'game'),'missing'],cwd=R/'game').strip())
assert hashlib.sha256(core.read_bytes()).hexdigest()==before
manager=(S/'BotManager.java').read_text();tactics=(S/'BotTactics.java').read_text()
assert 'ArenaCombatRules.update(bot, activeOpponent, preset)' in manager
assert 'ArenaCombatRules.update(bot, enemy, preset)' in tactics
assert 'ArenaCombatRules.clear(botId)' in tactics and 'ArenaCombatRules.clear(e.getKey())' in tactics
assert 'ArenaCombatRules.kitePoint(bot, enemy, 100)' in tactics and 'ArenaCombatRules.kitePoint(bot, enemy, 120)' in tactics
assert 'ArenaCombatRules.kitePoint(bot, enemy.asPlayer(), KITE_STEP)' in manager
assert '(bot.getX() + enemy.getX()) / 2' not in tactics
assert 'CardinalTrance.apply(player)' in (S/'BotSpawner.java').read_text()
assert 'if (isBot(player) && player.getClient() == null) return false;' in manager
assert 'arena-boundary/InstallArenaBoundary.java' in (R/'game/GameServerTask.sh').read_text()
assert r'arena-boundary\InstallArenaBoundary.java' in (R/'game/InstallDebuffGuard.cmd').read_text()
print('PASS: verified backups/idempotence/unknown+tampered rejection, native coordinate wrappers, shared ranked/training/legacy wiring; repository JAR unchanged')
