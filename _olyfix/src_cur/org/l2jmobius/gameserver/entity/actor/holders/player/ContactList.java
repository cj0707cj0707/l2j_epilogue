package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ContactList {
   private static final Logger LOGGER = Logger.getLogger(ContactList.class.getName());
   private final Player _player;
   private final Set<String> _contacts = ConcurrentHashMap.newKeySet();
   private static final String QUERY_ADD = "REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)";
   private static final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
   private static final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";

   public ContactList(Player player) {
      this._player = player;
      this.restore();
   }

   public void restore() {
      this._contacts.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT contactId FROM character_contacts WHERE charId = ?");
      ) {
         statement.setInt(1, this._player.getObjectId());

         try (ResultSet rset = statement.executeQuery()) {
            while (rset.next()) {
               int contactId = rset.getInt(1);
               String contactName = CharInfoTable.getInstance().getNameById(contactId);
               if (contactName != null && !contactName.equals(this._player.getName()) && contactId != this._player.getObjectId()) {
                  this._contacts.add(contactName);
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Error found in " + this._player.getName() + "'s ContactsList: " + var12.getMessage(), (Throwable)var12);
      }
   }

   public boolean add(String name) {
      if (this._contacts.contains(name)) {
         this._player.sendMessage("The name already exists on the added list.");
         return false;
      } else if (this._player.getName().equals(name)) {
         this._player.sendMessage("You cannot add your own name.");
         return false;
      } else if (this._contacts.size() >= 100) {
         this._player.sendMessage("The maximum number of names (100) has been reached. You cannot register any more.");
         return false;
      } else {
         int contactId = CharInfoTable.getInstance().getIdByName(name);
         if (contactId < 1) {
            this._player.sendMessage("The name " + name + " doesn't exist. Please try another name.");
            return false;
         } else {
            for (String contactName : this._contacts) {
               if (contactName.equalsIgnoreCase(name)) {
                  this._player.sendMessage("The name already exists on the added list.");
                  return false;
               }
            }

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)");
            ) {
               statement.setInt(1, this._player.getObjectId());
               statement.setInt(2, contactId);
               statement.execute();
               this._contacts.add(name);
               this._player.sendMessage(name + " was successfully added to your Contact List.");
            } catch (Exception var11) {
               LOGGER.log(Level.WARNING, "Error found in " + this._player.getName() + "'s ContactsList: " + var11.getMessage(), (Throwable)var11);
            }

            return true;
         }
      }
   }

   public void remove(String name) {
      if (!this._contacts.contains(name)) {
         this._player.sendMessage("The name is not currently registered.");
      } else {
         int contactId = CharInfoTable.getInstance().getIdByName(name);
         if (contactId >= 1) {
            this._contacts.remove(name);

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("DELETE FROM character_contacts WHERE charId = ? and contactId = ?");
            ) {
               statement.setInt(1, this._player.getObjectId());
               statement.setInt(2, contactId);
               statement.execute();
               this._player.sendMessage(name + " was successfully deleted from your Contact List.");
            } catch (Exception var11) {
               LOGGER.log(Level.WARNING, "Error found in " + this._player.getName() + "'s ContactsList: " + var11.getMessage(), (Throwable)var11);
            }
         }
      }
   }

   public Set<String> getAllContacts() {
      return this._contacts;
   }
}
