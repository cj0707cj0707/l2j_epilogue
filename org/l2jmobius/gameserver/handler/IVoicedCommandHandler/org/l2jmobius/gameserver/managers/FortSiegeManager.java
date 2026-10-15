package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.siege.CombatFlag;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class FortSiegeManager {
   private static final Logger LOGGER = Logger.getLogger(FortSiegeManager.class.getName());
   private static final String FORTSIEGE_CONFIG_FILE = "./config/FortSiege.ini";
   private int _attackerMaxClans = 500;
   private Map<Integer, List<FortSpawnHolder>> _commanderSpawnList;
   private Map<Integer, List<CombatFlag>> _flagList;
   private boolean _justToTerritory = true;
   private int _flagMaxCount = 1;
   private int _siegeClanMinLevel = 4;
   private int _siegeLength = 60;
   private int _countDownLength = 10;
   private int _suspiciousMerchantRespawnDelay = 180;
   private final List<FortSiege> _sieges = new ArrayList<>();

   protected FortSiegeManager() {
      this.load();
   }

   public void addSiegeSkills(Player character) {
      character.addSkill(CommonSkill.SEAL_OF_RULER.getSkill(), false);
      character.addSkill(CommonSkill.BUILD_HEADQUARTERS.getSkill(), false);
   }

   public boolean checkIsRegistered(Clan clan, int fortid) {
      if (clan == null) {
         return false;
      } else {
         boolean register = false;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT clan_id FROM fortsiege_clans where clan_id=? and fort_id=?");
         ) {
            ps.setInt(1, clan.getId());
            ps.setInt(2, fortid);

            try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) {
                  register = true;
               }
            }
         } catch (Exception var15) {
            LOGGER.log(Level.WARNING, "Exception: checkIsRegistered(): " + var15.getMessage(), (Throwable)var15);
         }

         return register;
      }
   }

   public void removeSiegeSkills(Player character) {
      character.removeSkill(CommonSkill.SEAL_OF_RULER.getSkill());
      character.removeSkill(CommonSkill.BUILD_HEADQUARTERS.getSkill());
   }

   private void load() {
      Properties siegeSettings = new Properties();
      File file = new File("./config/FortSiege.ini");

      try (InputStream is = new FileInputStream(file)) {
         siegeSettings.load(is);
      } catch (Exception var19) {
         LOGGER.log(Level.WARNING, "Error while loading Fort Siege Manager settings!", (Throwable)var19);
      }

      this._justToTerritory = Boolean.parseBoolean(siegeSettings.getProperty("JustToTerritory", "true"));
      this._attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
      this._flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
      this._siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
      this._siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "60"));
      this._countDownLength = Integer.decode(siegeSettings.getProperty("CountDownLength", "10"));
      this._suspiciousMerchantRespawnDelay = Integer.decode(siegeSettings.getProperty("SuspiciousMerchantRespawnDelay", "180"));
      this._commanderSpawnList = new ConcurrentHashMap<>();
      this._flagList = new ConcurrentHashMap<>();

      for (Fort fort : FortManager.getInstance().getForts()) {
         List<FortSpawnHolder> commanderSpawns = new ArrayList<>();
         List<CombatFlag> flagSpawns = new ArrayList<>();

         for (int i = 1; i < 5; i++) {
            String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "Commander" + i, "");
            if (_spawnParams.isEmpty()) {
               break;
            }

            StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

            try {
               int x = Integer.parseInt(st.nextToken());
               int y = Integer.parseInt(st.nextToken());
               int z = Integer.parseInt(st.nextToken());
               int heading = Integer.parseInt(st.nextToken());
               int npc_id = Integer.parseInt(st.nextToken());
               commanderSpawns.add(new FortSpawnHolder(fort.getResidenceId(), x, y, z, heading, npc_id, i));
            } catch (Exception var15) {
               LOGGER.warning("Error while loading commander(s) for " + fort.getName() + " fort.");
            }
         }

         this._commanderSpawnList.put(fort.getResidenceId(), commanderSpawns);

         for (int i = 1; i < 4; i++) {
            String _spawnParams = siegeSettings.getProperty(fort.getName().replace(" ", "") + "Flag" + i, "");
            if (_spawnParams.isEmpty()) {
               break;
            }

            StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

            try {
               int x = Integer.parseInt(st.nextToken());
               int y = Integer.parseInt(st.nextToken());
               int z = Integer.parseInt(st.nextToken());
               int flag_id = Integer.parseInt(st.nextToken());
               flagSpawns.add(new CombatFlag(fort.getResidenceId(), x, y, z, 0, flag_id));
            } catch (Exception var16) {
               LOGGER.warning("Error while loading flag(s) for " + fort.getName() + " fort.");
            }
         }

         this._flagList.put(fort.getResidenceId(), flagSpawns);
      }
   }

   public List<FortSpawnHolder> getCommanderSpawnList(int fortId) {
      return this._commanderSpawnList.get(fortId);
   }

   public List<CombatFlag> getFlagList(int fortId) {
      return this._flagList.get(fortId);
   }

   public int getAttackerMaxClans() {
      return this._attackerMaxClans;
   }

   public int getFlagMaxCount() {
      return this._flagMaxCount;
   }

   public boolean canRegisterJustTerritory() {
      return this._justToTerritory;
   }

   public int getSuspiciousMerchantRespawnDelay() {
      return this._suspiciousMerchantRespawnDelay;
   }

   public FortSiege getSiege(WorldObject activeObject) {
      return this.getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public FortSiege getSiege(int x, int y, int z) {
      for (Fort fort : FortManager.getInstance().getForts()) {
         if (fort.getSiege().checkIfInZone(x, y, z)) {
            return fort.getSiege();
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

   public int getCountDownLength() {
      return this._countDownLength;
   }

   public List<FortSiege> getSieges() {
      return this._sieges;
   }

   public void addSiege(FortSiege fortSiege) {
      this._sieges.add(fortSiege);
   }

   public boolean isCombat(int itemId) {
      return itemId == 9819;
   }

   public boolean activateCombatFlag(Player player, Item item) {
      if (!this.checkIfCanPickup(player)) {
         return false;
      } else {
         Fort fort = FortManager.getInstance().getFort(player);

         for (CombatFlag cf : this._flagList.get(fort.getResidenceId())) {
            if (cf.getCombatFlagInstance() == item) {
               cf.activate(player, item);
            }
         }

         return true;
      }
   }

   public boolean checkIfCanPickup(Player player) {
      if (player.isCombatFlagEquipped()) {
         return false;
      } else if (!player.isClanLeader()) {
         player.sendMessage("The flag can only be picked up by a clan leader.");
         return false;
      } else {
         Fort fort = FortManager.getInstance().getFort(player);
         if (fort == null || fort.getResidenceId() <= 0 || fort.getSiege().getAttackerClan(player.getClan()) == null) {
            return false;
         } else if (!fort.getSiege().isInProgress()) {
            player.sendPacket(new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED).addItemName(9819));
            return false;
         } else {
            return true;
         }
      }
   }

   public void dropCombatFlag(Player player, int fortId) {
      Fort fort = FortManager.getInstance().getFortById(fortId);

      for (CombatFlag cf : this._flagList.get(fort.getResidenceId())) {
         if (cf.getPlayerObjectId() == player.getObjectId()) {
            cf.dropIt();
            if (fort.getSiege().isInProgress()) {
               cf.spawnMe();
            }
         }
      }
   }

   public static FortSiegeManager getInstance() {
      return FortSiegeManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FortSiegeManager INSTANCE = new FortSiegeManager();
   }
}
