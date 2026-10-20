#!/usr/bin/env python3
"""Six-patch COPY only, native helper + narrow headless seams and full custom compilation."""
from pathlib import Path
import hashlib,os,subprocess,tempfile,zipfile
R=Path(__file__).resolve().parents[2];P=R/'game/core-patches';T=Path(__file__).parent
core=R/'libs/GameServer.jar';before=hashlib.sha256(core.read_bytes()).hexdigest()
def run(args,ok=True):
 p=subprocess.run(list(map(str,args)),cwd=R,capture_output=True,text=True,timeout=240)
 if ok and p.returncode:raise AssertionError(p.stdout[-2000:]+p.stderr[-6000:])
 if not ok:assert p.returncode!=0
 return p.stdout
with tempfile.TemporaryDirectory(prefix='ordered-olympiad-') as temp:
 d=Path(temp);jar=d/'GameServer.jar';jar.write_bytes(core.read_bytes())
 for folder,installer,helper in [('debuff-no-refresh','InstallDebuffGuard','DebuffCategoryGuard'),('arena-boundary','InstallArenaBoundary','ArenaBoundaryGuard'),('olympiad-diversity','InstallOlympiadDiversity','OlympiadDiversity')]:
  run(['java','--class-path','.',P/folder/(installer+'.java'),jar,P/folder/(helper+'.java')])
 for folder,installer in [('observer-cleanup','InstallObserverCleanup'),('olympiad-battle-timer','InstallOlympiadBattleTimer')]:run(['java','--class-path','.',P/folder/(installer+'.java'),jar,P])
 old=jar.read_bytes();install=['java','--class-path','.',P/'ordered-olympiad/InstallOrderedOlympiad.java',jar,P];run(install);patched=jar.read_bytes()
 import io
 with zipfile.ZipFile(io.BytesIO(old)) as a,zipfile.ZipFile(jar) as b:
  changed={n for n in a.namelist() if a.read(n)!=b.read(n)}
  assert changed=={'org/l2jmobius/gameserver/mechanics/olympiad/OlympiadManager.class','org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGameTask.class'},changed
  assert all('OrderedOlympiad' in n or 'ordered-olympiad' in n for n in set(b.namelist())-set(a.namelist()))
 # Every old entrypoint must delegate to the final whole-payload verifier.
 for folder,installer,tail in [('debuff-no-refresh','InstallDebuffGuard',P/'debuff-no-refresh/DebuffCategoryGuard.java'),('arena-boundary','InstallArenaBoundary',P/'arena-boundary/ArenaBoundaryGuard.java'),('olympiad-diversity','InstallOlympiadDiversity',P/'olympiad-diversity/OlympiadDiversity.java'),('observer-cleanup','InstallObserverCleanup',P),('olympiad-battle-timer','InstallOlympiadBattleTimer',P),('ordered-olympiad','InstallOrderedOlympiad',P)]:
  run(['java','--class-path','.',P/folder/(installer+'.java'),jar,tail]);assert jar.read_bytes()==patched
 assert next(d.glob('GameServer.jar.pre-ordered-olympiad-*')).read_bytes()==old
 unknown=d/'unknown.jar';unknown.write_bytes(core.read_bytes());run([*install[:4],unknown,P],False);assert unknown.read_bytes()==core.read_bytes()
 bad=d/'bad.jar'
 with zipfile.ZipFile(jar) as a,zipfile.ZipFile(bad,'w') as b:
  for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n.endswith('/OrderedOlympiad.class') else b''))
 bad_before=bad.read_bytes();run([*install[:4],bad,P],False);assert bad.read_bytes()==bad_before
 compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 cp=os.pathsep.join([str(jar),*map(str,sorted((R/'libs').glob('*.jar')))])
 sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),T/'OrderedOlympiadTest.java']
 run(['java',compiler,'-cp',cp,'-d',d/'classes',*sources])
 run(['java',T/'OrderedTestSeams.java',jar,d/'seams'])
 print(run(['java','-cp',os.pathsep.join([str(d/'seams'),str(d/'classes'),cp]),'org.l2jmobius.gameserver.mechanics.olympiad.OrderedOlympiadTest']).strip())
 print('PASS: full custom/effect compilation; only manager/task changed; backup, idempotence, six verifier paths, unknown and tampered-core rejection; repository core unchanged')
assert hashlib.sha256(core.read_bytes()).hexdigest()==before
