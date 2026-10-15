package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.announce.Announcement;
import org.l2jmobius.gameserver.mechanics.announce.AnnouncementType;
import org.l2jmobius.gameserver.mechanics.announce.AutoAnnouncement;
import org.l2jmobius.gameserver.mechanics.announce.IAnnouncement;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class AnnouncementsTable {
   private static final Logger LOGGER = Logger.getLogger(AnnouncementsTable.class.getName());
   private final Map<Integer, IAnnouncement> _announcements = new ConcurrentSkipListMap<>();

   protected AnnouncementsTable() {
      this.load();
   }

   private void load() {
      this._announcements.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement st = con.createStatement();
         ResultSet rset = st.executeQuery("SELECT * FROM announcements");
      ) {
         while (rset.next()) {
            AnnouncementType type = AnnouncementType.findById(rset.getInt("type"));
            Announcement announce;
            switch (type) {
               case NORMAL:
               case CRITICAL:
                  announce = new Announcement(rset);
                  break;
               case AUTO_NORMAL:
               case AUTO_CRITICAL:
                  announce = new AutoAnnouncement(rset);
                  break;
               default:
                  continue;
            }

            this._announcements.put(announce.getId(), announce);
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Failed loading announcements:", (Throwable)var12);
      }
   }

   public void showAnnouncements(Player player) {
      this.sendAnnouncements(player, AnnouncementType.NORMAL);
      this.sendAnnouncements(player, AnnouncementType.CRITICAL);
      this.sendAnnouncements(player, AnnouncementType.EVENT);
   }

   private void sendAnnouncements(Player player, AnnouncementType type) {
      for (IAnnouncement announce : this._announcements.values()) {
         if (announce.isValid() && announce.getType() == type) {
            player.sendPacket(
               new CreatureSay(
                  null, type == AnnouncementType.CRITICAL ? ChatType.CRITICAL_ANNOUNCE : ChatType.ANNOUNCEMENT, player.getName(), announce.getContent()
               )
            );
         }
      }
   }

   public void addAnnouncement(IAnnouncement announce) {
      if (announce.storeMe()) {
         this._announcements.put(announce.getId(), announce);
      }
   }

   public boolean deleteAnnouncement(int id) {
      IAnnouncement announce = this._announcements.remove(id);
      return announce != null && announce.deleteMe();
   }

   public IAnnouncement getAnnounce(int id) {
      return this._announcements.get(id);
   }

   public Collection<IAnnouncement> getAllAnnouncements() {
      return this._announcements.values();
   }

   public static AnnouncementsTable getInstance() {
      return AnnouncementsTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final AnnouncementsTable INSTANCE = new AnnouncementsTable();
   }
}
