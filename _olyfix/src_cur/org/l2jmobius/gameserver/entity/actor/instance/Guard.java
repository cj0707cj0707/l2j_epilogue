package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class Guard extends Attackable {
   public Guard(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Guard);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (attacker.isMonster() && !attacker.isFakePlayer()) {
         return true;
      } else {
         if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && attacker.isPlayable()) {
            Player player = attacker.asPlayer();
            if (player.isGood() && this.getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)
               || player.isEvil() && this.getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME)) {
               return true;
            }
         }

         return super.isAutoAttackable(attacker);
      }
   }

   @Override
   public void addDamage(Creature attacker, int damage, Skill skill) {
      super.addDamage(attacker, damage, skill);
      this.getAI().startFollow(attacker);
      this.addDamageHate(attacker, 0L, 10L);
      World.forEachVisibleObjectInRange(this, Guard.class, 500, guard -> {
         guard.getAI().startFollow(attacker);
         guard.addDamageHate(attacker, 0L, 10L);
      });
   }

   @Override
   public void onSpawn() {
      this.setRandomWalking(false);
      super.onSpawn();
      this.getAI().setIntentionActive();
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/guard/" + pom + ".htm";
   }

   @Override
   public void onAction(Player player, boolean interactValue) {
      if (this.canTarget(player)) {
         boolean interact = interactValue;
         if (FactionSystemConfig.FACTION_SYSTEM_ENABLED
            && FactionSystemConfig.FACTION_GUARDS_ENABLED
            && (
               player.isGood() && this.getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)
                  || player.isEvil() && this.getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME)
            )) {
            interact = false;
            player.getAI().setIntentionAttack(this);
         }

         if (this.isFakePlayer() && this.isInCombat()) {
            interact = false;
            player.getAI().setIntentionAttack(this);
         }

         if (this.getObjectId() != player.getTargetId()) {
            player.setTarget(this);
         } else if (interact) {
            if (this.isInAggroList(player)) {
               player.getAI().setIntentionAttack(this);
            } else if (!this.canInteract(player)) {
               player.getAI().setIntentionInteract(this);
            } else {
               player.setLastFolkNPC(this);
               if (this.hasListener(EventType.ON_NPC_QUEST_START)) {
                  player.setLastQuestNpcObject(this.getObjectId());
               }

               if (this.hasListener(EventType.ON_NPC_FIRST_TALK)) {
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_FIRST_TALK, this)) {
                     EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
                  }
               } else {
                  this.showChatWindow(player, 0);
               }
            }
         }

         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }
}
