import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательная проверка (13.09.2026): загрузить ключевые классы из обновлённого
 * GameServer.jar и убедиться, что линковка не нарушена (без запуска самого сервера).
 * Проверяет: Creature (патч Blink), его связи с Player, Skill, EffectScope и хендлерами.
 */
public class LinkCheck
{
	public static void main(String[] args) throws Throwable
	{
		final java.net.URL jarUrl = new java.io.File("C:/EpilogueServer/libs/GameServer.jar").toURI().toURL();
		try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]
		{
			jarUrl
		}, LinkCheck.class.getClassLoader()))
		{
			final String[] classes =
			{
				"org.l2jmobius.gameserver.entity.actor.Creature",
				"org.l2jmobius.gameserver.entity.actor.Player",
				"org.l2jmobius.gameserver.entity.actor.Npc",
				"org.l2jmobius.gameserver.entity.actor.Summon",
				"org.l2jmobius.gameserver.entity.actor.instance.GrandBoss",
				"org.l2jmobius.gameserver.entity.actor.Attackable",
				"org.l2jmobius.gameserver.mechanics.skill.Skill",
				"org.l2jmobius.gameserver.mechanics.skill.EffectScope",
				"org.l2jmobius.gameserver.mechanics.skill.SkillOperateType",
				"org.l2jmobius.gameserver.mechanics.effects.AbstractEffect",
				"org.l2jmobius.gameserver.handler.EffectHandler",
				"org.l2jmobius.gameserver.entity.actor.tasks.creature.MagicUseTask",
				"org.l2jmobius.gameserver.network.serverpackets.FlyToLocation",
				"org.l2jmobius.gameserver.geoengine.GeoEngine"
			};

			for (String name : classes)
			{
				// load + link (resolve), но НЕ инициализировать (static init не выполняется)
				Class<?> c = Class.forName(name, false, loader);
				System.out.println("OK: " + name + " (" + c.getDeclaredMethods().length + " methods)");
			}

			// Верификация байткода происходит при линковке; выше Class.forName(..., false, ...)
			// загружает класс, а getDeclaredMethods() форсирует разрешение сигнатур.
			System.out.println("LINKCHECK PASSED");
		}
	}

	private static java.net.URL jarUrl;
}
