package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class PlayerFreight extends ItemContainer {
   private final Player _owner;
   private final int _ownerId;

   public PlayerFreight(int objectId) {
      this._owner = null;
      this._ownerId = objectId;
      this.restore();
   }

   public PlayerFreight(Player owner) {
      this._owner = owner;
      this._ownerId = owner.getObjectId();
   }

   @Override
   public int getOwnerId() {
      return this._ownerId;
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   public ItemLocation getBaseLocation() {
      return ItemLocation.FREIGHT;
   }

   @Override
   public String getName() {
      return "Freight";
   }

   @Override
   public boolean validateCapacity(long slots) {
      return this.getSize() + slots
         <= (
            this._owner == null
               ? PlayerConfig.ALT_FREIGHT_SLOTS
               : PlayerConfig.ALT_FREIGHT_SLOTS + (int)this._owner.getStat().calcStat(Stat.FREIGHT_LIM, 0.0, null, null)
         );
   }

   @Override
   public void refreshWeight() {
   }
}
