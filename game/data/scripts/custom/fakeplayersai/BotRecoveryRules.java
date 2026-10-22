package custom.fakeplayersai;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/** Shared selection + synchronous core cast guard; no player skill/XML edits. */
public final class BotRecoveryRules
{
    private static boolean initialized;
    private static Skill sacrifice;
    private BotRecoveryRules() { }

    public static synchronized void init()
    {
        if (initialized) return;
        Containers.Global().addListener(new FunctionEventListener(Containers.Global(), EventType.ON_CREATURE_SKILL_USE,
            (OnCreatureSkillUse e) -> guard(e), BotRecoveryRules.class));
        Containers.Global().addListener(new FunctionEventListener(Containers.Global(), EventType.ON_CREATURE_ATTACK,
            (OnCreatureAttack e) -> bluffAttack(e), BotRecoveryRules.class));
        initialized = true;
    }

    private static boolean managed(Creature actor)
    {
        return actor != null && actor.isPlayer() && actor.asPlayer().getClient() == null
            && (BotManager.isBot(actor.asPlayer()) || TrainingArena.isTrainingBot(actor.asPlayer()));
    }

    private static boolean arenaManaged(Creature actor)
    {
        return managed(actor) && (actor.asPlayer().isInOlympiadMode() && actor.asPlayer().isOlympiadStart()
            || TrainingArena.isTrainingBot(actor.asPlayer()));
    }

    /** Recheck facing at native blow execution: the victim may turn during a cast. */
    public static boolean blockedBluffStrike(Creature effector, Creature effected, Skill skill)
    {
        return arenaManaged(effector) && effected != null && effected.isPlayer() && skill != null
            && !BluffRear.skillAllowed(effector.asPlayer(), effected.asPlayer(), skill);
    }

    /** Also used at effect execution, when invulnerability may have proc'd mid-cast. */
    public static boolean blockedSelfHeal(Creature effector, Creature effected)
    {
        return effector == effected && managed(effected) && effected.isInvul();
    }

    /** Recheck native icon effects at Heal execution as well as cast admission. */
    public static boolean blockedSacrifice(Creature effector, Creature effected, Skill skill)
    {
        return skill != null && skill.getId() == 69 && effector == effected && managed(effected)
            && TankClassMechanics.sacrificeBlocked(effected.asPlayer());
    }

    public static boolean healing(Skill skill)
    {
        if (skill == null) return false;
        if (skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL)) return true;
        for (EffectScope scope : EffectScope.values())
        {
            final var effects = skill.getEffects(scope);
            if (effects != null) for (var effect : effects)
                if (switch (effect.getName())
                {
                    case "Heal", "HealPercent", "HealOverTime", "CpHeal", "CpHealPercent", "CpHealOverTime", "HpByLevel", "RebalanceHP" -> true;
                    default -> false;
                }) return true;
        }
        return false;
    }

    public static boolean selfHealAllowed(Player bot, Skill skill)
    {
        if (!healing(skill)) return true;
        if (bot.isInvul() || skill.getId() == 69 && TankClassMechanics.sacrificeBlocked(bot)) return false;
        return TacticsPolicy.recoveryHealAllowed(bot.getPlayerClass().getId(), skill.getId(),
            bot.getCurrentHp() / Math.max(1, bot.getMaxHp()));
    }

    static TerminateReturn bluffAttack(OnCreatureAttack event)
    {
        return event!=null&&arenaManaged(event.getAttacker())&&event.getTarget()!=null&&event.getTarget().isPlayer()
            &&(RoleCombatPolicy.rangedDominator(event.getAttacker().asPlayer(),event.getTarget().asPlayer()) || event.getTarget().isInvul() || BluffRear.stun(event.getAttacker().asPlayer(),event.getTarget().asPlayer())!=null
                ||event.getAttacker().asPlayer().getPlayerClass().getId()==97&&event.getTarget().asPlayer().isSleeping()) ? new TerminateReturn(true,true,true):null;
    }

    static TerminateReturn guard(OnCreatureSkillUse event)
    {
        if (event == null || !managed(event.getCaster()) || event.getSkill()==null) return null;
        final Player actor=event.getCaster().asPlayer();
        if (!CardinalSurvival.skillAllowed(actor,event.getSkill().getId())) return new TerminateReturn(true,true,true);
        Player victim=event.getTarget()!=null&&event.getTarget().isPlayer()?event.getTarget().asPlayer():null;
        if (event.getSkill().getId()==1230 && actor.getPlayerClass().getId()==97 && arenaManaged(actor) && !CardinalSurvival.prominenceAllowed(actor,victim)) return new TerminateReturn(true,true,true);
        if(arenaManaged(actor))
        {
            Player enemy=BotTactics.currentOpponent(actor);
            if(!TankClassMechanics.castAllowed(actor,enemy,event.getSkill())
                ||!BotTactics.dominatorRootAllowed(actor,enemy,event.getSkill().getId())
                ||!BotSpawner.tankMirrorDefenseAllowed(actor,enemy,event.getSkill())
                ||!RoleCombatPolicy.candidate(actor,enemy,event.getSkill(),victim==actor)
                ||TacticsPolicy.chargeMeleeClass(actor.getPlayerClass().getId())&&RoleCombatPolicy.builder(event.getSkill().getId())&&!RoleCombatPolicy.needsCharge(actor))return new TerminateReturn(true,true,true);
            if(victim!=null&&victim!=actor&&victim.isInvul())return new TerminateReturn(true,true,true);
            if(event.getTargets()!=null)for(var target:event.getTargets())
                if(target!=null&&target!=actor&&target.isCreature()&&target.asCreature().isInvul())return new TerminateReturn(true,true,true);
        }
        if (victim!=null && victim!=actor && actor.getPlayerClass().getId()==97 && arenaManaged(actor) && victim.isSleeping() && !SleepRetreat.wakeSkillAllowed(actor,victim,event.getSkill())) return new TerminateReturn(true,true,true);
        if (victim!=null&&blockedBluffStrike(actor,victim,event.getSkill())) return new TerminateReturn(true,true,true);
        if (!healing(event.getSkill())) return null;
        final Player bot = event.getCaster().asPlayer();
        boolean includesSelf = event.getTarget() == bot;
        if (event.getTargets() != null) includesSelf |= event.getTargets().contains(bot);
        // The bot-only Sacrifice must never heal the opponent via a restored enemy target.
        if (bot.getPlayerClass().getId() == 90 && event.getSkill().getId() == 69 && event.getTarget() != bot)
            return new TerminateReturn(true, true, true);
        return includesSelf && !selfHealAllowed(bot, event.getSkill()) ? new TerminateReturn(true, true, true) : null;
    }

    /** Independent +30 Power template, with ONLY the requested bot reuse override.
     * Copy this exact simple skill's native properties/effects; never mutate SkillData. */
    public static synchronized void applyPhoenixSacrifice(Player bot)
    {
        if (bot == null || bot.getClient() != null || bot.getPlayerClass().getId() != 90 || bot.getKnownSkill(69) == null) return;
        if (sacrifice == null)
        {
            final Skill nativeSkill = SkillData.getInstance().getSkill(69, 130);
            if (nativeSkill == null)
            {
                java.util.logging.Logger.getLogger(BotRecoveryRules.class.getName()).warning("Phoenix Sacrifice +30 Power template missing; existing skill retained");
                return;
            }
            StatSet set = new StatSet();
            set.set("skill_id", 69); set.set("level", nativeSkill.getLevel()); set.set("name", nativeSkill.getName());
            set.set("icon", nativeSkill.getIcon()); set.set("operateType", "A1"); set.set("targetType", "ONE");
            set.set("castRange", nativeSkill.getCastRange()); set.set("effectRange", nativeSkill.getEffectRange());
            set.set("effectPoint", nativeSkill.getEffectPoint()); set.set("hitTime", nativeSkill.getHitTime());
            set.set("hpConsume", nativeSkill.getHpConsume()); set.set("isMagic", 1); set.set("magicLevel", nativeSkill.getMagicLevel());
            set.set("reuseDelay", 0);
            Skill copy = new Skill(set);
            for (EffectScope scope : EffectScope.values())
            {
                final var effects = nativeSkill.getEffects(scope);
                if (effects != null) for (var effect : effects) copy.addEffect(scope, effect);
            }
            sacrifice = copy;
        }
        if (bot.getKnownSkill(69) != sacrifice) bot.addSkill(sacrifice, false);
    }
}