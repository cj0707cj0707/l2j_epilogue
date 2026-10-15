package org.l2jmobius.gameserver.entity.clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.ClanWarehouse;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanJoin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanLeaderChange;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanLeft;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan.OnPlayerClanLvlUp;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExSubPledgeSkillAdd;
import org.l2jmobius.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillListAdd;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Clan {
   private static final Logger LOGGER = Logger.getLogger(Clan.class.getName());
   private static final String INSERT_CLAN_DATA = "INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,blood_alliance_count,blood_oath_count,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,new_leader_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
   private static final String SELECT_CLAN_DATA = "SELECT * FROM clan_data where clan_id=?";
   public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
   public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
   public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
   public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
   public static final int SUBUNIT_ACADEMY = -1;
   public static final int SUBUNIT_ROYAL1 = 100;
   public static final int SUBUNIT_ROYAL2 = 200;
   public static final int SUBUNIT_KNIGHT1 = 1001;
   public static final int SUBUNIT_KNIGHT2 = 1002;
   public static final int SUBUNIT_KNIGHT3 = 2001;
   public static final int SUBUNIT_KNIGHT4 = 2002;
   private String _name;
   private int _clanId;
   private ClanMember _leader;
   private final Map<Integer, ClanMember> _members = new ConcurrentHashMap<>();
   private String _allyName;
   private int _allyId = 0;
   private int _level;
   private int _castleId;
   private int _fortId;
   private int _hideoutId;
   private int _hiredGuards;
   private int _crestId;
   private int _crestLargeId;
   private int _allyCrestId;
   private int _auctionBiddedAt = 0;
   private long _allyPenaltyExpiryTime;
   private int _allyPenaltyType;
   private long _charPenaltyExpiryTime;
   private long _dissolvingExpiryTime;
   private int _bloodAllianceCount;
   private int _bloodOathCount;
   private final ItemContainer _warehouse = new ClanWarehouse(this);
   private final Set<Integer> _atWarWith = ConcurrentHashMap.newKeySet();
   private final Set<Integer> _atWarAttackers = ConcurrentHashMap.newKeySet();
   private Forum _forum;
   private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
   private final Map<Integer, Clan.RankPrivs> _privs = new ConcurrentHashMap<>();
   private final Map<Integer, Clan.SubPledge> _subPledges = new ConcurrentHashMap<>();
   private final Map<Integer, Skill> _subPledgeSkills = new ConcurrentHashMap<>();
   private int _reputationScore = 0;
   private int _rank = 0;
   private String _notice;
   private boolean _noticeEnabled = false;
   private static final int MAX_NOTICE_LENGTH = 8192;
   private int _newLeaderId;
   private final AtomicInteger _siegeKills = new AtomicInteger();
   private final AtomicInteger _siegeDeaths = new AtomicInteger();

   public Clan(int clanId) {
      this._clanId = clanId;
      this.initializePrivs();
      this.restore();
      this._warehouse.restore();
   }

   public Clan(int clanId, String clanName) {
      this._clanId = clanId;
      this._name = clanName;
      this.initializePrivs();
   }

   public int getId() {
      return this._clanId;
   }

   public void setClanId(int clanId) {
      this._clanId = clanId;
   }

   public int getLeaderId() {
      return this._leader != null ? this._leader.getObjectId() : 0;
   }

   public ClanMember getLeader() {
      return this._leader;
   }

   public void setLeader(ClanMember leader) {
      this._leader = leader;
      this._members.put(leader.getObjectId(), leader);
   }

   public void setNewLeader(ClanMember member) {
      Player newLeader = member.getPlayer();
      ClanMember exMember = this._leader;
      Player exLeader = exMember.getPlayer();
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEADER_CHANGE)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLeaderChange(exMember, member, this));
      }

      if (exLeader != null) {
         if (exLeader.isFlying()) {
            exLeader.dismount();
         }

         if (this.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) {
            SiegeManager.getInstance().removeSiegeSkills(exLeader);
         }

         exLeader.getClanPrivileges().disableAll();
         exLeader.broadcastUserInfo();
      } else {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?");
         ) {
            ps.setInt(1, 0);
            ps.setInt(2, this.getLeaderId());
            ps.execute();
         } catch (Exception var18) {
            LOGGER.log(Level.WARNING, "Couldn't update clan privs for old clan leader", (Throwable)var18);
         }
      }

      this.setLeader(member);
      if (this._newLeaderId != 0) {
         this.setNewLeaderId(0, true);
      }

      this.updateClanInDB();
      if (exLeader != null) {
         exLeader.setPledgeClass(ClanMember.calculatePledgeClass(exLeader));
         exLeader.broadcastUserInfo();
         exLeader.checkItemRestriction();
      }

      if (newLeader != null) {
         newLeader.setPledgeClass(ClanMember.calculatePledgeClass(newLeader));
         newLeader.getClanPrivileges().enableAll();
         if (this.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) {
            SiegeManager.getInstance().addSiegeSkills(newLeader);
         }

         newLeader.broadcastUserInfo();
      } else {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET clan_privs = ? WHERE charId = ?");
         ) {
            ps.setInt(1, ClanPrivileges.getCompleteMask());
            ps.setInt(2, this.getLeaderId());
            ps.execute();
         } catch (Exception var15) {
            LOGGER.log(Level.WARNING, "Couldn't update clan privs for new clan leader", (Throwable)var15);
         }
      }

      this.broadcastClanStatus();
      this.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LORD_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_C1).addString(member.getName()));
      LOGGER.log(Level.INFO, "Leader of Clan: " + this.getName() + " changed to: " + member.getName() + " ex leader: " + exMember.getName());
   }

   public String getLeaderName() {
      if (this._leader == null) {
         LOGGER.warning(Clan.class.getName() + ": Clan " + this.getName() + " without clan leader!");
         return "";
      } else {
         return this._leader.getName();
      }
   }

   public String getName() {
      return this._name;
   }

   public void setName(String name) {
      this._name = name;
   }

   private void addClanMember(ClanMember member) {
      this._members.put(member.getObjectId(), member);
   }

   public void addClanMember(Player player) {
      ClanMember member = new ClanMember(this, player);
      member.setPlayer(player);
      this.addClanMember(member);
      player.setClan(this);
      player.setPledgeClass(ClanMember.calculatePledgeClass(player));
      player.sendPacket(new PledgeShowMemberListUpdate(player));
      player.sendPacket(new PledgeSkillList(this));
      this.addSkillEffects(player);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_JOIN)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanJoin(member, this));
      }
   }

   public void updateClanMember(Player player) {
      ClanMember member = new ClanMember(player.getClan(), player);
      if (player.isClanLeader()) {
         this.setLeader(member);
      }

      this.addClanMember(member);
   }

   public ClanMember getClanMember(String name) {
      for (ClanMember temp : this._members.values()) {
         if (temp.getName().equals(name)) {
            return temp;
         }
      }

      return null;
   }

   public ClanMember getClanMember(int objectId) {
      return this._members.get(objectId);
   }

   public void removeClanMember(int objectId, long clanJoinExpiryTime) {
      ClanMember exMember = this._members.remove(objectId);
      if (exMember == null) {
         LOGGER.warning("Member Object ID: " + objectId + " not found in clan while trying to remove");
      } else {
         int leadssubpledge = this.getLeaderSubPledge(objectId);
         if (leadssubpledge != 0) {
            this.getSubPledge(leadssubpledge).setLeaderId(0);
            this.updateSubPledgeInDB(leadssubpledge);
         }

         if (exMember.getApprentice() != 0) {
            ClanMember apprentice = this.getClanMember(exMember.getApprentice());
            if (apprentice != null) {
               if (apprentice.getPlayer() != null) {
                  apprentice.getPlayer().setSponsor(0);
               } else {
                  apprentice.setApprenticeAndSponsor(0, 0);
               }

               apprentice.saveApprenticeAndSponsor(0, 0);
            }
         }

         if (exMember.getSponsor() != 0) {
            ClanMember sponsor = this.getClanMember(exMember.getSponsor());
            if (sponsor != null) {
               if (sponsor.getPlayer() != null) {
                  sponsor.getPlayer().setApprentice(0);
               } else {
                  sponsor.setApprenticeAndSponsor(0, 0);
               }

               sponsor.saveApprenticeAndSponsor(0, 0);
            }
         }

         exMember.saveApprenticeAndSponsor(0, 0);
         if (PlayerConfig.REMOVE_CASTLE_CIRCLETS) {
            CastleManager.getInstance().removeCirclet(exMember, this.getCastleId());
         }

         Player player = exMember.getPlayer();
         if (player != null) {
            if (!player.isNoble()) {
               player.setTitle("");
            }

            player.setApprentice(0);
            player.setSponsor(0);
            if (player.isClanLeader()) {
               SiegeManager.getInstance().removeSiegeSkills(player);
               player.setClanCreateExpiryTime(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(PlayerConfig.ALT_CLAN_CREATE_DAYS));
            }

            this.removeSkillEffects(player);
            if (this.getCastleId() > 0) {
               Castle castle = CastleManager.getInstance().getCastleByOwner(this);
               if (castle != null) {
                  castle.removeResidentialSkills(player);
               }
            }

            if (this.getFortId() > 0) {
               Fort fort = FortManager.getInstance().getFortByOwner(this);
               if (fort != null) {
                  fort.removeResidentialSkills(player);
               }
            }

            player.sendSkillList();
            player.setClan(null);
            if (exMember.getPledgeType() != -1) {
               player.setClanJoinExpiryTime(clanJoinExpiryTime);
            }

            player.setPledgeClass(ClanMember.calculatePledgeClass(player));
            player.broadcastUserInfo();
            player.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
         } else {
            this.removeMemberInDatabase(
               exMember.getObjectId(),
               clanJoinExpiryTime,
               this.getLeaderId() == objectId ? System.currentTimeMillis() + TimeUnit.DAYS.toMillis(PlayerConfig.ALT_CLAN_CREATE_DAYS) : 0L
            );
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEFT)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLeft(exMember, this));
         }
      }
   }

   public Collection<ClanMember> getMembers() {
      return this._members.values();
   }

   public int getMembersCount() {
      return this._members.size();
   }

   public int getSubPledgeMembersCount(int subpl) {
      int result = 0;

      for (ClanMember temp : this._members.values()) {
         if (temp.getPledgeType() == subpl) {
            result++;
         }
      }

      return result;
   }

   public int getMaxNrOfMembers(int pledgeType) {
      int limit = 0;
      switch (pledgeType) {
         case -1:
            limit = 20;
            break;
         case 0:
            switch (this._level) {
               case 0:
                  int limitxxxx = 10;
                  return limitxxxx;
               case 1:
                  int limitxxx = 15;
                  return limitxxx;
               case 2:
                  int limitxx = 20;
                  return limitxx;
               case 3:
                  int limitx = 30;
                  return limitx;
               default:
                  int limitxxxxx = 40;
                  return limitxxxxx;
            }
         case 100:
         case 200:
            switch (this._level) {
               case 11:
                  int limitx = 30;
                  return limitx;
               default:
                  int limitxx = 20;
                  return limitxx;
            }
         case 1001:
         case 1002:
         case 2001:
         case 2002:
            switch (this._level) {
               case 9:
               case 10:
               case 11:
                  limit = 25;
                  break;
               default:
                  limit = 10;
            }
      }

      return limit;
   }

   public List<Player> getOnlineMembers(int exclude) {
      List<Player> onlineMembers = new ArrayList<>();

      for (ClanMember temp : this._members.values()) {
         if (temp != null && temp.isOnline() && temp.getObjectId() != exclude) {
            onlineMembers.add(temp.getPlayer());
         }
      }

      return onlineMembers;
   }

   public int getOnlineMembersCount() {
      int count = 0;

      for (ClanMember temp : this._members.values()) {
         if (temp != null && temp.isOnline()) {
            count++;
         }
      }

      return count;
   }

   public int getAllyId() {
      return this._allyId;
   }

   public String getAllyName() {
      return this._allyName;
   }

   public void setAllyCrestId(int allyCrestId) {
      this._allyCrestId = allyCrestId;
   }

   public int getAllyCrestId() {
      return this._allyCrestId;
   }

   public int getLevel() {
      return this._level;
   }

   private void setLevel(int level) {
      this._level = level;
      if (this._level >= 2 && this._forum == null && GeneralConfig.ENABLE_COMMUNITY_BOARD) {
         Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
         if (forum != null) {
            this._forum = forum.getChildByName(this._name);
            if (this._forum == null) {
               this._forum = ForumsBBSManager.getInstance()
                  .createNewForum(this._name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), 2, 2, this.getId());
            }
         }
      }
   }

   public int getCastleId() {
      return this._castleId;
   }

   public int getFortId() {
      return this._fortId;
   }

   public int getHideoutId() {
      return this._hideoutId;
   }

   public void setCrestId(int crestId) {
      this._crestId = crestId;
   }

   public int getCrestId() {
      return this._crestId;
   }

   public void setCrestLargeId(int crestLargeId) {
      this._crestLargeId = crestLargeId;
   }

   public int getCrestLargeId() {
      return this._crestLargeId;
   }

   public void setAllyId(int allyId) {
      this._allyId = allyId;
   }

   public void setAllyName(String allyName) {
      this._allyName = allyName;
   }

   public void setCastleId(int castleId) {
      this._castleId = castleId;
   }

   public void setFortId(int fortId) {
      this._fortId = fortId;
   }

   public void setHideoutId(int hideoutId) {
      this._hideoutId = hideoutId;
   }

   public boolean isMember(int id) {
      return id != 0 && this._members.containsKey(id);
   }

   public int getBloodAllianceCount() {
      return this._bloodAllianceCount;
   }

   public void increaseBloodAllianceCount() {
      this._bloodAllianceCount = this._bloodAllianceCount + SiegeManager.getInstance().getBloodAllianceReward();
      this.updateBloodAllianceCountInDB();
   }

   public void resetBloodAllianceCount() {
      this._bloodAllianceCount = 0;
      this.updateBloodAllianceCountInDB();
   }

   public void updateBloodAllianceCountInDB() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET blood_alliance_count=? WHERE clan_id=?");
      ) {
         ps.setInt(1, this._bloodAllianceCount);
         ps.setInt(2, this._clanId);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception on updateBloodAllianceCountInDB(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public int getBloodOathCount() {
      return this._bloodOathCount;
   }

   public void increaseBloodOathCount() {
      this._bloodOathCount = this._bloodOathCount + FeatureConfig.FS_BLOOD_OATH_COUNT;
      this.updateBloodOathCountInDB();
   }

   public void resetBloodOathCount() {
      this._bloodOathCount = 0;
      this.updateBloodOathCountInDB();
   }

   public void updateBloodOathCountInDB() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET blood_oath_count=? WHERE clan_id=?");
      ) {
         ps.setInt(1, this._bloodOathCount);
         ps.setInt(2, this._clanId);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception on updateBloodAllianceCountInDB(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void updateClanInDB() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=?,new_leader_id=? WHERE clan_id=?"
         );
      ) {
         ps.setInt(1, this.getLeaderId());
         ps.setInt(2, this._allyId);
         ps.setString(3, this._allyName);
         ps.setInt(4, this._reputationScore);
         ps.setLong(5, this._allyPenaltyExpiryTime);
         ps.setInt(6, this._allyPenaltyType);
         ps.setLong(7, this._charPenaltyExpiryTime);
         ps.setLong(8, this._dissolvingExpiryTime);
         ps.setInt(9, this._newLeaderId);
         ps.setInt(10, this._clanId);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Error saving clan: " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void store() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,blood_alliance_count,blood_oath_count,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,new_leader_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?)"
         );
      ) {
         ps.setInt(1, this._clanId);
         ps.setString(2, this._name);
         ps.setInt(3, this._level);
         ps.setInt(4, this._castleId);
         ps.setInt(5, this._bloodAllianceCount);
         ps.setInt(6, this._bloodOathCount);
         ps.setInt(7, this._allyId);
         ps.setString(8, this._allyName);
         ps.setInt(9, this.getLeaderId());
         ps.setInt(10, this._crestId);
         ps.setInt(11, this._crestLargeId);
         ps.setInt(12, this._allyCrestId);
         ps.setInt(13, this._newLeaderId);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Error saving new clan: " + var9.getMessage(), (Throwable)var9);
      }
   }

   private void removeMemberInDatabase(int playerId, long clanJoinExpiryTime, long clanCreateExpiryTime) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement(
            "UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE charId=?"
         );
         PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
         PreparedStatement ps3 = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
      ) {
         ps1.setString(1, "");
         ps1.setLong(2, clanJoinExpiryTime);
         ps1.setLong(3, clanCreateExpiryTime);
         ps1.setInt(4, playerId);
         ps1.execute();
         ps2.setInt(1, playerId);
         ps2.execute();
         ps3.setInt(1, playerId);
         ps3.execute();
      } catch (Exception var20) {
         LOGGER.log(Level.SEVERE, "Error removing clan member: " + var20.getMessage(), (Throwable)var20);
      }
   }

   private void restore() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM clan_data where clan_id=?");
      ) {
         ps.setInt(1, this._clanId);

         try (ResultSet clanData = ps.executeQuery()) {
            if (clanData.next()) {
               this.setName(clanData.getString("clan_name"));
               this.setLevel(clanData.getInt("clan_level"));
               this.setCastleId(clanData.getInt("hasCastle"));
               this._bloodAllianceCount = clanData.getInt("blood_alliance_count");
               this._bloodOathCount = clanData.getInt("blood_oath_count");
               this.setAllyId(clanData.getInt("ally_id"));
               this.setAllyName(clanData.getString("ally_name"));
               this.setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
               if (this._allyPenaltyExpiryTime < System.currentTimeMillis()) {
                  this.setAllyPenaltyExpiryTime(0L, 0);
               }

               this.setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
               if (this._charPenaltyExpiryTime + PlayerConfig.ALT_CLAN_JOIN_DAYS * 86400000 < System.currentTimeMillis()) {
                  this.setCharPenaltyExpiryTime(0L);
               }

               this.setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));
               this.setCrestId(clanData.getInt("crest_id"));
               this.setCrestLargeId(clanData.getInt("crest_large_id"));
               this.setAllyCrestId(clanData.getInt("ally_crest_id"));
               this.setReputationScore(clanData.getInt("reputation_score"));
               this.setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);
               this.setNewLeaderId(clanData.getInt("new_leader_id"), false);
               int leaderId = clanData.getInt("leader_id");
               ps.clearParameters();

               try (PreparedStatement select = con.prepareStatement(
                     "SELECT char_name,level,classid,charId,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?"
                  )) {
                  select.setInt(1, this._clanId);

                  try (ResultSet clanMember = select.executeQuery()) {
                     ClanMember member = null;

                     while (clanMember.next()) {
                        member = new ClanMember(this, clanMember);
                        if (member.getObjectId() == leaderId) {
                           this.setLeader(member);
                        } else {
                           this.addClanMember(member);
                        }
                     }
                  }
               }
            }
         }

         this.restoreSubPledges();
         this.restoreRankPrivs();
         this.restoreSkills();
         this.restoreNotice();
      } catch (Exception var19) {
         LOGGER.log(Level.SEVERE, "Error restoring clan data: " + var19.getMessage(), (Throwable)var19);
      }
   }

   private void restoreNotice() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT enabled,notice FROM clan_notices WHERE clan_id=?");
      ) {
         ps.setInt(1, this._clanId);

         try (ResultSet noticeData = ps.executeQuery()) {
            while (noticeData.next()) {
               this._noticeEnabled = Boolean.parseBoolean(noticeData.getString("enabled"));
               this._notice = noticeData.getString("notice");
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Error restoring clan notice: " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void storeNotice(String noticeValue, boolean enabled) {
      String notice = noticeValue;
      if (noticeValue == null) {
         notice = "";
      }

      if (notice.length() > 8192) {
         notice = notice.substring(0, 8191);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO clan_notices (clan_id,notice,enabled) values (?,?,?) ON DUPLICATE KEY UPDATE notice=?,enabled=?"
         );
      ) {
         ps.setInt(1, this._clanId);
         ps.setString(2, notice);
         if (enabled) {
            ps.setString(3, "true");
         } else {
            ps.setString(3, "false");
         }

         ps.setString(4, notice);
         if (enabled) {
            ps.setString(5, "true");
         } else {
            ps.setString(5, "false");
         }

         ps.execute();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Error could not store clan notice: " + var12.getMessage(), (Throwable)var12);
      }

      this._notice = notice;
      this._noticeEnabled = enabled;
   }

   public void setNoticeEnabled(boolean enabled) {
      this.storeNotice(this.getNotice(), enabled);
   }

   public void setNotice(String notice) {
      this.storeNotice(notice, this._noticeEnabled);
   }

   public boolean isNoticeEnabled() {
      return this._noticeEnabled;
   }

   public String getNotice() {
      if (this._notice == null) {
         return "";
      } else {
         String text = this._notice.toLowerCase();
         return text.contains("action") && text.contains("bypass") ? "" : this._notice.replaceAll("<.*?>", "");
      }
   }

   private void restoreSkills() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT skill_id,skill_level,sub_pledge_id FROM clan_skills WHERE clan_id=?");
      ) {
         ps.setInt(1, this._clanId);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               int id = rset.getInt("skill_id");
               int level = rset.getInt("skill_level");
               Skill skill = SkillData.getInstance().getSkill(id, level);
               int subType = rset.getInt("sub_pledge_id");
               if (subType == -2) {
                  this._skills.put(skill.getId(), skill);
               } else if (subType == 0) {
                  this._subPledgeSkills.put(skill.getId(), skill);
               } else {
                  Clan.SubPledge subunit = this._subPledges.get(subType);
                  if (subunit != null) {
                     subunit.addNewSkill(skill);
                  } else {
                     LOGGER.info("Missing subpledge " + subType + " for clan " + this + ", skill skipped.");
                  }
               }
            }
         }
      } catch (Exception var15) {
         LOGGER.log(Level.SEVERE, "Error restoring clan skills: " + var15.getMessage(), (Throwable)var15);
      }
   }

   public Collection<Skill> getAllSkills() {
      return this._skills.values();
   }

   public Map<Integer, Skill> getSkills() {
      return this._skills;
   }

   public Skill addSkill(Skill newSkill) {
      return newSkill != null ? this._skills.put(newSkill.getId(), newSkill) : null;
   }

   public Skill addNewSkill(Skill newSkill) {
      return this.addNewSkill(newSkill, -2);
   }

   public Skill addNewSkill(Skill newSkill, int subType) {
      Skill oldSkill = null;
      if (newSkill != null) {
         if (subType == -2) {
            oldSkill = this._skills.put(newSkill.getId(), newSkill);
         } else if (subType == 0) {
            oldSkill = this._subPledgeSkills.put(newSkill.getId(), newSkill);
         } else {
            Clan.SubPledge subunit = this.getSubPledge(subType);
            if (subunit == null) {
               LOGGER.log(Level.WARNING, "Subpledge " + subType + " does not exist for clan " + this);
               return oldSkill;
            }

            oldSkill = subunit.addNewSkill(newSkill);
         }

         try (Connection con = DatabaseFactory.getConnection()) {
            if (oldSkill != null) {
               try (PreparedStatement ps = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?")) {
                  ps.setInt(1, newSkill.getLevel());
                  ps.setInt(2, oldSkill.getId());
                  ps.setInt(3, this._clanId);
                  ps.execute();
               }
            } else {
               try (PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name,sub_pledge_id) VALUES (?,?,?,?,?)"
                  )) {
                  ps.setInt(1, this._clanId);
                  ps.setInt(2, newSkill.getId());
                  ps.setInt(3, newSkill.getLevel());
                  ps.setString(4, newSkill.getName());
                  ps.setInt(5, subType);
                  ps.execute();
               }
            }
         } catch (Exception var14) {
            LOGGER.log(Level.WARNING, "Error could not store clan skills: " + var14.getMessage(), (Throwable)var14);
         }

         SystemMessage sm = new SystemMessage(SystemMessageId.THE_CLAN_SKILL_S1_HAS_BEEN_ADDED);
         sm.addSkillName(newSkill.getId());

         for (ClanMember temp : this._members.values()) {
            if (temp != null && temp.getPlayer() != null && temp.isOnline()) {
               if (subType == -2) {
                  if (newSkill.getMinPledgeClass() <= temp.getPlayer().getPledgeClass()) {
                     temp.getPlayer().addSkill(newSkill, false);
                     temp.getPlayer().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
                     temp.getPlayer().sendPacket(sm);
                     temp.getPlayer().sendSkillList();
                  }
               } else if (temp.getPledgeType() == subType) {
                  temp.getPlayer().addSkill(newSkill, false);
                  temp.getPlayer().sendPacket(new ExSubPledgeSkillAdd(subType, newSkill.getId(), newSkill.getLevel()));
                  temp.getPlayer().sendPacket(sm);
                  temp.getPlayer().sendSkillList();
               }
            }
         }
      }

      return oldSkill;
   }

   public void addSkillEffects() {
      for (Skill skill : this._skills.values()) {
         for (ClanMember temp : this._members.values()) {
            try {
               if (temp != null && temp.isOnline() && skill.getMinPledgeClass() <= temp.getPlayer().getPledgeClass()) {
                  temp.getPlayer().addSkill(skill, false);
               }
            } catch (NullPointerException var6) {
               LOGGER.log(Level.WARNING, var6.getMessage(), (Throwable)var6);
            }
         }
      }
   }

   public void addSkillEffects(Player player) {
      if (player != null) {
         int playerSocialClass = player.getPledgeClass() + 1;

         for (Skill skill : this._skills.values()) {
            SkillLearn skillLearn = SkillTreeData.getInstance().getPledgeSkill(skill.getId(), skill.getLevel());
            if (skillLearn == null || skillLearn.getSocialClass() == null || playerSocialClass >= skillLearn.getSocialClass().ordinal()) {
               player.addSkill(skill, false);
            }
         }

         if (player.getPledgeType() == 0) {
            for (Skill skillx : this._subPledgeSkills.values()) {
               SkillLearn skillLearn = SkillTreeData.getInstance().getSubPledgeSkill(skillx.getId(), skillx.getLevel());
               if (skillLearn == null || skillLearn.getSocialClass() == null || playerSocialClass >= skillLearn.getSocialClass().ordinal()) {
                  player.addSkill(skillx, false);
               }
            }
         } else {
            Clan.SubPledge subunit = this.getSubPledge(player.getPledgeType());
            if (subunit == null) {
               return;
            }

            for (Skill skillxx : subunit.getSkills()) {
               player.addSkill(skillxx, false);
            }
         }

         if (this._reputationScore < 0) {
            this.skillsStatus(player, true);
         }
      }
   }

   public void removeSkillEffects(Player player) {
      if (player != null) {
         for (Skill skill : this._skills.values()) {
            player.removeSkill(skill, false);
         }

         if (player.getPledgeType() == 0) {
            for (Skill skill : this._subPledgeSkills.values()) {
               player.removeSkill(skill, false);
            }
         } else {
            Clan.SubPledge subunit = this.getSubPledge(player.getPledgeType());
            if (subunit == null) {
               return;
            }

            for (Skill skill : subunit.getSkills()) {
               player.removeSkill(skill, false);
            }
         }
      }
   }

   public void skillsStatus(Player player, boolean disable) {
      if (player != null) {
         for (Skill skill : this._skills.values()) {
            if (disable) {
               player.disableSkill(skill, -1L);
            } else {
               player.enableSkill(skill);
            }
         }

         if (player.getPledgeType() == 0) {
            for (Skill skillx : this._subPledgeSkills.values()) {
               if (disable) {
                  player.disableSkill(skillx, -1L);
               } else {
                  player.enableSkill(skillx);
               }
            }
         } else {
            Clan.SubPledge subunit = this.getSubPledge(player.getPledgeType());
            if (subunit != null) {
               for (Skill skillxx : subunit.getSkills()) {
                  if (disable) {
                     player.disableSkill(skillxx, -1L);
                  } else {
                     player.enableSkill(skillxx);
                  }
               }
            }
         }
      }
   }

   public void broadcastToOnlineAllyMembers(ServerPacket packet) {
      for (Clan clan : ClanTable.getInstance().getClanAllies(this.getAllyId())) {
         clan.broadcastToOnlineMembers(packet);
      }
   }

   public void broadcastToOnlineMembers(ServerPacket packet) {
      for (ClanMember member : this._members.values()) {
         if (member != null && member.isOnline()) {
            member.getPlayer().sendPacket(packet);
         }
      }
   }

   public void broadcastCSToOnlineMembers(CreatureSay packet, Player broadcaster) {
      for (ClanMember member : this._members.values()) {
         if (member != null && member.isOnline() && !BlockList.isBlocked(member.getPlayer(), broadcaster)) {
            member.getPlayer().sendPacket(packet);
         }
      }
   }

   public void broadcastToOtherOnlineMembers(ServerPacket packet, Player player) {
      for (ClanMember member : this._members.values()) {
         if (member != null && member.isOnline() && member.getPlayer() != player) {
            member.getPlayer().sendPacket(packet);
         }
      }
   }

   @Override
   public String toString() {
      return this._name + "[" + this._clanId + "]";
   }

   public ItemContainer getWarehouse() {
      return this._warehouse;
   }

   public boolean isAtWarWith(int id) {
      return this._atWarWith.contains(id);
   }

   public boolean isAtWarWith(Clan clan) {
      return this._atWarWith.contains(clan.getId());
   }

   public boolean isAtWarAttacker(int id) {
      return this._atWarAttackers.contains(id);
   }

   public void setEnemyClan(Clan clan) {
      this._atWarWith.add(clan.getId());
   }

   public void setEnemyClan(int id) {
      this._atWarWith.add(id);
   }

   public void setAttackerClan(Clan clan) {
      this._atWarAttackers.add(clan.getId());
   }

   public void setAttackerClan(int clan) {
      this._atWarAttackers.add(clan);
   }

   public void deleteEnemyClan(Clan clan) {
      this._atWarWith.remove(clan.getId());
   }

   public void deleteAttackerClan(Clan clan) {
      this._atWarAttackers.remove(clan.getId());
   }

   public int getHiredGuards() {
      return this._hiredGuards;
   }

   public void incrementHiredGuards() {
      this._hiredGuards++;
   }

   public boolean isAtWar() {
      return this._atWarWith != null && !this._atWarWith.isEmpty();
   }

   public Set<Integer> getWarList() {
      return this._atWarWith;
   }

   public Set<Integer> getAttackerList() {
      return this._atWarAttackers;
   }

   public void broadcastClanStatus() {
      for (Player member : this.getOnlineMembers(0)) {
         member.sendPacket(PledgeShowMemberListDeleteAll.STATIC_PACKET);
         member.sendPacket(new PledgeShowMemberListAll(this, member));
      }
   }

   private void restoreSubPledges() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
      ) {
         ps.setInt(1, this._clanId);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               int id = rset.getInt("sub_pledge_id");
               String name = rset.getString("name");
               int leaderId = rset.getInt("leader_id");
               Clan.SubPledge pledge = new Clan.SubPledge(id, name, leaderId);
               this._subPledges.put(id, pledge);
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Could not restore clan sub-units: " + var14.getMessage(), (Throwable)var14);
      }
   }

   public Clan.SubPledge getSubPledge(int pledgeType) {
      return this._subPledges.get(pledgeType);
   }

   public Clan.SubPledge getSubPledge(String pledgeName) {
      for (Clan.SubPledge sp : this._subPledges.values()) {
         if (sp.getName().equalsIgnoreCase(pledgeName)) {
            return sp;
         }
      }

      return null;
   }

   public Collection<Clan.SubPledge> getAllSubPledges() {
      return this._subPledges.values();
   }

   public Clan.SubPledge createSubPledge(Player player, int pledgeTypeValue, int leaderId, String subPledgeName) {
      Clan.SubPledge subPledge = null;
      int pledgeType = this.getAvailablePledgeTypes(pledgeTypeValue);
      if (pledgeType == 0) {
         if (pledgeType == -1) {
            player.sendPacket(SystemMessageId.YOUR_CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
         } else {
            player.sendMessage("You can't create any more sub-units of this type");
         }

         return null;
      } else if (this._leader.getObjectId() == leaderId) {
         player.sendMessage("Leader is not correct");
         return null;
      } else if (pledgeType == -1
         || (this._reputationScore >= FeatureConfig.ROYAL_GUARD_COST || pledgeType >= 1001)
            && (this._reputationScore >= FeatureConfig.KNIGHT_UNIT_COST || pledgeType <= 200)) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
         ) {
            ps.setInt(1, this._clanId);
            ps.setInt(2, pledgeType);
            ps.setString(3, subPledgeName);
            ps.setInt(4, pledgeType != -1 ? leaderId : 0);
            ps.execute();
            subPledge = new Clan.SubPledge(pledgeType, subPledgeName, leaderId);
            this._subPledges.put(pledgeType, subPledge);
            if (pledgeType != -1) {
               if (pledgeType < 1001) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.ROYAL_GUARD_COST);
               } else {
                  this.setReputationScore(this._reputationScore - FeatureConfig.KNIGHT_UNIT_COST);
               }
            }
         } catch (Exception var15) {
            LOGGER.log(Level.SEVERE, "Error saving sub clan data: " + var15.getMessage(), (Throwable)var15);
         }

         this.broadcastToOnlineMembers(new PledgeShowInfoUpdate(this._leader.getClan()));
         this.broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, this._leader.getClan()));
         return subPledge;
      } else {
         player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
         return null;
      }
   }

   public int getAvailablePledgeTypes(int pledgeType) {
      if (this._subPledges.get(pledgeType) != null) {
         switch (pledgeType) {
            case -1:
               return 0;
            case 100:
               return this.getAvailablePledgeTypes(200);
            case 200:
               return 0;
            case 1001:
               return this.getAvailablePledgeTypes(1002);
            case 1002:
               return this.getAvailablePledgeTypes(2001);
            case 2001:
               return this.getAvailablePledgeTypes(2002);
            case 2002:
               return 0;
         }
      }

      return pledgeType;
   }

   public void updateSubPledgeInDB(int pledgeType) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_subpledges SET leader_id=?, name=? WHERE clan_id=? AND sub_pledge_id=?");
      ) {
         ps.setInt(1, this.getSubPledge(pledgeType).getLeaderId());
         ps.setString(2, this.getSubPledge(pledgeType).getName());
         ps.setInt(3, this._clanId);
         ps.setInt(4, pledgeType);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Error updating subpledge: " + var10.getMessage(), (Throwable)var10);
      }
   }

   private void restoreRankPrivs() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT privs,`rank`,party FROM clan_privs WHERE clan_id=?");
      ) {
         ps.setInt(1, this._clanId);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               int rank = rset.getInt("rank");
               if (rank != -1) {
                  int privileges = rset.getInt("privs");
                  this._privs.get(rank).setPrivs(privileges);
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Error restoring clan privs by rank: " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void initializePrivs() {
      for (int i = 1; i < 10; i++) {
         this._privs.put(i, new Clan.RankPrivs(i, 0, new ClanPrivileges()));
      }
   }

   public ClanPrivileges getRankPrivs(int rank) {
      return this._privs.get(rank) != null ? this._privs.get(rank).getPrivs() : new ClanPrivileges();
   }

   public void setRankPrivs(int rank, int privs) {
      if (this._privs.get(rank) != null) {
         this._privs.get(rank).setPrivs(privs);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_privs (clan_id,`rank`,party,privs) VALUES (?,?,?,?)");
         ) {
            ps.setInt(1, this._clanId);
            ps.setInt(2, rank);
            ps.setInt(3, 0);
            ps.setInt(4, privs);
            ps.execute();
         } catch (Exception var16) {
            LOGGER.log(Level.WARNING, "Could not store clan privs for rank: " + var16.getMessage(), (Throwable)var16);
         }

         for (ClanMember cm : this.getMembers()) {
            if (cm.isOnline() && cm.getPowerGrade() == rank && cm.getPlayer() != null) {
               cm.getPlayer().getClanPrivileges().setMask(privs);
               cm.getPlayer().updateUserInfo();
            }
         }

         this.broadcastClanStatus();
      } else {
         this._privs.put(rank, new Clan.RankPrivs(rank, 0, privs));

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO clan_privs (clan_id,`rank`,party,privs) VALUES (?,?,?,?)");
         ) {
            ps.setInt(1, this._clanId);
            ps.setInt(2, rank);
            ps.setInt(3, 0);
            ps.setInt(4, privs);
            ps.execute();
         } catch (Exception var13) {
            LOGGER.log(Level.WARNING, "Could not create new rank and store clan privs for rank: " + var13.getMessage(), (Throwable)var13);
         }
      }
   }

   public Collection<Clan.RankPrivs> getAllRankPrivs() {
      return this._privs.values();
   }

   public int getLeaderSubPledge(int leaderId) {
      int id = 0;

      for (Clan.SubPledge sp : this._subPledges.values()) {
         if (sp.getLeaderId() != 0 && sp.getLeaderId() == leaderId) {
            id = sp.getId();
         }
      }

      return id;
   }

   public synchronized void addReputationScore(int value) {
      this.setReputationScore(this._reputationScore + value);
   }

   public synchronized void takeReputationScore(int value) {
      this.setReputationScore(this._reputationScore - value);
   }

   private void setReputationScore(int value) {
      if (this._reputationScore >= 0 && value < 0) {
         this.broadcastToOnlineMembers(
            new SystemMessage(SystemMessageId.SINCE_THE_CLAN_REPUTATION_SCORE_HAS_DROPPED_TO_0_OR_LOWER_YOUR_CLAN_SKILL_S_WILL_BE_DE_ACTIVATED)
         );

         for (ClanMember member : this._members.values()) {
            if (member.isOnline() && member.getPlayer() != null) {
               this.skillsStatus(member.getPlayer(), true);
            }
         }
      } else if (this._reputationScore < 0 && value >= 0) {
         this.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILLS_WILL_NOW_BE_ACTIVATED_SINCE_THE_CLAN_S_REPUTATION_SCORE_IS_0_OR_HIGHER));

         for (ClanMember memberx : this._members.values()) {
            if (memberx.isOnline() && memberx.getPlayer() != null) {
               this.skillsStatus(memberx.getPlayer(), false);
            }
         }
      }

      this._reputationScore = value;
      if (this._reputationScore > 100000000) {
         this._reputationScore = 100000000;
      }

      if (this._reputationScore < -100000000) {
         this._reputationScore = -100000000;
      }

      this.broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
   }

   public int getReputationScore() {
      return this._reputationScore;
   }

   public synchronized void setRank(int rank) {
      this._rank = rank;
   }

   public int getRank() {
      return this._rank;
   }

   public int getAuctionBiddedAt() {
      return this._auctionBiddedAt;
   }

   public void setAuctionBiddedAt(int id, boolean storeInDb) {
      this._auctionBiddedAt = id;
      if (storeInDb) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
         ) {
            ps.setInt(1, id);
            ps.setInt(2, this._clanId);
            ps.execute();
         } catch (Exception var11) {
            LOGGER.log(Level.WARNING, "Could not store auction for clan: " + var11.getMessage(), (Throwable)var11);
         }
      }
   }

   public boolean checkClanJoinCondition(Player player, Player target, int pledgeType) {
      if (player == null) {
         return false;
      } else if (!player.hasAccess(ClanAccess.INVITE_MEMBER)) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         return false;
      } else if (target == null) {
         player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
         return false;
      } else if (player.getObjectId() == target.getObjectId()) {
         player.sendPacket(SystemMessageId.YOU_CANNOT_ASK_YOURSELF_TO_APPLY_TO_A_CLAN);
         return false;
      } else if (this._charPenaltyExpiryTime > System.currentTimeMillis()) {
         player.sendPacket(SystemMessageId.AFTER_A_CLAN_MEMBER_IS_DISMISSED_FROM_A_CLAN_THE_CLAN_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_ACCEPTING_A_NEW_MEMBER);
         return false;
      } else if (target.getClanId() != 0) {
         SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_WORKING_WITH_ANOTHER_CLAN);
         sm.addString(target.getName());
         player.sendPacket(sm);
         return false;
      } else if (target.getClanJoinExpiryTime() > System.currentTimeMillis()) {
         SystemMessage sm = new SystemMessage(SystemMessageId.C1_CANNOT_JOIN_THE_CLAN_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_THEY_LEFT_ANOTHER_CLAN);
         sm.addString(target.getName());
         player.sendPacket(sm);
         return false;
      } else if ((target.getLevel() > 40 || target.getPlayerClass().level() >= 2) && pledgeType == -1) {
         SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_MEET_THE_REQUIREMENTS_TO_JOIN_A_CLAN_ACADEMY);
         sm.addString(target.getName());
         player.sendPacket(sm);
         player.sendPacket(
            SystemMessageId.TO_JOIN_A_CLAN_ACADEMY_CHARACTERS_MUST_BE_LEVEL_40_OR_BELOW_NOT_BELONG_ANOTHER_CLAN_AND_NOT_YET_COMPLETED_THEIR_2ND_CLASS_TRANSFER
         );
         return false;
      } else if (this.getSubPledgeMembersCount(pledgeType) >= this.getMaxNrOfMembers(pledgeType)) {
         if (pledgeType == 0) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_FULL_AND_CANNOT_ACCEPT_ADDITIONAL_CLAN_MEMBERS_AT_THIS_TIME);
            sm.addString(this._name);
            player.sendPacket(sm);
         } else {
            player.sendPacket(SystemMessageId.THE_ACADEMY_ROYAL_GUARD_ORDER_OF_KNIGHTS_IS_FULL_AND_CANNOT_ACCEPT_NEW_MEMBERS_AT_THIS_TIME);
         }

         return false;
      } else {
         return true;
      }
   }

   public boolean checkAllyJoinCondition(Player player, Player target) {
      if (player == null) {
         return false;
      } else if (player.getAllyId() != 0 && player.isClanLeader() && player.getClanId() == player.getAllyId()) {
         Clan leaderClan = player.getClan();
         if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis() && leaderClan.getAllyPenaltyType() == 3) {
            player.sendPacket(SystemMessageId.YOU_MAY_NOT_ACCEPT_ANY_CLAN_WITHIN_A_DAY_AFTER_EXPELLING_ANOTHER_CLAN);
            return false;
         } else if (target == null) {
            player.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            return false;
         } else if (player.getObjectId() == target.getObjectId()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_ASK_YOURSELF_TO_APPLY_TO_A_CLAN);
            return false;
         } else if (target.getClan() == null) {
            player.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
            return false;
         } else if (!target.isClanLeader()) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
            sm.addString(target.getName());
            player.sendPacket(sm);
            return false;
         } else {
            Clan targetClan = target.getClan();
            if (target.getAllyId() != 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_ALREADY_A_MEMBER_OF_S2_ALLIANCE);
               sm.addString(targetClan.getName());
               sm.addString(targetClan.getAllyName());
               player.sendPacket(sm);
               return false;
            } else {
               if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis()) {
                  if (targetClan.getAllyPenaltyType() == 1) {
                     SystemMessage sm = new SystemMessage(
                        SystemMessageId.S1_CLAN_CANNOT_JOIN_THE_ALLIANCE_BECAUSE_ONE_DAY_HAS_NOT_YET_PASSED_SINCE_IT_LEFT_ANOTHER_ALLIANCE
                     );
                     sm.addString(target.getClan().getName());
                     sm.addString(target.getClan().getAllyName());
                     player.sendPacket(sm);
                     return false;
                  }

                  if (targetClan.getAllyPenaltyType() == 2) {
                     player.sendPacket(
                        SystemMessageId.A_CLAN_THAT_HAS_WITHDRAWN_OR_BEEN_EXPELLED_CANNOT_ENTER_INTO_AN_ALLIANCE_WITHIN_ONE_DAY_OF_WITHDRAWAL_OR_EXPULSION
                     );
                     return false;
                  }
               }

               if (player.isInsideZone(ZoneId.SIEGE) && target.isInsideZone(ZoneId.SIEGE)) {
                  player.sendPacket(SystemMessageId.THE_OPPOSING_CLAN_IS_PARTICIPATING_IN_A_SIEGE_BATTLE);
                  return false;
               } else if (leaderClan.isAtWarWith(targetClan.getId())) {
                  player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALLY_WITH_A_CLAN_YOU_ARE_CURRENTLY_AT_WAR_WITH_THAT_WOULD_BE_DIABOLICAL_AND_TREACHEROUS);
                  return false;
               } else if (ClanTable.getInstance().getClanAllies(player.getAllyId()).size() >= PlayerConfig.ALT_MAX_NUM_OF_CLANS_IN_ALLY) {
                  player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT);
                  return false;
               } else {
                  return true;
               }
            }
         }
      } else {
         player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_ALLIANCE_LEADERS);
         return false;
      }
   }

   public long getAllyPenaltyExpiryTime() {
      return this._allyPenaltyExpiryTime;
   }

   public int getAllyPenaltyType() {
      return this._allyPenaltyType;
   }

   public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType) {
      this._allyPenaltyExpiryTime = expiryTime;
      this._allyPenaltyType = penaltyType;
   }

   public long getCharPenaltyExpiryTime() {
      return this._charPenaltyExpiryTime;
   }

   public void setCharPenaltyExpiryTime(long time) {
      this._charPenaltyExpiryTime = time;
   }

   public long getDissolvingExpiryTime() {
      return this._dissolvingExpiryTime;
   }

   public void setDissolvingExpiryTime(long time) {
      this._dissolvingExpiryTime = time;
   }

   public void createAlly(Player player, String allyName) {
      if (player != null) {
         if (!player.isClanLeader()) {
            player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
         } else if (this._allyId != 0) {
            player.sendPacket(SystemMessageId.YOU_ALREADY_BELONG_TO_ANOTHER_ALLIANCE);
         } else if (this._level < 5) {
            player.sendPacket(SystemMessageId.TO_CREATE_AN_ALLIANCE_YOUR_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
         } else if (this._allyPenaltyExpiryTime > System.currentTimeMillis() && this._allyPenaltyType == 4) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CREATE_A_NEW_ALLIANCE_WITHIN_1_DAY_OF_DISSOLUTION);
         } else if (this._dissolvingExpiryTime > System.currentTimeMillis()) {
            player.sendPacket(SystemMessageId.DURING_THE_DISSOLUTION_SUSPENSION_PERIOD_NO_ALLIANCE_CAN_BE_CREATED);
         } else if (!StringUtil.isAlphaNumeric(allyName)) {
            player.sendPacket(SystemMessageId.INCORRECT_ALLIANCE_NAME_PLEASE_TRY_AGAIN);
         } else if (allyName.length() > 16 || allyName.length() < 2) {
            player.sendPacket(SystemMessageId.INCORRECT_LENGTH_FOR_AN_ALLIANCE_NAME);
         } else if (ClanTable.getInstance().isAllyExists(allyName)) {
            player.sendPacket(SystemMessageId.THIS_ALLIANCE_NAME_ALREADY_EXISTS);
         } else {
            this.setAllyId(this._clanId);
            this.setAllyName(allyName.trim());
            this.setAllyPenaltyExpiryTime(0L, 0);
            this.updateClanInDB();
            player.updateUserInfo();
            player.sendMessage("Alliance " + allyName + " has been created.");
         }
      }
   }

   public void dissolveAlly(Player player) {
      if (this._allyId == 0) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS);
      } else if (player.isClanLeader() && this._clanId == this._allyId) {
         if (player.isInsideZone(ZoneId.SIEGE)) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_AN_ALLIANCE_WHILE_AN_AFFILIATED_CLAN_IS_PARTICIPATING_IN_A_SIEGE_BATTLE);
         } else {
            this.broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.THE_ALLIANCE_HAS_BEEN_DISSOLVED));
            long currentTime = System.currentTimeMillis();

            for (Clan clan : ClanTable.getInstance().getClanAllies(this.getAllyId())) {
               if (clan.getId() != this.getId()) {
                  clan.setAllyId(0);
                  clan.setAllyName(null);
                  clan.setAllyPenaltyExpiryTime(0L, 0);
                  clan.updateClanInDB();
               }
            }

            this.setAllyId(0);
            this.setAllyName(null);
            this.changeAllyCrest(0, false);
            this.setAllyPenaltyExpiryTime(currentTime + PlayerConfig.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000, 4);
            this.updateClanInDB();
         }
      } else {
         player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_ALLIANCE_LEADERS);
      }
   }

   public boolean levelUpClan(Player player) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         return false;
      } else if (System.currentTimeMillis() < this._dissolvingExpiryTime) {
         player.sendPacket(SystemMessageId.DURING_THE_SUSPENDED_DISSOLUTION_PERIOD_CLAN_LEVELS_CANNOT_BE_INCREASED);
         return false;
      } else {
         boolean increaseClanLevel = false;
         switch (this._level) {
            case 0:
               if (player.getSp() >= 20000L && player.getAdena() >= 650000L && player.reduceAdena(ItemProcessType.FEE, 650000L, player.getTarget(), true)) {
                  player.setSp(player.getSp() - 20000L);
                  SystemMessage sp = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
                  sp.addInt(20000);
                  player.sendPacket(sp);
                  increaseClanLevel = true;
               }
               break;
            case 1:
               if (player.getSp() >= 100000L && player.getAdena() >= 2500000L && player.reduceAdena(ItemProcessType.FEE, 2500000L, player.getTarget(), true)) {
                  player.setSp(player.getSp() - 100000L);
                  SystemMessage sp = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
                  sp.addInt(100000);
                  player.sendPacket(sp);
                  increaseClanLevel = true;
               }
               break;
            case 2:
               if (player.getSp() >= 350000L
                  && player.getInventory().getItemByItemId(1419) != null
                  && player.destroyItemByItemId(ItemProcessType.FEE, 1419, 1L, player.getTarget(), false)) {
                  player.setSp(player.getSp() - 350000L);
                  SystemMessage sp = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
                  sp.addInt(350000);
                  player.sendPacket(sp);
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  sm.addItemName(1419);
                  player.sendPacket(sm);
                  increaseClanLevel = true;
               }
               break;
            case 3:
               if (player.getSp() >= 1000000L
                  && player.getInventory().getItemByItemId(3874) != null
                  && player.destroyItemByItemId(ItemProcessType.FEE, 3874, 1L, player.getTarget(), false)) {
                  player.setSp(player.getSp() - 1000000L);
                  SystemMessage sp = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
                  sp.addInt(1000000);
                  player.sendPacket(sp);
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  sm.addItemName(3874);
                  player.sendPacket(sm);
                  increaseClanLevel = true;
               }
               break;
            case 4:
               if (player.getSp() >= 2500000L
                  && player.getInventory().getItemByItemId(3870) != null
                  && player.destroyItemByItemId(ItemProcessType.FEE, 3870, 1L, player.getTarget(), false)) {
                  player.setSp(player.getSp() - 2500000L);
                  SystemMessage sp = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
                  sp.addInt(2500000);
                  player.sendPacket(sp);
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  sm.addItemName(3870);
                  player.sendPacket(sm);
                  increaseClanLevel = true;
               }
               break;
            case 5:
               if (this._reputationScore >= FeatureConfig.CLAN_LEVEL_6_COST && this._members.size() >= FeatureConfig.CLAN_LEVEL_6_REQUIREMENT) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_6_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_6_COST);
                  player.sendPacket(cr);
                  increaseClanLevel = true;
               }
               break;
            case 6:
               if (this._reputationScore >= FeatureConfig.CLAN_LEVEL_7_COST && this._members.size() >= FeatureConfig.CLAN_LEVEL_7_REQUIREMENT) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_7_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_7_COST);
                  player.sendPacket(cr);
                  increaseClanLevel = true;
               }
               break;
            case 7:
               if (this._reputationScore >= FeatureConfig.CLAN_LEVEL_8_COST && this._members.size() >= FeatureConfig.CLAN_LEVEL_8_REQUIREMENT) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_8_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_8_COST);
                  player.sendPacket(cr);
                  increaseClanLevel = true;
               }
               break;
            case 8:
               if (this._reputationScore >= FeatureConfig.CLAN_LEVEL_9_COST
                  && player.getInventory().getItemByItemId(9910) != null
                  && this._members.size() >= FeatureConfig.CLAN_LEVEL_9_REQUIREMENT
                  && player.destroyItemByItemId(ItemProcessType.FEE, 9910, 150L, player.getTarget(), false)) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_9_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_9_COST);
                  player.sendPacket(cr);
                  SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                  sm.addItemName(9910);
                  sm.addLong(150L);
                  player.sendPacket(sm);
                  increaseClanLevel = true;
               }
               break;
            case 9:
               if (this._reputationScore >= FeatureConfig.CLAN_LEVEL_10_COST
                  && player.getInventory().getItemByItemId(9911) != null
                  && this._members.size() >= FeatureConfig.CLAN_LEVEL_10_REQUIREMENT
                  && player.destroyItemByItemId(ItemProcessType.FEE, 9911, 5L, player.getTarget(), false)) {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_10_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_10_COST);
                  player.sendPacket(cr);
                  SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                  sm.addItemName(9911);
                  sm.addLong(5L);
                  player.sendPacket(sm);
                  increaseClanLevel = true;
               }
               break;
            case 10:
               boolean hasTerritory = false;

               for (TerritoryWarManager.Territory terr : TerritoryWarManager.getInstance().getAllTerritories()) {
                  if (terr.getOwnerClan().getId() == this.getId()) {
                     hasTerritory = true;
                     break;
                  }
               }

               if (hasTerritory && this._reputationScore >= FeatureConfig.CLAN_LEVEL_11_COST && this._members.size() >= FeatureConfig.CLAN_LEVEL_11_REQUIREMENT
                  )
                {
                  this.setReputationScore(this._reputationScore - FeatureConfig.CLAN_LEVEL_11_COST);
                  SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
                  cr.addInt(FeatureConfig.CLAN_LEVEL_11_COST);
                  player.sendPacket(cr);
                  increaseClanLevel = true;
               }
               break;
            default:
               return false;
         }

         if (!increaseClanLevel) {
            player.sendPacket(SystemMessageId.THE_CONDITIONS_NECESSARY_TO_INCREASE_THE_CLAN_S_LEVEL_HAVE_NOT_BEEN_MET);
            return false;
         } else {
            StatusUpdate su = new StatusUpdate(player);
            su.addAttribute(13, (int)player.getSp());
            player.sendPacket(su);
            player.sendItemList(false);
            this.changeLevel(this._level + 1);
            if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_LEVELUP)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanLvlUp(player, this));
            }

            return true;
         }
      }
   }

   public void changeLevel(int level) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
      ) {
         ps.setInt(1, level);
         ps.setInt(2, this._clanId);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "could not increase clan level:" + var10.getMessage(), (Throwable)var10);
      }

      this.setLevel(level);
      this.setRank(ClanTable.getInstance().getClanRank(this));
      if (this._leader.isOnline()) {
         Player leader = this._leader.getPlayer();
         if (level > 4) {
            SiegeManager.getInstance().addSiegeSkills(leader);
            leader.sendPacket(SystemMessageId.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
         } else if (level < 5) {
            SiegeManager.getInstance().removeSiegeSkills(leader);
         }
      }

      this.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_CLAN_S_SKILL_LEVEL_HAS_INCREASED));
      this.broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
   }

   public void changeClanCrest(int crestId) {
      if (this._crestId != 0) {
         CrestTable.getInstance().removeCrest(this.getCrestId());
      }

      this.setCrestId(crestId);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
      ) {
         ps.setInt(1, crestId);
         ps.setInt(2, this._clanId);
         ps.executeUpdate();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, "Could not update crest for clan " + this._name + " [" + this._clanId + "] : " + var10.getMessage(), (Throwable)var10);
      }

      for (Player member : this.getOnlineMembers(0)) {
         member.broadcastUserInfo();
      }
   }

   public void changeAllyCrest(int crestId, boolean onlyThisClan) {
      String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
      int allyId = this._clanId;
      if (!onlyThisClan) {
         if (this._allyCrestId != 0) {
            CrestTable.getInstance().removeCrest(this.getAllyCrestId());
         }

         sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
         allyId = this._allyId;
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(sqlStatement);
      ) {
         ps.setInt(1, crestId);
         ps.setInt(2, allyId);
         ps.executeUpdate();
      } catch (SQLException var13) {
         LOGGER.log(Level.WARNING, "Could not update ally crest for ally/clan id " + allyId + " : " + var13.getMessage(), (Throwable)var13);
      }

      if (onlyThisClan) {
         this.setAllyCrestId(crestId);

         for (Player member : this.getOnlineMembers(0)) {
            member.broadcastUserInfo();
         }
      } else {
         for (Clan clan : ClanTable.getInstance().getClanAllies(this.getAllyId())) {
            clan.setAllyCrestId(crestId);

            for (Player member : clan.getOnlineMembers(0)) {
               member.broadcastUserInfo();
            }
         }
      }
   }

   public void changeLargeCrest(int crestId) {
      if (this._crestLargeId != 0) {
         CrestTable.getInstance().removeCrest(this.getCrestLargeId());
      }

      this.setCrestLargeId(crestId);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
      ) {
         ps.setInt(1, crestId);
         ps.setInt(2, this._clanId);
         ps.executeUpdate();
      } catch (SQLException var10) {
         LOGGER.log(Level.WARNING, "Could not update large crest for clan " + this._name + " [" + this._clanId + "] : " + var10.getMessage(), (Throwable)var10);
      }

      for (Player member : this.getOnlineMembers(0)) {
         member.broadcastUserInfo();
      }
   }

   public boolean isLearnableSubSkill(int skillId, int skillLevel) {
      Skill current = this._subPledgeSkills.get(skillId);
      if (current != null && current.getLevel() + 1 == skillLevel) {
         return true;
      } else if (current == null && skillLevel == 1) {
         return true;
      } else {
         for (Clan.SubPledge subunit : this._subPledges.values()) {
            if (subunit.getId() != -1) {
               current = subunit.getSkill(skillId);
               if (current != null && current.getLevel() + 1 == skillLevel) {
                  return true;
               }

               if (current == null && skillLevel == 1) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public boolean isLearnableSubPledgeSkill(Skill skill, int subType) {
      if (subType == -1) {
         return false;
      } else {
         int id = skill.getId();
         Skill current;
         if (subType == 0) {
            current = this._subPledgeSkills.get(id);
         } else {
            current = this._subPledges.get(subType).getSkill(id);
         }

         return current != null && current.getLevel() + 1 == skill.getLevel() ? true : current == null && skill.getLevel() == 1;
      }
   }

   public Collection<PledgeSkillList.SubPledgeSkill> getAllSubSkills() {
      List<PledgeSkillList.SubPledgeSkill> list = new ArrayList<>();

      for (Skill skill : this._subPledgeSkills.values()) {
         list.add(new PledgeSkillList.SubPledgeSkill(0, skill.getId(), skill.getLevel()));
      }

      for (Clan.SubPledge subunit : this._subPledges.values()) {
         for (Skill skill : subunit.getSkills()) {
            list.add(new PledgeSkillList.SubPledgeSkill(subunit.getId(), skill.getId(), skill.getLevel()));
         }
      }

      return list;
   }

   public void setNewLeaderId(int objectId, boolean storeInDb) {
      this._newLeaderId = objectId;
      if (storeInDb) {
         this.updateClanInDB();
      }
   }

   public int getNewLeaderId() {
      return this._newLeaderId;
   }

   public Player getNewLeader() {
      return World.getPlayer(this._newLeaderId);
   }

   public String getNewLeaderName() {
      return CharInfoTable.getInstance().getNameById(this._newLeaderId);
   }

   public int getSiegeKills() {
      return this._siegeKills.get();
   }

   public int getSiegeDeaths() {
      return this._siegeDeaths.get();
   }

   public int addSiegeKill() {
      return this._siegeKills.incrementAndGet();
   }

   public int addSiegeDeath() {
      return this._siegeDeaths.incrementAndGet();
   }

   public void clearSiegeKills() {
      this._siegeKills.set(0);
   }

   public void clearSiegeDeaths() {
      this._siegeDeaths.set(0);
   }

   public static class RankPrivs {
      private final int _rankId;
      private final int _party;
      private final ClanPrivileges _rankPrivs;

      public RankPrivs(int rank, int party, int privs) {
         this._rankId = rank;
         this._party = party;
         this._rankPrivs = new ClanPrivileges(privs);
      }

      public RankPrivs(int rank, int party, ClanPrivileges rankPrivs) {
         this._rankId = rank;
         this._party = party;
         this._rankPrivs = rankPrivs;
      }

      public int getRank() {
         return this._rankId;
      }

      public int getParty() {
         return this._party;
      }

      public ClanPrivileges getPrivs() {
         return this._rankPrivs;
      }

      public void setPrivs(int privs) {
         this._rankPrivs.setMask(privs);
      }
   }

   public static class SubPledge {
      private final int _id;
      private String _subPledgeName;
      private int _leaderId;
      private final Map<Integer, Skill> _subPledgeSkills = new HashMap<>();

      public SubPledge(int id, String name, int leaderId) {
         this._id = id;
         this._subPledgeName = name;
         this._leaderId = leaderId;
      }

      public int getId() {
         return this._id;
      }

      public String getName() {
         return this._subPledgeName;
      }

      public void setName(String name) {
         this._subPledgeName = name;
      }

      public int getLeaderId() {
         return this._leaderId;
      }

      public void setLeaderId(int leaderId) {
         this._leaderId = leaderId;
      }

      public Skill addNewSkill(Skill skill) {
         return this._subPledgeSkills.put(skill.getId(), skill);
      }

      public Collection<Skill> getSkills() {
         return this._subPledgeSkills.values();
      }

      public Skill getSkill(int id) {
         return this._subPledgeSkills.get(id);
      }
   }
}
