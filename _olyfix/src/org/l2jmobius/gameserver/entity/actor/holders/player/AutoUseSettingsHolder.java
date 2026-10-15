package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoUseSettingsHolder {
   private final Collection<Integer> _autoSupplyItems = ConcurrentHashMap.newKeySet();
   private final Collection<Integer> _autoActions = ConcurrentHashMap.newKeySet();
   private final Collection<Integer> _autoBuffs = ConcurrentHashMap.newKeySet();
   private final List<Integer> _autoSkills = new CopyOnWriteArrayList<>();
   private final AtomicInteger _autoPotionItem = new AtomicInteger();
   private int _skillIndex = 0;
   /** FakePlayersAI bots: the auto-use engines must never cast buffs/heals on anyone but the player himself. */
   private volatile boolean _selfCastOnly = false;

   public boolean isSelfCastOnly() {
      return this._selfCastOnly;
   }

   public void setSelfCastOnly(boolean value) {
      this._selfCastOnly = value;
   }

   public Collection<Integer> getAutoSupplyItems() {
      return this._autoSupplyItems;
   }

   public Collection<Integer> getAutoActions() {
      return this._autoActions;
   }

   public Collection<Integer> getAutoBuffs() {
      return this._autoBuffs;
   }

   public List<Integer> getAutoSkills() {
      return this._autoSkills;
   }

   public int getAutoPotionItem() {
      return this._autoPotionItem.get();
   }

   public void setAutoPotionItem(int itemId) {
      this._autoPotionItem.set(itemId);
   }

   public boolean isAutoSkill(int skillId) {
      return this._autoSkills.contains(skillId) || this._autoBuffs.contains(skillId);
   }

   public Integer getNextSkillId() {
      if (this._skillIndex >= this._autoSkills.size()) {
         this._skillIndex = 0;
      }

      Integer skillId = Integer.MIN_VALUE;

      try {
         skillId = this._autoSkills.get(this._skillIndex);
      } catch (Exception var3) {
         this.resetSkillOrder();
      }

      return skillId;
   }

   public void incrementSkillOrder() {
      this._skillIndex++;
   }

   public void resetSkillOrder() {
      this._skillIndex = 0;
   }

   public boolean isEmpty() {
      return this._autoSupplyItems.isEmpty()
         && this._autoPotionItem.get() == 0
         && this._autoSkills.isEmpty()
         && this._autoBuffs.isEmpty()
         && this._autoActions.isEmpty();
   }
}
