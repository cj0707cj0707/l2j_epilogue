package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

public class FestivalMonster extends Monster {
   protected int _bonusMultiplier = 1;

   public FestivalMonster(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FestivalMonster);
   }

   public void setOfferingBonus(int bonusMultiplier) {
      this._bonusMultiplier = bonusMultiplier;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return !(attacker instanceof FestivalMonster);
   }

   @Override
   public boolean isAggressive() {
      return true;
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }

   @Override
   public void doItemDrop(Creature lastAttacker) {
      Player killingChar = null;
      if (lastAttacker.isPlayer()) {
         killingChar = lastAttacker.asPlayer();
         Party associatedParty = killingChar.getParty();
         if (associatedParty != null) {
            Player partyLeader = associatedParty.getLeader();
            Item addedOfferings = partyLeader.getInventory().addItem(ItemProcessType.REWARD, 5901, this._bonusMultiplier, partyLeader, this);
            InventoryUpdate iu = new InventoryUpdate();
            if (addedOfferings.getCount() != this._bonusMultiplier) {
               iu.addModifiedItem(addedOfferings);
            } else {
               iu.addNewItem(addedOfferings);
            }

            partyLeader.sendInventoryUpdate(iu);
            super.doItemDrop(lastAttacker);
         }
      }
   }
}
