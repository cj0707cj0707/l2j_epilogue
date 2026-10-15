package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;

public class StaticObjectStatus extends CreatureStatus {
   public StaticObjectStatus(StaticObject activeChar) {
      super(activeChar);
   }

   public StaticObject getActiveChar() {
      return (StaticObject)super.getActiveChar();
   }
}
