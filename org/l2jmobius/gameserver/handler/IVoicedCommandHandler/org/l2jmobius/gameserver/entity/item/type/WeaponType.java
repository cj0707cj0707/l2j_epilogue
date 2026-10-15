package org.l2jmobius.gameserver.entity.item.type;

import org.l2jmobius.gameserver.mechanics.stats.TraitType;

public enum WeaponType implements ItemType {
   SWORD(TraitType.SWORD),
   BLUNT(TraitType.BLUNT),
   DAGGER(TraitType.DAGGER),
   BOW(TraitType.BOW),
   POLE(TraitType.POLE),
   NONE(TraitType.NONE),
   DUAL(TraitType.DUAL),
   ETC(TraitType.ETC),
   FIST(TraitType.FIST),
   DUALFIST(TraitType.DUALFIST),
   FISHINGROD(TraitType.NONE),
   RAPIER(TraitType.RAPIER),
   ANCIENTSWORD(TraitType.ANCIENTSWORD),
   CROSSBOW(TraitType.CROSSBOW),
   FLAG(TraitType.NONE),
   OWNTHING(TraitType.NONE),
   DUALDAGGER(TraitType.DUALDAGGER);

   private final int _mask = 1 << this.ordinal();
   private final TraitType _traitType;

   private WeaponType(TraitType traitType) {
      this._traitType = traitType;
   }

   @Override
   public int mask() {
      return this._mask;
   }

   public TraitType getTraitType() {
      return this._traitType;
   }

   public boolean isRanged() {
      return this == BOW || this == CROSSBOW;
   }
}
