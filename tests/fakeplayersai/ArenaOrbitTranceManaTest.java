package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.lang.invoke.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.stats.*;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;
import sun.misc.Unsafe;

/** Real installed core + native Skill/Formulas; no live database/client. */
public class ArenaOrbitTranceManaTest
{
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Field f(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    // Status UI/regen seam only: no client packets or background regeneration task
    // in this fixture. Production still calls the real Player/Creature setter facade.
    static class HeadlessStatus extends org.l2jmobius.gameserver.entity.actor.status.PlayerStatus
    {
        HeadlessStatus(Player p){super(p);}
        @Override public boolean setCurrentHp(double value,boolean broadcast)
        {
            try{double old=getCurrentHp();f(CreatureStatus.class,"_currentHp").setDouble(this,value);return old!=value;}
            catch(Exception e){throw new IllegalStateException(e);}
        }
    }
    static Player actor(int cid,boolean ranked,boolean human)throws Exception
    {
        Method m=CombatRecoveryRulesTest.class.getDeclaredMethod("actor",int.class,boolean.class,boolean.class);m.setAccessible(true);
        Player p=(Player)m.invoke(null,cid,ranked,human);
        Unsafe unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);
        HeadlessStatus status=(HeadlessStatus)unsafe.allocateInstance(HeadlessStatus.class);
        f(CreatureStatus.class,"_creature").set(status,p);f(CreatureStatus.class,"_currentHp").setDouble(status,5000);
        f(Creature.class,"_status").set(p,status);
        f(WorldObject.class,"_name").set(p,"Fixture");
        Class<?> template=org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate.class;
        f(template,"_baseMAtk").setInt(p.getTemplate(),1000);f(template,"_baseMDef").setInt(p.getTemplate(),1000);
        f(template,"_baseMEN").setInt(p.getTemplate(),40);
        p.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet(Stat.MAGIC_ATTACK,0x60,new Object(),1000,null));
        p.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet(Stat.MAGIC_DEFENCE,0x60,new Object(),1000,null));
        return p;
    }
    static void mp(Player p,double n)throws Exception{f(CreatureStatus.class,"_currentMp").setDouble(p.getStatus(),n);}
    static void hp(Player p,double n)throws Exception{f(CreatureStatus.class,"_currentHp").setDouble(p.getStatus(),n);}
    @SuppressWarnings("unchecked") static void learn(Player p,Skill s)throws Exception{((Map<Integer,Skill>)f(Creature.class,"_skills").get(p)).put(s.getId(),s);}
    static void trance()throws Exception
    {
        Player p=actor(97,true,false),target=actor(94,true,true);
        for(int level:new int[]{10,101,130,201,230})
        {
            Skill original=SkillData.getInstance().getSkill(1394,level);check(original!=null,"native Trance level "+level);
            learn(p,original);CardinalTrance.apply(p);
            Skill scoped=p.getKnownSkill(1394);check(scoped!=original&&scoped.getLevel()==level&&scoped.getActivateRate()==20,"bot-only20 copy, same native level");
            // Reproduce an old private template and verify runtime repair without changing enchant.
            org.l2jmobius.gameserver.util.StatSet set=new org.l2jmobius.gameserver.util.StatSet();
            set.set("skill_id",1394);set.set("level",level);set.set("operateType","A2");set.set("targetType","ONE");set.set("activateRate",65);set.set("minChance",65);set.set("maxChance",65);
            learn(p,new Skill(set));CardinalTrance.apply(p);check(p.getKnownSkill(1394).getLevel()==level&&p.getKnownSkill(1394).getActivateRate()==20,"legacy private template replaced with20");
            CardinalTrance.apply(p);check(p.getKnownSkill(1394).getLevel()==level&&p.getKnownSkill(1394).getActivateRate()==20,"idempotent scoped20 repair");
            Player human=actor(97,true,true);Skill privateSkill=new Skill(set);learn(human,privateSkill);CardinalTrance.apply(human);
            check(human.getKnownSkill(1394)==privateSkill,"human skill untouched");
        }
        Skill nativeChance=SkillData.getInstance().getSkill(1394,230);
        check(nativeChance.getActivateRate()==100,"native +30 Chance activateRate100 preserved, not a private guarantee");
        for(int i=0;i<100;i++)check(Formulas.calcEffectSuccess(p,target,nativeChance),"native high-activate branch, no fixed65 clamp");
        Object immunity=new Object();target.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet(Stat.DEBUFF_IMMUNITY,0x70,immunity,1,null));
        for(int i=0;i<100;i++)check(!Formulas.calcEffectSuccess(p,target,nativeChance),"debuff immunity still wins");target.removeStatsOwner(immunity);
        target.getStat().getTraitsInvul()[TraitType.SLEEP.ordinal()]=1;
        for(int i=0;i<100;i++)check(Formulas.calcEffectSuccess(p,target,nativeChance),"native activate100 bypasses trait calculation; do not add a private clamp");
        for(int i=0;i<100;i++)check(!Formulas.calcEffectSuccess(p,target,SkillData.getInstance().getSkill(1394,10)),"ordinary base Trance still uses native sleep immunity");
    }
    static void orbit()
    {
        for(int step:new int[]{100,120,180})for(int direction:new int[]{1,-1})
        {
            Location p=new Location(450,0,7);double angle=0;
            for(int i=0;i<1000;i++)
            {
                Location n=ArenaCombatRules.orbit(p.getX(),p.getY(),7,50,10,0,0,step,direction);
                double radius=Math.hypot(n.getX(),n.getY());check(radius>=448&&radius<=451,"orbit stays on450 ring");
                check(Math.hypot(n.getX()-p.getX(),n.getY()-p.getY())<=step+1,"bounded chord, no teleport");
                double delta=Math.atan2(n.getY(),n.getX())-Math.atan2(p.getY(),p.getX());
                if(delta>Math.PI)delta-=2*Math.PI;if(delta< -Math.PI)delta+=2*Math.PI;
                check(delta*direction>0,"persistent angular movement, not one-direction radial retreat");angle+=delta;p=n;
            }
            check(Math.abs(angle)>10*Math.PI,"multiple full revolutions without450 dead end");
        }
        for(int x:new int[]{0,10,100,450,460,900})
        {
            Location n=ArenaCombatRules.orbit(x,0,7,30,40,0,0,100,1);
            check(Math.hypot(n.getX()-x,n.getY())<=101,"bounded transition from inside/outside ring");
        }
        // Enemy north: clockwise is away; enemy south: counter-clockwise is away.
        check(ArenaCombatRules.awayDirection(450,0,7,450,100,0,0,120,1)==-1,"enemy north selects clockwise escape");
        check(ArenaCombatRules.awayDirection(450,0,7,450,-100,0,0,120,-1)==1,"enemy south dynamically switches to counter-clockwise escape");
        check(ArenaCombatRules.awayDirection(450,0,7,0,0,0,0,120,-1)==-1,"equal separation retains previous side without jitter");
        Location toward=ArenaCombatRules.orbit(-450,0,7,450,0,0,0,120,1);
        check(!ArenaCombatRules.awayStep(-450,0,450,0,toward),"farthest ring point holds instead of running closer to enemy");
        for(int ey:new int[]{-300,-100,100,300})
        {
            int d=ArenaCombatRules.awayDirection(450,0,7,450,ey,0,0,120,1);
            Location n=ArenaCombatRules.orbit(450,0,7,450,ey,0,0,120,d);
            check(ArenaCombatRules.awayStep(450,0,450,ey,n),"chosen side never reduces separation ey="+ey);
        }
    }
    @SuppressWarnings("unchecked")
    static void boundaries()throws Throwable
    {
        Class<?> guard=Class.forName("org.l2jmobius.gameserver.mechanics.skill.ArenaBoundaryGuard");
        Method bind=guard.getMethod("bind",Player.class,Player.class,int.class,int.class),clear=guard.getMethod("clear",int.class);
        Method clamp=guard.getMethod("clamp",WorldObject.class,int.class,int.class);
        for(boolean ranked:new boolean[]{false,true})
        {
            Player p=actor(95,ranked,false),enemy=actor(90,ranked,false);
            if(!ranked)for(Player a:List.of(p,enemy))
            {f(Player.class,"_isInDuel").setBoolean(a,true);f(Player.class,"_duelId").setInt(a,50);f(Player.class,"_duelState").setInt(a,Duel.DUELSTATE_DUELLING);}
            bind.invoke(null,p,enemy,100,200);
            for(int angle=0;angle<360;angle++)for(int r:new int[]{0,450,459,460,461,600,2000})
            {
                int x=100+(int)Math.round(r*Math.cos(Math.toRadians(angle))),y=200+(int)Math.round(r*Math.sin(Math.toRadians(angle)));
                int[] xy=(int[])clamp.invoke(null,p,x,y);
                check(Math.hypot(xy[0]-100,xy[1]-200)<=460,"actual coordinate clamp <=460 ranked="+ranked+" angle="+angle+" r="+r);
                if(Math.hypot(x-100,y-200)<=460)check(xy[0]==x&&xy[1]==y,"inside unchanged");
            }
            // Invoke actual WorldObject setXYZ wrapper (bypass only fixture-unsafe zone bookkeeping in Creature).
            MethodHandle commit=MethodHandles.privateLookupIn(WorldObject.class,MethodHandles.lookup()).findSpecial(WorldObject.class,
                "setXYZ",MethodType.methodType(void.class,int.class,int.class,int.class),WorldObject.class).bindTo(p);
            commit.invoke(10000,20000,7);check(Math.hypot(p.getX()-100,p.getY()-200)<=460,"real native position commit bounded");
            p.setLocation(new Location(9000,9000,7,10,10));check(Math.hypot(p.getX()-100,p.getY()-200)<=460,"setLocation bypass bounded too");
            clear.invoke(null,p.getObjectId());int[] xy=(int[])clamp.invoke(null,p,9000,9000);check(xy[0]==9000&&xy[1]==9000,"explicit teardown releases boundary");
            Player human=actor(90,ranked,true);bind.invoke(null,p,human,100,200);xy=(int[])clamp.invoke(null,p,9000,9000);
            check(xy[0]==9000&&xy[1]==9000,"bot versus human has no460 clamp");
            bind.invoke(null,p,enemy,100,200);f(Player.class,"_isOnline").setBoolean(enemy,false);
            xy=(int[])clamp.invoke(null,p,9000,9000);check(xy[0]==9000,"offline invalidates lease synchronously");
            f(Player.class,"_isOnline").setBoolean(enemy,true);bind.invoke(null,p,enemy,100,200);
            f(WorldObject.class,"_location").set(enemy,new Location(100,200,7,0,11));
            xy=(int[])clamp.invoke(null,p,9000,9000);check(xy[0]==9000,"instance change invalidates lease synchronously");
            if(ranked)
            {
                f(WorldObject.class,"_location").set(enemy,new Location(100,200,7,0,10));bind.invoke(null,p,enemy,100,200);
                f(Player.class,"_olympiadStart").setBoolean(enemy,false);xy=(int[])clamp.invoke(null,p,9000,9000);
                check(xy[0]==9000,"countdown/end does not retain hard movement constraint");
                f(Player.class,"_olympiadStart").setBoolean(enemy,true);
            }
            int[] c=ArenaCombatRules.center(p);
            f(WorldObject.class,"_location").set(p,new Location(c[0]+100,c[1],7,0,10));
            f(WorldObject.class,"_location").set(enemy,new Location(c[0]-100,c[1],7,0,10));
            Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
            roster.put(p.getObjectId(),p);roster.put(enemy.getObjectId(),enemy);
            check(!ArenaCombatRules.update(p,enemy,BotPresets.getPresetByClassId(95)),"actual shared adapter admits pair");
            for(Player a:List.of(p,enemy))
            {
                xy=(int[])clamp.invoke(null,a,c[0]+5000,c[1]+5000);
                check(ArenaCombatRules.meleeClass(a) ? xy[0]==c[0]+5000&&xy[1]==c[1]+5000 : Math.hypot(xy[0]-c[0],xy[1]-c[1])<=460,"own class role controls radial binding before either driver: melee pursuit unbound, ranged bounded");
                ArenaCombatRules.clear(a.getObjectId());roster.remove(a.getObjectId());
            }
        }
    }
    @SuppressWarnings("unchecked")
    static void mana()throws Exception
    {
        for(BotClassPreset preset:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})
        {
            Player p=actor(preset.getClassId(),ranked,false);mp(p,0);
            Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);roster.put(p.getObjectId(),p);
            Method floor=BotManager.class.getDeclaredMethod("olympiadMpFloorAllowed",Player.class);floor.setAccessible(true);
            check(!(boolean)floor.invoke(null,p),"no managed class has legacy MP refill");roster.remove(p.getObjectId());
            boolean mage=preset.getClassId()!=97&&(preset.isCaster()||p.getPlayerClass().isMage());
            check(!ArenaCombatRules.exhausted(p,preset),"all19-class mage/physical/Cardinal policy");
            ArenaCombatRules.drainMage(p,preset);check(p.getCurrentHp()==5000,"native MP exhaustion never changes HP");
            check(p.getCurrentMp()==0&&p.getCurrentCp()==0,"no MP/CP change");
            hp(p,50);ArenaCombatRules.drainMage(p,preset);check(p.getCurrentHp()==50,"never heal a mage below101");
            Player human=actor(preset.getClassId(),ranked,true);mp(human,0);ArenaCombatRules.drainMage(human,preset);check(human.getCurrentHp()==5000,"human HP untouched");
            int minimum=Integer.MAX_VALUE;
            for(int id:preset.getRotation())
            {
                Skill s=SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));if(s==null)continue;learn(p,s);
                if(!BotSpawner.isOpenerOnlySkill(id)&&!s.isPassive()&&!s.isToggle()&&!s.isSuicideAttack()&&(s.hasNegativeEffect()||s.isDebuff()))
                    minimum=Math.min(minimum,p.getStat().getMpInitialConsume(s)+p.getStat().getMpConsume(s));
            }
            if(mage&&minimum>0&&minimum<Integer.MAX_VALUE)
            {
                mp(p,minimum-1);check(!ArenaCombatRules.exhausted(p,preset),"unpayable skills never trigger artificial exhaustion penalty");
                mp(p,minimum);check(!ArenaCombatRules.exhausted(p,preset),"exact payable skill unlocks normal HP behavior");
            }
        }
    }
    public static void main(String[] args)throws Throwable
    {
        f(LowManaAutoAttackTest.class,"unsafe").set(null,f(Unsafe.class,"theUnsafe").get(null));
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;handlers.EffectMasterHandler.main(new String[0]);
        if(args.length>1)
        {
            Player p=actor(90,true,false),enemy=actor(95,true,false);
            @SuppressWarnings("unchecked") Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
            roster.put(p.getObjectId(),p);roster.put(enemy.getObjectId(),enemy);
            check(ArenaCombatRules.update(p,enemy,BotPresets.getPresetByClassId(90)),"unpatched core holds bot/bot combat closed");
            System.out.println("PASS: missing boundary core holds bot/bot combat closed");System.exit(0);
        }
        trance();orbit();boundaries();mana();
        System.out.println("PASS: "+checks+" native Trance/enchant, dynamic two-way away-orbit, installed-core boundary and all19-class mana checks; no live match");
        System.exit(0);
    }
}
