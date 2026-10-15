package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class EffectPoint extends Npc {
   private final Player _owner;

   public EffectPoint(NpcTemplate template, Creature owner) {
      super(template);
      this.setInstanceType(InstanceType.EffectPoint);
      this.setInvul(false);
      this._owner = owner == null ? null : owner.asPlayer();
      if (owner != null) {
         this.setInstanceId(owner.getInstanceId());
      }
   }

   @Override
   public Player asPlayer() {
      return this._owner;
   }

   @Override
   public void onAction(Player player, boolean interact) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   @Override
   public void onActionShift(Player player) {
      if (player != null) {
         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }
}
