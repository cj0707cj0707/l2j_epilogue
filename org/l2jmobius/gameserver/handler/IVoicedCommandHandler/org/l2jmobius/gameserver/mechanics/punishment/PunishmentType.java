package org.l2jmobius.gameserver.mechanics.punishment;

public enum PunishmentType {
   BAN,
   CHAT_BAN,
   PARTY_BAN,
   JAIL;

   public static PunishmentType getByName(String name) {
      for (PunishmentType type : values()) {
         if (type.name().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return null;
   }
}
