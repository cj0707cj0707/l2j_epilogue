#!/usr/bin/env python3
"""Production gate/preparation against narrow native doubles + real JAR effect denial tests.
No live server, database, NPC spawn or packets.
"""
from pathlib import Path
import os, subprocess, tempfile, sys
sys.dont_write_bytecode = True
from run_olympiad_gate_tests import STUBS, source, PKG
R=Path(__file__).resolve().parents[2]; T=Path(__file__).parent; S=R/'game/data/scripts/custom/fakeplayersai'
def run(args, **kw):
    p=subprocess.run(args,capture_output=True,text=True,timeout=180,**kw)
    if p.returncode: print(p.stdout[-4000:]);print(p.stderr[-6000:]);p.check_returncode()
    return p
s=(S/'BotSpawner.java').read_text();m=(S/'BotManager.java').read_text()
a=s[s.index('public static void maintainOlympiadSummon'):s.index('public static void ensurePresetGear')]
assert 'summonSkill.applyEffects' not in a and '!player.hasSummon()' not in a
hook=m[m.index('private static void olympiadPreStart'):m.index('private static void resetOlympiadPoints')]
assert hook.index('maintainFullBuffs(player, caster)') < hook.index('claimPreStart(player)') < hook.index('OlympiadSummonRules.prepare(player, preset)') < hook.index('applyPreCombatOpeners(player, preset)')
e=(R/'game/data/scripts/handlers/skill/effects/Summon.java').read_text()
assert e.index('OlympiadSummonRules.allowSummon')<e.index('NpcData.getInstance()')<e.index('summon.fullRestore()')<e.index('summon.spawnMe()')
assert 'PERMIT.remove();' in (S/'OlympiadSummonRules.java').read_text()
assert 'candidates.sort' in m and 'e.getKey() == owner ? 0 : 1' in m
with tempfile.TemporaryDirectory(prefix='oly-summon45-') as d:
    d=Path(d);compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    stubs=dict(STUBS)
    stubs['entity.actor.Player']=stubs['entity.actor.Player'].replace('public Summon summon;', '''public Summon summon; public Object client; public int cid=95,spawns; public boolean managed=true; public org.l2jmobius.gameserver.mechanics.skill.Skill skill;
 public Object getClient(){return client;} public String getName(){return "Fixture";}
 public PlayerClass getPlayerClass(){return new PlayerClass(cid);} public record PlayerClass(int getId) { public int getId(){return getId;} }
 public org.l2jmobius.gameserver.mechanics.skill.Skill getKnownSkill(int id){return skill!=null&&skill.getId()==id?skill:null;}''')
    stubs['entity.actor.Summon']=stubs['entity.actor.Summon'].replace('public boolean follow=true;', '''public boolean follow=true; public Player owner; public boolean dead,detach=true; public int hp=100,max=100,restores;
 public Player getOwner(){return owner;} public boolean isAlikeDead(){return dead;}
 public void fullRestore(){hp=max;restores++;} public void unSummon(Player p){if(detach)p.summon=null;}''')
    stubs['mechanics.skill.Skill']='''public class Skill {
 public final int id; public boolean fail,throwsNow; public Skill(int id){this.id=id;} public int getId(){return id;}
 public void applyEffects(org.l2jmobius.gameserver.entity.actor.Player p, org.l2jmobius.gameserver.entity.actor.Player target){
 if(!custom.fakeplayersai.OlympiadSummonRules.allowSummon(p,this))return;
 if(throwsNow)throw new IllegalStateException("injected spawn failure"); if(fail)return;
 var pet=new org.l2jmobius.gameserver.entity.actor.Summon();pet.owner=p;pet.fullRestore();p.summon=pet;p.spawns++;
 // A second effect in the same skill must not create another pet.
 if(custom.fakeplayersai.OlympiadSummonRules.allowSummon(p,this))throw new AssertionError("permit reused");
 }}'''
    sources=[source(d/'src',PKG+n,b) for n,b in stubs.items()]
    for n,b in {
      'BotManager':'public class BotManager {public static boolean isBot(org.l2jmobius.gameserver.entity.actor.Player p){return p.managed;}}',
      'BotClassPreset':'public record BotClassPreset(int getClassId){public int getClassId(){return getClassId;}}',
      'BotSpawner':'public class BotSpawner {public static int getOlympiadSummonSkillId(BotClassPreset p){return switch(p.getClassId()){case 95->1334;case 91->283;case 90->912;default->0;};}}'
    }.items():sources.append(source(d/'src','custom.fakeplayersai.'+n,b))
    sources += [S/'OlympiadCombatGate.java',S/'OlympiadSummonRules.java',T/'OlympiadSummonLifecycleTest.java']
    run(['java',str(compiler),'-d',str(d/'logic'),*map(str,sources)],cwd=R)
    print(run(['java','-cp',str(d/'logic'),PKG+'mechanics.olympiad.OlympiadSummonLifecycleTest'],cwd=R).stdout.strip())
    cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
    sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java','OlympiadSummonNativeGuardTest.java']]]
    run(['java',str(compiler),'-cp',cp,'-d',str(d/'actual'),*map(str,sources)],cwd=R)
    print(run(['java','-cp',str(d/'actual')+os.pathsep+cp,'custom.fakeplayersai.OlympiadSummonNativeGuardTest',str(R/'game')],cwd=R/'game').stdout.strip())
    print('PASS: all custom/effect scripts compile; production pre-start/maintenance/effect/name-owner wiring; no live DB/NPC spawn')
