package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;

/** One active, authority-validated arena pair. This is not a global Mana Storm XML change.
 * Owned by BotTactics.State; every pair/end/eviction path must call clear(). */
final class CardinalArenaSupport
{
    private static final Logger LOGGER = Logger.getLogger(CardinalArenaSupport.class.getName());
    private WeakReference<Player> source = new WeakReference<>(null);
    private WeakReference<Player> opponent = new WeakReference<>(null);

    void bind(Player bot, Player enemy)
    {
        if (bot == null || enemy == null || bot == enemy || bot.getClient() != null
            || bot.getPlayerClass().getId() != 97)
        {
            clear();
            return;
        }
        if (source.get() == bot && opponent.get() == enemy) return;
        clear();
        source = new WeakReference<>(bot);
        opponent = new WeakReference<>(enemy);
        try
        {
            // Final arena regeneration-stat multiplier: leave exactly10%, do not set current MP.
            enemy.addStatFunc(new FuncMul(Stat.REGENERATE_MP_RATE, 0x70, this, 0.10, null));
            LOGGER.info("FakePlayersAI: Cardinal arena regMp x0.10: " + bot.getName() + " -> " + enemy.getName());
        }
        catch (RuntimeException | Error error)
        {
            clear();
            throw error;
        }
    }

    boolean active() { return opponent.get() != null; }

    void clear()
    {
        Player previous = opponent.get();
        source.clear();
        opponent.clear();
        if (previous != null)
        {
            previous.removeStatsOwner(this);
            LOGGER.info("FakePlayersAI: Cardinal arena regMp released: " + previous.getName());
        }
    }
}
