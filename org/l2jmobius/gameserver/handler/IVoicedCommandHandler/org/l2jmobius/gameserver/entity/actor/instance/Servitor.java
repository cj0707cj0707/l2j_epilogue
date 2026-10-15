package org.l2jmobius.gameserver.entity.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SetSummonRemainTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Servitor extends Summon implements Runnable {
   protected static final Logger log = Logger.getLogger(Servitor.class.getName());
   private static final String ADD_SKILL_SAVE = "REPLACE INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?,?)";
   private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
   private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";
   private float _expMultiplier = 0.0F;
   private ItemHolder _itemConsume;
   private int _lifeTime;
   private int _lifeTimeRemaining;
   private int _consumeItemInterval;
   private int _consumeItemIntervalRemaining;
   protected Future<?> _summonLifeTask;
   private int _referenceSkill;

   public Servitor(NpcTemplate template, Player owner) {
      super(template, owner);
      this.setInstanceType(InstanceType.Servitor);
      this.setShowSummonAnimation(true);
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      if (this._summonLifeTask == null) {
         this._summonLifeTask = ThreadPool.scheduleAtFixedRate(this, 0L, 5000L);
      }
   }

   @Override
   public int getLevel() {
      return this.getTemplate() != null ? this.getTemplate().getLevel() : 0;
   }

   @Override
   public int getSummonType() {
      return 1;
   }

   public void setExpMultiplier(float expMultiplier) {
      this._expMultiplier = expMultiplier;
   }

   public float getExpMultiplier() {
      return this._expMultiplier;
   }

   public void setItemConsume(ItemHolder item) {
      this._itemConsume = item;
   }

   public ItemHolder getItemConsume() {
      return this._itemConsume;
   }

   public void setItemConsumeInterval(int interval) {
      this._consumeItemInterval = interval;
      this._consumeItemIntervalRemaining = interval;
   }

   public int getItemConsumeInterval() {
      return this._consumeItemInterval;
   }

   public void setLifeTime(int lifeTime) {
      this._lifeTime = lifeTime;
      this._lifeTimeRemaining = lifeTime;
   }

   public int getLifeTime() {
      return this._lifeTime;
   }

   public void setLifeTimeRemaining(int time) {
      this._lifeTimeRemaining = time;
   }

   public int getLifeTimeRemaining() {
      return this._lifeTimeRemaining;
   }

   public void setReferenceSkill(int skillId) {
      this._referenceSkill = skillId;
   }

   public int getReferenceSkill() {
      return this._referenceSkill;
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this._summonLifeTask != null) {
            this._summonLifeTask.cancel(false);
         }

         CharSummonTable.getInstance().removeServitor(this.getOwner());
         return true;
      }
   }

   @Override
   public void doPickupItem(WorldObject object) {
   }

   @Override
   public void setRestoreSummon(boolean value) {
      this._restoreSummon = value;
   }

   @Override
   public void stopSkillEffects(SkillFinishType type, int skillId) {
      super.stopSkillEffects(type, skillId);
      SummonEffectTable.getInstance().removeServitorEffects(this.getOwner(), this.getReferenceSkill(), skillId);
   }

   @Override
   public void storeMe() {
      if (this._referenceSkill != 0 && !this.isDead()) {
         if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT) {
            CharSummonTable.getInstance().saveSummon(this);
         }
      }
   }

   @Override
   public void storeEffect(boolean storeEffects) {
      if (PlayerConfig.SUMMON_STORE_SKILL_COOLTIME && this.getOwner() != null && !this.getOwner().isInOlympiadMode()) {
         SummonEffectTable.getInstance().clearServitorEffects(this.getOwner(), this.getReferenceSkill());

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?");
         ) {
            ps.setInt(1, this.getOwner().getObjectId());
            ps.setInt(2, this.getOwner().getClassIndex());
            ps.setInt(3, this._referenceSkill);
            ps.execute();
            int buffIndex = 0;
            List<Integer> storedSkills = new ArrayList<>();
            if (storeEffects) {
               try (PreparedStatement ps2 = con.prepareStatement(
                     "REPLACE INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?,?)"
                  )) {
                  for (BuffInfo info : this.getEffectList().getEffects()) {
                     if (info != null) {
                        Skill skill = info.getSkill();
                        if (skill.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS
                           && !skill.isToggle()
                           && (!skill.isDance() || PlayerConfig.ALT_STORE_DANCES)
                           && !storedSkills.contains(skill.getReuseHashCode())) {
                           storedSkills.add(skill.getReuseHashCode());
                           ps2.setInt(1, this.getOwner().getObjectId());
                           ps2.setInt(2, this.getOwner().getClassIndex());
                           ps2.setInt(3, this._referenceSkill);
                           ps2.setInt(4, skill.getId());
                           ps2.setInt(5, skill.getLevel());
                           ps2.setInt(6, info.getTime());
                           ps2.setInt(7, ++buffIndex);
                           ps2.addBatch();
                           SummonEffectTable.getInstance().addServitorEffect(this.getOwner(), this.getReferenceSkill(), skill, info.getTime());
                        }
                     }
                  }

                  ps2.executeBatch();
               }
            }
         } catch (Exception var16) {
            LOGGER.log(Level.WARNING, "Could not store summon effect data: ", (Throwable)var16);
         }
      }
   }

   @Override
   public void restoreEffects() {
      if (!this.getOwner().isInOlympiadMode()) {
         try (Connection con = DatabaseFactory.getConnection()) {
            if (!SummonEffectTable.getInstance().containsSkill(this.getOwner(), this.getReferenceSkill())) {
               try (PreparedStatement ps = con.prepareStatement(
                     "SELECT skill_id,skill_level,remaining_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC"
                  )) {
                  ps.setInt(1, this.getOwner().getObjectId());
                  ps.setInt(2, this.getOwner().getClassIndex());
                  ps.setInt(3, this._referenceSkill);

                  try (ResultSet rs = ps.executeQuery()) {
                     while (rs.next()) {
                        Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
                        if (skill != null && skill.hasEffects(EffectScope.GENERAL)) {
                           SummonEffectTable.getInstance().addServitorEffect(this.getOwner(), this.getReferenceSkill(), skill, rs.getInt("remaining_time"));
                        }
                     }
                  }
               }
            }

            try (PreparedStatement statement = con.prepareStatement(
                  "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?"
               )) {
               statement.setInt(1, this.getOwner().getObjectId());
               statement.setInt(2, this.getOwner().getClassIndex());
               statement.setInt(3, this._referenceSkill);
               statement.executeUpdate();
            }
         } catch (Exception var25) {
            LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + var25.getMessage(), (Throwable)var25);
         } finally {
            SummonEffectTable.getInstance().applyServitorEffects(this, this.getOwner(), this.getReferenceSkill());
         }
      }
   }

   @Override
   public void unSummon(Player owner) {
      if (this._summonLifeTask != null) {
         this._summonLifeTask.cancel(false);
      }

      super.unSummon(owner);
      if (!this._restoreSummon) {
         CharSummonTable.getInstance().removeServitor(owner);
      }
   }

   @Override
   public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage) {
      return this.getOwner().destroyItem(process, objectId, count, reference, sendMessage);
   }

   @Override
   public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage) {
      return this.getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
   }

   @Override
   public byte getAttackElement() {
      return super.getAttackElement();
   }

   @Override
   public int getAttackElementValue(byte attackAttribute) {
      return super.getAttackElementValue(attackAttribute);
   }

   @Override
   public int getDefenseElementValue(byte defenseAttribute) {
      return super.getDefenseElementValue(defenseAttribute);
   }

   @Override
   public boolean isServitor() {
      return true;
   }

   @Override
   public Servitor asServitor() {
      return this;
   }

   @Override
   public void run() {
      int usedtime = 5000;
      this._lifeTimeRemaining -= 5000;
      if (!this.isDead() && this.isSpawned()) {
         if (this._lifeTimeRemaining < 0) {
            this.sendPacket(SystemMessageId.SERVITOR_PASSED_AWAY);
            this.unSummon(this.getOwner());
         } else {
            if (this._consumeItemInterval > 0) {
               this._consumeItemIntervalRemaining -= 5000;
               if (this._consumeItemIntervalRemaining <= 0 && this._itemConsume.getCount() > 0L && this._itemConsume.getId() > 0 && !this.isDead()) {
                  if (this.destroyItemByItemId(null, this._itemConsume.getId(), this._itemConsume.getCount(), this, false)) {
                     SystemMessage msg = new SystemMessage(SystemMessageId.A_SUMMONED_MONSTER_USES_S1);
                     msg.addItemName(this._itemConsume.getId());
                     this.sendPacket(msg);
                     this._consumeItemIntervalRemaining = this._consumeItemInterval;
                  } else {
                     this.sendPacket(SystemMessageId.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITOR_S_STAY_THE_SERVITOR_WILL_DISAPPEAR);
                     this.unSummon(this.getOwner());
                  }
               }
            }

            this.sendPacket(new SetSummonRemainTime(this._lifeTime, this._lifeTimeRemaining));
            this.updateEffectIcons();
         }
      } else {
         if (this._summonLifeTask != null) {
            this._summonLifeTask.cancel(false);
         }
      }
   }

   @Override
   public double getMAtk(Creature target, Skill skill) {
      Player player = this.asPlayer();
      return player == null
         ? super.getMAtk(target, skill)
         : super.getMAtk(target, skill) + player.getMAtk(target, skill) * (player.getServitorShareBonus(Stat.MAGIC_ATTACK) - 1.0);
   }

   @Override
   public double getMDef(Creature target, Skill skill) {
      Player player = this.asPlayer();
      return player == null
         ? super.getMDef(target, skill)
         : super.getMDef(target, skill) + player.getMDef(target, skill) * (player.getServitorShareBonus(Stat.MAGIC_DEFENCE) - 1.0);
   }

   @Override
   public double getPAtk(Creature target) {
      Player player = this.asPlayer();
      return player == null ? super.getPAtk(target) : super.getPAtk(target) + player.getPAtk(target) * (player.getServitorShareBonus(Stat.POWER_ATTACK) - 1.0);
   }

   @Override
   public double getPDef(Creature target) {
      Player player = this.asPlayer();
      return player == null ? super.getPDef(target) : super.getPDef(target) + player.getPDef(target) * (player.getServitorShareBonus(Stat.POWER_DEFENCE) - 1.0);
   }

   @Override
   public int getMAtkSpd() {
      Player player = this.asPlayer();
      return player == null
         ? super.getMAtkSpd()
         : (int)(super.getMAtkSpd() + player.getMAtkSpd() * (player.getServitorShareBonus(Stat.MAGIC_ATTACK_SPEED) - 1.0));
   }

   @Override
   public int getMaxHp() {
      Player player = this.asPlayer();
      return player == null ? super.getMaxHp() : (int)(super.getMaxHp() + player.getMaxHp() * (player.getServitorShareBonus(Stat.MAX_HP) - 1.0));
   }

   @Override
   public int getMaxMp() {
      Player player = this.asPlayer();
      return player == null ? super.getMaxMp() : (int)(super.getMaxMp() + player.getMaxMp() * (player.getServitorShareBonus(Stat.MAX_MP) - 1.0));
   }

   @Override
   public int getCriticalHit(Creature target, Skill skill) {
      Player player = this.asPlayer();
      return player == null
         ? super.getCriticalHit(target, skill)
         : (int)(super.getCriticalHit(target, skill) + player.getCriticalHit(target, skill) * (player.getServitorShareBonus(Stat.CRITICAL_RATE) - 1.0));
   }

   @Override
   public double getPAtkSpd() {
      Player player = this.asPlayer();
      return player == null ? super.getPAtkSpd() : super.getPAtkSpd() + player.getPAtkSpd() * (player.getServitorShareBonus(Stat.POWER_ATTACK_SPEED) - 1.0);
   }
}
