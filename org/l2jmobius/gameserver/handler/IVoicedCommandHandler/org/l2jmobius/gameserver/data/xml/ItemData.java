package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.util.DocumentItem;

public class ItemData {
   private static final Logger LOGGER = Logger.getLogger(ItemData.class.getName());
   private ItemTemplate[] _allTemplates;
   private final Map<Integer, EtcItem> _etcItems = new HashMap<>();
   private final Map<Integer, Armor> _armors = new HashMap<>();
   private final Map<Integer, Weapon> _weapons = new HashMap<>();
   private final List<File> _itemFiles = new ArrayList<>();

   protected ItemData() {
      this.processDirectory("data/stats/items", this._itemFiles);
      if (GeneralConfig.CUSTOM_ITEMS_LOAD) {
         this.processDirectory("data/stats/items/custom", this._itemFiles);
      }

      this.load();
   }

   private void processDirectory(String dirName, List<File> list) {
      File dir = new File(ServerConfig.DATAPACK_ROOT, dirName);
      if (!dir.exists()) {
         LOGGER.warning("Directory " + dir.getAbsolutePath() + " does not exist.");
      } else {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                  list.add(file);
               }
            }
         }
      }
   }

   private void load() {
      Collection<ItemTemplate> items = ConcurrentHashMap.newKeySet();
      int highestId = 0;
      this._armors.clear();
      this._etcItems.clear();
      this._weapons.clear();
      if (ThreadConfig.THREADS_FOR_LOADING) {
         Collection<ScheduledFuture<?>> tasks = ConcurrentHashMap.newKeySet();

         for (File file : this._itemFiles) {
            tasks.add(ThreadPool.schedule(() -> {
               DocumentItem documentx = new DocumentItem(file);
               documentx.parse();
               items.addAll(documentx.getItemList());
            }, 0L));
         }

         while (!tasks.isEmpty()) {
            for (ScheduledFuture<?> task : tasks) {
               if (task == null || task.isDone() || task.isCancelled()) {
                  tasks.remove(task);
               }
            }
         }
      } else {
         for (File file : this._itemFiles) {
            DocumentItem document = new DocumentItem(file);
            document.parse();
            items.addAll(document.getItemList());
         }
      }

      for (ItemTemplate item : items) {
         if (highestId < item.getId()) {
            highestId = item.getId();
         }

         if (item instanceof EtcItem) {
            this._etcItems.put(item.getId(), (EtcItem)item);
         } else if (item instanceof Armor) {
            this._armors.put(item.getId(), (Armor)item);
         } else {
            this._weapons.put(item.getId(), (Weapon)item);
         }
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Highest item id used: " + highestId);
      this._allTemplates = new ItemTemplate[highestId + 1];

      for (Armor item : this._armors.values()) {
         this._allTemplates[item.getId()] = item;
      }

      for (Weapon item : this._weapons.values()) {
         this._allTemplates[item.getId()] = item;
      }

      for (EtcItem item : this._etcItems.values()) {
         this._allTemplates[item.getId()] = item;
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._etcItems.size() + " etc items.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._armors.size() + " armor items.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._weapons.size() + " weapon items.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + (this._etcItems.size() + this._armors.size() + this._weapons.size()) + " items in total.");
   }

   public void reload() {
      this.load();
      EnchantItemHPBonusData.getInstance().load();
   }

   public ItemTemplate getTemplate(int id) {
      return id < this._allTemplates.length && id >= 0 ? this._allTemplates[id] : null;
   }

   public Set<Integer> getAllArmorsId() {
      return this._armors.keySet();
   }

   public Collection<Armor> getAllArmors() {
      return this._armors.values();
   }

   public Set<Integer> getAllWeaponsId() {
      return this._weapons.keySet();
   }

   public Collection<Weapon> getAllWeapons() {
      return this._weapons.values();
   }

   public Set<Integer> getAllEtcItemsId() {
      return this._etcItems.keySet();
   }

   public Collection<EtcItem> getAllEtcItems() {
      return this._etcItems.values();
   }

   public ItemTemplate[] getAllItems() {
      return this._allTemplates;
   }

   public int getArraySize() {
      return this._allTemplates.length;
   }

   public static ItemData getInstance() {
      return ItemData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ItemData INSTANCE = new ItemData();
   }
}
