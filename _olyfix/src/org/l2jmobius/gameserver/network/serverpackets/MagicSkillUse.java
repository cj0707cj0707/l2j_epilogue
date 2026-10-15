package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MagicSkillUse extends ServerPacket {
   private final int _skillId;
   private final int _skillLevel;
   private final int _hitTime;
   private final int _reuseDelay;
   private final Creature _creature;
   private final Creature _target;
   private final Location _groundLocation;

   public MagicSkillUse(Creature creature, Creature target, int skillId, int skillLevel, int hitTime, int reuseDelay) {
      this._creature = creature;
      this._target = target;
      this._skillId = skillId;
      this._skillLevel = skillLevel;
      this._hitTime = hitTime;
      this._reuseDelay = reuseDelay;
      this._groundLocation = creature.isPlayer() ? creature.asPlayer().getCurrentSkillWorldPosition() : null;
   }

   public MagicSkillUse(Creature creature, int skillId, int skillLevel, int hitTime, int reuseDelay) {
      this(creature, creature, skillId, skillLevel, hitTime, reuseDelay);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MAGIC_SKILL_USE.writeId(this, buffer);
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeInt(this._target.getObjectId());
      buffer.writeInt(this._skillId);
      buffer.writeInt(this._skillLevel);
      buffer.writeInt(this._hitTime);
      buffer.writeInt(this._reuseDelay);
      buffer.writeInt(this._creature.getX());
      buffer.writeInt(this._creature.getY());
      buffer.writeInt(this._creature.getZ());
      buffer.writeShort(0);
      if (this._groundLocation == null) {
         buffer.writeShort(0);
      } else {
         buffer.writeShort(1);
         buffer.writeInt(this._groundLocation.getX());
         buffer.writeInt(this._groundLocation.getY());
         buffer.writeInt(this._groundLocation.getZ());
      }

      buffer.writeInt(this._target.getX());
      buffer.writeInt(this._target.getY());
      buffer.writeInt(this._target.getZ());
   }
}
