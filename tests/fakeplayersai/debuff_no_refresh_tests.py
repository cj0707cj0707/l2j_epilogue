#!/usr/bin/env python3
"""Patch a COPY of the shipped core, verify idempotence, fail-closed input and real EffectList admission."""
from pathlib import Path
import subprocess,tempfile,os,hashlib,zipfile
R=Path(__file__).resolve().parents[2]
P=R/'game/core-patches/debuff-no-refresh'
core=R/'libs/GameServer.jar'; before=hashlib.sha256(core.read_bytes()).hexdigest()
cp=os.pathsep.join(str(p) for p in sorted((R/'libs').glob('*.jar')))
with tempfile.TemporaryDirectory(prefix='debuff-core-') as d:
 t=Path(d); jar=t/'GameServer.jar';jar.write_bytes(core.read_bytes())
 cmd=['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(jar),str(P/'DebuffCategoryGuard.java')]
 subprocess.run(cmd,cwd=R,check=True)
 out=jar.read_bytes();subprocess.run(cmd,cwd=R,check=True);assert out==jar.read_bytes(),'idempotence'
 with zipfile.ZipFile(core) as a,zipfile.ZipFile(jar) as b:
  changed=[n for n in a.namelist() if a.read(n)!=b.read(n)]
  assert changed==['org/l2jmobius/gameserver/entity/actor/holders/creature/EffectList.class'],changed
  assert len(set(b.namelist())-set(a.namelist()))==2
 # Actual compiled test helpers; no server/singleton startup.
 classes=t/'classes';classes.mkdir()
 launcher=t/'Compile.java';launcher.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 patchedcp=str(jar)+os.pathsep+cp
 subprocess.run(['java',str(launcher),'-cp',patchedcp,'-d',str(classes),str(Path(__file__).with_name('DebuffNoRefreshTest.java')),*[str(R/'game/data/scripts/handlers/skill/effects'/f'{name}.java') for name in ('Stun','Fear','Root','Sleep','Mute','PhysicalMute','Disarm','Paralyze')]],check=True)
 test='org.l2jmobius.gameserver.mechanics.skill.DebuffNoRefreshTest'
 subprocess.run(['java','-Dtest.datapack='+str(R/'game'),'-cp',str(classes)+os.pathsep+patchedcp,test],cwd=t,check=True)
 subprocess.run(['java','-Dtest.datapack='+str(R/'game'),'-cp',str(classes)+os.pathsep+cp,test,'baseline'],cwd=t,check=True)
 bad=t/'unknown.jar';bad.write_bytes(core.read_bytes()+b'changed');badbytes=bad.read_bytes()
 failed=subprocess.run(['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(bad),str(P/'DebuffCategoryGuard.java')],capture_output=True,text=True)
 assert failed.returncode!=0 and 'Unsupported GameServer.jar' in failed.stderr and bad.read_bytes()==badbytes
 # Tampered installed core is never silently accepted.
 corrupt=t/'tampered.jar'
 with zipfile.ZipFile(jar) as a,zipfile.ZipFile(corrupt,'w') as b:
  for n in a.namelist():b.writestr(n,a.read(n)+(b'x' if n.endswith('/DebuffCategoryGuard.class') else b''))
 failed=subprocess.run(['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(corrupt),str(P/'DebuffCategoryGuard.java')],capture_output=True,text=True)
 assert failed.returncode!=0 and 'marker/hash mismatch' in failed.stderr
 backupdir=t/'backup-case';backupdir.mkdir();backupbad=backupdir/'backup-bad.jar';backupbad.write_bytes(core.read_bytes())
 backupbad.with_name(backupbad.name+'.pre-debuff-no-refresh-'+before[:12]).write_bytes(b'not the core')
 failed=subprocess.run(['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(backupbad),str(P/'DebuffCategoryGuard.java')],capture_output=True,text=True)
 assert failed.returncode!=0 and 'Backup exists but hash differs' in failed.stderr and backupbad.read_bytes()==core.read_bytes()
 # Prove the lock-order fixture catches the rejected list-before-actor design.
 wrong=t/'wrong-order';wrong.mkdir();wrongjar=wrong/'GameServer.jar';wrongjar.write_bytes(core.read_bytes())
 wrongsource=wrong/'DebuffCategoryGuard.java'
 text=(P/'DebuffCategoryGuard.java').read_text()
 old='synchronized (owner != null ? owner : list)\n        {\n            synchronized (list) { work.run(); }'
 new='synchronized (list)\n        {\n            synchronized (owner != null ? owner : list) { work.run(); }'
 assert old in text;wrongsource.write_text(text.replace(old,new))
 subprocess.run(['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(wrongjar),str(wrongsource)],check=True)
 failed=subprocess.run(['java','-Dtest.datapack='+str(R/'game'),'-cp',str(classes)+os.pathsep+str(wrongjar)+os.pathsep+cp,test,'locks'],capture_output=True,text=True,timeout=20)
 assert failed.returncode!=0 and 'LOCK_ORDER actor/effect-list inversion' in failed.stderr,failed.stderr
 print('PASS: actor-before-list add/expiry regression; deliberately reversed order reproduces deadlock')
 # Compile all custom scripts against the PATCHED core as well.
 sources=sorted((R/'game/data/scripts/custom').rglob('*.java'))
 subprocess.run(['java',str(launcher),'-cp',patchedcp,'-d',str(t/'production'),*map(str,sources)],check=True)
 assert before==hashlib.sha256(core.read_bytes()).hexdigest(),'repository binary untouched'
 print('PASS: exact-core patch, verified backup, untouched other entries, idempotence, unknown/tampered core fail-closed')

# All supported launchers install/verify before their actual core start.
for name in ('GameServer.vbs','GameServerWindow.vbs'):
 s=(R/'game'/name).read_text();assert s.index('patchExit = shell.Run')<s.index('command = Chr(34) & javaExe');assert 'WScript.Quit patchExit' in s
s=(R/'game/GameServerTask.sh').read_text();assert s.index('InstallDebuffGuard.java')<s.index('while :; do') and '|| exit 1' in s
print('PASS: supported launchers verify patch before starting GameServer')

# XML audit of user's examples: all use the same actual category and have both
# instant damage and a continuous Stun; there is no per-skill whitelist in guard.
import xml.etree.ElementTree as ET
examples={260:'PhysicalDamage',362:'PhysicalDamage',793:'PhysicalSoulDamage'}
seen=set()
for xml in (R/'game/data/stats/skills').glob('*.xml'):
 for skill in ET.parse(xml).getroot().findall('skill'):
  sid=int(skill.get('id','0'))
  if sid not in examples:continue
  assert any(e.text=='STUN' for e in skill.findall('.//abnormalType'))
  names={e.get('name') for e in skill.findall('.//effect')}
  assert 'Stun' in names and examples[sid] in names;seen.add(sid)
assert seen==set(examples)
print('PASS: real XML + core instant/continuous dispatch: Hammer Crush260, Armor Crush362, Rush Impact793')
