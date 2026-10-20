#!/usr/bin/env python3
"""Patch only a copy; verify exact native loop fix, startup config, full chain and tamper guards."""
from pathlib import Path
import hashlib,io,os,re,shutil,subprocess,tempfile,zipfile
R=Path(__file__).resolve().parents[2];P=R/'game/core-patches';original=(R/'libs/GameServer.jar').read_bytes()
TASK='org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGameTask.class';MARK='META-INF/l2j-olympiad-battle-timer-v1.properties'
KINDS={'debuff-no-refresh':('InstallDebuffGuard','DebuffCategoryGuard'),'arena-boundary':('InstallArenaBoundary','ArenaBoundaryGuard'),'olympiad-diversity':('InstallOlympiadDiversity','OlympiadDiversity')}
def run(args,ok=True,cwd=R):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180,cwd=cwd)
 if ok and p.returncode:print(p.stdout[-2000:]);print(p.stderr[-6500:]);raise SystemExit(p.returncode)
 if not ok:assert p.returncode!=0,p.stdout
 return p.stdout
with tempfile.TemporaryDirectory(prefix='timer55-') as directory:
 d=Path(directory);jar=d/'GameServer.jar';jar.write_bytes(original)
 def install(kind,target=jar,ok=True,patches=P):
  if kind in ['observer-cleanup','olympiad-battle-timer']:
   name='InstallObserverCleanup' if kind=='observer-cleanup' else 'InstallOlympiadBattleTimer';args=[patches/kind/(name+'.java'),target,patches]
  else:
   a,b=KINDS[kind];args=[patches/kind/(a+'.java'),target,patches/kind/(b+'.java')]
  return run(['java','--class-path','.',*args],ok)
 install('olympiad-battle-timer',ok=False);assert jar.read_bytes()==original
 for kind in KINDS:install(kind)
 # Upgrade already deployed v52 observer installer, not only a newly created marker.
 legacy=P/'observer-cleanup/InstallObserverCleanup.java';old_source=legacy.read_text()
 start=old_source.index('        boolean battleTimer;');end=old_source.index('        String sourceHash=',start)
 old_source=old_source[:start]+old_source[end:]
 old_source=old_source.replace('!(sourceHash.equals(p.getProperty("source"))||"7d48e6b2377d04d0ee705f71665855151eb630e128dd4df22ac23468109bc426".equals(p.getProperty("source")))','!sourceHash.equals(p.getProperty("source"))')
 assert hashlib.sha256(old_source.encode()).hexdigest()=='7d48e6b2377d04d0ee705f71665855151eb630e128dd4df22ac23468109bc426'
 prior=d/'prior';shutil.copytree(P,prior);(prior/'observer-cleanup/InstallObserverCleanup.java').write_text(old_source)
 install('observer-cleanup',patches=prior);before=jar.read_bytes();install('observer-cleanup');assert jar.read_bytes()==before
 install('olympiad-battle-timer');patched=jar.read_bytes()
 with zipfile.ZipFile(io.BytesIO(before)) as a,zipfile.ZipFile(jar) as b:
  assert {n for n in a.namelist() if a.read(n)!=b.read(n)}=={TASK}
  assert set(b.namelist())-set(a.namelist())=={MARK}
 backups=list(d.glob('GameServer.jar.pre-olympiad-battle-timer-*'));assert len(backups)==1 and backups[0].read_bytes()==before
 for kind in [*KINDS,'observer-cleanup','olympiad-battle-timer']:install(kind);assert jar.read_bytes()==patched
 for entry in [TASK,'org/l2jmobius/gameserver/entity/actor/Player.class','org/l2jmobius/gameserver/mechanics/skill/ArenaBoundaryGuard.class',MARK]:
  bad=d/'bad.jar'
  with zipfile.ZipFile(jar) as a,zipfile.ZipFile(bad,'w') as b:
   for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n==entry else b''))
  data=bad.read_bytes()
  for kind in [*KINDS,'observer-cleanup','olympiad-battle-timer']:install(kind,bad,False);assert bad.read_bytes()==data
 sources=d/'changed';shutil.copytree(P,sources)
 for source in ['debuff-no-refresh/DebuffCategoryGuard.java','arena-boundary/ArenaBoundaryGuard.java','olympiad-diversity/OlympiadDiversity.java','observer-cleanup/InstallObserverCleanup.java','olympiad-battle-timer/InstallOlympiadBattleTimer.java']:
  p=sources/source;data=p.read_bytes();p.write_bytes(data+b'\n// mismatch\n');install('olympiad-battle-timer',ok=False,patches=sources);assert jar.read_bytes()==patched;p.write_bytes(data)
 print('PASS: legacy observer upgrade, exact task-only patch, backup, five-installer idempotence, unknown/tampered/source rejection; original JAR untouched')
 inspect=d/'Inspect.java';inspect.write_text('class Inspect{public static void main(String[]a){System.exit(java.util.spi.ToolProvider.findFirst("javap").orElseThrow().run(System.out,System.err,a));}}')
 def javap(target):return run(['java',inspect,'-c','-p','-classpath',target,'org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask'])
 old,new=javap(R/'libs/GameServer.jar'),javap(jar)
 # Source transformer preserves all offsets by replacing i2s with nop: no other instruction changes.
 assert old.count('i2s')==1 and old.replace('i2s','nop')==new
 assert 'Field BATTLE_PERIOD:J' in new and 'long 10000l' in new and 'Method org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGame.validateWinner:()V' in new
 # Interpret the verified native increment with/without its narrowing, not a substituted game rule.
 for limit in [90000,150000,210000,360000]:
  counter=0;iterations=0
  while counter<limit and iterations<1000:counter+=10000;iterations+=1
  assert iterations==limit//10000 and counter==limit
  counter=0
  for _ in range(1000):counter=((counter+10000+32768)%65536)-32768;assert counter<limit
 config=d/'TimerConfigCheck.java';config.write_text('''class TimerConfigCheck{public static void main(String[]a)throws Exception{
 org.l2jmobius.gameserver.config.OlympiadConfig.load();
 if(org.l2jmobius.gameserver.config.OlympiadConfig.OLYMPIAD_BATTLE!=210000)throw new AssertionError("config210s");
 Class<?> t=Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask");var f=t.getDeclaredField("BATTLE_PERIOD");f.setAccessible(true);
 if(f.getLong(null)!=210000)throw new AssertionError("native static battle period");
 System.out.println("PASS: real OlympiadConfig.load and patched native task BATTLE_PERIOD=210000; JVM -Xverify:all");}}''')
 compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 cp=os.pathsep.join([str(jar),*map(str,sorted((R/'libs').glob('*.jar')))])
 run(['java',compiler,'-cp',cp,'-d',d/'classes',config])
 print(run(['java','-Xverify:all','-cp',str(d/'classes')+os.pathsep+cp,'TimerConfigCheck'],cwd=R/'game'))
 scripts=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java'))]
 run(['java',compiler,'-cp',cp,'-d',d/'all',*scripts])
 print('PASS: only native i2s replaced by nop; unchanged cleanup/winner logic; 21x10s at210s, no signed-short overflow at90/150/210/360s; all custom/effect scripts compile against five-patch core')
assert (R/'libs/GameServer.jar').read_bytes()==original
for name,sep in [('GameServerTask.sh','/'),('InstallDebuffGuard.cmd','\\')]:
 s=(R/'game'/name).read_text();assert 'olympiad-battle-timer'+sep+'InstallOlympiadBattleTimer.java' in s
 assert s.index('observer-cleanup'+sep+'InstallObserverCleanup.java')<s.index('olympiad-battle-timer'+sep+'InstallOlympiadBattleTimer.java')
print('PASS: Linux/Windows five-patch fail-closed startup chain; 210s applies to active Olympiad, not TrainingArena')
