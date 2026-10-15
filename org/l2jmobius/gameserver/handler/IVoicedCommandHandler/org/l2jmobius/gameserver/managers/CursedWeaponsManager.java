package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CursedWeapon;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.FeedableBeast;
import org.l2jmobius.gameserver.entity.actor.instance.FestivalMonster;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.RiftInvader;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class CursedWeaponsManager {
   private static final Logger LOGGER = Logger.getLogger(CursedWeaponsManager.class.getName());
   private Map<Integer, CursedWeapon> _cursedWeapons;

   protected CursedWeaponsManager() {
      this.init();
   }

   private void init() {
      this._cursedWeapons = new HashMap<>();
      if (GeneralConfig.ALLOW_CURSED_WEAPONS) {
         this.load();
         this.restore();
         this.controlPlayers();
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded : " + this._cursedWeapons.size() + " cursed weapon(s).");
      }
   }

   public void reload() {
      this.init();
   }

   private void load() {
      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringComments(true);
         File file = new File(ServerConfig.DATAPACK_ROOT + "/data/CursedWeapons.xml");
         if (!file.exists()) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't find data/" + file.getName());
            return;
         }

         for (Node n = factory.newDocumentBuilder().parse(file).getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("list".equalsIgnoreCase(n.getNodeName())) {
               for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                  if ("item".equalsIgnoreCase(d.getNodeName())) {
                     NamedNodeMap attrs = d.getAttributes();
                     int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                     int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
                     String name = attrs.getNamedItem("name").getNodeValue();
                     CursedWeapon cw = new CursedWeapon(id, skillId, name);

                     for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                        if ("dropRate".equalsIgnoreCase(cd.getNodeName())) {
                           attrs = cd.getAttributes();
                           int val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           cw.setDropRate(val);
                        } else if ("duration".equalsIgnoreCase(cd.getNodeName())) {
                           attrs = cd.getAttributes();
                           int val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           cw.setDuration(val);
                        } else if ("durationLost".equalsIgnoreCase(cd.getNodeName())) {
                           attrs = cd.getAttributes();
                           int val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           cw.setDurationLost(val);
                        } else if ("disapearChance".equalsIgnoreCase(cd.getNodeName())) {
                           attrs = cd.getAttributes();
                           int val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           cw.setDisapearChance(val);
                        } else if ("stageKills".equalsIgnoreCase(cd.getNodeName())) {
                           attrs = cd.getAttributes();
                           int val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           cw.setStageKills(val);
                        }
                     }

                     this._cursedWeapons.put(id, cw);
                  }
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Error parsing cursed weapons file.", (Throwable)var12);
      }
   }

   private void restore() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT itemId, charId, playerKarma, playerPkKills, nbKills, endTime FROM cursed_weapons");
      ) {
         while (rs.next()) {
            CursedWeapon cw = this._cursedWeapons.get(rs.getInt("itemId"));
            cw.setPlayerId(rs.getInt("charId"));
            cw.setPlayerKarma(rs.getInt("playerKarma"));
            cw.setPlayerPkKills(rs.getInt("playerPkKills"));
            cw.setNbKills(rs.getInt("nbKills"));
            cw.setEndTime(rs.getLong("endTime"));
            cw.reActivate();
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore CursedWeapons data: " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void controlPlayers() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?");
      ) {
         for (CursedWeapon cw : this._cursedWeapons.values()) {
            if (!cw.isActivated()) {
               int itemId = cw.getItemId();
               ps.setInt(1, itemId);

               try (ResultSet rset = ps.executeQuery()) {
                  if (rset.next()) {
                     int playerId = rset.getInt("owner_id");
                     LOGGER.info("PROBLEM : Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");

                     try (PreparedStatement delete = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?")) {
                        delete.setInt(1, playerId);
                        delete.setInt(2, itemId);
                        if (delete.executeUpdate() != 1) {
                           LOGGER.warning("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
                        }
                     }

                     try (PreparedStatement update = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?")) {
                        update.setInt(1, cw.getPlayerKarma());
                        update.setInt(2, cw.getPlayerPkKills());
                        update.setInt(3, playerId);
                        if (update.executeUpdate() != 1) {
                           LOGGER.warning("Error while updating karma & pkkills for userId " + cw.getPlayerId());
                        }
                     }

                     removeFromDb(itemId);
                  }
               }

               ps.clearParameters();
            }
         }
      } catch (Exception var21) {
         LOGGER.log(Level.WARNING, "Could not check CursedWeapons data: " + var21.getMessage(), (Throwable)var21);
      }
   }

   public synchronized void checkDrop(Attackable attackable, Player player) {
      if (!(attackable instanceof Defender)
         && !(attackable instanceof RiftInvader)
         && !(attackable instanceof FestivalMonster)
         && !(attackable instanceof Guard)
         && !(attackable instanceof GrandBoss)
         && !(attackable instanceof FeedableBeast)
         && !(attackable instanceof FortCommander)) {
         for (CursedWeapon cw : this._cursedWeapons.values()) {
            if (!cw.isActive() && cw.checkDrop(attackable, player)) {
               break;
            }
         }
      }
   }

   public void activate(Player player, Item item) {
      CursedWeapon cw = this._cursedWeapons.get(item.getId());
      if (player.isCursedWeaponEquipped()) {
         CursedWeapon cw2 = this._cursedWeapons.get(player.getCursedWeaponEquippedId());
         cw2.setNbKills(cw2.getStageKills() - 1);
         cw2.increaseKills();
         cw.setPlayer(player);
         cw.endOfLife();
      } else {
         cw.activate(player, item);
      }
   }

   public void drop(int itemId, Creature killer) {
      this._cursedWeapons.get(itemId).dropIt(killer);
   }

   public void increaseKills(int itemId) {
      this._cursedWeapons.get(itemId).increaseKills();
   }

   public int getLevel(int itemId) {
      return this._cursedWeapons.get(itemId).getLevel();
   }

   public static void announce(SystemMessage sm) {
      World.broadcastToAllOnlinePlayers(sm);
   }

   public void checkPlayer(Player player) {
      if (player != null) {
         for (CursedWeapon cw : this._cursedWeapons.values()) {
            if (cw.isActivated() && player.getObjectId() == cw.getPlayerId()) {
               cw.setPlayer(player);
               cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
               cw.giveSkill();
               player.setCursedWeaponEquippedId(cw.getItemId());
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_S2_MINUTE_S_OF_USAGE_TIME_REMAINING);
               sm.addString(cw.getName());
               sm.addInt((int)((cw.getEndTime() - System.currentTimeMillis()) / 60000L));
               player.sendPacket(sm);
            }
         }
      }
   }

   public int checkOwnsWeaponId(int ownerId) {
      for (CursedWeapon cw : this._cursedWeapons.values()) {
         if (cw.isActivated() && ownerId == cw.getPlayerId()) {
            return cw.getItemId();
         }
      }

      return -1;
   }

   public static void removeFromDb(int itemId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
      ) {
         ps.setInt(1, itemId);
         ps.executeUpdate();
      } catch (SQLException var9) {
         LOGGER.log(Level.SEVERE, "CursedWeaponsManager: Failed to remove data: " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void saveData() {
      for (CursedWeapon cw : this._cursedWeapons.values()) {
         cw.saveData();
      }
   }

   public boolean isCursed(int itemId) {
      return this._cursedWeapons.containsKey(itemId);
   }

   public Collection<CursedWeapon> getCursedWeapons() {
      return this._cursedWeapons.values();
   }

   public Set<Integer> getCursedWeaponsIds() {
      return this._cursedWeapons.keySet();
   }

   public CursedWeapon getCursedWeapon(int itemId) {
      return this._cursedWeapons.get(itemId);
   }

   public void givePassive(int itemId) {
      try {
         this._cursedWeapons.get(itemId).giveSkill();
      } catch (Exception var3) {
      }
   }

   public static CursedWeaponsManager getInstance() {
      return CursedWeaponsManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CursedWeaponsManager INSTANCE = new CursedWeaponsManager();
   }
}
