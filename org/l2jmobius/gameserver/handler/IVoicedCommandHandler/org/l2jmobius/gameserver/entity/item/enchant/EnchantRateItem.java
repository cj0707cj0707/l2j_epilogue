package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;

public class EnchantRateItem {
   private final String _name;
   private final Set<Integer> _items = new HashSet<>();
   private int _slot;
   private Boolean _isMagicWeapon = null;

   public EnchantRateItem(String name) {
      this._name = name;
   }

   public String getName() {
      return this._name;
   }

   public void addItemId(int id) {
      this._items.add(id);
   }

   public void addSlot(int slot) {
      this._slot |= slot;
   }

   public void setMagicWeapon(boolean magicWeapon) {
      this._isMagicWeapon = magicWeapon;
   }

   public boolean validate(ItemTemplate item) {
      if (!this._items.isEmpty() && !this._items.contains(item.getId())) {
         return false;
      } else {
         return this._slot != 0 && (item.getBodyPart().getMask() & this._slot) == 0
            ? false
            : this._isMagicWeapon == null || item.isMagicWeapon() == this._isMagicWeapon;
      }
   }
}
