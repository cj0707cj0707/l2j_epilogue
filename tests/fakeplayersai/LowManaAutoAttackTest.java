package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.stats.Calculator;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;
import org.l2jmobius.gameserver.network.GameClient;
import sun.misc.Unsafe;

/** Real predicate/weapon permission, learned skill costs and bow stat lifecycle. No live server. */
public final class LowManaAutoAttackTest
{
    private static Unsafe unsafe;
    private static int checks, sequence=5000;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void equal(double a,double b,String why){check(Math.abs(a-b)<1e-8,why+": "+a+" != "+b);}
    private static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void mp(Player p,double v)throws Exception{field(CreatureStatus.class,"_currentMp").setDouble(p.getStatus(),v);}
    private static Player actor(int cid,boolean ranked,boolean human)throws Exception
    {
        Player p=(Player)unsafe.allocateInstance(Player.class);
        PlayerTemplate t=(PlayerTemplate)unsafe.allocateInstance(PlayerTemplate.class);
        field(PlayerTemplate.class,"_playerClass").set(t,PlayerClass.getPlayerClass(cid));
        field(Creature.class,"_template").set(p,t);
        field(Creature.class,"_calculators").set(p,new Calculator[Stat.NUM_STATS]);
        field(Creature.class,"_stat").set(p,new PlayerStat(p));
        field(Creature.class,"_skills").set(p,new HashMap<Integer,Skill>());
        field(Creature.class,"_effectList").set(p,new EffectList(p));
        field(Creature.class,"_status").set(p,unsafe.allocateInstance(PlayerStatus.class));
        field(WorldObject.class,"_objectId").setInt(p,sequence++);
        field(WorldObject.class,"_location").set(p,new Location(100,200,0,0,10));
        field(Player.class,"_isOnline").setBoolean(p,true);
        field(Player.class,"_inOlympiadMode").setBoolean(p,ranked);
        field(Player.class,"_olympiadStart").setBoolean(p,ranked);
        field(Player.class,"_olympiadGameId").setInt(p,ranked?5:-1);
        if(human)field(Player.class,"_client").set(p,unsafe.allocateInstance(GameClient.class));
        PlayerInventory inv=(PlayerInventory)unsafe.allocateInstance(PlayerInventory.class);
        field(Inventory.class,"_paperdoll").set(inv,new Item[32]);
        field(Player.class,"_inventory").set(p,inv);
        field(Player.class,"_fistsWeaponItem").set(p,unsafe.allocateInstance(Weapon.class));
        p.addStatFunc(new FuncAdd(Stat.POWER_ATTACK_RANGE,0x10,new Object(),40,null));
        return p;
    }
    @SuppressWarnings("unchecked")
    private static void learn(Player p,BotClassPreset preset)throws Exception
    {
        Map<Integer,Skill> skills=(Map<Integer,Skill>)field(Creature.class,"_skills").get(p);
        for(int id:preset.getRotation())
        {
            Skill s=SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));
            check(s!=null,"actual rotation template "+id);skills.put(id,s);
        }
    }
    private static double shot(Player p){return p.calcStat(Stat.BOW_MP_CONSUME_RATE,12,null,null);}
    @SuppressWarnings("unchecked")
    private static void rosterAndThresholds()throws Exception
    {
        Map<Integer,Player> roster=(Map<Integer,Player>)field(BotManager.class,"BOTS").get(null);
        Method floor=BotManager.class.getDeclaredMethod("olympiadMpFloorAllowed",Player.class);floor.setAccessible(true);
        int classes=0;
        for(BotClassPreset preset:BotPresets.getPresets())
        {
            classes++;
            for(boolean ranked:new boolean[]{false,true})
            {
                Player bot=actor(preset.getClassId(),ranked,false);
                learn(bot,preset);mp(bot,0);
                boolean physical=!preset.isCaster()&&!bot.getPlayerClass().isMage()&&preset.getClassId()!=97;
                check(BotTactics.needsManaAutoAttack(bot,preset)==physical,"zero-MP class guard "+preset.getClassId());
                check(BotTactics.allowsBasicAttack(bot,preset)==physical,"armed zero-MP attack permission "+preset.getClassId());
                if(physical)
                {
                    Field weapon=field(Player.class,"_fistsWeaponItem");Object equipped=weapon.get(bot);
                    weapon.set(bot,null);check(!BotTactics.allowsBasicAttack(bot,preset),"no weapon does not bypass attack gate");weapon.set(bot,equipped);
                    field(EffectList.class,"_effectFlags").setInt(bot.getEffectList(),EffectFlag.DISARMED.getMask());
                    check(!BotTactics.allowsBasicAttack(bot,preset),"low MP never bypasses real disarm");
                    field(EffectList.class,"_effectFlags").setInt(bot.getEffectList(),0);
                    Player enemy=actor(94,ranked,true);
                    equal(BotTactics.tacticalApproachRange(bot,enemy,preset),40,"normal weapon range, not distant control range");
                    roster.put(bot.getObjectId(),bot);
                    check(!(boolean)floor.invoke(null,bot),"physical bot cannot refill from old Olympiad floor");
                    roster.remove(bot.getObjectId());
                    mp(bot,100000);
                    check(!BotTactics.needsManaAutoAttack(bot,preset),"enough MP returns to skill policy");
                    if(preset.getWeaponKind()==BotClassPreset.WeaponKind.DAGGER)
                        check(!BotTactics.allowsBasicAttack(bot,preset),"normal-MP dagger restriction preserved");
                    // Use one real learned offensive skill to check the exact core cost boundary.
                    Skill attack=null;
                    Method kind=BotTactics.class.getDeclaredMethod("kind",Skill.class);kind.setAccessible(true);
                    Method self=BotTactics.class.getDeclaredMethod("self",TacticsPolicy.Kind.class);self.setAccessible(true);
                    for(int id:preset.getRotation())
                    {
                        Skill s=bot.getKnownSkill(id);TacticsPolicy.Kind k=(TacticsPolicy.Kind)kind.invoke(null,s);
                        if(k!=null&&!(boolean)self.invoke(null,k)&&s.getMpConsume()+s.getMpInitialConsume()>0){attack=s;break;}
                    }
                    check(attack!=null,"real payable offensive skill exists "+preset.getClassId());
                    BotClassPreset one=new BotClassPreset(preset.getClassId(),"test",preset.getGear(),preset.getWeaponKind(),false,false,false,new int[]{attack.getId()},new int[0]);
                    int cost=bot.getStat().getMpInitialConsume(attack)+bot.getStat().getMpConsume(attack);
                    mp(bot,cost-1);check(BotTactics.needsManaAutoAttack(bot,one),"one MP short activates fallback");
                    mp(bot,cost);check(!BotTactics.needsManaAutoAttack(bot,one),"exact sufficient MP restores skills");
                    Object reduction=new Object();bot.addStatFunc(new FuncMul(Stat.MP_CONSUME,0x30,reduction,.5,null));
                    int reduced=bot.getStat().getMpInitialConsume(attack)+bot.getStat().getMpConsume(attack);
                    mp(bot,reduced);check(!BotTactics.needsManaAutoAttack(bot,one),"real reduced-cost stat respected");
                    bot.removeStatsOwner(reduction);
                }
                Player human=actor(preset.getClassId(),ranked,true);mp(human,0);
                check(!BotTactics.needsManaAutoAttack(human,preset),"human excluded");
            }
        }
        check(classes==19,"current actual nineteen-class roster covered");
    }
    private static void bowLifecycle()throws Exception
    {
        BotClassPreset preset=BotPresets.getPresetByClassId(92);
        for(boolean ranked:new boolean[]{false,true})
        {
            Player bot=actor(92,ranked,false);learn(bot,preset);mp(bot,0);
            Object buff=new Object();bot.addStatFunc(new FuncMul(Stat.BOW_MP_CONSUME_RATE,0x30,buff,.8,null));
            ManaAutoAttackSupport support=new ManaAutoAttackSupport();
            Skill skill=bot.getKnownSkill(preset.getRotation()[0]);
            int cost=bot.getStat().getMpConsume(skill);
            for(int i=0;i<300;i++)
            {
                support.update(bot,preset);
                equal(shot(bot),0,"normal shot possible at zero MP");
                equal(bot.getCurrentMp(),0,"no MP refill");
                check(bot.getStat().getMpConsume(skill)==cost,"offensive skill still pays native MP");
            }
            mp(bot,100000);support.update(bot,preset);equal(shot(bot),9.6,"MP recovery restores existing shot stat");
            mp(bot,0);support.update(bot,preset);support.clear();support.clear();equal(shot(bot),9.6,"double clear preserves other owners");
            Method state=BotTactics.class.getDeclaredMethod("state",int.class,long.class);state.setAccessible(true);
            Object st=state.invoke(null,bot.getObjectId(),1000);
            ManaAutoAttackSupport owned=(ManaAutoAttackSupport)field(st.getClass(),"manaAutoAttack").get(st);
            owned.update(bot,preset);BotTactics.forget(bot.getObjectId());equal(shot(bot),9.6,"forget removes only owned shot waiver");
            st=state.invoke(null,bot.getObjectId(),1000);
            owned=(ManaAutoAttackSupport)field(st.getClass(),"manaAutoAttack").get(st);
            owned.update(bot,preset);check(!BotTactics.tick(bot,null,null,ranked),"invalid pair rejects control");
            equal(shot(bot),9.6,"invalid context actually clears attached owner");
            field(BotTactics.class,"nextSweep").setLong(null,0);
            st=state.invoke(null,bot.getObjectId(),1000);
            owned=(ManaAutoAttackSupport)field(st.getClass(),"manaAutoAttack").get(st);owned.update(bot,preset);
            state.invoke(null,900000,30*60_000L+1001);
            equal(shot(bot),9.6,"actual TTL eviction clears shot exemption");
            st=state.invoke(null,bot.getObjectId(),30*60_000L+1002);
            owned=(ManaAutoAttackSupport)field(st.getClass(),"manaAutoAttack").get(st);owned.update(bot,preset);
            for(int id=100000;id<102048;id++)state.invoke(null,id,30*60_000L+1002);
            equal(shot(bot),9.6,"actual capacity eviction clears shot exemption");
            support.update(bot,preset);field(Player.class,"_isOnline").setBoolean(bot,false);
            support.update(bot,preset);equal(shot(bot),9.6,"offline transition removes exemption");
        }
    }
    public static void main(String[] args)throws Exception
    {
        unsafe=(Unsafe)field(Unsafe.class,"theUnsafe").get(null);
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]);
        rosterAndThresholds();bowLifecycle();
        System.out.println("PASS: "+checks+" actual-core low-MP/all19-class/weapon/cost-boundary/floor/bow-stat lifecycle checks; no live match");
    }
}