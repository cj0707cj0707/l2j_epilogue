package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Objects;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.ControllableAirShipStat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ControllableAirShip extends AirShip {
   private static final int HELM = 13556;
   private static final int LOW_FUEL = 40;
   private int _fuel = 0;
   private int _maxFuel = 0;
   private final int _ownerId;
   private int _helmId;
   private Player _captain = null;
   private Future<?> _consumeFuelTask;
   private Future<?> _checkTask;

   public ControllableAirShip(CreatureTemplate template, int ownerId) {
      super(template);
      this.setInstanceType(InstanceType.ControllableAirShip);
      this._ownerId = ownerId;
      this._helmId = IdManager.getInstance().getNextId();
   }

   public ControllableAirShipStat getStat() {
      return (ControllableAirShipStat)super.getStat();
   }

   @Override
   public void initCharStat() {
      this.setStat(new ControllableAirShipStat(this));
   }

   @Override
   public boolean canBeControlled() {
      return super.canBeControlled() && !this.isInDock();
   }

   @Override
   public boolean isOwner(Player player) {
      return this._ownerId == 0 ? false : player.getClanId() == this._ownerId || player.getObjectId() == this._ownerId;
   }

   @Override
   public int getOwnerId() {
      return this._ownerId;
   }

   @Override
   public boolean isCaptain(Player player) {
      return this._captain != null && player == this._captain;
   }

   @Override
   public int getCaptainId() {
      return this._captain != null ? this._captain.getObjectId() : 0;
   }

   @Override
   public int getHelmObjectId() {
      return this._helmId;
   }

   @Override
   public int getHelmItemId() {
      return 13556;
   }

   @Override
   public boolean setCaptain(Player player) {
      if (player == null) {
         this._captain = null;
      } else {
         if (this._captain != null || player.getAirShip() != this) {
            return false;
         }

         int x = player.getInVehiclePosition().getX() - 366;
         int y = player.getInVehiclePosition().getY();
         int z = player.getInVehiclePosition().getZ() - 107;
         if (x * x + y * y + z * z > 2500) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_BECAUSE_YOU_ARE_TOO_FAR);
            return false;
         }

         if (player.isInCombat()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_BATTLE);
            return false;
         }

         if (player.isSitting()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_SITTING_POSITION);
            return false;
         }

         if (player.isParalyzed()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_YOU_ARE_PETRIFIED);
            return false;
         }

         if (player.isCursedWeaponEquipped()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
            return false;
         }

         if (player.isFishing()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_FISHING);
            return false;
         }

         if (player.isDead() || player.isFakeDeath()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHEN_YOU_ARE_DEAD);
            return false;
         }

         if (player.isCastingNow()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_USING_A_SKILL);
            return false;
         }

         if (player.isTransformed()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_TRANSFORMED);
            return false;
         }

         if (player.isCombatFlagEquipped()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_HOLDING_A_FLAG);
            return false;
         }

         if (player.isInDuel()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_DUEL);
            return false;
         }

         this._captain = player;
         player.broadcastUserInfo();
      }

      this.updateAbnormalEffect();
      return true;
   }

   @Override
   public int getFuel() {
      return this._fuel;
   }

   @Override
   public void setFuel(int f) {
      int old = this._fuel;
      if (f < 0) {
         this._fuel = 0;
      } else if (f > this._maxFuel) {
         this._fuel = this._maxFuel;
      } else {
         this._fuel = f;
      }

      if (this._fuel == 0 && old > 0) {
         this.broadcastToPassengers(
            new SystemMessage(SystemMessageId.THE_AIRSHIP_S_FUEL_EP_HAS_RUN_OUT_THE_AIRSHIP_S_SPEED_WILL_BE_GREATLY_DECREASED_IN_THIS_CONDITION)
         );
      } else if (this._fuel < 40) {
         this.broadcastToPassengers(new SystemMessage(SystemMessageId.THE_AIRSHIP_S_FUEL_EP_WILL_SOON_RUN_OUT));
      }
   }

   @Override
   public int getMaxFuel() {
      return this._maxFuel;
   }

   @Override
   public void setMaxFuel(int mf) {
      this._maxFuel = mf;
   }

   @Override
   public void oustPlayer(Player player) {
      if (player == this._captain) {
         this.setCaptain(null);
      }

      super.oustPlayer(player);
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._checkTask = ThreadPool.scheduleAtFixedRate(new ControllableAirShip.CheckTask(), 60000L, 10000L);
      this._consumeFuelTask = ThreadPool.scheduleAtFixedRate(new ControllableAirShip.ConsumeFuelTask(), 60000L, 60000L);
   }

   @Override
   public boolean deleteMe() {
      if (!super.deleteMe()) {
         return false;
      } else {
         if (this._checkTask != null) {
            this._checkTask.cancel(false);
            this._checkTask = null;
         }

         if (this._consumeFuelTask != null) {
            this._consumeFuelTask.cancel(false);
            this._consumeFuelTask = null;
         }

         this.broadcastPacket(new DeleteObject(this._helmId));
         return true;
      }
   }

   @Override
   public void refreshId() {
      super.refreshId();
      IdManager.getInstance().releaseId(this._helmId);
      this._helmId = IdManager.getInstance().getNextId();
   }

   @Override
   public void sendInfo(Player player) {
      super.sendInfo(player);
      if (this._captain != null) {
         this._captain.sendInfo(player);
      }
   }

   protected class CheckTask implements Runnable {
      protected CheckTask() {
         Objects.requireNonNull(ControllableAirShip.this);
         super();
      }

      @Override
      public void run() {
         if (ControllableAirShip.this.isSpawned() && ControllableAirShip.this.isEmpty() && !ControllableAirShip.this.isInDock()) {
            ThreadPool.execute(ControllableAirShip.this.new DecayTask());
         }
      }
   }

   protected class ConsumeFuelTask implements Runnable {
      protected ConsumeFuelTask() {
         Objects.requireNonNull(ControllableAirShip.this);
         super();
      }

      @Override
      public void run() {
         int fuel = ControllableAirShip.this.getFuel();
         if (fuel > 0) {
            fuel -= 10;
            if (fuel < 0) {
               fuel = 0;
            }

            ControllableAirShip.this.setFuel(fuel);
            ControllableAirShip.this.updateAbnormalEffect();
         }
      }
   }

   protected class DecayTask implements Runnable {
      protected DecayTask() {
         Objects.requireNonNull(ControllableAirShip.this);
         super();
      }

      @Override
      public void run() {
         ControllableAirShip.this.deleteMe();
      }
   }
}
