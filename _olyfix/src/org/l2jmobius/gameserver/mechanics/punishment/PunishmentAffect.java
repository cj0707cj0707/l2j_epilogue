package org.l2jmobius.gameserver.mechanics.punishment;

public enum PunishmentAffect {
   ACCOUNT,
   CHARACTER,
   IP,
   HWID;

   public static PunishmentAffect getByName(String name) {
      for (PunishmentAffect type : values()) {
         if (type.name().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return null;
   }
}
