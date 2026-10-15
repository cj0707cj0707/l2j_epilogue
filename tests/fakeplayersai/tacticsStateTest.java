package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;
import java.util.Map;

/** Exercises real production bookkeeping without constructing a GameServer/DB. */
public final class tacticsStateTest
{
	private static void check(boolean condition, String message)
	{
		if (!condition) { throw new AssertionError(message); }
	}
	/** Source-backed enum fixture: no GameServer, DB, actors, or applied effects. */
	private static final class AuditEffect extends AbstractEffect
	{
		private final EffectType type;
		AuditEffect(String name, EffectType type)
		{
			super(null, null, effectSet(name), new StatSet()); this.type = type;
		}
		@Override public EffectType getEffectType() { return type; }
	}
	private static StatSet effectSet(String name)
	{
		StatSet set = new StatSet(); set.set("name", name); return set;
	}
	private static Skill skill(int id, String target)
	{
		StatSet set = new StatSet(); set.set("skill_id",id); set.set("level",1);
		set.set("operateType","A1"); set.set("targetType",target);
		return new Skill(set);
	}
	private static TacticsPolicy.Kind classification(Skill skill) throws Exception
	{
		Method m = BotTactics.class.getDeclaredMethod("kind",Skill.class); m.setAccessible(true);
		return (TacticsPolicy.Kind)m.invoke(null,skill);
	}
	private static void actualHandlers() throws Exception
	{
		String[] names = {"Backstab","FatalBlow","EnergyDamage","PhysicalSoulDamage","PhysicalMute","Disarm","CpDamPercent","HealPercent"};
		TacticsPolicy.Kind[] expected = {TacticsPolicy.Kind.DAMAGE,TacticsPolicy.Kind.DAMAGE,TacticsPolicy.Kind.DAMAGE,TacticsPolicy.Kind.DAMAGE,
			TacticsPolicy.Kind.CONTROL,TacticsPolicy.Kind.CONTROL,TacticsPolicy.Kind.DAMAGE,TacticsPolicy.Kind.HEAL};
		for (int i=0;i<names.length;i++)
		{
			AbstractEffect effect = (AbstractEffect)Class.forName("handlers.skill.effects."+names[i])
				.getConstructor(Condition.class,Condition.class,StatSet.class,StatSet.class)
				.newInstance(null,null,effectSet(names[i]),new StatSet());
			Skill skill = skill(20000+i,i==7 ? "SELF":"ONE"); skill.addEffect(EffectScope.GENERAL,effect);
			check(classification(skill)==expected[i],"actual handler classification "+names[i]);
			if (i<4) check(skill.isDamage(),"actual blows/energy/soul DO set core damage flag "+names[i]);
			if (i==4 || i==5) check(effect.getEffectType()==EffectType.NONE,"actual control handler needs named fallback "+names[i]);
		}
		check(classification(skill(110,"SELF"))==TacticsPolicy.Kind.DEFENSE,"Ultimate Defense classified by ID independent of translated name");
		check(classification(skill(368,"AURA"))==TacticsPolicy.Kind.DEFENSE,"Vengeance defense wins over taunt target metadata");
		Method supported=BotTactics.class.getDeclaredMethod("supportedTarget",Skill.class,boolean.class); supported.setAccessible(true);
		for (String target:new String[] {"SELF","ONE","PARTY","CLAN","PARTY_MEMBER"})
			check((boolean)supported.invoke(null,skill(20000,target),true),"self support target accepted "+target);
		for (String target:new String[] {"ONE","AREA","AURA","FRONT_AURA","BEHIND_AURA","FRONT_AREA","BEHIND_AREA"})
			check((boolean)supported.invoke(null,skill(20000,target),false),"offensive target accepted "+target);
		check((boolean)supported.invoke(null,skill(368,"AURA"),true),"Vengeance carries self defense on offensive aura metadata");
		check(!(boolean)supported.invoke(null,skill(20000,"CORPSE"),true),"corpse cannot be living self support");
	}
	private static void auditFixtures(Path path) throws Exception
	{
		Set<Integer> classes=new HashSet<>(); int rows=0, attackRows=0;
		Set<Integer> deliberatelyUnsupported=Set.of(2,821,484,1105);
		for (String row:Files.readAllLines(path))
		{
			String[] cols=row.split("\\t",-1); int classId=Integer.parseInt(cols[0]), id=Integer.parseInt(cols[1]);
			classes.add(classId); rows++;
			StatSet set=new StatSet(); set.set("skill_id",id); set.set("level",1);
			for (String attr:cols[4].split(";")) { String[] pair=attr.split("=",2); set.set(pair[0],pair[1]); }
			Skill skill=new Skill(set);
			if (!cols[5].isEmpty()) for (String token:cols[5].split(";"))
			{
				String[] pair=token.split(":",2);
				skill.addEffect(EffectScope.GENERAL,new AuditEffect(pair[0],EffectType.valueOf(pair[1])));
			}
			TacticsPolicy.Kind kind=classification(skill);
			if (cols[2].equals("R") && cols[3].equals("1") && !deliberatelyUnsupported.contains(id))
			{
				attackRows++;
				check(kind!=null,"every learned default rotation skill classified: class="+classId+" id="+id);
				Method supported=BotTactics.class.getDeclaredMethod("supportedTarget",Skill.class,boolean.class); supported.setAccessible(true);
				boolean self=kind==TacticsPolicy.Kind.BUFF || kind==TacticsPolicy.Kind.CHARGE || kind==TacticsPolicy.Kind.DEFENSE || kind==TacticsPolicy.Kind.HEAL;
				check((boolean)supported.invoke(null,skill,self),"every classified rotation target supported: class="+classId+" id="+id);
			}
			if (skill.getTargetType().name().contains("AURA"))
				check(skill.getCastRange()==-1 && skill.getAffectRange()>0,"actual aura defaults are negative cast + positive affect radius "+id);
		}
		check(classes.size()==18,"all eighteen presets audited, not representative examples");
		check(rows>200 && attackRows>120,"exhaustive actual XML skill fixture coverage");
		System.out.println("PASS: "+rows+" actual XML preset entries; "+attackRows+" learned rotation classifications across "+classes.size()+" classes");
	}
	public static void main(String[] args) throws Exception
	{
		org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT = (args.length>1 ? Path.of(args[1]) : Path.of("game")).toFile();
		actualHandlers();
		if (args.length>0) auditFixtures(Path.of(args[0]));
		check(BotTactics.tacticalApproachRange(null,null,null)==40,"null-safe tactical pursuit API");
		check(!BotTactics.allowsBasicAttack(null,null),"null fallback attack denied");
		check(BotTactics.getDefaultDifficulty().equals("strong"), "initial ranked default is strong");
		BotTactics.setDefaultDifficulty("strong");
		check(BotTactics.getDifficulty(77).equals("strong"), "missing state follows default");
		BotTactics.setDifficulty(77, "normal");
		BotTactics.setDefaultDifficulty("experienced");
		check(BotTactics.getDifficulty(77).equals("normal"), "explicit training override survives global changes");
		check(!BotTactics.isOwned(77), "configuration alone cannot claim a real player");
		BotTactics.forget(77);
		check(BotTactics.getDifficulty(77).equals("experienced"), "forget drops all per-bot settings");
		check(!BotTactics.tick(null, null, null, true), "null tick safe");
		check(!BotTactics.initializeTraining(null, null), "null registration denied");
		check(!BotTactics.tickDefense(null, null, null, false), "null defense hook safe");
		for (int id = 1; id <= 3000; id++) { BotTactics.setDifficulty(id, "strong"); }
		Field field = BotTactics.class.getDeclaredField("STATES");
		field.setAccessible(true);
		Map<?, ?> states = (Map<?, ?>) field.get(null);
		check(states.size() == 2048, "hard bounded bot state");
		check(BotTactics.getDifficulty(1).equals("experienced"), "oldest state evicted");
		Object sample = states.values().iterator().next();
		Field sleep=sample.getClass().getDeclaredField("sleepObserved"); sleep.setAccessible(true);
		check(sleep.getLong(sample)==-1,"fresh pair state starts with no sleep phase");
		sleep.setLong(sample,1234);
		int evictedId=(int)states.keySet().iterator().next(); BotTactics.forget(evictedId); BotTactics.setDifficulty(evictedId,"strong");
		check(sleep.getLong(states.get(evictedId))==-1,"teardown drops observed sleep phase, never leaks to new pair");
		for (Field f : sample.getClass().getDeclaredFields())
		{
			check(!f.getType().getName().contains("actor.Player"), "state never retains Player references");
		}
		for (int id = 1; id <= 3000; id++) { BotTactics.forget(id); }
		check(states.isEmpty(), "complete teardown");
		BotTactics.setDifficulty(77, "invalid");
		check(BotTactics.getDifficulty(77).equals("experienced"), "invalid difficulty safe");
		BotTactics.forget(77);
		System.out.println("PASS: production tactics bookkeeping, cleanup and 2048-state cap");
	}
}
