package custom.fakeplayersai;

import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;
import handlers.skill.effects.Stun;

/** Real JAR getters/real Stun handler, but inert fixture actors and no server/DB startup.
 * Unsafe is TEST ONLY: avoid Player and BuffInfo constructors that start server services. */
public final class RankedRushOriginalStunRegression
{
	private static int checks;
	private static final Object UNSAFE;
	private static final Method ALLOCATE;
	static {
		try {
			Class<?> type=Class.forName("sun.misc.Unsafe"); Field f=type.getDeclaredField("theUnsafe"); f.setAccessible(true);
			UNSAFE=f.get(null); ALLOCATE=type.getMethod("allocateInstance",Class.class);
		} catch(Exception e) { throw new ExceptionInInitializerError(e); }
	}
	private static void check(boolean value,String message) { checks++; if(!value)throw new AssertionError(message); }
	private static Method manager(String name,Class<?>...args)throws Exception { Method m=BotManager.class.getDeclaredMethod(name,args);m.setAccessible(true);return m; }
	private static Object allocate(Class<?> type)throws Exception {return ALLOCATE.invoke(UNSAFE,type);}
	private static void set(Object o,Class<?> owner,String name,Object value)throws Exception {Field f=owner.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
	private static Skill skill(int id)throws Exception {
		StatSet set=new StatSet();set.set("skill_id",id);set.set("level",1);set.set("operateType","DA2");set.set("targetType","FRONT_AREA");set.set("abnormalType","STUN");
		return new Skill(set);
	}
	private static BuffInfo info(Player bot,Creature enemy,Skill skill,AbstractEffect effect)throws Exception {
		BuffInfo info=(BuffInfo)allocate(BuffInfo.class);
		set(info,BuffInfo.class,"_effector",bot);set(info,BuffInfo.class,"_effected",enemy);set(info,BuffInfo.class,"_skill",skill);
		set(info,BuffInfo.class,"_effects",new ArrayList<>(effect==null?List.of():List.of(effect)));
		info.setInUse(true);info.setFinishType(SkillFinishType.NORMAL);return info;
	}
	private static void actualAppliedEffect()throws Exception {
		Player bot=(Player)allocate(Player.class),enemy=(Player)allocate(Player.class),other=(Player)allocate(Player.class);
		StatSet params=new StatSet();params.set("name","Stun");
		Stun actual=new Stun(null,null,params,new StatSet());
		check(actual.getEffectType()==EffectType.STUN,"actual STUN handler enum");
		check((actual.getEffectFlags()&EffectFlag.STUNNED.getMask())!=0,"actual STUN handler flag");
		Method owns=manager("isOwnRushStun",Player.class,Creature.class,BuffInfo.class);
		Skill rush=skill(793); BuffInfo original=info(bot,enemy,rush,actual);
		check((boolean)owns.invoke(null,bot,enemy,original),"own actual793 applied stun accepted");
		check(!(boolean)owns.invoke(null,other,enemy,original),"another caster's793 not ours");
		check(!(boolean)owns.invoke(null,bot,other,original),"wrong effected target");
		check(!(boolean)owns.invoke(null,null,enemy,original),"null owner rejected");
		check(!(boolean)owns.invoke(null,bot,enemy,info(bot,enemy,skill(92),actual)),"other STUN skill not793");
		check(!(boolean)owns.invoke(null,bot,enemy,info(bot,enemy,rush,null)),"damage-only/resisted793 cannot create stun loop");
		original.setInUse(false);check(!(boolean)owns.invoke(null,bot,enemy,original),"inactive original rejected");original.setInUse(true);
		original.setFinishType(SkillFinishType.REMOVED);check(!(boolean)owns.invoke(null,bot,enemy,original),"cancelled original rejected");original.setFinishType(SkillFinishType.NORMAL);
		// Build only inert collection fixtures. Never call EffectList.add/applyEffects/onStart.
		EffectList list=new EffectList(enemy);set(enemy,Creature.class,"_effectList",list);
		list.getDebuffs().add(original);
		Field map=EffectList.class.getDeclaredField("_stackedEffects");map.setAccessible(true);
		@SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)map.get(list);
		slots.put(AbnormalType.STUN,original);set(list,EffectList.class,"_effectFlags",EffectFlag.STUNNED.getMask());
		Method applied=manager("ownRushStun",Player.class,Creature.class);
		check(applied.invoke(null,bot,enemy)==original,"exact original registered in applied list/abnormal slot");
		BuffInfo replacement=info(bot,enemy,rush,actual);slots.put(AbnormalType.STUN,replacement);
		check(applied.invoke(null,bot,enemy)==null,"original no longer active abnormal slot, even when same skill ID");
		slots.put(AbnormalType.STUN,original);set(list,EffectList.class,"_effectFlags",0);
		check(applied.invoke(null,bot,enemy)==null,"missing actual STUN flag fails closed");
		set(list,EffectList.class,"_effectFlags",EffectFlag.STUNNED.getMask());list.getDebuffs().clear();
		check(applied.invoke(null,bot,enemy)==null,"removed from actual applied list fails closed");
	}
	private static void identityPolicy()throws Exception {
		Class<?> type=Class.forName("custom.fakeplayersai.BotManager$RushStunLoop");
		Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);
		Method request=type.getDeclaredMethod("requested",Object.class,Object.class,long.class);request.setAccessible(true);
		Method observe=type.getDeclaredMethod("observe",Object.class,Object.class,long.class);observe.setAccessible(true);
		Object target=new Object(),foreignTarget=new Object(),original=new Object(),replacement=new Object();
		Object loop=ctor.newInstance();
		check((int)observe.invoke(loop,target,original,0L)==1,"current genuine applied stun acquired even without observing our request");
		request.invoke(loop,target,null,0L);check((int)observe.invoke(loop,target,null,1L)==2,"expired actual effect ends phase despite a pending request");
		check((int)observe.invoke(loop,target,original,2L)==1,"new applied effect acquired after own cast");
		for(int i=0;i<100;i++){request.invoke(loop,target,original,3L+i);check((int)observe.invoke(loop,target,original,10000L+i)==1,"repeat damage keeps original identity, no guessed stun timer");}
		check((int)observe.invoke(loop,target,replacement,10200L)==1,"current live replacement keeps Rush-only phase");
		check((int)observe.invoke(loop,target,replacement,10201L)==1,"replacement followed without synthetic duration change");
		loop=ctor.newInstance();request.invoke(loop,target,original,0L);
		check((int)observe.invoke(loop,target,original,1L)==1,"preexisting real stun is sufficient for latest requested rule");
		check((int)observe.invoke(loop,target,replacement,6001L)==1,"native effect, not a guessed request timeout, determines active phase");
		loop=ctor.newInstance();request.invoke(loop,target,null,0L);observe.invoke(loop,target,original,1L);
		check((int)observe.invoke(loop,foreignTarget,original,2L)==2,"target swap ends lease");
		loop=ctor.newInstance();request.invoke(loop,target,null,0L);observe.invoke(loop,target,original,1L);
		check((int)observe.invoke(loop,target,null,2L)==2,"expiry/cancellation ends lease immediately");
		request.invoke(loop,target,null,3L);check((int)observe.invoke(loop,target,replacement,4L)==1,"later NORMAL own Rush can start a fresh independent stun lease");
	}
	private static void safetyAndGeometry()throws Exception {
		check(Player.class.getMethod("isCastingNow").getReturnType()==boolean.class,"actual JAR normal cast descriptor");
		check(Player.class.getMethod("isCastingSimultaneouslyNow").getReturnType()==boolean.class,"actual JAR simultaneous cast descriptor");
		Method allowed=manager("rushStunRepeatAllowed",boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,boolean.class,double.class,double.class,double.class);
		for(int bits=0;bits<4;bits++) {
			boolean normal=(bits&1)!=0, simultaneous=(bits&2)!=0;
			check((boolean)allowed.invoke(null,true,true,true,normal||simultaneous,false,false,true,true,true,220d,600d,0d)==(!normal&&!simultaneous),"both casting modes block repeat "+bits);
		}
		for(int bits=0;bits<512;bits++) {
			boolean[] b=new boolean[9];for(int i=0;i<9;i++)b[i]=(bits&(1<<i))!=0;
			boolean expected=b[0]&&b[1]&&b[2]&&!b[3]&&!b[4]&&!b[5]&&b[6]&&b[7]&&b[8];
			check((boolean)allowed.invoke(null,b[0],b[1],b[2],b[3],b[4],b[5],b[6],b[7],b[8],220d,600d,0d)==expected,"gate/resources/CC/disarm/blocked/LoS/geodata/condition matrix "+bits);
		}
		for(double d:new double[]{-1,0,199.999,600.001,Double.NaN,Double.POSITIVE_INFINITY})check(!(boolean)allowed.invoke(null,true,true,true,false,false,false,true,true,true,d,600d,0d),"illegal rush distance "+d);
		for(double d:new double[]{200,220,600})check((boolean)allowed.invoke(null,true,true,true,false,false,false,true,true,true,d,600d,0d),"legal rush distance "+d);
		check(!(boolean)allowed.invoke(null,true,true,true,false,false,false,true,true,true,220d,600d,150d),"vertical barrier");
		Method candidates=manager("rushStunRetreatCandidates",int.class,int.class,int.class,int.class,int.class,int.class,int.class,double.class);
		Random random=new Random(793);
		for(int i=0;i<2000;i++) {
			int x=random.nextInt(1801)-900,y=random.nextInt(1801)-900,ex=x+random.nextInt(201)-100,ey=y+random.nextInt(201)-100;
			@SuppressWarnings("unchecked") List<Location> points=(List<Location>)candidates.invoke(null,x,y,123,ex,ey,0,0,1200d);
			for(Location p:points)check(Math.hypot((double)p.getX()-ex,(double)p.getY()-ey)>=200&&Math.hypot(p.getX(),p.getY())<=1200&&Math.hypot((double)p.getX()-x,(double)p.getY()-y)<=241&&p.getZ()==123,"bounded arena retreat reaches legal793 distance");
		}
		@SuppressWarnings("unchecked") List<Location> zero=(List<Location>)candidates.invoke(null,0,0,0,0,0,0,0,1200d);
		check(!zero.isEmpty(),"coincident actors choose finite legal escape direction");
		check(((List<?>)candidates.invoke(null,0,0,0,0,0,0,0,100d)).isEmpty(),"no legal arena retreat returns none");
		check(((List<?>)candidates.invoke(null,220,0,0,0,0,0,0,1200d)).isEmpty(),"already sufficient220 margin, do not retreat more");
	}
	public static void main(String[]args)throws Exception {
		actualAppliedEffect();identityPolicy();safetyAndGeometry();
		System.out.println("PASS: "+checks+" ranked original-stun Rush checks (actual JAR APIs + pure policies; no live server)");
	}
}
