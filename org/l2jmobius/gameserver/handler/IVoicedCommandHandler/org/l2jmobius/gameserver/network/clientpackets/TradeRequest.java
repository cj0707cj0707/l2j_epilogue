package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SendTradeRequest;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class TradeRequest extends ClientPacket {
   private int _objectId;

   @Override
   protected void readImpl() {
      this._objectId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!player.getAccessLevel().allowTransaction()) {
            player.sendMessage("Transactions are disabled for your current Access Level.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
            if (info != null) {
               for (AbstractEffect effect : info.getEffects()) {
                  if (!effect.checkCondition(-2)) {
                     player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_YOUR_ACTIVITIES_ARE_ONLY_ALLOWED_WITHIN_LIMITATION);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }
            }

            WorldObject target = World.findObject(this._objectId);
            if (target != null && player.isInSurroundingRegion(target) && (target.getInstanceId() == player.getInstanceId() || player.getInstanceId() == -1)) {
               if (target.getObjectId() == player.getObjectId()) {
                  player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
               } else if (!target.isPlayer()) {
                  player.sendPacket(SystemMessageId.INVALID_TARGET);
               } else {
                  Player partner = target.asPlayer();
                  if (!partner.isInOlympiadMode() && !player.isInOlympiadMode()) {
                     info = partner.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
                     if (info != null) {
                        for (AbstractEffect effectx : info.getEffects()) {
                           if (!effectx.checkCondition(-2)) {
                              SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_HAS_BEEN_INVESTIGATED);
                              sm.addString(partner.getName());
                              player.sendPacket(sm);
                              player.sendPacket(ActionFailed.STATIC_PACKET);
                              return;
                           }
                        }
                     }

                     if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TRADE && player.getKarma() > 0) {
                        player.sendMessage("You cannot trade while you are in a chaotic state.");
                     } else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TRADE && partner.getKarma() > 0) {
                        player.sendMessage("You cannot request a trade while your target is in a chaotic state.");
                     } else if (!GeneralConfig.JAIL_DISABLE_TRANSACTION || !player.isJailed() && !partner.isJailed()) {
                        if (player.isInStoreMode() || partner.isInStoreMode()) {
                           player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
                        } else if (player.isProcessingTransaction()) {
                           player.sendPacket(SystemMessageId.YOU_ARE_ALREADY_TRADING_WITH_SOMEONE);
                        } else if (partner.isProcessingRequest() || partner.isProcessingTransaction()) {
                           SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_TRADING_WITH_ANOTHER_PERSON_PLEASE_TRY_AGAIN_LATER);
                           sm.addString(partner.getName());
                           player.sendPacket(sm);
                        } else if (partner.getTradeRefusal()) {
                           player.sendMessage("That person is in trade refusal mode.");
                        } else if (BlockList.isBlocked(partner, player)) {
                           SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST);
                           sm.addString(partner.getName());
                           player.sendPacket(sm);
                        } else if (player.calculateDistance3D(partner) > 150.0) {
                           player.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
                        } else {
                           player.onTransactionRequest(partner);
                           partner.sendPacket(new SendTradeRequest(player.getObjectId()));
                           SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_REQUESTED_A_TRADE_WITH_C1);
                           sm.addString(partner.getName());
                           player.sendPacket(sm);
                        }
                     } else {
                        player.sendMessage("You cannot trade while you are in in Jail.");
                     }
                  } else {
                     player.sendMessage("A user currently participating in the Olympiad cannot accept or request a trade.");
                  }
               }
            }
         }
      }
   }
}
