package org.l2jmobius.gameserver.data;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.entity.actor.holders.npc.BuffSkillHolder;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SchemeBufferTable {
   private static final Logger LOGGER = Logger.getLogger(SchemeBufferTable.class.getName());
   private static final String SKILLS_XML_PATH = "./data/SchemeBufferSkills.xml";
   private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
   private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
   private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
   private static final String TYPE_MAGE_GROUP = "MAGE_GROUP";
   private static final String TYPE_FIGHTER_GROUP = "FIGHTER_GROUP";
   private static final String DB_COLUMN_OBJECT_ID = "object_id";
   private static final String DB_COLUMN_SCHEME_NAME = "scheme_name";
   private static final String DB_COLUMN_SKILLS = "skills";
   private static final String SKILL_SEPARATOR = ",";
   private final Map<Integer, Map<String, List<Integer>>> _schemesTable = new ConcurrentHashMap<>();
   private final Map<Integer, BuffSkillHolder> _availableBuffs = new LinkedHashMap<>();
   private final Map<String, List<Integer>> _skillIdsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
   private final Map<String, Map<Integer, BuffSkillHolder>> _availableBuffsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
   private final List<String> _skillTypesOrder = new ArrayList<>();

   public SchemeBufferTable() {
      this.loadAvailableBuffs();
      this.loadPlayerSchemes();
      LOGGER.info("SchemeBufferTable: Loaded " + this._schemesTable.size() + " players and " + this._availableBuffs.size() + " available skills.");
   }

   public void saveSchemes() {
      try (Connection connection = DatabaseFactory.getConnection()) {
         try (PreparedStatement deleteStatement = connection.prepareStatement("TRUNCATE TABLE buffer_schemes")) {
            deleteStatement.execute();
         }

         try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)")) {
            for (Entry<Integer, Map<String, List<Integer>>> playerEntry : this._schemesTable.entrySet()) {
               for (Entry<String, List<Integer>> schemeEntry : playerEntry.getValue().entrySet()) {
                  StringBuilder skillsBuilder = new StringBuilder();

                  for (int skillId : schemeEntry.getValue()) {
                     skillsBuilder.append(skillId).append(",");
                  }

                  if (!skillsBuilder.isEmpty()) {
                     skillsBuilder.setLength(skillsBuilder.length() - ",".length());
                  }

                  insertStatement.setInt(1, playerEntry.getKey());
                  insertStatement.setString(2, schemeEntry.getKey());
                  insertStatement.setString(3, skillsBuilder.toString());
                  insertStatement.addBatch();
               }
            }

            insertStatement.executeBatch();
         }
      } catch (Exception var16) {
         LOGGER.log(Level.WARNING, "SchemeBufferTable: Error while saving schemes. Players: " + this._schemesTable.size() + ".", (Throwable)var16);
      }
   }

   public void setScheme(int playerId, String schemeName, List<Integer> skillIds) {
      Map<String, List<Integer>> schemes = this._schemesTable.computeIfAbsent(playerId, key -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER));
      if (schemes.size() < SchemeBufferConfig.BUFFER_MAX_SCHEMES || schemes.containsKey(schemeName)) {
         List<Integer> safeList = skillIds != null ? List.copyOf(skillIds) : Collections.emptyList();
         schemes.put(schemeName, safeList);
      }
   }

   public boolean deleteScheme(int playerId, String schemeName) {
      Map<String, List<Integer>> schemes = this._schemesTable.get(playerId);
      if (schemes == null) {
         return false;
      } else {
         boolean removed = schemes.remove(schemeName) != null;
         if (schemes.isEmpty()) {
            this._schemesTable.remove(playerId);
         }

         return removed;
      }
   }

   public boolean addSkillToScheme(int playerId, String schemeName, int skillId) {
      Map<String, List<Integer>> schemes = this._schemesTable.get(playerId);
      if (schemes == null) {
         return false;
      } else {
         List<Integer> current = schemes.get(schemeName);
         if (current == null) {
            return false;
         } else if (current.contains(skillId)) {
            return false;
         } else {
            List<Integer> updated = new ArrayList<>(current);
            updated.add(skillId);
            schemes.put(schemeName, Collections.unmodifiableList(updated));
            return true;
         }
      }
   }

   public boolean removeSkillFromScheme(int playerId, String schemeName, int skillId) {
      Map<String, List<Integer>> schemes = this._schemesTable.get(playerId);
      if (schemes == null) {
         return false;
      } else {
         List<Integer> current = schemes.get(schemeName);
         if (current == null) {
            return false;
         } else if (!current.contains(skillId)) {
            return false;
         } else {
            List<Integer> updated = new ArrayList<>(current);
            updated.remove(Integer.valueOf(skillId));
            schemes.put(schemeName, Collections.unmodifiableList(updated));
            return true;
         }
      }
   }

   public Map<String, List<Integer>> getPlayerSchemes(int playerId) {
      Map<String, List<Integer>> schemes = this._schemesTable.get(playerId);
      return schemes != null ? Collections.unmodifiableMap(schemes) : null;
   }

   public List<Integer> getScheme(int playerId, String schemeName) {
      Map<String, List<Integer>> schemes = this._schemesTable.get(playerId);
      return schemes != null && schemes.get(schemeName) != null ? schemes.get(schemeName) : Collections.emptyList();
   }

   public List<Integer> getSkillsIdsByType(String groupType) {
      return this._skillIdsByType.getOrDefault(groupType, Collections.emptyList());
   }

   public List<String> getSkillTypes() {
      List<String> skillTypes = new ArrayList<>(this._skillTypesOrder.size());

      for (String type : this._skillTypesOrder) {
         if (!type.equalsIgnoreCase("MAGE_GROUP") && !type.equalsIgnoreCase("FIGHTER_GROUP")) {
            skillTypes.add(type);
         }
      }

      return skillTypes;
   }

   public BuffSkillHolder getAvailableBuff(int skillId) {
      return this._availableBuffs.get(skillId);
   }

   public BuffSkillHolder getAvailableBuff(String groupType, int skillId) {
      Map<Integer, BuffSkillHolder> holdersByType = this._availableBuffsByType.get(groupType);
      return holdersByType != null ? holdersByType.getOrDefault(skillId, this._availableBuffs.get(skillId)) : this._availableBuffs.get(skillId);
   }

   public static SchemeBufferTable getInstance() {
      return SchemeBufferTable.SingletonHolder.INSTANCE;
   }

   private void loadAvailableBuffs() {
      int categoryCount = 0;
      int skillCount = 0;

      try {
         Node rootNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("./data/SchemeBufferSkills.xml")).getDocumentElement();

         for (Node categoryNode = rootNode != null ? rootNode.getFirstChild() : null; categoryNode != null; categoryNode = categoryNode.getNextSibling()) {
            if ("category".equalsIgnoreCase(categoryNode.getNodeName())) {
               NamedNodeMap categoryAttributes = categoryNode.getAttributes();
               if (categoryAttributes != null) {
                  Node typeNode = categoryAttributes.getNamedItem("type");
                  if (typeNode != null) {
                     String category = typeNode.getNodeValue();
                     if (!this._skillIdsByType.containsKey(category)) {
                        this._skillIdsByType.put(category, new ArrayList<>());
                        this._availableBuffsByType.put(category, new LinkedHashMap<>());
                        this._skillTypesOrder.add(category);
                        categoryCount++;
                     }

                     for (Node buffNode = categoryNode.getFirstChild(); buffNode != null; buffNode = buffNode.getNextSibling()) {
                        if ("buff".equalsIgnoreCase(buffNode.getNodeName())) {
                           NamedNodeMap attributes = buffNode.getAttributes();
                           if (attributes != null) {
                              Node idNode = attributes.getNamedItem("id");
                              Node levelNode = attributes.getNamedItem("level");
                              Node priceNode = attributes.getNamedItem("price");
                              if (idNode != null && levelNode != null && priceNode != null) {
                                 int skillId = Integer.parseInt(idNode.getNodeValue());
                                 int level = Integer.parseInt(levelNode.getNodeValue());
                                 int price = Integer.parseInt(priceNode.getNodeValue());
                                 Node descNode = attributes.getNamedItem("desc");
                                 String description = descNode != null ? descNode.getNodeValue() : "";
                                 List<Integer> skillIds = this._skillIdsByType.get(category);
                                 if (!skillIds.contains(skillId)) {
                                    skillIds.add(skillId);
                                 }

                                 BuffSkillHolder holder = new BuffSkillHolder(skillId, level, price, category, description);
                                 this._availableBuffsByType.get(category).put(skillId, holder);
                                 BuffSkillHolder existing = this._availableBuffs.get(skillId);
                                 if (existing == null || holder.getLevel() > existing.getLevel()) {
                                    this._availableBuffs.put(skillId, holder);
                                 }

                                 skillCount++;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         for (Entry<String, List<Integer>> entry : this._skillIdsByType.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
         }

         LOGGER.info("SchemeBufferTable: Loaded " + categoryCount + " categories and " + skillCount + " entries.");
      } catch (Exception var21) {
         LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load skills XML: ./data/SchemeBufferSkills.xml.", (Throwable)var21);
      }
   }

   private void loadPlayerSchemes() {
      int entryCount = 0;

      try (
         Connection connection = DatabaseFactory.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM buffer_schemes");
         ResultSet resultSet = statement.executeQuery();
      ) {
         while (resultSet.next()) {
            String schemeName = resultSet.getString("scheme_name");
            String skills = resultSet.getString("skills");
            if (schemeName != null && skills != null) {
               int objectId = resultSet.getInt("object_id");
               String[] split = skills.split(",");
               List<Integer> schemeSkillIds = new ArrayList<>(split.length);

               for (String token : split) {
                  if (!token.isEmpty()) {
                     int skillId;
                     try {
                        skillId = Integer.parseInt(token);
                     } catch (NumberFormatException var19) {
                        LOGGER.warning("SchemeBufferTable: Invalid skill id '" + token + "' for scheme '" + schemeName + "' (objectId: " + objectId + ").");
                        continue;
                     }

                     if (this._availableBuffs.containsKey(skillId)) {
                        schemeSkillIds.add(skillId);
                     }
                  }
               }

               this.setScheme(objectId, schemeName, schemeSkillIds);
               entryCount++;
            }
         }

         LOGGER.info("SchemeBufferTable: Loaded " + entryCount + " scheme entries from database.");
      } catch (Exception var23) {
         LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load buff schemes from database.", (Throwable)var23);
      }
   }

   private static class SingletonHolder {
      protected static final SchemeBufferTable INSTANCE = new SchemeBufferTable();
   }
}
