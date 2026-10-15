import java.lang.reflect.Modifier;

/** Load without server initialization; verify actual JAR types, visibility and signatures. */
public class OlympiadCombatGateDescriptorTest
{
	private static int checks;
	private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
	public static void main(String[] args) throws Exception
	{
		String base = "org.l2jmobius.gameserver.";
		ClassLoader loader = OlympiadCombatGateDescriptorTest.class.getClassLoader();
		Class<?> game = Class.forName(base + "mechanics.olympiad.OlympiadGame", false, loader);
		Class<?> stadium = Class.forName(base + "mechanics.olympiad.OlympiadStadium", false, loader);
		Class<?> manager = Class.forName(base + "mechanics.olympiad.OlympiadManager", false, loader);
		Class<?> player = Class.forName(base + "entity.actor.Player", false, loader);
		check(!Modifier.isPublic(game.getModifiers()), "actual game is package private");
		check(!Modifier.isPublic(stadium.getModifiers()), "actual stadium is package private");
		for (String name : new String[]{"_playerOne", "_playerTwo"}) {
			var field = game.getDeclaredField(name); field.setAccessible(true); check(field.getType() == player, name);
		}
		var start = game.getDeclaredField("_startTime"); start.setAccessible(true); check(start.getType() == long.class, "startTime long");
		var id = game.getDeclaredField("_stadiumID"); id.setAccessible(true); check(id.getType() == int.class, "stadium ID int");
		var aborted = game.getDeclaredMethod("isAborted"); aborted.setAccessible(true); check(aborted.getReturnType() == boolean.class, "aborted boolean");
		var stadiums = manager.getDeclaredField("STADIUMS"); stadiums.setAccessible(true);
		check(Modifier.isStatic(stadiums.getModifiers()) && stadiums.getType().getComponentType() == stadium, "stadium array");
		var doors = stadium.getDeclaredMethod("getDoorID"); doors.setAccessible(true); check(doors.getReturnType() == int[].class, "two door ID accessor");
		Class<?> door = Class.forName(base + "entity.actor.instance.Door", false, loader);
		check(door.getMethod("isOpen").getReturnType() == boolean.class, "Door.isOpen");
		Class<?> data = Class.forName(base + "data.xml.DoorData", false, loader);
		check(data.getMethod("getDoor", int.class).getReturnType() == door, "DoorData.getDoor");
		Class<?> skill = Class.forName(base + "mechanics.skill.Skill", false, loader);
		check(player.getMethod("setQueuedSkill", skill, boolean.class, boolean.class).getReturnType() == void.class, "clear queued skill signature");
		Class<?> ai = Class.forName(base + "ai.CreatureAI", false, loader);
		check(ai.getMethod("notifyActionCancel").getReturnType() == void.class, "clear saved intentions");
		Class<?> summon = Class.forName(base + "entity.actor.Summon", false, loader);
		check(summon.getMethod("setFollowStatus", boolean.class).getReturnType() == void.class, "summon follow status");
		System.out.println("PASS: " + checks + " actual-jar descriptor checks (no server initialization)");
	}
}
