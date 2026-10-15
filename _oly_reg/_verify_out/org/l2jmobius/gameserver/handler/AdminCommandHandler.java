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
import org.l2jmobius.gameserver.handler.AdminCommandHandler.SingletonHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.util.GMAudit;

public class AdminCommandHandler implements IHandler<IAdminCommandHandler, String> {
   private static final Logger LOGGER = Logger.getLogger(AdminCommandHandler.class.getName());
   private final Map<String, IAdminCommandHandler> _datatable = new HashMap<>();

   protected AdminCommandHandler() {
   }

   public void registerHandler(IAdminCommandHandler var1) {
      for (String var5 : var1.getCommandList()) {
         this._datatable.put(var5, var1);
      }
   }

   public synchronized void removeHandler(IAdminCommandHandler var1) {
      this._datatable.remove(var1);
   }

   public IAdminCommandHandler getHandler(String var1) {
      String var2 = var1;
      if (var1.contains(" ")) {
         var2 = var1.substring(0, var1.indexOf(32));
      }

      return this._datatable.get(var2);
   }

   public void onCommand(Player var1, String var2, boolean var3) {
      String var4 = var2.split(" ")[0];
      if (var1.isGM() || AdminData.getInstance().hasAccess(var4, var1.getAccessLevel())) {
         String var5 = var4.substring(6);
         IAdminCommandHandler var6 = this.getHandler(var4);
         if (var6 == null) {
            var1.sendMessage("The command '" + var5 + "' does not exist!");
            LOGGER.warning("No handler registered for admin command '" + var4 + "'");
         } else if (!AdminData.getInstance().hasAccess(var4, var1.getAccessLevel())) {
            var1.sendMessage("You don't have the access rights to use this command!");
            LOGGER.warning(var1 + " tried to use admin command '" + var4 + "', without proper access level!");
         } else if (var3 && AdminData.getInstance().requireConfirm(var4)) {
            var1.setAdminConfirmCmd(var2);
            ConfirmDlg var7 = new ConfirmDlg(SystemMessageId.S1_3);
            var7.getSystemMessage().addString("Are you sure you want execute command '" + var5 + "' ?");
            var1.addAction(PlayerAction.ADMIN_COMMAND);
            var1.sendPacket(var7);
         } else {
            ThreadPool.execute(() -> {
               long var3x = System.currentTimeMillis();

               try {
                  if (GeneralConfig.GMAUDIT) {
                     WorldObject var5x = var1.getTarget();
                     GMAudit.logAction(var1.getName() + " [" + var1.getObjectId() + "]", var2, var5x != null ? var5x.getName() : "no-target");
                  }

                  var6.onCommand(var2, var1);
               } catch (RuntimeException var12) {
                  var1.sendMessage("Exception during execution of  '" + var2 + "': " + var12.toString());
                  LOGGER.log(Level.WARNING, "Exception during execution of " + var2, (Throwable)var12);
               } finally {
                  long var8 = System.currentTimeMillis() - var3x;
                  if (var8 > 5000L) {
                     var1.sendMessage("The execution of '" + var2 + "' took " + TimeUtil.formatDuration(var8) + ".");
                  }
               }
            });
         }
      }
   }

   public int size() {
      return this._datatable.size();
   }

   public static AdminCommandHandler getInstance() {
      return SingletonHolder.INSTANCE;
   }
}
