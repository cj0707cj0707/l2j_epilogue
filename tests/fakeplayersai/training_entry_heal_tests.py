#!/usr/bin/env python3
"""Verbatim production entry heal and tank-pool gates with bounded actor doubles. Not live effects."""
from pathlib import Path
import subprocess,tempfile
R=Path(__file__).resolve().parents[2]
a=(R/'game/data/scripts/custom/fakeplayersai/TrainingArena.java').read_text()
b=(R/'game/data/scripts/custom/fakeplayersai/BotSpawner.java').read_text()
def method(text,signature):
 start=text.index(signature);brace=text.index('{',start);i=brace+1;depth=1
 while depth:
  if text[i]=='{':depth+=1
  elif text[i]=='}':depth-=1
  i+=1
 return text[start:i]
heal=method(a,'private static void restoreEntryResources(')
assert 'getPlayerClass' not in heal and 'reset' not in heal
strong=method(b,'public static boolean isStrongTankDefenseSkill(')
gate=method(b,'public static boolean tacticalSkillAllowed(')
source='''import java.util.*;
class TrainingEntryHealRegression {
static int checks; static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
static int VENGEANCE_ID=368, ESCAPE_SHACKLE_ID=453;
static int[] TANK_DEFENSE_ALL={110,368,760,350,439,527,112,82,86,913,351,72,916};
static int[] TANK_DEFENSE_PHYSICAL={368,110,350,439,527,112,82,86},TANK_DEFENSE_MAGIC={368,760,913,351,72,916};
static class Player {
 int cid,broadcasts;boolean bot,olympiad,started=true,mage,tank=true;double hp,mp,cp,maxHp=1000,maxMp=800,maxCp=600;Set<Integer> active=new HashSet<>();
 double getMaxHp(){return maxHp;}double getMaxMp(){return maxMp;}double getMaxCp(){return maxCp;}
 void setCurrentHp(double v){hp=v;}void setCurrentMp(double v){mp=v;}void setCurrentCp(double v){cp=v;}void broadcastUserInfo(){broadcasts++;}
 boolean isInOlympiadMode(){return olympiad;}boolean isOlympiadStart(){return started;}boolean isMageClass(){return mage;}boolean isSleeping(){return false;}boolean isAffectedBySkill(int id){return active.contains(id);}
}
record Skill(int id){int getId(){return id;}boolean isDance(){return false;}}
// This existing fixture isolates heal/exclusivity; the real role gate is tested by tank_ranged_defense_tests.py.
static boolean tankMovementDefenseAllowed(Player enemy,Skill skill){return true;}
static boolean isOpenerOnlySkill(int id){return false;}static boolean isTacticalTank(Player p){return p.tank;}
static boolean isTankDefenseSkill(int id){return containsId(TANK_DEFENSE_ALL,id);}static boolean isChantOfVictoryFamily(int id){return false;}static boolean isOlympiadChantBanned(Player p,int id){return false;}
static boolean isLongSelfBuff(Player p,int id){return false;}static boolean containsId(int[] list,int id){return Arrays.stream(list).anyMatch(v->v==id);}
static class CardinalSurvival {static boolean active(Player p,Player e){return false;}static boolean allowed(int id){return true;}}
static class TankClassMechanics {static boolean allowed(Player p,int id){return true;}static boolean skill(int id){return false;}}
'''+heal+'\n'+strong+'\n'+gate+'''
public static void main(String[]args){
 for(int cid=0;cid<=140;cid++)for(boolean bot:new boolean[]{false,true})for(double before:new double[]{0,0.15,0.7,1}){
  Player p=new Player();p.cid=cid;p.bot=bot;p.hp=1000*before;p.mp=800*before;p.cp=600*before;restoreEntryResources(p);
  check(p.hp==1000&&p.mp==800&&p.cp==600&&p.broadcasts==1);
 }
 for(int id:new int[]{110,368,760})check(isStrongTankDefenseSkill(id));for(int id:new int[]{350,439,527,112,82,86,913,351,72,916})check(!isStrongTankDefenseSkill(id));
 Player p=new Player(),enemy=new Player();p.active.add(439);check(tacticalSkillAllowed(p,enemy,new Skill(110),true));check(tacticalSkillAllowed(p,enemy,new Skill(368),true));
 check(!tacticalSkillAllowed(p,enemy,new Skill(350),true));p.active.clear();p.active.add(110);check(!tacticalSkillAllowed(p,enemy,new Skill(368),true));
 p.active.clear();p.active.add(368);check(!tacticalSkillAllowed(p,enemy,new Skill(110),true));
 p.active.clear();check(tacticalSkillAllowed(p,enemy,new Skill(368),true));p.active.add(760);check(!tacticalSkillAllowed(p,enemy,new Skill(110),true));
 System.out.println("PASS: "+checks+" verbatim entry-heal/all-class and strong-defense exclusivity checks (not live effects)");
}}
'''
with tempfile.TemporaryDirectory(prefix='entry-heal-') as d:
 p=Path(d)/'TrainingEntryHealRegression.java';p.write_text(source);subprocess.run(['java',str(p)],check=True)
arrival=a[a.index('if (!s.duelRequested)'):a.index('if (s.duel == null && p.isInDuel())')]
assert arrival.index('if (p.isTeleporting() || b.isTeleporting()) return;') < arrival.index('restoreEntryResources(p); restoreEntryResources(b);') < arrival.index('DuelManager.getInstance().addDuel(p, b, 0)')
assert 'if (!s.entryHealed)' in arrival and 's.entryHealed = true;' in arrival
assert a.count('restoreEntryResources(p); restoreEntryResources(b);')==1
print('PASS: once-only acknowledged-entry integration; final class-based heal/cooldown timing unchanged')

assert arrival.index('startingDuelPending(b, STARTING_DUEL)') < arrival.index('restoreEntryResources(p); restoreEntryResources(b);')
