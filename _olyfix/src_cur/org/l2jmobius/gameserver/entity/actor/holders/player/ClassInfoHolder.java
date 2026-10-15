package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

public class ClassInfoHolder {
   private final PlayerClass _playerClass;
   private final PlayerClass _parentClass;
   private final String _className;

   public ClassInfoHolder(PlayerClass playerClass, PlayerClass parentClass, String className) {
      this._playerClass = playerClass;
      this._parentClass = parentClass;
      this._className = className;
   }

   public PlayerClass getPlayerClass() {
      return this._playerClass;
   }

   public PlayerClass getParentClass() {
      return this._parentClass;
   }

   public String getClassName() {
      return this._className;
   }
}
