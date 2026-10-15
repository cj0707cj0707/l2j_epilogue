package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class DoorRequestHolder {
   private final Door _target;

   public DoorRequestHolder(Door door) {
      this._target = door;
   }

   public Door getDoor() {
      return this._target;
   }
}
