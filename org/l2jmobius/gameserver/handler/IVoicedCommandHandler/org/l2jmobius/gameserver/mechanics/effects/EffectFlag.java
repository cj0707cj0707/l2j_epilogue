package org.l2jmobius.gameserver.mechanics.effects;

public enum EffectFlag {
   NONE,
   RESURRECTION_SPECIAL,
   NOBLESS_BLESSING,
   SILENT_MOVE,
   PROTECTION_BLESSING,
   RELAXING,
   FEAR,
   CONFUSED,
   MUTED,
   PSYCHICAL_MUTED,
   PSYCHICAL_ATTACK_MUTED,
   PASSIVE,
   DISARMED,
   ROOTED,
   SLEEP,
   STUNNED,
   BETRAYED,
   INVUL,
   PARALYZED,
   BLOCK_RESURRECTION,
   SERVITOR_SHARE,
   POLEARM_SINGLE_TARGET;

   public int getMask() {
      return 1 << this.ordinal();
   }
}
