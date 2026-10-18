package org.l2jmobius.gameserver.mechanics.olympiad;

import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import sun.misc.Unsafe;

/** Native Player observer entry/exit, real flags/access levels/stadium removal.
 * Only teleport, instance transfer, target and network/info facades are recorded;
 * no live world, DB, client or EnterWorld login is run. */
public class ObserverCleanupNativeTest
{
    static Unsafe unsafe;static Class<?> recording;static int checks,sequence;
    static final Map<Player,List<String>> packets=new IdentityHashMap<>();
    static final Map<Player,Integer> transfers=new IdentityHashMap<>();
    static final Map<Player,boolean[]> broadcast=new IdentityHashMap<>();
    static Field f(Class<?> c,String name)throws Exception{Field x=c.getDeclaredField(name);x.setAccessible(true);return x;}
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static boolean flag(Player p)throws Exception{return f(Creature.class,"_isInvul").getBoolean(p);}
    public static void packet(Player p,ServerPacket value){packets.computeIfAbsent(p,k->new ArrayList<>()).add(value.getClass().getSimpleName());}
    public static void info(Player p)
    {try{broadcast.put(p,new boolean[]{p.inObserverMode(),flag(p),p.isInvisible()});}catch(Exception e){throw new RuntimeException(e);}}
    public static void teleport(Player p,ILocational location,boolean random)
    {transfers.merge(p,1,Integer::sum);p.getLocation().setXYZ(location.getX(),location.getY(),location.getZ());}
    public static void teleport(Player p,ILocational location,int instance){teleport(p,location,false);}
    public static void instance(Player p,int id){p.getLocation().setInstanceId(id);}
    static void recordingType()throws Exception
    {
        ClassDesc owner=ClassDesc.of("org.l2jmobius.gameserver.entity.actor.Player"),test=ClassDesc.of(ObserverCleanupNativeTest.class.getName());
        ClassDesc loc=ClassDesc.of(ILocational.class.getName()),packet=ClassDesc.of(ServerPacket.class.getName());
        var bytes=ClassFile.of().build(ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.ObserverRecordingPlayer"),c->c.withSuperclass(owner)
            .withMethodBody("sendPacket",MethodTypeDesc.of(ConstantDescs.CD_void,packet),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(test,"packet",MethodTypeDesc.of(ConstantDescs.CD_void,owner,packet)).return_())
            .withMethodBody("broadcastInfo",MethodTypeDesc.of(ConstantDescs.CD_void),ClassFile.ACC_PUBLIC,b->b.return_())
            .withMethodBody("broadcastUserInfo",MethodTypeDesc.of(ConstantDescs.CD_void),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(test,"info",MethodTypeDesc.of(ConstantDescs.CD_void,owner)).return_())
            .withMethodBody("setTarget",MethodTypeDesc.of(ConstantDescs.CD_void,ClassDesc.of(WorldObject.class.getName())),ClassFile.ACC_PUBLIC,b->b.return_())
            .withMethodBody("setInstanceId",MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_int),ClassFile.ACC_PUBLIC,b->b.aload(0).iload(1).invokestatic(test,"instance",MethodTypeDesc.of(ConstantDescs.CD_void,owner,ConstantDescs.CD_int)).return_())
            .withMethodBody("teleToLocation",MethodTypeDesc.of(ConstantDescs.CD_void,loc,ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).invokestatic(test,"teleport",MethodTypeDesc.of(ConstantDescs.CD_void,owner,loc,ConstantDescs.CD_boolean)).return_())
            .withMethodBody("teleToLocation",MethodTypeDesc.of(ConstantDescs.CD_void,loc,ConstantDescs.CD_int),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).invokestatic(test,"teleport",MethodTypeDesc.of(ConstantDescs.CD_void,owner,loc,ConstantDescs.CD_int)).return_()));
        recording=MethodHandles.lookup().defineClass(bytes);
    }
    static Player player(int level)throws Exception
    {
        Player p=(Player)unsafe.allocateInstance(recording);
        AccessLevel access=(AccessLevel)unsafe.allocateInstance(AccessLevel.class);
        f(AccessLevel.class,"_isGm").setBoolean(access,level>=70);f(AccessLevel.class,"_accessLevel").setInt(access,level);
        f(Player.class,"_accessLevel").set(p,access);f(Player.class,"_olympiadGameId").setInt(p,-1);
        f(Player.class,"_lastLoc").set(p,new Location(0,0,0));f(Player.class,"_cubics").set(p,new HashMap<>());
        f(Player.class,"_cubicOrder").set(p,new java.util.concurrent.ConcurrentLinkedDeque<>());
        f(WorldObject.class,"_objectId").setInt(p,++sequence);f(WorldObject.class,"_location").set(p,new Location(100,200,300,0,0));
        f(Creature.class,"_effectList").set(p,new EffectList(p));
        check(p.isGM()==(level>=70),"native access-level GM identity");return p;
    }
    static void nativeExit(boolean patched)throws Exception
    {
        for(int level:new int[]{0,30,70,100})for(int game=0;game<22;game++)for(int prior=0;prior<4;prior++)
        {
            Player p=player(level);p.setInvul((prior&1)!=0);p.setInvisible((prior&2)!=0);
            Location arena=new Location(-88000+game*10,-252637,-3331);
            p.enterOlympiadObserverMode(arena,game,true);
            check(p.inObserverMode()&&p.getOlympiadGameId()==game,"native observer entry/game id preserved");
            check(p.isInvul()&&p.isInvisible(),"observer remains protected and hidden during viewing");
            OlympiadManager.STADIUMS[game].getSpectators().add(p);
            p.leaveOlympiadObserverMode();
            boolean retained=!patched&&p.isGM();
            check(flag(p)==retained&&p.isInvisible()==retained,"native GM cleanup branch result");
            check(!p.inObserverMode()&&p.getOlympiadGameId()==-1,"native observer exit bookkeeping");
            check(!OlympiadManager.STADIUMS[game].getSpectators().contains(p),"native stadium removes spectator");
            check(p.getInstanceId()==0&&p.getX()==100&&p.getY()==200&&p.getZ()==300,"return location and instance preserved");
            check(transfers.get(p)==2&&packets.get(p).equals(List.of("ExOlympiadMode","ExOlympiadMode")),"entry/return teleports and UI packets preserved");
            boolean[] last=broadcast.get(p);check(!last[0]&&last[1]==retained&&last[2]==retained,"final user info sees cleaned flags");
            Location saved=(Location)f(Player.class,"_lastLoc").get(p);check(saved.getX()==0&&saved.getY()==0&&saved.getZ()==0,"native saved-location reset preserved");
            p.setInvul(true);p.setInvisible(true);p.leaveOlympiadObserverMode();
            check(flag(p)&&p.isInvisible()&&transfers.get(p)==2,"no active observation: manual GM flags not globally disabled");
        }
    }
    static void unchangedProtection(boolean patched)throws Exception
    {
        for(int level:new int[]{0,70,100})
        {
            Player p=player(level);p.enterOlympiadObserverMode(new Location(-88000,-252637,-3331),0,true);
            p.enterOlympiadObserverMode(new Location(-83760,-252637,-3331),1,false);
            p.leaveOlympiadObserverMode();check(p.getX()==100&&p.getY()==200&&transfers.get(p)==3,"switching arena preserves original return location");
            if(!patched&&p.isGM()){p.setInvul(false);p.setInvisible(false);}
            f(Player.class,"_teleportProtectEndTime").setLong(p,System.currentTimeMillis()+60_000);
            check(!flag(p)&&p.isInvul(),"native short teleport protection not erased with observer flag");
            f(Player.class,"_teleportProtectEndTime").setLong(p,0);
            f(EffectList.class,"_effectFlags").setInt(p.getEffectList(),EffectFlag.INVUL.getMask());
            check(!flag(p)&&p.isInvul(),"legitimate skill invulnerability not stripped");
            f(EffectList.class,"_effectFlags").setInt(p.getEffectList(),0);
            check(!p.isInvul(),"no sticky observer invulnerability once independent protection expires");
        }
        Player gm=player(100);check(gm.setHiding(true)&&flag(gm)&&gm.isInvisible(),"old builder-hide source of both flags reproduced natively");
        check(gm.setHiding(false)&&!flag(gm)&&!gm.isInvisible(),"manual GM hide toggle remains functional");
    }
    public static void main(String[] args)throws Exception
    {
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);GeneralConfig.load();
        check(!GeneralConfig.GM_STARTUP_BUILDER_HIDE&&!GeneralConfig.GM_STARTUP_INVULNERABLE&&!GeneralConfig.GM_STARTUP_INVISIBLE,"actual config parser disables all login paths");
        recordingType();boolean patched=Boolean.parseBoolean(args[0]);nativeExit(patched);unchangedProtection(patched);
        System.out.println("PASS: "+checks+" native observer/GM/config checks (patched="+patched+"); no live client/DB/login");
    }
}
