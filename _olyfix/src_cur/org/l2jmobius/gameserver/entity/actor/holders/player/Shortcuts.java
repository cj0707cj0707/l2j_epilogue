package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;

public class Shortcuts {
   private static final Logger LOGGER = Logger.getLogger(Shortcuts.class.getName());
   private static final int MAX_SHORTCUTS_PER_BAR = 12;
   private final Player _owner;
   private final Map<Integer, Shortcut> _shortcuts = new ConcurrentHashMap<>();

   public Shortcuts(Player owner) {
      this._owner = owner;
   }

   public Collection<Shortcut> getAllShortcuts() {
      return this._shortcuts.values();
   }

   public Shortcut getShortcut(int slot, int page) {
      Shortcut sc = this._shortcuts.get(slot + page * 12);
      if (sc != null && sc.getType() == ShortcutType.ITEM && this._owner.getInventory().getItemByObjectId(sc.getId()) == null) {
         this.deleteShortcut(sc.getSlot(), sc.getPage());
         sc = null;
      }

      return sc;
   }

   public void registerShortcut(Shortcut shortcut) {
      if (shortcut.getType() == ShortcutType.ITEM) {
         Item item = this._owner.getInventory().getItemByObjectId(shortcut.getId());
         if (item == null) {
            return;
         }

         shortcut.setSharedReuseGroup(item.getSharedReuseGroup());
      }

      this.registerShortcutInDb(shortcut, this._shortcuts.put(shortcut.getSlot() + shortcut.getPage() * 12, shortcut));
   }

   private void registerShortcutInDb(Shortcut shortcut, Shortcut oldShortcut) {
      if (oldShortcut != null) {
         this.deleteShortcutFromDb(oldShortcut);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "REPLACE INTO character_shortcuts (charId,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)"
         );
      ) {
         statement.setInt(1, this._owner.getObjectId());
         statement.setInt(2, shortcut.getSlot());
         statement.setInt(3, shortcut.getPage());
         statement.setInt(4, shortcut.getType().ordinal());
         statement.setInt(5, shortcut.getId());
         statement.setInt(6, shortcut.getLevel());
         statement.setInt(7, this._owner.getClassIndex());
         statement.execute();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Could not store character shortcut: " + var11.getMessage(), (Throwable)var11);
      }
   }

   public void deleteShortcut(int slot, int page) {
      Shortcut old = this._shortcuts.remove(slot + page * 12);
      if (old != null && this._owner != null) {
         this.deleteShortcutFromDb(old);
         if (old.getType() == ShortcutType.ITEM) {
            Item item = this._owner.getInventory().getItemByObjectId(old.getId());
            if (item != null && item.getItemType() == EtcItemType.SHOT && this._owner.removeAutoSoulShot(item.getId())) {
               this._owner.sendPacket(new ExAutoSoulShot(item.getId(), 0));
            }
         }

         this._owner.sendPacket(new ShortcutInit(this._owner));

         for (int shotId : this._owner.getAutoSoulShot()) {
            this._owner.sendPacket(new ExAutoSoulShot(shotId, 1));
         }
      }
   }

   public void deleteShortcutByObjectId(int objectId) {
      for (Shortcut shortcut : this._shortcuts.values()) {
         if (shortcut.getType() == ShortcutType.ITEM && shortcut.getId() == objectId) {
            this.deleteShortcut(shortcut.getSlot(), shortcut.getPage());
            break;
         }
      }
   }

   private void deleteShortcutFromDb(Shortcut shortcut) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND slot=? AND page=? AND class_index=?");
      ) {
         statement.setInt(1, this._owner.getObjectId());
         statement.setInt(2, shortcut.getSlot());
         statement.setInt(3, shortcut.getPage());
         statement.setInt(4, this._owner.getClassIndex());
         statement.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, "Could not delete character shortcut: " + var10.getMessage(), (Throwable)var10);
      }
   }

   public boolean restoreMe() {
      this._shortcuts.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "SELECT charId, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE charId=? AND class_index=?"
         );
      ) {
         statement.setInt(1, this._owner.getObjectId());
         statement.setInt(2, this._owner.getClassIndex());

         try (ResultSet rset = statement.executeQuery()) {
            while (rset.next()) {
               int slot = rset.getInt("slot");
               int page = rset.getInt("page");
               this._shortcuts
                  .put(
                     slot + page * 12,
                     new Shortcut(slot, page, ShortcutType.values()[rset.getInt("type")], rset.getInt("shortcut_id"), rset.getInt("level"), 1)
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore character shortcuts: " + var12.getMessage(), (Throwable)var12);
         return false;
      }

      for (Shortcut sc : this.getAllShortcuts()) {
         if (sc.getType() == ShortcutType.ITEM) {
            Item item = this._owner.getInventory().getItemByObjectId(sc.getId());
            if (item == null) {
               this.deleteShortcut(sc.getSlot(), sc.getPage());
            } else if (item.isEtcItem()) {
               sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
            }
         }
      }

      return true;
   }

   public void updateShortcuts(int skillId, int skillLevel) {
      for (Shortcut sc : this._shortcuts.values()) {
         if (sc.getId() == skillId && sc.getType() == ShortcutType.SKILL) {
            Shortcut newsc = new Shortcut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), skillLevel, 1);
            this._owner.sendPacket(new ShortcutRegister(newsc));
            this._owner.registerShortcut(newsc);
         }
      }
   }
}
