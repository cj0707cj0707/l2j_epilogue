package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class DoorStatus extends CreatureStatus {
   public DoorStatus(Door activeChar) {
      super(activeChar);
   }

   public Door getActiveChar() {
      return super.getActiveChar().asDoor();
   }
}
