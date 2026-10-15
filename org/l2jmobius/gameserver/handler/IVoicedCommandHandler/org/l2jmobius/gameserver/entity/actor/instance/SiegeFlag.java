package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Objects;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.status.SiegeFlagStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class SiegeFlag extends Npc {
   private Clan _clan;
   private Siegable _siege;
   private final boolean _isAdvanced;
   private boolean _canTalk;

   public SiegeFlag(Player player, NpcTemplate template, boolean advanced, boolean outPost) {
      super(template);
      this.setInstanceType(InstanceType.SiegeFlag);
      if (TerritoryWarManager.getInstance().isTWInProgress()) {
         this._clan = player.getClan();
         this._canTalk = false;
         if (this._clan == null) {
            this.deleteMe();
         }

         if (outPost) {
            this._isAdvanced = false;
            this.setInvul(true);
         } else {
            this._isAdvanced = advanced;
            this.setInvul(false);
         }

         this.getStatus();
      } else {
         this._clan = player.getClan();
         this._canTalk = true;
         this._siege = SiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
         if (this._siege == null) {
            this._siege = FortSiegeManager.getInstance().getSiege(player.getX(), player.getY(), player.getZ());
         }

         if (this._siege == null) {
            this._siege = CHSiegeManager.getInstance().getSiege(player);
         }

         if (this._clan != null && this._siege != null) {
            SiegeClan sc = this._siege.getAttackerClan(this._clan);
            if (sc == null) {
               throw new NullPointerException(this.getClass().getSimpleName() + ": Cannot find siege clan.");
            } else {
               sc.addFlag(this);
               this._isAdvanced = advanced;
               this.getStatus();
               this.setInvul(false);
            }
         } else {
            throw new NullPointerException(this.getClass().getSimpleName() + ": Initialization failed.");
         }
      }
   }

   @Override
   public boolean canBeAttacked() {
      return !this.isInvul();
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return !this.isInvul();
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this._siege != null && this._clan != null) {
            SiegeClan sc = this._siege.getAttackerClan(this._clan);
            if (sc != null) {
               sc.removeFlag(this);
            }
         } else if (this._clan != null) {
            TerritoryWarManager.getInstance().removeClanFlag(this._clan);
         }

         return true;
      }
   }

   @Override
   public void onForcedAttack(Player player) {
      this.onAction(player);
   }

   @Override
   public void onAction(Player player, boolean interact) {
      if (player != null && this.canTarget(player)) {
         if (this != player.getTarget()) {
            player.setTarget(this);
         } else if (interact) {
            if (this.isAutoAttackable(player) && Math.abs(player.getZ() - this.getZ()) < 100) {
               player.getAI().setIntentionAttack(this);
            } else {
               player.sendPacket(ActionFailed.STATIC_PACKET);
            }
         }
      }
   }

   public boolean isAdvancedHeadquarter() {
      return this._isAdvanced;
   }

   public SiegeFlagStatus getStatus() {
      return (SiegeFlagStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new SiegeFlagStatus(this));
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, Skill skill) {
      super.reduceCurrentHp(damage, attacker, skill);
      if (this.canTalk()
         && (
            this.getCastle() != null && this.getCastle().getSiege().isInProgress()
               || this.getFort() != null && this.getFort().getSiege().isInProgress()
               || this.getConquerableHall() != null && this.getConquerableHall().isInSiege()
         )
         && this._clan != null) {
         this._clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.YOUR_BASE_IS_BEING_ATTACKED));
         this.setCanTalk(false);
         ThreadPool.schedule(new SiegeFlag.ScheduleTalkTask(), 20000L);
      }
   }

   void setCanTalk(boolean value) {
      this._canTalk = value;
   }

   private boolean canTalk() {
      return this._canTalk;
   }

   private class ScheduleTalkTask implements Runnable {
      public ScheduleTalkTask() {
         Objects.requireNonNull(SiegeFlag.this);
         super();
      }

      @Override
      public void run() {
         SiegeFlag.this.setCanTalk(true);
      }
   }
}
