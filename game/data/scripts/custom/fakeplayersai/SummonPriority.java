package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/** Per validated arena pair. Only existing owned summons; never summons or resurrects one. */
final class SummonPriority
{
    private WeakReference<Summon> cursed=new WeakReference<>(null);
    private boolean leased,previousOwned;
    void reset(){cursed.clear();leased=false;previousOwned=false;}
    boolean leased(){return leased;}
    void lease(boolean owned){if(!leased){leased=true;previousOwned=owned;}}
    boolean release(){boolean previous=previousOwned;leased=false;previousOwned=false;return previous;}
    static boolean panther(int id){return id>=14799&&id<=14835;}
    static boolean cursedMan(int id){return id>=14074&&id<=14110;}
    static boolean phoenix(int id){return id==14918;}
    Summon target(Player bot,Player enemy)
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
