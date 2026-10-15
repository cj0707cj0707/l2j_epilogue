package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class TownZone extends ZoneType {
   private int _townId;
   private int _taxById = 0;

   public TownZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("townId")) {
         this._townId = Integer.parseInt(value);
      } else if (name.equals("taxById")) {
         this._taxById = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      creature.setInsideZone(ZoneId.TOWN, true);
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.TOWN, false);
   }

   public int getTownId() {
      return this._townId;
   }

   public int getTaxById() {
      return this._taxById;
   }
}
