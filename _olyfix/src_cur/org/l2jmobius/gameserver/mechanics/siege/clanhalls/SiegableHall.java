package org.l2jmobius.gameserver.mechanics.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.type.SiegableHallZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClanType;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.util.StatSet;

public class SiegableHall extends ClanHall {
   private static final String SQL_SAVE = "UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE clanHallId=?";
   private Calendar _nextSiege;
   private final long _siegeLength;
   private final int[] _scheduleConfig = new int[]{7, 0, 0, 12, 0};
   private SiegeStatus _status = SiegeStatus.REGISTERING;
   private SiegeZone _siegeZone;
   private ClanHallSiegeEngine _siege;

   public SiegableHall(StatSet set) {
      super(set);
      this._siegeLength = set.getLong("siegeLength");
      String[] rawSchConfig = set.getString("scheduleConfig").split(";");
      if (rawSchConfig.length == 5) {
         for (int i = 0; i < 5; i++) {
            try {
               this._scheduleConfig[i] = Integer.parseInt(rawSchConfig[i]);
            } catch (Exception var5) {
               LOGGER.warning("SiegableHall - " + this.getName() + ": Wrong schedule_config parameters!");
            }
         }
      } else {
         LOGGER.warning(this.getName() + ": Wrong schedule_config value in siegable_halls table, using default (7 days)");
      }

      this._nextSiege = Calendar.getInstance();
      long nextSiege = set.getLong("nextSiege");
      if (nextSiege - System.currentTimeMillis() - 3600000L < 0L) {
         this.updateNextSiege();
      } else {
         this._nextSiege.setTimeInMillis(nextSiege);
      }

      if (this.getOwnerId() != 0) {
         this._isFree = false;
         this.loadFunctions();
      }
   }

   public void spawnDoor() {
      this.spawnDoor(false);
   }

   public void spawnDoor(boolean isDoorWeak) {
      for (Door door : this.getDoors()) {
         if (door.isDead()) {
            door.doRevive();
            if (isDoorWeak) {
               door.setCurrentHp(door.getMaxHp() / 2);
            } else {
               door.setCurrentHp(door.getMaxHp());
            }
         }

         if (door.isOpen()) {
            door.closeMe();
         }
      }
   }

   @Override
   public void updateDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE clanHallId=?");
      ) {
         ps.setInt(1, this.getOwnerId());
         ps.setLong(2, this.getNextSiegeTime());
         ps.setInt(3, this.getId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: SiegableHall.updateDb(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void setSiege(ClanHallSiegeEngine siegable) {
      this._siege = siegable;
      this._siegeZone.setSiegeInstance(siegable);
   }

   public ClanHallSiegeEngine getSiege() {
      return this._siege;
   }

   public Calendar getSiegeDate() {
      return this._nextSiege;
   }

   public long getNextSiegeTime() {
      return this._nextSiege.getTimeInMillis();
   }

   public long getSiegeLength() {
      return this._siegeLength;
   }

   public void setNextSiegeDate(long date) {
      this._nextSiege.setTimeInMillis(date);
   }

   public void setNextSiegeDate(Calendar c) {
      this._nextSiege = c;
   }

   public void updateNextSiege() {
      Calendar callendar = Calendar.getInstance();
      callendar.add(6, this._scheduleConfig[0]);
      callendar.add(2, this._scheduleConfig[1]);
      callendar.add(1, this._scheduleConfig[2]);
      callendar.set(11, this._scheduleConfig[3]);
      callendar.set(12, this._scheduleConfig[4]);
      callendar.set(13, 0);
      this.setNextSiegeDate(callendar);
      this.updateDb();
   }

   public void addAttacker(Clan clan) {
      if (this._siege != null) {
         this._siege.getAttackers().put(clan.getId(), new SiegeClan(clan.getId(), SiegeClanType.ATTACKER));
      }
   }

   public void removeAttacker(Clan clan) {
      if (this._siege != null) {
         this._siege.getAttackers().remove(clan.getId());
      }
   }

   public boolean isRegistered(Clan clan) {
      return this._siege != null && this._siege.checkIsAttacker(clan);
   }

   public SiegeStatus getSiegeStatus() {
      return this._status;
   }

   public boolean isRegistering() {
      return this._status == SiegeStatus.REGISTERING;
   }

   public boolean isInSiege() {
      return this._status == SiegeStatus.RUNNING;
   }

   public boolean isWaitingBattle() {
      return this._status == SiegeStatus.WAITING_BATTLE;
   }

   public void updateSiegeStatus(SiegeStatus status) {
      this._status = status;
   }

   public SiegeZone getSiegeZone() {
      return this._siegeZone;
   }

   public void setSiegeZone(SiegeZone zone) {
      this._siegeZone = zone;
   }

   public void updateSiegeZone(boolean active) {
      this._siegeZone.setActive(active);
   }

   public void showSiegeInfo(Player player) {
      player.sendPacket(new SiegeInfo(this, player));
   }

   @Override
   public boolean isSiegableHall() {
      return true;
   }

   public SiegableHallZone getZone() {
      return (SiegableHallZone)super.getZone();
   }
}
