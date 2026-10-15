package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.TowerSpawn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class SiegeManager {
   private static final Logger LOGGER = Logger.getLogger(SiegeManager.class.getName());
   private static final String SIEGE_CONFIG_FILE = "./config/Siege.ini";
   private final Map<Integer, List<TowerSpawn>> _controlTowers = new HashMap<>();
   private final Map<Integer, List<TowerSpawn>> _flameTowers = new HashMap<>();
   private int _siegeCycle = 2;
   private int _attackerMaxClans = 500;
   private int _attackerRespawnDelay = 0;
   private int _defenderMaxClans = 500;
   private int _flagMaxCount = 1;
   private int _siegeClanMinLevel = 5;
   private int _siegeLength = 120;
   private int _bloodAllianceReward = 0;

   protected SiegeManager() {
      this.load();
   }

   public void addSiegeSkills(Player character) {
      for (Skill sk : SkillData.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0)) {
         character.addSkill(sk, false);
      }
   }

   public boolean checkIsRegistered(Clan clan, int castleid) {
      if (clan == null) {
         return false;
      } else if (clan.getCastleId() > 0) {
         return true;
      } else {
         boolean register = false;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=? and castle_id=?");
         ) {
            ps.setInt(1, clan.getId());
            ps.setInt(2, castleid);

            try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) {
                  register = true;
               }
            }
         } catch (Exception var15) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Exception: checkIsRegistered(): " + var15.getMessage(), (Throwable)var15);
         }

         return register;
      }
   }

   public void removeSiegeSkills(Player character) {
      for (Skill sk : SkillData.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getCastleId() > 0)) {
         character.removeSkill(sk);
      }
   }

   private void load() {
      ConfigReader siegeConfig = new ConfigReader("./config/Siege.ini");
      this._siegeCycle = siegeConfig.getInt("SiegeCycle", 2);
      this._attackerMaxClans = siegeConfig.getInt("AttackerMaxClans", 500);
      this._attackerRespawnDelay = siegeConfig.getInt("AttackerRespawn", 0);
      this._defenderMaxClans = siegeConfig.getInt("DefenderMaxClans", 500);
      this._flagMaxCount = siegeConfig.getInt("MaxFlags", 1);
      this._siegeClanMinLevel = siegeConfig.getInt("SiegeClanMinLevel", 5);
      this._siegeLength = siegeConfig.getInt("SiegeLength", 120);
      this._bloodAllianceReward = siegeConfig.getInt("BloodAllianceReward", 1);

      for (Castle castle : CastleManager.getInstance().getCastles()) {
         List<TowerSpawn> controlTowers = new ArrayList<>();

         for (int i = 1; i < 255; i++) {
            String configKey = castle.getName() + "ControlTower" + i;
            if (!siegeConfig.containsKey(configKey)) {
               break;
            }

            StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");

            try {
               int x = Integer.parseInt(st.nextToken());
               int y = Integer.parseInt(st.nextToken());
               int z = Integer.parseInt(st.nextToken());
               int npcId = Integer.parseInt(st.nextToken());
               controlTowers.add(new TowerSpawn(npcId, new Location(x, y, z)));
            } catch (Exception var14) {
               LOGGER.warning(this.getClass().getSimpleName() + ": Error while loading control tower(s) for " + castle.getName() + " castle.");
            }
         }

         List<TowerSpawn> flameTowers = new ArrayList<>();

         for (int i = 1; i < 255; i++) {
            String configKey = castle.getName() + "FlameTower" + i;
            if (!siegeConfig.containsKey(configKey)) {
               break;
            }

            StringTokenizer st = new StringTokenizer(siegeConfig.getString(configKey, ""), ",");

            try {
               int x = Integer.parseInt(st.nextToken());
               int y = Integer.parseInt(st.nextToken());
               int z = Integer.parseInt(st.nextToken());
               int npcId = Integer.parseInt(st.nextToken());
               List<Integer> zoneList = new ArrayList<>();

               while (st.hasMoreTokens()) {
                  zoneList.add(Integer.parseInt(st.nextToken()));
               }

               flameTowers.add(new TowerSpawn(npcId, new Location(x, y, z), zoneList));
            } catch (Exception var15) {
               LOGGER.warning(this.getClass().getSimpleName() + ": Error while loading flame tower(s) for " + castle.getName() + " castle.");
            }
         }

         this._controlTowers.put(castle.getResidenceId(), controlTowers);
         this._flameTowers.put(castle.getResidenceId(), flameTowers);
         MercTicketManager.MERCS_MAX_PER_CASTLE[castle.getResidenceId() - 1] = siegeConfig.getInt(
            castle.getName() + "MaxMercenaries", MercTicketManager.MERCS_MAX_PER_CASTLE[castle.getResidenceId() - 1]
         );
         if (castle.getOwnerId() != 0) {
            this.loadTrapUpgrade(castle.getResidenceId());
         }
      }
   }

   public List<TowerSpawn> getControlTowers(int castleId) {
      return this._controlTowers.get(castleId);
   }

   public List<TowerSpawn> getFlameTowers(int castleId) {
      return this._flameTowers.get(castleId);
   }

   public int getSiegeCycle() {
      return this._siegeCycle;
   }

   public int getAttackerMaxClans() {
      return this._attackerMaxClans;
   }

   public int getAttackerRespawnDelay() {
      return this._attackerRespawnDelay;
   }

   public int getDefenderMaxClans() {
      return this._defenderMaxClans;
   }

   public int getFlagMaxCount() {
      return this._flagMaxCount;
   }

   public Siege getSiege(ILocational loc) {
      return this.getSiege(loc.getX(), loc.getY(), loc.getZ());
   }

   public Siege getSiege(WorldObject activeObject) {
      return this.getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public Siege getSiege(int x, int y, int z) {
      for (Castle castle : CastleManager.getInstance().getCastles()) {
         if (castle.getSiege().checkIfInZone(x, y, z)) {
            return castle.getSiege();
         }
      }

      return null;
   }

   public int getSiegeClanMinLevel() {
      return this._siegeClanMinLevel;
   }

   public int getSiegeLength() {
      return this._siegeLength;
   }

   public int getBloodAllianceReward() {
      return this._bloodAllianceReward;
   }

   public List<Siege> getSieges() {
      List<Siege> sieges = new ArrayList<>();

      for (Castle castle : CastleManager.getInstance().getCastles()) {
         sieges.add(castle.getSiege());
      }

      return sieges;
   }

   private void loadTrapUpgrade(int castleId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_trapupgrade WHERE castleId=?");
      ) {
         ps.setInt(1, castleId);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._flameTowers.get(castleId).get(rs.getInt("towerIndex")).setUpgradeLevel(rs.getInt("level"));
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Exception: loadTrapUpgrade(): " + var13.getMessage(), (Throwable)var13);
      }
   }

   public static SiegeManager getInstance() {
      return SiegeManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SiegeManager INSTANCE = new SiegeManager();
   }
}
