#!/usr/bin/env python3
"""Patch a COPY; exercise actual Player observer methods before/after and verify chain."""
from pathlib import Path
import hashlib, io, os, re, shutil, subprocess, tempfile, zipfile
R=Path(__file__).resolve().parents[2];P=R/'game/core-patches';T=Path(__file__).parent
core=R/'libs/GameServer.jar';original=core.read_bytes()
PLAYER='org/l2jmobius/gameserver/entity/actor/Player.class';MARK='META-INF/l2j-observer-cleanup-v1.properties'
KINDS={'debuff-no-refresh':('InstallDebuffGuard','DebuffCategoryGuard'),'arena-boundary':('InstallArenaBoundary','ArenaBoundaryGuard'),'olympiad-diversity':('InstallOlympiadDiversity','OlympiadDiversity')}
def run(args,ok=True,cwd=R):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180,cwd=cwd)
 if ok and p.returncode:
  print(p.stdout[-2000:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
 if not ok:assert p.returncode!=0,p.stdout
 return p.stdout
with tempfile.TemporaryDirectory(prefix='observer-cleanup-') as work:
 d=Path(work);jar=d/'GameServer.jar';jar.write_bytes(original)
 def install(kind,target=jar,ok=True,patches=P):
  if kind=='observer-cleanup':args=[patches/kind/'InstallObserverCleanup.java',target,patches]
  else:
   a,b=KINDS[kind];args=[patches/kind/(a+'.java'),target,patches/kind/(b+'.java')]
  return run(['java','--class-path','.',*args],ok)
 for kind in KINDS:install(kind)
 before=jar.read_bytes();install('observer-cleanup');patched=jar.read_bytes()
 with zipfile.ZipFile(io.BytesIO(before)) as a,zipfile.ZipFile(jar) as b:
  assert {n for n in a.namelist() if a.read(n)!=b.read(n)}=={PLAYER}
  assert set(b.namelist())-set(a.namelist())=={MARK}
 backups=list(d.glob('GameServer.jar.pre-observer-cleanup-*'));assert len(backups)==1 and backups[0].read_bytes()==before
 for kind in [*KINDS,'observer-cleanup']:install(kind);assert jar.read_bytes()==patched
 unknown=d/'unknown.jar';unknown.write_bytes(original);install('observer-cleanup',unknown,False);assert unknown.read_bytes()==original
 for entry in [PLAYER,'org/l2jmobius/gameserver/mechanics/skill/DebuffCategoryGuard.class','org/l2jmobius/gameserver/mechanics/skill/ArenaBoundaryGuard.class','org/l2jmobius/gameserver/mechanics/olympiad/OlympiadDiversity.class',MARK]:
  bad=d/'tampered.jar'
  with zipfile.ZipFile(jar) as a,zipfile.ZipFile(bad,'w') as b:
   for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n==entry else b''))
  data=bad.read_bytes()
  for kind in [*KINDS,'observer-cleanup']:install(kind,bad,False);assert bad.read_bytes()==data
 sources=d/'patches';shutil.copytree(P,sources)
 for source in ['debuff-no-refresh/DebuffCategoryGuard.java','arena-boundary/ArenaBoundaryGuard.java','olympiad-diversity/OlympiadDiversity.java','observer-cleanup/InstallObserverCleanup.java']:
  p=sources/source;data=p.read_bytes();p.write_bytes(data+b'\n// mismatch test\n')
  install('observer-cleanup',jar,False,sources);assert jar.read_bytes()==patched;p.write_bytes(data)
 print('PASS: atomic backup, exact Player-only change, four-installer idempotence, unknown/tampered core and changed source rejection; repository JAR untouched')
 inspect=d/'Inspect.java';inspect.write_text('class Inspect{public static void main(String[]a){System.exit(java.util.spi.ToolProvider.findFirst("javap").orElseThrow().run(System.out,System.err,a));}}')
 def javap(jar,cls):return run(['java',inspect,'-c','-p','-classpath',jar,cls])
 old=javap(core,'org.l2jmobius.gameserver.entity.actor.Player');new=javap(jar,'org.l2jmobius.gameserver.entity.actor.Player')
 def segment(text):return re.search(r'  public void leaveOlympiadObserverMode\(\);.*?(?=\n  (?:public|private|protected))',text,re.S).group()
 old_exit,new_exit=segment(old),segment(new)
 assert old.replace(old_exit,'')==new.replace(new_exit,''),'unrelated Player method changed'
 assert 'Method isGM:()Z' in old_exit and 'Method isGM:()Z' not in new_exit
 assert all(token in new_exit for token in ['Method setInvisible:(Z)V','Method setInvul:(Z)V','Method broadcastUserInfo:()V','Method teleToLocation:','Method unsetLastLocation:()V'])
 enter=javap(core,'org.l2jmobius.gameserver.network.clientpackets.EnterWorld')
 assert all(x in enter for x in ['GM_STARTUP_BUILDER_HIDE:Z','GM_STARTUP_INVULNERABLE:Z','GM_STARTUP_INVISIBLE:Z','Method org/l2jmobius/gameserver/entity/actor/Player.setHiding:(Z)Z'])
 print('PASS: only GM guard removed in native Olympiad exit; all other Player methods bytecode-disassembly identical, including observer entry and ordinary observer exit; actual EnterWorld login gates audited')
 compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 cp=os.pathsep.join([str(jar),*map(str,sorted((R/'libs').glob('*.jar')))])
 run(['java',compiler,'-cp',cp,'-d',d/'classes',T/'ObserverCleanupNativeTest.java'])
 deps=[str(x) for x in sorted((R/'libs').glob('*.jar')) if x!=core]
 for version,expected in [(core,'false'),(jar,'true')]:
  runtime=os.pathsep.join([str(d/'classes'),str(version),*deps])
  print(run(['java','-cp',runtime,'org.l2jmobius.gameserver.mechanics.olympiad.ObserverCleanupNativeTest',expected],cwd=R/'game').strip())
 # Full datapack compile checks all prior patches and bot scripts still link.
 scripts=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java'))]
 run(['java',compiler,'-cp',cp,'-d',d/'all',*scripts])
 print('PASS: all custom/effect scripts compile against four-patch core')
assert core.read_bytes()==original
ini=(R/'game/config/General.ini').read_text()
for name in ['GMStartupBuilderHide','GMStartupInvulnerable','GMStartupInvisible']:assert re.search(r'^'+name+r'\s*=\s*False\s*$',ini,re.M)
sh=(R/'game/GameServerTask.sh').read_text();cmd=(R/'game/InstallDebuffGuard.cmd').read_text()
assert 'observer-cleanup/InstallObserverCleanup.java' in sh and r'observer-cleanup\InstallObserverCleanup.java' in cmd
assert sh.index('olympiad-diversity/InstallOlympiadDiversity.java')<sh.index('observer-cleanup/InstallObserverCleanup.java')<sh.index('while :; do')
assert cmd.index(r'olympiad-diversity\InstallOlympiadDiversity.java')<cmd.index(r'observer-cleanup\InstallObserverCleanup.java')<cmd.index(':guard_failed')
print('PASS: three GM startup switches off; Linux/Windows fail-closed four-patch launch chain wired')
