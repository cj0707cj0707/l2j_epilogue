package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConditionPlayerCanSummon extends Condition {
   private final boolean _value;

   public ConditionPlayerCanSummon(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player != null && !player.isSpawnProtected() && !player.isTeleportProtected()) {
         boolean canSummon = true;
         if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT && CharSummonTable.getInstance().getServitors().containsKey(player.getObjectId())) {
            player.sendPacket(SystemMessageId.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
            canSummon = false;
         } else if (PlayerConfig.RESTORE_PET_ON_RECONNECT && CharSummonTable.getInstance().getPets().containsKey(player.getObjectId())) {
            player.sendPacket(SystemMessageId.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
            canSummon = false;
         } else if (player.hasSummon()) {
            player.sendPacket(SystemMessageId.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
            canSummon = false;
         } else if (player.isFlyingMounted() || player.isMounted()) {
            canSummon = false;
         }

         return this._value == canSummon;
      } else {
         return false;
      }
   }
}
