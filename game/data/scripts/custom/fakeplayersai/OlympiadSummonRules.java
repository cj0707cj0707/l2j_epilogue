package custom.fakeplayersai;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** One fresh class servitor per real Olympiad game, never a combat-tick resurrection.
 * The native Summon effect must use allowSummon: direct applyEffects bypasses cast events. */
public final class OlympiadSummonRules
{
    private static final Logger LOGGER = Logger.getLogger(OlympiadSummonRules.class.getName());
    private record Permit(Player owner, int skillId) { }
    private static final ThreadLocal<Permit> PERMIT = new ThreadLocal<>();

    private OlympiadSummonRules() { }

    private static boolean restricted(Player player)
    {
        return player != null && player.getClient() == null && BotManager.isBot(player)
            && (player.isInOlympiadMode() || player.getOlympiadGameId() >= 0);
    }

    /** A permit is bound to the actor, skill and synchronous preparation thread, and consumed
     * before any native construction/spawn. Failure is closed: no retries during that game. */
    public static boolean allowSummon(Player player, Skill skill)
    {
        if (!restricted(player)) return true; // Humans, training and ordinary world unchanged.
        final Permit permit = PERMIT.get();
        if (permit == null || permit.owner != player || skill == null || permit.skillId != skill.getId()) return false;
        PERMIT.remove();
        return true;
    }

    private static void withPermit(Player player, Skill skill, Runnable action)
    {
        if (PERMIT.get() != null) throw new IllegalStateException("Nested Olympiad summon preparation");
        PERMIT.set(new Permit(player, skill.getId()));
        try { action.run(); }
        finally { PERMIT.remove(); }
    }

    /** Validated pre-start only, once per actual game object (stadium IDs are reused).
     * Repeated callbacks and reconnects must not replace a killed/dismissed servitor. */
    public static void prepare(Player player, BotClassPreset preset)
    {
        if (!restricted(player) || preset == null || !player.isOnline() || player.isAlikeDead()
            || player.getPlayerClass().getId() != preset.getClassId()) return;
        final int skillId = BotSpawner.getOlympiadSummonSkillId(preset);
        if (skillId == 0 || !OlympiadCombatGate.claimSummonPreparation(player)) return;
        synchronized (player)
        {
            try
            {
                // Dispose of previous match's live/dead servitor instead of carrying damage/effects over.
                final Summon previous = player.getSummon();
                if (previous != null)
                {
                    if (previous.getOwner() != player)
                    {
                        LOGGER.warning("FakePlayersAI: foreign summon owner; preparation refused for " + player.getName());
                        return;
                    }
                    previous.unSummon(player);
                    if (player.getSummon() != null)
                    {
                        LOGGER.warning("FakePlayersAI: old summon did not detach; no replacement for " + player.getName());
                        return;
                    }
                }
                final Skill skill = player.getKnownSkill(skillId);
                if (skill == null)
                {
                    LOGGER.warning("FakePlayersAI: missing Olympiad summon skill " + skillId + " for " + player.getName());
                    return;
                }
                withPermit(player, skill, () -> skill.applyEffects(player, player));
                final Summon fresh = player.getSummon();
                if (fresh != null && fresh != previous && fresh.getOwner() == player && !fresh.isAlikeDead())
                {
                    // Native handler also fullRestores BEFORE spawn. Only pre-combat, never a heal tick.
                    fresh.fullRestore();
                    LOGGER.info("FakePlayersAI: one fresh full-HP Olympiad summon prepared for " + player.getName());
                }
                else LOGGER.warning("FakePlayersAI: Olympiad summon failed; no combat retry for " + player.getName());
            }
            catch (RuntimeException | LinkageError failure)
            {
                // Keep other pre-start buffs/openers working; the spent claim is NOT reset.
                LOGGER.log(java.util.logging.Level.WARNING, "FakePlayersAI: Olympiad summon preparation failed for " + player.getName() + "; no combat retry", failure);
            }
        }
    }
}
