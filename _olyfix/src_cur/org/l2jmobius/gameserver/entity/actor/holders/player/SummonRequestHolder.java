package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

public class SummonRequestHolder {
   private final Player _summoner;
   private final Location _location;

   public SummonRequestHolder(Player summoner) {
      this._summoner = summoner;
      this._location = summoner == null ? null : new Location(summoner.getX(), summoner.getY(), summoner.getZ(), summoner.getHeading());
   }

   public Player getSummoner() {
      return this._summoner;
   }

   public Location getLocation() {
      return this._location;
   }
}
