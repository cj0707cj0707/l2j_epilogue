package org.l2jmobius.gameserver.mechanics.olympiad;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.WorldObject;
import sun.misc.Unsafe;

/** Actual installed helper/game/manager/task types. Only actors, Olympiad service,
 * singleton bootstrap and native combat task are headless recording seams. */
public class OrderedOlympiadTest
{
    static int checks,seq;static Unsafe u;static Class<?> playerType;
    public static Olympiad service;public static OlympiadManager manager;
    static final Map<Player,Integer> classes=new IdentityHashMap<>();
    static final Set<Player> offline=new HashSet<>();static final List<String> messages=new ArrayList<>();
    static CountDownLatch started=new CountDownLatch(1),finish=new CountDownLatch(1);
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void message(Player p,String s){messages.add(s);}
    public static boolean online(Player p){return !offline.contains(p);}
    public static PlayerClass profession(Player p){return PlayerClass.getPlayerClass(classes.get(p));}
    public static void observe(int arena,Player owner,boolean enter)
    {check(arena==14&&enter,"observer enters the actual ordered arena through native API seam");getService().spectators++;}
    public static void combat(OlympiadGameTask task)
    {
        task._game._playerOne.setInOlympiadMode(true);task._game._playerTwo.setInOlympiadMode(true);started.countDown();
        try{finish.await(3,TimeUnit.SECONDS);task._game=null;}catch(InterruptedException e){throw new RuntimeException(e);}
    }
    static class Service extends Olympiad
    {
        boolean active=true,admit=true;int registrations,spectators;
        @Override public boolean inCompPeriod(){return active;}
        @Override public boolean playerInStadia(Player p){return p.isInOlympiadMode();}
        @Override public int getNoblePoints(int id){return 200;}
        @Override public boolean registerNoble(Player p,boolean classed){registrations++;getRegisteredNonClassBased().add(p);return admit;}
    }
    static Player actor(int cid)throws Exception
    {
        Player p=(Player)u.allocateInstance(playerType);classes.put(p,cid);
        field(WorldObject.class,"_objectId").setInt(p,++seq);field(WorldObject.class,"_name").set(p,"Test"+seq);
        field(Player.class,"_olympiadGameId").setInt(p,-1);return p;
    }
    static void actors()throws Exception
    {
        var owner=ClassDesc.of(Player.class.getName());var test=ClassDesc.of(OrderedOlympiadTest.class.getName());
        byte[] bytes=ClassFile.of().build(ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OrderedRecordingPlayer"),c->{
            c.withSuperclass(owner);
            for(String name:List.of("isNoble"))c.withMethodBody(name,MethodTypeDesc.of(ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.iconst_1().ireturn());
            for(String name:List.of("isAlikeDead","isTeleporting","isInDuel","inObserverMode"))c.withMethodBody(name,MethodTypeDesc.of(ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.iconst_0().ireturn());
            c.withMethodBody("isOnline",MethodTypeDesc.of(ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(test,"online",MethodTypeDesc.of(ConstantDescs.CD_boolean,owner)).ireturn());
            c.withMethodBody("getPlayerClass",MethodTypeDesc.of(ClassDesc.of(PlayerClass.class.getName())),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(test,"profession",MethodTypeDesc.of(ClassDesc.of(PlayerClass.class.getName()),owner)).areturn());
            c.withMethodBody("sendMessage",MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_String),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(test,"message",MethodTypeDesc.of(ConstantDescs.CD_void,owner,ConstantDescs.CD_String)).return_());
        });playerType=MethodHandles.lookup().defineClass(bytes);
    }
    @SuppressWarnings("unchecked") static Map<Integer,?> orders()throws Exception{return (Map<Integer,?>)field(OrderedOlympiad.class,"ORDERS").get(null);}
    static void reset(Map<Integer,OlympiadGameTask> tasks)throws Exception
    {
        orders().clear();((Set<?>)field(OrderedOlympiad.class,"HELD").get(null)).clear();((Set<?>)field(OrderedOlympiad.class,"GAMES").get(null)).clear();
        tasks.clear();manager.getOlympiadGames().clear();for(var s:OlympiadManager.STADIUMS)s.setStadiaFree();
        getService().active=true;getService().admit=true;Olympiad.getRegisteredNonClassBased().clear();Olympiad.getRegisteredClassBased().clear();offline.clear();
    }
    static Service getService(){return (Service)service;}
    static void tick(long time){synchronized(manager){OrderedOlympiad.tick(manager,time);}}
    public static void main(String[] args)throws Exception
    {
        u=(Unsafe)field(Unsafe.class,"theUnsafe").get(null);actors();
        service=(Service)u.allocateInstance(Service.class);getService().active=true;getService().admit=true;
        manager=(OlympiadManager)u.allocateInstance(OlympiadManager.class);
        field(OlympiadManager.class,"_olympiadInstances").set(manager,new HashMap<>());
        field(Olympiad.class,"_nonClassBasedRegisters").set(null,Collections.synchronizedList(new ArrayList<Player>()));
        field(Olympiad.class,"_classBasedRegisters").set(null,new HashMap<Integer,List<Player>>());
        Map<Integer,OlympiadGameTask> tasks=new HashMap<>();OrderedOlympiad.attach(manager,tasks);
        Player owner=actor(88),a=actor(94),b=actor(95),other=actor(97),same=actor(94);
        check(OrderedOlympiad.request(owner,a,same,p->true,18).contains("разных"),"same profession rejected");
        check(OrderedOlympiad.request(owner,a,a,p->true,18).contains("разных"),"same bot rejected");
        check(OrderedOlympiad.request(owner,a,b,p->false,18).contains("недоступны"),"unsafe observer rejected");
        getService().active=false;check(OrderedOlympiad.request(owner,a,b,p->true,18).contains("период"),"closed period rejected");getService().active=true;
        a.setOlympiadGameId(2);b.setOlympiadGameId(3);
        Olympiad.getRegisteredNonClassBased().addAll(List.of(a,b,other));
        check(OrderedOlympiad.request(owner,a,b,p->true,18).contains("принят"),"busy bots can be reserved without interrupting fights");
        check(a.getOlympiadGameId()==2&&b.getOlympiadGameId()==3,"current games untouched");
        check(OrderedOlympiad.held(a.getObjectId())&&OrderedOlympiad.held(b.getObjectId()),"both reservations atomic");
        check(Olympiad.getRegisteredNonClassBased().equals(List.of(other)),"only ordered bots withdrawn from queue");
        check(OrderedOlympiad.request(other,a,owner,p->true,18).contains("зарезервированы"),"cannot double-book a bot");
        check(OrderedOlympiad.request(owner,other,same,p->true,18).contains("уже"),"one order per observer");
        Class<?> gate=Class.forName("custom.fakeplayersai.OlympiadRegistration");
        Method registration=gate.getDeclaredMethod("register",Olympiad.class,Collection.class,int.class);registration.setAccessible(true);
        check((int)registration.invoke(null,service,List.of(a,b),18)==0&&getService().registrations==0,"actual auto/admin gate cannot register reserved bots");
        tick(100);check(manager.getOlympiadGames().isEmpty(),"both busy: no allocation");a.setOlympiadGameId(-1);tick(200);
        check(manager.getOlympiadGames().isEmpty()&&OrderedOlympiad.held(a.getObjectId()),"early finisher held, not started alone");
        b.setOlympiadGameId(-1);tick(300);
        var game=manager.getOlympiadGame(14);check(game!=null&&a.getOlympiadGameId()==14&&b.getOlympiadGameId()==14,"arena15 assigned only when both free");
        check(!OlympiadManager.STADIUMS[14].isFreeToUse()&&tasks.get(14).isStarted(),"stadium busy and native duplicate start blocked");
        check(getService().registrations==2&&Olympiad.getRegisteredNonClassBased().equals(List.of(other)),"native admission then immediate withdrawal");
        check(OrderedOlympiad.ordered(game),"only new ordered identity skips ordinary countdown");
        var previous=new OlympiadGame(1,CompetitionType.NON_CLASSED,new ArrayList<>(List.of(a,b)));
        check(!OrderedOlympiad.ordered(previous),"existing match of same two reserved bots retains native countdown");a.setOlympiadGameId(14);b.setOlympiadGameId(14);
        Method countdown=OlympiadGameTask.class.getDeclaredMethod("teleportCountdown");countdown.setAccessible(true);
        check((boolean)countdown.invoke(tasks.get(14)),"installed countdown wrapper recognizes ordered game");
        tick(5299);check(started.getCount()==1,"4999ms: no task launch");tick(5300);
        check(started.await(1,TimeUnit.SECONDS),"5000ms: real OrderedTask dispatches native task seam");
        tick(5301);tick(5302);check(getService().spectators==1,"automatic spectator entry once, only after both arena flags");
        finish.countDown();for(int i=0;i<100&&!tasks.get(14).isTerminated();i++)Thread.sleep(5);
        tick(5400);check(!OrderedOlympiad.held(a.getObjectId())&&orders().isEmpty(),"task completion releases both bots");
        check(manager.getOlympiadGame(14)==null&&OlympiadManager.STADIUMS[14].isFreeToUse(),"sparse high arena cleaned by identity");
        a.setInOlympiadMode(false);b.setInOlympiadMode(false);a.setOlympiadGameId(-1);b.setOlympiadGameId(-1);reset(tasks);
        for(var s:OlympiadManager.STADIUMS)s.setStadiaBusy();
        OrderedOlympiad.request(owner,a,b,p->true,18);tick(100);check(manager.getOlympiadGames().isEmpty()&&OrderedOlympiad.held(a.getObjectId()),"all arenas busy: wait without stealing");
        OlympiadManager.STADIUMS[6].setStadiaFree();tick(200);check(manager.getOlympiadGame(6)!=null,"falls back to only free arena");
        getService().active=false;tick(201);check(orders().isEmpty()&&a.getOlympiadGameId()==-1&&b.getOlympiadGameId()==-1,"period close cancels countdown safely");
        reset(tasks);OrderedOlympiad.request(owner,a,b,p->true,18);offline.add(b);tick(100);check(orders().isEmpty()&&!OrderedOlympiad.held(a.getObjectId()),"disconnected bot cancels and releases peer");
        reset(tasks);OrderedOlympiad.request(owner,a,b,p->true,18);tick(Long.MAX_VALUE/2);check(orders().isEmpty(),"pending timeout releases bots");
        reset(tasks);getService().admit=false;OrderedOlympiad.request(owner,a,b,p->true,18);tick(100);check(orders().isEmpty()&&tasks.isEmpty(),"native admission failure leaves no allocated stadium/task");
        reset(tasks);OrderedOlympiad.request(owner,a,b,p->true,18);tick(100);getService().active=false;tick(101);check(orders().isEmpty(),"cancel before launch clears game and task");
        System.out.println("PASS: "+checks+" installed ordered reservation/queue/admission/arena/5000ms/cancellation/task lifecycle checks (offline seams, not live combat)");System.exit(0);
    }
}
