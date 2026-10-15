package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class DoorStat extends CreatureStat {
   private int _upgradeHpRatio = 1;

   public DoorStat(Door activeChar) {
      super(activeChar);
   }

   public Door getActiveChar() {
      return super.getActiveChar().asDoor();
   }

   @Override
   public int getMaxHp() {
      return super.getMaxHp() * this._upgradeHpRatio;
   }

   public void setUpgradeHpRatio(int ratio) {
      this._upgradeHpRatio = ratio;
   }

   public int getUpgradeHpRatio() {
      return this._upgradeHpRatio;
   }
}
