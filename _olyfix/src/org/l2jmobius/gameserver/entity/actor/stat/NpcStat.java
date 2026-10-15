package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.Npc;

public class NpcStat extends CreatureStat {
   public NpcStat(Npc activeChar) {
      super(activeChar);
   }

   @Override
   public byte getLevel() {
      return this.getActiveChar().getTemplate().getLevel();
   }

   public Npc getActiveChar() {
      return super.getActiveChar().asNpc();
   }

   @Override
   public int getPhysicalAttackAngle() {
      return this.getActiveChar().getTemplate().getBaseAttackAngle();
   }
}
