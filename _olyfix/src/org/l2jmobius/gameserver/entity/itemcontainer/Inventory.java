package org.l2jmobius.gameserver.entity.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemUnequip;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

public abstract class Inventory extends ItemContainer {
   protected static final Logger LOGGER = Logger.getLogger(Inventory.class.getName());
   public static final int ADENA_ID = 57;
   public static final int ANCIENT_ADENA_ID = 5575;
   public static final long MAX_ADENA = PlayerConfig.MAX_ADENA;
   public static final int PAPERDOLL_UNDER = 0;
   public static final int PAPERDOLL_HEAD = 1;
   public static final int PAPERDOLL_HAIR = 2;
   public static final int PAPERDOLL_HAIR2 = 3;
   public static final int PAPERDOLL_NECK = 4;
   public static final int PAPERDOLL_RHAND = 5;
   public static final int PAPERDOLL_CHEST = 6;
   public static final int PAPERDOLL_LHAND = 7;
   public static final int PAPERDOLL_REAR = 8;
   public static final int PAPERDOLL_LEAR = 9;
   public static final int PAPERDOLL_GLOVES = 10;
   public static final int PAPERDOLL_LEGS = 11;
   public static final int PAPERDOLL_FEET = 12;
   public static final int PAPERDOLL_RFINGER = 13;
   public static final int PAPERDOLL_LFINGER = 14;
   public static final int PAPERDOLL_LBRACELET = 15;
   public static final int PAPERDOLL_RBRACELET = 16;
   public static final int PAPERDOLL_DECO1 = 17;
   public static final int PAPERDOLL_DECO2 = 18;
   public static final int PAPERDOLL_DECO3 = 19;
   public static final int PAPERDOLL_DECO4 = 20;
   public static final int PAPERDOLL_DECO5 = 21;
   public static final int PAPERDOLL_DECO6 = 22;
   public static final int PAPERDOLL_CLOAK = 23;
   public static final int PAPERDOLL_BELT = 24;
   public static final int PAPERDOLL_TOTALSLOTS = 25;
   public static final double MAX_ARMOR_WEIGHT = 12000.0;
   private final Item[] _paperdoll = new Item[25];
   private final List<Inventory.PaperdollListener> _paperdollListeners = new ArrayList<>();
   protected int _totalWeight;
   private int _wearedMask;

   protected Inventory() {
      if (this instanceof PlayerInventory) {
         this.addPaperdollListener(Inventory.ArmorSetListener.getInstance());
         this.addPaperdollListener(Inventory.BowCrossRodListener.getInstance());
         this.addPaperdollListener(Inventory.ItemSkillsListener.getInstance());
         this.addPaperdollListener(Inventory.BraceletListener.getInstance());
      }

      this.addPaperdollListener(Inventory.StatsListener.getInstance());
   }

   protected abstract ItemLocation getEquipLocation();

   private Inventory.ChangeRecorder newRecorder() {
      return new Inventory.ChangeRecorder(this);
   }

   public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference) {
      if (item == null) {
         return null;
      } else {
         synchronized (item) {
            if (!this._items.contains(item)) {
               return null;
            } else {
               this.removeItem(item);
               item.setOwnerId(process, 0, actor, reference);
               item.setItemLocation(ItemLocation.VOID);
               item.setLastChange(3);
               item.updateDatabase();
               this.refreshWeight();
               return item;
            }
         }
      }
   }

   public Item dropItem(ItemProcessType process, int objectId, long count, Player actor, Object reference) {
      Item item = this.getItemByObjectId(objectId);
      if (item == null) {
         return null;
      } else {
         synchronized (item) {
            if (!this._items.contains(item)) {
               return null;
            }

            if (item.getCount() > count) {
               item.changeCount(process, -count, actor, reference);
               item.setLastChange(2);
               item.updateDatabase();
               Item newItem = ItemManager.createItem(process, item.getId(), count, actor, reference);
               newItem.updateDatabase();
               this.refreshWeight();
               return newItem;
            }
         }

         return this.dropItem(process, item, actor, reference);
      }
   }

   @Override
   protected void addItem(Item item) {
      super.addItem(item);
      if (item.isEquipped()) {
         this.equipItem(item);
      }

      if (item.getTemplate().getSkills() != null) {
         Player owner = this.getOwner().asPlayer();
         if (owner != null) {
            owner.sendSkillList();
         }
      }
   }

   @Override
   protected boolean removeItem(Item item) {
      for (int i = 0; i < this._paperdoll.length; i++) {
         if (this._paperdoll[i] == item) {
            this.unEquipItemInSlot(i);
         }
      }

      if (item.getTemplate().getSkills() != null) {
         Player owner = this.getOwner().asPlayer();
         if (owner != null) {
            owner.sendSkillList();
         }
      }

      return super.removeItem(item);
   }

   public Item getPaperdollItem(int slot) {
      return this._paperdoll[slot];
   }

   public boolean isPaperdollSlotEmpty(int slot) {
      return this._paperdoll[slot] == null;
   }

   public static int getPaperdollIndex(BodyPart bodyPart) {
      return bodyPart == null ? -1 : bodyPart.getPaperdollSlot();
   }

   public Item getPaperdollItemByBodyPart(BodyPart bodyPart) {
      int index = getPaperdollIndex(bodyPart);
      return index == -1 ? null : this._paperdoll[index];
   }

   public int getPaperdollItemId(int slot) {
      Item item = this._paperdoll[slot];
      if (item != null) {
         if (TransmogConfig.ENABLE_TRANSMOG) {
            int transmogId = item.getTransmogId();
            if (transmogId > 0) {
               return transmogId;
            }
         }

         return item.getId();
      } else {
         return 0;
      }
   }

   public Item getPaperdollItemByItemId(int itemId) {
      for (int i = 0; i < this._paperdoll.length; i++) {
         Item item = this._paperdoll[i];
         if (item != null && item.getId() == itemId) {
            return item;
         }
      }

      return null;
   }

   public int getPaperdollItemDisplayId(int slot) {
      Item item = this._paperdoll[slot];
      if (item != null) {
         if (TransmogConfig.ENABLE_TRANSMOG) {
            int transmogId = item.getTransmogId();
            if (transmogId > 0) {
               return transmogId;
            }
         }

         return item.getDisplayId();
      } else {
         return 0;
      }
   }

   public int getPaperdollAugmentationId(int slot) {
      Item item = this._paperdoll[slot];
      return item != null && item.getAugmentation() != null ? item.getAugmentation().getAugmentationId() : 0;
   }

   public int getPaperdollObjectId(int slot) {
      Item item = this._paperdoll[slot];
      return item != null ? item.getObjectId() : 0;
   }

   public synchronized void addPaperdollListener(Inventory.PaperdollListener listener) {
      if (!this._paperdollListeners.contains(listener)) {
         this._paperdollListeners.add(listener);
      }
   }

   public synchronized void removePaperdollListener(Inventory.PaperdollListener listener) {
      this._paperdollListeners.remove(listener);
   }

   public synchronized Item setPaperdollItem(int slot, Item item) {
      Item old = this._paperdoll[slot];
      if (old != item) {
         if (old != null) {
            this._paperdoll[slot] = null;
            old.setItemLocation(this.getBaseLocation());
            old.setLastChange(2);
            int mask = 0;

            for (int i = 0; i < 25; i++) {
               Item pi = this._paperdoll[i];
               if (pi != null) {
                  mask |= pi.getTemplate().getItemMask();
               }
            }

            this._wearedMask = mask;

            for (Inventory.PaperdollListener listener : this._paperdollListeners) {
               if (listener != null) {
                  listener.notifyUnequiped(slot, old, this);
               }
            }

            old.updateDatabase();
         }

         if (item != null) {
            this._paperdoll[slot] = item;
            item.setItemLocation(this.getEquipLocation(), slot);
            item.setLastChange(2);
            this._wearedMask = this._wearedMask | item.getTemplate().getItemMask();

            for (Inventory.PaperdollListener listenerx : this._paperdollListeners) {
               if (listenerx != null) {
                  listenerx.notifyEquiped(slot, item, this);
               }
            }

            item.updateDatabase();
         }
      }

      if (old != null) {
         Creature owner = this.getOwner();
         if (owner != null && owner.isPlayer() && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_UNEQUIP, old.getTemplate())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemUnequip(owner.asPlayer(), old), old.getTemplate());
         }
      }

      return old;
   }

   public int getWearedMask() {
      return this._wearedMask;
   }

   public Collection<Item> unEquipItemInBodySlotAndRecord(BodyPart bodyPart) {
      Inventory.ChangeRecorder recorder = this.newRecorder();

      try {
         this.unEquipItemInBodySlot(bodyPart);
      } finally {
         this.removePaperdollListener(recorder);
      }

      return recorder.getChangedItems();
   }

   public Item unEquipItemInSlot(int pdollSlot) {
      return this.setPaperdollItem(pdollSlot, null);
   }

   public Collection<Item> unEquipItemInSlotAndRecord(int slot) {
      Inventory.ChangeRecorder recorder = this.newRecorder();

      try {
         this.unEquipItemInSlot(slot);
         if (this.getOwner().isPlayer()) {
            this.getOwner().asPlayer().refreshExpertisePenalty();
         }
      } finally {
         this.removePaperdollListener(recorder);
      }

      return recorder.getChangedItems();
   }

   public Item unEquipItemInBodySlot(BodyPart bodyPart) {
      if (bodyPart == BodyPart.HAIRALL) {
         this.setPaperdollItem(2, null);
      }

      int pdollSlot = BodyPart.getPaperdollIndex(bodyPart);
      if (pdollSlot >= 0) {
         Item old = this.setPaperdollItem(pdollSlot, null);
         if (old != null && this.getOwner().isPlayer()) {
            this.getOwner().asPlayer().refreshExpertisePenalty();
         }

         return old;
      } else {
         return null;
      }
   }

   public Collection<Item> equipItemAndRecord(Item item) {
      Inventory.ChangeRecorder recorder = this.newRecorder();

      try {
         this.equipItem(item);
      } finally {
         this.removePaperdollListener(recorder);
      }

      return recorder.getChangedItems();
   }

   public void equipItem(Item item) {
      if (this.getOwner().isPlayer()) {
         if (this.getOwner().asPlayer().isInStoreMode()) {
            return;
         }

         Player player = this.getOwner().asPlayer();
         if (!player.isGM() && !player.isHero() && item.isHeroItem()) {
            return;
         }
      }

      BodyPart bodyPart = item.getTemplate().getBodyPart();
      Item formal = this.getPaperdollItem(6);
      if (item.getId() != 21163 && formal != null && formal.getTemplate().getBodyPart() == BodyPart.ALLDRESS) {
         switch (bodyPart) {
            case LR_HAND:
            case L_HAND:
            case R_HAND:
            case LEGS:
            case FEET:
            case GLOVES:
            case HEAD:
               return;
         }
      }

      switch (bodyPart) {
         case LR_HAND:
            this.setPaperdollItem(7, null);
            this.setPaperdollItem(5, item);
            break;
         case L_HAND:
            Item rh = this.getPaperdollItem(5);
            if (rh != null
               && rh.getTemplate().getBodyPart() == BodyPart.LR_HAND
               && (rh.getItemType() != WeaponType.BOW || item.getItemType() != EtcItemType.ARROW)
               && (rh.getItemType() != WeaponType.CROSSBOW || item.getItemType() != EtcItemType.BOLT)
               && (rh.getItemType() != WeaponType.FISHINGROD || item.getItemType() != EtcItemType.LURE)) {
               this.setPaperdollItem(5, null);
            }

            this.setPaperdollItem(7, item);
            break;
         case R_HAND:
            this.setPaperdollItem(5, item);
            break;
         case LEGS:
            Item chest = this.getPaperdollItem(6);
            if (chest != null && chest.getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) {
               this.setPaperdollItem(6, null);
            }

            this.setPaperdollItem(11, item);
            break;
         case FEET:
            this.setPaperdollItem(12, item);
            break;
         case GLOVES:
            this.setPaperdollItem(10, item);
            break;
         case HEAD:
            this.setPaperdollItem(1, item);
            break;
         case L_EAR:
         case R_EAR:
         case LR_EAR:
            if (this._paperdoll[9] == null) {
               this.setPaperdollItem(9, item);
            } else if (this._paperdoll[8] == null) {
               this.setPaperdollItem(8, item);
            } else {
               this.setPaperdollItem(9, item);
            }
            break;
         case L_FINGER:
         case R_FINGER:
         case LR_FINGER:
            if (this._paperdoll[14] == null) {
               this.setPaperdollItem(14, item);
            } else if (this._paperdoll[13] == null) {
               this.setPaperdollItem(13, item);
            } else {
               this.setPaperdollItem(14, item);
            }
            break;
         case NECK:
            this.setPaperdollItem(4, item);
            break;
         case FULL_ARMOR:
            this.setPaperdollItem(11, null);
            this.setPaperdollItem(6, item);
            break;
         case CHEST:
            this.setPaperdollItem(6, item);
            break;
         case HAIR:
            Item hair = this.getPaperdollItem(2);
            if (hair != null && hair.getTemplate().getBodyPart() == BodyPart.HAIRALL) {
               this.setPaperdollItem(3, null);
            } else {
               this.setPaperdollItem(2, null);
            }

            this.setPaperdollItem(2, item);
            break;
         case HAIR2:
            Item hair2 = this.getPaperdollItem(2);
            if (hair2 != null && hair2.getTemplate().getBodyPart() == BodyPart.HAIRALL) {
               this.setPaperdollItem(2, null);
            } else {
               this.setPaperdollItem(3, null);
            }

            this.setPaperdollItem(3, item);
            break;
         case HAIRALL:
            this.setPaperdollItem(3, null);
            this.setPaperdollItem(2, item);
            break;
         case UNDERWEAR:
            this.setPaperdollItem(0, item);
            break;
         case BACK:
            this.setPaperdollItem(23, item);
            break;
         case L_BRACELET:
            this.setPaperdollItem(15, item);
            break;
         case R_BRACELET:
            this.setPaperdollItem(16, item);
            break;
         case DECO:
            this.equipTalisman(item);
            break;
         case BELT:
            this.setPaperdollItem(24, item);
            break;
         case ALLDRESS:
            this.setPaperdollItem(11, null);
            this.setPaperdollItem(7, null);
            this.setPaperdollItem(5, null);
            this.setPaperdollItem(1, null);
            this.setPaperdollItem(12, null);
            this.setPaperdollItem(10, null);
            this.setPaperdollItem(6, item);
            break;
         default:
            LOGGER.warning("Unknown body slot " + bodyPart + " for Item ID: " + item.getId());
      }
   }

   @Override
   protected void refreshWeight() {
      long weight = 0L;

      for (Item item : this._items) {
         if (item != null && item.getTemplate() != null) {
            weight += item.getTemplate().getWeight() * item.getCount();
         }
      }

      this._totalWeight = (int)Math.min(weight, 2147483647L);
   }

   public int getTotalWeight() {
      return this._totalWeight;
   }

   public Item findArrowForBow(ItemTemplate bow) {
      if (bow == null) {
         return null;
      } else {
         Item arrow = null;

         for (Item item : this._items) {
            if (item.isEtcItem() && item.getTemplate().getCrystalTypePlus() == bow.getCrystalTypePlus() && item.getEtcItem().getItemType() == EtcItemType.ARROW
               )
             {
               arrow = item;
               break;
            }
         }

         return arrow;
      }
   }

   public Item findBoltForCrossBow(ItemTemplate crossbow) {
      Item bolt = null;

      for (Item item : this._items) {
         if (item.isEtcItem()
            && item.getTemplate().getCrystalTypePlus() == crossbow.getCrystalTypePlus()
            && item.getEtcItem().getItemType() == EtcItemType.BOLT) {
            bolt = item;
            break;
         }
      }

      return bolt;
   }

   @Override
   public void restore() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement(
            "SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data"
         );
      ) {
         statement.setInt(1, this.getOwnerId());
         statement.setString(2, this.getBaseLocation().name());
         statement.setString(3, this.getEquipLocation().name());

         try (ResultSet inv = statement.executeQuery()) {
            while (inv.next()) {
               Item item = Item.restoreFromDb(this.getOwnerId(), inv);
               if (item != null) {
                  if (this.getOwner().isPlayer()) {
                     Player player = this.getOwner().asPlayer();
                     if (!player.isGM() && !player.isHero() && item.isHeroItem()) {
                        item.setItemLocation(ItemLocation.INVENTORY);
                     }
                  }

                  World.addObject(item);
                  if (item.isStackable() && this.getItemByItemId(item.getId()) != null) {
                     this.addItem(ItemProcessType.RESTORE, item, this.getOwner().asPlayer(), null);
                  } else {
                     this.addItem(item);
                  }
               }
            }
         }

         this.refreshWeight();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Could not restore inventory: " + var12.getMessage(), (Throwable)var12);
      }
   }

   public int getTalismanSlots() {
      return this.getOwner().asPlayer().getStat().getTalismanSlots();
   }

   private void equipTalisman(Item item) {
      if (this.getTalismanSlots() != 0) {
         for (int i = 17; i < 17 + this.getTalismanSlots(); i++) {
            if (this._paperdoll[i] != null && this.getPaperdollItemId(i) == item.getId()) {
               this.setPaperdollItem(i, item);
               return;
            }
         }

         for (int ix = 17; ix < 17 + this.getTalismanSlots(); ix++) {
            if (this._paperdoll[ix] == null) {
               this.setPaperdollItem(ix, item);
               return;
            }
         }

         this.setPaperdollItem(17, item);
      }
   }

   public boolean canEquipCloak() {
      return this.getOwner().asPlayer().getStat().canEquipCloak();
   }

   public void reloadEquippedItems() {
      for (Item item : this._paperdoll) {
         if (item != null) {
            int slot = item.getLocationSlot();

            for (Inventory.PaperdollListener listener : this._paperdollListeners) {
               if (listener != null) {
                  listener.notifyUnequiped(slot, item, this);
                  listener.notifyEquiped(slot, item, this);
               }
            }
         }
      }
   }

   @SafeVarargs
   public final Collection<Item> getPaperdollItems(Predicate<Item>... filters) {
      Predicate<Item> filter = Objects::nonNull;

      for (Predicate<Item> additionalFilter : filters) {
         filter = filter.and(additionalFilter);
      }

      Collection<Item> items = new ArrayDeque<>();

      for (Item item : this._paperdoll) {
         if (filter.test(item)) {
            items.add(item);
         }
      }

      return items;
   }

   private static class ArmorSetListener implements Inventory.PaperdollListener {
      private static Inventory.ArmorSetListener instance = new Inventory.ArmorSetListener();

      public static Inventory.ArmorSetListener getInstance() {
         return instance;
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
         if (inventory.getOwner().isPlayer()) {
            Player player = inventory.getOwner().asPlayer();
            Item chestItem = inventory.getPaperdollItem(6);
            if (chestItem != null) {
               if (ArmorSetData.getInstance().isArmorSet(chestItem.getId())) {
                  ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getId());
                  boolean update = false;
                  boolean updateTimeStamp = false;
                  if (armorSet.containItem(slot, item.getId())) {
                     if (armorSet.containAll(player)) {
                        List<SkillHolder> skills = armorSet.getSkills();
                        if (skills != null) {
                           for (SkillHolder holder : skills) {
                              Skill itemSkill = holder.getSkill();
                              if (itemSkill != null) {
                                 player.addSkill(itemSkill, false);
                                 if (itemSkill.isActive()) {
                                    if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
                                       int equipDelay = item.getEquipReuseDelay();
                                       if (equipDelay > 0) {
                                          player.addTimeStamp(itemSkill, equipDelay);
                                          player.disableSkill(itemSkill, equipDelay);
                                       }
                                    }

                                    updateTimeStamp = true;
                                 }

                                 update = true;
                              } else {
                                 Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
                              }
                           }
                        }

                        if (armorSet.containShield(player)) {
                           for (SkillHolder holderx : armorSet.getShieldSkillId()) {
                              if (holderx.getSkill() != null) {
                                 player.addSkill(holderx.getSkill(), false);
                                 update = true;
                              } else {
                                 Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holderx + ".");
                              }
                           }
                        }

                        if (armorSet.isEnchanted6(player)) {
                           for (SkillHolder holderxx : armorSet.getEnchant6skillId()) {
                              if (holderxx.getSkill() != null) {
                                 player.addSkill(holderxx.getSkill(), false);
                                 update = true;
                              } else {
                                 Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holderxx + ".");
                              }
                           }
                        }
                     }
                  } else if (armorSet.containShield(item.getId())) {
                     for (SkillHolder holderxxx : armorSet.getShieldSkillId()) {
                        if (holderxxx.getSkill() != null) {
                           player.addSkill(holderxxx.getSkill(), false);
                           update = true;
                        } else {
                           Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holderxxx + ".");
                        }
                     }
                  }

                  if (update) {
                     player.sendSkillList();
                     if (updateTimeStamp) {
                        player.sendPacket(new SkillCoolTime(player));
                     }
                  }
               }
            }
         }
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         if (inventory.getOwner().isPlayer()) {
            Player player = inventory.getOwner().asPlayer();
            boolean remove = false;
            List<SkillHolder> skills = null;
            List<SkillHolder> shieldSkill = null;
            List<SkillHolder> skillId6 = null;
            if (slot == 6) {
               if (!ArmorSetData.getInstance().isArmorSet(item.getId())) {
                  return;
               }

               ArmorSet armorSet = ArmorSetData.getInstance().getSet(item.getId());
               remove = true;
               skills = armorSet.getSkills();
               shieldSkill = armorSet.getShieldSkillId();
               skillId6 = armorSet.getEnchant6skillId();
            } else {
               Item chestItem = inventory.getPaperdollItem(6);
               if (chestItem == null) {
                  return;
               }

               ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getId());
               if (armorSet == null) {
                  return;
               }

               if (armorSet.containItem(slot, item.getId())) {
                  remove = true;
                  skills = armorSet.getSkills();
                  shieldSkill = armorSet.getShieldSkillId();
                  skillId6 = armorSet.getEnchant6skillId();
               } else if (armorSet.containShield(item.getId())) {
                  remove = true;
                  shieldSkill = armorSet.getShieldSkillId();
               }
            }

            if (remove) {
               if (skills != null) {
                  for (SkillHolder holder : skills) {
                     Skill itemSkill = holder.getSkill();
                     if (itemSkill != null) {
                        player.removeSkill(itemSkill, false, itemSkill.isPassive());
                     } else {
                        Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
                     }
                  }
               }

               if (shieldSkill != null) {
                  for (SkillHolder holderx : shieldSkill) {
                     Skill itemSkill = holderx.getSkill();
                     if (itemSkill != null) {
                        player.removeSkill(itemSkill, false, itemSkill.isPassive());
                     } else {
                        Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holderx + ".");
                     }
                  }
               }

               if (skillId6 != null) {
                  for (SkillHolder holderxx : skillId6) {
                     Skill itemSkill = holderxx.getSkill();
                     if (itemSkill != null) {
                        player.removeSkill(itemSkill, false, itemSkill.isPassive());
                     } else {
                        Inventory.LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holderxx + ".");
                     }
                  }
               }

               player.checkItemRestriction();
               player.sendSkillList();
            }
         }
      }
   }

   private static class BowCrossRodListener implements Inventory.PaperdollListener {
      private static Inventory.BowCrossRodListener instance = new Inventory.BowCrossRodListener();

      public static Inventory.BowCrossRodListener getInstance() {
         return instance;
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         if (slot == 5) {
            if (item.getItemType() == WeaponType.BOW) {
               Item arrow = inventory.getPaperdollItem(7);
               if (arrow != null) {
                  inventory.setPaperdollItem(7, null);
               }
            } else if (item.getItemType() == WeaponType.CROSSBOW) {
               Item bolts = inventory.getPaperdollItem(7);
               if (bolts != null) {
                  inventory.setPaperdollItem(7, null);
               }
            } else if (item.getItemType() == WeaponType.FISHINGROD) {
               Item lure = inventory.getPaperdollItem(7);
               if (lure != null) {
                  inventory.setPaperdollItem(7, null);
               }
            }
         }
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
         if (slot == 5) {
            if (item.getItemType() == WeaponType.BOW) {
               Item arrow = inventory.findArrowForBow(item.getTemplate());
               if (arrow != null) {
                  inventory.setPaperdollItem(7, arrow);
               }
            } else if (item.getItemType() == WeaponType.CROSSBOW) {
               Item bolts = inventory.findBoltForCrossBow(item.getTemplate());
               if (bolts != null) {
                  inventory.setPaperdollItem(7, bolts);
               }
            }
         }
      }
   }

   private static class BraceletListener implements Inventory.PaperdollListener {
      private static Inventory.BraceletListener instance = new Inventory.BraceletListener();

      public static Inventory.BraceletListener getInstance() {
         return instance;
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         Player player = item.asPlayer();
         if (player == null || !player.isChangingClass()) {
            if (item.getTemplate().getBodyPart() == BodyPart.R_BRACELET) {
               inventory.unEquipItemInSlot(17);
               inventory.unEquipItemInSlot(18);
               inventory.unEquipItemInSlot(19);
               inventory.unEquipItemInSlot(20);
               inventory.unEquipItemInSlot(21);
               inventory.unEquipItemInSlot(22);
            }
         }
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
      }
   }

   private static class ChangeRecorder implements Inventory.PaperdollListener {
      private final Inventory _inventory;
      private final Collection<Item> _changed = new ArrayDeque<>(1);

      ChangeRecorder(Inventory inventory) {
         this._inventory = inventory;
         this._inventory.addPaperdollListener(this);
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
         this._changed.add(item);
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         this._changed.add(item);
      }

      public Collection<Item> getChangedItems() {
         return this._changed;
      }
   }

   private static class ItemSkillsListener implements Inventory.PaperdollListener {
      private static Inventory.ItemSkillsListener instance = new Inventory.ItemSkillsListener();

      public static Inventory.ItemSkillsListener getInstance() {
         return instance;
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         if (inventory.getOwner().isPlayer()) {
            Player player = inventory.getOwner().asPlayer();
            ItemTemplate it = item.getTemplate();
            boolean update = false;
            boolean updateTimeStamp = false;
            if (item.isAugmented()) {
               item.getAugmentation().removeBonus(player);
            }

            item.removeElementAttrBonus(player);
            if (item.getEnchantLevel() >= 4) {
               Skill enchant4Skill = it.getEnchant4Skill();
               if (enchant4Skill != null) {
                  player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
                  update = true;
               }
            }

            item.clearEnchantStats();
            SkillHolder[] skills = it.getSkills();
            if (skills != null) {
               for (SkillHolder skillInfo : skills) {
                  if (skillInfo != null) {
                     Skill itemSkill = skillInfo.getSkill();
                     if (itemSkill != null) {
                        player.removeSkill(itemSkill, false, itemSkill.isPassive());
                        update = true;
                     } else {
                        Inventory.LOGGER.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
                     }
                  }
               }
            }

            if (item.isArmor()) {
               for (Item itm : inventory.getItems()) {
                  if (itm.isEquipped() && itm.getTemplate().getSkills() != null && !itm.equals(item)) {
                     for (SkillHolder sk : itm.getTemplate().getSkills()) {
                        if (player.getSkillLevel(sk.getSkillId()) == 0) {
                           Skill itemSkill = sk.getSkill();
                           if (itemSkill != null) {
                              player.addSkill(itemSkill, false);
                              if (itemSkill.isActive()) {
                                 if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
                                    int equipDelay = item.getEquipReuseDelay();
                                    if (equipDelay > 0) {
                                       player.addTimeStamp(itemSkill, equipDelay);
                                       player.disableSkill(itemSkill, equipDelay);
                                    }
                                 }

                                 updateTimeStamp = true;
                              }

                              update = true;
                           }
                        }
                     }
                  }
               }
            }

            Skill unequipSkill = it.getUnequipSkill();
            if (unequipSkill != null) {
               unequipSkill.activateSkill(player, Collections.singletonList(player));
            }

            if (update) {
               player.sendSkillList();
               if (updateTimeStamp) {
                  player.sendPacket(new SkillCoolTime(player));
               }
            }
         }
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
         if (inventory.getOwner().isPlayer()) {
            Player player = inventory.getOwner().asPlayer();
            ItemTemplate it = item.getTemplate();
            boolean update = false;
            boolean updateTimeStamp = false;
            if (item.isAugmented()) {
               item.getAugmentation().applyBonus(player);
            }

            item.updateElementAttrBonus(player);
            if (item.getEnchantLevel() >= 4) {
               Skill enchant4Skill = it.getEnchant4Skill();
               if (enchant4Skill != null) {
                  player.addSkill(enchant4Skill, false);
                  update = true;
               }
            }

            item.applyEnchantStats();
            SkillHolder[] skills = it.getSkills();
            if (skills != null) {
               for (SkillHolder skillInfo : skills) {
                  if (skillInfo != null) {
                     Skill itemSkill = skillInfo.getSkill();
                     if (itemSkill != null) {
                        itemSkill.setReferenceItemId(item.getId());
                        player.addSkill(itemSkill, false);
                        if (itemSkill.isActive()) {
                           if (!player.hasSkillReuse(itemSkill.getReuseHashCode())) {
                              int equipDelay = item.getEquipReuseDelay();
                              if (equipDelay > 0) {
                                 player.addTimeStamp(itemSkill, equipDelay);
                                 player.disableSkill(itemSkill, equipDelay);
                              }
                           }

                           updateTimeStamp = true;
                        }

                        update = true;
                     } else {
                        Inventory.LOGGER.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
                     }
                  }
               }
            }

            if (update) {
               player.sendSkillList();
               if (updateTimeStamp) {
                  player.sendPacket(new SkillCoolTime(player));
               }
            }
         }
      }
   }

   public interface PaperdollListener {
      void notifyEquiped(int var1, Item var2, Inventory var3);

      void notifyUnequiped(int var1, Item var2, Inventory var3);
   }

   private static class StatsListener implements Inventory.PaperdollListener {
      private static Inventory.StatsListener instance = new Inventory.StatsListener();

      public static Inventory.StatsListener getInstance() {
         return instance;
      }

      @Override
      public void notifyUnequiped(int slot, Item item, Inventory inventory) {
         inventory.getOwner().removeStatsOwner(item);
      }

      @Override
      public void notifyEquiped(int slot, Item item, Inventory inventory) {
         inventory.getOwner().addStatFuncs(item.getStatFuncs(inventory.getOwner()));
      }
   }
}
