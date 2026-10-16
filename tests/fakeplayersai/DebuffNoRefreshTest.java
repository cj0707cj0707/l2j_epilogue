package org.l2jmobius.gameserver.mechanics.skill;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;

/** Actual patched EffectList/Skill/BuffInfo API. Constructor-free test fixtures only: no DB/server/timer thread. */
public class DebuffNoRefreshTest
{
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Object allocate(Class<?> type)throws Exception
    {Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    static void set(Object o,String name,Object value)throws Exception
    {Class<?> c=o.getClass();while(c!=null){try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,value);return;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
    static class Effect extends AbstractEffect
    {
        int flags;EffectType type;boolean instant;
        Effect(int f,EffectType t){super(null,null,named(),new StatSet());flags=f;type=t;}
        static StatSet named(){StatSet s=new StatSet();s.set("name","GuardTest");return s;}
        public int getEffectFlags(){return flags;}
        public EffectType getEffectType(){return type;}
        public boolean isInstant(){return instant;}
    }
    static class InstantHit extends AbstractEffect
    {
        int hits;InstantHit(){super(null,null,Effect.named(),new StatSet());}
        public boolean isInstant(){return true;}
        public boolean calcSuccess(org.l2jmobius.gameserver.entity.actor.Creature a,org.l2jmobius.gameserver.entity.actor.Creature b,Skill s){return true;}
        public void onStart(org.l2jmobius.gameserver.entity.actor.Creature a,org.l2jmobius.gameserver.entity.actor.Creature b,Skill s){hits++;}
    }
    static class ContinuousStun extends Effect
    {
        ContinuousStun(){super(EffectFlag.STUNNED.getMask(),EffectType.STUN);}
        public boolean canStart(org.l2jmobius.gameserver.entity.actor.Creature a,org.l2jmobius.gameserver.entity.actor.Creature b,Skill s){return true;}
    }
    static class Info extends BuffInfo
    {
        int starts,stops; Object statOwner;
        Info(){super(null,null,null);} // NEVER called: real core constructor needs GameTime singleton
        public void initializeEffects(){if(statOwner==null)starts++;else synchronized(statOwner){starts++;}}
        public void stopAllEffects(SkillFinishType type,boolean broadcast){if(statOwner==null){stops++;setFinishType(type);}else synchronized(statOwner){stops++;setFinishType(type);}}
        public void removeStats(){if(statOwner!=null)synchronized(statOwner){/* faithful Creature stat-owner monitor */}}
    }
    static Skill skill(int id,String slot,boolean debuff,int level)
    {StatSet s=new StatSet();s.set("skill_id",id);s.set("level",level);s.set("operateType","A2");s.set("isDebuff",debuff);s.set("abnormalType",slot);s.set("abnormalLevel",level);return new Skill(s);}
    static Info info(int id,String slot,int flags,EffectType type,int level)throws Exception
    {
        Info b=(Info)allocate(Info.class);set(b,"_skill",skill(id,slot,true,level));set(b,"_effected",allocate(Player.class));
        set(b,"_effects",new ArrayList<>(List.of(new Effect(flags,type))));set(b,"_isInUse",true);set(b,"_finishType",SkillFinishType.NORMAL);
        set(b,"_abnormalTime",5);set(b,"_periodStartTicks",12345);return b;
    }
    static class SuppressedIconTask implements ScheduledFuture<Object>
    {
        public long getDelay(TimeUnit unit){return 1;}public int compareTo(Delayed d){return 0;}
        public boolean cancel(boolean mayInterrupt){return false;}public boolean isCancelled(){return false;}public boolean isDone(){return false;}
        public Object get(){return null;}public Object get(long t,TimeUnit unit){return null;}
    }
    static void lockOrder(boolean removal)throws Exception
    {
        Player owner=(Player)allocate(Player.class);EffectList list=new EffectList(owner);
        // Only asynchronous icon scheduling is suppressed. The REAL actor
        // monitor and REAL patched add/remove wrappers are used by both threads.
        set(list,"_updateEffectIconTask",new SuppressedIconTask());
        Info existing=info(600,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);existing.statOwner=owner;set(existing,"_effected",owner);
        if(removal)list.add(existing);
        CountDownLatch held=new CountDownLatch(1),proceed=new CountDownLatch(1);
        ExecutorService threads=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
        try
        {
            Future<?> actorThread=threads.submit(()->{synchronized(owner){held.countDown();try{if(!proceed.await(5,TimeUnit.SECONDS))throw new AssertionError("release signal");list.add(info(601,"ROOT_MAGICALLY",flag(EffectFlag.ROOTED),EffectType.ROOT,1));}catch(Exception e){throw new RuntimeException(e);}}});
            if(!held.await(5,TimeUnit.SECONDS))throw new AssertionError("actor monitor not held");
            CountDownLatch called=new CountDownLatch(1);
            var worker=new java.util.concurrent.atomic.AtomicReference<Thread>();
            Future<?> effectThread=threads.submit(()->{worker.set(Thread.currentThread());called.countDown();if(removal)list.remove(SkillFinishType.REMOVED,existing);else list.add(existing);});
            if(!called.await(5,TimeUnit.SECONDS))throw new AssertionError("effect path not entered");
            // Actor is held throughout; give the competing effect path time to
            // reach its first monitor. Never wait for its callback (correctly blocked).
            long waitUntil=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(worker.get().getState()!=Thread.State.BLOCKED && System.nanoTime()<waitUntil)Thread.yield();
            if(worker.get().getState()!=Thread.State.BLOCKED)throw new AssertionError("effect path did not reach actor monitor");
            proceed.countDown();
            actorThread.get(3,TimeUnit.SECONDS);effectThread.get(3,TimeUnit.SECONDS);
            check(removal?existing.stops==1:existing.starts==1,"owner-before-list "+(removal?"expiry":"add"));
        }
        catch(TimeoutException e){throw new AssertionError("LOCK_ORDER actor/effect-list inversion",e);}
        finally{proceed.countDown();threads.shutdownNow();}
    }
    static int flag(EffectFlag f){return f.getMask();}
    public static void main(String[] args)throws Exception
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=java.nio.file.Path.of(System.getProperty("test.datapack")).toFile();
        if(args.length>0&&args[0].equals("locks")){lockOrder(false);lockOrder(true);System.out.println("PASS: actor/list lock-order regression");return;}
        if(args.length>0&&args[0].equals("baseline"))
        {
            EffectList list=new EffectList(null);Info old=info(260,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);
            list.add(old);list.add(info(260,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1));
            check(old.stops==1,"baseline must reproduce timer-replacing path");System.out.println("PASS: original core reproduces replacement of Hammer Crush");return;
        }
        check(!Modifier.isSynchronized(EffectList.class.getMethod("add",BuffInfo.class).getModifiers()),"no implicit list-before-owner monitor");
        check(!Modifier.isSynchronized(EffectList.class.getMethod("guardOriginalAdd",BuffInfo.class).getModifiers()),"bridge cannot lock list before owner");
        lockOrder(false); lockOrder(true);
        EffectList list=new EffectList(null);Info old=info(260,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);list.add(old);
        for(int level:new int[]{1,2,1030})for(int id:new int[]{260,485,9999})
        {
            Info next=info(id,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,level);list.add(next);
            check(next.starts==0&&next.stops==0,"rejected info must neither start nor exit");
            check(old.starts==1&&old.stops==0&&list.getDebuffs().peek()==old&&list.getDebuffs().size()==1,"preserve exact old info");
            check(old.getAbnormalTime()==5&&old.getPeriodStartTicks()==12345,"original duration and start ticks unchanged");
        }
        // Actual Skill.applyEffectScope: ALL damage+debuff skills retain their
        // instant payload while the shared continuous admission rejects refresh.
        for(int id:new int[]{260,362,793})
        {
            Info next=info(id,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);set(next,"_effects",new ArrayList<AbstractEffect>());
            InstantHit hit=new InstantHit();next.getSkill().addEffect(EffectScope.GENERAL,hit);next.getSkill().addEffect(EffectScope.GENERAL,new ContinuousStun());
            next.getSkill().applyEffectScope(EffectScope.GENERAL,next,true,true);list.add(next);
            check(hit.hits==1,"instant hit retained for "+id);check(next.starts==0&&old.stops==0,"generic no refresh for "+id);
            check(old.getAbnormalTime()==5&&old.getPeriodStartTicks()==12345,"original timer for "+id);
        }
        list.remove(SkillFinishType.REMOVED,old);Info fresh=info(260,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,2);list.add(fresh);
        check(fresh.starts==1&&list.getDebuffs().peek()==fresh,"available again after actual removal");
        EffectFlag[] flags={EffectFlag.STUNNED,EffectFlag.FEAR,EffectFlag.ROOTED,EffectFlag.SLEEP,EffectFlag.MUTED,EffectFlag.DISARMED,EffectFlag.PARALYZED,EffectFlag.CONFUSED,EffectFlag.PSYCHICAL_ATTACK_MUTED,EffectFlag.BETRAYED};
        for(int a=0;a<flags.length;a++)for(int b=0;b<flags.length;b++)
        {
            Info one=info(1000+a,"NONE",flag(flags[a]),EffectType.NONE,1),two=info(2000+b,"NONE",flag(flags[b]),EffectType.NONE,1);
            check(DebuffCategoryGuard.conflicts(one,two)==(a==b),"control matrix "+a+"/"+b);
        }
        check(DebuffCategoryGuard.conflicts(info(1,"SILENCE",flag(EffectFlag.MUTED),EffectType.MUTE,1),info(2,"SILENCE_PHYSICAL",flag(EffectFlag.PSYCHICAL_MUTED),EffectType.NONE,99)),"magic/physical skill seals share category");
        check(DebuffCategoryGuard.conflicts(info(1,"ROOT_MAGICALLY",flag(EffectFlag.ROOTED),EffectType.ROOT,1),info(2,"ROOT_PHYSICALLY",flag(EffectFlag.ROOTED),EffectType.ROOT,2)),"root variants share category");
        check(DebuffCategoryGuard.conflicts(info(1,"POISON",0,EffectType.DMG_OVER_TIME,1),info(2,"POISON",0,EffectType.DMG_OVER_TIME,99)),"non-control debuff slots");
        check(!DebuffCategoryGuard.conflicts(info(1,"NONE",0,EffectType.NONE,1),info(2,"NONE",0,EffectType.NONE,1)),"NONE is not global category");
        Info positive=info(1,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);set(positive,"_skill",skill(1,"STUN",false,1));
        check(!DebuffCategoryGuard.blocked(list,positive),"positive buffs unchanged");
        Info hidden=info(2,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1);EffectList hiddenList=new EffectList(null);hiddenList.getDebuffs().add(hidden);hidden.setInUse(false);
        check(!DebuffCategoryGuard.blocked(hiddenList,fresh),"inactive hidden effect is not active control");hidden.setInUse(true);hidden.setFinishType(SkillFinishType.REMOVED);
        check(!DebuffCategoryGuard.blocked(hiddenList,fresh),"removed effect is not active control");
        // Actual datapack handlers (not flag doubles) must map to the intended categories.
        String[] handlers={"Stun","Fear","Root","Sleep","Mute","PhysicalMute","Disarm","Paralyze"};
        long[] expected={1,2,4,8,16,16,32,64};
        for(int i=0;i<handlers.length;i++){
            Class<?> c=Class.forName("handlers.skill.effects."+handlers[i]);
            StatSet metadata=new StatSet();metadata.set("name",handlers[i]);
            var constructor=c.getConstructors()[0];AbstractEffect effect=(AbstractEffect)constructor.newInstance(null,null,metadata,new StatSet());
            Info actual=info(8000+i,"NONE",0,EffectType.NONE,1);set(actual,"_effects",new ArrayList<>(List.of(effect)));
            check(DebuffCategoryGuard.controls(actual)==expected[i],"actual handler category "+handlers[i]);
        }
        // Different categories coexist, but neither refreshes its own active instance.
        EffectList together=new EffectList(null);Info stun=info(51,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1),sleep=info(52,"SLEEP",flag(EffectFlag.SLEEP),EffectType.SLEEP,1);
        together.add(stun);together.add(sleep);check(stun.starts==1&&sleep.starts==1&&together.getDebuffs().size()==2,"different categories coexist");
        together.add(info(53,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,100));check(together.getDebuffs().size()==2&&stun.stops==0&&sleep.stops==0,"other category not disturbed");
        // Real patched admission must not replace across DIFFERENT core slots.
        list=new EffectList(null);Info root=info(1,"ROOT_MAGICALLY",flag(EffectFlag.ROOTED),EffectType.ROOT,1);list.add(root);
        Info root2=info(2,"ROOT_PHYSICALLY",flag(EffectFlag.ROOTED),EffectType.ROOT,9);list.add(root2);
        check(root.stops==0&&root2.starts==0,"cross-slot category checked before any core mutation");
        // Combined continuous payload is rejected atomically on ANY conflicting category.
        Info combined=info(3,"NONE",flag(EffectFlag.ROOTED)|flag(EffectFlag.STUNNED),EffectType.NONE,1);list.add(combined);check(combined.starts==0,"no partial restart of combined payload");
        final EffectList concurrent=new EffectList(null);var pool=Executors.newFixedThreadPool(8);List<Future<?>> futures=new ArrayList<>();
        for(int thread=0;thread<8;thread++){final int n=thread;futures.add(pool.submit(()->{try{for(int j=0;j<100;j++)concurrent.add(info(3000+n,"STUN",flag(EffectFlag.STUNNED),EffectType.STUN,1+n));}catch(Exception e){throw new RuntimeException(e);}}));}
        for(var f:futures)f.get(15,TimeUnit.SECONDS);pool.shutdownNow();
        check(concurrent.getDebuffs().size()==1,"800 concurrent attempts admit exactly one control");
        Info winner=(Info)concurrent.getDebuffs().peek();check(winner.starts==1&&winner.stops==0,"concurrent winner untouched");
        System.out.println("PASS: "+checks+" actual patched core/category/timer-identity/concurrency checks (no live actors or DB)");
    }
}
