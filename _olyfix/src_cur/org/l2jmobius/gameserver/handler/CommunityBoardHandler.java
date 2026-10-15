package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class CommunityBoardHandler implements IHandler<IParseBoardHandler, String> {
   private static final Logger LOG = Logger.getLogger(CommunityBoardHandler.class.getName());
   private final Map<String, IParseBoardHandler> _datatable = new HashMap<>();
   private final Map<Integer, String> _bypasses = new ConcurrentHashMap<>();

   protected CommunityBoardHandler() {
   }

   public void registerHandler(IParseBoardHandler handler) {
      for (String cmd : handler.getCommandList()) {
         this._datatable.put(cmd.toLowerCase(), handler);
      }
   }

   public synchronized void removeHandler(IParseBoardHandler handler) {
      for (String cmd : handler.getCommandList()) {
         this._datatable.remove(cmd.toLowerCase());
      }
   }

   public IParseBoardHandler getHandler(String cmd) {
      for (IParseBoardHandler cb : this._datatable.values()) {
         for (String command : cb.getCommandList()) {
            if (cmd.toLowerCase().startsWith(command.toLowerCase())) {
               return cb;
            }
         }
      }

      return null;
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public boolean isCommunityBoardCommand(String cmd) {
      return this.getHandler(cmd) != null;
   }

   public void handleParseCommand(String command, Player player) {
      if (player != null) {
         if (!GeneralConfig.ENABLE_COMMUNITY_BOARD) {
            player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
         } else {
            IParseBoardHandler cb = this.getHandler(command);
            if (cb == null) {
               LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find parse handler for command " + command + "!");
            } else {
               cb.onCommand(command, player);
            }
         }
      }
   }

   public void handleWriteCommand(Player player, String url, String arg1, String arg2, String arg3, String arg4, String arg5) {
      if (player != null) {
         if (!GeneralConfig.ENABLE_COMMUNITY_BOARD) {
            player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
         } else {
            String cmd = "";
            switch (url) {
               case "Topic":
                  cmd = "_bbstop";
                  break;
               case "Post":
                  cmd = "_bbspos";
                  break;
               case "Region":
                  cmd = "_bbsloc";
                  break;
               case "Notice":
                  cmd = "_bbsclan";
                  break;
               default:
                  separateAndSend("<html><body><br><br><center>The command: " + url + " is not implemented yet.</center><br><br></body></html>", player);
                  return;
            }

            IParseBoardHandler cb = this.getHandler(cmd);
            if (cb == null) {
               LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find write handler for command " + cmd + "!");
            } else if (!(cb instanceof IWriteBoardHandler)) {
               LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": " + cb.getClass().getSimpleName() + " doesn't implement write!");
            } else {
               ((IWriteBoardHandler)cb).writeCommunityBoardCommand(player, arg1, arg2, arg3, arg4, arg5);
            }
         }
      }
   }

   public void addBypass(Player player, String title, String bypass) {
      this._bypasses.put(player.getObjectId(), title + "&" + bypass);
   }

   public String removeBypass(Player player) {
      return this._bypasses.remove(player.getObjectId());
   }

   public static void separateAndSend(String html, Player player) {
      HtmlUtil.sendCBHtml(player, html);
   }

   public static CommunityBoardHandler getInstance() {
      return CommunityBoardHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CommunityBoardHandler INSTANCE = new CommunityBoardHandler();
   }
}
