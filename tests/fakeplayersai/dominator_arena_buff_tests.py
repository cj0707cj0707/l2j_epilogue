#!/usr/bin/env python3
"""Exact production Dominator1414 exception/baseline/CP getters and ban maintenance.
Inert actors/effects only; no live GameServer/DB/target scheduling.
"""
from pathlib import Path
import subprocess,tempfile,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2];P=R/'game/data/scripts/custom/fakeplayersai';s=(P/'BotSpawner.java').read_text()
def method(sig):
 a=s.index(sig);b=s.index('{',a);i=b+1;d=1
 while d:d+=(s[i]=='{')-(s[i]=='}');i+=1
 return s[a:i]
methods=[method(x) for x in ('private static boolean isDominatorVictory(', 'private static boolean isOlympiadChantBanned(', 'private static void applyDominatorArenaBuff(', 'public static int[] getTacticalCpSupport(', 'private static boolean isLongSelfBuff(', 'public static boolean tacticalSkillAllowed(', 'public static int stripChantBuffs(', 'public static void trimOlympiadBannedSkillsFromAutoLists(')]
maintenance=method('public static void maintainFullBuffs(Player player, boolean caster)')
assert 'if (olympiad && !activeMatch) applyDominatorArenaBuff(player);' in maintenance
training=method('public static void applyTrainingOlympiadBuffs(')
assert training.count('applyDominatorArenaBuff(player);')==1 and training.index('applyDominatorArenaBuff(player);')>training.index('for (int[] buff : getOlympiadBuffs(caster))')
assert 'applyDominatorArenaBuff' not in (P/'BotTactics.java').read_text(), 'combat must use real casts, not free effect injection'
profile=method('private static int[][] buildOlympiadProfile(')
assert 'isChantOfVictoryFamily(buff[0])' in profile, 'do not globally allow1414 in every mage profile'
presets=(P/'BotPresets.java').read_text();dom=presets[presets.index('addPreset(115,'):presets.index('// Doombringer')]
assert '337, 1532' in dom
assert 'dominatorPresetAllows' not in method('private static void applyDominatorArenaBuff(')
ore=method('private static void giveConsumables(')
assert 'SPIRIT_ORE_ID' in ore
source='''import java.util.*;import java.util.logging.*;
class BotSpawner {
static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
static final int OLYMPIAD_MAX_SELFBUFF_SECONDS=120, ESCAPE_SHACKLE_ID=453;
static final int[] CHANT_OF_VICTORY_FAMILY={1355,1356,1357,1363,1413,1414,7061,7062,7063,7064};
static int[] TANK_DEFENSE_MAGIC={368,760},TANK_DEFENSE_PHYSICAL={368,110},TANK_DEFENSE_ALL={368,110,760};
static Logger LOGGER=Logger.getLogger("test");enum SkillFinishType{REMOVED}
record Skill(int id,int duration){int getId(){return id;}int getAbnormalTime(){return duration;}boolean isDance(){return false;}void applyEffects(Player caster,Player target){check(caster==target);if(target.effects.active.add(id))target.grants++;}}
static class SkillData {static SkillData getInstance(){return new SkillData();}Skill getSkill(int id,int level){return new Skill(id,300);}}
record PlayerClass(int id){int getId(){return id;}}
static class AutoUseSettingsHolder {Set<Integer> buffs=new HashSet<>(),skills=new HashSet<>();Set<Integer> getAutoBuffs(){return buffs;}Set<Integer> getAutoSkills(){return skills;}}
static class Effects {Set<Integer> active=new HashSet<>();boolean isAffectedBySkill(int id){return active.contains(id);}void stopSkillEffects(SkillFinishType f,int id){active.remove(id);}}
static class Player {int cid,grants;boolean oly=true,started=true;Map<Integer,Skill> known=new HashMap<>();Effects effects=new Effects();AutoUseSettingsHolder auto=new AutoUseSettingsHolder();
 Player(int cid){this.cid=cid;}PlayerClass getPlayerClass(){return new PlayerClass(cid);}Skill getKnownSkill(int id){return known.get(id);}Effects getEffectList(){return effects;}
 AutoUseSettingsHolder getAutoUseSettings(){return auto;}String getName(){return "fixture";}boolean isAlikeDead(){return false;}boolean isInOlympiadMode(){return oly;}boolean isOlympiadStart(){return started;}boolean isMageClass(){return true;}boolean isSleeping(){return false;}boolean isAffectedBySkill(int id){return effects.active.contains(id);}}
static class BotClassPreset {boolean override;int[] buffs={1414},rotation={1245};boolean hasBuffsOverride(){return override;}boolean hasRotationOverride(){return override;}int[] getBuffs(){return buffs;}int[] getRotation(){return rotation;}}
static class BotPresets{static BotClassPreset preset=new BotClassPreset();static BotClassPreset getPresetByClassId(int id){return id==115?preset:null;}}
static boolean containsId(int[]a,int id){return Arrays.stream(a).anyMatch(v->v==id);}static boolean isChantOfVictoryFamily(int id){return containsId(CHANT_OF_VICTORY_FAMILY,id);}
static void applyOpeningSelfBuff(Player p,int id){if(p.known.containsKey(id)&&p.effects.active.add(id))p.grants++;}
static boolean isTacticalTank(Player p){return false;}static boolean isTankDefenseSkill(int id){return false;}static boolean isStrongTankDefenseSkill(int id){return false;}
static boolean tankMovementDefenseAllowed(Player enemy,Skill skill){return true;}static boolean isOpenerOnlySkill(int id){return false;}static boolean isDanceOrSong(Player p,int id){return false;}
static class CardinalSurvival {static boolean active(Player p,Player e){return false;}static boolean allowed(int id){return true;}}
static class TankClassMechanics {static boolean allowed(Player p,int id){return true;}static boolean skill(int id){return false;}}
'''+ '\n'.join(methods)+'''
public static void main(String[]a){LOGGER.setLevel(Level.OFF);
 for(int cid:new int[]{90,91,94,95,103,106,110,113,114,115,131})for(boolean learned:new boolean[]{false,true}){
  Player p=new Player(cid),enemy=new Player(94);if(learned){p.known.put(1414,new Skill(1414,300));p.known.put(1305,new Skill(1305,0));p.known.put(1306,new Skill(1306,0));}
  for(int id:CHANT_OF_VICTORY_FAMILY){boolean allowed=cid==115&&id==1414;
   check(isOlympiadChantBanned(p,id)==!allowed);p.effects.active.add(id);p.auto.buffs.add(id);p.auto.skills.add(id);
  }
  stripChantBuffs(p);trimOlympiadBannedSkillsFromAutoLists(p);
  for(int id:CHANT_OF_VICTORY_FAMILY){boolean allowed=cid==115&&id==1414;check(p.effects.active.contains(id)==allowed);check(p.auto.buffs.contains(id)==allowed);check(p.auto.skills.contains(id)==allowed);}
  if(learned){check(isLongSelfBuff(p,1414)==(cid!=115));check(tacticalSkillAllowed(p,enemy,p.known.get(1414),true)==(cid==115));}
  p.effects.active.clear();applyDominatorArenaBuff(p);applyDominatorArenaBuff(p);check(p.grants==(cid==115?1:0));
  check(Arrays.equals(getTacticalCpSupport(p,enemy),cid==115&&learned?new int[]{1305,1306}:new int[0]));
 }
 Player p=new Player(115),enemy=new Player(94);p.known.put(1414,new Skill(1414,300));p.known.put(1305,new Skill(1305,0));p.known.put(1306,new Skill(1306,0));
 BotPresets.preset.override=true;BotPresets.preset.buffs=new int[]{337};BotPresets.preset.rotation=new int[]{1245};
 applyDominatorArenaBuff(p);check(p.grants==1);check(Arrays.equals(getTacticalCpSupport(p,enemy),new int[]{1305,1306}));
 BotPresets.preset.buffs=new int[]{1414,1306};applyDominatorArenaBuff(p);check(p.grants==1);check(Arrays.equals(getTacticalCpSupport(p,enemy),new int[]{1305,1306}));
 BotPresets.preset.rotation=new int[]{1305};check(Arrays.equals(getTacticalCpSupport(p,enemy),new int[]{1305,1306}));
 p.known.remove(1305);check(Arrays.equals(getTacticalCpSupport(p,enemy),new int[]{1306}));
 p.started=false;check(!tacticalSkillAllowed(p,enemy,p.known.get(1414),true));p.started=true;check(tacticalSkillAllowed(p,enemy,p.known.get(1414),true));
 check(!isDominatorVictory(null,1414));check(getTacticalCpSupport(null,enemy).length==0);
 System.out.println("PASS: "+checks+" exact production class115-only1414 exception/strip/trim/long-buff/baseline-once/mandatory server-buff/CP override checks (inert actors)");
}}
'''
with tempfile.TemporaryDirectory(prefix='dominator-arena-buff-') as d:
 p=Path(d)/'DominatorArenaBuffRegression.java';p.write_text(source);subprocess.run(['java',str(p)],check=True)
# Independently inspect actual learned victory and no changes to skill effects.
skills={int(x.get('id')):x for f in (R/'game/data/stats/skills').glob('*.xml') for x in ET.parse(f).getroot().findall('skill')}
v=skills[1414];assert v.get('name')=="Victory of Pa'agrio" and v.findtext('targetType')=='PARTY_CLAN' and v.findtext('abnormalTime')=='300'
tree=ET.parse(R/'game/data/stats/players/skillTrees/3rdClass/Dominator.xml').getroot();assert any(x.get('skillId')=='1414' for x in tree.iter('skill'))
print('PASS: real learned1414/300s/PARTY_CLAN, scoped prep-only instant grant and default-preset/source integration; no live server',flush=True)
