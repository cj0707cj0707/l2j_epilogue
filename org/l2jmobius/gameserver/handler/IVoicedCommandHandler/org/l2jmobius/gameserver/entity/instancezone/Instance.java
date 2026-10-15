package org.l2jmobius.gameserver.entity.instancezone;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Instance {
   private static final Logger LOGGER = Logger.getLogger(Instance.class.getName());
   private final int _id;
   private int _templateId = -1;
   private int _ejectTime = GeneralConfig.EJECT_DEAD_PLAYER_TIME;
   private boolean _allowRandomWalk = true;
   private final Collection<Integer> _players = ConcurrentHashMap.newKeySet();
   private final Collection<Npc> _npcs = ConcurrentHashMap.newKeySet();
   private final Collection<StatSet> _doorTemplates = ConcurrentHashMap.newKeySet();
   private final Map<Integer, Door> _doors = new ConcurrentHashMap<>();
   private final Collection<StatSet> _spawnTemplates = ConcurrentHashMap.newKeySet();
   private Collection<Location> _enterLocations = ConcurrentHashMap.newKeySet();
   private Location _exitLocation = null;
   private boolean _allowSummon = true;
   private long _emptyDestroyTime = -1L;
   private long _lastLeft = -1L;
   private long _instanceStartTime = -1L;
   private long _instanceEndTime = -1L;
   private boolean _isPvP = false;
   private boolean _showTimer = false;
   private boolean _isTimerIncrease = true;
   private String _timerText = "";
   private InstanceReenterType _type = InstanceReenterType.NONE;
   private final List<InstanceReenterTimeHolder> _resetData = new ArrayList<>();
   private InstanceRemoveBuffType _removeBuffType = InstanceRemoveBuffType.NONE;
   private final List<Integer> _exceptionList = new ArrayList<>();
   protected ScheduledFuture<?> _checkTimeUpTask = null;
   protected final Map<Integer, ScheduledFuture<?>> _ejectDeadTasks = new ConcurrentHashMap<>();

   public Instance(int id) {
      this._id = id;
      this._instanceStartTime = System.currentTimeMillis();
   }

   public int getId() {
      return this._id;
   }

   public int getTemplateId() {
      return this._templateId;
   }

   public int getEjectTime() {
      return this._ejectTime;
   }

   public void setEjectTime(int ejectTime) {
      this._ejectTime = ejectTime;
   }

   public boolean isSummonAllowed() {
      return this._allowSummon;
   }

   public void setAllowSummon(boolean value) {
      this._allowSummon = value;
   }

   public boolean isPvP() {
      return this._isPvP;
   }

   public void setPvP(boolean value) {
      this._isPvP = value;
   }

   public void setDuration(int duration) {
      if (this._checkTimeUpTask != null) {
         this._checkTimeUpTask.cancel(true);
      }

      this._checkTimeUpTask = ThreadPool.schedule(new Instance.CheckTimeUp(duration), 500L);
      this._instanceEndTime = System.currentTimeMillis() + duration + 500L;
   }

   public void setEmptyDestroyTime(long time) {
      this._emptyDestroyTime = time;
   }

   public boolean containsPlayer(int objectId) {
      return this._players.contains(objectId);
   }

   public void addPlayer(int objectId) {
      this._players.add(objectId);
   }

   public void removePlayer(Integer objectId) {
      this._players.remove(objectId);
      if (this._players.isEmpty() && this._emptyDestroyTime >= 0L) {
         this._lastLeft = System.currentTimeMillis();
         this.setDuration((int)(this._instanceEndTime - System.currentTimeMillis() - 500L));
      }
   }

   public void addNpc(Npc npc) {
      this._npcs.add(npc);
   }

   public void removeNpc(Npc npc) {
      if (npc.getSpawn() != null) {
         npc.getSpawn().stopRespawn();
      }

      this._npcs.remove(npc);
   }

   public void addDoor(StatSet set) {
      if (this._doorTemplates.contains(set)) {
         LOGGER.warning("Door ID " + set.getInt("DoorId") + " already exists in instance " + this._id);
      } else {
         this._doorTemplates.add(set);
      }
   }

   public void spawnDoors() {
      for (StatSet template : this._doorTemplates) {
         int doorId = template.getInt("DoorId");
         StatSet doorTemplate = DoorData.getInstance().getDoorTemplate(doorId);
         Door newdoor = new Door(new DoorTemplate(doorTemplate));
         newdoor.setInstanceId(this._id);
         newdoor.setCurrentHp(newdoor.getMaxHp());
         newdoor.spawnMe(newdoor.getTemplate().getX(), newdoor.getTemplate().getY(), newdoor.getTemplate().getZ());
         this._doors.put(doorId, newdoor);
      }
   }

   public Collection<Integer> getPlayers() {
      return this._players;
   }

   public Collection<Npc> getNpcs() {
      return this._npcs;
   }

   public Collection<Door> getDoors() {
      return this._doors.values();
   }

   public Door getDoor(int id) {
      return this._doors.get(id);
   }

   public long getInstanceEndTime() {
      return this._instanceEndTime;
   }

   public long getInstanceStartTime() {
      return this._instanceStartTime;
   }

   public boolean isShowTimer() {
      return this._showTimer;
   }

   public boolean isTimerIncrease() {
      return this._isTimerIncrease;
   }

   public String getTimerText() {
      return this._timerText;
   }

   public Collection<Location> getEnterLocs() {
      return this._enterLocations;
   }

   public void addEnterLoc(Location loc) {
      this._enterLocations.add(loc);
   }

   public Location getExitLoc() {
      return this._exitLocation;
   }

   public void setExitLoc(Location loc) {
      this._exitLocation = loc;
   }

   public void removePlayers() {
      for (Integer objectId : this._players) {
         Player player = World.getPlayer(objectId);
         if (player != null && player.getInstanceId() == this._id) {
            player.setInstanceId(0);
            if (this._exitLocation != null) {
               player.teleToLocation(this._exitLocation, true);
            } else {
               player.teleToLocation(TeleportWhereType.TOWN);
            }
         }
      }

      this._players.clear();
   }

   public void removeNpcs() {
      for (Npc mob : this._npcs) {
         if (mob != null) {
            if (mob.getSpawn() != null) {
               mob.getSpawn().stopRespawn();
            }

            mob.deleteMe();
         }
      }

      this._npcs.clear();
   }

   public void removeDoors() {
      for (Door door : this._doors.values()) {
         if (door != null) {
            WorldRegion region = door.getWorldRegion();
            door.decayMe();
            if (region != null) {
               region.removeVisibleObject(door);
            }

            World.removeObject(door);
         }
      }

      this._doors.clear();
   }

   public List<Npc> spawnGroup(String groupName) {
      List<Npc> spawnedNpcs = new ArrayList<>();

      for (StatSet set : this._spawnTemplates) {
         if (set.getString("spawnGroup").equals(groupName)) {
            try {
               Spawn spawnDat = new Spawn(set.getInt("npcId"));
               spawnDat.setXYZ(set.getInt("x"), set.getInt("y"), set.getInt("z"));
               spawnDat.setAmount(1);
               spawnDat.setHeading(set.getInt("heading"));
               spawnDat.setRespawnDelay(set.getInt("respawn"), set.getInt("respawnRandom"));
               spawnDat.setInstanceId(this._id);
               spawnDat.setRandomWalking(set.getBoolean("allowRandomWalk"));
               Npc spawned = spawnDat.doSpawn(false);
               if (set.getInt("delay") >= 0 && spawned.isAttackable()) {
                  spawned.asAttackable().setOnKillDelay(set.getInt("delay"));
               }

               if (set.getInt("respawn") == 0) {
                  spawnDat.stopRespawn();
               } else {
                  spawnDat.startRespawn();
               }

               spawnedNpcs.add(spawned);
            } catch (Exception var7) {
               LOGGER.warning("Problem with Instance: " + var7.toString());
            }
         }
      }

      return spawnedNpcs;
   }

   public void loadInstanceTemplate(int templateId) {
      Document document = null;
      File xml = new File(
         "." + File.separator + "data" + File.separator + "instances" + File.separator + InstanceManager.getInstance().getInstanceTemplateFileName(templateId)
      );

      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringComments(true);
         document = factory.newDocumentBuilder().parse(xml);

         for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("instance".equalsIgnoreCase(n.getNodeName())) {
               this.parseInstance(n);
            }
         }
      } catch (IOException var6) {
         LOGGER.log(Level.WARNING, "Instance: can not find " + xml.getAbsolutePath() + " ! " + var6.getMessage(), (Throwable)var6);
      } catch (Exception var7) {
         LOGGER.log(Level.WARNING, "Instance: error while loading " + xml.getAbsolutePath() + " ! " + var7.getMessage(), (Throwable)var7);
      }
   }

   private void parseInstance(Node node) throws Exception {
      this._templateId = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
      Node a = node.getAttributes().getNamedItem("ejectTime");
      if (a != null) {
         this._ejectTime = 1000 * Integer.parseInt(a.getNodeValue());
      }

      a = node.getAttributes().getNamedItem("allowRandomWalk");
      label188:
      if (a != null) {
         this._allowRandomWalk = Boolean.parseBoolean(a.getNodeValue());
      }

      for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
         String var4 = n.getNodeName().toLowerCase();
         switch (var4) {
            case "activitytime":
               a = n.getAttributes().getNamedItem("val");
               if (a != null) {
                  this._checkTimeUpTask = ThreadPool.schedule(new Instance.CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000L);
                  this._instanceEndTime = System.currentTimeMillis() + Long.parseLong(a.getNodeValue()) * 60000L + 15000L;
               }
               break;
            case "allowsummon":
               a = n.getAttributes().getNamedItem("val");
               if (a != null) {
                  this.setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
               }
               break;
            case "emptydestroytime":
               a = n.getAttributes().getNamedItem("val");
               if (a != null) {
                  this._emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000L;
               }
               break;
            case "showtimer":
               a = n.getAttributes().getNamedItem("val");
               if (a != null) {
                  this._showTimer = Boolean.parseBoolean(a.getNodeValue());
               }

               a = n.getAttributes().getNamedItem("increase");
               if (a != null) {
                  this._isTimerIncrease = Boolean.parseBoolean(a.getNodeValue());
               }

               a = n.getAttributes().getNamedItem("text");
               if (a != null) {
                  this._timerText = a.getNodeValue();
               }
               break;
            case "pvpinstance":
               a = n.getAttributes().getNamedItem("val");
               if (a != null) {
                  this.setPvP(Boolean.parseBoolean(a.getNodeValue()));
               }
               break;
            case "doorlist":
               Node d = n.getFirstChild();

               for (; d != null; d = d.getNextSibling()) {
                  int doorId = 0;
                  if ("door".equalsIgnoreCase(d.getNodeName())) {
                     doorId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
                     StatSet set = new StatSet();
                     set.set("DoorId", doorId);

                     for (Node bean = d.getFirstChild(); bean != null; bean = bean.getNextSibling()) {
                        if ("set".equalsIgnoreCase(bean.getNodeName())) {
                           NamedNodeMap attrs = bean.getAttributes();
                           String setname = attrs.getNamedItem("name").getNodeValue();
                           String value = attrs.getNamedItem("val").getNodeValue();
                           set.set(setname, value);
                        }
                     }

                     this.addDoor(set);
                  }
               }
               break;
            case "spawnlist":
               for (Node group = n.getFirstChild(); group != null; group = group.getNextSibling()) {
                  if ("group".equalsIgnoreCase(group.getNodeName())) {
                     String spawnGroup = group.getAttributes().getNamedItem("name").getNodeValue();

                     for (Node dxx = group.getFirstChild(); dxx != null; dxx = dxx.getNextSibling()) {
                        int npcId = 0;
                        int xx = 0;
                        int yx = 0;
                        int zx = 0;
                        int heading = 0;
                        int respawn = 0;
                        int respawnRandom = 0;
                        int delay = -1;
                        Boolean allowRandomWalk = null;
                        if ("npc".equalsIgnoreCase(dxx.getNodeName())) {
                           npcId = Integer.parseInt(dxx.getAttributes().getNamedItem("id").getNodeValue());
                           xx = Integer.parseInt(dxx.getAttributes().getNamedItem("x").getNodeValue());
                           yx = Integer.parseInt(dxx.getAttributes().getNamedItem("y").getNodeValue());
                           zx = Integer.parseInt(dxx.getAttributes().getNamedItem("z").getNodeValue());
                           if (dxx.getAttributes().getNamedItem("heading") != null) {
                              heading = Integer.parseInt(dxx.getAttributes().getNamedItem("heading").getNodeValue());
                           }

                           if (dxx.getAttributes().getNamedItem("respawn") != null) {
                              respawn = Integer.parseInt(dxx.getAttributes().getNamedItem("respawn").getNodeValue());
                           }

                           if (dxx.getAttributes().getNamedItem("onKillDelay") != null) {
                              delay = Integer.parseInt(dxx.getAttributes().getNamedItem("onKillDelay").getNodeValue());
                           }

                           if (dxx.getAttributes().getNamedItem("respawnRandom") != null) {
                              respawnRandom = Integer.parseInt(dxx.getAttributes().getNamedItem("respawnRandom").getNodeValue());
                           }

                           if (dxx.getAttributes().getNamedItem("allowRandomWalk") != null) {
                              allowRandomWalk = Boolean.parseBoolean(dxx.getAttributes().getNamedItem("allowRandomWalk").getNodeValue());
                           }

                           StatSet spawnSet = new StatSet();
                           spawnSet.set("spawnGroup", spawnGroup);
                           spawnSet.set("npcId", npcId);
                           spawnSet.set("x", xx);
                           spawnSet.set("y", yx);
                           spawnSet.set("z", zx);
                           spawnSet.set("heading", heading);
                           spawnSet.set("delay", delay);
                           spawnSet.set("respawn", respawn);
                           spawnSet.set("respawnRandom", respawnRandom);
                           if (allowRandomWalk == null) {
                              spawnSet.set("allowRandomWalk", !this._allowRandomWalk);
                           } else {
                              spawnSet.set("allowRandomWalk", !allowRandomWalk);
                           }

                           this._spawnTemplates.add(spawnSet);
                        }
                     }
                  }
               }
               break label188;
            case "exitpoint":
               int x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
               int y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
               int z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
               this._exitLocation = new Location(x, y, z);
               break;
            case "spawnpoints":
               this._enterLocations = new ArrayList<>();

               for (Node loc = n.getFirstChild(); loc != null; loc = loc.getNextSibling()) {
                  if (loc.getNodeName().equals("Location")) {
                     try {
                        int xx = Integer.parseInt(loc.getAttributes().getNamedItem("x").getNodeValue());
                        int yx = Integer.parseInt(loc.getAttributes().getNamedItem("y").getNodeValue());
                        int zx = Integer.parseInt(loc.getAttributes().getNamedItem("z").getNodeValue());
                        this._enterLocations.add(new Location(xx, yx, zx));
                     } catch (Exception var19) {
                        LOGGER.log(Level.WARNING, "Error parsing instance xml: " + var19.getMessage(), (Throwable)var19);
                     }
                  }
               }
               break label188;
            case "reenter":
               a = n.getAttributes().getNamedItem("additionStyle");
               if (a != null) {
                  this._type = InstanceReenterType.valueOf(a.getNodeValue());
               }

               for (Node dx = n.getFirstChild(); dx != null; dx = dx.getNextSibling()) {
                  long time = -1L;
                  DayOfWeek day = null;
                  int hour = -1;
                  int minute = -1;
                  if ("reset".equalsIgnoreCase(dx.getNodeName())) {
                     a = dx.getAttributes().getNamedItem("time");
                     if (a != null) {
                        time = Long.parseLong(a.getNodeValue());
                        if (time > 0L) {
                           this._resetData.add(new InstanceReenterTimeHolder(time));
                           break;
                        }
                     } else if (time == -1L) {
                        a = dx.getAttributes().getNamedItem("day");
                        if (a != null) {
                           day = DayOfWeek.valueOf(a.getNodeValue().toUpperCase());
                        }

                        a = dx.getAttributes().getNamedItem("hour");
                        if (a != null) {
                           hour = Integer.parseInt(a.getNodeValue());
                        }

                        a = dx.getAttributes().getNamedItem("minute");
                        if (a != null) {
                           minute = Integer.parseInt(a.getNodeValue());
                        }

                        this._resetData.add(new InstanceReenterTimeHolder(day, hour, minute));
                     }
                  }
               }
               break label188;
            case "removebuffs":
               a = n.getAttributes().getNamedItem("type");
               if (a != null) {
                  this._removeBuffType = InstanceRemoveBuffType.valueOf(a.getNodeValue().toUpperCase());
               }

               for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                  if ("skill".equalsIgnoreCase(d.getNodeName())) {
                     a = d.getAttributes().getNamedItem("id");
                     if (a != null) {
                        this._exceptionList.add(Integer.parseInt(a.getNodeValue()));
                     }
                  }
               }
         }
      }
   }

   protected void doCheckTimeUp(int value) {
      CreatureSay cs = null;
      int interval;
      int remaining;
      if (this._players.isEmpty() && this._emptyDestroyTime == 0L) {
         remaining = 0;
         interval = 500;
      } else if (this._players.isEmpty() && this._emptyDestroyTime > 0L) {
         Long emptyTimeLeft = this._lastLeft + this._emptyDestroyTime - System.currentTimeMillis();
         if (emptyTimeLeft <= 0L) {
            interval = 0;
            remaining = 0;
         } else if (value > 300000 && emptyTimeLeft > 300000L) {
            interval = 300000;
            remaining = value - 300000;
         } else if (value > 60000 && emptyTimeLeft > 60000L) {
            interval = 60000;
            remaining = value - 60000;
         } else if (value > 30000 && emptyTimeLeft > 30000L) {
            interval = 30000;
            remaining = value - 30000;
         } else {
            interval = 10000;
            remaining = value - 10000;
         }
      } else if (value > 300000) {
         int timeLeft = value / 60000;
         interval = 300000;
         SystemMessage sm = new SystemMessage(
            SystemMessageId.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTE_S_YOU_WILL_BE_FORCED_OUT_OF_THE_DUNGEON_WHEN_THE_TIME_EXPIRES
         );
         sm.addString(Integer.toString(timeLeft));
         sm.sendInBroadcast();

         for (Integer playerId : this._players) {
            Player player = World.getPlayer(playerId);
            if (player != null && player.isOnline() && player.getInstanceId() == this._id) {
               player.sendPacket(sm);
            }
         }

         remaining = value - 300000;
      } else if (value > 60000) {
         int timeLeft = value / 60000;
         interval = 60000;
         SystemMessage sm = new SystemMessage(
            SystemMessageId.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTE_S_YOU_WILL_BE_FORCED_OUT_OF_THE_DUNGEON_WHEN_THE_TIME_EXPIRES
         );
         sm.addString(Integer.toString(timeLeft));
         sm.sendInBroadcast();

         for (Integer playerIdx : this._players) {
            Player player = World.getPlayer(playerIdx);
            if (player != null && player.isOnline() && player.getInstanceId() == this._id) {
               player.sendPacket(sm);
            }
         }

         remaining = value - 60000;
      } else if (value > 30000) {
         int timeLeft = value / 1000;
         interval = 30000;
         cs = new CreatureSay(null, ChatType.ALLIANCE, "Notice", timeLeft + " seconds left.");
         remaining = value - 30000;
      } else {
         int timeLeft = value / 1000;
         interval = 10000;
         cs = new CreatureSay(null, ChatType.ALLIANCE, "Notice", timeLeft + " seconds left.");
         remaining = value - 10000;
      }

      if (cs != null) {
         for (Integer objectId : this._players) {
            Player player = World.getPlayer(objectId);
            if (player != null && player.getInstanceId() == this._id) {
               player.sendPacket(cs);
            }
         }
      }

      this.cancelTimer();
      if (remaining >= 10000) {
         this._checkTimeUpTask = ThreadPool.schedule(new Instance.CheckTimeUp(remaining), interval);
      } else {
         this._checkTimeUpTask = ThreadPool.schedule(new Instance.TimeUp(), interval);
      }
   }

   public void cancelTimer() {
      if (this._checkTimeUpTask != null) {
         this._checkTimeUpTask.cancel(true);
      }
   }

   public void cancelEjectDeadPlayer(Player player) {
      ScheduledFuture<?> task = this._ejectDeadTasks.remove(player.getObjectId());
      if (task != null) {
         task.cancel(true);
      }
   }

   public void notifyDeath(Player player) {
      if (!player.isOnEvent() && this._ejectTime > 0) {
         player.sendMessage("If you are not resurrected within " + this._ejectTime / 1000 / 60 + " minutes, you will be expelled from the instance zone.");
         this._ejectDeadTasks.put(player.getObjectId(), ThreadPool.schedule(() -> {
            if (player.isDead() && player.getInstanceId() == this._id) {
               player.setInstanceId(0);
               if (this._exitLocation != null) {
                  player.teleToLocation(this._exitLocation, true);
               } else {
                  player.teleToLocation(TeleportWhereType.TOWN);
               }
            }
         }, this._ejectTime));
      }
   }

   public InstanceReenterType getReenterType() {
      return this._type;
   }

   public void setReenterType(InstanceReenterType type) {
      this._type = type;
   }

   public List<InstanceReenterTimeHolder> getReenterData() {
      return this._resetData;
   }

   public boolean isRemoveBuffEnabled() {
      return this._removeBuffType != InstanceRemoveBuffType.NONE;
   }

   public InstanceRemoveBuffType getRemoveBuffType() {
      return this._removeBuffType;
   }

   public List<Integer> getBuffExceptionList() {
      return this._exceptionList;
   }

   public class CheckTimeUp implements Runnable {
      private final int _remaining;

      public CheckTimeUp(int remaining) {
         Objects.requireNonNull(Instance.this);
         super();
         this._remaining = remaining;
      }

      @Override
      public void run() {
         Instance.this.doCheckTimeUp(this._remaining);
      }
   }

   public class TimeUp implements Runnable {
      public TimeUp() {
         Objects.requireNonNull(Instance.this);
         super();
      }

      @Override
      public void run() {
         InstanceManager.getInstance().destroyInstance(Instance.this.getId());
      }
   }
}
