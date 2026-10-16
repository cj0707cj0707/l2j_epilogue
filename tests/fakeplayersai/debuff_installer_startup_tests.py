#!/usr/bin/env python3
"""Reproduce installer-held target handles; verify isolated installer and startup diagnostics.
Linux /proc probe models the Windows replacement restriction, NOT native Windows execution.
"""
from pathlib import Path
import subprocess, tempfile, os, hashlib, zipfile
R=Path(__file__).resolve().parents[2]; P=R/'game/core-patches/debuff-no-refresh'
core=R/'libs/GameServer.jar'; source=P/'DebuffCategoryGuard.java'
base=hashlib.sha256(core.read_bytes()).hexdigest()
probe='''
                // Test-only Windows-style deny-replacement probe on Linux.
                try(var descriptors=Files.list(Path.of("/proc/self/fd")))
                {
                    for(Path fd:descriptors.toList())
                    {
                        String link;
                        try {link=Files.readSymbolicLink(fd).toString();} catch(IOException ignored){continue;}
                        if(link.equals(core.toRealPath().toString()))
                            throw new IOException("OPEN_TARGET_JAR: "+link);
                    }
                }
'''
needle='                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);'
def instrument(s):
 assert s.count(needle)==1
 return s.replace(needle,probe+needle)
def invoke(cmd,**kw):
 return subprocess.run(cmd,text=True,capture_output=True,timeout=90,**kw)
with tempfile.TemporaryDirectory(prefix='debuff startup paths with spaces ') as d:
 t=Path(d)
 old=t/'old installer';old.mkdir(); oldjar=old/'GameServer.jar';oldjar.write_bytes(core.read_bytes())
 oldsource=old/'InstallDebuffGuard.java'
 original=subprocess.check_output(['git','show','46120f4df60a41d0c4cff4a9a792c835f8b8b46b:game/core-patches/debuff-no-refresh/InstallDebuffGuard.java'],cwd=R,text=True)
 oldsource.write_text(instrument(original))
 cp=os.pathsep.join([str(oldjar),*[str(p) for p in sorted((R/'libs').glob('*.jar')) if p.name!='GameServer.jar']])
 result=invoke(['java','-cp',cp,str(oldsource),str(oldjar),str(source)])
 assert result.returncode!=0 and 'OPEN_TARGET_JAR' in result.stderr,result.stdout+result.stderr
 assert hashlib.sha256(oldjar.read_bytes()).hexdigest()==base
 print('PASS: original launcher/installer pins its own target JAR before replacement (Windows hazard)',flush=True)
 new=t/'new installer';new.mkdir(); newjar=new/'GameServer.jar';newjar.write_bytes(core.read_bytes())
 newsource=new/'InstallDebuffGuard.java';newsource.write_text(instrument((P/'InstallDebuffGuard.java').read_text()))
 log=new/'loaded.log'
 cmd=['java','--class-path','.', '-Xlog:class+load=info:file='+str(log),str(newsource),str(newjar),str(source)]
 result=invoke(cmd,env=dict(os.environ,CLASSPATH=str(newjar)));assert result.returncode==0,result.stdout+result.stderr
 assert 'org.l2jmobius.' not in log.read_text(),'installer loaded server classes instead of parsing hierarchy'
 installed=newjar.read_bytes();result=invoke(cmd);assert result.returncode==0 and newjar.read_bytes()==installed
 assert 'already installed' in result.stdout
 backup=newjar.with_name('GameServer.jar.pre-debuff-no-refresh-'+base[:12]);assert backup.read_bytes()==core.read_bytes()
 print('PASS: no target-JAR handles / server classloading even with hostile CLASSPATH; paths with spaces, backup and idempotence',flush=True)
 # Already-installed v1 from prior installer remains compatible: metadata hashes are unchanged.
 # Old implementation succeeds with no target on application CP only if default resolver
 # has server classes available elsewhere, so use repository CP while replacing a different copy.
 legacy=t/'legacy';legacy.mkdir();legacyjar=legacy/'GameServer.jar';legacyjar.write_bytes(core.read_bytes())
 legacySource=legacy/'InstallDebuffGuard.java';legacySource.write_text(original)
 legacycp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
 result=invoke(['java','-cp',legacycp,str(legacySource),str(legacyjar),str(source)])
 assert result.returncode==0,result.stderr
 previous=legacyjar.read_bytes()
 result=invoke(['java','--class-path','.',str(P/'InstallDebuffGuard.java'),str(legacyjar),str(source)])
 assert result.returncode==0 and previous==legacyjar.read_bytes() and 'already installed' in result.stdout
 print('PASS: existing verified v1 patch retained byte-for-byte (no mechanic migration/reset)',flush=True)
 # New installation bytecode is mechanically identical to previous installation.
 with zipfile.ZipFile(newjar) as a,zipfile.ZipFile(legacyjar) as b:
  for name in a.namelist():
   if name!='META-INF/l2j-debuff-no-refresh-v1.properties':assert a.read(name)==b.read(name),name
 print('PASS: every runtime class/resource identical to previous guard installation',flush=True)
assert hashlib.sha256(core.read_bytes()).hexdigest()==base
for name in ('GameServer.vbs','GameServerWindow.vbs'):
 s=(R/'game'/name).read_text()
 assert s.index('shell.CurrentDirectory =')<s.index('OpenTextFile("java.cfg"')
 assert 'InstallDebuffGuard.cmd' in s and 'log\\debuff-guard-install.log' in s
 assert 'WScript.Quit patchExit' in s and 'Right(detail, 2200)' in s
 assert 'Progra~1' not in s and 'command = Chr(34) & javaExe & Chr(34)' in s
 assert ' -cp ' not in s
s=(R/'game/InstallDebuffGuard.cmd').read_text()
assert 'setlocal DisableDelayedExpansion' in s and 'cd /d "%~dp0"' in s
assert '-version >>"%GUARD_LOG%" 2>&1' in s and 'set "GUARD_RESULT=%ERRORLEVEL%"' in s
assert s.index('set "GUARD_RESULT=%ERRORLEVEL%"')>s.index('InstallDebuffGuard.java')
assert '--class-path "."' in s and '-cp' not in s and 'exit /b %GUARD_RESULT%' in s
s=(R/'game/GameServerTask.sh').read_text();assert '--class-path "."' in s and 'java -cp' not in s and 'debuff-guard-install.log' in s
subprocess.run(['bash','-n',str(R/'game/GameServerTask.sh')],check=True)
print('PASS: quoted Windows paths, script-relative cwd, version/error logs, fail-closed launch; shell syntax')
