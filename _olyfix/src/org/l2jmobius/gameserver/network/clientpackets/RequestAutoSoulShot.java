package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAutoSoulShot extends ClientPacket {
   private int _itemId;
   private int _type;

   @Override
   protected void readImpl() {
      this._itemId = this.readInt();
      this._type = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.isInStoreMode() && player.getActiveRequester() == null && !player.isDead()) {
            Item item = player.getInventory().getItemByItemId(this._itemId);
            if (item == null) {
               return;
            }

            if (player.isInOlympiadMode()
               && OlympiadConfig.OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS
               && item.getEtcItem().getDefaultAction() == ActionType.SPIRITSHOT
               && item.getEtcItem().isBlessed()) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
               return;
            }

            if (this._type == 1) {
               if (!player.getInventory().canManipulateWithItemId(item.getId())) {
                  player.sendMessage("Cannot use this item.");
                  return;
               }

               if (this._itemId < 6535 || this._itemId > 6540) {
                  if (this._itemId != 6645
                     && this._itemId != 6646
                     && this._itemId != 6647
                     && this._itemId != 20332
                     && this._itemId != 20333
                     && this._itemId != 20334) {
                     if (player.getActiveWeaponItem() != player.getFistsWeaponItem()
                        && item.getTemplate().getCrystalType() == player.getActiveWeaponItem().getCrystalTypePlus()) {
                        player.addAutoSoulShot(this._itemId);
                        player.sendPacket(new ExAutoSoulShot(this._itemId, this._type));
                     } else {
                        if ((this._itemId < 2509 || this._itemId > 2514)
                           && (this._itemId < 3947 || this._itemId > 3952)
                           && this._itemId != 5790
                           && (this._itemId < 22072 || this._itemId > 22081)) {
                           player.sendPacket(SystemMessageId.THE_SOULSHOT_YOU_ARE_ATTEMPTING_TO_USE_DOES_NOT_MATCH_THE_GRADE_OF_YOUR_EQUIPPED_WEAPON);
                        } else {
                           player.sendPacket(SystemMessageId.THE_SPIRITSHOT_DOES_NOT_MATCH_THE_WEAPON_S_GRADE);
                        }

                        player.addAutoSoulShot(this._itemId);
                        player.sendPacket(new ExAutoSoulShot(this._itemId, this._type));
                     }

                     SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_ACTIVATED);
                     sm.addItemName(item);
                     player.sendPacket(sm);
                     player.rechargeShots(true, true);
                  } else if (player.hasSummon()) {
                     if (item.getEtcItem().getHandlerName().equals("BeastSoulShot")) {
                        if (player.getSummon().getSoulShotsPerHit() > item.getCount()) {
                           player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_PET_SERVITOR);
                           return;
                        }
                     } else if (player.getSummon().getSpiritShotsPerHit() > item.getCount()) {
                        player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_SOULSHOTS_NEEDED_FOR_A_PET_SERVITOR);
                        return;
                     }

                     player.addAutoSoulShot(this._itemId);
                     player.sendPacket(new ExAutoSoulShot(this._itemId, this._type));
                     SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_ACTIVATED);
                     sm.addItemName(item);
                     player.sendPacket(sm);
                     player.rechargeShots(true, true);
                     player.getSummon().rechargeShots(true, true);
                  } else {
                     player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR_OR_PET_AND_THEREFORE_CANNOT_USE_THE_AUTOMATIC_USE_FUNCTION);
                  }
               }
            } else if (this._type == 0) {
               player.removeAutoSoulShot(this._itemId);
               player.sendPacket(new ExAutoSoulShot(this._itemId, this._type));
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
               sm.addItemName(item);
               player.sendPacket(sm);
            }
         }
      }
   }
}
