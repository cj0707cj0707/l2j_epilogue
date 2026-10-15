package org.l2jmobius.gameserver.entity.item.enchant;

import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.util.StatSet;

public class EnchantSupportItem extends AbstractEnchantItem {
   private final boolean _isWeapon = this.getItem().getItemType() == EtcItemType.SCRL_INC_ENCHANT_PROP_WP;

   public EnchantSupportItem(StatSet set) {
      super(set);
   }

   @Override
   public boolean isWeapon() {
      return this._isWeapon;
   }
}
