package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.PetDataTable;

public class PetNameTable {
   private static final Logger LOGGER = Logger.getLogger(PetNameTable.class.getName());
   private static final String CHECK_PET_NAME = "SELECT p.name FROM pets p JOIN items i ON p.item_obj_id = i.object_id WHERE p.name=? AND i.item_id=?";

   public boolean doesPetNameExist(String name, int petNpcId) {
      boolean result = false;
      int petItemId = PetDataTable.getInstance().getPetItemsByNpc(petNpcId);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT p.name FROM pets p JOIN items i ON p.item_obj_id = i.object_id WHERE p.name=? AND i.item_id=?");
      ) {
         ps.setString(1, name);
         ps.setInt(2, petItemId);

         try (ResultSet rs = ps.executeQuery()) {
            result = rs.next();
         }
      } catch (SQLException var16) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not check existing pet name: " + var16.getMessage(), (Throwable)var16);
      }

      return result;
   }

   public boolean isValidPetName(String name) {
      return !StringUtil.isAlphaNumeric(name) ? false : ServerConfig.PET_NAME_TEMPLATE_PATTERN.matcher(name).matches();
   }

   public static PetNameTable getInstance() {
      return PetNameTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PetNameTable INSTANCE = new PetNameTable();
   }
}
