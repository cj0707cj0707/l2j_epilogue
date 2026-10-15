package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.clan.ClanPrivileges;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.residences.ClanHallAuction;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanCreate;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.clan.OnClanWarFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.clan.OnClanWarStart;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ClanTable {
   private static final Logger LOGGER = Logger.getLogger(ClanTable.class.getName());
   private final Map<Integer, Clan> _clans = new ConcurrentHashMap<>();

   protected ClanTable() {
      if (GeneralConfig.ENABLE_COMMUNITY_BOARD) {
         ForumsBBSManager.getInstance().initRoot();
      }

      List<Integer> cids = new ArrayList<>();

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT clan_id FROM clan_data");
      ) {
         while (rs.next()) {
            cids.add(rs.getInt("clan_id"));
         }
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "Error restoring ClanTable.", (Throwable)var13);
      }

      for (int cid : cids) {
         Clan clan = new Clan(cid);
         this._clans.put(cid, clan);
         if (clan.getDissolvingExpiryTime() != 0L) {
            this.scheduleRemoveClan(clan.getId());
         }
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Restored " + cids.size() + " clans from the database.");
      this.allianceCheck();
      this.restoreClanWars();
      ThreadPool.scheduleAtFixedRate(this::updateClanRanks, 1000L, 1200000L);
   }

   public Collection<Clan> getClans() {
      return this._clans.values();
   }

   public int getClanCount() {
      return this._clans.size();
   }

   public Clan getClan(int clanId) {
      return this._clans.get(clanId);
   }

   public Clan getClanByName(String clanName) {
      for (Clan clan : this._clans.values()) {
         if (clan.getName().equalsIgnoreCase(clanName)) {
            return clan;
         }
      }

      return null;
   }

   public Clan createClan(Player player, String clanName) {
      if (player == null) {
         return null;
      } else if (10 > player.getLevel()) {
         player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_CRITERIA_IR_ORDER_TO_CREATE_A_CLAN);
         return null;
      } else if (0 != player.getClanId()) {
         player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_CREATE_A_CLAN);
         return null;
      } else if (System.currentTimeMillis() < player.getClanCreateExpiryTime()) {
         player.sendPacket(SystemMessageId.YOU_MUST_WAIT_10_DAYS_BEFORE_CREATING_A_NEW_CLAN);
         return null;
      } else if (!StringUtil.isAlphaNumeric(clanName) || 2 > clanName.length()) {
         player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
         return null;
      } else if (16 < clanName.length()) {
         player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
         return null;
      } else if (null != this.getClanByName(clanName)) {
         SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
         sm.addString(clanName);
         player.sendPacket(sm);
         return null;
      } else {
         Clan clan = new Clan(IdManager.getInstance().getNextId(), clanName);
         ClanMember leader = new ClanMember(clan, player);
         clan.setLeader(leader);
         leader.setPlayer(player);
         clan.store();
         player.setClan(clan);
         player.setPledgeClass(ClanMember.calculatePledgeClass(player));
         ClanPrivileges privileges = new ClanPrivileges();
         privileges.enableAll();
         player.setClanPrivileges(privileges);
         this._clans.put(clan.getId(), clan);
         player.sendPacket(new PledgeShowInfoUpdate(clan));
         player.sendPacket(new PledgeShowMemberListAll(clan, player));
         player.updateUserInfo();
         player.sendPacket(new PledgeShowMemberListUpdate(player));
         player.sendPacket(SystemMessageId.YOUR_CLAN_HAS_BEEN_CREATED);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_CREATE)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanCreate(player, clan));
         }

         return clan;
      }
   }

   public synchronized void destroyClan(int clanId) {
      Clan clan = this.getClan(clanId);
      if (clan != null) {
         clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));
         int castleId = clan.getCastleId();
         if (castleId == 0) {
            for (Siege siege : SiegeManager.getInstance().getSieges()) {
               siege.removeSiegeClan(clan);
            }
         }

         int fortId = clan.getFortId();
         if (fortId == 0) {
            for (FortSiege siege : FortSiegeManager.getInstance().getSieges()) {
               siege.removeAttacker(clan);
            }
         }

         int hallId = clan.getHideoutId();
         if (hallId == 0) {
            for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values()) {
               hall.removeAttacker(clan);
            }
         }

         ClanHallAuction auction = ClanHallAuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());
         if (auction != null) {
            auction.cancelBid(clan.getId());
         }

         ClanMember leaderMember = clan.getLeader();
         clan.getWarehouse().destroyAllItems(ItemProcessType.DESTROY, leaderMember == null ? null : clan.getLeader().getPlayer(), null);

         for (ClanMember member : clan.getMembers()) {
            clan.removeClanMember(member.getObjectId(), 0L);
         }

         this._clans.remove(clanId);
         IdManager.getInstance().releaseId(clanId);

         try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?")) {
               ps.setInt(1, clanId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?")) {
               ps.setInt(1, clanId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?")) {
               ps.setInt(1, clanId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?")) {
               ps.setInt(1, clanId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?")) {
               ps.setInt(1, clanId);
               ps.setInt(2, clanId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_notices WHERE clan_id=?")) {
               ps.setInt(1, clanId);
               ps.execute();
            }

            if (castleId != 0) {
               try (PreparedStatement ps = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?")) {
                  ps.setInt(1, castleId);
                  ps.execute();
               }
            }

            if (fortId != 0) {
               Fort fort = FortManager.getInstance().getFortById(fortId);
               if (fort != null && clan == fort.getOwnerClan()) {
                  fort.removeOwner(true);
               }
            }

            if (hallId != 0) {
               SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
               if (hall != null && hall.getOwnerId() == clanId) {
                  hall.free();
               }
            }
         } catch (Exception var28) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error removing clan from DB.", (Throwable)var28);
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_DESTROY)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanDestroy(leaderMember, clan));
         }
      }
   }

   public void scheduleRemoveClan(int clanId) {
      ThreadPool.schedule(() -> {
         if (this.getClan(clanId) != null) {
            if (this.getClan(clanId).getDissolvingExpiryTime() != 0L) {
               this.destroyClan(clanId);
            }
         }
      }, Math.max(this.getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis(), 300000L));
   }

   public boolean isAllyExists(String allyName) {
      for (Clan clan : this._clans.values()) {
         if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName)) {
            return true;
         }
      }

      return false;
   }

   public void storeClanWars(int clanId1, int clanId2) {
      Clan clan1 = this.getClan(clanId1);
      Clan clan2 = this.getClan(clanId2);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_CLAN_WAR_START)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnClanWarStart(clan1, clan2));
      }

      clan1.setEnemyClan(clan2);
      clan2.setAttackerClan(clan1);
      clan1.broadcastClanStatus();
      clan2.broadcastClanStatus();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES(?,?,?,?)");
      ) {
         ps.setInt(1, clanId1);
         ps.setInt(2, clanId2);
         ps.setInt(3, 0);
         ps.setInt(4, 0);
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error storing clan wars data.", (Throwable)var13);
      }

      SystemMessage msg = new SystemMessage(
         SystemMessageId.A_CLAN_WAR_HAS_BEEN_DECLARED_AGAINST_THE_CLAN_S1_IF_YOU_ARE_KILLED_DURING_THE_CLAN_WAR_BY_MEMBERS_OF_THE_OPPOSING_CLAN_YOU_WILL_ONLY_LOSE_A_QUARTER_OF_THE_NORMAL_EXPERIENCE_FROM_DEATH
      );
      msg.addString(clan2.getName());
      clan1.broadcastToOnlineMembers(msg);
      msg = new SystemMessage(SystemMessageId.THE_CLAN_S1_HAS_DECLARED_A_CLAN_WAR);
      msg.addString(clan1.getName());
      clan2.broadcastToOnlineMembers(msg);
   }

   public void deleteClanWars(int clanId1, int clanId2) {
      Clan clan1 = this.getClan(clanId1);
      Clan clan2 = this.getClan(clanId2);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_CLAN_WAR_FINISH)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnClanWarFinish(clan1, clan2));
      }

      clan1.deleteEnemyClan(clan2);
      clan2.deleteAttackerClan(clan1);
      clan1.broadcastClanStatus();
      clan2.broadcastClanStatus();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM clan_wars WHERE (clan1=? AND clan2=?) OR (clan2=? AND clan1=?)");
      ) {
         ps.setInt(1, clanId1);
         ps.setInt(2, clanId2);
         ps.setInt(3, clanId1);
         ps.setInt(4, clanId2);
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error removing clan wars data.", (Throwable)var13);
      }

      SystemMessage msg = new SystemMessage(SystemMessageId.THE_WAR_AGAINST_S1_CLAN_HAS_BEEN_STOPPED);
      msg.addString(clan2.getName());
      clan1.broadcastToOnlineMembers(msg);
      msg = new SystemMessage(SystemMessageId.THE_CLAN_S1_HAS_DECIDED_TO_STOP_THE_WAR);
      msg.addString(clan1.getName());
      clan2.broadcastToOnlineMembers(msg);
   }

   public void checkSurrender(Clan clan1, Clan clan2) {
      int count = 0;

      for (ClanMember member : clan1.getMembers()) {
         if (member != null && member.getPlayer().getWantsPeace() == 1) {
            count++;
         }
      }

      if (count == clan1.getMembers().size() - 1) {
         clan1.deleteEnemyClan(clan2);
         clan2.deleteEnemyClan(clan1);
         this.deleteClanWars(clan1.getId(), clan2.getId());
      }
   }

   private void restoreClanWars() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement statement = con.createStatement();
         ResultSet rset = statement.executeQuery("SELECT clan1, clan2 FROM clan_wars");
      ) {
         while (rset.next()) {
            Clan clan1 = this.getClan(rset.getInt("clan1"));
            Clan clan2 = this.getClan(rset.getInt("clan2"));
            if (clan1 != null && clan2 != null) {
               clan1.setEnemyClan(rset.getInt("clan2"));
               clan2.setAttackerClan(rset.getInt("clan1"));
            } else {
               LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": restorewars one of clans is null clan1:" + clan1 + " clan2:" + clan2);
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error restoring clan wars data.", (Throwable)var14);
      }
   }

   private void allianceCheck() {
      for (Clan clan : this._clans.values()) {
         int allyId = clan.getAllyId();
         if (allyId != 0 && clan.getId() != allyId && !this._clans.containsKey(allyId)) {
            clan.setAllyId(0);
            clan.setAllyName(null);
            clan.changeAllyCrest(0, true);
            clan.updateClanInDB();
            LOGGER.info(this.getClass().getSimpleName() + ": Removed alliance from clan: " + clan);
         }
      }
   }

   public List<Clan> getClanAllies(int allianceId) {
      List<Clan> clanAllies = new ArrayList<>();
      if (allianceId != 0) {
         for (Clan clan : this._clans.values()) {
            if (clan != null && clan.getAllyId() == allianceId) {
               clanAllies.add(clan);
            }
         }
      }

      return clanAllies;
   }

   public void shutdown() {
      for (Clan clan : this._clans.values()) {
         clan.updateClanInDB();
      }
   }

   private void updateClanRanks() {
      for (Clan clan : this._clans.values()) {
         clan.setRank(this.getClanRank(clan));
      }
   }

   public int getClanRank(Clan clan) {
      if (clan.getLevel() < 3) {
         return 0;
      } else {
         int rank = 1;

         for (Clan c : this._clans.values()) {
            if (clan != c && (clan.getLevel() < c.getLevel() || clan.getLevel() == c.getLevel() && clan.getReputationScore() <= c.getReputationScore())) {
               rank++;
            }
         }

         return rank;
      }
   }

   public static ClanTable getInstance() {
      return ClanTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ClanTable INSTANCE = new ClanTable();
   }
}
