/* Deterministic ordering model of core CAST approach / AutoUse / AutoPlay.
 * No live GameServer, threads, client packets or geodata are tested here.
 * Uses the unchanged public update signature, so it can reproduce the old bug too.
 */
package custom.fakeplayersai;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;

public class PursuitOwnershipRegression
{
    private static void autoUseTick(Player bot, Player enemy)
    {
        bot.itemTicks++; // Items are processed outside the offensive rotation gate.
        if (bot.isAutoPlaying() && !bot.isCastingNow())
        {
            bot.rotationTicks++;
            // Core useMagic(..., true, false) -> thinkCast -> maybeMoveToPawn:
            // no cast yet, but intention stays CAST and follows CURRENT coordinates.
            bot.ai.intention = Intention.CAST;
            bot.moving = true;
            bot.dx = enemy.x;
            bot.dy = enemy.y;
        }
    }

    public static void main(String[] args) throws Exception
    {
        boolean expectLegacy = args.length > 0 && args[0].equals("--expect-legacy");
        Player bot = new Player(71, 0, 0), enemy = new Player(72, 800, 0);
        bot.target = enemy;
        enemy.moving = true;
        enemy.dx = 800;
        enemy.dy = 1800;
        enemy.speed = 200;
        var tasks = AutoPlayTaskManager.getInstance();
        tasks.startAutoPlay(bot);
        for (int tick = 0; tick < 5; tick++)
        {
            autoUseTick(bot, enemy);
            OlympiadChase.update(bot, enemy, 150, true, 0, 0, 2000);
            tasks.retargetTick(bot);
            autoUseTick(bot, enemy);
        }
        if (expectLegacy)
        {
            if (bot.ai.moves != 0 || !bot.isAutoPlaying()) throw new AssertionError("Legacy bug not reproduced");
            System.out.println("REPRODUCED baseline: 0 intercept moves; rotation keeps replacing CAST approach.");
            return;
        }
        if (bot.ai.moves != 1 || bot.ai.intention != Intention.MOVE_TO || bot.dy <= enemy.y)
            throw new AssertionError("Intercept lost to auto rotation/retarget");
        if (bot.rotationTicks != 1 || bot.itemTicks != 10 || bot.isAutoPlaying())
            throw new AssertionError("Rotation must pause but item loop must remain available");
        enemy.x = 100;
        enemy.y = 0;
        OlympiadChase.update(bot, enemy, 150, true, 0, 0, 2000);
        if (!bot.isAutoPlaying() || bot.ai.intention != Intention.ATTACK)
            throw new AssertionError("Fight not resumed in range");
        autoUseTick(bot, enemy);
        if (bot.rotationTicks != 2) throw new AssertionError("Offensive rotation not restored");
        System.out.println("PASS: CAST-approach/rotation/retarget regression; intercept retained, items available, combat resumed. API model only.");
    }
}
