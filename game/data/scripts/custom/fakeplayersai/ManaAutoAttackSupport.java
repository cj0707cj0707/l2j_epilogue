package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;

/** Low-MP bot bows must still shoot: the core otherwise rejects normal shots at0MP.
 * Only normal bow-shot MP is waived, never skill MP/ammo/reuse. State-owned, not persisted. */
final class ManaAutoAttackSupport
{
    private WeakReference<Player> source = new WeakReference<>(null);

    void update(Player bot, BotClassPreset preset)
    {
        if (!BotTactics.needsManaAutoAttack(bot, preset)
            || (preset.getWeaponKind() != BotClassPreset.WeaponKind.BOW
                && preset.getWeaponKind() != BotClassPreset.WeaponKind.CROSSBOW))
        {
            clear();
            return;
        }
        if (source.get() == bot) return;
        clear();
        source = new WeakReference<>(bot);
        try { bot.addStatFunc(new FuncMul(Stat.BOW_MP_CONSUME_RATE, 0x70, this, 0, null)); }
        catch (RuntimeException | Error error) { clear(); throw error; }
    }

    void clear()
    {
        Player bot = source.get();
        source.clear();
        if (bot != null) bot.removeStatsOwner(this);
    }
}