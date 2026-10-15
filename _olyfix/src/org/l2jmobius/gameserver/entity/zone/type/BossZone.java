package org.l2jmobius.gameserver.entity.zone.type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;

public class BossZone extends ZoneType {
   private int _timeInvade;
   private int[] _oustLoc = new int[]{0, 0, 0};

   public BossZone(int id) {
      super(id);
      this._oustLoc = new int[3];
      AbstractZoneSettings settings = ZoneManager.getSettings(this.getName());
      if (settings == null) {
         settings = new BossZone.Settings();
      }

      this.setSettings(settings);
      GrandBossManager.getInstance().addZone(this);
   }

   public BossZone.Settings getSettings() {
      return (BossZone.Settings)super.getSettings();
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("InvadeTime")) {
         this._timeInvade = Integer.parseInt(value);
      } else if (name.equals("oustX")) {
         this._oustLoc[0] = Integer.parseInt(value);
      } else if (name.equals("oustY")) {
         this._oustLoc[1] = Integer.parseInt(value);
      } else if (name.equals("oustZ")) {
         this._oustLoc[2] = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.isGM()) {
            return;
         }

         if (this.getSettings().getPlayersAllowed().contains(player.getObjectId())) {
            Long expirationTime = this.getSettings().getPlayerAllowedReEntryTimes().get(player.getObjectId());
            if (expirationTime == null) {
               if (GameServer.getStartTime() > System.currentTimeMillis() - this._timeInvade) {
                  return;
               }
            } else {
               this.getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
               if (expirationTime > System.currentTimeMillis()) {
                  return;
               }
            }

            this.getSettings().getPlayersAllowed().remove(this.getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
         }

         if (this._oustLoc[0] != 0 && this._oustLoc[1] != 0 && this._oustLoc[2] != 0) {
            player.teleToLocation(this._oustLoc[0], this._oustLoc[1], this._oustLoc[2]);
         } else {
            player.teleToLocation(TeleportWhereType.TOWN);
         }
      } else if (creature.isSummon()) {
         Player playerx = creature.asPlayer();
         if (playerx != null) {
            if (this.getSettings().getPlayersAllowed().contains(playerx.getObjectId()) || playerx.isGM()) {
               return;
            }

            if (this._oustLoc[0] != 0 && this._oustLoc[1] != 0 && this._oustLoc[2] != 0) {
               playerx.teleToLocation(this._oustLoc[0], this._oustLoc[1], this._oustLoc[2]);
            } else {
               playerx.teleToLocation(TeleportWhereType.TOWN);
            }
         }

         creature.asSummon().unSummon(playerx);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.isGM()) {
            return;
         }

         if (!player.isOnline() && this.getSettings().getPlayersAllowed().contains(player.getObjectId())) {
            this.getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + this._timeInvade);
         } else {
            if (this.getSettings().getPlayersAllowed().contains(player.getObjectId())) {
               this.getSettings().getPlayersAllowed().remove(this.getSettings().getPlayersAllowed().indexOf(player.getObjectId()));
            }

            this.getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
         }
      }

      if (creature.isPlayable() && this.getCharactersInside() != null && !this.getCharactersInside().isEmpty()) {
         this.getSettings().getRaidList().clear();
         int count = 0;

         for (Creature obj : this.getCharactersInside()) {
            if (obj != null) {
               if (obj.isPlayable()) {
                  count++;
               } else if (obj.isAttackable() && obj.isRaid()) {
                  this.getSettings().getRaidList().add(obj);
               }
            }
         }

         if (count == 0 && !this.getSettings().getRaidList().isEmpty()) {
            for (int i = 0; i < this.getSettings().getRaidList().size(); i++) {
               Attackable raid = this.getSettings().getRaidList().get(i).asAttackable();
               if (raid != null && raid.getSpawn() != null && !raid.isDead() && !raid.isInsideRadius2D(raid.getSpawn(), 150)) {
                  raid.returnHome();
               }
            }
         }
      }

      if (creature.isAttackable() && creature.isRaid() && !creature.isDead()) {
         creature.asAttackable().returnHome();
      }
   }

   @Override
   public void setEnabled(boolean flag) {
      if (this.isEnabled() != flag) {
         this.oustAllPlayers();
      }

      super.setEnabled(flag);
   }

   public int getTimeInvade() {
      return this._timeInvade;
   }

   public void setAllowedPlayers(List<Integer> players) {
      if (players != null) {
         this.getSettings().getPlayersAllowed().clear();
         this.getSettings().getPlayersAllowed().addAll(players);
      }
   }

   public List<Integer> getAllowedPlayers() {
      return this.getSettings().getPlayersAllowed();
   }

   public boolean isPlayerAllowed(Player player) {
      if (!player.isGM() && !this.getSettings().getPlayersAllowed().contains(player.getObjectId())) {
         if (this._oustLoc[0] != 0 && this._oustLoc[1] != 0 && this._oustLoc[2] != 0) {
            player.teleToLocation(this._oustLoc[0], this._oustLoc[1], this._oustLoc[2]);
         } else {
            player.teleToLocation(TeleportWhereType.TOWN);
         }

         return false;
      } else {
         return true;
      }
   }

   public void movePlayersTo(Location loc) {
      for (Creature creature : this.getCharactersInside()) {
         if (creature != null && creature.isPlayer()) {
            Player player = creature.asPlayer();
            if (player.isOnline()) {
               player.teleToLocation(loc);
            }
         }
      }
   }

   @Override
   public void oustAllPlayers() {
      if (!this.getCharactersInside().isEmpty()) {
         for (Creature creature : this.getCharactersInside()) {
            if (creature != null && creature.isPlayer()) {
               Player player = creature.asPlayer();
               if (player.isOnline()) {
                  if (this._oustLoc[0] != 0 && this._oustLoc[1] != 0 && this._oustLoc[2] != 0) {
                     player.teleToLocation(this._oustLoc[0], this._oustLoc[1], this._oustLoc[2]);
                  } else {
                     player.teleToLocation(TeleportWhereType.TOWN);
                  }
               }
            }
         }

         this.getSettings().getPlayerAllowedReEntryTimes().clear();
         this.getSettings().getPlayersAllowed().clear();
      }
   }

   public void allowPlayerEntry(Player player, int durationInSec) {
      if (!player.isGM()) {
         if (!this.getSettings().getPlayersAllowed().contains(player.getObjectId())) {
            this.getSettings().getPlayersAllowed().add(player.getObjectId());
         }

         this.getSettings().getPlayerAllowedReEntryTimes().put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
      }
   }

   public void removePlayer(Player player) {
      if (!player.isGM()) {
         this.getSettings().getPlayersAllowed().remove(Integer.valueOf(player.getObjectId()));
         this.getSettings().getPlayerAllowedReEntryTimes().remove(player.getObjectId());
      }
   }

   public class Settings extends AbstractZoneSettings {
      private final Map<Integer, Long> _playerAllowedReEntryTimes;
      private final List<Integer> _playersAllowed;
      private final List<Creature> _raidList;

      protected Settings() {
         Objects.requireNonNull(BossZone.this);
         super();
         this._playerAllowedReEntryTimes = new ConcurrentHashMap<>();
         this._playersAllowed = new CopyOnWriteArrayList<>();
         this._raidList = new CopyOnWriteArrayList<>();
      }

      public Map<Integer, Long> getPlayerAllowedReEntryTimes() {
         return this._playerAllowedReEntryTimes;
      }

      public List<Integer> getPlayersAllowed() {
         return this._playersAllowed;
      }

      public List<Creature> getRaidList() {
         return this._raidList;
      }

      @Override
      public void clear() {
         this._playerAllowedReEntryTimes.clear();
         this._playersAllowed.clear();
         this._raidList.clear();
      }
   }
}
