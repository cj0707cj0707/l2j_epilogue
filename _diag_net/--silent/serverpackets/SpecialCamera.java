package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class SpecialCamera extends ServerPacket {
   private final int _id;
   private final int _force;
   private final int _angle1;
   private final int _angle2;
   private final int _time;
   private final int _duration;
   private final int _relYaw;
   private final int _relPitch;
   private final int _isWide;
   private final int _relAngle;
   private final int _unk;

   public SpecialCamera(
      Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle
   ) {
      this(creature, force, angle1, angle2, time, duration, range, relYaw, relPitch, isWide, relAngle, 0);
   }

   public SpecialCamera(
      Creature creature, Creature talker, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle
   ) {
      this(creature, force, angle1, angle2, time, duration, 0, relYaw, relPitch, isWide, relAngle, 0);
   }

   public SpecialCamera(
      Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk
   ) {
      this._id = creature.getObjectId();
      this._force = force;
      this._angle1 = angle1;
      this._angle2 = angle2;
      this._time = time;
      this._duration = duration;
      this._relYaw = relYaw;
      this._relPitch = relPitch;
      this._isWide = isWide;
      this._relAngle = relAngle;
      this._unk = unk;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SPECIAL_CAMERA.writeId(this, buffer);
      buffer.writeInt(this._id);
      buffer.writeInt(this._force);
      buffer.writeInt(this._angle1);
      buffer.writeInt(this._angle2);
      buffer.writeInt(this._time);
      buffer.writeInt(this._duration);
      buffer.writeInt(this._relYaw);
      buffer.writeInt(this._relPitch);
      buffer.writeInt(this._isWide);
      buffer.writeInt(this._relAngle);
      buffer.writeInt(this._unk);
   }
}
