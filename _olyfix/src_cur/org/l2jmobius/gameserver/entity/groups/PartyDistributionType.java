package org.l2jmobius.gameserver.entity.groups;

public enum PartyDistributionType {
   FINDERS_KEEPERS(0, 487),
   RANDOM(1, 488),
   RANDOM_INCLUDING_SPOIL(2, 798),
   BY_TURN(3, 799),
   BY_TURN_INCLUDING_SPOIL(4, 800);

   private final int _id;
   private final int _sysStringId;

   private PartyDistributionType(int id, int sysStringId) {
      this._id = id;
      this._sysStringId = sysStringId;
   }

   public int getId() {
      return this._id;
   }

   public int getSysStringId() {
      return this._sysStringId;
   }

   public static PartyDistributionType findById(int id) {
      for (PartyDistributionType partyDistributionType : values()) {
         if (partyDistributionType.getId() == id) {
            return partyDistributionType;
         }
      }

      return null;
   }
}
