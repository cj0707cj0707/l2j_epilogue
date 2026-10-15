package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Couple;

public class CoupleManager {
   private static final Logger LOGGER = Logger.getLogger(CoupleManager.class.getName());
   private final List<Couple> _couples = new CopyOnWriteArrayList<>();

   protected CoupleManager() {
      this.load();
   }

   public void reload() {
      this._couples.clear();
      this.load();
   }

   private void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement ps = con.createStatement();
         ResultSet rs = ps.executeQuery("SELECT id FROM mods_wedding ORDER BY id");
      ) {
         while (rs.next()) {
            this._couples.add(new Couple(rs.getInt("id")));
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._couples.size() + " couples(s)");
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Exception: CoupleManager.load(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public Couple getCouple(int coupleId) {
      int index = this.getCoupleIndex(coupleId);
      return index >= 0 ? this._couples.get(index) : null;
   }

   public void createCouple(Player player1, Player player2) {
      if (player1 != null && player2 != null && player1.getPartnerId() == 0 && player2.getPartnerId() == 0) {
         int player1id = player1.getObjectId();
         int player2id = player2.getObjectId();
         Couple couple = new Couple(player1, player2);
         this._couples.add(couple);
         player1.setPartnerId(player2id);
         player2.setPartnerId(player1id);
         player1.setCoupleId(couple.getId());
         player2.setCoupleId(couple.getId());
      }
   }

   public void deleteCouple(int coupleId) {
      int index = this.getCoupleIndex(coupleId);
      Couple couple = this._couples.get(index);
      if (couple != null) {
         Player player1 = World.getPlayer(couple.getPlayer1Id());
         Player player2 = World.getPlayer(couple.getPlayer2Id());
         if (player1 != null) {
            player1.setPartnerId(0);
            player1.setMarried(false);
            player1.setCoupleId(0);
         }

         if (player2 != null) {
            player2.setPartnerId(0);
            player2.setMarried(false);
            player2.setCoupleId(0);
         }

         couple.divorce();
         this._couples.remove(index);
      }
   }

   public int getCoupleIndex(int coupleId) {
      int i = 0;

      for (Couple temp : this._couples) {
         if (temp != null && temp.getId() == coupleId) {
            return i;
         }

         i++;
      }

      return -1;
   }

   public List<Couple> getCouples() {
      return this._couples;
   }

   public static CoupleManager getInstance() {
      return CoupleManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CoupleManager INSTANCE = new CoupleManager();
   }
}
