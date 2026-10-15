package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class FlyToLocation extends ServerPacket {
   private final int _destX;
   private final int _destY;
   private final int _destZ;
   private final int _chaObjId;
   private final int _orgX;
   private final int _orgY;
   private final int _orgZ;
   private final FlyType _type;

   public FlyToLocation(Creature creature, int destX, int destY, int destZ, FlyType type) {
      this._chaObjId = creature.getObjectId();
      this._orgX = creature.getX();
      this._orgY = creature.getY();
      this._orgZ = creature.getZ();
      this._destX = destX;
      this._destY = destY;
      this._destZ = destZ;
      this._type = type;
      if (creature.isPlayer()) {
         creature.asPlayer().setBlinkActive(true);
      }
   }

   public FlyToLocation(Creature creature, ILocational dest, FlyType type) {
      this(creature, dest.getX(), dest.getY(), dest.getZ(), type);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.FLY_TO_LOCATION.writeId(this, buffer);
      buffer.writeInt(this._chaObjId);
      buffer.writeInt(this._destX);
      buffer.writeInt(this._destY);
      buffer.writeInt(this._destZ);
      buffer.writeInt(this._orgX);
      buffer.writeInt(this._orgY);
      buffer.writeInt(this._orgZ);
      buffer.writeInt(this._type.ordinal());
   }
}
