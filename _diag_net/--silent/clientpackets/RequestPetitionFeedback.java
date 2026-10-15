package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;

public class RequestPetitionFeedback extends ClientPacket {
   private static final String INSERT_FEEDBACK = "INSERT INTO petition_feedback VALUES (?,?,?,?,?)";
   private int _rate;
   private String _message;

   @Override
   protected void readImpl() {
      this.readInt();
      this._rate = this.readInt();
      this._message = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && player.getLastPetitionGmName() != null) {
         if (this._rate <= 4 && this._rate >= 0) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("INSERT INTO petition_feedback VALUES (?,?,?,?,?)");
            ) {
               statement.setString(1, player.getName());
               statement.setString(2, player.getLastPetitionGmName());
               statement.setInt(3, this._rate);
               statement.setString(4, this._message);
               statement.setLong(5, System.currentTimeMillis());
               statement.execute();
            } catch (SQLException var10) {
               org.l2jmobius.gameserver.network.PacketLogger.warning(
                  this.getClass().getSimpleName() + ": Error while saving petition feedback: " + var10.getMessage()
               );
            }
         }
      }
   }
}
