package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/** Per validated arena pair. Only existing owned summons; never summons or resurrects one. */
final class SummonPriority
{
    private WeakReference<Summon> cursed=new WeakReference<>(null);
    private boolean leased,previousOwned;
    static final long NO_PROGRESS_MS=8000, RETRY_MS=3000;
    private WeakReference<Summon> observed=new WeakReference<>(null);
    private long progressAt,retryAt;
    private double lastHp,bestDistance;

    void reset(){cursed.clear();observed.clear();progressAt=retryAt=0;leased=false;previousOwned=false;}
    boolean leased(){return leased;}
    void lease(boolean owned){if(!leased){leased=true;previousOwned=owned;}}
    boolean release(){boolean previous=previousOwned;leased=false;previousOwned=false;return previous;}
    static boolean panther(int id){return id>=14799&&id<=14835;}
    static boolean cursedMan(int id){return id>=14074&&id<=14110;}
    static boolean phoenix(int id){return id==14918;}
    Summon target(Player bot,Player enemy)
    { return target(bot,enemy,System.nanoTime()/1_000_000); }
    /** An unreachable/protected/non-damageable pet cannot monopolize the whole match.
     * Observe real HP loss or meaningful closing, never a mere accepted cast request. */
    Summon target(Player bot,Player enemy,long time)
    {
        Summon pet=candidate(bot,enemy);
        if(pet==null){observed.clear();progressAt=retryAt=0;return null;}
        double hp=pet.getCurrentHp(),distance=bot.calculateDistance2D(pet);
        if(observed.get()!=pet)
        {observed=new WeakReference<>(pet);progressAt=time;retryAt=0;lastHp=hp;bestDistance=distance;}
        if(time<retryAt)return null;
        if(retryAt!=0)
        {retryAt=0;progressAt=time;lastHp=hp;bestDistance=distance;}
        if(hp<lastHp-0.5||distance<bestDistance-25)
        {progressAt=time;bestDistance=Math.min(bestDistance,distance);}
        lastHp=hp;
        if(time-progressAt>=NO_PROGRESS_MS)
        {retryAt=time+RETRY_MS;return null;}
        return pet;
    }
    private Summon candidate(Player bot,Player enemy)
    {
        if(bot==null||enemy==null||bot==enemy||bot.getInstanceId()!=enemy.getInstanceId())return null;
        Summon pet=enemy.getSummon();
        if(pet==null||pet.getOwner()!=enemy||pet.isAlikeDead()||pet.isTeleporting()||pet.getInstanceId()!=bot.getInstanceId())
        {cursed.clear();return null;}
        int id=pet.getId();
        if(TankClassMechanics.tank(enemy)&&(panther(id)||phoenix(id)))return pet;
        if(enemy.getPlayerClass().equalsOrChildOf(PlayerClass.NECROMANCER)&&cursedMan(id))
        {
            // Once redirected by real invulnerability, finish this same Cursed Man even if it ends.
            if(enemy.isInvul()||cursed.get()==pet){cursed=new WeakReference<>(pet);return pet;}
        }
        cursed.clear();return null;
    }
}
