package custom.fakeplayersai;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Shared live-arena rules. Reflection keeps scripts source-compatible with the
 * unpatched JAR used by older offline tests; launchers install the core guard first. */
public final class ArenaCombatRules
{
    private static final Logger LOG=Logger.getLogger(ArenaCombatRules.class.getName());
    private static final java.util.concurrent.ConcurrentHashMap<Integer,Integer> DIRECTIONS=new java.util.concurrent.ConcurrentHashMap<>();
    private static Method bind,clear;
    private static boolean warned;
    private ArenaCombatRules() { }
    static boolean managed(Player p)
    {return p!=null&&p.getClient()==null&&(BotManager.isBot(p)||TrainingArena.isTrainingBot(p));}
    private static boolean access()
    {
        if(bind!=null)return true;
        try
        {
            Class<?> c=Class.forName("org.l2jmobius.gameserver.mechanics.skill.ArenaBoundaryGuard");
            bind=c.getMethod("bind",Player.class,Player.class,int.class,int.class);clear=c.getMethod("clear",int.class);return true;
        }
        catch(ReflectiveOperationException e)
        {if(!warned){warned=true;LOG.severe("Arena boundary core patch missing: bot/bot combat is held closed; use supported server launcher");}return false;}
    }
    static void clear(int id)
    {
        DIRECTIONS.remove(id);
        clearBoundary(id);
    }
    private static void clearBoundary(int id)
    {
        if(clear!=null)try{clear.invoke(null,id);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static double retreatRadius(Player bot,org.l2jmobius.gameserver.entity.actor.Creature enemy,double normal)
    { return managed(bot)&&enemy!=null&&enemy.isPlayer()&&managed(enemy.asPlayer())?450:normal; }
    static int[] center(Player bot)
    {return bot.isInOlympiadMode()?BotManager.arenaCenter(bot):TrainingArena.arenaCenter();}
    /** Caller has already validated the active pair/doors or training session. */
    static synchronized boolean update(Player bot,Player enemy,BotClassPreset preset)
    {
        if(!managed(bot)||enemy==null||preset==null)return false;
        CardinalTrance.apply(bot); // scoped25% Trance / 15% Dominator controls for97/115; all other native enchant/resistance rules stay intact
        if(!managed(enemy)){clearBoundary(bot.getObjectId());return false;}
        int[] c=center(bot);
        if(c==null||!access())return true; // never allow unbounded bot/bot combat on an unsupported core
        try
        {
            // Admit both bots together, before either class-specific driver runs.
            bind.invoke(null,bot,enemy,c[0],c[1]);bind.invoke(null,enemy,bot,c[0],c[1]);
            // Initial arena spawn points can be outside460. Normalize on admission;
            // subsequent core position commits/charge landings are clamped as well.
            for(Player actor:new Player[]{bot,enemy})
            {
                if(Math.hypot((double)actor.getX()-c[0],(double)actor.getY()-c[1])<=460)continue;
                actor.stopMove(null);
                actor.setXYZ(actor.getX(),actor.getY(),actor.getZ());
                actor.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.ValidateLocation(actor));
            }
            return false;
        }
        catch(ReflectiveOperationException e){throw new IllegalStateException("Arena boundary binding failed",e);}
    }
    /** Compatibility hooks: no artificial HP penalty or MP refill on exhaustion. */
    static boolean exhausted(Player bot,BotClassPreset preset) { return false; }
    static void drainMage(Player bot,BotClassPreset preset) { }
    /** Short chord of the450 ring; transition from inside/outside is bounded too. */
    static Location orbit(int x,int y,int z,int enemyX,int enemyY,int cx,int cy,int step,int direction)
    {
        double dx=(double)x-cx,dy=(double)y-cy,r=Math.hypot(dx,dy);
        double angle=r<1?Math.atan2((double)cy-enemyY,(double)cx-enemyX):Math.atan2(dy,dx);
        double delta=Math.min(.35,Math.max(1,step)/450.0);
        double tx=cx+450*Math.cos(angle+direction*delta),ty=cy+450*Math.sin(angle+direction*delta);
        double length=Math.hypot(tx-x,ty-y),limit=Math.max(1,step);
        if(length>limit){tx=x+(tx-x)*limit/length;ty=y+(ty-y)*limit/length;}
        return new Location((int)Math.round(tx),(int)Math.round(ty),z);
    }
    static Location kitePoint(Player bot,Player enemy,int step)
    {
        int[] c=center(bot);if(c==null)return null;
        int id=bot.getObjectId();
        Integer chosen=DIRECTIONS.get(id);
        if(chosen==null)
        {
            Location plus=orbit(bot.getX(),bot.getY(),bot.getZ(),enemy.getX(),enemy.getY(),c[0],c[1],step,1);
            Location minus=orbit(bot.getX(),bot.getY(),bot.getZ(),enemy.getX(),enemy.getY(),c[0],c[1],step,-1);
            chosen=Math.hypot((double)plus.getX()-enemy.getX(),(double)plus.getY()-enemy.getY())
                >=Math.hypot((double)minus.getX()-enemy.getX(),(double)minus.getY()-enemy.getY())?1:-1;
            DIRECTIONS.put(id,chosen);
        }
        for(int direction:new int[]{chosen,-chosen})
        {
            Location p=orbit(bot.getX(),bot.getY(),bot.getZ(),enemy.getX(),enemy.getY(),c[0],c[1],step,direction);
            if(!GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),p.getX(),p.getY(),p.getZ(),bot.getInstanceId()))continue;
            DIRECTIONS.put(id,direction);return p; // retain angular direction; don't oscillate at the far side
        }
        return null;
    }
}
