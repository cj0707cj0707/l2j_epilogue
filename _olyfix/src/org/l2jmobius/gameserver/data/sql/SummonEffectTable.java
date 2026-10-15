package org.l2jmobius.gameserver.data.sql;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class SummonEffectTable {
   private final Map<Integer, Map<Integer, Map<Integer, Collection<SummonEffectTable.SummonEffect>>>> _servitorEffects = new ConcurrentHashMap<>();
   private final Map<Integer, Collection<SummonEffectTable.SummonEffect>> _petEffects = new ConcurrentHashMap<>();

   private Map<Integer, Collection<SummonEffectTable.SummonEffect>> getServitorEffects(Player owner) {
      Map<Integer, Map<Integer, Collection<SummonEffectTable.SummonEffect>>> servitorMap = this._servitorEffects.get(owner.getObjectId());
      return servitorMap == null ? null : servitorMap.get(owner.getClassIndex());
   }

   private Collection<SummonEffectTable.SummonEffect> getServitorEffects(Player owner, int referenceSkill) {
      return this.containsOwner(owner) ? this.getServitorEffects(owner).get(referenceSkill) : null;
   }

   private boolean containsOwner(Player owner) {
      return this._servitorEffects.getOrDefault(owner.getObjectId(), Collections.emptyMap()).containsKey(owner.getClassIndex());
   }

   private void removeEffects(Collection<SummonEffectTable.SummonEffect> effects, int skillId) {
      if (effects != null && !effects.isEmpty()) {
         for (SummonEffectTable.SummonEffect effect : effects) {
            Skill skill = effect.getSkill();
            if (skill != null && skill.getId() == skillId) {
               effects.remove(effect);
            }
         }
      }
   }

   private void applyEffects(Summon summon, Collection<SummonEffectTable.SummonEffect> summonEffects) {
      if (summonEffects != null) {
         for (SummonEffectTable.SummonEffect se : summonEffects) {
            if (se != null) {
               se.getSkill().applyEffects(summon, summon, false, se.getEffectCurTime());
            }
         }
      }
   }

   public boolean containsSkill(Player owner, int referenceSkill) {
      return this.containsOwner(owner) && this.getServitorEffects(owner).containsKey(referenceSkill);
   }

   public void clearServitorEffects(Player owner, int referenceSkill) {
      if (this.containsOwner(owner)) {
         this.getServitorEffects(owner).getOrDefault(referenceSkill, Collections.emptyList()).clear();
      }
   }

   public void addServitorEffect(Player owner, int referenceSkill, Skill skill, int effectCurTime) {
      this._servitorEffects.putIfAbsent(owner.getObjectId(), new ConcurrentHashMap<>());
      this._servitorEffects.get(owner.getObjectId()).putIfAbsent(owner.getClassIndex(), new ConcurrentHashMap<>());
      this.getServitorEffects(owner).putIfAbsent(referenceSkill, ConcurrentHashMap.newKeySet());
      this.getServitorEffects(owner).get(referenceSkill).add(new SummonEffectTable.SummonEffect(skill, effectCurTime));
   }

   public void removeServitorEffects(Player owner, int referenceSkill, int skillId) {
      this.removeEffects(this.getServitorEffects(owner, referenceSkill), skillId);
   }

   public void applyServitorEffects(Servitor servitor, Player owner, int referenceSkill) {
      this.applyEffects(servitor, this.getServitorEffects(owner, referenceSkill));
   }

   public void addPetEffect(int controlObjectId, Skill skill, int effectCurTime) {
      this._petEffects.computeIfAbsent(controlObjectId, var0 -> ConcurrentHashMap.newKeySet()).add(new SummonEffectTable.SummonEffect(skill, effectCurTime));
   }

   public boolean containsPetId(int controlObjectId) {
      return this._petEffects.containsKey(controlObjectId);
   }

   public void applyPetEffects(Pet pet, int controlObjectId) {
      this.applyEffects(pet, this._petEffects.get(controlObjectId));
   }

   public void clearPetEffects(int controlObjectId) {
      this._petEffects.getOrDefault(controlObjectId, Collections.emptyList()).clear();
   }

   public void removePetEffects(int controlObjectId, int skillId) {
      this.removeEffects(this._petEffects.get(controlObjectId), skillId);
   }

   public static SummonEffectTable getInstance() {
      return SummonEffectTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SummonEffectTable INSTANCE = new SummonEffectTable();
   }

   private class SummonEffect {
      Skill _skill;
      int _effectCurTime;

      public SummonEffect(Skill skill, int effectCurTime) {
         Objects.requireNonNull(SummonEffectTable.this);
         super();
         this._skill = skill;
         this._effectCurTime = effectCurTime;
      }

      public Skill getSkill() {
         return this._skill;
      }

      public int getEffectCurTime() {
         return this._effectCurTime;
      }
   }
}
