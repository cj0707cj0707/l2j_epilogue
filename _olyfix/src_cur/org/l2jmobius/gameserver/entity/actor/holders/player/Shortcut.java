package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;

public class Shortcut {
   private final int _slot;
   private final int _page;
   private final ShortcutType _type;
   private final int _id;
   private final int _level;
   private final int _characterType;
   private int _sharedReuseGroup = -1;

   public Shortcut(int slot, int page, ShortcutType type, int id, int level, int characterType) {
      this._slot = slot;
      this._page = page;
      this._type = type;
      this._id = id;
      this._level = level;
      this._characterType = characterType;
   }

   public int getId() {
      return this._id;
   }

   public int getLevel() {
      return this._level;
   }

   public int getPage() {
      return this._page;
   }

   public int getSlot() {
      return this._slot;
   }

   public ShortcutType getType() {
      return this._type;
   }

   public int getCharacterType() {
      return this._characterType;
   }

   public int getSharedReuseGroup() {
      return this._sharedReuseGroup;
   }

   public void setSharedReuseGroup(int sharedReuseGroup) {
      this._sharedReuseGroup = sharedReuseGroup;
   }
}
