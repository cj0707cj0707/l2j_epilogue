package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.instance.ClanHallManager;
import org.l2jmobius.gameserver.entity.actor.instance.Folk;

public class FolkStatus extends NpcStatus {
   public FolkStatus(Npc activeChar) {
      super(activeChar);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   @Override
   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption) {
   }

   @Override
   public void reduceMp(double value) {
      if (!FeatureConfig.CH_BUFF_FREE || !(this.getActiveChar() instanceof ClanHallManager)) {
         super.reduceMp(value);
      }
   }

   public Folk getActiveChar() {
      return (Folk)super.getActiveChar();
   }
}
