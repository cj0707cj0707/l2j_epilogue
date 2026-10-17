package org.l2jmobius.gameserver.mechanics.olympiad;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import sun.misc.Unsafe;


/** Actual patched core entry points; inert actors and clock arguments, not a live server. */
public class OlympiadDiversityTest
{
    static int count;
    static Unsafe unsafe;
    static void verify(boolean ok,String msg){count++;if(!ok)throw new AssertionError(msg);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Map<?,?> state(String name)throws Exception{return (Map<?,?>)field(OlympiadDiversity.class,name).get(null);}
    static void reset()throws Exception{for(String name:List.of("HISTORY","WAITING","BOTS"))state(name).clear();}
    static Player actor(int cid,boolean ranked,boolean human)throws Exception
    {
        Method m=custom.fakeplayersai.ArenaOrbitTranceManaTest.class.getDeclaredMethod("actor",int.class,boolean.class,boolean.class);m.setAccessible(true);return (Player)m.invoke(null,cid,ranked,human);
    }
    static Player player(int cid)throws Exception{return actor(cid,false,false);}
    static List<Player> queue(Player...players)throws Exception
    {var q=Collections.synchronizedList(new ArrayList<>(List.of(players)));field(Olympiad.class,"_nonClassBasedRegisters").set(null,q);return q;}
    static void remember(Player a,Player b,long now)throws Exception
    {var q=queue(a,b);verify(OlympiadDiversity.nextAt(q,now).size()==2,"actual accepted pair recorded");}
    static void historyAndFallback()throws Exception
    {
        reset();Player a=player(88),b=player(94),c=player(95),d=player(131),e=player(114);
        remember(a,b,0);remember(a,c,1);remember(a,d,2);
        var q=queue(a,b,c,d,e);verify(OlympiadDiversity.readyAt(q,3),"unseen opponent and class immediately available");
        var pair=OlympiadDiversity.nextAt(q,3);verify(pair.contains(a)&&pair.contains(e),"avoids last3 IDs and last2 classes with actual alternatives");
        verify(q.size()==3&&!q.contains(a)&&!q.contains(e),"removes exactly selected pair");
        q=queue(a,e);verify(!OlympiadDiversity.readyAt(q,4),"recent pair waits rather than instant rematch");
        verify(!OlympiadDiversity.readyAt(q,20003),"not relaxed at19999ms");verify(OlympiadDiversity.readyAt(q,20004),"relaxed at20000ms even if only two remain");
        verify(OlympiadDiversity.nextAt(q,20004).size()==2&&q.isEmpty(),"no starvation with two participants");
        reset();remember(b,a,0);q=queue(a,b,e);pair=OlympiadDiversity.nextAt(q,1);
        verify(pair.contains(e),"both participants' history is respected");
        reset();Player b2=player(94);remember(a,b,0);q=queue(a,b2,e);pair=OlympiadDiversity.nextAt(q,1);
        verify(pair.contains(e),"same class on different objectID is also avoided");
        reset();q=queue(a,b,c);verify(OlympiadDiversity.readyAt(q,10),"initial queue ready");q.add(e);pair=OlympiadDiversity.nextAt(q,11);
        verify(pair.size()==2&&q.size()==2,"queue growth between ready and selection still yields real pair");
        reset();q=queue(a,b,c);OlympiadDiversity.readyAt(q,0);q.remove(a);verify(OlympiadDiversity.readyAt(q,1),"withdrawal prunes stale waiter");
        verify(!state("WAITING").containsKey(a.getObjectId()),"withdrawn ID forgotten");
        reset();q=queue(a);verify(!OlympiadDiversity.readyAt(q,0),"no single-participant match");q.clear();verify(!OlympiadDiversity.readyAt(q,1),"empty queue held");
    }
    static void wrapperAndScope()throws Exception
    {
        reset();Player a=player(88),b=player(94),c=player(95);OlympiadDiversity.register(a);
        OlympiadManager mgr=(OlympiadManager)unsafe.allocateInstance(OlympiadManager.class);field(OlympiadManager.class,"_lastOpponentIds").set(mgr,new HashMap<>());
        remember(a,b,System.nanoTime()/1000000);var q=queue(a,b);
        verify(!mgr.existNextOpponents(q),"installed readiness wrapper applies actual repeat delay");q.add(c);
        verify(mgr.existNextOpponents(q),"installed readiness accepts a fresh candidate immediately");
        var pair=mgr.nextOpponents(q);verify(pair.size()==2&&pair.contains(c),"installed nextOpponents routes through actual helper");
        reset();q=queue(a,b);verify(mgr.existNextOpponents(q),"unmanaged/human-only native readiness unchanged");
        verify(mgr.nextOpponents(q).size()==2&&state("HISTORY").isEmpty(),"unmanaged/human-only goes through preserved native matcher");
        reset();Player human1=actor(88,false,true),human2=actor(94,false,true);q=queue(human1,human2);
        OlympiadDiversity.register(human1);verify(state("BOTS").isEmpty(),"client-backed human cannot be registered as a bot");
        verify(mgr.existNextOpponents(q)&&mgr.nextOpponents(q).size()==2&&state("HISTORY").isEmpty(),"real client-backed human-only queue retains native matcher");
        reset();OlympiadDiversity.register(a);var nonclass=queue(a,b);var classed=new ArrayList<>(List.of(a,b));
        verify(mgr.existNextOpponents(classed)&&mgr.nextOpponents(classed).size()==2,"classed list uses native implementation");
        verify(nonclass.size()==2&&state("HISTORY").isEmpty(),"classed matching cannot alter non-classed queue/history");
        reset();remember(a,b,0);q=queue(a,b);verify(OlympiadDiversity.readyAt(q,OlympiadDiversity.HISTORY_TTL_MS+1),"expired session history cannot block future periods");
        OlympiadDiversity.register(a);OlympiadDiversity.forget(a.getObjectId());verify(!state("BOTS").containsKey(a.getObjectId())&&!state("HISTORY").containsKey(a.getObjectId()),"despawn clears bot references/history");
    }
    static void rounds()throws Exception
    {
        reset();int[] classes={88,89,90,91,92,94,95,97,99,102,103,106,108,109,110,113,114,131,115};
        var players=new ArrayList<Player>();for(int cid:classes)players.add(player(cid));
        Map<Integer,Deque<Integer>> ids=new HashMap<>(),professions=new HashMap<>();Set<String> combinations=new HashSet<>();long now=0;
        for(int round=0;round<500;round++)
        {
            Player anchor=players.get(round%players.size());var q=queue(anchor);OlympiadDiversity.readyAt(q,now); // oldest anchor, newcomers at +1
            for(Player p:players)if(p!=anchor)q.add(p);
            verify(OlympiadDiversity.readyAt(q,now+1),"full roster always has a fresh alternative");
            var pair=OlympiadDiversity.nextAt(q,now+1);verify(pair.size()==2&&pair.get(0)==anchor,"oldest queued anchor is not starved");
            for(int side=0;side<2;side++)
            {
                Player a=pair.get(side),b=pair.get(1-side);var last=ids.computeIfAbsent(a.getObjectId(),k->new ArrayDeque<>());var cls=professions.computeIfAbsent(a.getObjectId(),k->new ArrayDeque<>());
                verify(!last.contains(b.getObjectId()),"last3 opponents not repeated while alternatives exist");verify(!cls.contains(b.getPlayerClass().getId()),"last2 classes not repeated while alternatives exist");
                last.addFirst(b.getObjectId());while(last.size()>3)last.removeLast();cls.addFirst(b.getPlayerClass().getId());while(cls.size()>2)cls.removeLast();
            }
            combinations.add(Math.min(pair.get(0).getObjectId(),pair.get(1).getObjectId())+":"+Math.max(pair.get(0).getObjectId(),pair.get(1).getObjectId()));now+=100;
        }
        verify(combinations.size()>60,"not fixed pairs: diverse eligible roster combinations");
    }
    static class FakeOlympiad extends Olympiad
    {
        boolean active=true;Set<Integer> registered=new HashSet<>();List<Integer> order=new ArrayList<>();int topups;
        @Override public boolean inCompPeriod(){return active;}
        @Override public boolean playerInStadia(Player p){return false;}
        @Override public boolean isRegisteredInComp(Player p){return registered.contains(p.getObjectId());}
        @Override public int getNoblePoints(int id){return 200;}
        @Override public void setNoblePoints(int id,int points){topups++;}
        @Override public boolean registerNoble(Player p,boolean classed){verify(!classed,"nonclass registration preserved");verify(registered.add(p.getObjectId()),"no double registration");order.add(p.getObjectId());return true;}
    }
    @SuppressWarnings("unchecked") static void registration()throws Exception
    {
        Class<?> type=Class.forName("custom.fakeplayersai.OlympiadRegistration");Method eligible=type.getDeclaredMethod("eligible",int.class,boolean.class,long.class,long.class);eligible.setAccessible(true);
        Map<Integer,Long> due=(Map<Integer,Long>)field(type,"DUE").get(null);due.clear();
        verify(!(boolean)eligible.invoke(null,1,true,0L,5000L),"first eligible appearance schedules pause");
        verify(!(boolean)eligible.invoke(null,1,true,4999L,20000L)&&due.get(1)==5000,"other path cannot reroll or bypass due-time");
        verify((boolean)eligible.invoke(null,1,true,5000L,20000L),"exact due time admitted");
        verify(!(boolean)eligible.invoke(null,1,false,5001L,5000L)&&due.isEmpty(),"busy/queued state resets pause lifecycle");
        verify(!(boolean)eligible.invoke(null,1,true,6000L,20000L)&&due.get(1)==26000,"new idle period receives fresh pause");due.clear();
        Method register=type.getDeclaredMethod("register",Olympiad.class,Collection.class,int.class);register.setAccessible(true);
        FakeOlympiad fake=(FakeOlympiad)unsafe.allocateInstance(FakeOlympiad.class);fake.active=true;fake.registered=new HashSet<>();fake.order=new ArrayList<>();
        var bots=new ArrayList<Player>();for(int cid:new int[]{88,94,95,97,131,115}){Player p=player(cid);field(Player.class,"_noble").setBoolean(p,true);bots.add(p);}
        long before=System.nanoTime()/1000000;verify((int)register.invoke(null,fake,bots,18)==0,"all automatic/manual calls schedule, never immediate");long after=System.nanoTime()/1000000;
        for(Player p:bots)verify(due.get(p.getObjectId())>=before+5000&&due.get(p.getObjectId())<=after+20000,"independent random delay in5..20sec");
        verify((int)register.invoke(null,fake,bots,18)==0,"second timer/admin pass cannot bypass delay");
        due.replaceAll((id,time)->0L);verify((int)register.invoke(null,fake,bots,18)==bots.size(),"due bots registered normally");
        verify((int)register.invoke(null,fake,bots,18)==0&&fake.order.size()==bots.size(),"already queued bots skipped, no double booking");
        fake.registered.clear();field(Player.class,"_olympiadGameId").setInt(bots.get(0),3);register.invoke(null,fake,bots,18);verify(!due.containsKey(bots.get(0).getObjectId()),"assigned countdown game excluded");
        fake.active=false;register.invoke(null,fake,bots,18);verify(due.isEmpty(),"competition closure clears due-times");
        Set<Integer> firsts=new HashSet<>();field(Player.class,"_olympiadGameId").setInt(bots.get(0),-1);
        for(int pass=0;pass<40;pass++){fake.active=true;fake.registered.clear();fake.order.clear();due.clear();register.invoke(null,fake,bots,18);due.replaceAll((id,time)->0L);register.invoke(null,fake,bots,18);firsts.add(fake.order.get(0));}
        verify(firsts.size()>1,"registration order actually shuffled, not stable BOTS iteration");
        due.clear();Player human=actor(88,false,true);field(Player.class,"_noble").setBoolean(human,true);register.invoke(null,fake,List.of(human),18);verify(!due.containsKey(human.getObjectId())&&!fake.registered.contains(human.getObjectId()),"humans not scheduled or topped up by bot path");
    }
    public static void main(String[] args)throws Exception
    {
        unsafe=(Unsafe)field(Unsafe.class,"theUnsafe").get(null);field(custom.fakeplayersai.LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        historyAndFallback();wrapperAndScope();rounds();registration();
        System.out.println("PASS: "+count+" actual installed-core history/last3 IDs/last2 classes/20s fallback/fairness/scope and shared5..20s randomized-registration checks; no live server");System.exit(0);
    }
}
