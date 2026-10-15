package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.Summon;

public class SummonStat extends PlayableStat {
   public SummonStat(Summon activeChar) {
      super(activeChar);
   }

   public Summon getActiveChar() {
      return super.getActiveChar().asSummon();
   }
}
