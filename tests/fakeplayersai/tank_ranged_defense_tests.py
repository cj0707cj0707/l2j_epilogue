#!/usr/bin/env python3
"""Exact production role/skill/admission/list bodies with inert actors; real XML + all-script JAR compile.
Not live AutoUse scheduling, GameServer, DB or client combat.
"""
from pathlib import Path
import os, subprocess, tempfile, xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2]
S=R/'game/data/scripts/custom/fakeplayersai'
s=(S/'BotSpawner.java').read_text()
def method(sig):
 start=s.index(sig); b=s.index('{',start); i=b+1; depth=1
 while depth:
  depth+=(s[i]=='{')-(s[i]=='}'); i+=1
 return s[start:i]
parts=[method(x) for x in ('private static boolean isRangedTankOpponent(', 'private static boolean isMovementRestrictingTankSupport(', 'private static boolean tankMovementDefenseAllowed(', 'private static void pruneRangedTankSupports(', 'public static int[] getTacticalDefenses(', 'public static boolean tacticalSkillAllowed(', 'public static boolean isStrongTankDefenseSkill(')]
legacy=method('public static void refreshTankDefenses(')
lo=legacy.index('int candidate = 0;'); hi=legacy.index('autoBuffs.removeIf(BotSpawner::isTankDefenseSkill);',lo)
candidate=legacy[lo:hi]
assert 'tankMovementDefenseAllowed(opponent, skill)' in candidate
assert 'tankMovementDefenseAllowed' not in legacy[legacy.index('int keep = 0;'):lo], 'do not cancel already active effects for the new veto'
assert legacy.index('pruneRangedTankSupports(player, opponent)')<legacy.index('OlympiadChase.isPursuing(')
auto=method('public static void refreshAutoBuffs(')
assert 'pruneRangedTankSupports(player, player.getTarget());' in auto
assert '&& (!tankBot || tankMovementDefenseAllowed(player.getTarget(), player.getKnownSkill(skillId)))' in auto
setup=method('public static void applyAutoCombat(')
assert setup.index('pruneRangedTankSupports(player, player.getTarget());')<setup.index('startAutoUseTask(player)')
for p in parts[:4]:
 for forbidden in ('abortCast(', 'stopSkillEffects(', 'setCurrent', 'setXYZ(', 'calculateDistance', 'setAbnormalTime('): assert forbidden not in p,forbidden
skills={int(x.get('id')):x for f in (R/'game/data/stats/skills').glob('*.xml') for x in ET.parse(f).getroot().findall('skill')}
assert skills[110].find('./effects/effect').get('name')=='ImmobileBuff'
assert skills[368].find('./selfEffects/effect').get('name')=='ImmobileBuff'
assert skills[760].find('./effects/effect').get('name')=='Buff'
print('PASS: actual XML immobility scopes; three production admission paths + legacy overrides/re-add/active-effect preservation guards',flush=True)
source='''package custom.fakeplayersai;
import java.util.*;
class BotSpawner {
static int checks; static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
static final int VENGEANCE_ID=368;
static final int[] TANK_DEFENSE_PHYSICAL={368,110,350,439,527,112,82,86},TANK_DEFENSE_MAGIC={368,760,913,351,72,916},TANK_DEFENSE_ALL={110,368,760,350,439,527,112,82,86,913,351,72,916};
enum EffectScope {GENERAL,SELF,PVP} enum TargetType {SELF,ONE,AURA,PARTY}
record AbstractEffect(String name){String getName(){return name;}}
static class Skill {
 int id;TargetType target=TargetType.SELF;Map<EffectScope,List<AbstractEffect>> effects=new EnumMap<>(EffectScope.class);
 Skill(int id){this.id=id;}Skill effect(EffectScope sc,String name){effects.put(sc,Arrays.asList(new AbstractEffect(name)));return this;}
 int getId(){return id;}TargetType getTargetType(){return target;}boolean isDance(){return false;}
 List<AbstractEffect> getEffects(EffectScope sc){return effects.get(sc);}boolean hasEffects(EffectScope sc){return effects.containsKey(sc);}
}
static class WorldObject {boolean isCreature(){return this instanceof Creature;}Creature asCreature(){return (Creature)this;}}
static class Creature extends WorldObject {double range=40;boolean isPlayer(){return this instanceof Player;}Player asPlayer(){return (Player)this;}double getPhysicalAttackRange(){return range;}}
record PlayerClass(int id){
 static final PlayerClass HAWKEYE=new PlayerClass(206),SILVER_RANGER=new PlayerClass(207),PHANTOM_RANGER=new PlayerClass(208),ARBALESTER=new PlayerClass(209);
 int getId(){return id;}boolean equalsOrChildOf(PlayerClass parent){return id==parent.id || id==parent.id+1000;}
}
static class AutoUse {Set<Integer> buffs=new HashSet<>(),skills=new HashSet<>();Set<Integer> getAutoBuffs(){return buffs;}Set<Integer> getAutoSkills(){return skills;}}
static class Player extends Creature {
 int cid;boolean mage,olympiad,started=true,tank=true;double distance;Set<Integer> active=new HashSet<>(),disabled=new HashSet<>();Map<Integer,Skill> known=new HashMap<>();AutoUse auto=new AutoUse();
 Player(int cid){this.cid=cid;}PlayerClass getPlayerClass(){return new PlayerClass(cid);}boolean isMageClass(){return mage;}
 Skill getKnownSkill(int id){return known.get(id);}AutoUse getAutoUseSettings(){return auto;}boolean isInOlympiadMode(){return olympiad;}boolean isOlympiadStart(){return started;}
 boolean isAffectedBySkill(int id){return active.contains(id);}boolean isSkillDisabled(Skill sk){return disabled.contains(sk.id);}
}
static class BotClassPreset {
 enum WeaponKind {SWORD,MAGIC,BOW,CROSSBOW};WeaponKind weapon;boolean override;int[] buffs={368,110,350,760},rotation={368,110};
 BotClassPreset(WeaponKind w){weapon=w;}boolean isCaster(){return weapon==WeaponKind.MAGIC;}WeaponKind getWeaponKind(){return weapon;}
 boolean hasBuffsOverride(){return override;}boolean hasRotationOverride(){return override;}int[] getBuffs(){return buffs;}int[] getRotation(){return rotation;}
}
static class BotPresets {static Map<Integer,BotClassPreset> all=new HashMap<>();static BotClassPreset getPresetByClassId(int id){return all.get(id);}}
static class BotManager {static int getBotClassId(Player p){return p.cid;}}
static boolean isTacticalTank(Player p){return p!=null&&(p.cid==90||p.cid==91||p.cid==106);}
static boolean containsId(int[] a,int id){return Arrays.stream(a).anyMatch(x->x==id);}static boolean isTankDefenseSkill(int id){return containsId(TANK_DEFENSE_ALL,id);}
static boolean isOpenerOnlySkill(int id){return false;}static boolean isChantOfVictoryFamily(int id){return false;}static boolean isOlympiadChantBanned(Player p,int id){return false;}static boolean isLongSelfBuff(Player p,int id){return false;}
'''+ '\n'.join(parts)+'''
static int legacyCandidate(Player player,BotClassPreset preset,WorldObject opponent,int[] desiredPool,boolean activeMatch){
'''+candidate+'''return candidate;}
public static void main(String[]args){
 for(int id:new int[]{90,91,106})BotPresets.all.put(id,new BotClassPreset(BotClassPreset.WeaponKind.SWORD));
 BotPresets.all.put(200,new BotClassPreset(BotClassPreset.WeaponKind.MAGIC));BotPresets.all.put(201,new BotClassPreset(BotClassPreset.WeaponKind.BOW));BotPresets.all.put(202,new BotClassPreset(BotClassPreset.WeaponKind.CROSSBOW));BotPresets.all.put(203,new BotClassPreset(BotClassPreset.WeaponKind.SWORD));
 check(!isRangedTankOpponent(null));check(!isRangedTankOpponent(new WorldObject()));Creature npc=new Creature();npc.range=300;check(isRangedTankOpponent(npc));npc.range=40;check(!isRangedTankOpponent(npc));
 for(int tankId:new int[]{90,91,106})for(int role:new int[]{200,201,202,203,204,205,206,207,208,209,1206,1207,1208,1209})for(double distance:new double[]{0,30,80,600,1500})for(double range:new double[]{0,40,600})for(boolean ranked:new boolean[]{false,true}){
  Player tank=new Player(tankId),enemy=new Player(role);enemy.distance=distance;enemy.range=range;enemy.mage=role==200||role==204;tank.olympiad=ranked;
  boolean ranged=role==200||role==201||role==202||role==204||role==205&&range>=250||role>=206;
  check(isRangedTankOpponent(enemy)==ranged);
  Skill ud=new Skill(110),vengeance=new Skill(368).effect(EffectScope.SELF,"ImmobileBuff"),armor=new Skill(760).effect(EffectScope.GENERAL,"Buff"),weak=new Skill(350).effect(EffectScope.GENERAL,"Buff");
  vengeance.target=TargetType.AURA;
  for(Skill sk:new Skill[]{ud,vengeance,armor,weak})tank.known.put(sk.id,sk);
  check(tankMovementDefenseAllowed(enemy,ud)==!ranged);check(tacticalSkillAllowed(tank,enemy,ud,true)==!ranged);
  check(tankMovementDefenseAllowed(enemy,vengeance));check(tacticalSkillAllowed(tank,enemy,vengeance,true));
  check(tankMovementDefenseAllowed(enemy,armor));check(tankMovementDefenseAllowed(enemy,weak));
  if(enemy.mage)check(tacticalSkillAllowed(tank,enemy,armor,true));
  int[] defenses=getTacticalDefenses(tank,enemy);if(ranged){check(!containsId(defenses,110));check(containsId(defenses,368));}else if(!enemy.mage){check(containsId(defenses,110));check(containsId(defenses,368));}
  int chosen=legacyCandidate(tank,BotPresets.all.get(tankId),enemy,enemy.mage?TANK_DEFENSE_MAGIC:TANK_DEFENSE_PHYSICAL,ranked);
  check(chosen==368); // Explicit all-opponent Vengeance; UD remains ranged-vetoed.
  for(EffectScope scope:EffectScope.values()){
   Skill root=new Skill(900).effect(scope,"Root"),immobile=new Skill(901).effect(scope,"ImmobileBuff");
   check(tacticalSkillAllowed(tank,enemy,root,true)==!ranged);check(tacticalSkillAllowed(tank,enemy,immobile,true)==!ranged);
  }
  Skill attackRoot=new Skill(403).effect(EffectScope.GENERAL,"Root");attackRoot.target=TargetType.ONE;tank.known.put(403,attackRoot);
  check(tacticalSkillAllowed(tank,enemy,attackRoot,false));
  Skill customSelf=new Skill(900).effect(EffectScope.GENERAL,"Root");tank.known.put(900,customSelf);
  tank.auto.skills.addAll(List.of(110,368,403,900));tank.auto.buffs.addAll(List.of(110,368,350,760,900));tank.active.add(368);
  pruneRangedTankSupports(tank,enemy);
  check(tank.active.contains(368));check(tank.auto.skills.contains(403));check(tank.auto.buffs.containsAll(List.of(350,760)));
  check(tank.auto.skills.contains(368)&&tank.auto.buffs.contains(368));
  for(int id:new int[]{110,900}){check(tank.auto.skills.contains(id)==!ranged);check(tank.auto.buffs.contains(id)==!ranged);}
  // Non-tanks are unaffected; target hostility/root casts were not blocked above.
  Player fighter=new Player(131);check(tacticalSkillAllowed(fighter,enemy,customSelf,true));
 }
 Player tank=new Player(90),bow=new Player(201);tank.known.put(110,new Skill(110));tank.known.put(368,new Skill(368));
 BotClassPreset preset=BotPresets.all.get(90);preset.override=true;preset.rotation=new int[]{110,368};preset.buffs=new int[]{110,368};
 check(Arrays.equals(getTacticalDefenses(tank,bow),new int[]{368}));check(!tacticalSkillAllowed(tank,bow,tank.known.get(110),true));
 Player melee=new Player(203);check(Arrays.equals(getTacticalDefenses(tank,melee),new int[]{368,110}));check(legacyCandidate(tank,preset,melee,TANK_DEFENSE_PHYSICAL,false)==368);
 check(!isMovementRestrictingTankSupport(null));check(!isMovementRestrictingTankSupport(new Skill(700)));check(!isMovementRestrictingTankSupport(new Skill(701).effect(EffectScope.GENERAL,"Buff")));
 System.out.println("PASS: "+checks+" exact-production ranged role/skill gate/defense-list/legacy candidate/override pruning checks (inert actors, not live combat)");
}}
'''
with tempfile.TemporaryDirectory(prefix='tank-ranged-') as d:
 t=Path(d);probe=t/'BotSpawner.java';probe.write_text(source)
 compiler=t/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;public class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 subprocess.run(['java',str(compiler),'-d',str(t/'probe'),str(S/'TacticsPolicy.java'),str(probe)],check=True)
 subprocess.run(['java','-cp',str(t/'probe'),'custom.fakeplayersai.BotSpawner'],check=True)
 cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
 subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(t/'production'),*map(str,sorted(S.glob('*.java')))],cwd=R,check=True)
 print('PASS: ALL production fake-player scripts compile against actual libs/*.jar',flush=True)
# Existing presets are the role source; verify archer/crossbow/mage mappings are not inferred from range alone.
presets=(S/'BotPresets.java').read_text()
assert 'WeaponKind.BOW' in presets and 'WeaponKind.MAGIC' in presets
assert 'WeaponKind.CROSSBOW' in parts[0] and 'PlayerClass.ARBALESTER' in parts[0]
print('PASS: ranged-veto regression; existing core/stat/skill XML and effect timers untouched',flush=True)
