package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoPotionTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

public class OfflinePlayTable {
   private static final Logger LOGGER = Logger.getLogger(OfflinePlayTable.class.getName());
   private static final String LOAD_PLAYER_IDS = "SELECT DISTINCT charId FROM character_offline_play";
   private static final String LOAD_PLAYER = "SELECT * FROM character_offline_play WHERE charId=?";
   private static final String SAVE_PLAYER = "INSERT INTO character_offline_play (charId, type, id) VALUES (?, ?, ?)";
   private static final String REMOVE_PLAYER = "DELETE FROM character_offline_play WHERE charId=?";
   private static final String LOAD_GROUP_LEADER_IDS = "SELECT DISTINCT leaderId FROM character_offline_play_group";
   private static final String LOAD_GROUP_MEMBERS = "SELECT charId, type FROM character_offline_play_group WHERE leaderId=?";
   private static final String SAVE_GROUP_MEMBER = "INSERT INTO character_offline_play_group (leaderId, charId, type) VALUES (?, ?, ?)";
   private static final String REMOVE_GROUP_MEMBERS = "TRUNCATE TABLE character_offline_play_group";
   private static final int TYPE_ACTIVE_SOULSHOT = 0;
   private static final int TYPE_AUTO_SUPPLY_ITEM = 1;
   private static final int TYPE_AUTO_BUFF = 2;
   private static final int TYPE_AUTO_SKILL = 3;
   private static final int TYPE_AUTO_ACTION = 4;
   private static final int TYPE_AUTO_POTION_ITEM = 5;
   private static final int TYPE_CUSTOM_AUTO_POTION_SYSTEM = 100;

   protected OfflinePlayTable() {
   }

   public void restoreOfflinePlayers() {
      LOGGER.info(this.getClass().getSimpleName() + ": Loading offline auto players...");

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement statement = con.createStatement();
         ResultSet result = statement.executeQuery("SELECT DISTINCT charId FROM character_offline_play");
      ) {
         int nPlayers = 0;

         while (result.next()) {
            Player player = null;

            try {
               player = Player.load(result.getInt("charId"));
               player.setOnlineStatus(true, false);
               player.spawnMe(player.getX(), player.getY(), player.getZ());

               try (PreparedStatement stmItems = con.prepareStatement("SELECT * FROM character_offline_play WHERE charId=?")) {
                  stmItems.setInt(1, player.getObjectId());

                  try (ResultSet items = stmItems.executeQuery()) {
                     AutoUseSettingsHolder settings = player.getAutoUseSettings();

                     while (items.next()) {
                        int type = items.getInt("type");
                        int id = items.getInt("id");
                        switch (type) {
                           case 0:
                              player.addAutoSoulShot(id);
                              break;
                           case 1:
                              settings.getAutoSupplyItems().add(id);
                              break;
                           case 2:
                              settings.getAutoBuffs().add(id);
                              break;
                           case 3:
                              settings.getAutoSkills().add(id);
                              break;
                           case 4:
                              settings.getAutoActions().add(id);
                              break;
                           case 5:
                              settings.setAutoPotionItem(id);
                              break;
                           case 100:
                              if (AutoPotionsConfig.AUTO_POTIONS_ENABLED) {
                                 AutoPotionTaskManager.getInstance().add(player);
                              }
                        }
                     }
                  }
               }

               player.setOfflinePlay(true);
               player.setOnlineStatus(true, true);
               player.restoreEffects();
               player.setRunning();
               if (OfflineTradeConfig.OFFLINE_SET_NAME_COLOR) {
                  player.getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
               }

               if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty()) {
                  player.startAbnormalVisualEffect(
                     true,
                     new AbnormalVisualEffect[]{
                        OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size()))
                     }
                  );
               }

               AutoPlayTaskManager.getInstance().startAutoPlay(player);
               AutoUseTaskManager.getInstance().startAutoUseTask(player);
               nPlayers++;
            } catch (Exception var35) {
               LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error loading auto player " + player, (Throwable)var35);
               if (player != null) {
                  Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
               }
            }
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + nPlayers + " offline auto players.");
      } catch (Exception var39) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while loading offline auto players. " + var39.getMessage(), (Throwable)var39);
      }

      if (AutoPlayConfig.ENABLE_AUTO_ASSIST) {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement statement = con.createStatement();
            ResultSet result = statement.executeQuery("SELECT DISTINCT leaderId FROM character_offline_play_group");
         ) {
            int nParties = 0;

            while (result.next()) {
               int leaderId = result.getInt("leaderId");
               Player leader = World.getPlayer(leaderId);
               if (leader != null) {
                  nParties++;

                  try (PreparedStatement stmtMembers = con.prepareStatement("SELECT charId, type FROM character_offline_play_group WHERE leaderId=?")) {
                     stmtMembers.setInt(1, leaderId);

                     try (ResultSet members = stmtMembers.executeQuery()) {
                        Party party = null;

                        while (members.next()) {
                           int charId = members.getInt("charId");
                           Player member = World.getPlayer(charId);
                           if (member != null) {
                              if (party == null) {
                                 party = new Party(leader, PartyDistributionType.findById(members.getInt("type")));
                              }

                              member.joinParty(party);
                           }
                        }
                     }
                  }
               }
            }

            LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + nParties + " offline groups.");
         } catch (Exception var32) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while restoring offline groups. " + var32.getMessage(), (Throwable)var32);
         }
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement statement = con.createStatement();
      ) {
         statement.executeUpdate("TRUNCATE TABLE character_offline_play_group");
      } catch (Exception var26) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error clearing offline group data. " + var26.getMessage(), (Throwable)var26);
      }
   }

   public void storeOfflinePlay(Player player) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement1 = con.prepareStatement("DELETE FROM character_offline_play WHERE charId=?");
         PreparedStatement statement2 = con.prepareStatement("INSERT INTO character_offline_play (charId, type, id) VALUES (?, ?, ?)");
      ) {
         int playerObjectId = player.getObjectId();
         statement1.setInt(1, playerObjectId);
         statement1.execute();

         try {
            for (int shotId : player.getAutoSoulShot()) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 0);
               statement2.setInt(3, shotId);
               statement2.addBatch();
            }

            for (Integer itemId : player.getAutoUseSettings().getAutoSupplyItems()) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 1);
               statement2.setInt(3, itemId);
               statement2.addBatch();
            }

            for (Integer buffId : player.getAutoUseSettings().getAutoBuffs()) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 2);
               statement2.setInt(3, buffId);
               statement2.addBatch();
            }

            for (Integer skillId : player.getAutoUseSettings().getAutoSkills()) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 3);
               statement2.setInt(3, skillId);
               statement2.addBatch();
            }

            for (Integer actionId : player.getAutoUseSettings().getAutoActions()) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 4);
               statement2.setInt(3, actionId);
               statement2.addBatch();
            }

            int autoPotionItem = player.getAutoUseSettings().getAutoPotionItem();
            if (autoPotionItem > 0) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 5);
               statement2.setInt(3, autoPotionItem);
               statement2.addBatch();
            }

            if (AutoPotionsConfig.AUTO_POTIONS_ENABLED && AutoPotionTaskManager.getInstance().hasPlayer(player)) {
               statement2.setInt(1, playerObjectId);
               statement2.setInt(2, 100);
               statement2.setInt(3, 0);
               statement2.addBatch();
            }

            statement2.executeBatch();
         } catch (Exception var11) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while saving offline auto player " + player, (Throwable)var11);
         }
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while saving offline auto players. " + var15.getMessage(), (Throwable)var15);
      }
   }

   public void storeOfflineGroups() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("INSERT INTO character_offline_play_group (leaderId, charId, type) VALUES (?, ?, ?)");
      ) {
         for (Player player : World.getPlayers()) {
            if (player.isAutoPlaying()) {
               Party party = player.getParty();
               if (party != null && !party.isLeader(player)) {
                  Player leader = party.getLeader();
                  if (leader.isAutoPlaying()) {
                     statement.setInt(1, leader.getObjectId());
                     statement.setInt(2, player.getObjectId());
                     statement.setInt(3, party.getDistributionType().getId());
                     statement.addBatch();
                  }
               }
            }
         }

         statement.executeBatch();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while storing offline parties. " + var11.getMessage(), (Throwable)var11);
      }
   }

   public void removeOfflinePlay(Player player) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("DELETE FROM character_offline_play WHERE charId=?");
      ) {
         statement.setInt(1, player.getObjectId());
         statement.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Error while removing offline auto players. " + var10.getMessage(), (Throwable)var10);
      }
   }

   public static OfflinePlayTable getInstance() {
      return OfflinePlayTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final OfflinePlayTable INSTANCE = new OfflinePlayTable();
   }
}
