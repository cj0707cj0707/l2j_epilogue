package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;

public class RequestConfirmRefinerItem extends AbstractRefinePacket {
   private int _targetItemObjId;
   private int _refinerItemObjId;

   @Override
   protected void readImpl() {
      this._targetItemObjId = this.readInt();
      this._refinerItemObjId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Item targetItem = player.getInventory().getItemByObjectId(this._targetItemObjId);
         if (targetItem != null) {
            Item refinerItem = player.getInventory().getItemByObjectId(this._refinerItemObjId);
            if (refinerItem != null) {
               if (!isValid(player, targetItem, refinerItem)) {
                  player.sendPacket(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM);
               } else {
                  int refinerItemId = refinerItem.getTemplate().getId();
                  CrystalType grade = targetItem.getTemplate().getCrystalType();
                  AbstractRefinePacket.LifeStone ls = getLifeStone(refinerItemId);
                  int gemStoneId = getGemStoneId(grade);
                  int gemStoneCount = getGemStoneCount(grade, ls.getGrade());
                  player.sendPacket(new ExPutIntensiveResultForVariationMake(this._refinerItemObjId, refinerItemId, gemStoneId, gemStoneCount));
               }
            }
         }
      }
   }
}
