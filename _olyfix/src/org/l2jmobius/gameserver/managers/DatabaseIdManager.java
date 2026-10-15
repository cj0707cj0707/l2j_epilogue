package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.IdManagerConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;

public class DatabaseIdManager {
   private static final Logger LOGGER = Logger.getLogger(DatabaseIdManager.class.getName());
   private static final String[] CLEANUP_QUERIES = new String[]{
      "DELETE FROM account_gsdata WHERE account_gsdata.account_name NOT IN (SELECT account_name FROM characters)",
      "DELETE FROM character_contacts WHERE character_contacts.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_contacts WHERE character_contacts.contactId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_friends WHERE character_friends.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_friends WHERE character_friends.friendId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_hennas WHERE character_hennas.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_macroses WHERE character_macroses.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_quests WHERE character_quests.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_recipebook WHERE character_recipebook.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_recipeshoplist WHERE character_recipeshoplist.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_shortcuts WHERE character_shortcuts.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_skills WHERE character_skills.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_skills_save WHERE character_skills_save.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_subclasses WHERE character_subclasses.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_raid_points WHERE character_raid_points.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_instance_time WHERE character_instance_time.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM items WHERE items.owner_id NOT IN (SELECT charId FROM characters) AND items.owner_id NOT IN (SELECT clan_id FROM clan_data) AND items.owner_id != -1",
      "DELETE FROM items WHERE items.owner_id = -1 AND loc LIKE 'MAIL' AND loc_data NOT IN (SELECT messageId FROM messages WHERE senderId = -1)",
      "DELETE FROM item_auction_bid WHERE item_auction_bid.playerObjId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM item_attributes WHERE item_attributes.itemId NOT IN (SELECT object_id FROM items)",
      "DELETE FROM item_elementals WHERE item_elementals.itemId NOT IN (SELECT object_id FROM items)",
      "DELETE FROM cursed_weapons WHERE cursed_weapons.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM heroes WHERE heroes.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM olympiad_nobles WHERE olympiad_nobles.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM olympiad_nobles_eom WHERE olympiad_nobles_eom.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items)",
      "DELETE FROM seven_signs WHERE seven_signs.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM merchant_lease WHERE merchant_lease.player_id NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_recommends WHERE character_recommends.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_recommends WHERE character_recommends.target_id NOT IN (SELECT charId FROM characters)",
      "DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT charId FROM characters)",
      "DELETE FROM clan_data WHERE clan_data.clan_id NOT IN (SELECT clanid FROM characters)",
      "DELETE FROM olympiad_fights WHERE olympiad_fights.charOneId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM olympiad_fights WHERE olympiad_fights.charTwoId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM heroes_diary WHERE heroes_diary.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_offline_trade WHERE character_offline_trade.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_offline_trade_items WHERE character_offline_trade_items.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_offline_play WHERE character_offline_play.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_offline_play_group WHERE character_offline_play_group.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_offline_play_group WHERE character_offline_play_group.leaderId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_tpbookmark WHERE character_tpbookmark.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM character_variables WHERE character_variables.charId NOT IN (SELECT charId FROM characters)",
      "DELETE FROM clan_privs WHERE clan_privs.clan_id NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clan_skills WHERE clan_skills.clan_id NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clan_wars WHERE clan_wars.clan1 NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clan_wars WHERE clan_wars.clan2 NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clanhall_functions WHERE clanhall_functions.hall_id NOT IN (SELECT id FROM clanhall WHERE ownerId <> 0 union all SELECT clanHallId FROM siegable_clanhall WHERE ownerId <> 0)",
      "DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM clan_notices WHERE clan_notices.clan_id NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM auction_bid WHERE auction_bid.bidderId NOT IN (SELECT clan_id FROM clan_data)",
      "DELETE FROM forums WHERE forums.forum_owner_id NOT IN (SELECT clan_id FROM clan_data) AND forums.forum_parent=2",
      "DELETE FROM forums WHERE forums.forum_owner_id NOT IN (SELECT charId FROM characters) AND forums.forum_parent=3",
      "DELETE FROM posts WHERE posts.post_forum_id NOT IN (SELECT forum_id FROM forums)",
      "DELETE FROM topic WHERE topic.topic_forum_id NOT IN (SELECT forum_id FROM forums)"
   };
   private static final String[] UPDATE_QUERIES = new String[]{
      "UPDATE clan_data SET auction_bid_at = 0 WHERE auction_bid_at NOT IN (SELECT auctionId FROM auction_bid)",
      "UPDATE clan_data SET new_leader_id = 0 WHERE new_leader_id <> 0 AND new_leader_id NOT IN (SELECT charId FROM characters)",
      "UPDATE clan_subpledges SET leader_id=0 WHERE clan_subpledges.leader_id NOT IN (SELECT charId FROM characters) AND leader_id > 0",
      "UPDATE castle SET taxpercent=0 WHERE castle.id NOT IN (SELECT hasCastle FROM clan_data)",
      "UPDATE characters SET clanid=0, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0, clan_join_expiry_time=0, clan_create_expiry_time=0 WHERE characters.clanid > 0 AND characters.clanid NOT IN (SELECT clan_id FROM clan_data)",
      "UPDATE clanhall SET ownerId=0, paidUntil=0, paid=0 WHERE clanhall.ownerId NOT IN (SELECT clan_id FROM clan_data)",
      "UPDATE fort SET owner=0 WHERE owner NOT IN (SELECT clan_id FROM clan_data)"
   };
   private static final String UPDATE_CHARACTER_STATUS_QUERY = "UPDATE characters SET online = 0";
   private static final String[] EXPIRATION_CLEANUP_QUERIES = new String[]{
      "DELETE FROM character_instance_time WHERE time <= ?", "DELETE FROM character_skills_save WHERE restore_type = 1 AND systime <= ?"
   };
   private static final String[] EXTRACT_USED_OBJECT_ID_QUERIES = new String[]{
      "SELECT charId FROM characters",
      "SELECT object_id FROM items",
      "SELECT clan_id FROM clan_data",
      "SELECT object_id FROM itemsonground",
      "SELECT messageId FROM messages"
   };

   private DatabaseIdManager() {
   }

   public static void cleanDatabase() {
      if (IdManagerConfig.DATABASE_CLEAN_UP) {
         int cleanCount = 0;
         long cleanupStart = System.currentTimeMillis();
         if (WeddingConfig.ALLOW_WEDDING) {
            try (
               Connection con = DatabaseFactory.getConnection();
               Statement statement = con.createStatement();
            ) {
               statement.executeUpdate("DELETE FROM mods_wedding WHERE player1Id NOT IN (SELECT charId FROM characters)");
               statement.executeUpdate("DELETE FROM mods_wedding WHERE player2Id NOT IN (SELECT charId FROM characters)");
            } catch (Exception var25) {
               LOGGER.warning("DatabaseIdManager: Could not clean up invalid weddings: " + var25);
            }
         }

         for (String query : CLEANUP_QUERIES) {
            try (
               Connection con = DatabaseFactory.getConnection();
               Statement statement = con.createStatement();
            ) {
               cleanCount += statement.executeUpdate(query);
            } catch (Exception var22) {
               LOGGER.warning("DatabaseIdManager: Could not execute cleanup query: " + query + " - " + var22);
            }
         }

         LOGGER.info("DatabaseIdManager: Cleaned " + cleanCount + " elements in " + (System.currentTimeMillis() - cleanupStart) / 1000L + " seconds.");

         for (String query : UPDATE_QUERIES) {
            try (
               Connection con = DatabaseFactory.getConnection();
               Statement statement = con.createStatement();
            ) {
               statement.executeUpdate(query);
            } catch (Exception var19) {
               LOGGER.warning("DatabaseIdManager: Could not execute update query: " + query + " - " + var19);
            }
         }
      }
   }

   public static void cleanCharacterStatus() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement statement = con.createStatement();
      ) {
         statement.executeUpdate("UPDATE characters SET online = 0");
         LOGGER.info("DatabaseIdManager: Updated character online status.");
      } catch (Exception var8) {
         LOGGER.warning("DatabaseIdManager: Could not update characters online status: " + var8);
      }
   }

   public static void cleanTimestamps() {
      try (Connection con = DatabaseFactory.getConnection()) {
         int cleanCount = 0;

         for (String query : EXPIRATION_CLEANUP_QUERIES) {
            try (PreparedStatement statement = con.prepareStatement(query)) {
               statement.setLong(1, System.currentTimeMillis());
               cleanCount += statement.executeUpdate();
            }
         }

         LOGGER.info("DatabaseIdManager: Cleaned " + cleanCount + " expired timestamps.");
      } catch (Exception var13) {
         LOGGER.warning("DatabaseIdManager: Could not clean expired timestamps from database: " + var13);
      }
   }

   public static Set<Integer> getUsedIds() {
      Set<Integer> usedIds = ConcurrentHashMap.newKeySet();
      ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
      List<Future<?>> futures = new ArrayList<>();

      for (String query : EXTRACT_USED_OBJECT_ID_QUERIES) {
         futures.add(
            executor.submit(
               () -> {
                  try (
                     Connection con = DatabaseFactory.getConnection();
                     Statement statement = con.createStatement();
                     ResultSet result = statement.executeQuery(query);
                  ) {
                     while (result.next()) {
                        int id = result.getInt(1);
                        if (id >= IdManagerConfig.FIRST_OBJECT_ID && id <= IdManagerConfig.LAST_OBJECT_ID) {
                           usedIds.add(id);
                        } else {
                           LOGGER.warning(
                              "DatabaseIdManager: ID "
                                 + id
                                 + " in database is out of valid range ("
                                 + IdManagerConfig.FIRST_OBJECT_ID
                                 + " - "
                                 + IdManagerConfig.LAST_OBJECT_ID
                                 + ")"
                           );
                        }
                     }
                  } catch (Exception var13) {
                     LOGGER.severe("DatabaseIdManager: Could not initialize used IDs for query " + query + ": " + var13.getMessage());
                  }
               }
            )
         );
      }

      for (Future<?> future : futures) {
         try {
            future.get();
         } catch (Exception var8) {
            LOGGER.warning("Failed to parse file: " + var8.getMessage());
         }
      }

      executor.shutdown();

      try {
         executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException var7) {
         Thread.currentThread().interrupt();
         LOGGER.warning("DatabaseIdManager: Extraction interrupted: " + var7.getMessage());
      }

      return usedIds;
   }
}
