package org.l2jmobius.gameserver.mechanics.skill;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;

/** Registered bot/bot live pairs only; all other objects retain native coordinates. */
public final class ArenaBoundaryGuard
{
    private record Boundary(WeakReference<Player> bot, WeakReference<Player> enemy, int game, int instance, int x, int y) { }
    private static final ConcurrentHashMap<Integer,Boundary> BOUNDS = new ConcurrentHashMap<>();
    private ArenaBoundaryGuard() { }
    public static void bind(Player bot, Player enemy, int x, int y)
    {
        if (!pair(bot,enemy)) { if(bot!=null) clear(bot.getObjectId()); return; }
        BOUNDS.put(bot.getObjectId(),new Boundary(new WeakReference<>(bot),new WeakReference<>(enemy),
            bot.isInOlympiadMode()?bot.getOlympiadGameId():-1,bot.getInstanceId(),x,y));
    }
    public static void clear(int id) { BOUNDS.remove(id); }
    private static boolean pair(Player a, Player b)
    {
        if(a==null||b==null||a==b||a.getClient()!=null||b.getClient()!=null||!a.isOnline()||!b.isOnline()
            ||a.isAlikeDead()||b.isAlikeDead()||a.isTeleporting()||b.isTeleporting()||a.getInstanceId()!=b.getInstanceId())return false;
        if(a.isInOlympiadMode()||b.isInOlympiadMode())return a.isInOlympiadMode()&&b.isInOlympiadMode()
            &&a.isOlympiadStart()&&b.isOlympiadStart()&&a.getOlympiadGameId()>=0&&a.getOlympiadGameId()==b.getOlympiadGameId();
        return a.getInstanceId()>0&&a.isInDuel()&&b.isInDuel()&&a.getDuelId()==b.getDuelId()
            &&a.getDuelState()==org.l2jmobius.gameserver.entity.actor.holders.player.Duel.DUELSTATE_DUELLING
            &&b.getDuelState()==org.l2jmobius.gameserver.entity.actor.holders.player.Duel.DUELSTATE_DUELLING;
    }
    public static org.l2jmobius.gameserver.entity.Location location(WorldObject actor,org.l2jmobius.gameserver.entity.Location loc)
    {
        if(loc.getInstanceId()!=actor.getInstanceId()){clear(actor.getObjectId());return loc;}
        int[] xy=clamp(actor,loc.getX(),loc.getY());
        return xy[0]==loc.getX()&&xy[1]==loc.getY()?loc:new org.l2jmobius.gameserver.entity.Location(xy[0],xy[1],loc.getZ(),loc.getHeading(),loc.getInstanceId());
    }
    public static int[] clamp(WorldObject actor,int x,int y)
    {
        Boundary b=BOUNDS.get(actor.getObjectId());
        if(b==null)return new int[]{x,y};
        Player bot=b.bot.get(),enemy=b.enemy.get();
        if(bot!=actor||!pair(bot,enemy)||bot.getInstanceId()!=b.instance
            ||(bot.isInOlympiadMode()?bot.getOlympiadGameId():-1)!=b.game)
        { BOUNDS.remove(actor.getObjectId(),b);return new int[]{x,y}; }
        double dx=(double)x-b.x,dy=(double)y-b.y,r=Math.hypot(dx,dy);
        if(r<=460)return new int[]{x,y};
        // 459 leaves integer-rounding headroom: actual /loc radius can never exceed460.
        return new int[]{b.x+(int)Math.round(dx*459/r),b.y+(int)Math.round(dy*459/r)};
    }
}
