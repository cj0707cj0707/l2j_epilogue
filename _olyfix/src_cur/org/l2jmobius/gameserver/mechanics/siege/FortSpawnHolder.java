package org.l2jmobius.gameserver.mechanics.siege;

import org.l2jmobius.gameserver.entity.Location;

public class FortSpawnHolder extends Location {
   private final int _npcId;
   private final int _fortId;
   private final int _id;

   public FortSpawnHolder(int fortId, int x, int y, int z, int heading, int npcId, int id) {
      super(x, y, z, heading);
      this._fortId = fortId;
      this._npcId = npcId;
      this._id = id;
   }

   public int getFortId() {
      return this._fortId;
   }

   public int getId() {
      return this._npcId;
   }

   public int getMessageId() {
      return this._id;
   }
}
