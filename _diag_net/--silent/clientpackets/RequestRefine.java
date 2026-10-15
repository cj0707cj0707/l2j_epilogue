package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.AugmentationData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestRefine extends AbstractRefinePacket {
   private int _targetItemObjId;
   private int _refinerItemObjId;
   private int _gemStoneItemObjId;
   private long _gemStoneCount;

   @Override
   protected void readImpl() {
      this._targetItemObjId = this.readInt();
      this._refinerItemObjId = this.readInt();
      this._gemStoneItemObjId = this.readInt();
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
               Item gemStoneItem = player.getInventory().getItemByObjectId(this._gemStoneItemObjId);
               if (gemStoneItem != null) {
                  if (!isValid(player, targetItem, refinerItem, gemStoneItem)) {
                     player.sendPacket(new ExVariationResult(0, 0, 0));
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
                  } else {
                     AbstractRefinePacket.LifeStone ls = getLifeStone(refinerItem.getId());
                     if (ls != null) {
                        int lifeStoneGrade = ls.getGrade();
                        if (this._gemStoneCount != getGemStoneCount(targetItem.getTemplate().getCrystalType(), lifeStoneGrade)) {
                           player.sendPacket(new ExVariationResult(0, 0, 0));
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
                        } else {
                           int lifeStoneLevel = ls.getLevel();
                           if (targetItem.isEquipped()) {
                              InventoryUpdate iu = new InventoryUpdate();

                              for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(targetItem.getLocationSlot())) {
                                 iu.addModifiedItem(itm);
                              }

                              player.sendPacket(iu);
                              player.broadcastUserInfo();
                           }

                           if (player.destroyItem(ItemProcessType.FEE, refinerItem, 1L, null, false)) {
                              if (player.destroyItem(ItemProcessType.FEE, gemStoneItem, this._gemStoneCount, null, false)) {
                                 Augmentation aug = AugmentationData.getInstance()
                                    .generateRandomAugmentation(
                                       lifeStoneLevel, lifeStoneGrade, targetItem.getTemplate().getBodyPart(), refinerItem.getId(), targetItem
                                    );
                                 targetItem.setAugmentation(aug);
                                 int stat12 = 65535 & aug.getAugmentationId();
                                 int stat34 = aug.getAugmentationId() >> 16;
                                 player.sendPacket(new ExVariationResult(stat12, stat34, 1));
                                 InventoryUpdate iu = new InventoryUpdate();
                                 iu.addModifiedItem(targetItem);
                                 player.sendPacket(iu);
                                 StatusUpdate su = new StatusUpdate(player);
                                 su.addAttribute(14, player.getCurrentLoad());
                                 player.sendPacket(su);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
