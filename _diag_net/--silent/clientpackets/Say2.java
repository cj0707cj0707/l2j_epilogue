package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.WalkerBotProtectionConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.ChatHandler;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerChat;
import org.l2jmobius.gameserver.mechanics.events.returns.ChatFilterReturn;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class Say2 extends ClientPacket {
   private static Logger LOGGER_CHAT = Logger.getLogger("chat");
   private static final String[] WALKER_COMMAND_LIST = new String[]{
      "USESKILL",
      "USEITEM",
      "BUYITEM",
      "SELLITEM",
      "SAVEITEM",
      "LOADITEM",
      "MSG",
      "DELAY",
      "LABEL",
      "JMP",
      "CALL",
      "RETURN",
      "MOVETO",
      "NPCSEL",
      "NPCDLG",
      "DLGSEL",
      "CHARSTATUS",
      "POSOUTRANGE",
      "POSINRANGE",
      "GOHOME",
      "SAY",
      "EXIT",
      "PAUSE",
      "STRINDLG",
      "STRNOTINDLG",
      "CHANGEWAITTYPE",
      "FORCEATTACK",
      "ISMEMBER",
      "REQUESTJOINPARTY",
      "REQUESTOUTPARTY",
      "QUITPARTY",
      "MEMBERSTATUS",
      "CHARBUFFS",
      "ITEMCOUNT",
      "FOLLOWTELEPORT"
   };
   private String _text;
   private int _type;
   private String _target;

   @Override
   protected void readImpl() {
      this._text = this.readString();
      this._type = this.readInt();
      this._target = this._type == ChatType.WHISPER.getClientId() ? this.readString() : null;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         ChatType chatType = ChatType.findByClientId(this._type);
         if (chatType == null) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "Say2: Invalid type: " + this._type + " Player : " + player.getName() + " text: " + this._text
            );
            player.sendPacket(ActionFailed.STATIC_PACKET);
            org.l2jmobius.gameserver.network.Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
         } else if (this._text.isEmpty()) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(player.getName() + ": sending empty text. Possible packet hack!");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            org.l2jmobius.gameserver.network.Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
         } else if (player.isGM() || (this._text.indexOf(8) < 0 || this._text.length() <= 500) && (this._text.indexOf(8) >= 0 || this._text.length() <= 105)) {
            if (WalkerBotProtectionConfig.L2WALKER_PROTECTION && chatType == ChatType.WHISPER && this.checkBot(this._text)) {
               PunishmentManager.handleIllegalPlayerAction(player, "Client Emulator Detect: " + player + " using L2Walker.", GeneralConfig.DEFAULT_PUNISH);
            } else if (!player.isCursedWeaponEquipped() || chatType != ChatType.TRADE && chatType != ChatType.SHOUT) {
               if (player.isChatBanned() && this._text.charAt(0) != '.') {
                  if (player.getEffectList().getFirstEffect(EffectType.CHAT_BLOCK) != null) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CHATTING_IS_NOT_ALLOWED
                     );
                  } else if (GeneralConfig.BAN_CHAT_CHANNELS.contains(chatType)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
                  }
               } else if (!player.isJailed()
                  || !GeneralConfig.JAIL_DISABLE_CHAT
                  || chatType != ChatType.WHISPER && chatType != ChatType.SHOUT && chatType != ChatType.TRADE && chatType != ChatType.HERO_VOICE) {
                  if (chatType == ChatType.PETITION_PLAYER && player.isGM()) {
                     chatType = ChatType.PETITION_GM;
                  }

                  if (GeneralConfig.LOG_CHAT) {
                     StringBuilder sb = new StringBuilder();
                     sb.append(chatType.name());
                     sb.append(" [");
                     sb.append(player);
                     if (chatType == ChatType.WHISPER) {
                        sb.append(" to ");
                        sb.append(this._target);
                        sb.append("] ");
                        sb.append(this._text);
                        LOGGER_CHAT.info(sb.toString());
                     } else {
                        sb.append("] ");
                        sb.append(this._text);
                        LOGGER_CHAT.info(sb.toString());
                     }
                  }

                  if (this._text.indexOf(8) < 0 || this.parseAndPublishItem(player)) {
                     if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CHAT)) {
                        ChatFilterReturn filter = (ChatFilterReturn)EventDispatcher.getInstance()
                           .notifyEvent(new OnPlayerChat(player, World.getPlayer(this._target), this._text, chatType), ChatFilterReturn.class);
                        if (filter != null) {
                           this._text = filter.getFilteredText();
                        }
                     }

                     if (GeneralConfig.USE_SAY_FILTER) {
                        this.checkText();
                     }

                     IChatHandler handler = ChatHandler.getInstance().getHandler(chatType);
                     if (handler != null) {
                        handler.onChat(chatType, player, this._target, this._text);
                     } else {
                        org.l2jmobius.gameserver.network.PacketLogger.info("No handler registered for ChatType: " + this._type + " Player: " + player);
                     }
                  }
               } else {
                  player.sendMessage("You can not chat with players outside of the jail.");
               }
            } else {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.SHOUT_AND_TRADE_CHATTING_CANNOT_BE_USED_WHILE_POSSESSING_A_CURSED_WEAPON);
            }
         } else {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.WHEN_A_USER_S_KEYBOARD_INPUT_EXCEEDS_A_CERTAIN_CUMULATIVE_SCORE_A_CHAT_BAN_WILL_BE_APPLIED_THIS_IS_DONE_TO_DISCOURAGE_SPAMMING_PLEASE_AVOID_POSTING_THE_SAME_MESSAGE_MULTIPLE_TIMES_DURING_A_SHORT_PERIOD
            );
         }
      }
   }

   private boolean checkBot(String text) {
      for (String botCommand : WALKER_COMMAND_LIST) {
         if (text.startsWith(botCommand)) {
            return true;
         }
      }

      return false;
   }

   private void checkText() {
      String filteredText = this._text;

      for (String pattern : ServerConfig.FILTER_LIST) {
         filteredText = filteredText.replaceAll("(?i)" + pattern, GeneralConfig.CHAT_FILTER_CHARS);
      }

      this._text = filteredText;
   }

   private boolean parseAndPublishItem(Player owner) {
      int pos1 = -1;

      while ((pos1 = this._text.indexOf(8, pos1)) > -1) {
         int pos = this._text.indexOf("ID=", pos1);
         if (pos == -1) {
            return false;
         }

         StringBuilder result = new StringBuilder(9);
         pos += 3;

         while (Character.isDigit(this._text.charAt(pos))) {
            result.append(this._text.charAt(pos++));
         }

         int id = Integer.parseInt(result.toString());
         WorldObject item = World.findObject(id);
         if (!(item instanceof Item)) {
            return false;
         }

         if (owner.getInventory().getItemByObjectId(id) == null) {
            org.l2jmobius.gameserver.network.PacketLogger.info(owner.getClient() + " trying publish item which does not own! ID:" + id);
            return false;
         }

         ((Item)item).publish();
         pos1 = this._text.indexOf(8, pos) + 1;
         if (pos1 == 0) {
            org.l2jmobius.gameserver.network.PacketLogger.info(owner.getClient() + " sent invalid publish item msg! ID:" + id);
            return false;
         }
      }

      return true;
   }
}
