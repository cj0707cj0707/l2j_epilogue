#!/usr/bin/env python3
"""Offline suite only. Requires Python+lxml, JDK25, repository libs/datapack."""
from pathlib import Path
import os, subprocess, sys, tempfile
from lxml import etree
ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'game/data'
for script in ('run_bot_combat_tests.py','tactics_tests.py','training_tests.py'):
    subprocess.run([sys.executable,str(Path(__file__).with_name(script))],cwd=ROOT,check=True)
for name,xsd in [('GiranServices.xml','spawns.xsd')]+[(f'GiranService{i}.xml','npcs.xsd') for i in (1008000,1009000,1010000,1011000)]:
    file=DATA/('spawns/Others' if xsd=='spawns.xsd' else 'stats/npcs/custom')/name
    etree.XMLSchema(etree.parse(str(DATA/'xsd'/xsd))).assertValid(etree.parse(str(file)))
ids={1008000,1009000,1010000,1011000}
found={i:0 for i in ids}
for file in (DATA/'stats/npcs').rglob('*.xml'):
    for npc in etree.parse(str(file)).findall('.//npc'):
        i=int(npc.get('id','0'))
        if i in ids: found[i]+=1
assert set(found.values())=={1},found
spawns=etree.parse(str(DATA/'spawns/Others/GiranServices.xml')).findall('.//npc')
assert {int(n.get('id')) for n in spawns}==ids
assert all(n.get('y')=='148613' and n.get('z')=='-3455' for n in spawns)
# Conservative full bounding-box check, INCLUDING cylinder radius, not just centers.
cx,cy,cz=630,179184,-3720
for file in (DATA/'zones').rglob('*.xml'):
    tree=etree.parse(str(file))
    if tree.getroot().get('enabled')=='false': continue
    for z in tree.findall('.//zone'):
        if z.get('type') not in {'PeaceZone','ArenaZone','OlympiadStadiumZone','SiegeZone','CastleZone','FortZone','WaterZone','DamageZone','EffectZone','NoPvPZone'}: continue
        if int(z.get('maxZ','0'))<cz-20 or int(z.get('minZ','0'))>cz+200: continue
        nodes=z.findall('node')
        if not nodes: continue
        xs=[int(n.get('X')) for n in nodes]; ys=[int(n.get('Y')) for n in nodes]
        r=int(z.get('rad','0')) if z.get('shape')=='Cylinder' else 0
        overlaps=not(max(xs)+r<cx-700 or min(xs)-r>cx+700 or max(ys)+r<cy-700 or min(ys)-r>cy+700)
        assert not overlaps, (file.name,z.get('name'),z.get('type'))
t=(DATA/'scripts/custom/fakeplayersai/TrainingArena.java').read_text()
assert 's.duelRequested = true;' in t and t.index('s.duelRequested = true;')<t.index('DuelManager.getInstance().addDuel(')
assert 'if (!reserveAccount())' in t and 'SELECT password, accessLevel FROM accounts WHERE login=?' in t
assert 'INSERT INTO accounts (login,password,lastactive,accessLevel,lastIP)' in t and 'VALUES (?,?,0,-100,?)' in t
assert 'UPDATE accounts' not in t and 'TrainingEnabled' in (ROOT/'game/config/Custom/OlympiadTactics.ini').read_text()
assert 'confirmReturnStored(p)' in t and 'managerHasDuel' in t and 'returnRequested' in t
assert 'GC ONLY: never alter a returned human again.' in t and 'j.setProperty("humanReturned", "true"); save(p.getObjectId(), j);' in t
assert 'if (p != null) recover(p); else cleanupBot(j);' in t
npc=(DATA/'scripts/custom/GiranServices/GiranServices.java').read_text()
assert 'npc.canInteract(p)' in npc and '&& p.isGM()' in npc and 'event.length()>100' in npc
assert 'Olympiad.sendMatchList(p)' in npc
print('PASS: 4 NPC definitions + spawn XSD, unique IDs, training zone bounds and integration guards',flush=True)
with tempfile.TemporaryDirectory(prefix='giran-tests-') as d:
    tmp=Path(d); launcher=tmp/'Compile.java'
    launcher.write_text('import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }')
    cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
    sources=list(sorted((DATA/'scripts/custom').rglob('*.java')))
    subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sources),str(Path(__file__).with_name('giranServicesTest.java'))],cwd=ROOT,check=True)
    subprocess.run(['java','-cp',str(tmp/'classes')+os.pathsep+cp,'custom.GiranServices.giranServicesTest'],cwd=tmp,check=True)
print('PASS: complete offline suite; live GameServer/DB/client acceptance still REQUIRED')
