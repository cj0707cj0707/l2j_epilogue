package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;

/** Private chat notice on arena arrival, once per recipient/native game identity.
 * Does not select targets, change packets/UI flags or open the combat gate. */
final class OlympiadOpponentNotice
{
    private static final Logger LOGGER = Logger.getLogger(OlympiadOpponentNotice.class.getName());
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final Map<Player, WeakReference<Object>> SENT = new WeakHashMap<>();
    private OlympiadOpponentNotice() { }

    static void init()
    {
        if (!INITIALIZED.compareAndSet(false, true)) return;
        Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_TELEPORTED,
            (OnCreatureTeleported e) -> { if (e.getCreature() instanceof Player p) onTeleported(p); }, OlympiadOpponentNotice.class));
    }

    static void onTeleported(Player player)
    {
        if (!arrivalCandidate(player) || announce(player)) return;
        // Native port sets Olympiad mode after teleToLocation; the event can win that race.
        // Participants finish independently. Bounded retries, no persistent ticker.
        for (long delay : new long[] {250, 1000, 2000}) ThreadPool.schedule(() -> announce(player), delay);
    }

    static boolean arrivalCandidate(Player p)
    {
        return p != null && p.getClient() != null && p.isOnline() && !p.inObserverMode() && p.getOlympiadGameId() >= 0;
    }

    private static boolean recipient(Player p)
    {
        return arrivalCandidate(p) && p.isInOlympiadMode() && !p.isTeleporting();
    }

    static boolean announce(Player player)
    {
        if (!recipient(player)) return false;
        try
        {
            final Olympiad olympiad = Olympiad.getInstance();
            final int id = player.getOlympiadGameId();
            final Object game = olympiad.getOlympiadGames().get(id);
            final Player[] pair = olympiad.getPlayers(id);
            if (game == null || olympiad.getOlympiadGames().get(id) != game) return false;
            return announce(player, game, pair);
        }
        catch (RuntimeException e)
        {
            LOGGER.log(Level.WARNING, "FakePlayersAI: Olympiad opponent notice unavailable", e);
            return false;
        }
    }

    static synchronized boolean announce(Player player, Object game, Player[] pair)
    {
        if (!recipient(player) || game == null || pair == null || pair.length != 2 || pair[0] == pair[1]) return false;
        final Player enemy = pair[0] == player ? pair[1] : pair[1] == player ? pair[0] : null;
        if (enemy == null || !enemy.isOnline() || !enemy.isInOlympiadMode() || enemy.isTeleporting()
            || enemy.getOlympiadGameId() != player.getOlympiadGameId() || enemy.getInstanceId() != player.getInstanceId()
            || enemy.getName() == null || enemy.getName().isBlank()) return false;
        final int[] center = BotManager.arenaCenter(player);
        if (center == null || Math.hypot((double)player.getX() - center[0], (double)player.getY() - center[1]) > 2000
            || Math.hypot((double)enemy.getX() - center[0], (double)enemy.getY() - center[1]) > 2000) return false;
        final WeakReference<Object> previous = SENT.get(player);
        if (previous != null && previous.get() == game) return true;
        player.sendMessage("Олимпиада: ваш противник — " + enemy.getName() + ".");
        SENT.put(player, new WeakReference<>(game));
        return true;
    }
}
