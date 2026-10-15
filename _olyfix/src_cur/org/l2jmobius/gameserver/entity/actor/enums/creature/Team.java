package org.l2jmobius.gameserver.entity.actor.enums.creature;

public enum Team {
   NONE(0),
   BLUE(1),
   RED(2);

   private int _id;

   private Team(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }
}
