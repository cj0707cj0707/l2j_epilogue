package custom.fakeplayersai;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;

/** Commands only an already existing living summon; never learns/casts a summon skill. */
final class SummonAttackPulse
{
    private long nextAt;
    private int targetId;
    void reset() { nextAt = 0; targetId=0; }
    void tick(Player owner, Creature enemy, long time)
    {
        if(enemy!=null&&enemy.isPlayer())
        { Summon priority=BotTactics.prioritySummon(owner,enemy.asPlayer());if(priority!=null)enemy=priority; }
        Summon summon = owner.getSummon();
        if (summon == null || summon.getOwner() != owner || summon.isAlikeDead() || summon.isTeleporting()
            || summon.getInstanceId() != owner.getInstanceId() || enemy == null || enemy.isAlikeDead()
            || enemy.getInstanceId() != owner.getInstanceId() || summon.isCastingNow() || summon.isCastingSimultaneouslyNow()
            || summon.isOutOfControl()) return;
        if(enemy.isInvul())
        {
            if(summon.isAttackingNow())summon.abortAttack();
            if(summon.getAI().getIntention()==org.l2jmobius.gameserver.ai.Intention.ATTACK)summon.getAI().setIntentionIdle();
            return;
        }
        if(time<nextAt&&targetId==enemy.getObjectId())return;
        targetId=enemy.getObjectId();
        nextAt = time + 3000;
        // Intentionally repeat even when the target is unchanged or AI has fallen idle.
        // Do not retarget the OWNER: it may be in the middle of a self-targeted cast.
        summon.doSummonAttack(enemy);
    }
}
