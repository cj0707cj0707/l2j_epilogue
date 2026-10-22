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
    /** A class role, not its current weapon override: bows/casters do not get melee pursuit. */
    static boolean meleeClass(Player bot)
    {
        BotClassPreset preset=bot==null?null:BotPresets.getPresetByClassId(bot.getPlayerClass().getId());
        if(preset==null||preset.isCaster())return false;
        return switch(preset.getWeaponKind())
        {
            case SWORD,TWO_HAND,POLE,DAGGER,DUAL,FIST,RAPIER,ANCIENT -> true;
            default -> false;
        };
    }
    /** Existing chase safety envelope, not the450 kite disk and not a wall bypass. */
    static double attackRadius(Player bot) { return bot.isInOlympiadMode()?2000:TrainingArena.arenaRadius(); }
    static double retreatRadius(Player bot,org.l2jmobius.gameserver.entity.actor.Creature enemy,double normal)
    { return managed(bot)&&enemy!=null&&enemy.isPlayer()&&managed(enemy.asPlayer())?450:normal; }
    static int[] center(Player bot)
    {return bot.isInOlympiadMode()?BotManager.arenaCenter(bot):TrainingArena.arenaCenter();}
    /** Caller has already validated the active pair/doors or training session. */
    static synchronized boolean update(Player bot,Player enemy,BotClassPreset preset)
    {
        if(!managed(bot)||enemy==null||preset==null)return false;
        CardinalTrance.apply(bot); // scoped20% Trance/Dreaming Spirit, 15% Fear for97/115; all other native enchant/resistance rules stay intact
        if(!managed(enemy)){clearBoundary(bot.getObjectId());return false;}
        int[] c=center(bot);
        if(c==null||!access())return true; // never allow unbounded bot/bot combat on an unsupported core
        try
        {
            // Resolve each participant's OWN role, including when the opponent's driver runs first.
            // The old global460 clamp must not turn the kite ring into a wall for melee attack paths.
            for(Player actor:new Player[]{bot,enemy})
            {
                if(meleeClass(actor)) { clearBoundary(actor.getObjectId()); continue; }
                Player opponent=actor==bot?enemy:bot;
                // Start rooms are about1200 from center. Never bind/snap a ranged
                // entrant into the460 kite disk: let ordinary geodata movement enter it.
                if(Math.hypot((double)actor.getX()-c[0],(double)actor.getY()-c[1])>460)
                { clearBoundary(actor.getObjectId()); continue; }
                bind.invoke(null,actor,opponent,c[0],c[1]);
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
    /** Pick clockwise/counter-clockwise from the enemy's current position.
     * A near tie retains the last side to avoid rounding oscillation. */
    static int awayDirection(int x,int y,int z,int enemyX,int enemyY,int cx,int cy,int step,int preferred)
    {
        Location plus=orbit(x,y,z,enemyX,enemyY,cx,cy,step,1);
        Location minus=orbit(x,y,z,enemyX,enemyY,cx,cy,step,-1);
        double pd=Math.hypot((double)plus.getX()-enemyX,(double)plus.getY()-enemyY);
        double md=Math.hypot((double)minus.getX()-enemyX,(double)minus.getY()-enemyY);
        if(pd>md+0.5)return 1;
        if(md>pd+0.5)return -1;
        return preferred<0?-1:1;
    }
    /** Rounding tolerance only; a kite step must not reduce enemy separation. */
    static boolean awayStep(int x,int y,int enemyX,int enemyY,Location p)
    {
        return Math.hypot((double)p.getX()-enemyX,(double)p.getY()-enemyY)+0.5
            >=Math.hypot((double)x-enemyX,(double)y-enemyY);
    }
    static Location kitePoint(Player bot,Player enemy,int step)
    {
        int[] c=center(bot);if(c==null)return null;
        int id=bot.getObjectId(),preferred=DIRECTIONS.getOrDefault(id,1);
        int chosen=awayDirection(bot.getX(),bot.getY(),bot.getZ(),enemy.getX(),enemy.getY(),c[0],c[1],step,preferred);
        for(int direction:new int[]{chosen,-chosen})
        {
            Location p=orbit(bot.getX(),bot.getY(),bot.getZ(),enemy.getX(),enemy.getY(),c[0],c[1],step,direction);
            if(!awayStep(bot.getX(),bot.getY(),enemy.getX(),enemy.getY(),p))continue;
            if(!GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),p.getX(),p.getY(),p.getZ(),bot.getInstanceId()))continue;
            DIRECTIONS.put(id,direction);return p;
        }
        return null; // at the farthest point or blocked: hold instead of running toward the enemy
    }
}
