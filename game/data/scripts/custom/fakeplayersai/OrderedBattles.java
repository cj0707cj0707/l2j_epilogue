package custom.fakeplayersai;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import org.l2jmobius.gameserver.entity.actor.Player;
import custom.GiranServices.Preparation;

/** Script-to-core boundary; only actual current managed bots, never temporary training bots. */
public final class OrderedBattles
{
    private static Method request,held;
    private OrderedBattles() { }
    private static synchronized void access() throws ReflectiveOperationException
    {
        if(request!=null)return;
        Class<?> type=Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OrderedOlympiad");
        Method r=type.getMethod("request",Player.class,Player.class,Player.class,Predicate.class,int.class);
        held=type.getMethod("held",int.class);request=r;
    }
    static boolean held(int id)
    {
        try {access();return (Boolean)held.invoke(null,id);}
        catch(ReflectiveOperationException e){return false;} // feature absent: existing automatic queue remains usable
    }
    /** Only the free waiting phase; NEVER stop either bot's existing Olympiad match. */
    static boolean waitOutsideMatch(Player p)
    {
        if(!held(p.getObjectId())||p.getOlympiadGameId()!=-1||p.isInOlympiadMode()
            ||org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getInstance().playerInStadia(p))return false;
        BotSpawner.stopAutoCombat(p);p.abortAttack();p.abortCast();p.setTarget(null);
        if(!p.isAlikeDead())p.getAI().setIntentionIdle();
        return true;
    }
    public static List<BotClassPreset> classes()
    {
        Set<Integer> present=new HashSet<>();
        for(Player bot:BotManager.getBots().values())
            if(bot.isOnline()&&bot.getClient()==null)present.add(bot.getPlayerClass().getId());
        List<BotClassPreset> result=new ArrayList<>();
        for(BotClassPreset preset:BotPresets.getPresetList())if(present.contains(preset.getClassId()))result.add(preset);
        return result;
    }
    private static Player find(int id)
    {
        return BotManager.getBots().values().stream().filter(p->p.isOnline()&&p.getClient()==null
            &&p.getPlayerClass().getId()==id&&!held(p.getObjectId()))
            .min(Comparator.comparingInt(Player::getObjectId)).orElse(null);
    }
    public static String start(Player owner,int first,int second)
    {
        // Same lock as ALL auto/admin registration passes: no check/register race with reservation.
        synchronized(OlympiadRegistration.class)
        {
            if(first==second)return "Выберите две разные профессии.";
            if(!Preparation.allowed(owner)||owner.getOlympiadGameId()!=-1||owner.isRegisteredOnEvent())return "Наблюдение сейчас недоступно: выйдите из боя, очереди или другого события.";
            try
            {
                access();Player a=find(first),b=find(second);
                if(a==null||b==null)return "В текущем пуле нет свободного для заказа бота выбранного класса.";
                return (String)request.invoke(null,owner,a,b,(Predicate<Player>)p->Preparation.allowed(p)
                    &&p.getOlympiadGameId()==-1&&!p.isRegisteredOnEvent()
                    &&BotManager.getBots().get(a.getObjectId())==a&&BotManager.getBots().get(b.getObjectId())==b,BotManager.getSettings().getOlympiadMinPoints());
            }
            catch(ReflectiveOperationException e){return "Расширение заказных боёв не установлено. Нужен полный перезапуск через штатный launcher.";}
        }
    }
}
