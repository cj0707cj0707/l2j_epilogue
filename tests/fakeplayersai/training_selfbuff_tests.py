#!/usr/bin/env python3
"""Compile all custom sources, real Skill eligibility + isolated production pulse."""
from pathlib import Path
import os, subprocess, tempfile
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai/BotSpawner.java'
s=SRC.read_text()
def method(signature):
    begin=s.index(signature); brace=s.index('{',begin); level=1; i=brace+1
    while level:
        if s[i]=='{': level+=1
        elif s[i]=='}': level-=1
        i+=1
    return s[begin:i]
pulse=method('public static boolean applyTrainingPreparationSelfBuffs(')
for bad in ('abortCast(', 'abortAttack(', 'applyEffects(', 'setCurrent', 'startAuto', 'stopMove(', 'setIntention'):
    assert bad not in pulse, bad
assert 'preset.getBuffs()' in pulse and 'TrainingArena.isPreparingBot(bot)' in pulse
with tempfile.TemporaryDirectory(prefix='training-selfbuff-') as d:
    tmp=Path(d); cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
    launcher=tmp/'Compile.java'
    launcher.write_text('import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }')
    sources=sorted((ROOT/'game/data/scripts/custom').rglob('*.java'))
    subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sources),str(Path(__file__).with_name('trainingSelfBuffTest.java'))],cwd=ROOT,check=True)
    subprocess.run(['java','-cp',str(tmp/'classes')+os.pathsep+cp,'custom.fakeplayersai.trainingSelfBuffTest',str(ROOT/'game')],cwd=tmp,check=True)
    # Only the pulse body is extracted verbatim. Doubles model admission/cost/
    # request bookkeeping, not core event dispatch, effects or cast completion.
    fixture='''
import java.util.*;
public class TrainingSelfBuffPulse {
static int checks; static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
enum AbnormalType { NONE, BUFF }
static class Skill {
 int id; boolean allowed=true, magic, statik, condition=true; double mp=5,initial=2,hp; int charges,items; AbnormalType abnormal=AbnormalType.NONE;
 Skill(int i){id=i;} int getId(){return id;} AbnormalType getAbnormalType(){return abnormal;} int getAbnormalLevel(){return 1;}
 double getHpConsume(){return hp;} boolean isStatic(){return statik;} boolean isMagic(){return magic;}
 int getChargeConsumeCount(){return charges;} int getItemConsumeCount(){return items;} int getItemConsumeId(){return 1;}
 boolean checkCondition(Player b,Player t,boolean x){return condition;}
}
static class BuffInfo { Skill s; BuffInfo(Skill v){s=v;} Skill getSkill(){return s;} }
static class Effects { BuffInfo stacked; BuffInfo getBuffInfoByAbnormalType(AbnormalType a){return stacked;} }
static class Inventory { int count=10; int getInventoryItemCount(int id,int enchant){return count;} }
static class Stats { double getMpInitialConsume(Skill s){return s.initial;} double getMpConsume(Skill s){return s.mp;} }
static class Player {
 boolean human,online=true,preparing=true,dead,casting,simultaneous,control,disabled,muted,physicalMuted; double mp=100,hp=100; int charges=7,requests,accepted; boolean accept=true;
 Map<Integer,Skill> skills=new HashMap<>(); Set<Integer> active=new HashSet<>(),reuse=new HashSet<>(); Effects effects=new Effects(); Inventory inv=new Inventory(); Player target;
 Object getClient(){return human?this:null;} boolean isOnline(){return online;} boolean isAlikeDead(){return dead;} boolean isCastingNow(){return casting;} boolean isCastingSimultaneouslyNow(){return simultaneous;}
 boolean isOutOfControl(){return control;} boolean isAllSkillsDisabled(){return disabled;} Skill getKnownSkill(int id){return skills.get(id);} boolean isSkillDisabled(Skill s){return reuse.contains(s.id);}
 Effects getEffectList(){return effects;} double getCurrentMp(){return mp;} double getCurrentHp(){return hp;} Stats getStat(){return new Stats();} int getCharges(){return charges;}
 boolean isMuted(){return muted;} boolean isPhysicalMuted(){return physicalMuted;} Inventory getInventory(){return inv;}
 void setTarget(Player p){target=p;} boolean useMagic(Skill s,boolean force,boolean shift){check(force&&shift&&target==this);requests++;if(accept){accepted++;casting=true;return true;}return false;}
}
static class TrainingArena {static boolean isPreparingBot(Player p){return p.preparing;}}
static class BotClassPreset { int[] ids; BotClassPreset(int...i){ids=i;} int[] getBuffs(){return ids;} }
static boolean trainingPreparationSkillAllowed(Skill s){return s!=null&&s.allowed;}
static boolean isSelfBuffApplied(Player p,int id){return p.active.contains(id);}
''' + pulse + '''
static Player bot(){Player p=new Player();p.skills.put(1,new Skill(1));p.skills.put(2,new Skill(2));return p;}
public static void main(String[] args){
 BotClassPreset preset=new BotClassPreset(1,2);check(!applyTrainingPreparationSelfBuffs(null,preset));check(!applyTrainingPreparationSelfBuffs(bot(),null));
 for(int g=0;g<9;g++){Player p=bot();switch(g){case 0:p.human=true;break;case 1:p.online=false;break;case 2:p.preparing=false;break;case 3:p.dead=true;break;case 4:p.casting=true;break;case 5:p.simultaneous=true;break;case 6:p.control=true;break;case 7:p.disabled=true;break;default:p.physicalMuted=true;}check(!applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==0);}
 Player p=bot();check(applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==1&&p.accepted==1&&p.casting);check(!applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==1);
 for(int g=0;g<10;g++){p=bot();Skill first=p.skills.get(1);switch(g){case 0:p.active.add(1);break;case 1:p.reuse.add(1);break;case 2:first.allowed=false;break;case 3:p.skills.remove(1);break;case 4:first.condition=false;break;case 5:first.mp=101;break;case 6:first.hp=100;break;case 7:first.charges=8;break;case 8:first.items=11;break;default:first.magic=true;p.muted=true;}check(applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==1);}
 p=bot();p.mp=6;check(!applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==0);p.mp=7;check(applyTrainingPreparationSelfBuffs(p,preset));
 p=bot();p.effects.stacked=new BuffInfo(new Skill(3));p.skills.get(1).abnormal=AbnormalType.BUFF;check(applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==1);
 p=bot();p.accept=false;check(!applyTrainingPreparationSelfBuffs(p,preset));check(p.requests==2&&p.accepted==0);p=bot();check(!applyTrainingPreparationSelfBuffs(p,new BotClassPreset(99)));check(p.requests==0);
 System.out.println("PASS: "+checks+" isolated verbatim-production selfbuff pulse checks (not core cast simulation)");
}}
'''
    source=tmp/'TrainingSelfBuffPulse.java'; source.write_text(fixture)
    subprocess.run(['java',str(source)],cwd=tmp,check=True)
    # Verbatim production grant/selection/known-skill application with actor doubles.
    # This checks authorization, idempotence and learned levels, NOT live stat math.
    grant=method('public static void applyTrainingClassBuffs(')
    ids=method('static int[] trainingClassBuffIds(')
    application=method('private static void applyOpeningSelfBuff(')
    assert 'getKnownSkill(skillId)' in application and 'skill.applyEffects(player, player);' in application
    for text in (grant,ids,application):
        for forbidden in ('setCurrentHp(', 'addSkill(', 'getMaxLevel(', 'useMagic(', 'MagicSkillUse(', 'resetTimeStamps('):
            assert forbidden not in text, forbidden
    class_fixture='''
import java.util.*;
public class TrainingOrcBuffGrant {
static int checks; static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
static java.util.logging.Logger LOGGER=java.util.logging.Logger.getLogger("isolatedGrant");
static class Skill {
 int id,level; Skill(int i,int l){id=i;level=l;} int getId(){return id;} String getName(){return "Skill"+id;}
 void applyEffects(Player p,Player target){check(p==target);p.active.add(id);p.applied.add(this);}
}
static class Player {
 int cid; boolean human,online=true,dead,training=true; double hp=1000;
 Map<Integer,Skill> known=new HashMap<>();Set<Integer> active=new HashSet<>();List<Skill> applied=new ArrayList<>();
 Player(int c){cid=c;known.put(176,new Skill(176,3));known.put(420,new Skill(420,1030));}
 Object getClient(){return human?this:null;} boolean isOnline(){return online;} boolean isAlikeDead(){return dead;}
 String getName(){return "TrainingBot";} PlayerClass getPlayerClass(){return new PlayerClass(cid);} Skill getKnownSkill(int id){return known.get(id);}
 Effects getEffectList(){return new Effects(active);}
}
record PlayerClass(int id){int getId(){return id;}}
record Effects(Set<Integer> active){boolean isAffectedBySkill(int id){return active.contains(id);}}
record BotClassPreset(int cid){int getClassId(){return cid;}}
static class TrainingArena{static boolean isTrainingBot(Player p){return p.training;}}
''' + grant + '\n' + ids + '\n' + application + '''
public static void main(String[] args){
 LOGGER.setLevel(java.util.logging.Level.OFF);
 BotClassPreset titan=new BotClassPreset(113), gk=new BotClassPreset(114);
 Player p=new Player(113);applyTrainingClassBuffs(p,titan);check(p.active.equals(Set.of(176,420)));check(p.applied.size()==2);check(p.applied.get(0)==p.known.get(176)&&p.applied.get(1)==p.known.get(420));check(p.applied.get(1).level==1030&&p.hp==1000);applyTrainingClassBuffs(p,titan);check(p.applied.size()==2);
 p=new Player(114);applyTrainingClassBuffs(p,gk);check(p.active.equals(Set.of(420)));check(p.applied.size()==1&&p.applied.get(0).level==1030);
 for(int gate=0;gate<5;gate++){p=new Player(113);switch(gate){case 0:p.human=true;break;case 1:p.training=false;break;case 2:p.dead=true;break;case 3:p.online=false;break;default:p.cid=114;}applyTrainingClassBuffs(p,titan);check(p.applied.isEmpty());}
 p=new Player(113);p.known.remove(176);applyTrainingClassBuffs(p,titan);check(p.active.equals(Set.of(420)));check(!p.known.containsKey(176));
 p=new Player(113);applyTrainingClassBuffs(p,null);check(p.applied.isEmpty());applyTrainingClassBuffs(null,titan);
 for(int cid:new int[]{88,90,91,92,93,94,95,101,102,103,106,108,109,110,115,131}){p=new Player(cid);applyTrainingClassBuffs(p,new BotClassPreset(cid));check(p.applied.isEmpty());}
 System.out.println("PASS: "+checks+" isolated verbatim-production Orc buff grant checks; no live effect simulation");
}}
'''
    source=tmp/'TrainingOrcBuffGrant.java'; source.write_text(class_fixture)
    subprocess.run(['java',str(source)],cwd=tmp,check=True)
