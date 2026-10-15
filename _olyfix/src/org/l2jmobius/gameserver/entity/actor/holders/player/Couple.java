package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.IdManager;

public class Couple {
   private static final Logger LOGGER = Logger.getLogger(Couple.class.getName());
   private int _id = 0;
   private int _player1Id = 0;
   private int _player2Id = 0;
   private boolean _maried = false;
   private Calendar _affiancedDate;
   private Calendar _weddingDate;

   public Couple(int coupleId) {
      this._id = coupleId;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM mods_wedding WHERE id = ?");
      ) {
         ps.setInt(1, this._id);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._player1Id = rs.getInt("player1Id");
               this._player2Id = rs.getInt("player2Id");
               this._maried = Boolean.parseBoolean(rs.getString("married"));
               this._affiancedDate = Calendar.getInstance();
               this._affiancedDate.setTimeInMillis(rs.getLong("affianceDate"));
               this._weddingDate = Calendar.getInstance();
               this._weddingDate.setTimeInMillis(rs.getLong("weddingDate"));
            }
         }
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "Exception: Couple.load(): " + var13.getMessage(), (Throwable)var13);
      }
   }

   public Couple(Player player1, Player player2) {
      long currentTime = System.currentTimeMillis();
      this._player1Id = player1.getObjectId();
      this._player2Id = player2.getObjectId();
      this._affiancedDate = Calendar.getInstance();
      this._affiancedDate.setTimeInMillis(currentTime);
      this._weddingDate = Calendar.getInstance();
      this._weddingDate.setTimeInMillis(currentTime);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO mods_wedding (id, player1Id, player2Id, married, affianceDate, weddingDate) VALUES (?, ?, ?, ?, ?, ?)"
         );
      ) {
         this._id = IdManager.getInstance().getNextId();
         ps.setInt(1, this._id);
         ps.setInt(2, this._player1Id);
         ps.setInt(3, this._player2Id);
         ps.setBoolean(4, false);
         ps.setLong(5, this._affiancedDate.getTimeInMillis());
         ps.setLong(6, this._weddingDate.getTimeInMillis());
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, "Could not create couple: " + var13.getMessage(), (Throwable)var13);
      }
   }

   public void marry() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE mods_wedding set married = ?, weddingDate = ? where id = ?");
      ) {
         ps.setBoolean(1, true);
         this._weddingDate = Calendar.getInstance();
         ps.setLong(2, this._weddingDate.getTimeInMillis());
         ps.setInt(3, this._id);
         ps.execute();
         this._maried = true;
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Could not marry: " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void divorce() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE id=?");
      ) {
         ps.setInt(1, this._id);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Exception: Couple.divorce(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public int getId() {
      return this._id;
   }

   public int getPlayer1Id() {
      return this._player1Id;
   }

   public int getPlayer2Id() {
      return this._player2Id;
   }

   public boolean getMaried() {
      return this._maried;
   }

   public Calendar getAffiancedDate() {
      return this._affiancedDate;
   }

   public Calendar getWeddingDate() {
      return this._weddingDate;
   }
}
