package org.l2jmobius.gameserver.mechanics.krateisCube;

import org.l2jmobius.gameserver.entity.actor.Player;

public class KrateiCubePlayer {
   private final Player _player;
   private boolean _isInside = false;
   private boolean _isRegister = false;
   private int _points = 0;

   public KrateiCubePlayer(Player player) {
      this._player = player;
   }

   public Player getPlayer() {
      return this._player;
   }

   public void setIsRegister(boolean value) {
      this._isRegister = value;
   }

   public boolean isRegister() {
      return this._isRegister;
   }

   public void setIsInside(boolean value) {
      this._isInside = value;
   }

   public boolean isInside() {
      return this._isInside;
   }

   public void addPoints(int points) {
      this._points = points;
   }

   public int getPoints() {
      return this._points;
   }
}
