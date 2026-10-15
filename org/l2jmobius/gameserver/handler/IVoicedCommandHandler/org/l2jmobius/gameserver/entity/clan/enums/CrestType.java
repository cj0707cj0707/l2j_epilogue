package org.l2jmobius.gameserver.entity.clan.enums;

public enum CrestType {
   PLEDGE(1),
   PLEDGE_LARGE(2),
   ALLY(3);

   private final int _id;

   private CrestType(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }

   public static CrestType getById(int id) {
      for (CrestType crestType : values()) {
         if (crestType.getId() == id) {
            return crestType;
         }
      }

      return null;
   }
}
