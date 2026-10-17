package org.l2jmobius.gameserver.mechanics.olympiad;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.l2jmobius.gameserver.entity.actor.Player;

/** Non-classed queue only. No combat, points, skills, teleports or database writes. */
public final class OlympiadDiversity
{
    static final long WAIT_MS=20_000, HISTORY_TTL_MS=7_200_000;
    private static final Map<Integer,WeakReference<Player>> BOTS=new ConcurrentHashMap<>();
    private static final Map<Integer,Long> WAITING=new HashMap<>();
    private static final LinkedHashMap<Integer,History> HISTORY=new LinkedHashMap<>(128,.75f,true);
    private static final class History
    {
        final Deque<Integer> ids=new ArrayDeque<>(), classes=new ArrayDeque<>();
        long touched;
    }
    private OlympiadDiversity() { }
    public static void register(Player bot)
    { if(bot!=null&&bot.getClient()==null)BOTS.put(bot.getObjectId(),new WeakReference<>(bot)); }
    public static synchronized void forget(int id)
    { BOTS.remove(id);WAITING.remove(id);HISTORY.remove(id); }
    private static boolean bot(Player p)
    {
        if(p==null)return false;
        WeakReference<Player> ref=BOTS.get(p.getObjectId());
        return ref!=null&&ref.get()==p&&p.getClient()==null;
    }
    private static boolean applies(List<Player> queue)
    {
        // The original native classed/human-only matcher remains the authority.
        if(queue==null||queue!=Olympiad.getRegisteredNonClassBased())return false;
        synchronized(queue){return queue.stream().anyMatch(OlympiadDiversity::bot);}
    }
    private static int penalty(Player a,Player b)
    {
        History h=HISTORY.get(a.getObjectId());if(h==null)return 0;
        return (h.ids.contains(b.getObjectId())?10:0)+(h.classes.contains(b.getPlayerClass().getId())?3:0);
    }
    private static List<Player> candidates(List<Player> queue,long now)
    {
        var unique=new LinkedHashMap<Integer,Player>();
        for(Player p:queue)if(p!=null)unique.putIfAbsent(p.getObjectId(),p);
        WAITING.keySet().retainAll(unique.keySet());
        for(int id:unique.keySet())WAITING.putIfAbsent(id,now);
        HISTORY.entrySet().removeIf(e->now-e.getValue().touched>HISTORY_TTL_MS);
        BOTS.entrySet().removeIf(e->e.getValue().get()==null);
        var result=new ArrayList<>(unique.values());
        // Stable same-tick anchor in ready/next; opponent ties are randomized.
        result.sort(Comparator.<Player>comparingLong(p->WAITING.get(p.getObjectId())).thenComparingInt(Player::getObjectId));
        return result;
    }
    private static Player partner(Player first,List<Player> candidates)
    {
        Player selected=null;int best=Integer.MAX_VALUE,ties=0;
        for(Player p:candidates)if(p!=first)
        {
            int score=penalty(first,p)+penalty(p,first);
            if(score<best){selected=p;best=score;ties=1;}
            else if(score==best&&ThreadLocalRandom.current().nextInt(++ties)==0)selected=p;
        }
        return selected;
    }
    public static synchronized boolean ready(List<Player> queue)
    { if(!applies(queue))return true; synchronized(queue){return readyAt(queue,System.nanoTime()/1_000_000);} }
    static boolean readyAt(List<Player> queue,long now)
    {
        var choices=candidates(queue,now);if(choices.size()<2)return false;
        Player a=choices.get(0),b=partner(a,choices);
        return penalty(a,b)+penalty(b,a)==0||now-WAITING.get(a.getObjectId())>=WAIT_MS;
    }
    private static void remember(Player a,Player b,long now)
    {
        History h=HISTORY.computeIfAbsent(a.getObjectId(),id->new History());
        h.ids.addFirst(b.getObjectId());while(h.ids.size()>3)h.ids.removeLast();
        h.classes.addFirst(b.getPlayerClass().getId());while(h.classes.size()>2)h.classes.removeLast();h.touched=now;
        while(HISTORY.size()>8192)HISTORY.remove(HISTORY.keySet().iterator().next());
    }
    public static synchronized List<Player> next(OlympiadManager manager,List<Player> queue)
    {
        if(!applies(queue))return manager.diversityOriginalNextOpponents(queue);
        synchronized(queue){return nextAt(queue,System.nanoTime()/1_000_000);}
    }
    static List<Player> nextAt(List<Player> queue,long now)
    {
        // ready() is the wait gate. Never return an empty pair just because
        // preferences changed between the manager's check and its constructor.
        var choices=candidates(queue,now);if(choices.size()<2)return new ArrayList<>();
        Player a=choices.get(0),b=partner(a,choices);
        queue.remove(a);queue.remove(b);WAITING.remove(a.getObjectId());WAITING.remove(b.getObjectId());
        remember(a,b,now);remember(b,a,now);
        return new ArrayList<>(List.of(a,b));
    }
}
