package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;

public class StaticObjectStat extends CreatureStat {
   public StaticObjectStat(StaticObject activeChar) {
      super(activeChar);
   }

   public StaticObject getActiveChar() {
      return (StaticObject)super.getActiveChar();
   }

   @Override
   public byte getLevel() {
      return (byte)this.getActiveChar().getLevel();
   }
}
