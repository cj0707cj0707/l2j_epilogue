package org.l2jmobius.gameserver.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.ActionClickHandler;
import org.l2jmobius.gameserver.handler.ActionShiftHandler;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.LocationUtil;

public abstract class WorldObject extends ListenersContainer implements IPositionable {
   private String _name;
   private int _objectId;
   private WorldRegion _worldRegion;
   private final Location _location = new Location(0, 0, -10000);
   private InstanceType _instanceType;
   private boolean _isSpawned;
   private boolean _isInvisible;
   private Map<String, Object> _scripts;

   public WorldObject(int objectId) {
      this.setInstanceType(InstanceType.WorldObject);
      this._objectId = objectId;
   }

   public InstanceType getInstanceType() {
      return this._instanceType;
   }

   protected void setInstanceType(InstanceType newInstanceType) {
      this._instanceType = newInstanceType;
   }

   public boolean isInstanceTypes(InstanceType... instanceTypes) {
      return this._instanceType.isTypes(instanceTypes);
   }

   public void onAction(Player player) {
      this.onAction(player, true);
   }

   public void onAction(Player player, boolean interact) {
      IActionClickHandler handler = ActionClickHandler.getInstance().getHandler(this.getInstanceType());
      if (handler != null) {
         handler.onAction(player, this, interact);
      }

      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   public void onActionShift(Player player) {
      IActionShiftHandler handler = ActionShiftHandler.getInstance().getHandler(this.getInstanceType());
      if (handler != null) {
         handler.onAction(player, this, true);
      }

      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   public void onForcedAttack(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
   }

   public void onSpawn() {
   }

   public boolean decayMe() {
      this._isSpawned = false;
      World.removeVisibleObject(this, this._worldRegion);
      World.removeObject(this);
      return true;
   }

   public void refreshId() {
      World.removeObject(this);
      IdManager.getInstance().releaseId(this.getObjectId());
      this._objectId = IdManager.getInstance().getNextId();
   }

   public boolean spawnMe() {
      synchronized (this) {
         this._isSpawned = true;
         this.setWorldRegion(World.getRegion(this));
         World.addObject(this);
         this._worldRegion.addVisibleObject(this);
      }

      World.addVisibleObject(this, this.getWorldRegion());
      this.onSpawn();
      return true;
   }

   public void spawnMe(int x, int y, int z) {
      synchronized (this) {
         int spawnX = x;
         if (x > 229376) {
            spawnX = 224376;
         }

         if (spawnX < -294912) {
            spawnX = -289912;
         }

         int spawnY = y;
         if (y > 294912) {
            spawnY = 289912;
         }

         if (spawnY < -262144) {
            spawnY = -257144;
         }

         this.setXYZ(spawnX, spawnY, z);
      }

      this.spawnMe();
   }

   public boolean canBeAttacked() {
      return false;
   }

   public abstract boolean isAutoAttackable(Creature var1);

   public boolean isSpawned() {
      return this._isSpawned;
   }

   public void setSpawned(boolean value) {
      this._isSpawned = value;
   }

   public String getName() {
      return this._name;
   }

   public void setName(String value) {
      this._name = value;
   }

   public int getId() {
      return 0;
   }

   public int getObjectId() {
      return this._objectId;
   }

   public abstract void sendInfo(Player var1);

   public void sendPacket(ServerPacket packet) {
   }

   public void sendPacket(SystemMessageId id) {
   }

   public Attackable asAttackable() {
      return null;
   }

   public Creature asCreature() {
      return null;
   }

   public Door asDoor() {
      return null;
   }

   public Monster asMonster() {
      return null;
   }

   public Npc asNpc() {
      return null;
   }

   public Pet asPet() {
      return null;
   }

   public Playable asPlayable() {
      return null;
   }

   public Player asPlayer() {
      return null;
   }

   public Servitor asServitor() {
      return null;
   }

   public Summon asSummon() {
      return null;
   }

   public boolean isArtefact() {
      return false;
   }

   public boolean isAttackable() {
      return false;
   }

   public boolean isCreature() {
      return false;
   }

   public boolean isCubic() {
      return false;
   }

   public boolean isDoor() {
      return false;
   }

   public boolean isFakePlayer() {
      return false;
   }

   public boolean isFence() {
      return false;
   }

   public boolean isItem() {
      return false;
   }

   public boolean isMonster() {
      return false;
   }

   public boolean isNpc() {
      return false;
   }

   public boolean isPet() {
      return false;
   }

   public boolean isPlayable() {
      return false;
   }

   public boolean isPlayer() {
      return false;
   }

   public boolean isServitor() {
      return false;
   }

   public boolean isSummon() {
      return false;
   }

   public boolean isTrap() {
      return false;
   }

   public boolean isVehicle() {
      return false;
   }

   public boolean isWalker() {
      return false;
   }

   public boolean isTargetable() {
      return true;
   }

   public boolean isInsideZone(ZoneId zone) {
      return false;
   }

   public boolean isChargedShot(ShotType type) {
      return false;
   }

   public void setChargedShot(ShotType type, boolean charged) {
   }

   public void rechargeShots(boolean physical, boolean magical) {
   }

   public <T> T addScript(T script) {
      if (this._scripts == null) {
         synchronized (this) {
            if (this._scripts == null) {
               this._scripts = new ConcurrentHashMap<>();
            }
         }
      }

      this._scripts.put(script.getClass().getName(), script);
      return script;
   }

   public <T> T removeScript(Class<T> script) {
      return (T)(this._scripts == null ? null : this._scripts.remove(script.getName()));
   }

   public <T> T getScript(Class<T> script) {
      return (T)(this._scripts == null ? null : this._scripts.get(script.getName()));
   }

   public void removeStatusListener(Creature object) {
   }

   public void setXYZInvisible(int x, int y, int z) {
      int correctX = x;
      if (x > 229376) {
         correctX = 224376;
      }

      if (correctX < -294912) {
         correctX = -289912;
      }

      int correctY = y;
      if (y > 294912) {
         correctY = 289912;
      }

      if (correctY < -262144) {
         correctY = -257144;
      }

      this.setXYZ(correctX, correctY, z);
      this.setSpawned(false);
   }

   public void setLocationInvisible(ILocational loc) {
      this.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
   }

   public WorldRegion getWorldRegion() {
      return this._worldRegion;
   }

   public void setWorldRegion(WorldRegion region) {
      if (region == null && this._worldRegion != null) {
         this._worldRegion.removeVisibleObject(this);
      }

      this._worldRegion = region;
   }

   @Override
   public int getX() {
      return this._location.getX();
   }

   @Override
   public int getY() {
      return this._location.getY();
   }

   @Override
   public int getZ() {
      return this._location.getZ();
   }

   @Override
   public int getHeading() {
      return this._location.getHeading();
   }

   @Override
   public int getInstanceId() {
      return this._location.getInstanceId();
   }

   public boolean isInInstance() {
      return this._location.getInstanceId() > 0;
   }

   public Location getLocation() {
      return this._location;
   }

   @Override
   public void setXYZ(int newX, int newY, int newZ) {
      this._location.setXYZ(newX, newY, newZ);
      if (this._isSpawned) {
         WorldRegion newRegion = World.getRegion(this);
         if (newRegion != null && newRegion != this._worldRegion) {
            if (this._worldRegion != null) {
               this._worldRegion.removeVisibleObject(this);
            }

            newRegion.addVisibleObject(this);
            World.switchRegion(this, newRegion);
            this.setWorldRegion(newRegion);
         }
      }
   }

   @Override
   public void setXYZ(ILocational loc) {
      this.setXYZ(loc.getX(), loc.getY(), loc.getZ());
   }

   @Override
   public void setHeading(int newHeading) {
      this._location.setHeading(newHeading);
   }

   @Override
   public void setInstanceId(int instanceId) {
      int oldInstanceId = this.getInstanceId();
      if (instanceId >= 0 && oldInstanceId != instanceId) {
         Instance newI = InstanceManager.getInstance().getInstance(instanceId);
         if (newI != null) {
            Instance oldI = InstanceManager.getInstance().getInstance(oldInstanceId);
            if (this.isPlayer()) {
               Player player = this.asPlayer();
               if (oldInstanceId > 0 && oldI != null) {
                  oldI.removePlayer(this._objectId);
                  if (oldI.isShowTimer()) {
                     this.sendInstanceUpdate(oldI, true);
                  }
               }

               if (instanceId > 0) {
                  newI.addPlayer(this._objectId);
                  if (newI.isShowTimer()) {
                     this.sendInstanceUpdate(newI, false);
                  }
               }

               if (player.hasSummon()) {
                  player.getSummon().setInstanceId(instanceId);
               }
            } else if (this.isNpc()) {
               Npc npc = this.asNpc();
               if (oldInstanceId > 0 && oldI != null) {
                  oldI.removeNpc(npc);
               }

               if (instanceId > 0) {
                  newI.addNpc(npc);
               }
            }

            this._location.setInstanceId(instanceId);
         }
      }
   }

   private void sendInstanceUpdate(Instance instance, boolean hide) {
      int startTime = (int)((System.currentTimeMillis() - instance.getInstanceStartTime()) / 1000L);
      int endTime = (int)((instance.getInstanceEndTime() - instance.getInstanceStartTime()) / 1000L);
      if (instance.isTimerIncrease()) {
         this.sendPacket(new ExSendUIEvent(this.asPlayer(), hide, true, startTime, endTime, instance.getTimerText()));
      } else {
         this.sendPacket(new ExSendUIEvent(this.asPlayer(), hide, false, endTime - startTime, 0, instance.getTimerText()));
      }
   }

   @Override
   public void setLocation(Location loc) {
      this._location.setXYZ(loc.getX(), loc.getY(), loc.getZ());
      this._location.setHeading(loc.getHeading());
      this._location.setInstanceId(loc.getInstanceId());
   }

   public double calculateDistance2D(int x, int y, int z) {
      double dx = x - this.getX();
      double dy = y - this.getY();
      return Math.sqrt(dx * dx + dy * dy);
   }

   public double calculateDistance2D(ILocational loc) {
      return loc == null ? Double.MAX_VALUE : this.calculateDistance2D(loc.getX(), loc.getY(), loc.getZ());
   }

   public double calculateDistance3D(int x, int y, int z) {
      double dx = x - this.getX();
      double dy = y - this.getY();
      double dz = z - this.getZ();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public double calculateDistance3D(ILocational loc) {
      return loc == null ? Double.MAX_VALUE : this.calculateDistance3D(loc.getX(), loc.getY(), loc.getZ());
   }

   public double calculateDirectionTo(ILocational target) {
      return LocationUtil.calculateAngleFrom(this, target);
   }

   public boolean isInvisible() {
      return this._isInvisible;
   }

   public void setInvisible(boolean invisible) {
      this._isInvisible = invisible;
      if (invisible) {
         DeleteObject deletePacket = new DeleteObject(this);
         World.forEachVisibleObject(this, Player.class, player -> {
            if (!this.isVisibleFor(player)) {
               player.sendPacket(deletePacket);
            }
         });
      }

      this.broadcastInfo();
   }

   public boolean isVisibleFor(Player player) {
      return !this._isInvisible || player.isGM();
   }

   public void broadcastInfo() {
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            this.sendInfo(player);
         }
      });
   }

   public boolean isInSurroundingRegion(WorldObject worldObject) {
      if (worldObject == null) {
         return false;
      } else {
         WorldRegion worldRegion = worldObject.getWorldRegion();
         if (worldRegion == null) {
            return false;
         } else {
            return this._worldRegion == null ? false : worldRegion.isSurroundingRegion(this._worldRegion);
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof WorldObject && ((WorldObject)obj).getObjectId() == this.getObjectId();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getClass().getSimpleName());
      sb.append(':');
      sb.append(this._name);
      sb.append('[');
      sb.append(this._objectId);
      sb.append(']');
      return sb.toString();
   }
}
