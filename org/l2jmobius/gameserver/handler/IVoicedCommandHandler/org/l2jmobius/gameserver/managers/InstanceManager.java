package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.StringStringHolder;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class InstanceManager implements IXmlReader {
   private static final Map<Integer, Instance> INSTANCES = new ConcurrentHashMap<>();
   private final Map<Integer, InstanceWorld> _instanceWorlds = new ConcurrentHashMap<>();
   private int _dynamic = 300000;
   private static final Map<Integer, String> _instanceIdNames = new HashMap<>();
   private final List<StringStringHolder> _instanceTemplateNames = new ArrayList<>();
   private final Map<Integer, String> _instanceTemplates = new ConcurrentHashMap<>();
   private final Map<Integer, Map<Integer, Long>> _playerTimes = new ConcurrentHashMap<>();
   private static final String ADD_INSTANCE_TIME = "INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?";
   private static final String RESTORE_INSTANCE_TIMES = "SELECT instanceId,time FROM character_instance_time WHERE charId=?";
   private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";

   protected InstanceManager() {
      INSTANCES.put(-1, new Instance(-1));
      LOGGER.info(this.getClass().getSimpleName() + ": Multiverse Instance created.");
      INSTANCES.put(0, new Instance(0));
      LOGGER.info(this.getClass().getSimpleName() + ": Universe Instance created.");
      this.load();
   }

   @Override
   public void load() {
      _instanceIdNames.clear();
      this.parseDatapackFile("data/InstanceNames.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _instanceIdNames.size() + " instance names.");
      this._instanceTemplates.clear();
      this.parseDatapackDirectory("data/instances", true);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._instanceTemplates.size() + " instance templates.");
   }

   public long getInstanceTime(int playerObjId, int id) {
      if (!this._playerTimes.containsKey(playerObjId)) {
         this.restoreInstanceTimes(playerObjId);
      }

      return this._playerTimes.get(playerObjId).containsKey(id) ? this._playerTimes.get(playerObjId).get(id) : -1L;
   }

   public Map<Integer, Long> getAllInstanceTimes(int playerObjId) {
      if (!this._playerTimes.containsKey(playerObjId)) {
         this.restoreInstanceTimes(playerObjId);
      }

      return this._playerTimes.get(playerObjId);
   }

   public void setInstanceTime(int playerObjId, int id, long time) {
      if (!this._playerTimes.containsKey(playerObjId)) {
         this.restoreInstanceTimes(playerObjId);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?"
         );
      ) {
         ps.setInt(1, playerObjId);
         ps.setInt(2, id);
         ps.setLong(3, time);
         ps.setLong(4, time);
         ps.execute();
         this._playerTimes.get(playerObjId).put(id, time);
      } catch (Exception var13) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not insert character instance time data: " + var13.getMessage());
      }
   }

   public void deleteInstanceTime(int playerObjId, int id) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=? AND instanceId=?");
      ) {
         ps.setInt(1, playerObjId);
         ps.setInt(2, id);
         ps.execute();
         this._playerTimes.get(playerObjId).remove(id);
      } catch (Exception var11) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not delete character instance time data: " + var11.getMessage());
      }
   }

   public void restoreInstanceTimes(int playerObjId) {
      if (!this._playerTimes.containsKey(playerObjId)) {
         this._playerTimes.put(playerObjId, new ConcurrentHashMap<>());

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT instanceId,time FROM character_instance_time WHERE charId=?");
         ) {
            ps.setInt(1, playerObjId);

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  int id = rs.getInt("instanceId");
                  long time = rs.getLong("time");
                  if (time < System.currentTimeMillis()) {
                     this.deleteInstanceTime(playerObjId, id);
                  } else {
                     this._playerTimes.get(playerObjId).put(id, time);
                  }
               }
            }
         } catch (Exception var14) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Could not delete character instance time data: " + var14.getMessage());
         }
      }
   }

   public String getInstanceIdName(int id) {
      return _instanceIdNames.getOrDefault(id, "UnknownInstance");
   }

   public Collection<StringStringHolder> getInstanceTemplateNames() {
      return this._instanceTemplateNames;
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         String var4 = n.getNodeName();
         switch (var4) {
            case "list":
               for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                  if ("instance".equals(d.getNodeName())) {
                     NamedNodeMap attrsx = d.getAttributes();
                     int id = this.parseInteger(attrsx, "id");
                     String name = attrsx.getNamedItem("name").getNodeValue();
                     _instanceIdNames.put(id, name);
                     this._instanceTemplateNames.add(new StringStringHolder(String.valueOf(id), name));
                  }
               }
               break;
            case "instance":
               NamedNodeMap attrs = n.getAttributes();
               this._instanceTemplates
                  .put(this.parseInteger(attrs, "id"), file.getPath().substring(file.getPath().lastIndexOf(File.separator + "instances" + File.separator) + 11));
         }
      }
   }

   public void addWorld(InstanceWorld world) {
      this._instanceWorlds.put(world.getInstanceId(), world);
   }

   public InstanceWorld getWorld(int instanceId) {
      return this._instanceWorlds.get(instanceId);
   }

   public InstanceWorld getWorld(WorldObject object) {
      return this._instanceWorlds.get(object.getInstanceId());
   }

   public InstanceWorld getPlayerWorld(Player player) {
      for (InstanceWorld temp : this._instanceWorlds.values()) {
         if (temp != null && temp.isAllowed(player)) {
            return temp;
         }
      }

      return null;
   }

   public void destroyInstance(int instanceid) {
      if (instanceid > 0) {
         Instance temp = INSTANCES.get(instanceid);
         if (temp != null) {
            temp.removeNpcs();
            temp.removePlayers();
            temp.removeDoors();
            temp.cancelTimer();
            INSTANCES.remove(instanceid);
            this._instanceWorlds.remove(instanceid);
         }
      }
   }

   public Instance getInstance(int instanceid) {
      return INSTANCES.get(instanceid);
   }

   public Map<Integer, Instance> getInstances() {
      return INSTANCES;
   }

   public int getPlayer(int objectId) {
      for (Instance temp : INSTANCES.values()) {
         if (temp != null && temp.containsPlayer(objectId)) {
            return temp.getId();
         }
      }

      return 0;
   }

   public boolean createInstanceFromTemplate(int id, int templateId) {
      if (this.getInstance(id) != null) {
         return false;
      } else {
         Instance instance = new Instance(id);
         INSTANCES.put(id, instance);
         instance.loadInstanceTemplate(templateId);
         instance.spawnDoors();
         instance.spawnGroup("general");
         return true;
      }
   }

   public synchronized Instance createDynamicInstance(int templateId) {
      while (this.getInstance(this._dynamic) != null) {
         this._dynamic++;
         if (this._dynamic == Integer.MAX_VALUE) {
            LOGGER.warning(this.getClass().getSimpleName() + ": More then 2147183647 instances created");
            this._dynamic = 300000;
         }
      }

      Instance instance = new Instance(this._dynamic);
      INSTANCES.put(this._dynamic, instance);
      if (templateId > 0) {
         instance.loadInstanceTemplate(templateId);
         instance.spawnDoors();
         instance.spawnGroup("general");
      }

      return instance;
   }

   public String getInstanceTemplateFileName(int id) {
      return this._instanceTemplates.get(id);
   }

   public static InstanceManager getInstance() {
      return InstanceManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final InstanceManager INSTANCE = new InstanceManager();
   }
}
