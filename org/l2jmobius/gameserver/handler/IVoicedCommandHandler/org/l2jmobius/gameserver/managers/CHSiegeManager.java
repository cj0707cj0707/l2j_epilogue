package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.ConquerableHallSiegeConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class CHSiegeManager {
   private static final Logger LOGGER = Logger.getLogger(CHSiegeManager.class.getName());
   private static final String SQL_LOAD_HALLS = "SELECT * FROM siegable_clanhall";
   private final Map<Integer, SiegableHall> _siegableHalls = new HashMap<>();

   protected CHSiegeManager() {
      this.loadClanHalls();
   }

   private void loadClanHalls() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM siegable_clanhall");
      ) {
         this._siegableHalls.clear();

         while (rs.next()) {
            int id = rs.getInt("clanHallId");
            StatSet set = new StatSet();
            set.set("id", id);
            set.set("name", rs.getString("name"));
            set.set("ownerId", rs.getInt("ownerId"));
            set.set("desc", rs.getString("desc"));
            set.set("location", rs.getString("location"));
            set.set("nextSiege", rs.getLong("nextSiege"));
            set.set("siegeLength", rs.getLong("siegeLength"));
            set.set("scheduleConfig", rs.getString("schedule_config"));
            SiegableHall hall = new SiegableHall(set);
            this._siegableHalls.put(id, hall);
            ClanHallTable.addClanHall(hall);
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._siegableHalls.size() + " conquerable clan halls.");
      } catch (Exception var13) {
         LOGGER.warning("CHSiegeManager: Could not load siegable clan halls!:" + var13.getMessage());
      }
   }

   public Map<Integer, SiegableHall> getConquerableHalls() {
      return this._siegableHalls;
   }

   public SiegableHall getSiegableHall(int clanHall) {
      return this._siegableHalls.get(clanHall);
   }

   public SiegableHall getNearbyClanHall(Creature creature) {
      return this.getNearbyClanHall(creature.getX(), creature.getY(), 10000);
   }

   public SiegableHall getNearbyClanHall(int x, int y, int maxDist) {
      ClanHallZone zone = null;

      for (Entry<Integer, SiegableHall> ch : this._siegableHalls.entrySet()) {
         ClanHallZone var7 = ch.getValue().getZone();
         if (var7 != null && var7.getDistanceToZone(x, y) < maxDist) {
            return ch.getValue();
         }
      }

      return null;
   }

   public ClanHallSiegeEngine getSiege(Creature creature) {
      SiegableHall hall = this.getNearbyClanHall(creature);
      return hall == null ? null : hall.getSiege();
   }

   public void registerClan(Clan clan, SiegableHall hall, Player player) {
      if (clan.getLevel() < ConquerableHallSiegeConfig.CHS_CLAN_MINLEVEL) {
         player.sendMessage("Only clans of level " + ConquerableHallSiegeConfig.CHS_CLAN_MINLEVEL + " or higher may register for a castle siege");
      } else if (hall.isWaitingBattle()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
         sm.addString(hall.getName());
         player.sendPacket(sm);
      } else if (hall.isInSiege()) {
         player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
      } else if (hall.getOwnerId() == clan.getId()) {
         player.sendPacket(SystemMessageId.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
      } else if (clan.getCastleId() == 0 && clan.getHideoutId() == 0) {
         if (hall.getSiege().checkIsAttacker(clan)) {
            player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
         } else if (this.isClanParticipating(clan)) {
            player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
         } else if (hall.getSiege().getAttackers().size() >= ConquerableHallSiegeConfig.CHS_MAX_ATTACKERS) {
            player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
         } else {
            hall.addAttacker(clan);
         }
      } else {
         player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
      }
   }

   public void unRegisterClan(Clan clan, SiegableHall hall) {
      if (hall.isRegistering()) {
         hall.removeAttacker(clan);
      }
   }

   public boolean isClanParticipating(Clan clan) {
      for (SiegableHall hall : this._siegableHalls.values()) {
         if (hall.getSiege() != null && hall.getSiege().checkIsAttacker(clan)) {
            return true;
         }
      }

      return false;
   }

   public void onServerShutDown() {
      for (SiegableHall hall : this._siegableHalls.values()) {
         if (hall.getSiege() != null) {
            hall.getSiege().saveAttackers();
         }
      }
   }

   public static CHSiegeManager getInstance() {
      return CHSiegeManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CHSiegeManager INSTANCE = new CHSiegeManager();
   }
}
