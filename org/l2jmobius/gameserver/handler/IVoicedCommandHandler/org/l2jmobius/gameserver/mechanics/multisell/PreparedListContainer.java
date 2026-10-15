package org.l2jmobius.gameserver.mechanics.multisell;

import java.util.ArrayList;
import java.util.Collection;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class PreparedListContainer extends ListContainer {
   private int _npcObjectId = 0;

   public PreparedListContainer(ListContainer template, boolean inventoryOnly, Player player, Npc npc) {
      super(template.getListId());
      this.setMaintainEnchantment(template.getMaintainEnchantment());
      this.setApplyTaxes(false);
      double taxRate = 0.0;
      if (npc != null) {
         this._npcObjectId = npc.getObjectId();
         if (template.getApplyTaxes() && npc.isInTown() && npc.getCastle().getOwnerId() > 0) {
            this.setApplyTaxes(true);
            taxRate = npc.getCastle().getTaxRate();
         }
      }

      if (inventoryOnly) {
         if (player == null) {
            return;
         }

         Collection<Item> items;
         if (this.getMaintainEnchantment()) {
            items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
         } else {
            items = player.getInventory().getUniqueItems(false, false, false, true);
         }

         this._entries = new ArrayList<>();

         for (Entry entry : template.getEntries()) {
            if (!entry.getIngredients().isEmpty()) {
               int ingredientId = entry.getIngredients().get(0).getItemId();

               for (Item item : items) {
                  if (!item.isEquipped() && item.getId() == ingredientId) {
                     this._entries.add(new PreparedEntry(entry, item, this.getApplyTaxes(), this.getMaintainEnchantment(), taxRate));
                     break;
                  }
               }
            }
         }
      } else {
         this._entries = new ArrayList<>(template.getEntries().size());

         for (Entry ent : template.getEntries()) {
            this._entries.add(new PreparedEntry(ent, null, this.getApplyTaxes(), false, taxRate));
         }
      }

      this._npcsAllowed = template._npcsAllowed;
   }

   public boolean checkNpcObjectId(int npcObjectId) {
      return this._npcObjectId == 0 || this._npcObjectId == npcObjectId;
   }
}
