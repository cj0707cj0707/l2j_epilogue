package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.util.GMAudit;

public class AdminCommandHandler implements IHandler<IAdminCommandHandler, String> {
   private static final Logger LOGGER = Logger.getLogger(AdminCommandHandler.class.getName());
   private final Map<String, IAdminCommandHandler> _datatable = new HashMap<>();

   protected AdminCommandHandler() {
   }

   public void registerHandler(IAdminCommandHandler handler) {
      for (String id : handler.getCommandList()) {
         this._datatable.put(id, handler);
      }
   }

   public synchronized void removeHandler(IAdminCommandHandler handler) {
      for (String id : handler.getCommandList()) {
         this._datatable.remove(id);
      }
   }

   public IAdminCommandHandler getHandler(String adminCommand) {
      String command = adminCommand;
      if (adminCommand.contains(" ")) {
         command = adminCommand.substring(0, adminCommand.indexOf(32));
      }

      return this._datatable.get(command);
   }

   public void onCommand(Player player, String fullCommand, boolean useConfirm) {
      if (player.isGM()) {
         String command = fullCommand.split(" ")[0];
         String commandNoPrefix = command.substring(6);
         IAdminCommandHandler handler = this.getHandler(command);
         if (handler == null) {
            player.sendMessage("The command '" + commandNoPrefix + "' does not exist!");
            LOGGER.warning("No handler registered for admin command '" + command + "'");
         } else if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel())) {
            player.sendMessage("You don't have the access rights to use this command!");
            LOGGER.warning(player + " tried to use admin command '" + command + "', without proper access level!");
         } else {
            if (useConfirm && AdminData.getInstance().requireConfirm(command)) {
               player.setAdminConfirmCmd(fullCommand);
               ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_3);
               dlg.getSystemMessage().addString("Are you sure you want execute command '" + commandNoPrefix + "' ?");
               player.addAction(PlayerAction.ADMIN_COMMAND);
               player.sendPacket(dlg);
            } else {
               ThreadPool.execute(() -> {
                  long begin = System.currentTimeMillis();

                  try {
                     if (GeneralConfig.GMAUDIT) {
                        WorldObject target = player.getTarget();
                        GMAudit.logAction(player.getName() + " [" + player.getObjectId() + "]", fullCommand, target != null ? target.getName() : "no-target");
                     }

                     handler.onCommand(fullCommand, player);
                  } catch (RuntimeException var12) {
                     player.sendMessage("Exception during execution of  '" + fullCommand + "': " + var12.toString());
                     LOGGER.log(Level.WARNING, "Exception during execution of " + fullCommand, (Throwable)var12);
                  } finally {
                     long runtime = System.currentTimeMillis() - begin;
                     if (runtime > 5000L) {
                        player.sendMessage("The execution of '" + fullCommand + "' took " + TimeUtil.formatDuration(runtime) + ".");
                     }
                  }
               });
            }
         }
      }
   }

   @Override
   public int size() {
      return this._datatable.size();
   }

   public static AdminCommandHandler getInstance() {
      return AdminCommandHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final AdminCommandHandler INSTANCE = new AdminCommandHandler();
   }
}
