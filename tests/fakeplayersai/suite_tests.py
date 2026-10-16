#!/usr/bin/env python3
"""Offline suite only. Requires Python+lxml, JDK25, repository libs/datapack."""
from pathlib import Path
import os, subprocess, sys, tempfile
from lxml import etree
ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/'game/data'
for script in ('run_bot_combat_tests.py','tactics_tests.py','training_tests.py','run_olympiad_gate_tests.py','fear_tests.py','training_selfbuff_tests.py','run_training_peace_zones_tests.py','training_berserker_pressure_tests.py'):
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
cx,cy,cz=43409,188493,-3513
expected_peace={"giran_port3","giran_port4"}
found_peace=set()
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
        overlaps=not(max(xs)+r<cx-1000 or min(xs)-r>cx+1000 or max(ys)+r<cy-1000 or min(ys)-r>cy+1000)
        if overlaps and file.name=='peace.xml' and z.get('type')=='PeaceZone' and z.get('name') in expected_peace:
            found_peace.add(z.get('name')); continue # only these have scoped instance membership adapters
        assert not overlaps, (file.name,z.get('name'),z.get('type'))
assert found_peace==expected_peace, found_peace
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
assert 'BypassHandler.getInstance().registerHandler(new NpcCommands())' in npc
assert 'handleNpcEvent(event,target.asNpc(),player)' in npc
assert 'bypass -h Script GiranServices' not in npc
assert 'bypass -h GiranService ' in npc
assert 'if (!notice.isBlank()) p.sendMessage(notice);' in npc
assert 'A noninteractive, origin-free status is safe' in npc
spawner=(DATA/'scripts/custom/fakeplayersai/BotSpawner.java').read_text()
training_buff=spawner.split('public static void applyTrainingOlympiadBuffs(')[1].split('public static void maintainFullBuffs(')[0]
assert 'applyOlympiadBuffs(player);' in training_buff and 'getOlympiadBuffs(caster)' in training_buff
assert 'setInOlympiadMode' not in training_buff and 'FULL_BUFFS_MAGE' not in training_buff
assert '"profile equal"' not in npc and 'Сравнительный профиль выравнивает' not in npc
assert '"profile","custom"); // legacy equal' in npc and 'return "custom";' in npc
assert '10–60 сек.' in npc and '30–60' not in npc and 'Сброса откатов игрока нет' not in npc
assert 'те же настройки снаряжения и заточки умений' in npc and 'Бот 85 уровня' in npc
assert 'За 1 сек. до боя обоим' in npc and 'не в конце боя' in npc
assert 'Без минимального HP/CP/MP' in npc and 'При входе снимаются прежние эффекты' in npc
assert '42705/188869/-3513' in npc and '44112/188116/-3513' in npc
assert 'до остатка общего лимита 120 сек.' in npc and 'победа боту автоматически не присуждается' in npc
assert 'approachWithMargin(BotTactics.tacticalApproachRange(b, p, preset))' in t
assert 'BotTactics.allowsBasicAttack(b, preset)' in t and 'b.calculateDistance2D(p) <= range &&' in t
assert 'DefaultDifficulty = strong' in (ROOT/'game/config/Custom/OlympiadTactics.ini').read_text()
assert 'TrainingPreparationSeconds = 10' in (ROOT/'game/config/Custom/OlympiadTactics.ini').read_text()
print('PASS: 4 NPC definitions + spawn XSD, unique IDs, training zone bounds and integration guards',flush=True)
with tempfile.TemporaryDirectory(prefix='giran-tests-') as d:
    tmp=Path(d); launcher=tmp/'Compile.java'
    launcher.write_text('import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }')
    cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
    sources=list(sorted((DATA/'scripts/custom').rglob('*.java')))
    subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sources),str(Path(__file__).with_name('giranServicesTest.java'))],cwd=ROOT,check=True)
    subprocess.run(['java','-cp',str(tmp/'classes')+os.pathsep+cp,'custom.GiranServices.giranServicesTest'],cwd=tmp,check=True)
print('PASS: complete offline suite; live GameServer/DB/client acceptance still REQUIRED')

# Shared ranked/training defenses must not depend on permanent-roster membership.
spawner=(DATA/'scripts/custom/fakeplayersai/BotSpawner.java').read_text()
defenses=spawner.split('public static int[] getTacticalDefenses(',1)[1].split('public static boolean tacticalSkillAllowed(',1)[0]
assert 'isTacticalTank(player)' in defenses and 'BotManager.isTankBot' not in defenses
training_profile=spawner.split('public static void applyTrainingOlympiadBuffs(',1)[1].split('public static void maintainFullBuffs(',1)[0]
assert 'isOpenerOnlySkill(buff[0]) || isTankDefenseSkill(buff[0])' in training_profile
assert 'aus.getAutoSkills().removeIf(BotSpawner::isOpenerOnlySkill)' in spawner
assert 'aus.getAutoBuffs().removeIf(BotSpawner::isOpenerOnlySkill)' in spawner
print('PASS: shared defense/preparation/opener integration guards')
