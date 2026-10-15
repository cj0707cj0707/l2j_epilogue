package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class Hero {
   private static final Logger LOGGER = Logger.getLogger(Hero.class.getName());
   private static final DateTimeFormatter DIARY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
   private static final DateTimeFormatter FIGHT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
   private static final String GET_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1";
   private static final String GET_ALL_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId";
   private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
   private static final String INSERT_HERO = "INSERT INTO heroes (charId, class_id, count, played, claimed) VALUES (?,?,?,?,?)";
   private static final String UPDATE_HERO = "UPDATE heroes SET class_id = ?, count = ?, played = ?, claimed = ? WHERE charId = ?";
   private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?";
   private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";
   private static final Map<Integer, StatSet> HEROES = new ConcurrentHashMap<>();
   private static final Map<Integer, StatSet> COMPLETE_HEROS = new ConcurrentHashMap<>();
   private static final Map<Integer, StatSet> HERO_COUNTS = new ConcurrentHashMap<>();
   private static final Map<Integer, List<StatSet>> HERO_FIGHTS = new ConcurrentHashMap<>();
   private static final Map<Integer, List<StatSet>> HERO_DIARY = new ConcurrentHashMap<>();
   private static final Map<Integer, String> HERO_MESSAGE = new ConcurrentHashMap<>();
   public static final String COUNT = "count";
   public static final String PLAYED = "played";
   public static final String CLAIMED = "claimed";
   public static final String CLAN_NAME = "clan_name";
   public static final String CLAN_CREST = "clan_crest";
   public static final String ALLY_NAME = "ally_name";
   public static final String ALLY_CREST = "ally_crest";
   public static final int ACTION_RAID_KILLED = 1;
   public static final int ACTION_HERO_GAINED = 2;
   public static final int ACTION_CASTLE_TAKEN = 3;

   protected Hero() {
      if (OlympiadConfig.OLYMPIAD_ENABLED) {
         this.init();
      }
   }

   private void init() {
      HEROES.clear();
      COMPLETE_HEROS.clear();
      HERO_COUNTS.clear();
      HERO_FIGHTS.clear();
      HERO_DIARY.clear();
      HERO_MESSAGE.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s1 = con.createStatement();
         ResultSet rset = s1.executeQuery(
            "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1"
         );
         PreparedStatement ps = con.prepareStatement(
            "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?"
         );
         Statement s2 = con.createStatement();
         ResultSet rset2 = s2.executeQuery(
            "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId"
         );
      ) {
         while (rset.next()) {
            StatSet hero = new StatSet();
            int charId = rset.getInt("charId");
            hero.set("char_name", rset.getString("char_name"));
            hero.set("class_id", rset.getInt("class_id"));
            hero.set("count", rset.getInt("count"));
            hero.set("played", rset.getInt("played"));
            hero.set("claimed", Boolean.parseBoolean(rset.getString("claimed")));
            this.loadFights(charId);
            this.loadDiary(charId);
            this.loadMessage(charId);
            this.processHeros(ps, charId, hero);
            HEROES.put(charId, hero);
         }

         while (rset2.next()) {
            StatSet hero = new StatSet();
            int charId = rset2.getInt("charId");
            hero.set("char_name", rset2.getString("char_name"));
            hero.set("class_id", rset2.getInt("class_id"));
            hero.set("count", rset2.getInt("count"));
            hero.set("played", rset2.getInt("played"));
            hero.set("claimed", Boolean.parseBoolean(rset2.getString("claimed")));
            this.processHeros(ps, charId, hero);
            COMPLETE_HEROS.put(charId, hero);
         }
      } catch (SQLException var21) {
         LOGGER.warning("Hero System: Could not load Heroes: " + var21.getMessage());
      }

      LOGGER.info("Hero System: Loaded " + HEROES.size() + " Heroes.");
      LOGGER.info("Hero System: Loaded " + COMPLETE_HEROS.size() + " all time Heroes.");
   }

   private void processHeros(PreparedStatement ps, int charId, StatSet hero) throws SQLException {
      ps.setInt(1, charId);

      try (ResultSet rs = ps.executeQuery()) {
         if (rs.next()) {
            int clanId = rs.getInt("clanid");
            int allyId = rs.getInt("allyId");
            String clanName = "";
            String allyName = "";
            int clanCrest = 0;
            int allyCrest = 0;
            if (clanId > 0) {
               clanName = ClanTable.getInstance().getClan(clanId).getName();
               clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
               if (allyId > 0) {
                  allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
                  allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
               }
            }

            hero.set("clan_crest", clanCrest);
            hero.set("clan_name", clanName);
            hero.set("ally_crest", allyCrest);
            hero.set("ally_name", allyName);
         }

         ps.clearParameters();
      }
   }

   private String calcFightTime(long fightTimeValue) {
      String format = String.format("%%0%dd", 2);
      long fightTime = fightTimeValue / 1000L;
      return String.format(format, fightTime % 3600L / 60L) + ":" + String.format(format, fightTime % 60L);
   }

   public void loadMessage(int charId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT message FROM heroes WHERE charId=?");
      ) {
         ps.setInt(1, charId);

         try (ResultSet rset = ps.executeQuery()) {
            if (rset.next()) {
               HERO_MESSAGE.put(charId, rset.getString("message"));
            }
         }
      } catch (SQLException var13) {
         LOGGER.warning("Hero System: Could not load Hero Message for CharId: " + charId + ": " + var13.getMessage());
      }
   }

   public void loadDiary(int charId) {
      List<StatSet> diary = new ArrayList<>();
      int diaryentries = 0;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM  heroes_diary WHERE charId=? ORDER BY time ASC");
      ) {
         ps.setInt(1, charId);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               StatSet diaryEntry = new StatSet();
               long time = rset.getLong("time");
               int action = rset.getInt("action");
               int param = rset.getInt("param");
               String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
               diaryEntry.set("date", date);
               if (action == 1) {
                  NpcTemplate template = NpcData.getInstance().getTemplate(param);
                  if (template != null) {
                     diaryEntry.set("action", template.getName() + " was defeated");
                  }
               } else if (action == 2) {
                  diaryEntry.set("action", "Gained Hero status");
               } else if (action == 3) {
                  Castle castle = CastleManager.getInstance().getCastleById(param);
                  if (castle != null) {
                     diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
                  }
               }

               diary.add(diaryEntry);
               diaryentries++;
            }
         }

         HERO_DIARY.put(charId, diary);
         LOGGER.info("Hero System: Loaded " + diaryentries + " diary entries for Hero: " + CharInfoTable.getInstance().getNameById(charId));
      } catch (SQLException var20) {
         LOGGER.warning("Hero System: Could not load Hero Diary for CharId: " + charId + ": " + var20.getMessage());
      }
   }

   public void loadFights(int charId) {
      List<StatSet> fights = new ArrayList<>();
      StatSet heroCountData = new StatSet();
      Calendar data = Calendar.getInstance();
      data.set(5, 1);
      data.set(11, 0);
      data.set(12, 0);
      data.set(14, 0);
      long from = data.getTimeInMillis();
      int numberOfFights = 0;
      int victories = 0;
      int losses = 0;
      int draws = 0;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC");
      ) {
         ps.setInt(1, charId);
         ps.setInt(2, charId);
         ps.setLong(3, from);

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               int charOneId = rset.getInt("charOneId");
               int charOneClass = rset.getInt("charOneClass");
               int charTwoId = rset.getInt("charTwoId");
               int charTwoClass = rset.getInt("charTwoClass");
               int winner = rset.getInt("winner");
               long start = rset.getLong("start");
               long time = rset.getLong("time");
               int classed = rset.getInt("classed");
               if (charId == charOneId) {
                  String name = CharInfoTable.getInstance().getNameById(charTwoId);
                  String cls = ClassListData.getInstance().getClass(charTwoClass).getClassName();
                  if (name != null && cls != null) {
                     StatSet fight = new StatSet();
                     fight.set("oponent", name);
                     fight.set("oponentclass", cls);
                     fight.set("time", this.calcFightTime(time));
                     String date = FIGHT_DATE_FORMAT.format(Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()));
                     fight.set("start", date);
                     fight.set("classed", classed);
                     if (winner == 1) {
                        fight.set("result", "<font color=\"00ff00\">victory</font>");
                        victories++;
                     } else if (winner == 2) {
                        fight.set("result", "<font color=\"ff0000\">loss</font>");
                        losses++;
                     } else if (winner == 0) {
                        fight.set("result", "<font color=\"ffff00\">draw</font>");
                        draws++;
                     }

                     fights.add(fight);
                     numberOfFights++;
                  }
               } else if (charId == charTwoId) {
                  String name = CharInfoTable.getInstance().getNameById(charOneId);
                  String cls = ClassListData.getInstance().getClass(charOneClass).getClassName();
                  if (name != null && cls != null) {
                     StatSet fight = new StatSet();
                     fight.set("oponent", name);
                     fight.set("oponentclass", cls);
                     fight.set("time", this.calcFightTime(time));
                     String date = FIGHT_DATE_FORMAT.format(Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()));
                     fight.set("start", date);
                     fight.set("classed", classed);
                     if (winner == 1) {
                        fight.set("result", "<font color=\"ff0000\">loss</font>");
                        losses++;
                     } else if (winner == 2) {
                        fight.set("result", "<font color=\"00ff00\">victory</font>");
                        victories++;
                     } else if (winner == 0) {
                        fight.set("result", "<font color=\"ffff00\">draw</font>");
                        draws++;
                     }

                     fights.add(fight);
                     numberOfFights++;
                  }
               }
            }
         }

         heroCountData.set("victory", victories);
         heroCountData.set("draw", draws);
         heroCountData.set("loss", losses);
         HERO_COUNTS.put(charId, heroCountData);
         HERO_FIGHTS.put(charId, fights);
         LOGGER.info("Hero System: Loaded " + numberOfFights + " fights for Hero: " + CharInfoTable.getInstance().getNameById(charId));
      } catch (SQLException var34) {
         LOGGER.warning("Hero System: Could not load Hero fights history for CharId: " + charId + ": " + var34);
      }
   }

   public Map<Integer, StatSet> getHeroes() {
      return HEROES;
   }

   public int getHeroByClass(int classid) {
      for (Entry<Integer, StatSet> e : HEROES.entrySet()) {
         if (e.getValue().getInt("class_id") == classid) {
            return e.getKey();
         }
      }

      return 0;
   }

   public void resetData() {
      HERO_DIARY.clear();
      HERO_FIGHTS.clear();
      HERO_COUNTS.clear();
      HERO_MESSAGE.clear();
   }

   public void showHeroDiary(Player player, int heroclass, int charid, int page) {
      int perpage = 10;
      List<StatSet> mainList = HERO_DIARY.get(charid);
      if (mainList != null) {
         NpcHtmlMessage diaryReply = new NpcHtmlMessage();
         String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herodiary.htm");
         String heroMessage = HERO_MESSAGE.get(charid);
         if (htmContent != null && heroMessage != null) {
            diaryReply.setHtml(htmContent);
            diaryReply.replace("%heroname%", CharInfoTable.getInstance().getNameById(charid));
            diaryReply.replace("%message%", heroMessage);
            diaryReply.disableValidation();
            if (mainList.isEmpty()) {
               diaryReply.replace("%list%", "");
               diaryReply.replace("%buttprev%", "");
               diaryReply.replace("%buttnext%", "");
            } else {
               List<StatSet> list = new ArrayList<>(mainList);
               Collections.reverse(list);
               boolean color = true;
               StringBuilder fList = new StringBuilder(500);
               int counter = 0;
               int breakat = 0;

               for (int i = (page - 1) * 10; i < list.size(); i++) {
                  breakat = i;
                  StatSet diaryEntry = list.get(i);
                  fList.append("<tr><td>");
                  if (color) {
                     fList.append("<table width=270 bgcolor=\"131210\">");
                  } else {
                     fList.append("<table width=270>");
                  }

                  fList.append("<tr><td width=270><font color=\"LEVEL\">" + diaryEntry.getString("date") + ":xx</font></td></tr>");
                  fList.append("<tr><td width=270>" + diaryEntry.getString("action", "") + "</td></tr>");
                  fList.append("<tr><td>&nbsp;</td></tr></table>");
                  fList.append("</td></tr>");
                  color = !color;
                  if (++counter >= 10) {
                     break;
                  }
               }

               if (breakat < list.size() - 1) {
                  diaryReply.replace(
                     "%buttprev%",
                     "<button value=\"Prev\" action=\"bypass _diary?class="
                        + heroclass
                        + "&page="
                        + (page + 1)
                        + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
                  );
               } else {
                  diaryReply.replace("%buttprev%", "");
               }

               if (page > 1) {
                  diaryReply.replace(
                     "%buttnext%",
                     "<button value=\"Next\" action=\"bypass _diary?class="
                        + heroclass
                        + "&page="
                        + (page - 1)
                        + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
                  );
               } else {
                  diaryReply.replace("%buttnext%", "");
               }

               diaryReply.replace("%list%", fList.toString());
            }

            player.sendPacket(diaryReply);
         }
      }
   }

   public void showHeroFights(Player player, int heroclass, int charid, int page) {
      int perpage = 20;
      int win = 0;
      int loss = 0;
      int draw = 0;
      List<StatSet> heroFights = HERO_FIGHTS.get(charid);
      if (heroFights != null) {
         NpcHtmlMessage fightReply = new NpcHtmlMessage();
         String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herohistory.htm");
         if (htmContent != null) {
            fightReply.setHtml(htmContent);
            fightReply.replace("%heroname%", CharInfoTable.getInstance().getNameById(charid));
            if (heroFights.isEmpty()) {
               fightReply.replace("%list%", "");
               fightReply.replace("%buttprev%", "");
               fightReply.replace("%buttnext%", "");
            } else {
               StatSet heroCount = HERO_COUNTS.get(charid);
               if (heroCount != null) {
                  win = heroCount.getInt("victory");
                  loss = heroCount.getInt("loss");
                  draw = heroCount.getInt("draw");
               }

               boolean color = true;
               StringBuilder fList = new StringBuilder(500);
               int counter = 0;
               int breakat = 0;

               for (int i = (page - 1) * 20; i < heroFights.size(); i++) {
                  breakat = i;
                  StatSet fight = heroFights.get(i);
                  fList.append("<tr><td>");
                  if (color) {
                     fList.append("<table width=270 bgcolor=\"131210\">");
                  } else {
                     fList.append("<table width=270>");
                  }

                  fList.append(
                     "<tr><td width=220><font color=\"LEVEL\">"
                        + fight.getString("start")
                        + "</font>&nbsp;&nbsp;"
                        + fight.getString("result")
                        + "</td><td width=50 align=right>"
                        + (fight.getInt("classed") > 0 ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>")
                        + "</td></tr>"
                  );
                  fList.append(
                     "<tr><td width=220>vs "
                        + fight.getString("oponent")
                        + " ("
                        + fight.getString("oponentclass")
                        + ")</td><td width=50 align=right>("
                        + fight.getString("time")
                        + ")</td></tr>"
                  );
                  fList.append("<tr><td colspan=2>&nbsp;</td></tr></table>");
                  fList.append("</td></tr>");
                  color = !color;
                  if (++counter >= 20) {
                     break;
                  }
               }

               if (breakat < heroFights.size() - 1) {
                  fightReply.replace(
                     "%buttprev%",
                     "<button value=\"Prev\" action=\"bypass _match?class="
                        + heroclass
                        + "&page="
                        + (page + 1)
                        + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
                  );
               } else {
                  fightReply.replace("%buttprev%", "");
               }

               if (page > 1) {
                  fightReply.replace(
                     "%buttnext%",
                     "<button value=\"Next\" action=\"bypass _match?class="
                        + heroclass
                        + "&page="
                        + (page - 1)
                        + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
                  );
               } else {
                  fightReply.replace("%buttnext%", "");
               }

               fightReply.replace("%list%", fList.toString());
            }

            fightReply.replace("%win%", String.valueOf(win));
            fightReply.replace("%draw%", String.valueOf(draw));
            fightReply.replace("%loos%", String.valueOf(loss));
            player.sendPacket(fightReply);
         }
      }
   }

   public synchronized void computeNewHeroes(List<StatSet> newHeroes) {
      this.updateHeroes(true);

      for (Integer objectId : HEROES.keySet()) {
         Player player = World.getPlayer(objectId);
         if (player != null) {
            player.setHero(false);

            for (int i = 0; i < 25; i++) {
               Item equippedItem = player.getInventory().getPaperdollItem(i);
               if (equippedItem != null && equippedItem.isHeroItem()) {
                  player.getInventory().unEquipItemInSlot(i);
               }
            }

            InventoryUpdate iu = new InventoryUpdate();

            for (Item item : player.getInventory().getAvailableItems(false, false, false)) {
               if (item != null && item.isHeroItem()) {
                  player.destroyItem(ItemProcessType.DESTROY, item, null, true);
                  iu.addRemovedItem(item);
               }
            }

            if (!iu.getItems().isEmpty()) {
               player.sendInventoryUpdate(iu);
            }

            player.broadcastUserInfo();
         }
      }

      this.deleteItemsInDb();
      HEROES.clear();
      if (!newHeroes.isEmpty()) {
         for (StatSet hero : newHeroes) {
            int charId = hero.getInt("charId");
            StatSet oldHero = COMPLETE_HEROS.get(charId);
            if (oldHero != null) {
               int count = oldHero.getInt("count");
               oldHero.set("count", count + 1);
               oldHero.set("played", 1);
               oldHero.set("claimed", false);
               HEROES.put(charId, oldHero);
            } else {
               StatSet newHero = new StatSet();
               newHero.set("char_name", hero.getString("char_name"));
               newHero.set("class_id", hero.getInt("class_id"));
               newHero.set("count", 1);
               newHero.set("played", 1);
               newHero.set("claimed", false);
               HEROES.put(charId, newHero);
            }
         }

         this.updateHeroes(false);
      }
   }

   public void updateHeroes(boolean setDefault) {
      try (Connection con = DatabaseFactory.getConnection()) {
         if (setDefault) {
            try (Statement s = con.createStatement()) {
               s.executeUpdate("UPDATE heroes SET played = 0");
            }
         } else {
            for (Entry<Integer, StatSet> entry : HEROES.entrySet()) {
               StatSet hero = entry.getValue();
               int heroId = entry.getKey();
               if (!COMPLETE_HEROS.containsKey(heroId)) {
                  try (PreparedStatement insert = con.prepareStatement("INSERT INTO heroes (charId, class_id, count, played, claimed) VALUES (?,?,?,?,?)")) {
                     insert.setInt(1, heroId);
                     insert.setInt(2, hero.getInt("class_id"));
                     insert.setInt(3, hero.getInt("count", 0));
                     insert.setInt(4, hero.getInt("played", 0));
                     insert.setString(5, String.valueOf(hero.getBoolean("claimed", false)));
                     insert.execute();
                     insert.close();
                  }

                  try (PreparedStatement statement = con.prepareStatement(
                        "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?"
                     )) {
                     statement.setInt(1, heroId);

                     try (ResultSet rset = statement.executeQuery()) {
                        if (rset.next()) {
                           int clanId = rset.getInt("clanid");
                           int allyId = rset.getInt("allyId");
                           String clanName = "";
                           String allyName = "";
                           int clanCrest = 0;
                           int allyCrest = 0;
                           if (clanId > 0) {
                              clanName = ClanTable.getInstance().getClan(clanId).getName();
                              clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
                              if (allyId > 0) {
                                 allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
                                 allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
                              }
                           }

                           hero.set("clan_crest", clanCrest);
                           hero.set("clan_name", clanName);
                           hero.set("ally_crest", allyCrest);
                           hero.set("ally_name", allyName);
                        }
                     }
                  }

                  HEROES.put(heroId, hero);
                  COMPLETE_HEROS.put(heroId, hero);
               } else {
                  try (PreparedStatement statement = con.prepareStatement("UPDATE heroes SET class_id = ?, count = ?, played = ?, claimed = ? WHERE charId = ?")) {
                     statement.setInt(1, hero.getInt("class_id"));
                     statement.setInt(2, hero.getInt("count", 0));
                     statement.setInt(3, hero.getInt("played", 0));
                     statement.setString(4, String.valueOf(hero.getBoolean("claimed", false)));
                     statement.setInt(5, heroId);
                     statement.execute();
                  }
               }
            }
         }
      } catch (SQLException var27) {
         LOGGER.warning("Hero System: Could not update Heroes: " + var27.getMessage());
      }
   }

   public void setHeroGained(int charId) {
      this.setDiaryData(charId, 2, 0);
   }

   public void setRBkilled(int charId, int npcId) {
      this.setDiaryData(charId, 1, npcId);
      NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
      List<StatSet> list = HERO_DIARY.get(charId);
      if (list != null && template != null) {
         StatSet diaryEntry = new StatSet();
         String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()));
         diaryEntry.set("date", date);
         diaryEntry.set("action", template.getName() + " was defeated");
         list.add(diaryEntry);
      }
   }

   public void setCastleTaken(int charId, int castleId) {
      this.setDiaryData(charId, 3, castleId);
      Castle castle = CastleManager.getInstance().getCastleById(castleId);
      List<StatSet> list = HERO_DIARY.get(charId);
      if (list != null && castle != null) {
         StatSet diaryEntry = new StatSet();
         String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()));
         diaryEntry.set("date", date);
         diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
         list.add(diaryEntry);
      }
   }

   public void setDiaryData(int charId, int action, int param) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("INSERT INTO heroes_diary (charId, time, action, param) values(?,?,?,?)");
      ) {
         ps.setInt(1, charId);
         ps.setLong(2, System.currentTimeMillis());
         ps.setInt(3, action);
         ps.setInt(4, param);
         ps.execute();
      } catch (SQLException var12) {
         LOGGER.severe("SQL exception while saving DiaryData: " + var12.getMessage());
      }
   }

   public void setHeroMessage(Player player, String message) {
      HERO_MESSAGE.put(player.getObjectId(), message);
   }

   public void saveHeroMessage(int charId) {
      if (HERO_MESSAGE.containsKey(charId)) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE heroes SET message=? WHERE charId=?;");
         ) {
            ps.setString(1, HERO_MESSAGE.get(charId));
            ps.setInt(2, charId);
            ps.execute();
         } catch (SQLException var10) {
            LOGGER.severe("SQL exception while saving HeroMessage:" + var10.getMessage());
         }
      }
   }

   private void deleteItemsInDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
      ) {
         s.executeUpdate(
            "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)"
         );
      } catch (SQLException var9) {
         LOGGER.warning("Heroes: " + var9.getMessage());
      }
   }

   public void shutdown() {
      HERO_MESSAGE.keySet().forEach(this::saveHeroMessage);
   }

   public boolean isHero(int objectId) {
      return HEROES.containsKey(objectId) && HEROES.get(objectId).getBoolean("claimed");
   }

   public boolean isUnclaimedHero(int objectId) {
      return HEROES.containsKey(objectId) && !HEROES.get(objectId).getBoolean("claimed");
   }

   public void claimHero(Player player) {
      StatSet hero = HEROES.get(player.getObjectId());
      if (hero == null) {
         hero = new StatSet();
         HEROES.put(player.getObjectId(), hero);
      }

      hero.set("claimed", true);
      Clan clan = player.getClan();
      if (clan != null && clan.getLevel() >= 5) {
         clan.addReputationScore(FeatureConfig.HERO_POINTS);
         SystemMessage sm = new SystemMessage(
            SendMessageLocalisationData.getLocalisation(
               player,
               "Clan member "
                  + CharInfoTable.getInstance().getNameById(player.getObjectId())
                  + " was named a hero. "
                  + FeatureConfig.HERO_POINTS
                  + " points have been added to your clan's reputation score."
            )
         );
         clan.broadcastToOnlineMembers(sm);
      }

      player.setHero(true);
      player.broadcastPacket(new SocialAction(player.getObjectId(), 20016));
      player.broadcastUserInfo();
      this.setHeroGained(player.getObjectId());
      this.loadFights(player.getObjectId());
      this.loadDiary(player.getObjectId());
      HERO_MESSAGE.put(player.getObjectId(), "");
      this.updateHeroes(false);
   }

   public static Hero getInstance() {
      return Hero.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final Hero INSTANCE = new Hero();
   }
}
