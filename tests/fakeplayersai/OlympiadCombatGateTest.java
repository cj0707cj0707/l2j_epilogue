package org.l2jmobius.gameserver.mechanics.olympiad;

import custom.fakeplayersai.OlympiadCombatGate;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/** Pure API doubles, intentionally package-private game/stadium like the real JAR. */
public class OlympiadCombatGateTest
{
	private static int checks;
	private static Player bot, enemy;
	private static OlympiadGame game;
	private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
	private static void reset()
	{
		bot = new Player(1); enemy = new Player(2); game = new OlympiadGame();
		game._playerOne = bot; game._playerTwo = enemy;
		Olympiad.getInstance().games.clear(); Olympiad.getInstance().games.put(0, game);
		DoorData.INSTANCE.doors.clear(); DoorData.INSTANCE.doors.put(1, new Door()); DoorData.INSTANCE.doors.put(2, new Door());
		OlympiadManager.STADIUMS[0].ids = new int[]{1,2};
		OlympiadCombatGate.forget(1); OlympiadCombatGate.forget(2);
	}
	public static void main(String[] args) throws Exception
	{
		for (int flags=0; flags<256; flags++)
		{
			reset();
			bot.mode=(flags&1)!=0; enemy.mode=(flags&2)!=0;
			bot.online=(flags&4)!=0; enemy.online=(flags&8)!=0;
			bot.start=(flags&16)!=0; enemy.start=(flags&32)!=0;
			DoorData.INSTANCE.doors.get(1).open=(flags&64)!=0; DoorData.INSTANCE.doors.get(2).open=(flags&128)!=0;
			check(OlympiadCombatGate.ready(bot)==(flags==255), "both-participant/door matrix " + flags);
		}
		reset(); check(OlympiadCombatGate.ready(bot), "valid official match without BotManager registration");
		check(!OlympiadCombatGate.ready(null), "null bot");
		check(!OlympiadCombatGate.ready(new Player(1)), "object ID spoof not participant identity");
		game.aborted=true; check(!OlympiadCombatGate.ready(bot), "aborted"); game.aborted=false;
		game._startTime=0; check(!OlympiadCombatGate.ready(bot), "no official start attempt"); game._startTime=1;
		bot.dead=true; check(!OlympiadCombatGate.ready(bot), "bot dead"); bot.dead=false;
		enemy.dead=true; check(!OlympiadCombatGate.ready(bot), "enemy dead"); enemy.dead=false;
		bot.teleport=true; check(!OlympiadCombatGate.ready(bot), "bot teleport"); bot.teleport=false;
		enemy.teleport=true; check(!OlympiadCombatGate.ready(bot), "enemy teleport"); enemy.teleport=false;
		enemy.instance=1; check(!OlympiadCombatGate.ready(bot), "instance mismatch"); enemy.instance=0;
		enemy.game=1; check(!OlympiadCombatGate.ready(bot), "game mismatch"); enemy.game=0;
		game._playerTwo=bot; check(!OlympiadCombatGate.ready(bot), "duplicate participant"); game._playerTwo=enemy;
		DoorData.INSTANCE.doors.remove(2); check(!OlympiadCombatGate.ready(bot), "missing second door");
		OlympiadManager.STADIUMS[0].ids=new int[]{1,1}; check(!OlympiadCombatGate.ready(bot), "duplicate door ID");
		OlympiadManager.STADIUMS[0].ids=new int[0]; check(!OlympiadCombatGate.ready(bot), "malformed door IDs");
		Olympiad.getInstance().games.clear(); check(!OlympiadCombatGate.ready(bot), "game removed");
		reset(); bot.start=false; enemy.start=false; game._startTime=0;
		check(OlympiadCombatGate.claimPreStart(bot), "initial prestart claim");
		check(!OlympiadCombatGate.claimPreStart(bot), "exactly once");
		check(OlympiadCombatGate.claimPreStart(enemy), "other participant independent claim");
		OlympiadCombatGate.hold(bot); check(!OlympiadCombatGate.claimPreStart(bot), "hold does not forget claim");
		OlympiadGame replacement=new OlympiadGame(); replacement._playerOne=bot; replacement._playerTwo=enemy; replacement._startTime=0;
		Olympiad.getInstance().games.put(0,replacement); game=replacement;
		check(OlympiadCombatGate.claimPreStart(bot), "same arena and IDs, NEW actual game object");
		OlympiadCombatGate.forget(1); enemy.start=true;
		check(!OlympiadCombatGate.claimPreStart(bot), "partial start transition cannot claim"); enemy.start=false;
		game._startTime=1; check(!OlympiadCombatGate.claimPreStart(bot), "ended/start-attempted match cannot claim even flags reset");
		reset(); bot.start=false; enemy.start=false; game._startTime=0;
		var successes=new java.util.concurrent.atomic.AtomicInteger(); var threads=new java.util.ArrayList<Thread>();
		for(int i=0;i<32;i++){var t=new Thread(()->{if(OlympiadCombatGate.claimPreStart(bot))successes.incrementAndGet();});threads.add(t);t.start();}
		for(var t:threads)t.join(); check(successes.get()==1,"concurrent claims atomic");
		bot.summon=new Summon(); bot.throwAbort=true;
		AutoPlayTaskManager.running=true; AutoUseTaskManager.running=true;
		OlympiadCombatGate.hold(bot);
		check(!AutoPlayTaskManager.running&&!AutoUseTaskManager.running,"both engines stopped");
		check(!bot.attack&&!bot.cast&&!bot.moving&&!bot.queued&&bot.target==null&&bot.ai.idle&&!bot.ai.saved,"all player actions held despite independent abort failure");
		check(!bot.summon.follow&&!bot.summon.attack&&!bot.summon.cast&&!bot.summon.moving&&bot.summon.target==null&&bot.summon.ai.idle&&!bot.summon.ai.saved,"servitor held, follow off");
		OlympiadCombatGate.hold(null); check(true,"null hold safe");
		System.out.println("PASS: " + checks + " isolated gate matrix/lifecycle/hold checks; not a live match");
	}
}
