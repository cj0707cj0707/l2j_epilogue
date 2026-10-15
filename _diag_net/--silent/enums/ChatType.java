package org.l2jmobius.gameserver.network.enums;

public enum ChatType {
   GENERAL(0),
   SHOUT(1),
   WHISPER(2),
   PARTY(3),
   CLAN(4),
   GM(5),
   PETITION_PLAYER(6),
   PETITION_GM(7),
   TRADE(8),
   ALLIANCE(9),
   ANNOUNCEMENT(10),
   BOAT(11),
   FRIEND(12),
   MSNCHAT(13),
   PARTYMATCH_ROOM(14),
   PARTYROOM_COMMANDER(15),
   PARTYROOM_ALL(16),
   HERO_VOICE(17),
   CRITICAL_ANNOUNCE(18),
   SCREEN_ANNOUNCE(19),
   BATTLEFIELD(20),
   MPCC_ROOM(21),
   NPC_GENERAL(0),
   NPC_SHOUT(1);

   private final int _clientId;

   private ChatType(int clientId) {
      this._clientId = clientId;
   }

   public int getClientId() {
      return this._clientId;
   }

   public static ChatType findByClientId(int clientId) {
      for (ChatType ChatType : values()) {
         if (ChatType.getClientId() == clientId) {
            return ChatType;
         }
      }

      return null;
   }
}
