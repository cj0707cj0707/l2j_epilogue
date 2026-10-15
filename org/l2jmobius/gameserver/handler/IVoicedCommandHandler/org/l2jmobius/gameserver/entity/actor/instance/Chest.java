package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

public class Chest extends Monster {
   private volatile boolean _isInteracted;
   private volatile boolean _specialDrop;

   public Chest(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Chest);
      this.setRandomWalking(false);
      this._specialDrop = false;
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._isInteracted = false;
      this._specialDrop = false;
      this.setMustRewardExpSp(true);
   }

   public synchronized boolean isInteracted() {
      return this._isInteracted;
   }

   public synchronized void setInteracted() {
      this._isInteracted = true;
   }

   public synchronized void setSpecialDrop() {
      this._specialDrop = true;
   }

   @Override
   public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker) {
      int id = this.getTemplate().getId();
      if (!this._specialDrop) {
         if (id >= 18265 && id <= 18286) {
            id += 3536;
         } else if (id == 18287 || id == 18288) {
            id = 21671;
         } else if (id == 18289 || id == 18290) {
            id = 21694;
         } else if (id == 18291 || id == 18292) {
            id = 21717;
         } else if (id == 18293 || id == 18294) {
            id = 21740;
         } else if (id == 18295 || id == 18296) {
            id = 21763;
         } else if (id == 18297 || id == 18298) {
            id = 21786;
         }
      }

      super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
   }

   public void chestTrap(Creature player) {
      int trapSkillId = 0;
      int rnd = Rnd.get(120);
      short var4;
      if (this.getTemplate().getLevel() >= 61) {
         if (rnd >= 90) {
            var4 = 4139;
         } else if (rnd >= 50) {
            var4 = 4118;
         } else if (rnd >= 20) {
            var4 = 1167;
         } else {
            var4 = 223;
         }
      } else if (this.getTemplate().getLevel() >= 41) {
         if (rnd >= 90) {
            var4 = 4139;
         } else if (rnd >= 60) {
            var4 = 96;
         } else if (rnd >= 20) {
            var4 = 1167;
         } else {
            var4 = 4118;
         }
      } else if (this.getTemplate().getLevel() >= 21) {
         if (rnd >= 80) {
            var4 = 4139;
         } else if (rnd >= 50) {
            var4 = 96;
         } else if (rnd >= 20) {
            var4 = 1167;
         } else {
            var4 = 129;
         }
      } else if (rnd >= 80) {
         var4 = 4139;
      } else if (rnd >= 50) {
         var4 = 96;
      } else {
         var4 = 129;
      }

      player.sendMessage("There was a trap!");
      this.handleCast(player, var4);
   }

   private boolean handleCast(Creature player, int skillId) {
      int skillLevel = 1;
      byte level = this.getTemplate().getLevel();
      if (level > 20 && level <= 40) {
         skillLevel = 3;
      } else if (level > 40 && level <= 60) {
         skillLevel = 5;
      } else if (level > 60) {
         skillLevel = 6;
      }

      if (!player.isDead() && !player.isInvisible() && player.isInsideRadius2D(this, 1500)) {
         Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
         if (!player.isAffectedBySkill(skillId)) {
            skill.applyEffects(this, player);
            this.broadcastPacket(new MagicSkillUse(this, player, skillId, skillLevel, skill.getHitTime(), 0));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean isMovementDisabled() {
      return true;
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }
}
