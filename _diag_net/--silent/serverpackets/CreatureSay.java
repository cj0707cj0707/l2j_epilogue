package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class CreatureSay extends ServerPacket {
   private final Creature _sender;
   private final ChatType _chatType;
   private String _senderName = null;
   private String _text = null;
   private int _charId = 0;
   private int _messageId = 0;
   private List<String> _parameters;

   public CreatureSay(Creature sender, ChatType chatType, String senderName, String text) {
      this._sender = sender;
      this._chatType = chatType;
      this._senderName = senderName;
      this._text = text;
   }

   public CreatureSay(Creature sender, ChatType chatType, String text) {
      this._sender = sender;
      this._chatType = chatType;
      this._text = text;
      if (sender != null) {
         this._senderName = sender.getName();
      }
   }

   public CreatureSay(ChatType chatType, int charId, org.l2jmobius.gameserver.network.SystemMessageId systemMessageId) {
      this._sender = null;
      this._chatType = chatType;
      this._charId = charId;
      this._messageId = systemMessageId.getId();
   }

   public void addStringParameter(String text) {
      if (this._parameters == null) {
         this._parameters = new ArrayList<>();
      }

      this._parameters.add(text);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SAY2.writeId(this, buffer);
      buffer.writeInt(this._sender == null ? 0 : this._sender.getObjectId());
      buffer.writeInt(this._chatType.getClientId());
      if (this._senderName != null) {
         buffer.writeString(this._senderName);
      } else {
         buffer.writeInt(this._charId);
      }

      if (this._messageId != 0) {
         buffer.writeInt(this._messageId);
      } else if (this._text != null) {
         buffer.writeString(this._text);
      } else if (this._parameters != null) {
         for (String s : this._parameters) {
            buffer.writeString(s);
         }
      }
   }

   @Override
   public void runImpl(Player player) {
      if (player != null) {
         player.broadcastSnoop(this._chatType, this._senderName, this._text, this);
      }
   }
}
