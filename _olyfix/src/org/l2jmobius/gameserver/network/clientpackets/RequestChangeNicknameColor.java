package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class RequestChangeNicknameColor extends ClientPacket {
   private static final int[] COLORS = new int[]{9671679, 8145404, 9959676, 16423662, 16735635, 64672, 10528257, 7903407, 4743829, 10066329};
   private int _colorNum;
   private int _itemObjectId;
   private String _title;

   @Override
   protected void readImpl() {
      this._colorNum = this.readInt();
      this._title = this.readString();
      this._itemObjectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._colorNum >= 0 && this._colorNum < COLORS.length) {
            Item item = player.getInventory().getItemByObjectId(this._itemObjectId);
            if (item != null
               && item.getEtcItem() != null
               && item.getEtcItem().getHandlerName() != null
               && item.getEtcItem().getHandlerName().equalsIgnoreCase("NicknameColor")) {
               if (player.destroyItem(ItemProcessType.NONE, item, 1L, null, true)) {
                  player.setTitle(this._title);
                  player.getAppearance().setTitleColor(COLORS[this._colorNum]);
                  player.broadcastUserInfo();
               }
            }
         }
      }
   }
}
