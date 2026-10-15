package org.l2jmobius.gameserver.network.clientpackets;

import java.util.StringTokenizer;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.CaptchaManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcManorBypass;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public class RequestBypassToServer extends ClientPacket {
   private static final String[] _possibleNonHtmlCommands = new String[]{
      "item_", "_bbs", "bbs", "_mail", "_friend", "_match", "_diary", "OlympiadArenaChange", "manor_menu_select", "report"
   };
   private String _command;

   @Override
   protected void readImpl() {
      this._command = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._command.isEmpty()) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(player + " sent empty bypass!");
            org.l2jmobius.gameserver.network.Disconnection.of((org.l2jmobius.gameserver.network.GameClient)this.getClient(), player)
               .storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
         } else {
            boolean requiresBypassValidation = true;

            for (String possibleNonHtmlCommand : _possibleNonHtmlCommands) {
               if (this._command.startsWith(possibleNonHtmlCommand)) {
                  requiresBypassValidation = false;
                  break;
               }
            }

            int bypassOriginId = 0;
            if (requiresBypassValidation) {
               bypassOriginId = player.validateHtmlAction(this._command);
               if (bypassOriginId == -1) {
                  return;
               }

               if (bypassOriginId > 0 && !LocationUtil.isInsideRangeOfObjectId(player, bypassOriginId, 250)) {
                  return;
               }
            }

            if (((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getFloodProtectors().canUseServerBypass()) {
               try {
                  if (this._command.startsWith("admin_")) {
                     AdminCommandHandler.getInstance().onCommand(player, this._command, true);
                  } else if (CommunityBoardHandler.getInstance().isCommunityBoardCommand(this._command)) {
                     CommunityBoardHandler.getInstance().handleParseCommand(this._command, player);
                  } else if (this._command.equals("come_here") && player.isGM()) {
                     this.comeHere(player);
                  } else if (this._command.startsWith("npc_")) {
                     int endOfId = this._command.indexOf(95, 5);
                     String id;
                     if (endOfId > 0) {
                        id = this._command.substring(4, endOfId);
                     } else {
                        id = this._command.substring(4);
                     }

                     if (StringUtil.isNumeric(id)) {
                        WorldObject object = World.findObject(Integer.parseInt(id));
                        if (object != null && object.isNpc() && endOfId > 0 && player.isInsideRadius2D(object, 250)) {
                           object.asNpc().onBypassFeedback(player, this._command.substring(endOfId + 1));
                        }
                     }

                     player.sendPacket(ActionFailed.STATIC_PACKET);
                  } else if (this._command.startsWith("item_")) {
                     int endOfIdx = this._command.indexOf(95, 5);
                     String idx;
                     if (endOfIdx > 0) {
                        idx = this._command.substring(5, endOfIdx);
                     } else {
                        idx = this._command.substring(5);
                     }

                     try {
                        Item item = player.getInventory().getItemByObjectId(Integer.parseInt(idx));
                        if (item != null && endOfIdx > 0) {
                           item.onBypassFeedback(player, this._command.substring(endOfIdx + 1));
                        }

                        player.sendPacket(ActionFailed.STATIC_PACKET);
                     } catch (NumberFormatException var10) {
                        org.l2jmobius.gameserver.network.PacketLogger.warning("NFE for command [" + this._command + "] " + var10.getMessage());
                     }
                  } else if (this._command.startsWith("_match")) {
                     String params = this._command.substring(this._command.indexOf(63) + 1);
                     StringTokenizer st = new StringTokenizer(params, "&");
                     int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                     int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                     int heroid = Hero.getInstance().getHeroByClass(heroclass);
                     if (heroid > 0) {
                        Hero.getInstance().showHeroFights(player, heroclass, heroid, heropage);
                     }
                  } else if (this._command.startsWith("_diary")) {
                     String params = this._command.substring(this._command.indexOf(63) + 1);
                     StringTokenizer st = new StringTokenizer(params, "&");
                     int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
                     int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
                     int heroid = Hero.getInstance().getHeroByClass(heroclass);
                     if (heroid > 0) {
                        Hero.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
                     }
                  } else if (this._command.startsWith("OlympiadArenaChange")) {
                     Olympiad.bypassChangeArena(this._command, player);
                  } else if (this._command.startsWith("manor_menu_select")) {
                     Npc lastNpc = player.getLastFolkNPC();
                     if (GeneralConfig.ALLOW_MANOR
                        && lastNpc != null
                        && lastNpc.canInteract(player)
                        && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MANOR_BYPASS, lastNpc)) {
                        String[] split = this._command.substring(this._command.indexOf(63) + 1).split("&");
                        int ask = Integer.parseInt(split[0].split("=")[1]);
                        int state = Integer.parseInt(split[1].split("=")[1]);
                        boolean time = split[2].split("=")[1].equals("1");
                        EventDispatcher.getInstance().notifyEventAsync(new OnNpcManorBypass(player, lastNpc, ask, state, time), lastNpc);
                     }
                  } else if (this._command.startsWith("report")) {
                     CaptchaManager.getInstance().analyseBypass(this._command, player);
                  } else {
                     IBypassHandler handler = BypassHandler.getInstance().getHandler(this._command);
                     if (handler != null) {
                        if (bypassOriginId > 0) {
                           WorldObject bypassOrigin = World.findObject(bypassOriginId);
                           if (bypassOrigin != null && bypassOrigin.isCreature()) {
                              handler.onCommand(this._command, player, bypassOrigin.asCreature());
                           } else {
                              handler.onCommand(this._command, player, null);
                           }
                        } else {
                           handler.onCommand(this._command, player, null);
                        }
                     } else {
                        org.l2jmobius.gameserver.network.PacketLogger.warning(
                           this.getClient() + " sent not handled RequestBypassToServer: [" + this._command + "]"
                        );
                     }
                  }
               } catch (Exception var11) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     "Exception processing bypass from " + player + ": " + this._command + " " + var11.getMessage()
                  );
                  org.l2jmobius.gameserver.network.PacketLogger.warning(TraceUtil.getStackTrace(var11));
                  if (player.isGM()) {
                     StringBuilder sb = new StringBuilder(200);
                     sb.append("<html><body>");
                     sb.append("Bypass error: " + var11 + "<br1>");
                     sb.append("Bypass command: " + this._command + "<br1>");
                     sb.append("StackTrace:<br1>");

                     for (StackTraceElement ste : var11.getStackTrace()) {
                        sb.append(ste + "<br1>");
                     }

                     sb.append("</body></html>");
                     NpcHtmlMessage msg = new NpcHtmlMessage(0, 1, sb.toString());
                     msg.disableValidation();
                     player.sendPacket(msg);
                  }
               }

               if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_BYPASS, player)) {
                  EventDispatcher.getInstance().notifyEventAsync(new OnPlayerBypass(player, this._command), player);
               }
            }
         }
      }
   }

   private void comeHere(Player player) {
      WorldObject obj = player.getTarget();
      if (obj != null) {
         if (obj instanceof Npc) {
            Npc temp = obj.asNpc();
            temp.setTarget(player);
            temp.getAI().setIntentionMoveTo(player.getLocation());
         }
      }
   }
}
