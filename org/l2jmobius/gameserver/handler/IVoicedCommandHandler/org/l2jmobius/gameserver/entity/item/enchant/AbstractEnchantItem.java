package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.util.StatSet;

public abstract class AbstractEnchantItem {
   protected static final Logger LOGGER = Logger.getLogger(AbstractEnchantItem.class.getName());
   private static final Set<EtcItemType> ENCHANT_TYPES = EnumSet.noneOf(EtcItemType.class);
   private final int _id;
   private final CrystalType _grade;
   private final int _maxEnchantLevel;
   private final double _bonusRate;

   public AbstractEnchantItem(StatSet set) {
      this._id = set.getInt("id");
      if (this.getItem() == null) {
         throw new NullPointerException();
      } else if (!ENCHANT_TYPES.contains(this.getItem().getItemType())) {
         throw new IllegalAccessError();
      } else {
         this._grade = set.getEnum("targetGrade", CrystalType.class, CrystalType.NONE);
         this._maxEnchantLevel = set.getInt("maxEnchant", 65535);
         this._bonusRate = set.getDouble("bonusRate", 0.0);
      }
   }

   public int getId() {
      return this._id;
   }

   public double getBonusRate() {
      return this._bonusRate;
   }

   public ItemTemplate getItem() {
      return ItemData.getInstance().getTemplate(this._id);
   }

   public CrystalType getGrade() {
      return this._grade;
   }

   public abstract boolean isWeapon();

   public int getMaxEnchantLevel() {
      return this._maxEnchantLevel;
   }

   public boolean isValid(Item itemToEnchant, EnchantSupportItem supportItem) {
      if (itemToEnchant == null) {
         return false;
      } else if (!itemToEnchant.isEnchantable()) {
         return false;
      } else if (!this.isValidItemType(itemToEnchant.getTemplate().getType2())) {
         return false;
      } else {
         return this._maxEnchantLevel != 0 && itemToEnchant.getEnchantLevel() >= this._maxEnchantLevel
            ? false
            : this._grade == itemToEnchant.getTemplate().getCrystalTypePlus();
      }
   }

   private boolean isValidItemType(int type2) {
      if (type2 == 0) {
         return this.isWeapon();
      } else {
         return type2 != 1 && type2 != 2 ? false : !this.isWeapon();
      }
   }

   static {
      ENCHANT_TYPES.add(EtcItemType.ANCIENT_CRYSTAL_ENCHANT_AM);
      ENCHANT_TYPES.add(EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP);
      ENCHANT_TYPES.add(EtcItemType.BLESS_SCRL_ENCHANT_AM);
      ENCHANT_TYPES.add(EtcItemType.BLESS_SCRL_ENCHANT_WP);
      ENCHANT_TYPES.add(EtcItemType.SCRL_ENCHANT_AM);
      ENCHANT_TYPES.add(EtcItemType.SCRL_ENCHANT_WP);
      ENCHANT_TYPES.add(EtcItemType.SCRL_INC_ENCHANT_PROP_AM);
      ENCHANT_TYPES.add(EtcItemType.SCRL_INC_ENCHANT_PROP_WP);
   }
}
