package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.util.StatSet;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Full production buff entrypoints with real Player/Skill/EffectList queries.
 * Effect application alone is recorded, not scheduled: no DB/client/live server. */
public class DominatorServerVictoryTest
{
    static Unsafe unsafe;
    static int checks;
    static final Map<Player,List<Skill>> grants=new IdentityHashMap<>();
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static class RecordingSkill extends Skill
    {
        private RecordingSkill(){super(new StatSet());} // allocated without constructor
        @Override public void applyEffects(Creature caster,Creature target)
        {
            check(caster==target&&target instanceof Player,"server buff affects recipient only, never PARTY_CLAN targets");
            Player p=target.asPlayer();grants.computeIfAbsent(p,k->new ArrayList<>()).add(this);
            try
            {
                BuffInfo info=(BuffInfo)unsafe.allocateInstance(BuffInfo.class);
                f(BuffInfo.class,"_skill").set(info,this);f(BuffInfo.class,"_effector").set(info,p);f(BuffInfo.class,"_effected").set(info,p);
                p.getEffectList().getBuffs().add(info);
            }
            catch(Exception e){throw new IllegalStateException(e);}
        }
    }
    static Skill record(Skill skill)throws Exception
    {
        RecordingSkill copy=(RecordingSkill)unsafe.allocateInstance(RecordingSkill.class);
        for(Field field:Skill.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(copy,field.get(skill));}
        return copy;
    }
    static long victoryCount(Player p){return grants.getOrDefault(p,List.of()).stream().filter(s->s.getId()==1414).count();}
    static void grant(Player p,boolean ranked){if(ranked)BotSpawner.maintainFullBuffs(p,true);else BotSpawner.applyTrainingOlympiadBuffs(p,true);}
    @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        handlers.EffectMasterHandler.main(new String[0]);SkillData data=SkillData.getInstance();
        Skill nativeVictory=data.getSkill(1414,1);
        check(nativeVictory.getName().equals("Victory of Pa'agrio")&&nativeVictory.getAbnormalTime()==300,"native1414 base five minute duration");
        check(nativeVictory.getTargetType()==org.l2jmobius.gameserver.mechanics.skill.targets.TargetType.PARTY_CLAN,"native PARTY_CLAN unchanged");
        Method profile=BotSpawner.class.getDeclaredMethod("getOlympiadBuffs",boolean.class);profile.setAccessible(true);
        Set<Integer> hashes=new HashSet<>();
        for(String field:new String[]{"OLYMPIAD_BUFFS","FULL_BUFFS_MAGE","FULL_BUFFS_FIGHTER"})for(int[] pair:(int[][])f(BotSpawner.class,field).get(null))hashes.add(SkillData.getSkillHashCode(pair[0],pair[1]));
        for(int level:new int[]{1,115,215,315})hashes.add(SkillData.getSkillHashCode(1414,level));
        profile.invoke(null,true);profile.invoke(null,false);
        Map<Integer,Skill> templates=(Map<Integer,Skill>)f(SkillData.class,"_skillsByHash").get(data),originals=new HashMap<>();
        for(int hash:hashes){Skill original=templates.get(hash);if(original!=null){originals.put(hash,original);templates.put(hash,record(original));}}
        BotClassPreset preset=BotPresets.getPresetByClassId(115);
        int[] savedBuffs=(int[])f(BotClassPreset.class,"_buffsOverride").get(preset),savedRotation=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
        try
        {
            for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int override:new int[]{0,1,2})for(int learned:new int[]{0,1,115,215,315})
            {
                preset.setBuffsOverride(override==0?null:override==1?new int[0]:new int[]{337});preset.setRotationOverride(override==0?null:new int[]{1245});
                Player p=actor(115,ranked,human);f(Player.class,"_olympiadStart").setBoolean(p,false);mp(p,0);
                if(learned!=0)learn(p,data.getSkill(1414,learned));
                Skill known=p.getKnownSkill(1414);grant(p,ranked);
                check(p.getEffectList().isAffectedBySkill(1414)&&victoryCount(p)==1,"Dominator receives extra1414 regardless of learned/override/human/mode");
                check(p.getEffectList().isAffectedBySkill(1059)&&p.getEffectList().isAffectedBySkill(1204),"existing mage server profile preserved");
                Skill granted=grants.get(p).stream().filter(s->s.getId()==1414).findFirst().orElseThrow();
                check(granted.getLevel()==(learned==0?1:learned),"preserve learned enchant; native base fallback without skill grant");
                check(p.getKnownSkill(1414)==known&&p.getCurrentMp()==0,"server grant neither learns skill nor spends/refills MP");
                check(BotSpawner.stripChantBuffs(p)==0&&p.getEffectList().isAffectedBySkill(1414),"common CoV strip keeps class-specific1414 even unlearned");
                grant(p,ranked);check(victoryCount(p)==1,"existing1414 not refreshed/replaced in preparation");
                if(ranked)
                {
                    p.getEffectList().getBuffs().removeIf(i->i.getSkill().getId()==1414);f(Player.class,"_olympiadStart").setBoolean(p,true);
                    grant(p,true);check(victoryCount(p)==1&&!p.getEffectList().isAffectedBySkill(1414),"no free in-combat reapplication");
                }
            }
            for(BotClassPreset other:BotPresets.getPresets())if(other.getClassId()!=115)for(boolean ranked:new boolean[]{false,true})
            {
                Player p=actor(other.getClassId(),ranked,false);f(Player.class,"_olympiadStart").setBoolean(p,false);learn(p,data.getSkill(1414,1));grant(p,ranked);
                check(victoryCount(p)==0&&!p.getEffectList().isAffectedBySkill(1414),"other class never receives extra1414:"+other.getClassId());
            }
        }
        finally{originals.forEach(templates::put);preset.setBuffsOverride(savedBuffs);preset.setRotationOverride(savedRotation);}
        check(data.getSkill(1414,1)==nativeVictory,"native shared templates restored after fixture");
        System.out.println("PASS: "+checks+" production Dominator server-buff checks; full ranked/training profiles, native gates and recorded effect-application seam; no live server");
    }
}
