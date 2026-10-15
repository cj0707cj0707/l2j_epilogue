package org.l2jmobius.gameserver.data.holders;

public class SellBuffHolder {
   private final int _skillId;
   private long _price;

   public SellBuffHolder(int skillId, long price) {
      this._skillId = skillId;
      this._price = price;
   }

   public int getSkillId() {
      return this._skillId;
   }

   public void setPrice(int price) {
      this._price = price;
   }

   public long getPrice() {
      return this._price;
   }
}
