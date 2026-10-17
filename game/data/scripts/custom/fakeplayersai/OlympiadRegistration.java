package custom.fakeplayersai;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;

/** One serialized registration gate for fast, maintenance and admin passes. */
final class OlympiadRegistration
{
    private static final Logger LOG=Logger.getLogger(OlympiadRegistration.class.getName());
    private static final Map<Integer,Long> DUE=new HashMap<>();
    private static Method bind,forget;
    private static boolean warned,manualRetry;
    private OlympiadRegistration() { }
    private static boolean access()
    {
        if(bind!=null)return true;
        try
        {
            Class<?> c=Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadDiversity");
            bind=c.getMethod("register",Player.class);forget=c.getMethod("forget",int.class);return true;
        }
        catch(ReflectiveOperationException e)
        {if(!warned){warned=true;LOG.severe("Olympiad diversity core patch missing: automatic bot registration held; use supported launcher");}return false;}
    }
    static boolean eligible(int id,boolean available,long now,long delay)
    {
        if(!available){DUE.remove(id);return false;}
        return now>=DUE.computeIfAbsent(id,key->now+delay);
    }
    static synchronized void clear(int id)
    {
        DUE.remove(id);
        if(forget!=null)try{forget.invoke(null,id);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    /** One delayed follow-up also honors an admin request when auto-registration is OFF. */
    static synchronized int request(Olympiad olympiad,Collection<Player> bots,int minPoints)
    {
        int registered=register(olympiad,bots,minPoints);
        if(!DUE.isEmpty()&&!manualRetry)
        {
            manualRetry=true;
            ThreadPool.schedule(()->
            {
                synchronized(OlympiadRegistration.class){manualRetry=false;}
                register(olympiad,bots,minPoints);
            },20001);
        }
        return registered;
    }
    static synchronized int register(Olympiad olympiad,Collection<Player> bots,int minPoints)
    {
        if(olympiad==null||!olympiad.inCompPeriod()) { DUE.clear();return 0; }
        if(!access())return 0;
        var shuffled=new ArrayList<>(bots);Collections.shuffle(shuffled);
        var ids=new HashSet<Integer>();for(Player p:shuffled)ids.add(p.getObjectId());DUE.keySet().retainAll(ids);
        int registered=0;long now=System.nanoTime()/1_000_000;
        for(Player bot:shuffled)
        {
            if(bot.getClient()!=null)continue; // never schedule or top up a human
            try{bind.invoke(null,bot);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            boolean available=bot.isOnline()&&bot.isNoble()&&!bot.isAlikeDead()&&!bot.isTeleporting()
                &&bot.getOlympiadGameId()==-1&&!bot.isInOlympiadMode()&&!olympiad.playerInStadia(bot)&&!olympiad.isRegisteredInComp(bot);
            if(!eligible(bot.getObjectId(),available,now,ThreadLocalRandom.current().nextLong(5000,20001)))continue;
            // Existing bot eligibility top-up, unchanged; not a matchmaking score.
            if(olympiad.getNoblePoints(bot.getObjectId())<minPoints)olympiad.setNoblePoints(bot.getObjectId(),minPoints);
            if(olympiad.registerNoble(bot,false))registered++;
            DUE.remove(bot.getObjectId()); // success waits until next idle period; failure gets a new bounded retry delay
        }
        return registered;
    }
}
