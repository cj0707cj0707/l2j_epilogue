package custom.fakeplayersai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** Actual own358 STUN: reach the rear by ordinary movement BEFORE any strike. */
final class BluffRear
{
    private static final Logger LOG = Logger.getLogger(BluffRear.class.getName());
    private String status = "idle";
    private long blockedAt;
    private boolean moving;
    private int x, y, enemyX, enemyY, heading;
    private long moveAt;
    private long retryAt;
    static BuffInfo stun(Player bot, Player enemy)
    {
        if (bot == null || enemy == null || bot.getClient() != null || !enemy.isStunned()) return null;
        int cid = bot.getPlayerClass().getId();
        if (cid != 93 && cid != 101 && cid != 108) return null;
        for (BuffInfo info : enemy.getEffectList().getEffects())
        {
            if (info == null || info.isRemoved() || !info.isInUse() || info.getEffector() != bot
                || info.getEffected() != enemy || info.getSkill() == null || info.getSkill().getId() != 358
                || info.getEffects() == null || enemy.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType()) != info) continue;
            for (var effect : info.getEffects())
                if (!effect.isInstant() && effect.getEffectType() == EffectType.STUN
                    && (effect.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0) return info;
        }
        return null;
    }
    static boolean targetStunned(Player bot)
    {
        return bot != null && bot.getTarget() != null && bot.getTarget().isPlayer()
            && stun(bot, bot.getTarget().asPlayer()) != null;
    }
    static boolean ready(Player bot,Player enemy)
    {
        return bot.calculateDistance2D(enemy)<=40 && Math.abs(bot.getZ()-enemy.getZ())<=100
            && TacticsPolicy.behind(enemy.getX(),enemy.getY(),enemy.getHeading(),bot.getX(),bot.getY());
    }
    static boolean skillAllowed(Player bot,Player enemy,org.l2jmobius.gameserver.mechanics.skill.Skill skill)
    {
        return stun(bot,enemy)==null || ready(bot,enemy) && (skill.getId()==531 || skill.isPhysical()&&BotTactics.directDamage(skill));
    }
    private boolean owns(Player bot)
    {
        return moving && bot.isMoving() && bot.getAI().getIntention() == Intention.MOVE_TO
            && Math.hypot((double)bot.getXdestination() - x, (double)bot.getYdestination() - y) < 5;
    }
    void stop(Player bot)
    {
        if (!bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && owns(bot)) bot.getAI().setIntentionIdle();
        moving = false; retryAt = 0; blockedAt = 0; status = "idle";
    }
    boolean tick(Player bot, Player enemy, BuffInfo effect, long time, int[] center)
    {
        if (effect == null) { stop(bot); return false; }
        if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true;
        if (ready(bot, enemy))
        { stop(bot); return false; }
        if (bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { stop(bot); status = "actor-movement-or-control-blocked"; return true; }
        // Native facing/location can change after Bluff. Never keep a stale route
        // forever if another intention or movement collision interrupted it.
        int turn=Math.abs(enemy.getHeading()-heading);turn=Math.min(turn,65536-turn);
        if (owns(bot) && Math.hypot(enemy.getX()-enemyX,enemy.getY()-enemyY)<10 && turn<2048 && time-moveAt<750) return true;
        if (owns(bot)) { stop(bot); }
        if (time < retryAt) return true;
        retryAt = time + 150;
        if (center == null || !GeoEngine.getInstance().canSeeTarget(bot, enemy) || Math.abs(bot.getZ() - enemy.getZ()) > 100)
        { status = "missing-center-or-los-or-z"; return true; }
        Location next = nextPoint(bot, enemy, center);
        if (next == null)
        {
            status = "no-valid-attack-route";
            if (blockedAt == 0) blockedAt = time;
            else if (time - blockedAt >= 2000)
            {
                LOG.warning("FakePlayersAI: bluff-rear blocked " + bot.getName() + ": " + status
                    + ", bot=" + bot.getX() + "," + bot.getY() + ", target=" + enemy.getX() + "," + enemy.getY()
                    + ", heading=" + enemy.getHeading() + ", center=" + center[0] + "," + center[1]);
                blockedAt = time;
            }
            return true;
        }
        blockedAt = 0;
        bot.getAI().setIntentionMoveTo(next);
        x=next.getX(); y=next.getY(); moving=true;
        enemyX=enemy.getX(); enemyY=enemy.getY(); heading=enemy.getHeading(); moveAt=time;
        status = owns(bot) ? "moving" : "native-move-queued-or-rejected";
        // PlayerAI may queue MOVE_TO while a native attack animation finishes.
        // Do not overwrite that saved destination with every alternative route.
        return true;
    }

    String describe() { return status; }

    /** Plan the WHOLE arc, not merely a locally legal next waypoint.
     * Exact180deg is preferred, but the native rear sector is wider than that.
     * Use the existing whole-arena chase envelope, NOT the kite circle: an
     * offensive rear approach may legitimately cross450/460. Never walk back and forth
     * between two locally legal points whose common final goal is unreachable. */
    private static Location nextPoint(Player bot, Player enemy, int[] center)
    {
        double facing=enemy.getHeading()*(2*Math.PI/65536.0);
        double bearing=Math.atan2((double)bot.getY()-enemy.getY(),(double)bot.getX()-enemy.getX());
        double rear=facing+Math.PI;
        double shortest=Math.atan2(Math.sin(rear-bearing),Math.cos(rear-bearing));
        int preferred=shortest<0?-1:1;
        int safe=(int)Math.max(50,Math.min(85,bot.calculateDistance2D(enemy)));
        for (int radius : new int[]{safe,50,40,35,30,25,20,15,10,5})
            for (int offset : new int[]{0,15,-15,30,-30,45,-45,55,-55})
            {
                double goalAngle=rear+Math.toRadians(offset);
                int reach=Math.min(35,radius);
                Location goal=point(enemy,reach,goalAngle,bot.getZ());
                if (!inside(bot,goal,center) || !TacticsPolicy.behind(enemy.getX(),enemy.getY(),enemy.getHeading(),goal.getX(),goal.getY())
                    || Math.hypot((double)goal.getX()-enemy.getX(),(double)goal.getY()-enemy.getY())>40) continue;
                for (int direction : new int[]{preferred,-preferred})
                {
                    double arc=direction*(goalAngle-bearing);
                    arc=(arc%(2*Math.PI)+2*Math.PI)%(2*Math.PI);
                    if (arc<1e-6 || 2*Math.PI-arc<1e-6) arc=0;
                    List<Location> route=new ArrayList<>();
                    route.add(point(enemy,radius,bearing,bot.getZ()));
                    int steps=(int)Math.ceil(arc/(Math.PI/3));
                    for (int i=1;i<=steps;i++)
                        route.add(point(enemy,radius,bearing+direction*Math.min(i*Math.PI/3,arc),bot.getZ()));
                    route.add(goal);
                    Location previous=new Location(bot.getX(),bot.getY(),bot.getZ()),first=null;
                    boolean valid=true;
                    for (Location node : route)
                    {
                        if (!inside(bot,node,center)) { valid=false; break; }
                        if (Math.hypot((double)node.getX()-previous.getX(),(double)node.getY()-previous.getY())<1) continue;
                        if (!GeoEngine.getInstance().canMoveToTarget(previous.getX(),previous.getY(),previous.getZ(),node.getX(),node.getY(),node.getZ(),bot.getInstanceId()))
                        { valid=false; break; }
                        if (first==null) first=node;
                        previous=node;
                    }
                    if (!valid || first==null) continue;
                    double dx=(double)first.getX()-bot.getX(),dy=(double)first.getY()-bot.getY(),length=Math.hypot(dx,dy);
                    if (length>160)
                        first=new Location(bot.getX()+(int)Math.round(dx*160/length),bot.getY()+(int)Math.round(dy*160/length),bot.getZ());
                    return first;
                }
            }
        return null;
    }
    private static Location point(Player enemy,int radius,double angle,int z)
    {
        return new Location(enemy.getX()+(int)Math.round(radius*Math.cos(angle)),enemy.getY()+(int)Math.round(radius*Math.sin(angle)),z);
    }
    private static boolean inside(Player bot,Location point,int[] center)
    {
        return Math.hypot((double)point.getX()-center[0],(double)point.getY()-center[1])<=ArenaCombatRules.attackRadius(bot);
    }
}
