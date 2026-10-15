package org.l2jmobius.gameserver.entity.actor.holders.npc;

public class AbsorberInfo {
   private int _objectId;
   private double _absorbedHp;

   public AbsorberInfo(int objectId, double pAbsorbedHp) {
      this._objectId = objectId;
      this._absorbedHp = pAbsorbedHp;
   }

   public double getAbsorbedHp() {
      return this._absorbedHp;
   }

   public void setAbsorbedHp(double absorbedHp) {
      this._absorbedHp = absorbedHp;
   }

   public int getObjectId() {
      return this._objectId;
   }

   public void setObjectId(int objectId) {
      this._objectId = objectId;
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj || obj instanceof AbsorberInfo && ((AbsorberInfo)obj).getObjectId() == this._objectId;
   }

   @Override
   public int hashCode() {
      return this._objectId;
   }
}
