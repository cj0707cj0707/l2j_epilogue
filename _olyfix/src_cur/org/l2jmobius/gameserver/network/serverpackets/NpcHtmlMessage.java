package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

public class NpcHtmlMessage extends AbstractHtmlPacket {
   private final int _itemId;

   public NpcHtmlMessage() {
      this._itemId = 0;
   }

   public NpcHtmlMessage(int npcObjId) {
      super(npcObjId);
      this._itemId = 0;
   }

   public NpcHtmlMessage(String html) {
      super(html);
      this._itemId = 0;
   }

   public NpcHtmlMessage(int npcObjId, String html) {
      super(npcObjId, html);
      this._itemId = 0;
   }

   public NpcHtmlMessage(int npcObjId, int itemId) {
      super(npcObjId);
      if (itemId < 0) {
         throw new IllegalArgumentException();
      } else {
         this._itemId = itemId;
      }
   }

   public NpcHtmlMessage(int npcObjId, int itemId, String html) {
      super(npcObjId, html);
      if (itemId < 0) {
         throw new IllegalArgumentException();
      } else {
         this._itemId = itemId;
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.NPC_HTML_MESSAGE.writeId(this, buffer);
      buffer.writeInt(this.getNpcObjId());
      buffer.writeString(this.getHtml());
      buffer.writeInt(this._itemId);
   }

   @Override
   public HtmlActionScope getScope() {
      return this._itemId == 0 ? HtmlActionScope.NPC_HTML : HtmlActionScope.NPC_ITEM_HTML;
   }
}
