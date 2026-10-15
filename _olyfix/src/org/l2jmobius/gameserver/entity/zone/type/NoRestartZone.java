package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class NoRestartZone extends ZoneType {
   private int _restartAllowedTime = 0;
   private int _restartTime = 0;

   public NoRestartZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equalsIgnoreCase("restartAllowedTime")) {
         this._restartAllowedTime = Integer.parseInt(value) * 1000;
      } else if (name.equalsIgnoreCase("restartTime")) {
         this._restartTime = Integer.parseInt(value) * 1000;
      } else if (!name.equalsIgnoreCase("instanceId")) {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.NO_RESTART, true);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.NO_RESTART, false);
      }
   }

   @Override
   public void onPlayerLoginInside(Player player) {
      if (this.isEnabled()) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - player.getLastAccess() > this._restartTime && currentTime - GameServer.getStartTime() > this._restartAllowedTime) {
            player.teleToLocation(TeleportWhereType.TOWN);
         }
      }
   }

   public int getRestartAllowedTime() {
      return this._restartAllowedTime;
   }

   public void setRestartAllowedTime(int time) {
      this._restartAllowedTime = time;
   }

   public int getRestartTime() {
      return this._restartTime;
   }

   public void setRestartTime(int time) {
      this._restartTime = time;
   }
}
