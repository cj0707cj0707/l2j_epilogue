#!/usr/bin/env python3
"""Patch a COPY; verify native wrappers, three-installer chain and real helper behavior."""
from pathlib import Path
import hashlib,os,subprocess,tempfile,zipfile
R=Path(__file__).resolve().parents[2];P=R/'game/core-patches';S=R/'game/data/scripts/custom/fakeplayersai';T=Path(__file__).parent
core=R/'libs/GameServer.jar';before=hashlib.sha256(core.read_bytes()).hexdigest()
def run(args,ok=True,cwd=R):
 p=subprocess.run(args,capture_output=True,text=True,timeout=180,cwd=cwd)
 if ok and p.returncode:print(p.stdout[-1500:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
 if not ok:assert p.returncode!=0,p.stdout
 return p.stdout
with tempfile.TemporaryDirectory(prefix='olympiad-diversity-') as d:
 d=Path(d);jar=d/'GameServer.jar';jar.write_bytes(core.read_bytes())
 def install(kind,target=jar,ok=True):
  names={'debuff-no-refresh':('InstallDebuffGuard','DebuffCategoryGuard'),'arena-boundary':('InstallArenaBoundary','ArenaBoundaryGuard'),'olympiad-diversity':('InstallOlympiadDiversity','OlympiadDiversity')};a,b=names[kind]
  return run(['java','--class-path','.',str(P/kind/(a+'.java')),str(target),str(P/kind/(b+'.java'))],ok)
 install('debuff-no-refresh');install('arena-boundary');old=jar.read_bytes();install('olympiad-diversity');patched=jar.read_bytes()
 import io
 with zipfile.ZipFile(io.BytesIO(old)) as a,zipfile.ZipFile(jar) as b:
  assert {n for n in a.namelist() if a.read(n)!=b.read(n)}=={'org/l2jmobius/gameserver/mechanics/olympiad/OlympiadManager.class'}
  assert len(set(b.namelist())-set(a.namelist()))==3
 for kind in ['debuff-no-refresh','arena-boundary','olympiad-diversity']:install(kind);assert jar.read_bytes()==patched
 unknown=d/'unknown.jar';unknown.write_bytes(core.read_bytes());install('olympiad-diversity',unknown,False);assert unknown.read_bytes()==core.read_bytes()
 tampered=d/'tampered.jar'
 with zipfile.ZipFile(jar) as a,zipfile.ZipFile(tampered,'w') as b:
  for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n.endswith('/OlympiadDiversity.class') else b''))
 bad=tampered.read_bytes()
 for kind in ['debuff-no-refresh','arena-boundary','olympiad-diversity']:install(kind,tampered,False);assert tampered.read_bytes()==bad
 backup=list(d.glob('GameServer.jar.pre-olympiad-diversity-*'));assert len(backup)==1 and backup[0].read_bytes()==old
 compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 cp=os.pathsep.join([str(jar),*map(str,sorted((R/'libs').glob('*.jar')))])
 sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java','OlympiadDiversityTest.java']]]
 run(['java',str(compiler),'-cp',cp,'-d',str(d/'classes'),*map(str,sources)])
 print(run(['java','-cp',str(d/'classes')+os.pathsep+cp,'org.l2jmobius.gameserver.mechanics.olympiad.OlympiadDiversityTest',str(R/'game')],cwd=R/'game').strip())
 print(run(['java','-cp',str(d/'classes')+os.pathsep+cp,'custom.fakeplayersai.ArenaOrbitTranceManaTest',str(R/'game')],cwd=R/'game').strip())
 print('PASS: exact native method wrappers, unchanged unrelated entries, verified backups/idempotence/all3 paths reject tampered+unknown input; repository JAR unchanged')
assert hashlib.sha256(core.read_bytes()).hexdigest()==before
m=(S/'BotManager.java').read_text();reg=(S/'OlympiadRegistration.java').read_text()
assert 'registerNoble(' not in m and m.count('OlympiadRegistration.register(')==2 and m.count('OlympiadRegistration.request(')==1
for marker in ['Collections.shuffle(shuffled)','nextLong(5000,20001)','bot.getOlympiadGameId()==-1','!olympiad.playerInStadia(bot)','!olympiad.isRegisteredInComp(bot)','bot.getClient()!=null','DUE.keySet().retainAll(ids)']:assert marker in reg,marker
assert 'OlympiadRegistration.clear(bot.getObjectId())' in m
assert 'static synchronized int request(' in reg and 'ThreadPool.schedule(' in reg and '},20001)' in reg and '!manualRetry' in reg
assert reg.count('olympiad.registerNoble(')==1
assert 'olympiad-diversity/InstallOlympiadDiversity.java' in (R/'game/GameServerTask.sh').read_text()
assert r'olympiad-diversity\InstallOlympiadDiversity.java' in (R/'game/InstallDebuffGuard.cmd').read_text()
print('PASS: all automatic and admin registration paths share delay/shuffle/double-booking guards; both launchers install and verify diversity')
