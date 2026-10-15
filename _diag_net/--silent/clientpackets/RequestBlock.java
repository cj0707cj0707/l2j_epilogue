package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;

public class RequestBlock extends ClientPacket {
   private static final int BLOCK = 0;
   private static final int UNBLOCK = 1;
   private static final int BLOCKLIST = 2;
   private static final int ALLBLOCK = 3;
   private static final int ALLUNBLOCK = 4;
   private String _name;
   private Integer _type;

   @Override
   protected void readImpl() {
      this._type = this.readInt();
      if (this._type == 0 || this._type == 1) {
         this._name = this.readString();
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         int targetId = CharInfoTable.getInstance().getIdByName(this._name);
         int targetAL = CharInfoTable.getInstance().getAccessLevelById(targetId);
         switch (this._type) {
            case 0:
            case 1:
               if (targetId <= 0) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_FAILED_TO_REGISTER_THE_USER_TO_YOUR_IGNORE_LIST);
                  return;
               }

               if (targetAL > 0) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_A_GM);
                  return;
               }

               if (player.getObjectId() == targetId) {
                  return;
               }

               if (this._type == 0) {
                  BlockList.addToBlockList(player, targetId);
               } else {
                  BlockList.removeFromBlockList(player, targetId);
               }
               break;
            case 2:
               BlockList.sendListToOwner(player);
               break;
            case 3:
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.MESSAGE_REFUSAL_MODE);
               BlockList.setBlockAll(player, true);
               break;
            case 4:
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
               BlockList.setBlockAll(player, false);
               break;
            default:
               org.l2jmobius.gameserver.network.PacketLogger.info("Unknown 0xA9 block type: " + this._type);
         }
      }
   }
}
