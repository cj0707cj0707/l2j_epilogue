package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.entity.actor.holders.creature.DoorRequestHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.SummonRequestHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.managers.ZoneBuildManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerDlgAnswer;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class DlgAnswer extends ClientPacket {
   private int _messageId;
   private int _answer;
   private int _requesterId;

   @Override
   protected void readImpl() {
      this._messageId = this.readInt();
      this._answer = this.readInt();
      this._requesterId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_DLG_ANSWER, player)) {
            TerminateReturn term = EventDispatcher.getInstance()
               .notifyEvent(new OnPlayerDlgAnswer(player, this._messageId, this._answer, this._requesterId), player, TerminateReturn.class);
            if (term != null && term.terminate()) {
               return;
            }
         }

         if (this._messageId == SystemMessageId.S1_3.getId()) {
            if (player.removeAction(PlayerAction.OFFLINE_TRADE)) {
               if (this._answer != 0
                  && OfflineTradeConfig.ENABLE_OFFLINE_COMMAND
                  && (OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE)) {
                  if (!player.isInStoreMode()) {
                     player.sendPacket(SystemMessageId.PRIVATE_STORE_ALREADY_CLOSED);
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (!player.isInInstance() && !player.isInVehicle() && player.canLogout()) {
                     player.removeFromBossZone();
                     if (Olympiad.getInstance().isRegistered(player)) {
                        Olympiad.getInstance().unRegisterNoble(player);
                     }

                     if (!OfflineTraderTable.getInstance().enteredOfflineMode(player)) {
                        Disconnection.of(this.getClient(), player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
                     }

                     return;
                  }

                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               return;
            }

            if (player.removeAction(PlayerAction.OFFLINE_PLAY)) {
               if (this._answer != 0 && OfflinePlayConfig.ENABLE_OFFLINE_PLAY_COMMAND) {
                  if (OfflinePlayConfig.OFFLINE_PLAY_PREMIUM && !player.hasPremiumStatus()) {
                     player.sendMessage("This command is only available to premium players.");
                     return;
                  }

                  if (!player.isAutoPlaying()) {
                     player.sendMessage("You need to enable auto play before exiting.");
                     return;
                  }

                  if (!player.isInVehicle() && !player.isInsideZone(ZoneId.PEACE)) {
                     if (player.isRegisteredOnEvent()) {
                        player.sendMessage("Cannot use this command while registered on an event.");
                        return;
                     }

                     if (Olympiad.getInstance().isRegistered(player)) {
                        Olympiad.getInstance().unRegisterNoble(player);
                     }

                     player.startOfflinePlay();
                     return;
                  }

                  player.sendPacket(SystemMessageId.YOU_MAY_NOT_LOG_OUT_FROM_THIS_LOCATION);
                  return;
               }

               return;
            }

            if (player.removeAction(PlayerAction.USER_ENGAGE)) {
               if (WeddingConfig.ALLOW_WEDDING) {
                  player.engageAnswer(this._answer);
               }

               return;
            }

            if (player.removeAction(PlayerAction.ADMIN_SAVE_ZONE)) {
               if (this._answer == 0) {
                  return;
               }

               ZoneBuildManager.getInstance().buildZone(player);
               return;
            }

            if (player.removeAction(PlayerAction.ADMIN_COMMAND)) {
               String cmd = player.getAdminConfirmCmd();
               player.setAdminConfirmCmd(null);
               if (this._answer == 0) {
                  return;
               }

               AdminCommandHandler.getInstance().onCommand(player, cmd, false);
            }
         } else if (this._messageId == SystemMessageId.C1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_WITH_S2_EXPERIENCE_POINTS_DO_YOU_WANT_TO_BE_RESURRECTED.getId()
            || this._messageId == SystemMessageId.RESURRECTION_IS_POSSIBLE_BECAUSE_OF_THE_COURAGE_CHARM_S_EFFECT_WOULD_YOU_LIKE_TO_RESURRECT_NOW.getId()) {
            player.reviveAnswer(this._answer);
         } else if (this._messageId == SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId()) {
            SummonRequestHolder holder = player.removeScript(SummonRequestHolder.class);
            if (this._answer == 1 && holder != null && holder.getSummoner().getObjectId() == this._requesterId) {
               player.teleToLocation(holder.getLocation(), true);
            }
         } else if (this._messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId()) {
            DoorRequestHolder holder = player.removeScript(DoorRequestHolder.class);
            if (holder != null && holder.getDoor() == player.getTarget() && this._answer == 1) {
               holder.getDoor().openMe();
            }
         } else if (this._messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId()) {
            DoorRequestHolder holder = player.removeScript(DoorRequestHolder.class);
            if (holder != null && holder.getDoor() == player.getTarget() && this._answer == 1) {
               holder.getDoor().closeMe();
            }
         }
      }
   }
}
