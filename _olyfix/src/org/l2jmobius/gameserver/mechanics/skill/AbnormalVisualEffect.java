package org.l2jmobius.gameserver.mechanics.skill;

public enum AbnormalVisualEffect {
   NONE(0, 0),
   DOT_BLEEDING(1, 0),
   DOT_POISON(2, 0),
   DOT_FIRE(4, 0),
   DOT_WATER(8, 0),
   DOT_WIND(16, 0),
   DOT_SOIL(32, 0),
   STUN(64, 0),
   SLEEP(128, 0),
   SILENCE(256, 0),
   ROOT(512, 0),
   PARALYZE(1024, 0),
   FLESH_STONE(2048, 0),
   DOT_MP(4096, 0),
   BIG_HEAD(8192, 0),
   DOT_FIRE_AREA(16384, 0),
   CHANGE_TEXTURE(32768, 0),
   BIG_BODY(65536, 0),
   FLOATING_ROOT(131072, 0),
   DANCE_ROOT(262144, 0),
   GHOST_STUN(524288, 0),
   STEALTH(1048576, 0),
   SEIZURE1(2097152, 0),
   SEIZURE2(4194304, 0),
   MAGIC_SQUARE(8388608, 0),
   FREEZING(16777216, 0),
   SHAKE(33554432, 0),
   BLIND(67108864, 0),
   ULTIMATE_DEFENCE(134217728, 0),
   VP_UP(268435456, 0),
   REAL_TARGET(536870912, 0),
   DEATH_MARK(1073741824, 0),
   TURN_FLEE(Integer.MIN_VALUE, 0),
   VP_KEEP(268435456, 0),
   INVINCIBILITY(1, 1),
   AIR_BATTLE_SLOW(2, 1),
   AIR_BATTLE_ROOT(4, 1),
   CHANGE_WP(8, 1),
   CHANGE_HAIR_G(16, 1),
   CHANGE_HAIR_P(32, 1),
   CHANGE_HAIR_B(64, 1),
   STIGMA_OF_SILEN(256, 1),
   SPEED_DOWN(512, 1),
   FROZEN_PILLAR(1024, 1),
   CHANGE_VES_S(2048, 1),
   CHANGE_VES_C(4096, 1),
   CHANGE_VES_D(8192, 1),
   TIME_BOMB(16384, 1),
   MP_SHIELD(32768, 1),
   NAVIT_ADVENT(524288, 1),
   BR_NONE(0, 2),
   BR_AFRO_NORMAL(1, 2),
   BR_AFRO_PINK(2, 2),
   BR_AFRO_GOLD(4, 2),
   BR_POWER_OF_EVA(8, 2),
   BR_HEADPHONE(16, 2),
   BR_VESPER1(32, 2),
   BR_VESPER2(64, 2),
   BR_VESPER3(128, 2),
   BR_SOUL_AVATAR(256, 2);

   private final int _mask;
   private final int _type;

   private AbnormalVisualEffect(int mask, int type) {
      this._mask = mask;
      this._type = type;
   }

   public int getMask() {
      return this._mask;
   }

   public boolean isSpecial() {
      return this._type == 1;
   }

   public boolean isEvent() {
      return this._type == 2;
   }
}
