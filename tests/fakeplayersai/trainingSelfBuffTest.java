package custom.fakeplayersai;

import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.util.StatSet;

/** Real Skill metadata and production eligibility helper, without actors/DB. */
public final class trainingSelfBuffTest
{
    private static int checks;
    private static void check(boolean yes) { if (!yes) throw new AssertionError("check " + checks); checks++; }
    private static Skill skill(int id, String type, String target)
    {
        StatSet s=new StatSet(); s.set("skill_id",id); s.set("level",1); s.set("operateType",type); s.set("targetType",target);
        return new Skill(s);
    }
    private static final class Named extends AbstractEffect
    {
        Named(String name) { super(null,null,set(name),new StatSet()); }
        private static StatSet set(String name) { StatSet s=new StatSet(); s.set("name",name); return s; }
    }
    public static void main(String[] args)
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=java.nio.file.Path.of(args[0]).toFile();
        check(!BotSpawner.trainingPreparationSkillAllowed(null));
        for(String type:new String[]{"A2","T"}) for(String target:new String[]{"SELF","ONE"})
            check(BotSpawner.trainingPreparationSkillAllowed(skill(20000,type,target)));
        for(String target:new String[]{"AURA","AREA","PARTY","CLAN","PARTY_MEMBER","CORPSE","GROUND"})
            check(!BotSpawner.trainingPreparationSkillAllowed(skill(20000,"A2",target)));
        check(!BotSpawner.trainingPreparationSkillAllowed(skill(20000,"P","SELF")));
        // Instant heals/crafting/conversions must not monopolize the prep window.
        for(int id:new int[]{262,324,417,440,1157,1216,1387})
            check(!BotSpawner.trainingPreparationSkillAllowed(skill(id,"A1","SELF")));
        for(int id:new int[]{917,445,446,770,768,769,337,1532,110,368,527,439,1355,1356,1357,1363,1413,1414})
            check(!BotSpawner.trainingPreparationSkillAllowed(skill(id,"A2","SELF")));
        for(String name:new String[]{"FocusEnergy","FocusSouls"})
        {
            Skill s=skill(20000,"A1","SELF"); s.addEffect(EffectScope.GENERAL,new Named(name));
            check(BotSpawner.trainingPreparationSkillAllowed(s));
        }
        check(!BotSpawner.applyTrainingPreparationSelfBuffs(null,null));
        System.out.println("PASS: "+checks+" production training selfbuff eligibility checks (real Skill API; no live actors)");
    }
}
