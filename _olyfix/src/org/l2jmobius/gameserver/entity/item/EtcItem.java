package org.l2jmobius.gameserver.entity.item;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.util.StatSet;

public class EtcItem extends ItemTemplate {
   private String _handler;
   private EtcItemType _type;
   private List<ExtractableProduct> _extractableItems;
   private int _extractableCountMin;
   private int _extractableCountMax;
   private boolean _isBlessed;

   public EtcItem(StatSet set) {
      super(set);
   }

   @Override
   public void set(StatSet set) {
      super.set(set);
      this._type = set.getEnum("etcitem_type", EtcItemType.class, EtcItemType.NONE);
      switch (this.getDefaultAction()) {
         case SOULSHOT:
         case SUMMON_SOULSHOT:
         case SUMMON_SPIRITSHOT:
         case SPIRITSHOT:
            this._type = EtcItemType.SHOT;
      }

      this._type1 = 4;
      this._type2 = 5;
      if (this.isQuestItem()) {
         this._type2 = 3;
      } else if (this.getId() == 57 || this.getId() == 5575) {
         this._type2 = 4;
      }

      this._handler = set.getString("handler", null);
      this._extractableCountMin = set.getInt("extractableCountMin", 0);
      this._extractableCountMax = set.getInt("extractableCountMax", 0);
      if (this._extractableCountMin > this._extractableCountMax) {
         LOGGER.warning("Item " + this + " extractableCountMin is bigger than extractableCountMax!");
      }

      this._isBlessed = set.getBoolean("blessed", false)
         || (this.getDefaultAction() == ActionType.SPIRITSHOT || this.getDefaultAction() == ActionType.SOULSHOT)
            && this.getName() != null
            && this.getName().contains("Blessed");
   }

   public EtcItemType getItemType() {
      return this._type;
   }

   @Override
   public int getItemMask() {
      return this._type.mask();
   }

   @Override
   public boolean isEtcItem() {
      return true;
   }

   public String getHandlerName() {
      return this._handler;
   }

   public List<ExtractableProduct> getExtractableItems() {
      return this._extractableItems;
   }

   public int getExtractableCountMin() {
      return this._extractableCountMin;
   }

   public int getExtractableCountMax() {
      return this._extractableCountMax;
   }

   @Override
   public void addCapsuledItem(ExtractableProduct extractableProduct) {
      if (this._extractableItems == null) {
         this._extractableItems = new ArrayList<>();
      }

      this._extractableItems.add(extractableProduct);
   }

   public boolean isBlessed() {
      return this._isBlessed;
   }
}
