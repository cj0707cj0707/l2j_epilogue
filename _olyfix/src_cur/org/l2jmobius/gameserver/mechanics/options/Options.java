package org.l2jmobius.gameserver.mechanics.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

public class Options {
   private final int _id;
   private final List<FuncTemplate> _funcs = new ArrayList<>();
   private Skill _activeSkill = null;
   private Skill _passiveSkill = null;
   private final List<OptionSkillHolder> _activationSkills = new ArrayList<>();

   public Options(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }

   public boolean hasFuncs() {
      return !this._funcs.isEmpty();
   }

   public List<AbstractFunction> getStatFuncs(Item item, Creature creature) {
      if (this._funcs.isEmpty()) {
         return Collections.emptyList();
      } else {
         List<AbstractFunction> funcs = new ArrayList<>(this._funcs.size());

         for (FuncTemplate fuctionTemplate : this._funcs) {
            AbstractFunction fuction = fuctionTemplate.getFunc(creature, creature, item, this);
            if (fuction != null) {
               funcs.add(fuction);
            }
         }

         return funcs;
      }
   }

   public void addFunc(FuncTemplate template) {
      this._funcs.add(template);
   }

   public boolean hasActiveSkill() {
      return this._activeSkill != null;
   }

   public Skill getActiveSkill() {
      return this._activeSkill;
   }

   public void setActiveSkill(Skill skill) {
      this._activeSkill = skill;
   }

   public boolean hasPassiveSkill() {
      return this._passiveSkill != null;
   }

   public Skill getPassiveSkill() {
      return this._passiveSkill;
   }

   public void setPassiveSkill(Skill skill) {
      this._passiveSkill = skill;
   }

   public boolean hasActivationSkills() {
      return !this._activationSkills.isEmpty();
   }

   public boolean hasActivationSkills(OptionSkillType type) {
      for (OptionSkillHolder holder : this._activationSkills) {
         if (holder.getSkillType() == type) {
            return true;
         }
      }

      return false;
   }

   public List<OptionSkillHolder> getActivationSkills() {
      return this._activationSkills;
   }

   public List<OptionSkillHolder> getActivationSkills(OptionSkillType type) {
      List<OptionSkillHolder> temp = new ArrayList<>();

      for (OptionSkillHolder holder : this._activationSkills) {
         if (holder.getSkillType() == type) {
            temp.add(holder);
         }
      }

      return temp;
   }

   public void addActivationSkill(OptionSkillHolder holder) {
      this._activationSkills.add(holder);
   }

   public void apply(Player player) {
      if (!this._funcs.isEmpty()) {
         player.addStatFuncs(this.getStatFuncs(null, player));
      }

      if (this.hasActiveSkill()) {
         this.addSkill(player, this._activeSkill);
      }

      if (this.hasPassiveSkill()) {
         this.addSkill(player, this._passiveSkill);
      }

      if (!this._activationSkills.isEmpty()) {
         for (OptionSkillHolder holder : this._activationSkills) {
            player.addTriggerSkill(holder);
         }
      }

      player.sendSkillList();
   }

   public void remove(Player player) {
      if (!this._funcs.isEmpty()) {
         player.removeStatsOwner(this);
      }

      if (this.hasActiveSkill()) {
         player.removeSkill(this._activeSkill, false, false);
      }

      if (this.hasPassiveSkill()) {
         player.removeSkill(this._passiveSkill, false, true);
      }

      if (!this._activationSkills.isEmpty()) {
         for (OptionSkillHolder holder : this._activationSkills) {
            player.removeTriggerSkill(holder);
         }
      }

      player.sendSkillList();
   }

   private void addSkill(Player player, Skill skill) {
      boolean updateTimeStamp = false;
      player.addSkill(skill, false);
      if (skill.isActive()) {
         long remainingTime = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
         if (remainingTime > 0L) {
            player.addTimeStamp(skill, remainingTime);
            player.disableSkill(skill, remainingTime);
         }

         updateTimeStamp = true;
      }

      if (updateTimeStamp) {
         player.sendPacket(new SkillCoolTime(player));
      }
   }
}
