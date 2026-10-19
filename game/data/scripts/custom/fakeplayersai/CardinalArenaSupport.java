package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

/** State-owned opening60s native3282 effect; never modifies MP regeneration. */
final class CardinalArenaSupport
{
    private WeakReference<Player> source = new WeakReference<>(null), opponent = new WeakReference<>(null);
    private WeakReference<BuffInfo> granted = new WeakReference<>(null);
    private boolean attempted;
    // Retained for source compatibility; binding alone must never modify regeneration.
    void bind(Player bot, Player enemy)
    {
        if(bot==null||enemy==null||bot==enemy||bot.getClient()!=null||bot.getPlayerClass().getId()!=97)
        {clear();return;}
        if(source.get()==bot&&opponent.get()==enemy)return;
        clear();source=new WeakReference<>(bot);opponent=new WeakReference<>(enemy);
    }
    static int remaining(long started,long now)
    {long elapsed=Math.max(0,now-started);return elapsed>=60000?0:(int)((60000-elapsed+999)/1000);}
    void start(Player bot,Player enemy,long started,long now)
    {
        bind(bot,enemy);
        if(bot==null||enemy==null||source.get()!=bot||opponent.get()!=enemy)return;
        int seconds=remaining(started,now);
        if(seconds<=0){removeGranted();attempted=true;return;}
        if(attempted)return;
        attempted=true; // once per pair/combat, no refresh after dispel or expiry
        Skill skill=SkillData.getInstance().getSkill(3282,1);
        if(skill==null)throw new IllegalStateException("Missing native Maximum Clarity3282");
        BuffInfo previous=enemy.getEffectList().getBuffInfoBySkillId(3282);
        skill.applyEffects(enemy,enemy,true,seconds); // self-use effect; duration override is SECONDS
        BuffInfo effect=enemy.getEffectList().getBuffInfoBySkillId(3282);
        if(effect!=null&&effect!=previous&&effect.getSkill()==skill)granted=new WeakReference<>(effect);
    }
    boolean active(){BuffInfo b=granted.get();return b!=null&&!b.isRemoved()&&b.isInUse();}
    private void removeGranted()
    {
        BuffInfo effect=granted.get();Player enemy=opponent.get();granted.clear();
        if(enemy!=null&&effect!=null&&!effect.isRemoved())enemy.getEffectList().remove(SkillFinishType.REMOVED,effect);
    }
    void clear()
    {removeGranted();Player old=opponent.get();if(old!=null)old.removeStatsOwner(this);source.clear();opponent.clear();attempted=false;}
}
