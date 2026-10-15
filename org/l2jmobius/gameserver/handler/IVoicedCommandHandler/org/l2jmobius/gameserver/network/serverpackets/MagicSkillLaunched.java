package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.Collections;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MagicSkillLaunched extends ServerPacket {
   private final int _objectId;
   private final int _skillId;
   private final int _skillLevel;
   private final Collection<WorldObject> _targets;

   public MagicSkillLaunched(Creature creature, int skillId, int skillLevel, Collection<WorldObject> targets) {
      this._objectId = creature.getObjectId();
      this._skillId = skillId;
      this._skillLevel = skillLevel;
      if (targets == null) {
         this._targets = Collections.singletonList(creature);
      } else {
         this._targets = targets;
      }
   }

   public MagicSkillLaunched(Creature creature, int skillId, int skillLevel, WorldObject target) {
      this(creature, skillId, skillLevel, Collections.singletonList((WorldObject)(target == null ? creature : target)));
   }

   public MagicSkillLaunched(Creature creature, int skillId, int skillLevel) {
      this(creature, skillId, skillLevel, Collections.singletonList(creature));
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MAGIC_SKILL_LAUNCHED.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._skillId);
      buffer.writeInt(this._skillLevel);
      buffer.writeInt(this._targets.size());

      for (WorldObject target : this._targets) {
         buffer.writeInt(target.getObjectId());
      }
   }
}
