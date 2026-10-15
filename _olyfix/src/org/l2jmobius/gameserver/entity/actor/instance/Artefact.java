package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class Artefact extends Npc {
   public Artefact(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Artefact);
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this.getCastle().registerArtefact(this);
   }

   @Override
   public boolean isArtefact() {
      return true;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return false;
   }

   @Override
   public boolean canBeAttacked() {
      return false;
   }

   @Override
   public void onForcedAttack(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, Skill skill) {
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
   }
}
