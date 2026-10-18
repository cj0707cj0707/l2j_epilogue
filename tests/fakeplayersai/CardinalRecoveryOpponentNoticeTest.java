package custom.fakeplayersai;

import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual recovery SQL/filters with inert JDBC, native Player notice gates with
 * sendMessage recording only. No DB reads/writes, client or live teleport. */
public class CardinalRecoveryOpponentNoticeTest
{
    static int checks,closes;
    static Unsafe unsafe;
    static Class<?> recording;
    static final Map<Player,List<String>> messages=new IdentityHashMap<>();
    static final Map<Integer,String> binds=new HashMap<>();
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void message(Player p,String s){messages.computeIfAbsent(p,k->new ArrayList<>()).add(s);}
    static int count(Player p){return messages.getOrDefault(p,List.of()).size();}
    static void recordingType()throws Exception
    {
        ClassDesc player=ClassDesc.of(Player.class.getName()),test=ClassDesc.of(CardinalRecoveryOpponentNoticeTest.class.getName());
        var bytes=ClassFile.of().build(ClassDesc.of("custom.fakeplayersai.NoticeRecordingPlayer"),c->c.withSuperclass(player)
            .withMethodBody("sendMessage",MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_String),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(test,"message",MethodTypeDesc.of(ConstantDescs.CD_void,player,ConstantDescs.CD_String)).return_()));
        recording=MethodHandles.lookup().defineClass(bytes);
    }
    static Player player(int cid,boolean human,int game)throws Exception
    {
        Player original=actor(cid,true,human),p=(Player)unsafe.allocateInstance(recording);
        for(Class<?> c=Player.class;c!=Object.class;c=c.getSuperclass())for(Field field:c.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(p,field.get(original));}
        f(Player.class,"_olympiadGameId").setInt(p,game);f(Player.class,"_olympiadStart").setBoolean(p,false);
        int[] center=BotManager.arenaCenter(p);if(center!=null)f(WorldObject.class,"_location").set(p,new Location(center[0]-1200,center[1],center[2],0,10));
        f(WorldObject.class,"_name").set(p,cid==97?"Bishop":"HumanOpponent");
        return p;
    }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);}
    static Map<String,Object> row(int id,String name,String account,int cls,int base,int online,long deletion)
    {Map<String,Object> r=new HashMap<>();r.put("charId",id);r.put("char_name",name);r.put("account_name",account);r.put("classid",cls);r.put("base_class",base);r.put("online",online);r.put("deletetime",deletion);return r;}
    static Connection connection(List<Map<String,Object>> rows)
    {
        int[] index={-1};
        ResultSet rs=proxy(ResultSet.class,(p,m,a)->switch(m.getName()){
            case "next" -> ++index[0]<rows.size();
            case "getString" -> rows.get(index[0]).get((String)a[0]);
            case "getInt" -> ((Number)rows.get(index[0]).get((String)a[0])).intValue();
            case "getLong" -> ((Number)rows.get(index[0]).get((String)a[0])).longValue();
            case "close" -> {closes++;yield null;}
            default -> throw new AssertionError("unexpected ResultSet method:"+m);
        });
        PreparedStatement ps=proxy(PreparedStatement.class,(p,m,a)->switch(m.getName()){
            case "setString" -> {binds.put((Integer)a[0],(String)a[1]);yield null;}
            case "executeQuery" -> rs;
            case "close" -> {closes++;yield null;}
            default -> throw new AssertionError("unexpected PreparedStatement method:"+m);
        });
        return proxy(Connection.class,(p,m,a)->switch(m.getName()){
            case "prepareStatement" -> {String sql=(String)a[0];check(sql.startsWith("SELECT ")&&sql.contains("char_name IN (?,?)")&&sql.contains("classid=97")&&sql.contains("online=0")&&sql.contains("deletetime=0")&&sql.contains("LEFT JOIN accounts")&&sql.contains("a.login IS NULL"),"bounded read-only parameterized candidate query");yield ps;}
            case "close" -> {closes++;yield null;}
            default -> throw new AssertionError("unexpected Connection method:"+m);
        });
    }
    static void recovery()throws Exception
    {
        List<Map<String,Object>> rows=new ArrayList<>();
        rows.add(row(1,"Cardinal","fakeai123456",97,97,0,0));rows.add(row(2,"bishop","fakeai999999",97,97,0,0));
        rows.add(row(3,"Cardinal","human123456",97,97,0,0));rows.add(row(4,"Cardinal","fakeai012345",97,97,0,0));
        rows.add(row(5,"Cardinal","fakeai123456",16,97,0,0));rows.add(row(6,"Cardinal","fakeai123456",97,16,0,0));
        rows.add(row(7,"Cardinal","fakeai123456",97,97,1,0));rows.add(row(8,"Cardinal","fakeai123456",97,97,0,999));
        rows.add(row(9,"NotCardinal","fakeai123456",97,97,0,0));rows.add(row(10,"Cardinal",null,97,97,0,0));
        rows.add(row(11,"Cardinal","fakeai123456",97,97,0,0));rows.add(row(0,"Cardinal","fakeai123456",97,97,0,0));
        var realAccount=row(12,"Cardinal","fakeai123456",97,97,0,0);realAccount.put("registered_account","fakeai123456");rows.add(realAccount);
        try(Connection con=connection(rows))
        {
            var found=BotRosterRecovery.readCandidates(con,"fakeai",id->id==11);
            check(found.size()==2&&found.get(0).objectId()==1&&found.get(1).objectId()==2,"only offline generated-bot Cardinal/Bishop candidates survive actual filters");
            check(binds.equals(Map.of(1,"Cardinal",2,"Bishop")),"exact fixed names bound, no prefix wildcard adoption");
        }
        check(closes==3,"connection/result/statement resources closed");
        for(String prefix:new String[]{"fakeai","custom_","prefix.with.regex+"})
        {
            for(int i=100000;i<100120;i++)check(BotRosterRecovery.generatedBotAccount(prefix+i,prefix),"exact generated account pattern");
            for(String suffix:new String[]{"","12345","1234567","012345","12345a","１２３４５６","-12345","123456 "," 123456"})check(!BotRosterRecovery.generatedBotAccount(prefix+suffix,prefix),"reject malformed/foreign account suffix");
        }
        check(!BotRosterRecovery.generatedBotAccount("123456",""),"empty prefix cannot adopt arbitrary accounts");
        check(!BotRosterRecovery.generatedBotAccount(null,"fakeai")&&!BotRosterRecovery.generatedBotAccount("fakeai123456",null),"null account/prefix safely denied");
    }
    static void notices()throws Exception
    {
        for(int game:new int[]{0,5,21})for(int cid:new int[]{97,131,94})for(boolean enemyHuman:new boolean[]{false,true})
        {
            Player p=player(93,true,game),enemy=player(cid,enemyHuman,game);Object identity=new Object();Player[] pair={p,enemy};
            check(OlympiadOpponentNotice.announce(p,identity,pair),"real player receives arrival message before battle start");
            check(count(p)==1&&messages.get(p).get(0).equals("Олимпиада: ваш противник — "+enemy.getName()+"."),"actual sendMessage includes exact matched nickname");
            for(int i=0;i<10;i++)check(OlympiadOpponentNotice.announce(p,identity,pair)&&count(p)==1,"teleport retries/prestart deduplicated by native game identity");
            check(OlympiadOpponentNotice.announce(p,new Object(),pair)&&count(p)==2,"reused stadium/game number still notifies next match");
            check(!OlympiadOpponentNotice.announce(p,new Object(),new Player[]{enemy,enemy}),"malformed/foreign pair never announces");
            Player unrelated=player(94,true,game);check(!OlympiadOpponentNotice.announce(unrelated,new Object(),pair),"spectator/unrelated player cannot read pair notice");
            if(enemyHuman)check(OlympiadOpponentNotice.announce(enemy,identity,pair)&&count(enemy)==1&&messages.get(enemy).get(0).equals("Олимпиада: ваш противник — "+p.getName()+"."),"both humans receive their own matched opponent");
            if(!enemyHuman)check(!OlympiadOpponentNotice.announce(enemy,new Object(),pair)&&count(enemy)==0,"no pointless messages to clientless bots");
        }
        Player p=player(93,true,5),enemy=player(97,false,5);Player[] pair={p,enemy};
        check(!OlympiadOpponentNotice.announce(p,null,pair)&&!OlympiadOpponentNotice.announce(p,new Object(),null),"unpublished native pair waits");
        enemy.setTeleporting(true);check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"wait for opponent teleport completion");enemy.setTeleporting(false);
        p.setTeleporting(true);check(OlympiadOpponentNotice.arrivalCandidate(p),"pending own teleport still eligible for bounded retry");check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"wait for own teleport completion");p.setTeleporting(false);
        f(Player.class,"_observerMode").setBoolean(p,true);check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"observer never receives participant notice");f(Player.class,"_observerMode").setBoolean(p,false);
        f(Player.class,"_olympiadGameId").setInt(enemy,4);check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"mismatched match denied");f(Player.class,"_olympiadGameId").setInt(enemy,5);
        f(WorldObject.class,"_location").set(enemy,new Location(0,0,0,0,10));check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"world/return teleport not mistaken for arena arrival");
        enemy=player(97,false,5);pair[1]=enemy;f(Player.class,"_inOlympiadMode").setBoolean(p,false);check(!OlympiadOpponentNotice.announce(p,new Object(),pair),"finished match denied");
        check(OlympiadOpponentNotice.arrivalCandidate(p),"event before native mode publication still eligible for bounded retry");
        f(Player.class,"_inOlympiadMode").setBoolean(p,true);
        check(!OlympiadOpponentNotice.announce(null,new Object(),pair),"null recipient denied");
    }
    public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        recordingType();recovery();notices();
        System.out.println("PASS: "+checks+" production Cardinal recovery filter/JDBC and native Player opponent-notice checks; no live DB/world/client");
    }
}
