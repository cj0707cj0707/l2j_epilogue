package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.ai.*;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;

/** Actual private production step against real Player/core/geodata adapter.
 * AI recording seam does not run world movement, packets, database or scheduler. */
public class DaggerRearApproachTest
{
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Class<?> state;
    static Method step;
    static Object fresh()throws Exception{var c=state.getDeclaredConstructor();c.setAccessible(true);return c.newInstance();}
    static long next(Object s)throws Exception{return ArenaOrbitTranceManaTest.f(state,"nextStep").getLong(s);}
    static class RecordingAI extends PlayerAI
    {
        int requests;Location requested;boolean accept;
        RecordingAI(Player p){super(p);}
        @Override public void setIntentionMoveTo(ILocational p)
        {
            requests++;requested=new Location(p.getX(),p.getY(),p.getZ());
            // Only record native movement fields, never start world movement or packets.
            if(accept)try
            {
                Class<?> type=Class.forName("org.l2jmobius.gameserver.entity.actor.Creature$MoveData");
                var c=type.getDeclaredConstructor();c.setAccessible(true);Object move=c.newInstance();
                type.getField("xDestination").setInt(move,p.getX());type.getField("yDestination").setInt(move,p.getY());
                type.getField("zDestination").setInt(move,p.getZ());
                ArenaOrbitTranceManaTest.f(Creature.class,"_move").set(_actor,move);_intention=Intention.MOVE_TO;
            }
            catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static void at(Player p,int x,int y,int z,int heading)throws Exception
    {ArenaOrbitTranceManaTest.f(WorldObject.class,"_location").set(p,new Location(x,y,z,heading,10));}
    static boolean attempt(Player p,Player e,int cid,Object s,long time,double distance,boolean visible,int flags)throws Exception
    {return (boolean)step.invoke(null,p,e,BotPresets.getPresetByClassId(cid),s,time,distance,visible,1.0,(flags&1)!=0,(flags&2)!=0);}
    static void cases()throws Exception
    {
        for(int cid:new int[]{93,101,108})for(boolean ranked:new boolean[]{false,true})for(int heading:new int[]{0,8192,16384,32768,49152,65535})for(int flags=0;flags<4;flags++)
        {
            Player p=ArenaOrbitTranceManaTest.actor(cid,ranked,false),e=ArenaOrbitTranceManaTest.actor(94,ranked,true);
            RecordingAI ai=new RecordingAI(p);ArenaOrbitTranceManaTest.f(Creature.class,"_ai").set(p,ai);
            double angle=heading*2*Math.PI/65536.0;
            at(e,1000,1000,0,heading);at(p,1000+(int)Math.round(80*Math.cos(angle)),1000+(int)Math.round(80*Math.sin(angle)),0,0);
            Object s=fresh();
            check(!attempt(p,e,cid,s,1000,80,true,flags),"inert AI rejects actual movement");
            check(ai.requests==1&&next(s)==3000,"eligible attempt reserved for2000ms even with ready offense or rejected AI");
            double dx=1000-85*Math.cos(angle)-70*Math.sin(angle)-p.getX(),dy=1000-85*Math.sin(angle)+70*Math.cos(angle)-p.getY();
            double length=Math.hypot(dx,dy),scale=Math.min(1,160/length);
            check(ai.requested.getX()==(int)Math.round(p.getX()+dx*scale)&&ai.requested.getY()==(int)Math.round(p.getY()+dy*scale),"restored historical rear/side waypoint and160 cap");
            check(Math.hypot(ai.requested.getX()-p.getX(),ai.requested.getY()-p.getY())<=161,"bounded ordinary movement request");
            for(long time:new long[]{1001,1500,2999}){attempt(p,e,cid,s,time,80,true,flags);check(ai.requests==1&&next(s)==3000,"no request before2seconds");}
            attempt(p,e,cid,s,3000,80,true,flags);check(ai.requests==2&&next(s)==5000,"exact2second boundary");
            Object other=fresh();attempt(p,e,cid,other,1000,80,true,flags);check(ai.requests==3,"independent per-bot state");
            for(double distance:new double[]{170,200,349,500}){s=fresh();attempt(p,e,cid,s,6000,distance,true,flags);check(ai.requests==3&&next(s)==0,"far target belongs to pursuit, no recovery retreat");}
            s=fresh();attempt(p,e,cid,s,6000,80,false,flags);check(ai.requests==3&&next(s)==0,"no line of sight, no movement request");
            at(p,p.getX(),p.getY(),101,0);attempt(p,e,cid,s,6000,80,true,flags);check(ai.requests==3,"vertical separation protected");
            at(p,1000-(int)Math.round(80*Math.cos(angle)),1000-(int)Math.round(80*Math.sin(angle)),0,0);
            attempt(p,e,cid,s,6000,80,true,flags);check(ai.requests==3&&next(s)==0,"already behind: keep melee rather than retreat");
            check(e.getHeading()==heading&&e.getX()==1000&&e.getY()==1000,"no target facing/position mutation");
            at(p,1000+(int)Math.round(80*Math.cos(angle)),1000+(int)Math.round(80*Math.sin(angle)),0,0);
            ai.accept=true;s=fresh();
            check(attempt(p,e,cid,s,7000,80,true,flags),"accepted movement owns tactical tick");
            check(next(s)==9000&&ArenaOrbitTranceManaTest.f(state,"moveUntil").getLong(s)==7200,"successful step retains2s timer and200ms ownership");
            check(ArenaOrbitTranceManaTest.f(state,"moveX").getInt(s)==ai.requested.getX()&&ArenaOrbitTranceManaTest.f(state,"moveY").getInt(s)==ai.requested.getY(),"ownership tracks requested waypoint");
            check(ArenaOrbitTranceManaTest.f(state,"offensiveActionsSinceStep").getInt(s)==0,"offense counter reset on success");
            // Real disarm still selects defensive recovery with its existing600ms timer.
            ArenaOrbitTranceManaTest.f(EffectList.class,"_effectFlags").setInt(p.getEffectList(),EffectFlag.DISARMED.getMask());
            s=fresh();check(attempt(p,e,cid,s,10000,80,true,flags)&&next(s)==10600,"disarm keeps recovery instead of dagger flank timer");
            ArenaOrbitTranceManaTest.f(EffectList.class,"_effectFlags").setInt(p.getEffectList(),0);
        }
    }
    public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        ArenaOrbitTranceManaTest.f(LowManaAutoAttackTest.class,"unsafe").set(null,ArenaOrbitTranceManaTest.f(sun.misc.Unsafe.class,"theUnsafe").get(null));
        state=Class.forName("custom.fakeplayersai.BotTactics$State");
        step=BotTactics.class.getDeclaredMethod("step",Player.class,Player.class,BotClassPreset.class,state,long.class,double.class,boolean.class,double.class,boolean.class,boolean.class);step.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("dagger47-geodata-");
        org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;
        org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();
        java.nio.file.Files.delete(geo);
        cases();System.out.println("PASS: "+checks+" real production dagger rear-approach checks; all3 classes, both arenas,2second timer, heading geometry and recovery exclusion; no live movement");
    }
}
