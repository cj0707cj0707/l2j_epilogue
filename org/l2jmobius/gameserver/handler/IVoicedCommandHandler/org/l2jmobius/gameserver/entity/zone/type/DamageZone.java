package org.l2jmobius.gameserver.entity.zone.type;

import java.util.Objects;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class DamageZone extends ZoneType {
   private int _damageHPPerSec = 200;
   private int _damageMPPerSec = 0;
   private int _castleId;
   private Castle _castle;
   private int _startTask = 10;
   private int _reuseTask = 5000;
   protected volatile Future<?> _task;

   public DamageZone(int id) {
      super(id);
      this._castleId = 0;
      this._castle = null;
      this.setTargetType(InstanceType.Playable);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("dmgHPSec")) {
         this._damageHPPerSec = Integer.parseInt(value);
      } else if (name.equals("dmgMPSec")) {
         this._damageMPPerSec = Integer.parseInt(value);
      } else if (name.equals("castleId")) {
         this._castleId = Integer.parseInt(value);
      } else if (name.equalsIgnoreCase("initialDelay")) {
         this._startTask = Integer.parseInt(value);
      } else if (name.equalsIgnoreCase("reuse")) {
         this._reuseTask = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      Future<?> task = this._task;
      if (task == null && (this._damageHPPerSec != 0 || this._damageMPPerSec != 0)) {
         Player player = creature.asPlayer();
         Castle castle = this.getCastle();
         if (castle != null && (!castle.getSiege().isInProgress() || player == null || player.getSiegeState() == 2)) {
            return;
         }

         synchronized (this) {
            task = this._task;
            if (task == null) {
               this._task = ThreadPool.scheduleAtFixedRate(new DamageZone.ApplyDamage(), this._startTask, this._reuseTask);
            }
         }
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (this.getCharactersInside().isEmpty() && this._task != null) {
         this._task.cancel(true);
         this._task = null;
      }
   }

   protected int getHPDamagePerSecond() {
      return this._damageHPPerSec;
   }

   protected int getMPDamagePerSecond() {
      return this._damageMPPerSec;
   }

   protected Castle getCastle() {
      if (this._castleId > 0 && this._castle == null) {
         this._castle = CastleManager.getInstance().getCastleById(this._castleId);
      }

      return this._castle;
   }

   private class ApplyDamage implements Runnable {
      private final Castle _castle;

      protected ApplyDamage() {
         Objects.requireNonNull(DamageZone.this);
         super();
         this._castle = DamageZone.this.getCastle();
      }

      @Override
      public void run() {
         if (DamageZone.this.getCharactersInside().isEmpty()) {
            if (DamageZone.this._task != null) {
               DamageZone.this._task.cancel(false);
               DamageZone.this._task = null;
            }
         } else if (DamageZone.this.isEnabled()) {
            boolean siege = false;
            if (this._castle != null) {
               siege = this._castle.getSiege().isInProgress();
               if (!siege) {
                  DamageZone.this._task.cancel(false);
                  DamageZone.this._task = null;
                  return;
               }
            }

            for (Creature character : DamageZone.this.getCharactersInside()) {
               if (character != null && character.isPlayer() && !character.isDead()) {
                  if (siege) {
                     Player player = character.asPlayer();
                     if (player != null && player.isInSiege() && player.getSiegeState() == 2) {
                        continue;
                     }
                  }

                  double multiplier = 1.0 + character.calcStat(Stat.DAMAGE_ZONE_VULN, 0.0, null, null) / 100.0;
                  if (DamageZone.this.getHPDamagePerSecond() != 0) {
                     character.reduceCurrentHp(DamageZone.this.getHPDamagePerSecond() * multiplier, character, null);
                  }

                  if (DamageZone.this.getMPDamagePerSecond() != 0) {
                     character.reduceCurrentMp(DamageZone.this.getMPDamagePerSecond() * multiplier);
                  }
               }
            }
         }
      }
   }
}
