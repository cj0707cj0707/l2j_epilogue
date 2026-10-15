package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class EquipUpdate extends ServerPacket {
   private final Item _item;
   private final int _change;

   public EquipUpdate(Item item, int change) {
      this._item = item;
      this._change = change;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EQUIP_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._change);
      buffer.writeInt(this._item.getObjectId());
      switch (this._item.getTemplate().getBodyPart()) {
         case L_EAR:
            buffer.writeInt(1);
            break;
         case R_EAR:
            buffer.writeInt(2);
            break;
         case NECK:
            buffer.writeInt(3);
            break;
         case R_FINGER:
            buffer.writeInt(4);
            break;
         case L_FINGER:
            buffer.writeInt(5);
            break;
         case HEAD:
            buffer.writeInt(6);
            break;
         case R_HAND:
            buffer.writeInt(7);
            break;
         case L_HAND:
            buffer.writeInt(8);
            break;
         case GLOVES:
            buffer.writeInt(9);
            break;
         case CHEST:
            buffer.writeInt(10);
            break;
         case LEGS:
            buffer.writeInt(11);
            break;
         case FEET:
            buffer.writeInt(12);
            break;
         case BACK:
            buffer.writeInt(13);
            break;
         case LR_HAND:
            buffer.writeInt(14);
            break;
         case HAIR:
            buffer.writeInt(15);
            break;
         case BELT:
            buffer.writeInt(16);
            break;
         default:
            buffer.writeInt(0);
      }
   }
}
