package org.l2jmobius.gameserver.entity.zone.type;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ResidenceHallTeleportZone extends ResidenceTeleportZone {
   private int _id;
   private ScheduledFuture<?> _teleTask;

   public ResidenceHallTeleportZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("residenceZoneId")) {
         this._id = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   public int getResidenceZoneId() {
      return this._id;
   }

   public synchronized void checkTeleportTask() {
      if (this._teleTask == null || this._teleTask.isDone()) {
         this._teleTask = ThreadPool.schedule(new ResidenceHallTeleportZone.TeleportTask(), 30000L);
      }
   }

   protected class TeleportTask implements Runnable {
      protected TeleportTask() {
         Objects.requireNonNull(ResidenceHallTeleportZone.this);
         super();
      }

      @Override
      public void run() {
         int index = ResidenceHallTeleportZone.this.getSpawns().size() > 1 ? Rnd.get(ResidenceHallTeleportZone.this.getSpawns().size()) : 0;
         Location loc = ResidenceHallTeleportZone.this.getSpawns().get(index);
         if (loc == null) {
            throw new NullPointerException();
         } else {
            for (Player pc : ResidenceHallTeleportZone.this.getPlayersInside()) {
               if (pc != null) {
                  pc.teleToLocation(loc, false);
               }
            }
         }
      }
   }
}
