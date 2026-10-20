package custom.fakeplayersai;

import org.l2jmobius.gameserver.entity.actor.Player;

/** Compatibility entry for spawn/enchant/arena callers. The scoped25% Trance / 15% Dominator policy lives
 * in BotControlChance; it also handles Dominator's Dreaming Spirit and Fear. */
final class CardinalTrance
{
    private CardinalTrance() { }
    static void apply(Player bot) { BotControlChance.apply(bot); }
}
