package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DoorData implements IXmlReader {
   private static final Map<String, Set<Integer>> _groups = new HashMap<>();
   private final Map<Integer, Door> _doors = new HashMap<>();
   private final Map<Integer, StatSet> _templates = new HashMap<>();

   protected DoorData() {
      this.load();
   }

   @Override
   public void load() {
      this._doors.clear();
      _groups.clear();
      this.parseDatapackFile("data/Doors.xml");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node a = document.getFirstChild(); a != null; a = a.getNextSibling()) {
         if ("list".equalsIgnoreCase(a.getNodeName())) {
            for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling()) {
               if ("door".equalsIgnoreCase(b.getNodeName())) {
                  NamedNodeMap attrs = b.getAttributes();
                  StatSet set = new StatSet();
                  set.set("baseHpMax", 1);

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  this.makeDoor(set);
                  this._templates.put(set.getInt("id"), set);
               }
            }
         }
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._doors.size() + " doors.");
   }

   public void insertCollisionData(StatSet set) {
      int height = set.getInt("height");
      String[] pos = set.getString("node1").split(",");
      int nodeX = Integer.parseInt(pos[0]);
      int nodeY = Integer.parseInt(pos[1]);
      pos = set.getString("node2").split(",");
      int posX = Integer.parseInt(pos[0]);
      int posY = Integer.parseInt(pos[1]);
      int collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY));
      if (collisionRadius < 20) {
         collisionRadius = 20;
      }

      set.set("collisionRadius", collisionRadius);
      set.set("collisionHeight", height);
   }

   private void makeDoor(StatSet set) {
      this.insertCollisionData(set);
      DoorTemplate template = new DoorTemplate(set);
      Door door = new Door(template);
      door.setCurrentHp(door.getMaxHp());
      door.spawnMe(template.getX(), template.getY(), template.getZ());
      this._doors.put(door.getId(), door);
   }

   public StatSet getDoorTemplate(int doorId) {
      return this._templates.get(doorId);
   }

   public Door getDoor(int doorId) {
      return this._doors.get(doorId);
   }

   public static void addDoorGroup(String groupName, int doorId) {
      Set<Integer> set = _groups.get(groupName);
      if (set == null) {
         set = new HashSet<>();
         _groups.put(groupName, set);
      }

      set.add(doorId);
   }

   public static Set<Integer> getDoorsByGroup(String groupName) {
      return _groups.get(groupName);
   }

   public Collection<Door> getDoors() {
      return this._doors.values();
   }

   public boolean checkIfDoorsBetween(Location start, Location end, int instanceId) {
      return this.checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instanceId);
   }

   public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId) {
      return this.checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceId, false);
   }

   public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean doubleFaceCheck) {
      Collection<Door> doors;
      if (instanceId < 1) {
         WorldRegion region = World.getRegion(x, y, z);
         if (region != null) {
            doors = region.getDoors();
         } else {
            doors = null;
         }
      } else {
         Instance instance = InstanceManager.getInstance().getInstance(instanceId);
         if (instance != null) {
            doors = instance.getDoors();
         } else {
            doors = null;
         }
      }

      if (doors != null && !doors.isEmpty()) {
         for (Door doorInst : doors) {
            if (instanceId == doorInst.getInstanceId() && !doorInst.isDead() && !doorInst.isOpen() && doorInst.checkCollision() && doorInst.getX(0) != 0) {
               boolean intersectFace = false;

               for (int i = 0; i < 4; i++) {
                  int j = i + 1 < 4 ? i + 1 : 0;
                  int denominator = (ty - y) * (doorInst.getX(i) - doorInst.getX(j)) - (tx - x) * (doorInst.getY(i) - doorInst.getY(j));
                  if (denominator != 0) {
                     float multiplier1 = (float)(
                           (doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i)) - (doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i))
                        )
                        / denominator;
                     float multiplier2 = (float)((tx - x) * (y - doorInst.getY(i)) - (ty - y) * (x - doorInst.getX(i))) / denominator;
                     if (multiplier1 >= 0.0F && multiplier1 <= 1.0F && multiplier2 >= 0.0F && multiplier2 <= 1.0F) {
                        int intersectZ = Math.round(z + multiplier1 * (tz - z));
                        if (intersectZ > doorInst.getZMin() && intersectZ < doorInst.getZMax()) {
                           if (!doubleFaceCheck || intersectFace) {
                              return true;
                           }

                           intersectFace = true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static DoorData getInstance() {
      return DoorData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final DoorData INSTANCE = new DoorData();
   }
}
