package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Player;

/** Manager-monitor owned reservations. Uses native games, results and spectator entry. */
public final class OrderedOlympiad
{
    private static final Logger LOG=Logger.getLogger(OrderedOlympiad.class.getName());
    private static final Set<OlympiadGame> GAMES=ConcurrentHashMap.newKeySet();
    private static final Set<Integer> HELD=ConcurrentHashMap.newKeySet();
    private static final Map<Integer,Order> ORDERS=new LinkedHashMap<>();
    private static Map<Integer,OlympiadGameTask> tasks;
    private static final class Order
    {
        final Player owner,a,b; final Predicate<Player> allowed; final long expires; final int minPoints;
        OlympiadGame game; OrderedTask task; int arena=-1; long due; boolean started,observed;
        Order(Player p,Player x,Player y,Predicate<Player> gate,int points,long now)
        {owner=p;a=x;b=y;allowed=gate;minPoints=points;expires=now+600_000;}
    }
    /** Native manager must not start this task a second time during the five-second reservation. */
    private static final class OrderedTask extends OlympiadGameTask
    {
        private volatile boolean completed;
        OrderedTask(OlympiadGame game){super(game);}
        @Override public boolean isTerminated(){return completed;}
        @Override public void run()
        {
            try
            {
                super.run();
                if(_game==null)completed=true;
                else LOG.severe("Ordered Olympiad native task returned without cleanup; reservation retained for safe shutdown");
            }
            catch(Throwable e){LOG.log(java.util.logging.Level.SEVERE,"Ordered Olympiad native task failed; participants held for safe shutdown",e);}
        }
        @Override public boolean isStarted(){return true;}
    }
    private OrderedOlympiad() { }
    public static boolean held(int id){return HELD.contains(id);}
    public static boolean ordered(OlympiadGame game)
    {return game!=null&&GAMES.contains(game);}
    static void attach(OlympiadManager manager,Map<Integer,OlympiadGameTask> nativeTasks)
    {
        tasks=nativeTasks;
    }
    /** Called only at manager wait points; both this and native allocation hold the same monitor. */
    static void pulse(OlympiadManager manager)
    {
        tick(manager,System.nanoTime()/1_000_000);
    }
    static void await(OlympiadManager manager,long millis) throws InterruptedException
    {
        long deadline=System.nanoTime()+millis*1_000_000L;
        do
        {
            pulse(manager);
            long left=(deadline-System.nanoTime())/1_000_000L;
            // Native period cleanup must not clear an ordered game still running.
            if(left<=0 && (Olympiad.getInstance().inCompPeriod()||ORDERS.values().stream().noneMatch(o->o.started)))return;
            if(left<=0)left=250;
            manager.wait(Math.min(left,250));
        }while(true);
    }
    public static String request(Player owner,Player a,Player b,Predicate<Player> allowed,int minPoints)
    {
        OlympiadManager manager=OlympiadManager.getInstance();
        synchronized(manager)
        {
            if(tasks==null)return "Менеджер Олимпиады ещё не готов. Повторите запрос.";
            if(!Olympiad.getInstance().inCompPeriod())return "Сейчас не олимпийский период.";
            if(a==null||b==null||a==b||a.getPlayerClass()==b.getPlayerClass())return "Выберите два разных класса из текущего пула.";
            if(!valid(a)||!valid(b)||!allowed.test(owner))return "Участники или наблюдатель сейчас недоступны.";
            if(ORDERS.containsKey(owner.getObjectId()))return "У вас уже есть заказанный бой.";
            if(ORDERS.size()>=8||held(a.getObjectId())||held(b.getObjectId()))return "Выбранные боты уже зарезервированы. Повторите позже.";
            HELD.add(a.getObjectId());HELD.add(b.getObjectId());
            // Assignment and cancellation are serialized with native matching, never cancel an assigned match.
            try
            {
                withdraw(a);withdraw(b);
                ORDERS.put(owner.getObjectId(),new Order(owner,a,b,allowed,minPoints,System.nanoTime()/1_000_000));
            }
            catch(RuntimeException e)
            {
                HELD.remove(a.getObjectId());HELD.remove(b.getObjectId());
                LOG.log(java.util.logging.Level.WARNING,"Could not reserve ordered pair",e);
                return "Не удалось зарезервировать пару. Повторите запрос.";
            }
            manager.notifyAll();
            return "Заказ принят. Ждём окончания текущих боёв обоих ботов; затем перенос через 5 секунд.";
        }
    }
    private static boolean valid(Player p)
    {return p!=null&&p.getClient()==null&&p.isOnline()&&p.isNoble();}
    private static void withdraw(Player p)
    {
        Olympiad.getRegisteredNonClassBased().remove(p);
        for(List<Player> list:Olympiad.getRegisteredClassBased().values())list.remove(p);
    }
    static boolean idle(Player p)
    {
        return valid(p)&&!p.isAlikeDead()&&!p.isTeleporting()&&p.getOlympiadGameId()==-1
            &&!p.isInOlympiadMode()&&!p.isInDuel()&&!p.inObserverMode()
            &&!Olympiad.getInstance().playerInStadia(p);
    }
    private static boolean admit(Player p,int minPoints)
    {
        Olympiad olympiad=Olympiad.getInstance();
        if(olympiad.getNoblePoints(p.getObjectId())<minPoints)olympiad.setNoblePoints(p.getObjectId(),minPoints);
        try{return olympiad.registerNoble(p,false);}
        finally{withdraw(p);} // temporary native admission, never expose reserved bots to matching
    }
    private static int freeArena(OlympiadManager manager)
    {
        // Prefer arena 15 (zero-based 14); never take an occupied arena.
        for(int n=0;n<OlympiadManager.STADIUMS.length;n++)
        {
            int i=(14+n)%OlympiadManager.STADIUMS.length;
            if(OlympiadManager.STADIUMS[i].isFreeToUse()&&!manager.getOlympiadGames().containsKey(i)
                &&(!tasks.containsKey(i)||tasks.get(i).isTerminated()))return i;
        }
        return -1;
    }
    static void tick(OlympiadManager manager,long now)
    {
        for(Iterator<Order> it=ORDERS.values().iterator();it.hasNext();)
        {
            Order o=it.next();
            try
            {
                if(o.started)
                {
                    if(o.task.isTerminated()) {release(manager,o);it.remove();continue;}
                    if(!o.observed&&o.a.isInOlympiadMode()&&o.b.isInOlympiadMode()
                        &&o.a.getOlympiadGameId()==o.arena&&o.b.getOlympiadGameId()==o.arena)
                    {
                        o.observed=true;
                        if(o.allowed.test(o.owner))Olympiad.addSpectator(o.arena,o.owner,true);
                        else o.owner.sendMessage("Бой начался, но наблюдение недоступно в вашем текущем состоянии.");
                    }
                    continue;
                }
                if(now>=o.expires||!Olympiad.getInstance().inCompPeriod()||!valid(o.a)||!valid(o.b)||!o.allowed.test(o.owner))
                {o.owner.sendMessage("Заказанный бой отменён: истёк срок ожидания или изменилось состояние участников.");release(manager,o);it.remove();continue;}
                if(o.game==null)
                {
                    withdraw(o.a);withdraw(o.b);
                    if(!idle(o.a)||!idle(o.b))continue;
                    int arena=freeArena(manager);if(arena<0)continue;
                    if(!admit(o.a,o.minPoints)||!admit(o.b,o.minPoints))
                    {o.owner.sendMessage("Штатная проверка регистрации отклонила бота. Заказ отменён.");release(manager,o);it.remove();continue;}
                    o.arena=arena;
                    o.game=new OlympiadGame(arena,CompetitionType.NON_CLASSED,new ArrayList<>(List.of(o.a,o.b)));
                    if(o.game.isAborted())throw new IllegalStateException("Native game constructor aborted");
                    GAMES.add(o.game);o.task=new OrderedTask(o.game);
                    manager.getOlympiadGames().put(arena,o.game);tasks.put(arena,o.task);
                    OlympiadManager.STADIUMS[arena].setStadiaBusy();o.due=now+5000;
                    o.owner.sendMessage("Оба бота готовы. Через 5 секунд — арена "+(arena+1)+" и режим наблюдателя.");
                }
                else if(now>=o.due)
                {
                    // Native runGame performs the real admission, teleport, buffs and preparation.
                    Thread thread=new Thread(o.task,"OrderedOlympiad-"+o.arena);
                    thread.start();o.started=true;
                }
            }
            catch(Exception e)
            {
                LOG.warning("Ordered Olympiad: "+e);
                if(!o.started){release(manager,o);it.remove();o.owner.sendMessage("Заказанный бой отменён из-за ошибки сервера.");}
            }
        }
    }
    private static void release(OlympiadManager manager,Order o)
    {
        if(o.game!=null)
        {
            GAMES.remove(o.game);manager.getOlympiadGames().remove(o.arena,o.game);tasks.remove(o.arena,o.task);
            OlympiadManager.STADIUMS[o.arena].setStadiaFree();
            if(!o.started)
                for(Player p:List.of(o.a,o.b))if(p.getOlympiadGameId()==o.arena)p.setOlympiadGameId(-1);
        }
        HELD.remove(o.a.getObjectId());HELD.remove(o.b.getObjectId());
    }
}
