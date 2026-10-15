package org.l2jmobius.gameserver.entity.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneEnter;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureZoneExit;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public abstract class ZoneType extends ListenersContainer {
   protected static final Logger LOGGER = Logger.getLogger(ZoneType.class.getName());
   private final int _id;
   protected ZoneForm _zone;
   private final Map<Integer, Creature> _characterList = new ConcurrentHashMap<>();
   protected boolean _checkAffected = false;
   private String _name = null;
   private int _minLevel;
   private int _maxLevel;
   private int[] _race;
   private int[] _class;
   private char _classType;
   private InstanceType _target = InstanceType.Creature;
   private boolean _allowStore;
   private boolean _enabled;
   private AbstractZoneSettings _settings;
   private int _instanceTemplateId = -1;
   private int _instanceId = -1;

   protected ZoneType(int id) {
      this._id = id;
      this._minLevel = 0;
      this._maxLevel = 255;
      this._classType = 0;
      this._race = null;
      this._class = null;
      this._allowStore = true;
      this._enabled = true;
   }

   public int getId() {
      return this._id;
   }

   public void setParameter(String name, String value) {
      this._checkAffected = true;
      if (name.equals("name")) {
         this._name = value;
      } else if (name.equals("affectedLvlMin")) {
         this._minLevel = Integer.parseInt(value);
      } else if (name.equals("affectedLvlMax")) {
         this._maxLevel = Integer.parseInt(value);
      } else if (name.equals("affectedRace")) {
         if (this._race == null) {
            this._race = new int[1];
            this._race[0] = Integer.parseInt(value);
         } else {
            int[] temp = new int[this._race.length + 1];

            int i;
            for (i = 0; i < this._race.length; i++) {
               temp[i] = this._race[i];
            }

            temp[i] = Integer.parseInt(value);
            this._race = temp;
         }
      } else if (name.equals("affectedClassId")) {
         if (this._class == null) {
            this._class = new int[1];
            this._class[0] = Integer.parseInt(value);
         } else {
            int[] temp = new int[this._class.length + 1];

            int i;
            for (i = 0; i < this._class.length; i++) {
               temp[i] = this._class[i];
            }

            temp[i] = Integer.parseInt(value);
            this._class = temp;
         }
      } else if (name.equals("affectedClassType")) {
         if (value.equals("Fighter")) {
            this._classType = 1;
         } else {
            this._classType = 2;
         }
      } else if (name.equals("targetClass")) {
         this._target = Enum.valueOf(InstanceType.class, value);
      } else if (name.equals("allowStore")) {
         this._allowStore = Boolean.parseBoolean(value);
      } else if (name.equals("default_enabled")) {
         this._enabled = Boolean.parseBoolean(value);
      } else if (name.equals("instanceId")) {
         this._instanceTemplateId = Integer.parseInt(value);
      } else {
         LOGGER.info(this.getClass().getSimpleName() + ": Unknown parameter - " + name + " in zone: " + this._id);
      }
   }

   protected boolean isAffected(Creature creature) {
      if (this._instanceTemplateId > 0) {
         InstanceWorld world = InstanceManager.getInstance().getWorld(creature);
         if (world != null && world.getTemplateId() != this._instanceTemplateId) {
            return false;
         }
      }

      if (!this.isEnabled()) {
         return false;
      } else if (creature.getLevel() < this._minLevel || creature.getLevel() > this._maxLevel) {
         return false;
      } else if (!creature.isInstanceTypes(new InstanceType[]{this._target})) {
         return false;
      } else {
         if (creature.isPlayer()) {
            if (this._classType != 0) {
               if (creature.asPlayer().isMageClass()) {
                  if (this._classType == 1) {
                     return false;
                  }
               } else if (this._classType == 2) {
                  return false;
               }
            }

            if (this._race != null) {
               boolean ok = false;

               for (int element : this._race) {
                  if (creature.getRace().ordinal() == element) {
                     ok = true;
                     break;
                  }
               }

               if (!ok) {
                  return false;
               }
            }

            if (this._class != null) {
               boolean ok = false;

               for (int _clas : this._class) {
                  if (creature.asPlayer().getPlayerClass().getId() == _clas) {
                     ok = true;
                     break;
                  }
               }

               if (!ok) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public void setZone(ZoneForm zone) {
      if (this._zone != null) {
         throw new IllegalStateException("Zone already set");
      } else {
         this._zone = zone;
      }
   }

   public ZoneForm getZone() {
      return this._zone;
   }

   public void setName(String name) {
      this._name = name;
   }

   public String getName() {
      return this._name;
   }

   public void setInstanceId(int instanceId) {
      this._instanceId = instanceId;
   }

   public int getInstanceId() {
      return this._instanceId;
   }

   public void setInstanceTemplateId(int instanceTemplateId) {
      this._instanceTemplateId = instanceTemplateId;
   }

   public int getInstanceTemplateId() {
      return this._instanceTemplateId;
   }

   public boolean isInsideZone(int x, int y) {
      return this.isInsideZone(x, y, this._zone.getHighZ());
   }

   public boolean isInsideZone(ILocational loc) {
      return this.isInsideZone(loc.getX(), loc.getY(), loc.getZ());
   }

   public boolean isInsideZone(int x, int y, int z) {
      return this._zone.isInsideZone(x, y, z);
   }

   public double getDistanceToZone(int x, int y) {
      return this._zone.getDistanceToZone(x, y);
   }

   public double getDistanceToZone(WorldObject object) {
      return this._zone.getDistanceToZone(object.getX(), object.getY());
   }

   public void revalidateInZone(Creature creature) {
      if (this.isInsideZone(creature)) {
         if (this._checkAffected && !this.isAffected(creature)) {
            return;
         }

         if (this._characterList.putIfAbsent(creature.getObjectId(), creature) == null) {
            if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ZONE_ENTER, this)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnCreatureZoneEnter(creature, this), this);
            }

            this.onEnter(creature);
         }
      } else {
         this.removeCharacter(creature);
      }
   }

   public void removeCharacter(Creature creature) {
      if (this._characterList.containsKey(creature.getObjectId())) {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ZONE_EXIT, this)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnCreatureZoneExit(creature, this), this);
         }

         this._characterList.remove(creature.getObjectId());
         this.onExit(creature);
      }
   }

   public boolean isCharacterInZone(Creature creature) {
      return this._characterList.containsKey(creature.getObjectId());
   }

   public AbstractZoneSettings getSettings() {
      return this._settings;
   }

   public void setSettings(AbstractZoneSettings settings) {
      if (this._settings != null) {
         this._settings.clear();
      }

      this._settings = settings;
   }

   protected abstract void onEnter(Creature var1);

   protected abstract void onExit(Creature var1);

   public void onDieInside(Creature creature) {
   }

   public void onReviveInside(Creature creature) {
   }

   public void onPlayerLoginInside(Player player) {
   }

   public void onPlayerLogoutInside(Player player) {
   }

   public Collection<Creature> getCharactersInside() {
      return this._characterList.values();
   }

   public List<Player> getPlayersInside() {
      List<Player> players = new ArrayList<>();

      for (Creature ch : this._characterList.values()) {
         if (ch != null && ch.isPlayer()) {
            players.add(ch.asPlayer());
         }
      }

      return players;
   }

   public void broadcastPacket(ServerPacket packet) {
      if (!this._characterList.isEmpty()) {
         for (Creature creature : this._characterList.values()) {
            if (creature != null && creature.isPlayer()) {
               creature.sendPacket(packet);
            }
         }
      }
   }

   public InstanceType getTargetType() {
      return this._target;
   }

   public void setTargetType(InstanceType type) {
      this._target = type;
      this._checkAffected = true;
   }

   public boolean getAllowStore() {
      return this._allowStore;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + "[" + this._id + "]";
   }

   public void visualizeZone(int z) {
      this._zone.visualizeZone(z);
   }

   public void setEnabled(boolean value) {
      this._enabled = value;
   }

   public boolean isEnabled() {
      return this._enabled;
   }

   public void oustAllPlayers() {
      for (Creature obj : this._characterList.values()) {
         if (obj != null && obj.isPlayer()) {
            Player player = obj.asPlayer();
            if (player.isOnline()) {
               player.teleToLocation(TeleportWhereType.TOWN);
            }
         }
      }
   }
}
