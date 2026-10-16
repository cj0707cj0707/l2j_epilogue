package org.l2jmobius.gameserver.mechanics.skill;

import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import java.util.Queue;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;

/** Called BEFORE the core can replace/stop an existing BuffInfo. No timer state of our own. */
public final class DebuffCategoryGuard
{
    private DebuffCategoryGuard() { }

    // Core level-up/passive paths already hold Creature. ALWAYS acquire that
    // monitor before EffectList, including removal callbacks that touch stats.
    private static void locked(EffectList list, Creature owner, Runnable work)
    {
        synchronized (owner != null ? owner : list)
        {
            synchronized (list) { work.run(); }
        }
    }
    public static void add(EffectList list, Creature owner, BuffInfo info)
    { locked(list, owner, () -> { if (!blocked(list, info)) list.guardOriginalAdd(info); }); }
    public static void stopAndRemove(EffectList list, Creature owner, BuffInfo info)
    { locked(list, owner, () -> list.guardOriginalStopAndRemove(info)); }
    public static void stopAndRemove(EffectList list, Creature owner, boolean broadcast, BuffInfo info, Queue<BuffInfo> queue)
    { locked(list, owner, () -> list.guardOriginalStopAndRemove(broadcast, info, queue)); }
    public static void stopAndRemove(EffectList list, Creature owner, boolean broadcast, SkillFinishType type, BuffInfo info, Queue<BuffInfo> queue)
    { locked(list, owner, () -> list.guardOriginalStopAndRemove(broadcast, type, info, queue)); }
    public static void stopAllEffects(EffectList list, Creature owner)
    { locked(list, owner, () -> list.guardOriginalStopAllEffects()); }
    public static void stopAllBuffs(EffectList list, Creature owner, boolean update)
    { locked(list, owner, () -> list.guardOriginalStopAllBuffs(update)); }
    public static void stopAllDances(EffectList list, Creature owner, boolean update)
    { locked(list, owner, () -> list.guardOriginalStopAllDances(update)); }
    public static void stopAllToggles(EffectList list, Creature owner)
    { locked(list, owner, () -> list.guardOriginalStopAllToggles()); }
    public static void stopAllToggles(EffectList list, Creature owner, boolean update)
    { locked(list, owner, () -> list.guardOriginalStopAllToggles(update)); }
    public static void stopAllDebuffs(EffectList list, Creature owner, boolean update)
    { locked(list, owner, () -> list.guardOriginalStopAllDebuffs(update)); }

    public static boolean blocked(EffectList list, BuffInfo incoming)
    {
        if (incoming == null || !harmful(incoming)) return false;
        for (BuffInfo active : list.getEffects())
        {
            if (active != null && active.isInUse() && !active.isRemoved() && harmful(active)
                && conflicts(active, incoming)) return true;
        }
        return false;
    }

    static boolean harmful(BuffInfo info)
    {
        Skill skill = info.getSkill();
        return skill != null && !skill.isPassive() && (skill.isDebuff() || skill.hasNegativeEffect());
    }

    static boolean conflicts(BuffInfo active, BuffInfo incoming)
    {
        Skill old = active.getSkill(), next = incoming.getSkill();
        if (old.getId() == next.getId()) return true; // same skill, caster/enchant/level never bypass the rule
        long a = controls(active), b = controls(incoming);
        if ((a & b) != 0) return true;
        AbnormalType slot = old.getAbnormalType();
        // NONE is not one giant category. Other non-control debuffs use their actual core slot.
        return !slot.isNone() && slot == next.getAbnormalType();
    }

    static long controls(BuffInfo info)
    {
        int flags = 0;
        long result = 0;
        for (AbstractEffect effect : info.getEffects())
        {
            if (effect == null || effect.isInstant()) continue;
            flags |= effect.getEffectFlags();
            EffectType type = effect.getEffectType();
            if (type == EffectType.STUN) result |= 1;
            else if (type == EffectType.FEAR) result |= 2;
            else if (type == EffectType.ROOT) result |= 4;
            else if (type == EffectType.SLEEP) result |= 8;
            else if (type == EffectType.MUTE) result |= 16;
            else if (type == EffectType.PARALYZE) result |= 64;
        }
        if ((flags & EffectFlag.STUNNED.getMask()) != 0) result |= 1;
        if ((flags & EffectFlag.FEAR.getMask()) != 0) result |= 2;
        if ((flags & EffectFlag.ROOTED.getMask()) != 0) result |= 4;
        if ((flags & EffectFlag.SLEEP.getMask()) != 0) result |= 8;
        if ((flags & (EffectFlag.MUTED.getMask() | EffectFlag.PSYCHICAL_MUTED.getMask())) != 0) result |= 16;
        if ((flags & EffectFlag.DISARMED.getMask()) != 0) result |= 32;
        if ((flags & EffectFlag.PARALYZED.getMask()) != 0) result |= 64;
        if ((flags & EffectFlag.CONFUSED.getMask()) != 0) result |= 128;
        if ((flags & EffectFlag.PSYCHICAL_ATTACK_MUTED.getMask()) != 0) result |= 256;
        if ((flags & EffectFlag.BETRAYED.getMask()) != 0) result |= 512;
        return result;
    }
}
