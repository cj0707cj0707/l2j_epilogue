package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;

public class RequestConfirmGemStone extends AbstractRefinePacket {
   private int _targetItemObjId;
   private int _refinerItemObjId;
   private int _gemstoneItemObjId;
   private long _gemStoneCount;

   @Override
   protected void readImpl() {
      this._targetItemObjId = this.readInt();
      this._refinerItemObjId = this.readInt();
      this._gemstoneItemObjId = this.readInt();
      this._gemStoneCount = this.readLong();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item targetItem = player.getInventory().getItemByObjectId(this._targetItemObjId);
         if (targetItem != null) {
            Item refinerItem = player.getInventory().getItemByObjectId(this._refinerItemObjId);
            if (refinerItem != null) {
               Item gemStoneItem = player.getInventory().getItemByObjectId(this._gemstoneItemObjId);
               if (gemStoneItem != null) {
                  if (!isValid(player, targetItem, refinerItem, gemStoneItem)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
                  } else {
                     AbstractRefinePacket.LifeStone ls = getLifeStone(refinerItem.getId());
                     if (ls != null) {
                        if (this._gemStoneCount != getGemStoneCount(targetItem.getTemplate().getCrystalType(), ls.getGrade())) {
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT);
                        } else {
                           player.sendPacket(new ExPutCommissionResultForVariationMake(this._gemstoneItemObjId, this._gemStoneCount, gemStoneItem.getId()));
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
