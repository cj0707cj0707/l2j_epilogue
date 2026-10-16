package handlers.skill.effects;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Real handler, isolated API doubles. No GS/DB; no scheduling or actual geodata. */
public class BoundedFearTest
{
    static int assertions, scenarios;
    static final ArrayList<String> failures = new ArrayList<>();
    static final Skill A = new Skill(100), B = new Skill(200);
    static final Creature CASTER = new Creature(700), OTHER_CASTER = new Creature(701);
    static Fear fear() { return new Fear(null, null, null, null); }
    static void check(boolean condition, String label)
    {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }
    @SuppressWarnings("unchecked")
    static Map<Creature, ?> registry() throws Exception
    {
        Field f = Fear.class.getDeclaredField("WANDERS"); f.setAccessible(true);
        return (Map<Creature, ?>) f.get(null);
    }
    static void run(String name, Runnable test) throws Exception
    {
        registry().clear(); GeoEngine.mode="normal"; GeoEngine.projections=GeoEngine.paths=0;
        scenarios++;
        try { test.run(); System.out.println("PASS: " + name); }
        catch (Throwable ex) { failures.add(name + ": " + ex); System.out.println("FAIL: " + name + ": " + ex); }
    }
    static int size()
    {
        try { return registry().size(); } catch (Exception e) { throw new AssertionError(e); }
    }
    static void expectMove(Fear f, Creature target, Skill skill)
    {
        int n=target.ai.moves;
        for(int i=0;i<10 && target.ai.moves==n;i++) f.onActionTime(CASTER,target,skill);
        check(target.ai.moves>n,"expected bounded waypoint within bounded retries");
    }
    static void anchored(Creature target)
    {
        Fear f=fear(); int x=target.x,y=target.y;
        f.onStart(CASTER,target,A);
        check(target.ai.moves==1,"immediate initial step");
        int emitted=0;
        for(int i=0;emitted<5000 && i<6000;i++)
        {
            int oldX=target.x,oldY=target.y,n=target.ai.moves;
            if(i>0) f.onActionTime(CASTER,target,A);
            check(target.ai.moves <= n+1,"at most one waypoint per callback " + i);
            if(i>0 && target.ai.moves==n)
            {
                // Twelve random candidates may all be rejected near the edge. This
                // is intentional fail-closed behavior, not a flaky test failure.
                check(Math.hypot((double)target.x-x,(double)target.y-y)<=100,"rejected sample did not drift");
                continue;
            }
            emitted++;
            check(Math.hypot((double)target.dx-x,(double)target.dy-y)<=100,"fixed-origin radius " + i);
            check(Math.hypot((double)target.dx-oldX,(double)target.dy-oldY)<=100,"segment cap " + i);
            check(Math.hypot((double)target.dx-oldX,(double)target.dy-oldY)>=10,"nonzero segment " + i);
            target.arrive();
        }
        check(emitted==5000,"5000 emitted random steps exercised");
        check(Math.hypot((double)target.x-x,(double)target.y-y)<=100,"no cumulative drift");
        f.onExit(CASTER,target,A); check(size()==0,"last exit releases registry");
    }
    static void activeSkip(String label, Consumer<Player> alter)
    {
        Player p=new Player(10); Fear f=fear(); f.onStart(CASTER,p,A); p.arrive();
        int n=p.ai.moves; alter.accept(p); f.onActionTime(CASTER,p,A);
        check(p.ai.moves==n,label+" has no movement");
    }
    public static void main(String[] args) throws Exception
    {
        run("5000 player steps: fixed radius and segment bounds",()->anchored(new Player(1)));
        run("5000 summon steps: same global handler",()->anchored(new Summon(2)));
        run("tick setting and flags unchanged",()->{
            Fear f=fear();check(f.getTicks()==5,"five ticks => 3330ms with ratio 666");
            check(f.getEffectFlags()!=0,"FEAR retained");
        });
        run("different sources overlap and share first anchor",()->{
            Player p=new Player(3); Fear f=fear(),g=fear(); int x=p.x,y=p.y;
            f.onStart(CASTER,p,A);p.arrive();g.onStart(OTHER_CASTER,p,B);
            check(size()==1,"single recipient anchor");
            f.onExit(CASTER,p,A);int n=p.ai.moves,idles=p.ai.idles;
            f.onActionTime(CASTER,p,A);check(p.ai.moves==n,"removed distinct source cannot tick");
            check(p.ai.idles==idles,"removed source cannot idle overlap");
            for(int i=0;i<100;i++) { g.onActionTime(OTHER_CASTER,p,B);check(Math.hypot((double)p.dx-x,(double)p.dy-y)<=100,"overlap retains initial origin");p.arrive(); }
            g.onExit(OTHER_CASTER,p,B);check(size()==0,"final overlapping source removed");
        });
        run("same-source counted overlap balanced exits",()->{
            Player p=new Player(4);Fear f=fear();f.onStart(CASTER,p,A);f.onStart(CASTER,p,A);
            f.onExit(CASTER,p,A);check(size()==1,"second registration retained");expectMove(f,p,A);
            f.onExit(CASTER,p,A);check(size()==0,"both registrations removed");
        });
        run("same-source skipped application exit preserves active registration",()->{
            Player p=new Player(40);Fear f=fear();f.onStart(CASTER,p,A);
            p.teleport=true;f.onStart(CASTER,p,A); // Callback ran, but no AI action is allowed.
            p.teleport=false;f.onExit(CASTER,p,A); // Remove the skipped overlapping application.
            check(size()==1,"skipped overlapping start must be balanced before its exit");expectMove(f,p,A);
            f.onExit(CASTER,p,A);check(size()==0,"original registration eventually exits");
        });
        run("cure and stale tick do not recreate state",()->{
            Player p=new Player(5);Fear f=fear();f.onStart(CASTER,p,A);int n=p.ai.moves;
            f.onExit(CASTER,p,A);int stops=p.stopMoves;
            for(int i=0;i<25;i++)f.onActionTime(CASTER,p,A);
            check(size()==0&&p.ai.moves==n&&p.stopMoves==stops,"late ticks inert after cure");
        });
        run("stale distinct source exit cannot stop newer Fear",()->{
            Player p=new Player(6);Fear f=fear(),g=fear();f.onStart(CASTER,p,A);f.onExit(CASTER,p,A);
            g.onStart(OTHER_CASTER,p,B);int idles=p.ai.idles;
            f.onExit(CASTER,p,A);check(size()==1&&p.ai.idles==idles,"stale unrelated exit harmless");
            int n=p.ai.moves;g.onActionTime(OTHER_CASTER,p,B);check(p.ai.moves>n,"new source remains movable");
        });
        run("exit preserves unrelated movement",()->{
            Player p=new Player(7);Fear f=fear();f.onStart(CASTER,p,A);
            p.dx+=1000;int stops=p.stopMoves,idles=p.ai.idles;
            f.onExit(CASTER,p,A);check(p.stopMoves==stops&&p.ai.idles==idles,"not our waypoint: no cancel/idle/stop");
        });
        run("root tick skips",()->activeSkip("root",p->p.root=true));
        run("sleep tick skips",()->activeSkip("sleep",p->p.sleep=true));
        run("stun tick skips",()->activeSkip("stun",p->p.stun=true));
        run("paralysis tick skips",()->activeSkip("paralysis",p->p.paralyzed=true));
        run("teleport tick skips",()->activeSkip("teleport",p->p.teleport=true));
        run("instance change tick skips",()->activeSkip("instance",p->p.instance++));
        run("death tick skips",()->activeSkip("death",p->p.dead=true));
        run("despawn tick skips",()->activeSkip("despawn",p->p.spawned=false));
        run("off-AI tick does not create AI",()->{
            Player p=new Player(8);Fear f=fear();f.onStart(CASTER,p,A);p.aiPresent=false;
            int n=p.ai.moves;f.onActionTime(CASTER,p,A);check(p.ai.moves==n&&p.createdAI==0,"no off-AI creation");
        });
        run("external displacement does not warp or reanchor",()->{
            Player p=new Player(9);Fear f=fear();f.onStart(CASTER,p,A);p.x+=1000;int n=p.ai.moves;
            f.onActionTime(CASTER,p,A);check(p.x==111000&&p.ai.moves==n,"outside disk remains untouched");
            f.onActionTime(CASTER,p,A);check(p.ai.moves==n,"no tick reanchor");
        });
        run("root at start makes no waypoint",()->{
            Player p=new Player(11);p.root=true;fear().onStart(CASTER,p,A);check(p.ai.moves==0,"root start skips");
        });
        run("off-AI at start never initializes AI",()->{
            Player p=new Player(12);p.aiPresent=false;fear().onStart(CASTER,p,A);
            check(p.createdAI==0&&p.ai.moves==0,"onStart must not manufacture AI before guards");
        });
        for(String invalid:new String[]{"noAI","teleport","dead","despawn"})
        {
            run("invalid start balances exit without actor actions: "+invalid,()->{
                Player p=new Player(50);Fear f=fear();
                if(invalid.equals("noAI"))p.aiPresent=false;
                if(invalid.equals("teleport"))p.teleport=true;
                if(invalid.equals("dead"))p.dead=true;
                if(invalid.equals("despawn"))p.spawned=false;
                f.onStart(CASTER,p,A);
                check(p.createdAI==0&&p.getAICalls==0&&p.ai.moves==0,"invalid start does not touch or create AI");
                check(p.abortAttacks==0&&p.abortCasts==0&&p.stopMoves==0,"invalid start does not act on actor");
                check(size()==1,"invalid start still balances registered callback");
                f.onExit(CASTER,p,A);check(size()==0,"invalid start's exit releases registration");
            });
        }
        run("abortable cast start cancels then moves normally",()->{
            Player p=new Player(51);p.casting=true;p.canAbort=true;p.ai.intention=Intention.CAST;
            fear().onStart(CASTER,p,A);
            check(!p.casting&&p.abortCasts>0&&p.ai.moves==1,"explicitly abortable cast can yield to Fear");
        });
        run("uninterruptible cast start preserves cast target",()->{
            Player p=new Player(13);p.casting=true;p.canAbort=false;p.ai.intention=Intention.CAST;
            Object target=p.castTarget;fear().onStart(CASTER,p,A);
            check(p.casting&&p.abortCasts==0&&p.castTarget==target&&p.ai.intention==Intention.CAST,"do not idle live uninterruptible cast");
        });
        run("cast begins while owning move: tick preserves cast target",()->{
            Player p=new Player(14);Fear f=fear();f.onStart(CASTER,p,A);p.casting=true;p.canAbort=false;
            Object target=new Object();p.castTarget=target;int n=p.ai.moves,aborts=p.abortCasts;f.onActionTime(CASTER,p,A);
            check(p.ai.moves==n&&p.casting&&p.abortCasts==aborts&&p.castTarget==target,"casting guard must not call destructive idle via stopOwned");
        });
        run("last exit while casting preserves cast target",()->{
            Player p=new Player(15);Fear f=fear();f.onStart(CASTER,p,A);p.casting=true;p.canAbort=false;
            Object target=new Object();p.castTarget=target;int aborts=p.abortCasts;f.onExit(CASTER,p,A);
            check(p.casting&&p.abortCasts==aborts&&p.castTarget==target,"teardown must not idle newer cast");check(size()==0,"cast exit clears registry");
        });
        for(String mode:new String[]{"blocked","outside","farZ","null","zero"})
        {
            run("geodata fail-closed: "+mode,()->{
                Player p=new Player(20);Fear f=fear();GeoEngine.mode=mode;f.onStart(CASTER,p,A);
                for(int i=0;i<5;i++)f.onActionTime(CASTER,p,A);
                check(p.ai.moves==0,"no fallback route when "+mode);
            });
        }
        run("unsafe projection cancels prior owned route",()->{
            Player p=new Player(21);Fear f=fear();f.onStart(CASTER,p,A);GeoEngine.mode="outside";
            int n=p.ai.moves,stops=p.stopMoves;f.onActionTime(CASTER,p,A);
            check(p.ai.moves==n&&!p.ai.moving&&p.stopMoves>stops,"unsafe geo stops owned route only");
        });
        run("NPC legacy call path unchanged",()->{
            Creature npc=new Creature(30);npc.attackable=true;Fear f=fear();check(f.canStart(CASTER,npc,A),"ordinary NPC eligible");
            f.onStart(CASTER,npc,A);f.onActionTime(CASTER,npc,A);f.onExit(CASTER,npc,A);
            check(npc.ai.afraidStart==1&&npc.ai.afraidTick==1&&npc.ai.moves==0,"legacy afraid initial=true then false");
            check(size()==0,"NPC never registers bounded state");check(!f.canStart(CASTER,new Defender(),A),"defender exclusion unchanged");
        });
        run("pet retains ordinary walk/run policy",()->{
            Summon s=new Summon(31);s.pet=true;fear().onStart(CASTER,s,A);check(s.ai.moves==1&&s.runCalls==0,"pet not forced to run");
        });
        System.out.println("RESULT: "+scenarios+" scenarios, "+assertions+" assertions, "+failures.size()+" failures (isolated doubles, not live GS)");
        for(String failure:failures)System.err.println(failure);
        if(!failures.isEmpty())System.exit(1);
    }
}
