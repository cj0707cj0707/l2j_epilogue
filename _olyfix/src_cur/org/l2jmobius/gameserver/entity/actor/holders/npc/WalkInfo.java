package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.concurrent.ScheduledFuture;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveRouteFinished;

public class WalkInfo {
   private static final int FLAG_BLOCKED = 1;
   private static final int FLAG_SUSPENDED = 2;
   private static final int FLAG_STOPPED_BY_ATTACK = 4;
   private static final int STEP_FORWARD = 1;
   private static final int NODE_FIRST = 0;
   private static final int NODE_SECOND = 1;
   private final String _routeName;
   private ScheduledFuture<?> _walkCheckTask;
   private int _stateFlags;
   private int _cursor;
   private int _step;
   private long _lastActionTime;

   public WalkInfo(String routeName) {
      this._routeName = routeName;
      this._walkCheckTask = null;
      this._stateFlags = 0;
      this._cursor = 0;
      this._step = 1;
      this._lastActionTime = 0L;
   }

   public boolean isBlocked() {
      return (this._stateFlags & 1) != 0;
   }

   public void setBlocked(boolean value) {
      int flags = this._stateFlags;
      if (value) {
         flags |= 1;
      } else {
         flags &= -2;
      }

      this._stateFlags = flags;
   }

   public boolean isSuspended() {
      return (this._stateFlags & 2) != 0;
   }

   public void setSuspended(boolean value) {
      int flags = this._stateFlags;
      if (value) {
         flags |= 2;
      } else {
         flags &= -3;
      }

      this._stateFlags = flags;
   }

   public boolean isStoppedByAttack() {
      return (this._stateFlags & 4) != 0;
   }

   public void setStoppedByAttack(boolean value) {
      int flags = this._stateFlags;
      if (value) {
         flags |= 4;
      } else {
         flags &= -5;
      }

      this._stateFlags = flags;
   }

   public int getCurrentNodeId() {
      return this._cursor;
   }

   public long getLastAction() {
      return this._lastActionTime;
   }

   public void setLastAction(long value) {
      this._lastActionTime = value;
   }

   public ScheduledFuture<?> getWalkCheckTask() {
      return this._walkCheckTask;
   }

   public void setWalkCheckTask(ScheduledFuture<?> task) {
      this._walkCheckTask = task;
   }

   public WalkRoute getRoute() {
      WalkingManager manager = WalkingManager.getInstance();
      return manager.getRoute(this._routeName);
   }

   public WalkNode getCurrentNode() {
      WalkRoute route = this.getRoute();
      int cursor = this._cursor;
      if (cursor < 0) {
         cursor = 0;
      }

      int size = route.getNodeList().size();
      int last = size - 1;
      if (cursor > last) {
         cursor = last;
      }

      return route.getNodeList().get(cursor);
   }

   public synchronized void calculateNextNode(Npc npc) {
      WalkRoute route = this.getRoute();
      int nodesCount = route.getNodesCount();
      byte repeatType = route.getRepeatType();
      if (repeatType == 3) {
         if (nodesCount <= 1) {
            this._cursor = 0;
         } else {
            int base = this._cursor;
            int selected = base;

            while (selected == base) {
               int candidate = Rnd.get(nodesCount);
               if (candidate != base) {
                  selected = candidate;
               }
            }

            this._cursor = selected;
         }
      } else {
         int cursor = this._cursor;
         cursor += this._step;
         this._cursor = cursor;
         if (this._cursor == nodesCount) {
            EventDispatcher dispatcher = EventDispatcher.getInstance();
            if (dispatcher.hasListener(EventType.ON_NPC_MOVE_ROUTE_FINISHED, npc)) {
               dispatcher.notifyEventAsync(new OnNpcMoveRouteFinished(npc), npc);
            }

            if (!route.repeatWalk()) {
               WalkingManager.getInstance().cancelMoving(npc);
            } else {
               switch (repeatType) {
                  case 0:
                     this._step = -1;
                     this._cursor = nodesCount - 2;
                     break;
                  case 1:
                     this._step = 1;
                     this._cursor = 0;
                     break;
                  case 2:
                     npc.teleToLocation(npc.getSpawn().getLocation());
                     this._step = 1;
                     this._cursor = 0;
               }
            }
         } else {
            if (this._cursor == -1) {
               this._cursor = 1;
               this._step = 1;
            }
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(220);
      sb.append("WalkInfo [routeName=");
      sb.append(this._routeName);
      sb.append(", walkCheckTask=");
      sb.append(this._walkCheckTask);
      sb.append(", stateFlags=0x");
      sb.append(Integer.toHexString(this._stateFlags));
      sb.append(", cursor=");
      sb.append(this._cursor);
      sb.append(", step=");
      sb.append(this._step);
      sb.append(", blocked=");
      sb.append(this.isBlocked());
      sb.append(", suspended=");
      sb.append(this.isSuspended());
      sb.append(", stoppedByAttack=");
      sb.append(this.isStoppedByAttack());
      sb.append(", lastActionTime=");
      sb.append(this._lastActionTime);
      sb.append(']');
      return sb.toString();
   }
}
