package org.l2jmobius.gameserver.entity.actor.enums.player;

public enum PrivateStoreType {
   NONE(0),
   SELL(1),
   SELL_MANAGE(2),
   BUY(3),
   BUY_MANAGE(4),
   MANUFACTURE(5),
   PACKAGE_SELL(8),
   SELL_BUFFS(9);

   private int _id;

   private PrivateStoreType(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }

   public static PrivateStoreType findById(int id) {
      for (PrivateStoreType privateStoreType : values()) {
         if (privateStoreType.getId() == id) {
            return privateStoreType;
         }
      }

      return null;
   }
}
