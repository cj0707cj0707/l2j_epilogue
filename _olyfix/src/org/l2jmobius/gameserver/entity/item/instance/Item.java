package org.l2jmobius.gameserver.entity.item.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemOptionsData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.holders.DropProtection;
import org.l2jmobius.gameserver.entity.item.holders.Elementals;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.MercTicketManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAugment;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemDrop;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory.OnPlayerItemPickup;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemBypassEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.mechanics.options.EnchantOptions;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DropItem;
import org.l2jmobius.gameserver.network.serverpackets.GetItem;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SpawnItem;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.ItemLifeTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemManaTaskManager;
import org.l2jmobius.gameserver.util.GMAudit;

public class Item extends WorldObject {
   private static final Logger LOGGER = Logger.getLogger(Item.class.getName());
   private static final Logger LOG_ITEMS = Logger.getLogger("item");
   private int _ownerId;
   private Player _owner;
   private int _dropperObjectId = 0;
   private long _count;
   private long _initCount;
   private long _time;
   private boolean _decrease = false;
   private final int _itemId;
   private final ItemTemplate _itemTemplate;
   private ItemLocation _loc;
   private int _locData;
   private int _enchantLevel;
   private boolean _wear;
   private Augmentation _augmentation = null;
   private int _mana = -1;
   private boolean _consumingMana = false;
   private int _type1;
   private int _type2;
   private long _dropTime;
   private boolean _published = false;
   private boolean _protected;
   public static final int UNCHANGED = 0;
   public static final int ADDED = 1;
   public static final int REMOVED = 3;
   public static final int MODIFIED = 2;
   public static final int[] DEFAULT_ENCHANT_OPTIONS = new int[]{0, 0, 0};
   private int _lastChange = 2;
   private boolean _existsInDb;
   private boolean _storedInDb;
   private final ReentrantLock _dbLock = new ReentrantLock();
   private Elementals[] _elementals = null;
   private ScheduledFuture<?> _itemLootShedule = null;
   private final DropProtection _dropProtection = new DropProtection();
   private int _shotsMask = 0;
   private final List<Options> _enchantOptions = new ArrayList<>();

   public Item(int objectId, int itemId) {
      super(objectId);
      this.setInstanceType(InstanceType.Item);
      this._itemId = itemId;
      this._itemTemplate = ItemData.getInstance().getTemplate(itemId);
      if (this._itemId != 0 && this._itemTemplate != null) {
         super.setName(this._itemTemplate.getName());
         this.setCount(1L);
         this._loc = ItemLocation.VOID;
         this._type1 = 0;
         this._type2 = 0;
         this._dropTime = 0L;
         this._mana = this._itemTemplate.getDuration();
         this._time = this._itemTemplate.getTime() == -1L ? -1L : System.currentTimeMillis() + this._itemTemplate.getTime() * 60L * 1000L;
         this._enchantLevel = 0;
         this.scheduleLifeTimeTask();
      } else {
         throw new IllegalArgumentException();
      }
   }

   public Item(int objectId, ItemTemplate itemTemplate) {
      super(objectId);
      this.setInstanceType(InstanceType.Item);
      this._itemId = itemTemplate.getId();
      this._itemTemplate = itemTemplate;
      if (this._itemId == 0) {
         throw new IllegalArgumentException();
      } else {
         super.setName(this._itemTemplate.getName());
         this.setCount(1L);
         this._loc = ItemLocation.VOID;
         this._mana = this._itemTemplate.getDuration();
         this._time = this._itemTemplate.getTime() == -1L ? -1L : System.currentTimeMillis() + this._itemTemplate.getTime() * 60L * 1000L;
         this.scheduleLifeTimeTask();
      }
   }

   public Item(int itemId) {
      this(IdManager.getInstance().getNextId(), itemId);
   }

   public void pickupMe(Creature creature) {
      WorldRegion oldregion = this.getWorldRegion();
      creature.broadcastPacket(new GetItem(this, creature.getObjectId()));
      synchronized (this) {
         this.setSpawned(false);
      }

      if (MercTicketManager.getInstance().getTicketCastleId(this._itemId) > 0) {
         MercTicketManager.getInstance().removeTicket(this);
         ItemsOnGroundManager.getInstance().removeObject(this);
      }

      World.removeVisibleObject(this, oldregion);
      this.setWorldRegion(null);
      if (creature.isPlayer()) {
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_PICKUP, this.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemPickup(creature.asPlayer(), this), this.getTemplate());
         } else if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_PICKUP, creature.asPlayer())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemPickup(creature.asPlayer(), this), creature.asPlayer());
         }
      }
   }

   public void setOwnerId(ItemProcessType process, int ownerId, Player creator, Object reference) {
      this.setOwnerId(ownerId);
      if (GeneralConfig.LOG_ITEMS && !GeneralConfig.LOG_ITEMS_SMALL_LOG && !GeneralConfig.LOG_ITEMS_IDS_ONLY
         || GeneralConfig.LOG_ITEMS_SMALL_LOG && (this._itemTemplate.isEquipable() || this._itemTemplate.getId() == 57)
         || GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(this._itemTemplate.getId())) {
         if (this._enchantLevel > 0) {
            LOG_ITEMS.info(
               StringUtil.concat(
                  "SETOWNER:",
                  String.valueOf(process),
                  ", item ",
                  String.valueOf(this.getObjectId()),
                  ":+",
                  String.valueOf(this._enchantLevel),
                  " ",
                  this._itemTemplate.getName(),
                  "(",
                  String.valueOf(this._count),
                  "), ",
                  String.valueOf(creator),
                  ", ",
                  String.valueOf(reference)
               )
            );
         } else {
            LOG_ITEMS.info(
               StringUtil.concat(
                  "SETOWNER:",
                  String.valueOf(process),
                  ", item ",
                  String.valueOf(this.getObjectId()),
                  ":",
                  this._itemTemplate.getName(),
                  "(",
                  String.valueOf(this._count),
                  "), ",
                  String.valueOf(creator),
                  ", ",
                  String.valueOf(reference)
               )
            );
         }
      }

      if (creator != null && creator.isGM() && GeneralConfig.GMAUDIT) {
         String targetName = creator.getTarget() != null ? creator.getTarget().getName() : "no-target";
         String referenceName = "no-reference";
         if (reference instanceof WorldObject) {
            referenceName = ((WorldObject)reference).getName() != null ? ((WorldObject)reference).getName() : "no-name";
         } else if (reference instanceof String) {
            referenceName = (String)reference;
         }

         GMAudit.logAction(
            creator.toString(),
            StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(this._itemId), " name: ", this.getName(), ")"),
            targetName,
            StringUtil.concat("Object referencing this action is: ", referenceName)
         );
      }
   }

   public void setOwnerId(int ownerId) {
      if (ownerId != this._ownerId) {
         this.removeSkillsFromOwner();
         this._owner = null;
         this._ownerId = ownerId;
         this._storedInDb = false;
         this.giveSkillsToOwner();
      }
   }

   public int getOwnerId() {
      return this._ownerId;
   }

   public void setItemLocation(ItemLocation loc) {
      this.setItemLocation(loc, 0, true);
   }

   public void setItemLocation(ItemLocation loc, int locData) {
      this.setItemLocation(loc, locData, true);
   }

   public void setItemLocation(ItemLocation loc, int locData, boolean checkSkills) {
      if (loc != this._loc || locData != this._locData) {
         if (checkSkills) {
            this.removeSkillsFromOwner();
         }

         this._loc = loc;
         this._locData = locData;
         this._storedInDb = false;
         if (checkSkills) {
            this.giveSkillsToOwner();
         }
      }
   }

   public ItemLocation getItemLocation() {
      return this._loc;
   }

   public void setCount(long count) {
      if (this._count != count) {
         this._count = count >= -1L ? count : 0L;
         this._storedInDb = false;
      }
   }

   public long getCount() {
      return this._count;
   }

   public void changeCount(ItemProcessType process, long count, Player creator, Object reference) {
      if (count != 0L) {
         long old = this._count;
         long max = this._itemId == 57 ? Inventory.MAX_ADENA : Long.MAX_VALUE;
         if (count > 0L && this._count > max - count) {
            this.setCount(max);
         } else {
            this.setCount(this._count + count);
         }

         if (this._count < 0L) {
            this.setCount(0L);
         }

         this._storedInDb = false;
         if (process != null && process != ItemProcessType.NONE) {
            if (GeneralConfig.LOG_ITEMS && !GeneralConfig.LOG_ITEMS_SMALL_LOG && !GeneralConfig.LOG_ITEMS_IDS_ONLY
               || GeneralConfig.LOG_ITEMS_SMALL_LOG && (this._itemTemplate.isEquipable() || this._itemTemplate.getId() == 57)
               || GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(this._itemTemplate.getId())) {
               if (this._enchantLevel > 0) {
                  LOG_ITEMS.info(
                     StringUtil.concat(
                        "CHANGE:",
                        String.valueOf(process),
                        ", item ",
                        String.valueOf(this.getObjectId()),
                        ":+",
                        String.valueOf(this._enchantLevel),
                        " ",
                        this._itemTemplate.getName(),
                        "(",
                        String.valueOf(this._count),
                        "), PrevCount(",
                        String.valueOf(old),
                        "), ",
                        String.valueOf(creator),
                        ", ",
                        String.valueOf(reference)
                     )
                  );
               } else {
                  LOG_ITEMS.info(
                     StringUtil.concat(
                        "CHANGE:",
                        String.valueOf(process),
                        ", item ",
                        String.valueOf(this.getObjectId()),
                        ":",
                        this._itemTemplate.getName(),
                        "(",
                        String.valueOf(this._count),
                        "), PrevCount(",
                        String.valueOf(old),
                        "), ",
                        String.valueOf(creator),
                        ", ",
                        String.valueOf(reference)
                     )
                  );
               }
            }

            if (creator != null && creator.isGM() && GeneralConfig.GMAUDIT) {
               String targetName = creator.getTarget() != null ? creator.getTarget().getName() : "no-target";
               String referenceName = "no-reference";
               if (reference instanceof WorldObject) {
                  referenceName = ((WorldObject)reference).getName() != null ? ((WorldObject)reference).getName() : "no-name";
               } else if (reference instanceof String) {
                  referenceName = (String)reference;
               }

               GMAudit.logAction(
                  creator.toString(),
                  StringUtil.concat(
                     String.valueOf(process),
                     "(id: ",
                     String.valueOf(this._itemId),
                     " objId: ",
                     String.valueOf(this.getObjectId()),
                     " name: ",
                     this.getName(),
                     " count: ",
                     String.valueOf(count),
                     ")"
                  ),
                  targetName,
                  StringUtil.concat("Object referencing this action is: ", referenceName)
               );
            }
         }
      }
   }

   public boolean isEnchantable() {
      return this._loc != ItemLocation.INVENTORY && this._loc != ItemLocation.PAPERDOLL ? false : this._itemTemplate.isEnchantable();
   }

   public boolean isEquipable() {
      return this._itemTemplate.getBodyPart() != BodyPart.NONE
         && this._itemTemplate.getItemType() != EtcItemType.ARROW
         && this._itemTemplate.getItemType() != EtcItemType.BOLT
         && this._itemTemplate.getItemType() != EtcItemType.LURE;
   }

   public boolean isEquipped() {
      return this._loc == ItemLocation.PAPERDOLL || this._loc == ItemLocation.PET_EQUIP;
   }

   public int getLocationSlot() {
      return this._locData;
   }

   public ItemTemplate getTemplate() {
      return this._itemTemplate;
   }

   public int getCustomType1() {
      return this._type1;
   }

   public int getCustomType2() {
      return this._type2;
   }

   public void setCustomType1(int newtype) {
      this._type1 = newtype;
   }

   public void setCustomType2(int newtype) {
      this._type2 = newtype;
   }

   public void setDropTime(long time) {
      this._dropTime = time;
   }

   public long getDropTime() {
      return this._dropTime;
   }

   public ItemType getItemType() {
      return this._itemTemplate.getItemType();
   }

   @Override
   public int getId() {
      return this._itemId;
   }

   public int getDisplayId() {
      return this._itemTemplate.getDisplayId();
   }

   public boolean isEtcItem() {
      return this._itemTemplate instanceof EtcItem;
   }

   public boolean isWeapon() {
      return this._itemTemplate instanceof Weapon;
   }

   public boolean isArmor() {
      return this._itemTemplate instanceof Armor;
   }

   public EtcItem getEtcItem() {
      return this._itemTemplate instanceof EtcItem ? (EtcItem)this._itemTemplate : null;
   }

   public Weapon getWeaponItem() {
      return this._itemTemplate instanceof Weapon ? (Weapon)this._itemTemplate : null;
   }

   public Armor getArmorItem() {
      return this._itemTemplate instanceof Armor ? (Armor)this._itemTemplate : null;
   }

   public int getCrystalCount() {
      return this._itemTemplate.getCrystalCount(this._enchantLevel);
   }

   public int getReferencePrice() {
      return this._itemTemplate.getReferencePrice();
   }

   public String getItemName() {
      return this._itemTemplate.getName();
   }

   public int getReuseDelay() {
      return this._itemTemplate.getReuseDelay();
   }

   public int getSharedReuseGroup() {
      return this._itemTemplate.getSharedReuseGroup();
   }

   public int getLastChange() {
      return this._lastChange;
   }

   public void setLastChange(int lastChange) {
      this._lastChange = lastChange;
   }

   public boolean isStackable() {
      return this._itemTemplate.isStackable();
   }

   public boolean isDropable() {
      if (!this._itemTemplate.isDropable()) {
         return false;
      } else if (this.isEquipable() && this.getTransmogId() > 0) {
         return false;
      } else {
         return this.isAugmented() ? PlayerConfig.ALT_ALLOW_AUGMENT_TRADE : true;
      }
   }

   public boolean isDestroyable() {
      if (!this._itemTemplate.isDestroyable()) {
         return false;
      } else {
         return this.isAugmented() ? PlayerConfig.ALT_ALLOW_AUGMENT_DESTROY : true;
      }
   }

   public boolean isTradeable() {
      if (!this._itemTemplate.isTradeable()) {
         return false;
      } else if (this.isEquipable() && this.getTransmogId() > 0) {
         return false;
      } else {
         return this.isAugmented() ? PlayerConfig.ALT_ALLOW_AUGMENT_TRADE : true;
      }
   }

   public boolean isSellable() {
      if (!this._itemTemplate.isSellable()) {
         return false;
      } else if (this.isEquipable() && this.getTransmogId() > 0) {
         return false;
      } else {
         return this.isAugmented() ? PlayerConfig.ALT_ALLOW_AUGMENT_TRADE : true;
      }
   }

   public boolean isDepositable(boolean isPrivateWareHouse) {
      return this._itemTemplate.isDepositable() && !this.isEquipped() ? isPrivateWareHouse || this.isTradeable() && !this.isShadowItem() : false;
   }

   public boolean isPotion() {
      return this._itemTemplate.isPotion();
   }

   public boolean isElixir() {
      return this._itemTemplate.isElixir();
   }

   public boolean isScroll() {
      return this._itemTemplate.isScroll();
   }

   public boolean isHeroItem() {
      return this._itemTemplate.isHeroItem();
   }

   public boolean isCommonItem() {
      return this._itemTemplate.isCommon();
   }

   public boolean isPvp() {
      return this._itemTemplate.isPvpItem();
   }

   public boolean isOlyRestrictedItem() {
      return this._itemTemplate.isOlyRestrictedItem();
   }

   public boolean isAvailable(Player player, boolean allowAdena, boolean allowNonTradeable) {
      return !this.isEquipped()
         && this._itemTemplate.getType2() != 3
         && (this._itemTemplate.getType2() != 4 || this._itemTemplate.getType1() != 1)
         && (!player.hasSummon() || this.getObjectId() != player.getSummon().getControlObjectId())
         && player.getActiveEnchantItemId() != this.getObjectId()
         && player.getActiveEnchantSupportItemId() != this.getObjectId()
         && player.getActiveEnchantAttrItemId() != this.getObjectId()
         && (allowAdena || this._itemId != 57)
         && (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != this._itemId)
         && (
            !player.isCastingSimultaneouslyNow()
               || player.getLastSimultaneousSkillCast() == null
               || player.getLastSimultaneousSkillCast().getItemConsumeId() != this._itemId
         )
         && (allowNonTradeable || this.isTradeable() && (this._itemTemplate.getItemType() != EtcItemType.PET_COLLAR || !player.havePetInvItems()));
   }

   public int getEnchantLevel() {
      return this._enchantLevel;
   }

   public boolean isEnchanted() {
      return this._enchantLevel > 0;
   }

   public void setEnchantLevel(int level) {
      int newLevel = Math.max(0, level);
      if (this._enchantLevel != newLevel) {
         this.clearEnchantStats();
         this._enchantLevel = newLevel;
         this.applyEnchantStats();
         this._storedInDb = false;
      }
   }

   public boolean isAugmented() {
      return this._augmentation != null;
   }

   public Augmentation getAugmentation() {
      return this._augmentation;
   }

   public boolean setAugmentation(Augmentation augmentation) {
      if (this._augmentation != null) {
         LOGGER.info("Warning: Augment set for (" + this.getObjectId() + ") " + this.getName() + " owner: " + this._ownerId);
         return false;
      } else {
         this._augmentation = augmentation;

         try (Connection con = DatabaseFactory.getConnection()) {
            this.updateItemAttributes(con);
         } catch (SQLException var7) {
            LOGGER.log(Level.SEVERE, "Could not update atributes for item: " + this + " from DB:", (Throwable)var7);
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_AUGMENT, this.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(this.asPlayer(), this, augmentation, true), this.getTemplate());
         }

         return true;
      }
   }

   public void removeAugmentation() {
      if (this._augmentation != null) {
         Augmentation augment = this._augmentation;
         this._augmentation = null;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
         ) {
            ps.setInt(1, this.getObjectId());
            ps.executeUpdate();
         } catch (Exception var10) {
            LOGGER.log(Level.SEVERE, "Could not remove augmentation for item: " + this + " from DB:", (Throwable)var10);
         }

         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_AUGMENT, this.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(this.asPlayer(), this, augment, false), this.getTemplate());
         }
      }
   }

   public void restoreAttributes() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement("SELECT augAttributes FROM item_attributes WHERE itemId=?");
         PreparedStatement ps2 = con.prepareStatement("SELECT elemType,elemValue FROM item_elementals WHERE itemId=?");
      ) {
         ps1.setInt(1, this.getObjectId());

         try (ResultSet rs = ps1.executeQuery()) {
            if (rs.next()) {
               int aug_attributes = rs.getInt(1);
               if (aug_attributes != -1) {
                  this._augmentation = new Augmentation(rs.getInt("augAttributes"));
               }
            }
         }

         ps2.setInt(1, this.getObjectId());

         try (ResultSet rsx = ps2.executeQuery()) {
            while (rsx.next()) {
               byte elem_type = rsx.getByte(1);
               int elem_value = rsx.getInt(2);
               if (elem_type != -1 && elem_value != -1) {
                  this.applyAttribute(elem_type, elem_value);
               }
            }
         }
      } catch (Exception var17) {
         LOGGER.log(Level.SEVERE, "Could not restore augmentation and elemental data for item " + this + " from DB: " + var17.getMessage(), (Throwable)var17);
      }
   }

   private void updateItemAttributes(Connection con) {
      try (PreparedStatement ps = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?)")) {
         ps.setInt(1, this.getObjectId());
         ps.setInt(2, this._augmentation != null ? this._augmentation.getAugmentationId() : -1);
         ps.executeUpdate();
      } catch (SQLException var7) {
         LOGGER.log(Level.SEVERE, "Could not update atributes for item: " + this + " from DB:", (Throwable)var7);
      }
   }

   private void updateItemElements(Connection con) {
      try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?")) {
         ps.setInt(1, this.getObjectId());
         ps.executeUpdate();
      } catch (SQLException var12) {
         LOGGER.log(Level.SEVERE, "Could not update elementals for item: " + this + " from DB:", (Throwable)var12);
      }

      if (this._elementals != null) {
         try (PreparedStatement ps = con.prepareStatement("INSERT INTO item_elementals VALUES(?,?,?)")) {
            for (Elementals elm : this._elementals) {
               ps.setInt(1, this.getObjectId());
               ps.setByte(2, elm.getElement());
               ps.setInt(3, elm.getValue());
               ps.executeUpdate();
               ps.clearParameters();
            }
         } catch (SQLException var10) {
            LOGGER.log(Level.SEVERE, "Could not update elementals for item: " + this + " from DB:", (Throwable)var10);
         }
      }
   }

   public Elementals[] getElementals() {
      return this._elementals;
   }

   public Elementals getElemental(byte attribute) {
      if (this._elementals == null) {
         return null;
      } else {
         for (Elementals elm : this._elementals) {
            if (elm.getElement() == attribute) {
               return elm;
            }
         }

         return null;
      }
   }

   public byte getAttackElementType() {
      if (!this.isWeapon()) {
         return -2;
      } else if (this._itemTemplate.getElementals() != null) {
         return this._itemTemplate.getElementals()[0].getElement();
      } else {
         return this._elementals != null ? this._elementals[0].getElement() : -2;
      }
   }

   public int getAttackElementPower() {
      if (!this.isWeapon()) {
         return 0;
      } else if (this._itemTemplate.getElementals() != null) {
         return this._itemTemplate.getElementals()[0].getValue();
      } else {
         return this._elementals != null ? this._elementals[0].getValue() : 0;
      }
   }

   public int getElementDefAttr(byte element) {
      if (!this.isArmor()) {
         return 0;
      } else {
         if (this._itemTemplate.getElementals() != null) {
            Elementals elm = this._itemTemplate.getElemental(element);
            if (elm != null) {
               return elm.getValue();
            }
         } else if (this._elementals != null) {
            Elementals elm = this.getElemental(element);
            if (elm != null) {
               return elm.getValue();
            }
         }

         return 0;
      }
   }

   private void applyAttribute(byte element, int value) {
      if (this._elementals == null) {
         this._elementals = new Elementals[1];
         this._elementals[0] = new Elementals(element, value);
      } else {
         Elementals elm = this.getElemental(element);
         if (elm != null) {
            elm.setValue(value);
         } else {
            elm = new Elementals(element, value);
            Elementals[] array = new Elementals[this._elementals.length + 1];
            System.arraycopy(this._elementals, 0, array, 0, this._elementals.length);
            array[this._elementals.length] = elm;
            this._elementals = array;
         }
      }
   }

   public void setElementAttr(byte element, int value) {
      this.applyAttribute(element, value);

      try (Connection con = DatabaseFactory.getConnection()) {
         this.updateItemElements(con);
      } catch (SQLException var8) {
         LOGGER.log(Level.SEVERE, "Could not update elementals for item: " + this + " from DB:", (Throwable)var8);
      }
   }

   public void clearElementAttr(byte element) {
      if (this.getElemental(element) != null || element == -1) {
         Elementals[] array = null;
         if (element != -1 && this._elementals != null && this._elementals.length > 1) {
            array = new Elementals[this._elementals.length - 1];
            int i = 0;

            for (Elementals elm : this._elementals) {
               if (elm.getElement() != element) {
                  array[i++] = elm;
               }
            }
         }

         this._elementals = array;
         String query = element != -1 ? "DELETE FROM item_elementals WHERE itemId = ? AND elemType = ?" : "DELETE FROM item_elementals WHERE itemId = ?";

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query);
         ) {
            if (element != -1) {
               ps.setInt(2, element);
            }

            ps.setInt(1, this.getObjectId());
            ps.executeUpdate();
         } catch (Exception var12) {
            LOGGER.log(Level.SEVERE, "Could not remove elemental enchant for item: " + this + " from DB:", (Throwable)var12);
         }
      }
   }

   public boolean isShadowItem() {
      return this._mana >= 0;
   }

   public int getMana() {
      return this._mana;
   }

   public void decreaseMana(boolean resetConsumingMana) {
      this.decreaseMana(resetConsumingMana, 1);
   }

   public void decreaseMana(boolean resetConsumingMana, int count) {
      if (this.isShadowItem()) {
         if (this._mana - count >= 0) {
            this._mana -= count;
         } else {
            this._mana = 0;
         }

         if (this._storedInDb) {
            this._storedInDb = false;
         }

         if (resetConsumingMana) {
            this._consumingMana = false;
         }

         Player player = this.asPlayer();
         if (player != null) {
            switch (this._mana) {
               case 1: {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_1_IT_WILL_DISAPPEAR_SOON);
                  sm.addItemName(this._itemTemplate);
                  player.sendPacket(sm);
                  break;
               }
               case 5: {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_5);
                  sm.addItemName(this._itemTemplate);
                  player.sendPacket(sm);
                  break;
               }
               case 10: {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_10);
                  sm.addItemName(this._itemTemplate);
                  player.sendPacket(sm);
               }
            }

            if (this._mana == 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_S_REMAINING_MANA_IS_NOW_0_AND_THE_ITEM_HAS_DISAPPEARED);
               sm.addItemName(this._itemTemplate);
               player.sendPacket(sm);
               if (this.isEquipped()) {
                  InventoryUpdate iu = new InventoryUpdate();

                  for (Item item : player.getInventory().unEquipItemInSlotAndRecord(this.getLocationSlot())) {
                     item.unChargeAllShots();
                     iu.addModifiedItem(item);
                  }

                  player.sendInventoryUpdate(iu);
                  player.broadcastUserInfo();
               }

               if (this._loc != ItemLocation.WAREHOUSE) {
                  player.getInventory().destroyItem(ItemProcessType.DESTROY, this, player, null);
                  InventoryUpdate iu = new InventoryUpdate();
                  iu.addRemovedItem(this);
                  player.sendInventoryUpdate(iu);
                  StatusUpdate su = new StatusUpdate(player);
                  su.addAttribute(14, player.getCurrentLoad());
                  player.sendPacket(su);
               } else {
                  player.getWarehouse().destroyItem(ItemProcessType.DESTROY, this, player, null);
               }
            } else {
               if (!this._consumingMana && this.isEquipped()) {
                  this.scheduleConsumeManaTask();
               }

               if (this._loc != ItemLocation.WAREHOUSE) {
                  InventoryUpdate iu = new InventoryUpdate();
                  iu.addModifiedItem(this);
                  player.sendInventoryUpdate(iu);
               }
            }
         }
      }
   }

   public void scheduleConsumeManaTask() {
      if (!this._consumingMana) {
         this._consumingMana = true;
         ItemManaTaskManager.getInstance().add(this);
      }
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return false;
   }

   public List<AbstractFunction> getStatFuncs(Creature creature) {
      return this._itemTemplate.getStatFuncs(this, creature);
   }

   public void updateDatabase() {
      this.updateDatabase(false);
   }

   public void updateDatabase(boolean force) {
      this._dbLock.lock();

      try {
         if (this._existsInDb) {
            if (this._ownerId != 0
               && this._loc != ItemLocation.VOID
               && this._loc != ItemLocation.REFUND
               && (this._count != 0L || this._loc == ItemLocation.LEASE)) {
               if (!GeneralConfig.LAZY_ITEMS_UPDATE || force) {
                  this.updateInDb();
               }
            } else {
               this.removeFromDb();
            }

            return;
         }

         if (this._ownerId != 0 && this._loc != ItemLocation.VOID && this._loc != ItemLocation.REFUND && (this._count != 0L || this._loc == ItemLocation.LEASE)
            )
          {
            this.insertIntoDb();
            return;
         }
      } finally {
         this._dbLock.unlock();
      }
   }

   public static Item restoreFromDb(int ownerId, ResultSet rs) {
      Item inst = null;

      int objectId;
      int itemId;
      int locData;
      int enchantLevel;
      int customType1;
      int customType2;
      int manaLeft;
      long time;
      long count;
      ItemLocation loc;
      try {
         objectId = rs.getInt(1);
         itemId = rs.getInt("item_id");
         count = rs.getLong("count");
         loc = ItemLocation.valueOf(rs.getString("loc"));
         locData = rs.getInt("loc_data");
         enchantLevel = rs.getInt("enchant_level");
         customType1 = rs.getInt("custom_type1");
         customType2 = rs.getInt("custom_type2");
         manaLeft = rs.getInt("mana_left");
         time = rs.getLong("time");
      } catch (Exception var16) {
         LOGGER.log(Level.SEVERE, "Could not restore an item owned by " + ownerId + " from DB:", (Throwable)var16);
         return null;
      }

      ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
      if (item == null) {
         LOGGER.severe("Item item_id=" + itemId + " not known, object_id=" + objectId);
         return null;
      } else {
         inst = new Item(objectId, item);
         inst._ownerId = ownerId;
         inst.setCount(count);
         inst._enchantLevel = enchantLevel;
         inst._type1 = customType1;
         inst._type2 = customType2;
         inst._loc = loc;
         inst._locData = locData;
         inst._existsInDb = true;
         inst._storedInDb = true;
         inst._mana = manaLeft;
         inst._time = time;
         if (inst.isEquipable()) {
            inst.restoreAttributes();
         }

         return inst;
      }
   }

   public void dropMe(Creature dropper, int locX, int locY, int locZ) {
      int x = locX;
      int y = locY;
      int z = locZ;
      if (dropper != null) {
         Location dropDest = GeoEngine.getInstance()
            .getValidLocation(dropper.getX(), dropper.getY(), dropper.getZ(), locX, locY, locZ, dropper.getInstanceId());
         x = dropDest.getX();
         y = dropDest.getY();
         z = dropDest.getZ();
      }

      if (dropper != null) {
         this.setInstanceId(dropper.getInstanceId());
      } else {
         this.setInstanceId(0);
      }

      this.setSpawned(true);
      this.setXYZ(x, y, z);
      this.setDropTime(System.currentTimeMillis());
      this.setDropperObjectId(dropper != null ? dropper.getObjectId() : 0);
      WorldRegion region = this.getWorldRegion();
      region.addVisibleObject(this);
      World.addVisibleObject(this, region);
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         ItemsOnGroundManager.getInstance().save(this);
      }

      this.setDropperObjectId(0);
      if (dropper != null && dropper.isPlayer()) {
         this._owner = null;
         if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, this.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(dropper.asPlayer(), this, new Location(x, y, z)), this.getTemplate());
         }
      }
   }

   private void updateInDb() {
      if (this._existsInDb && !this._wear && !this._storedInDb) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=? WHERE object_id = ?"
            );
         ) {
            ps.setInt(1, this._ownerId);
            ps.setLong(2, this._count);
            ps.setString(3, this._loc.name());
            ps.setInt(4, this._locData);
            ps.setInt(5, this._enchantLevel);
            ps.setInt(6, this._type1);
            ps.setInt(7, this._type2);
            ps.setInt(8, this._mana);
            ps.setLong(9, this._time);
            ps.setInt(10, this.getObjectId());
            ps.executeUpdate();
            this._existsInDb = true;
            this._storedInDb = true;
         } catch (Exception var9) {
            LOGGER.log(Level.SEVERE, "Could not update item " + this + " in DB: Reason: " + var9.getMessage(), (Throwable)var9);
         }
      }
   }

   private void insertIntoDb() {
      if (!this._existsInDb && this.getObjectId() != 0 && !this._wear) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time) VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            );
         ) {
            ps.setInt(1, this._ownerId);
            ps.setInt(2, this._itemId);
            ps.setLong(3, this._count);
            ps.setString(4, this._loc.name());
            ps.setInt(5, this._locData);
            ps.setInt(6, this._enchantLevel);
            ps.setInt(7, this.getObjectId());
            ps.setInt(8, this._type1);
            ps.setInt(9, this._type2);
            ps.setInt(10, this._mana);
            ps.setLong(11, this._time);
            ps.executeUpdate();
            this._existsInDb = true;
            this._storedInDb = true;
            if (this._augmentation != null) {
               this.updateItemAttributes(con);
            }

            if (this._elementals != null) {
               this.updateItemElements(con);
            }
         } catch (Exception var9) {
            LOGGER.log(Level.SEVERE, "Could not insert item " + this + " into DB: Reason: " + var9.getMessage(), (Throwable)var9);
         }
      }
   }

   private void removeFromDb() {
      if (this._existsInDb && !this._wear) {
         try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE object_id = ?")) {
               ps.setInt(1, this.getObjectId());
               ps.executeUpdate();
               this._existsInDb = false;
               this._storedInDb = false;
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?")) {
               ps.setInt(1, this.getObjectId());
               ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_elementals WHERE itemId = ?")) {
               ps.setInt(1, this.getObjectId());
               ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_variables WHERE id = ?")) {
               ps.setInt(1, this.getObjectId());
               ps.executeUpdate();
            }
         } catch (Exception var15) {
            LOGGER.log(Level.SEVERE, "Could not delete item " + this + " in DB: " + var15.getMessage(), (Throwable)var15);
         }
      }
   }

   public void resetOwnerTimer() {
      if (this._itemLootShedule != null) {
         this._itemLootShedule.cancel(true);
         this._itemLootShedule = null;
      }
   }

   public void setItemLootShedule(ScheduledFuture<?> sf) {
      this._itemLootShedule = sf;
   }

   public ScheduledFuture<?> getItemLootShedule() {
      return this._itemLootShedule;
   }

   public void setProtected(boolean isProtected) {
      this._protected = isProtected;
   }

   public boolean isProtected() {
      return this._protected;
   }

   public boolean isNightLure() {
      return this._itemId >= 8505 && this._itemId <= 8513 || this._itemId == 8485;
   }

   public void setCountDecrease(boolean decrease) {
      this._decrease = decrease;
   }

   public boolean getCountDecrease() {
      return this._decrease;
   }

   public void setInitCount(int initCount) {
      this._initCount = initCount;
   }

   public long getInitCount() {
      return this._initCount;
   }

   public void restoreInitCount() {
      if (this._decrease) {
         this.setCount(this._initCount);
      }
   }

   public boolean isTimeLimitedItem() {
      return this._time > 0L;
   }

   public long getTime() {
      return this._time;
   }

   public long getRemainingTime() {
      return this._time - System.currentTimeMillis();
   }

   public void endOfLife() {
      Player player = this.asPlayer();
      if (player != null) {
         if (this.isEquipped()) {
            InventoryUpdate iu = new InventoryUpdate();

            for (Item item : player.getInventory().unEquipItemInSlotAndRecord(this.getLocationSlot())) {
               item.unChargeAllShots();
               iu.addModifiedItem(item);
            }

            player.sendInventoryUpdate(iu);
            player.broadcastUserInfo();
         }

         if (this._loc != ItemLocation.WAREHOUSE) {
            player.getInventory().destroyItem(ItemProcessType.DESTROY, this, player, null);
            InventoryUpdate iu = new InventoryUpdate();
            iu.addRemovedItem(this);
            player.sendInventoryUpdate(iu);
            StatusUpdate su = new StatusUpdate(player);
            su.addAttribute(14, player.getCurrentLoad());
            player.sendPacket(su);
         } else {
            player.getWarehouse().destroyItem(ItemProcessType.DESTROY, this, player, null);
         }

         player.sendPacket(SystemMessageId.THE_LIMITED_TIME_ITEM_HAS_BEEN_DELETED);
      }
   }

   public void scheduleLifeTimeTask() {
      if (this.isTimeLimitedItem()) {
         if (this.getRemainingTime() <= 0L) {
            this.endOfLife();
         } else {
            ItemLifeTimeTaskManager.getInstance().add(this, this.getTime());
         }
      }
   }

   public void updateElementAttrBonus(Player player) {
      if (this._elementals != null) {
         for (Elementals elm : this._elementals) {
            elm.updateBonus(player, this.isArmor());
         }
      }
   }

   public void removeElementAttrBonus(Player player) {
      if (this._elementals != null) {
         for (Elementals elm : this._elementals) {
            elm.removeBonus(player);
         }
      }
   }

   public void setDropperObjectId(int id) {
      this._dropperObjectId = id;
   }

   @Override
   public void sendInfo(Player player) {
      if (this._dropperObjectId != 0) {
         player.sendPacket(new DropItem(this, this._dropperObjectId));
      } else {
         player.sendPacket(new SpawnItem(this));
      }
   }

   public DropProtection getDropProtection() {
      return this._dropProtection;
   }

   public boolean isPublished() {
      return this._published;
   }

   public void publish() {
      this._published = true;
   }

   @Override
   public boolean decayMe() {
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         ItemsOnGroundManager.getInstance().removeObject(this);
      }

      return super.decayMe();
   }

   public boolean isQuestItem() {
      return this._itemTemplate.isQuestItem();
   }

   public boolean isElementable() {
      return (this._loc == ItemLocation.INVENTORY || this._loc == ItemLocation.PAPERDOLL) && this._itemTemplate.isElementable();
   }

   public boolean isFreightable() {
      return this._itemTemplate.isFreightable();
   }

   public int useSkillDisTime() {
      return this._itemTemplate.useSkillDisTime();
   }

   public int getOlyEnchantLevel() {
      Player player = this.asPlayer();
      int enchant = this._enchantLevel;
      if (player == null) {
         return enchant;
      } else {
         if (player.isInOlympiadMode() && OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT >= 0 && enchant > OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT) {
            enchant = OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT;
         }

         return enchant;
      }
   }

   public int getDefaultEnchantLevel() {
      return this._itemTemplate.getDefaultEnchantLevel();
   }

   public boolean hasPassiveSkills() {
      return this._itemTemplate.getItemType() == EtcItemType.RUNE && this._loc == ItemLocation.INVENTORY && this._ownerId > 0 && this._itemTemplate.hasSkills();
   }

   public void giveSkillsToOwner() {
      if (this._itemTemplate.hasSkills()) {
         if (this.isEquipped() || this.hasPassiveSkills()) {
            Player player = this.asPlayer();
            if (player != null) {
               for (SkillHolder holder : this._itemTemplate.getSkills()) {
                  Skill skill = holder.getSkill();
                  if (skill.isPassive()) {
                     player.addSkill(skill, false);
                  }
               }
            }
         }
      }
   }

   public void removeSkillsFromOwner() {
      if (this.hasPassiveSkills()) {
         Player player = this.asPlayer();
         if (player != null) {
            for (SkillHolder holder : this._itemTemplate.getSkills()) {
               Skill skill = holder.getSkill();
               if (skill.isPassive()) {
                  player.removeSkill(skill, false, true);
               }
            }
         }
      }
   }

   @Override
   public boolean isItem() {
      return true;
   }

   @Override
   public Player asPlayer() {
      if (this._owner == null && this._ownerId != 0) {
         this._owner = World.getPlayer(this._ownerId);
      }

      return this._owner;
   }

   public int getEquipReuseDelay() {
      return this._itemTemplate.getEquipReuseDelay();
   }

   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("Script")) {
         String questName = command.substring(6);
         String event = null;
         int idx = questName.indexOf(32);
         if (idx > 0) {
            event = questName.substring(idx).trim();
         }

         if (event != null) {
            if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_BYPASS_EVENT, this.getTemplate())) {
               EventDispatcher.getInstance().notifyEventAsync(new OnItemBypassEvent(this, player, event), this.getTemplate());
            }
         } else if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_TALK, this.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnItemTalk(this, player), this.getTemplate());
         }
      }
   }

   @Override
   public boolean isChargedShot(ShotType type) {
      return (this._shotsMask & type.getMask()) == type.getMask();
   }

   @Override
   public void setChargedShot(ShotType type, boolean charged) {
      if (charged) {
         this._shotsMask = this._shotsMask | type.getMask();
      } else {
         this._shotsMask = this._shotsMask & ~type.getMask();
      }
   }

   public void unChargeAllShots() {
      this._shotsMask = 0;
   }

   public int[] getEnchantOptions() {
      EnchantOptions op = EnchantItemOptionsData.getInstance().getOptions(this);
      return op != null ? op.getOptions() : DEFAULT_ENCHANT_OPTIONS;
   }

   public void clearEnchantStats() {
      Player player = this.asPlayer();
      if (player == null) {
         this._enchantOptions.clear();
      } else {
         for (Options op : this._enchantOptions) {
            op.remove(player);
         }

         this._enchantOptions.clear();
      }
   }

   public void applyEnchantStats() {
      Player player = this.asPlayer();
      if (this.isEquipped() && player != null && this.getEnchantOptions() != DEFAULT_ENCHANT_OPTIONS) {
         for (int id : this.getEnchantOptions()) {
            Options options = OptionData.getInstance().getOptions(id);
            if (options != null) {
               options.apply(player);
               this._enchantOptions.add(options);
            } else if (id != 0) {
               LOGGER.info("Item applyEnchantStats could not find option " + id + " " + this + " " + player);
            }
         }
      }
   }

   @Override
   public void setHeading(int heading) {
   }

   public void stopAllTasks() {
      ItemLifeTimeTaskManager.getInstance().remove(this);
   }

   public ItemVariables getVariables() {
      ItemVariables vars = this.getScript(ItemVariables.class);
      return vars != null ? vars : this.addScript(new ItemVariables(this.getObjectId()));
   }

   public int getTransmogId() {
      return !TransmogConfig.ENABLE_TRANSMOG ? 0 : this.getVariables().getInt("transmogId", 0);
   }

   public void setTransmogId(int transmogId) {
      this.getVariables().set("transmogId", transmogId);
      this.getVariables().storeMe();
   }

   public void removeTransmog() {
      this.getVariables().remove("transmogId");
      this.getVariables().storeMe();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this._itemTemplate);
      sb.append('[');
      sb.append(this.getObjectId());
      sb.append(']');
      return sb.toString();
   }
}
