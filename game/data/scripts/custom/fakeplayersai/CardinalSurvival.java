package custom.fakeplayersai;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** Cardinal live shields, all-target Trance eligibility and actual sleep/wake fallback history. */
final class CardinalSurvival
{
    private CardinalSurvival() { }
    static boolean active(Player bot,Player enemy)
    {
        if(bot==null||enemy==null||bot.getClient()!=null||bot.getPlayerClass().getId()!=97)return false;
        BuffInfo b=enemy.getEffectList().getBuffInfoBySkillId(3282);
        return b!=null&&!b.isRemoved()&&b.isInUse();
    }
    /** Actual current in-use effect, not a learned passive, visual, or guessed duration. */
    static boolean live(Player bot,int id)
    {
        if(bot==null)return false;
        BuffInfo b=bot.getEffectList().getBuffInfoBySkillId(id);
        return b!=null&&!b.isRemoved()&&b.isInUse();
    }
    static boolean talismanClarity(Player enemy) { return live(enemy,3281)||live(enemy,3282); }
    static boolean celestialSkillAllowed(Player bot,int id)
    { return !sleepShield(bot)||id==1394||id==1409; }
    static boolean sleepShield(Player bot)
    { return bot!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==97&&(live(bot,1418)||live(bot,5576)); }
    static boolean ownCelestial(Player bot) { return bot!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==97&&live(bot,1418); }
    static boolean noKite(Player bot)
    {
        return bot!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==97
            &&(bot.isInvul()||live(bot,1418)||live(bot,5576));
    }
    static boolean tranceTarget(Player bot,Player enemy)
    {
        return bot!=null&&enemy!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==97
            &&bot.getKnownSkill(1394)!=null&&!enemy.isSleeping()&&!enemy.isInvul();
    }
    static boolean passiveTrance(Player bot,Player enemy)
    { return tranceTarget(bot,enemy)&&bot.isInvul()&&live(bot,5576); }
    /** Bot97 only: strict >90%, awake and vulnerable target; Clarity is irrelevant. */
    static boolean prominenceAllowed(Player bot,Player enemy)
    {
        if(bot==null||bot.getClient()!=null||bot.getPlayerClass().getId()!=97)return true;
        return !sleepShield(bot)&&enemy!=null&&bot.getCurrentHp()>bot.getMaxHp()*.90&&!enemy.isSleeping()&&!enemy.isInvul();
    }
    /** Ignore saved auto-use overrides for bot Cardinal's own Celestial; passive5576 is separate. */
    public static boolean skillAllowed(Player bot,int id)
    { return id!=1418 || bot==null || bot.getClient()!=null || bot.getPlayerClass().getId()!=97; }
    static boolean urgentHeal(Player bot)
    { return bot!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==97&&bot.getCurrentHp()<bot.getMaxHp()*.75; }
    static boolean allowed(int id) { return id==1394||id==1398||id==1230||id==1218; }
    /** Per-pair live-effect history, not accepted casts or a guessed sleep timer. */
    static final class Window
    {
        private boolean observedTrance, endedTrance;
        private int phase; //0: sleep,1: wake/Mana Burn,2: one Prominence opportunity,3: resleep
        void reset() { observedTrance = false; endedTrance = false; phase = 0; }
        boolean pending() { return observedTrance || endedTrance && phase < 3; }
        String describe() { return observedTrance ? "sleep" : endedTrance && phase == 1 ? "wake-mana" : endedTrance && phase == 2 ? "wake-prominence" : "rotation"; }
        void observe(Player bot, Player enemy)
        {
            if (bot == null || enemy == null || bot.getClient() != null || bot.getPlayerClass().getId() != 97) { reset(); return; }
            // Sleep/wake is independent of Clarity's presence or expiry. Pair changes reset in prepare.
            BuffInfo sleep = SleepRetreat.cardinalSleep(bot, enemy);
            if (sleep != null)
            { observedTrance = true; endedTrance = false; phase = 0; }
            else if (observedTrance && !enemy.isSleeping())
            { observedTrance = false; endedTrance = true; phase = 1; }
        }
        boolean manaBurn(Player enemy) { return endedTrance && phase == 1 && !enemy.isSleeping(); }
        boolean prominence(Player enemy) { return endedTrance && phase == 2 && !enemy.isSleeping(); }
        // Advance only on an accepted native request; pending/casting guards serialize the spells.
        // A rejected request cannot unlock the next spell. No cooldown/MP/condition is bypassed.
        void requested(int id)
        {
            if (id == 1398 && phase == 1) phase = 2;
            else if (id == 1230 && phase == 2) phase = 3;
            else if (id == 1394) { phase = 0; endedTrance = false; }
        }
        int choose(List<TacticsPolicy.Option> options,double hp)
        {
            for (int id : new int[]{hp<.95?1218:-1})
                if (id > 0 && has(options,id)) return id;
            if (phase == 1)
            {
                if (has(options,1398)) return 1398;
                phase = 2; // unavailable native skill: try the legal nuke, never force a cast
            }
            if (phase == 2)
            {
                if (has(options,1230)) return 1230;
                phase = 3; // on cooldown/not legally usable: skip, never wait for it to refresh
            }
            return has(options,1394) ? 1394 : -1;
        }
        private static boolean has(List<TacticsPolicy.Option> options,int id)
        { return options.stream().anyMatch(o->o.id()==id); }
    }
}
