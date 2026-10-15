package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Objects;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

public class FortCommander extends Defender {
   private boolean _canTalk;

   public FortCommander(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FortCommander);
      this._canTalk = true;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return attacker != null && attacker.isPlayer()
         ? this.getFort() != null
            && this.getFort().getResidenceId() > 0
            && this.getFort().getSiege().isInProgress()
            && !this.getFort().getSiege().checkIsDefender(attacker.asPlayer().getClan())
         : false;
   }

   @Override
   public void addDamageHate(Creature attacker, long damage, long aggro) {
      if (attacker != null) {
         if (!(attacker instanceof FortCommander)) {
            super.addDamageHate(attacker, damage, aggro);
         }
      }
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this.getFort().getSiege().isInProgress()) {
            this.getFort().getSiege().killedCommander(this);
         }

         return true;
      }
   }

   @Override
   public void returnHome() {
      if (!this.isInsideRadius2D(this.getSpawn(), 200)) {
         this.clearAggroList();
         if (this.hasAI()) {
            this.getAI().setIntentionMoveTo(this.getSpawn().getLocation());
         }
      }
   }

   @Override
   public void addDamage(Creature creature, int damage, Skill skill) {
      Creature attacker = creature;
      Spawn spawn = this.getSpawn();
      if (spawn != null && this.canTalk()) {
         for (FortSpawnHolder spawn2 : FortSiegeManager.getInstance().getCommanderSpawnList(this.getFort().getResidenceId())) {
            if (spawn2.getId() == spawn.getId()) {
               String npcString = null;
               switch (spawn2.getMessageId()) {
                  case 1:
                     npcString = "Attacking the enemy's reinforcements is necessary. Time to die!";
                     break;
                  case 2:
                     if (attacker.isSummon()) {
                        attacker = attacker.asSummon().getOwner();
                     }

                     npcString = "Everyone, concentrate your attacks on " + attacker.getName() + "! Show the enemy your resolve!";
                     break;
                  case 3:
                     npcString = "Spirit of Fire, unleash your power! Burn the enemy!!";
               }

               if (npcString != null) {
                  NpcSay ns = new NpcSay(this.getObjectId(), ChatType.NPC_SHOUT, this.getId(), npcString);
                  this.broadcastPacket(ns);
                  this.setCanTalk(false);
                  ThreadPool.schedule(new FortCommander.ScheduleTalkTask(), 10000L);
               }
            }
         }
      }

      super.addDamage(attacker, damage, skill);
   }

   void setCanTalk(boolean value) {
      this._canTalk = value;
   }

   private boolean canTalk() {
      return this._canTalk;
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }

   private class ScheduleTalkTask implements Runnable {
      public ScheduleTalkTask() {
         Objects.requireNonNull(FortCommander.this);
         super();
      }

      @Override
      public void run() {
         FortCommander.this.setCanTalk(true);
      }
   }
}
