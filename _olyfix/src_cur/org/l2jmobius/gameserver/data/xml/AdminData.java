package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.holders.AccessRight;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class AdminData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(AdminData.class.getName());
   private final Map<Integer, AccessLevel> _accessLevels = new HashMap<>();
   private final Map<String, AccessRight> _adminCommandAccessRights = new LinkedHashMap<>();
   private final Map<Player, Boolean> _gmList = new ConcurrentHashMap<>();
   private int _highestLevel = 0;

   protected AdminData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._accessLevels.clear();
      this._adminCommandAccessRights.clear();
      this.parseDatapackFile("config/AccessLevels.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._accessLevels.size() + " access levels.");
      this.parseDatapackFile("config/AdminCommands.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._adminCommandAccessRights.size() + " access commands.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, n -> {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            this.forEach(n, d -> {
               String s0$ = d.getNodeName();
               switch (s0$) {
                  case "access": {
                     StatSet set = new StatSet(this.parseAttributes(d));
                     AccessLevel level = new AccessLevel(set);
                     if (level.getLevel() > this._highestLevel) {
                        this._highestLevel = level.getLevel();
                     }

                     this._accessLevels.put(level.getLevel(), level);
                     break;
                  }
                  case "admin": {
                     StatSet set = new StatSet(this.parseAttributes(d));
                     AccessRight command = new AccessRight(set);
                     this._adminCommandAccessRights.put(command.getCommand(), command);
                  }
               }
            });
         }
      });
   }

   public AccessLevel getAccessLevel(int accessLevel) {
      return accessLevel < 0 ? this._accessLevels.get(-1) : this._accessLevels.computeIfAbsent(accessLevel, k -> new AccessLevel());
   }

   public AccessLevel getMasterAccessLevel() {
      return this._accessLevels.get(this._highestLevel);
   }

   public boolean hasAccessLevel(int accessLevel) {
      return this._accessLevels.containsKey(accessLevel);
   }

   public boolean hasAccess(String adminCommand, AccessLevel accessLevel) {
      AccessRight accessRight = this._adminCommandAccessRights.get(adminCommand);
      if (accessRight == null) {
         if (accessLevel.getLevel() <= 0 || accessLevel.getLevel() != this._highestLevel) {
            LOGGER.info(this.getClass().getSimpleName() + ": No rights defined for admin command " + adminCommand + " !");
            return false;
         }

         accessRight = new AccessRight(adminCommand, "", true, accessLevel.getLevel());
         this._adminCommandAccessRights.put(accessRight.getCommand(), accessRight);
         LOGGER.info(
            this.getClass().getSimpleName()
               + ": No rights defined for admin command "
               + adminCommand
               + " auto setting accesslevel: "
               + accessLevel.getLevel()
               + " !"
         );
      }

      return accessRight.hasAccess(accessLevel);
   }

   public boolean requireConfirm(String command) {
      AccessRight accessRight = this._adminCommandAccessRights.get(command);
      if (accessRight == null) {
         LOGGER.info(this.getClass().getSimpleName() + ": No rights defined for admin command " + command + ".");
         return false;
      } else {
         return accessRight.requireConfirm();
      }
   }

   public Collection<Player> getAllGms(boolean includeHidden) {
      List<Player> result = new ArrayList<>();

      for (Entry<Player, Boolean> entry : this._gmList.entrySet()) {
         if (includeHidden || !entry.getValue()) {
            result.add(entry.getKey());
         }
      }

      return result;
   }

   public Collection<String> getAllGmNames(boolean includeHidden) {
      List<String> result = new ArrayList<>();

      for (Entry<Player, Boolean> entry : this._gmList.entrySet()) {
         if (!entry.getValue()) {
            result.add(entry.getKey().getName());
         } else if (includeHidden) {
            result.add(entry.getKey().getName() + " (invis)");
         }
      }

      return result;
   }

   public void addGm(Player player, boolean hidden) {
      this._gmList.put(player, hidden);
   }

   public void deleteGm(Player player) {
      this._gmList.remove(player);
   }

   public boolean isGmOnline(boolean includeHidden) {
      for (Entry<Player, Boolean> entry : this._gmList.entrySet()) {
         if (includeHidden || !entry.getValue()) {
            return true;
         }
      }

      return false;
   }

   public void sendListToPlayer(Player player) {
      if (this.isGmOnline(player.isGM())) {
         player.sendPacket(SystemMessageId.GM_LIST);

         for (String name : this.getAllGmNames(player.isGM())) {
            SystemMessage sm = new SystemMessage(SystemMessageId.GM_C1);
            sm.addString(name);
            player.sendPacket(sm);
         }
      } else {
         player.sendPacket(SystemMessageId.THERE_ARE_NO_GMS_CURRENTLY_VISIBLE_IN_THE_PUBLIC_LIST_AS_THEY_MAY_BE_PERFORMING_OTHER_FUNCTIONS_AT_THE_MOMENT);
      }
   }

   public void broadcastToGMs(ServerPacket packet) {
      for (Player player : this.getAllGms(true)) {
         player.sendPacket(packet);
      }
   }

   public void broadcastMessageToGMs(String message) {
      for (Player player : this.getAllGms(true)) {
         player.sendMessage(message);
      }
   }

   public Collection<AccessRight> getAdminCommandAccessRights() {
      return this._adminCommandAccessRights.values();
   }

   public static AdminData getInstance() {
      return AdminData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final AdminData INSTANCE = new AdminData();
   }
}
