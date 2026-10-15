package org.l2jmobius.gameserver.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class World {
   private static final Logger LOGGER = Logger.getLogger(World.class.getName());
   public static volatile int MAX_CONNECTED_COUNT = 0;
   public static volatile int OFFLINE_TRADE_COUNT = 0;
   public static final int GRACIA_MAX_X = -166168;
   public static final int GRACIA_MAX_Z = 6105;
   public static final int GRACIA_MIN_Z = -895;
   public static final int SHIFT_BY = 11;
   public static final int TILE_SIZE = 32768;
   public static final int TILE_X_MIN = 11;
   public static final int TILE_Y_MIN = 10;
   public static final int TILE_X_MAX = 26;
   public static final int TILE_Y_MAX = 26;
   public static final int TILE_ZERO_COORD_X = 20;
   public static final int TILE_ZERO_COORD_Y = 18;
   public static final int WORLD_X_MIN = -294912;
   public static final int WORLD_Y_MIN = -262144;
   public static final int WORLD_X_MAX = 229376;
   public static final int WORLD_Y_MAX = 294912;
   public static final int WORLD_Z_MIN = -16000;
   public static final int WORLD_Z_MAX = 16000;
   public static final int Z_REGION_SIZE = 2000;
   public static final int OFFSET_X = Math.abs(-144);
   public static final int OFFSET_Y = Math.abs(-128);
   private static final int REGIONS_X = 112 + OFFSET_X;
   private static final int REGIONS_Y = 144 + OFFSET_Y;
   private static final int REGIONS_Z = (Math.abs(-16000) + Math.abs(16000)) / 2000;
   private static final Map<Integer, Player> _allPlayers = new ConcurrentHashMap<>();
   private static final Map<Integer, Player> _allGoodPlayers = new ConcurrentHashMap<>();
   private static final Map<Integer, Player> _allEvilPlayers = new ConcurrentHashMap<>();
   private static final Map<Integer, WorldObject> _allObjects = new ConcurrentHashMap<>();
   private static final Map<Integer, Pet> _petsInstance = new ConcurrentHashMap<>();
   private static final WorldRegion[][][] _worldRegions = new WorldRegion[REGIONS_X + 1][REGIONS_Y + 1][REGIONS_Z];

   private World() {
   }

   public static void init() {
      for (int x = 0; x <= REGIONS_X; x++) {
         for (int y = 0; y <= REGIONS_Y; y++) {
            for (int z = 0; z < REGIONS_Z; z++) {
               _worldRegions[x][y][z] = new WorldRegion(x, y, z);
            }
         }
      }

      for (int rx = 0; rx <= REGIONS_X; rx++) {
         for (int ry = 0; ry <= REGIONS_Y; ry++) {
            for (int rz = 0; rz < REGIONS_Z; rz++) {
               List<WorldRegion> surroundingRegions = new ArrayList<>();

               for (int sx = rx - 1; sx <= rx + 1; sx++) {
                  for (int sy = ry - 1; sy <= ry + 1; sy++) {
                     for (int sz = rz - 1; sz <= rz + 1; sz++) {
                        if (sx >= 0 && sx < REGIONS_X && sy >= 0 && sy < REGIONS_Y && sz >= 0 && sz < REGIONS_Z) {
                           surroundingRegions.add(_worldRegions[sx][sy][sz]);
                        }
                     }
                  }
               }

               WorldRegion[] regionArray = new WorldRegion[surroundingRegions.size()];
               regionArray = surroundingRegions.toArray(regionArray);
               _worldRegions[rx][ry][rz].setSurroundingRegions(regionArray);
            }
         }
      }

      if (!InterfaceConfig.ENABLE_GUI) {
         LOGGER.info(World.class.getSimpleName() + ": (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
      }
   }

   public static void addObject(WorldObject object) {
      _allObjects.putIfAbsent(object.getObjectId(), object);
      if (object.isPlayer()) {
         Player newPlayer = object.asPlayer();
         if (newPlayer.isTeleporting()) {
            return;
         }

         Player existingPlayer = _allPlayers.putIfAbsent(object.getObjectId(), newPlayer);
         if (existingPlayer != null) {
            Disconnection.of(existingPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
            Disconnection.of(newPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
            LOGGER.warning(World.class.getSimpleName() + ": Duplicate character!? Disconnected both characters (" + newPlayer.getName() + ")");
         } else if (FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
            addFactionPlayerToWorld(newPlayer);
         }
      }
   }

   public static void removeObject(WorldObject object) {
      _allObjects.remove(object.getObjectId());
      if (object.isPlayer()) {
         Player player = object.asPlayer();
         if (player.isTeleporting()) {
            return;
         }

         _allPlayers.remove(object.getObjectId());
         if (FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
            if (player.isGood()) {
               _allGoodPlayers.remove(player.getObjectId());
            } else if (player.isEvil()) {
               _allEvilPlayers.remove(player.getObjectId());
            }
         }
      }
   }

   public static WorldObject findObject(int objectId) {
      return _allObjects.get(objectId);
   }

   public static Collection<WorldObject> getVisibleObjects() {
      return _allObjects.values();
   }

   public static int getVisibleObjectsCount() {
      return _allObjects.size();
   }

   public static Collection<Player> getPlayers() {
      return _allPlayers.values();
   }

   public static Collection<Player> getAllGoodPlayers() {
      return _allGoodPlayers.values();
   }

   public static Collection<Player> getAllEvilPlayers() {
      return _allEvilPlayers.values();
   }

   public static Player getPlayer(String name) {
      return getPlayer(CharInfoTable.getInstance().getIdByName(name));
   }

   public static Player getPlayer(int objectId) {
      return _allPlayers.get(objectId);
   }

   public static Pet getPet(int ownerId) {
      return _petsInstance.get(ownerId);
   }

   public static Pet addPet(int ownerId, Pet pet) {
      return _petsInstance.put(ownerId, pet);
   }

   public static void removePet(int ownerId) {
      _petsInstance.remove(ownerId);
   }

   public static Npc getNpc(int npcId) {
      for (WorldObject wo : getVisibleObjects()) {
         if (wo.isNpc() && wo.getId() == npcId) {
            return wo.asNpc();
         }
      }

      return null;
   }

   public static void addVisibleObject(WorldObject object, WorldRegion newRegion) {
      if (newRegion.isActive()) {
         forEachVisibleObject(object, WorldObject.class, wo -> {
            if (object.isPlayer() && wo.isVisibleFor(object.asPlayer())) {
               wo.sendInfo(object.asPlayer());
               if (wo.isCreature()) {
                  CreatureAI ai = wo.asCreature().getAI();
                  if (ai != null) {
                     ai.describeStateToPlayer(object.asPlayer());
                     if (wo.isMonster() && ai.getIntention() == Intention.IDLE) {
                        ai.setIntentionActive();
                     }
                  }
               }
            }

            if (wo.isPlayer() && object.isVisibleFor(wo.asPlayer())) {
               object.sendInfo(wo.asPlayer());
               if (object.isCreature()) {
                  CreatureAI ai = object.asCreature().getAI();
                  if (ai != null) {
                     ai.describeStateToPlayer(wo.asPlayer());
                     if (object.isMonster() && ai.getIntention() == Intention.IDLE) {
                        ai.setIntentionActive();
                     }
                  }
               }
            }
         });
      }
   }

   public static void addFactionPlayerToWorld(Player player) {
      if (player.isGood()) {
         _allGoodPlayers.put(player.getObjectId(), player);
      } else if (player.isEvil()) {
         _allEvilPlayers.put(player.getObjectId(), player);
      }
   }

   public static void removeVisibleObject(WorldObject object, WorldRegion oldRegion) {
      if (object != null && oldRegion != null) {
         oldRegion.removeVisibleObject(object);
         WorldRegion[] surroundingRegions = oldRegion.getSurroundingRegions();

         for (int i = 0; i < surroundingRegions.length; i++) {
            Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
            if (!visibleObjects.isEmpty()) {
               for (WorldObject wo : visibleObjects) {
                  if (wo != object) {
                     if (object.isCreature()) {
                        Creature objectCreature = object.asCreature();
                        CreatureAI ai = objectCreature.getAI();
                        if (ai != null) {
                           ai.notifyActionForgetObject(wo);
                        }

                        if (objectCreature.getTarget() == wo) {
                           objectCreature.setTarget(null);
                        }

                        if (object.isPlayer()) {
                           object.sendPacket(new DeleteObject(wo));
                        }
                     }

                     if (wo.isCreature()) {
                        Creature woCreature = wo.asCreature();
                        CreatureAI aix = woCreature.getAI();
                        if (aix != null) {
                           aix.notifyActionForgetObject(object);
                        }

                        if (woCreature.getTarget() == object) {
                           woCreature.setTarget(null);
                        }

                        if (wo.isPlayer()) {
                           wo.sendPacket(new DeleteObject(object));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void switchRegion(WorldObject object, WorldRegion newRegion) {
      WorldRegion oldRegion = object.getWorldRegion();
      if (oldRegion != null && oldRegion != newRegion) {
         WorldRegion[] oldSurroundingRegions = oldRegion.getSurroundingRegions();

         for (int i = 0; i < oldSurroundingRegions.length; i++) {
            WorldRegion worldRegion = oldSurroundingRegions[i];
            if (!newRegion.isSurroundingRegion(worldRegion)) {
               Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object) {
                        if (object.isCreature()) {
                           Creature objectCreature = object.asCreature();
                           CreatureAI ai = objectCreature.getAI();
                           if (ai != null) {
                              ai.notifyActionForgetObject(wo);
                           }

                           if (objectCreature.getTarget() == wo) {
                              objectCreature.setTarget(null);
                           }

                           if (object.isPlayer()) {
                              object.sendPacket(new DeleteObject(wo));
                           }
                        }

                        if (wo.isCreature()) {
                           Creature woCreature = wo.asCreature();
                           CreatureAI aix = woCreature.getAI();
                           if (aix != null) {
                              aix.notifyActionForgetObject(object);
                           }

                           if (woCreature.getTarget() == object) {
                              woCreature.setTarget(null);
                           }

                           if (wo.isPlayer()) {
                              wo.sendPacket(new DeleteObject(object));
                           }
                        }
                     }
                  }
               }
            }
         }

         WorldRegion[] newSurroundingRegions = newRegion.getSurroundingRegions();

         for (int ix = 0; ix < newSurroundingRegions.length; ix++) {
            WorldRegion worldRegion = newSurroundingRegions[ix];
            if (!oldRegion.isSurroundingRegion(worldRegion)) {
               Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wox : visibleObjects) {
                     if (wox != object && wox.getInstanceId() == object.getInstanceId()) {
                        if (object.isPlayer() && wox.isVisibleFor(object.asPlayer())) {
                           wox.sendInfo(object.asPlayer());
                           if (wox.isCreature()) {
                              CreatureAI aixx = wox.asCreature().getAI();
                              if (aixx != null) {
                                 aixx.describeStateToPlayer(object.asPlayer());
                                 if (wox.isMonster() && aixx.getIntention() == Intention.IDLE) {
                                    aixx.setIntentionActive();
                                 }
                              }
                           }
                        }

                        if (wox.isPlayer() && object.isVisibleFor(wox.asPlayer())) {
                           object.sendInfo(wox.asPlayer());
                           if (object.isCreature()) {
                              CreatureAI aixx = object.asCreature().getAI();
                              if (aixx != null) {
                                 aixx.describeStateToPlayer(wox.asPlayer());
                                 if (object.isMonster() && aixx.getIntention() == Intention.IDLE) {
                                    aixx.setIntentionActive();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz) {
      List<T> result = new ArrayList<>();
      if (object == null) {
         return result;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return result;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        result.add(clazz.cast(wo));
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   public static <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range) {
      List<T> result = new ArrayList<>();
      if (object == null) {
         return result;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return result;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        result.add(clazz.cast(wo));
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   public static <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz, Predicate<T> predicate) {
      List<T> result = new ArrayList<>();
      if (object == null) {
         return result;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return result;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           result.add(cast);
                        }
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   public static <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate) {
      List<T> result = new ArrayList<>();
      if (object == null) {
         return result;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return result;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           result.add(cast);
                        }
                     }
                  }
               }
            }

            return result;
         }
      }
   }

   public static <T extends WorldObject> void forEachVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> c) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        c.accept(clazz.cast(wo));
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> void forEachVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> c) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        c.accept(clazz.cast(wo));
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> T getFirstVisibleObject(WorldObject object, Class<T> clazz) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        return clazz.cast(wo);
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public static <T extends WorldObject> T getFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        return clazz.cast(wo);
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public static <T extends WorldObject> T getFirstVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           return cast;
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public static <T extends WorldObject> T getFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           return cast;
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public static <T extends WorldObject> void forFirstVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        action.accept(clazz.cast(wo));
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> void forFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        action.accept(clazz.cast(wo));
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> void forFirstVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           action.accept(cast);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> void forFirstVisibleObjectInRange(
      WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action
   ) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           action.accept(cast);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static <T extends WorldObject> T getNearestVisibleObject(WorldObject object, Class<T> clazz) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance < bestDistance) {
                           bestDistance = distance;
                           nearest = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            return nearest;
         }
      }
   }

   public static <T extends WorldObject> T getNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance <= range && distance < bestDistance) {
                           bestDistance = distance;
                           nearest = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            return nearest;
         }
      }
   }

   public static <T extends WorldObject> T getNearestVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           double distance = wo.calculateDistance3D(object);
                           if (distance < bestDistance) {
                              bestDistance = distance;
                              nearest = cast;
                           }
                        }
                     }
                  }
               }
            }

            return nearest;
         }
      }
   }

   public static <T extends WorldObject> T getNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance <= range) {
                           T cast = (T)clazz.cast(wo);
                           if (predicate.test(cast) && distance < bestDistance) {
                              bestDistance = distance;
                              nearest = cast;
                           }
                        }
                     }
                  }
               }
            }

            return nearest;
         }
      }
   }

   public static <T extends WorldObject> void forNearestVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance < bestDistance) {
                           bestDistance = distance;
                           nearest = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            if (nearest != null) {
               action.accept(nearest);
            }
         }
      }
   }

   public static <T extends WorldObject> void forNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance <= range && distance < bestDistance) {
                           bestDistance = distance;
                           nearest = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            if (nearest != null) {
               action.accept(nearest);
            }
         }
      }
   }

   public static <T extends WorldObject> void forNearestVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           double distance = wo.calculateDistance3D(object);
                           if (distance < bestDistance) {
                              bestDistance = distance;
                              nearest = cast;
                           }
                        }
                     }
                  }
               }
            }

            if (nearest != null) {
               action.accept(nearest);
            }
         }
      }
   }

   public static <T extends WorldObject> void forNearestVisibleObjectInRange(
      WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action
   ) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T nearest = null;
            double bestDistance = Double.MAX_VALUE;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        double distance = wo.calculateDistance3D(object);
                        if (distance <= range) {
                           T cast = (T)clazz.cast(wo);
                           if (predicate.test(cast) && distance < bestDistance) {
                              bestDistance = distance;
                              nearest = cast;
                           }
                        }
                     }
                  }
               }
            }

            if (nearest != null) {
               action.accept(nearest);
            }
         }
      }
   }

   public static <T extends WorldObject> T getRandomVisibleObject(WorldObject object, Class<T> clazz) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T selected = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        if (Rnd.get(++count) == 0) {
                           selected = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            return selected != null ? selected : getFirstVisibleObject(object, clazz);
         }
      }
   }

   public static <T extends WorldObject> T getRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T selected = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        if (Rnd.get(++count) == 0) {
                           selected = (T)clazz.cast(wo);
                        }
                     }
                  }
               }
            }

            return selected != null ? selected : getFirstVisibleObjectInRange(object, clazz, range);
         }
      }
   }

   public static <T extends WorldObject> T getRandomVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           if (firstValid == null) {
                              firstValid = cast;
                           }

                           if (Rnd.get(++count) == 0) {
                              selected = cast;
                           }
                        }
                     }
                  }
               }
            }

            return selected != null ? selected : firstValid;
         }
      }
   }

   public static <T extends WorldObject> T getRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate) {
      if (object == null) {
         return null;
      } else {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion == null) {
            return null;
         } else {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           if (firstValid == null) {
                              firstValid = cast;
                           }

                           if (Rnd.get(++count) == 0) {
                              selected = cast;
                           }
                        }
                     }
                  }
               }
            }

            return selected != null ? selected : firstValid;
         }
      }
   }

   public static <T extends WorldObject> void forRandomVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (firstValid == null) {
                           firstValid = cast;
                        }

                        if (Rnd.get(++count) == 0) {
                           selected = cast;
                        }
                     }
                  }
               }
            }

            T target = selected != null ? selected : firstValid;
            if (target != null) {
               action.accept(target);
            }
         }
      }
   }

   public static <T extends WorldObject> void forRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (firstValid == null) {
                           firstValid = cast;
                        }

                        if (Rnd.get(++count) == 0) {
                           selected = cast;
                        }
                     }
                  }
               }
            }

            T target = selected != null ? selected : firstValid;
            if (target != null) {
               action.accept(target);
            }
         }
      }
   }

   public static <T extends WorldObject> void forRandomVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId()) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           if (firstValid == null) {
                              firstValid = cast;
                           }

                           if (Rnd.get(++count) == 0) {
                              selected = cast;
                           }
                        }
                     }
                  }
               }
            }

            T target = selected != null ? selected : firstValid;
            if (target != null) {
               action.accept(target);
            }
         }
      }
   }

   public static <T extends WorldObject> void forRandomVisibleObjectInRange(
      WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action
   ) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            T selected = null;
            T firstValid = null;
            int count = 0;
            WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();

            for (int i = 0; i < surroundingRegions.length; i++) {
               Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
               if (!visibleObjects.isEmpty()) {
                  for (WorldObject wo : visibleObjects) {
                     if (wo != object && clazz.isInstance(wo) && wo.getInstanceId() == object.getInstanceId() && !(wo.calculateDistance3D(object) > range)) {
                        T cast = (T)clazz.cast(wo);
                        if (predicate.test(cast)) {
                           if (firstValid == null) {
                              firstValid = cast;
                           }

                           if (Rnd.get(++count) == 0) {
                              selected = cast;
                           }
                        }
                     }
                  }
               }
            }

            T target = selected != null ? selected : firstValid;
            if (target != null) {
               action.accept(target);
            }
         }
      }
   }

   public static void broadcastToVisiblePlayers(WorldObject object, ServerPacket packet) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            packet.sendInBroadcast();
            WorldRegion[] regions = worldRegion.getSurroundingRegions();
            if (regions != null) {
               for (int i = 0; i < regions.length; i++) {
                  Collection<WorldObject> objects = regions[i].getVisibleObjects();
                  if (!objects.isEmpty()) {
                     for (WorldObject nearby : objects) {
                        if (nearby != object && nearby.isPlayer() && nearby.getInstanceId() == object.getInstanceId()) {
                           nearby.asPlayer().sendPacket(packet);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void broadcastToVisiblePlayersInRange(WorldObject object, ServerPacket packet, int range) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            packet.sendInBroadcast();
            WorldRegion[] regions = worldRegion.getSurroundingRegions();
            if (regions != null) {
               for (int i = 0; i < regions.length; i++) {
                  Collection<WorldObject> objects = regions[i].getVisibleObjects();
                  if (!objects.isEmpty()) {
                     for (WorldObject nearby : objects) {
                        if (nearby != object
                           && nearby.isPlayer()
                           && nearby.getInstanceId() == object.getInstanceId()
                           && !(nearby.calculateDistance3D(object) > range)) {
                           nearby.asPlayer().sendPacket(packet);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void broadcastToSelfAndVisiblePlayers(WorldObject object, ServerPacket packet) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            packet.sendInBroadcast();
            WorldRegion[] regions = worldRegion.getSurroundingRegions();
            if (regions != null) {
               for (int i = 0; i < regions.length; i++) {
                  Collection<WorldObject> objects = regions[i].getVisibleObjects();
                  if (!objects.isEmpty()) {
                     for (WorldObject nearby : objects) {
                        if (nearby.isPlayer() && nearby.getInstanceId() == object.getInstanceId()) {
                           nearby.asPlayer().sendPacket(packet);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void broadcastToSelfAndVisiblePlayersInRange(WorldObject object, ServerPacket packet, int range) {
      if (object != null) {
         WorldRegion worldRegion = getRegion(object);
         if (worldRegion != null) {
            packet.sendInBroadcast();
            WorldRegion[] regions = worldRegion.getSurroundingRegions();
            if (regions != null) {
               for (int i = 0; i < regions.length; i++) {
                  Collection<WorldObject> objects = regions[i].getVisibleObjects();
                  if (!objects.isEmpty()) {
                     for (WorldObject nearby : objects) {
                        if (nearby.isPlayer() && nearby.getInstanceId() == object.getInstanceId() && !(nearby.calculateDistance3D(object) > range)) {
                           nearby.asPlayer().sendPacket(packet);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void broadcastToAllOnlinePlayers(ServerPacket packet) {
      packet.sendInBroadcast();

      for (Player player : _allPlayers.values()) {
         if (player.isOnline()) {
            player.sendPacket(packet);
         }
      }
   }

   public static void broadcastToAllOnlinePlayers(String text) {
      broadcastToAllOnlinePlayers(text, false);
   }

   public static void broadcastToAllOnlinePlayers(String text, boolean isCritical) {
      broadcastToAllOnlinePlayers(new CreatureSay(null, isCritical ? ChatType.CRITICAL_ANNOUNCE : ChatType.ANNOUNCEMENT, "", text));
   }

   public static void broadcastToAllOnlinePlayersOnScreen(String text) {
      broadcastToAllOnlinePlayers(new ExShowScreenMessage(text, 10000));
   }

   public static WorldRegion getRegion(WorldObject object) {
      try {
         int z = object.getZ();
         int regionZ = calculateRegionZ(z);
         return _worldRegions[(object.getX() >> 11) + OFFSET_X][(object.getY() >> 11) + OFFSET_Y][regionZ];
      } catch (ArrayIndexOutOfBoundsException var3) {
         disposeOutOfBoundsObject(object);
         return null;
      }
   }

   public static WorldRegion getRegion(int x, int y, int z) {
      try {
         int regionZ = calculateRegionZ(z);
         return _worldRegions[(x >> 11) + OFFSET_X][(y >> 11) + OFFSET_Y][regionZ];
      } catch (ArrayIndexOutOfBoundsException var4) {
         LOGGER.warning(
            World.class.getSimpleName()
               + ": Incorrect world region X: "
               + ((x >> 11) + OFFSET_X)
               + " Y: "
               + ((y >> 11) + OFFSET_Y)
               + " Z: "
               + calculateRegionZ(z)
         );
         return null;
      }
   }

   private static int calculateRegionZ(int z) {
      if (z < -16000) {
         return 0;
      } else {
         return z > 16000 ? REGIONS_Z - 1 : (z - -16000) / 2000;
      }
   }

   public static WorldRegion[][][] getWorldRegions() {
      return _worldRegions;
   }

   public static synchronized void disposeOutOfBoundsObject(WorldObject object) {
      if (object.isPlayer()) {
         object.asCreature().stopMove(object.asPlayer().getLastServerPosition());
      } else if (object.isSummon()) {
         Summon summon = object.asSummon();
         summon.unSummon(summon.getOwner());
      } else if (_allObjects.remove(object.getObjectId()) != null) {
         if (object.isNpc()) {
            Npc npc = object.asNpc();
            LOGGER.warning(
               "Deleting npc "
                  + object.getName()
                  + " NPCID["
                  + npc.getId()
                  + "] from invalid location X:"
                  + object.getX()
                  + " Y:"
                  + object.getY()
                  + " Z:"
                  + object.getZ()
            );
            npc.deleteMe();
            Spawn spawn = npc.getSpawn();
            if (spawn != null) {
               LOGGER.warning("Spawn location X:" + spawn.getX() + " Y:" + spawn.getY() + " Z:" + spawn.getZ() + " Heading:" + spawn.getHeading());
            }
         } else if (object.isCreature()) {
            LOGGER.warning(
               "Deleting object "
                  + object.getName()
                  + " OID["
                  + object.getObjectId()
                  + "] from invalid location X:"
                  + object.getX()
                  + " Y:"
                  + object.getY()
                  + " Z:"
                  + object.getZ()
            );
            object.asCreature().deleteMe();
         }

         if (object.getWorldRegion() != null) {
            object.getWorldRegion().removeVisibleObject(object);
         }
      }
   }
}
