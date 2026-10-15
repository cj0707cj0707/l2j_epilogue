package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.WarehouseItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.MaterialType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.WarehouseListType;

public class SortedWareHouseWithdrawalList extends ServerPacket {
   public static final int PRIVATE = 1;
   public static final int CLAN = 2;
   public static final int CASTLE = 3;
   public static final int FREIGHT = 4;
   public static final byte A2Z = 1;
   public static final byte Z2A = -1;
   public static final byte GRADE = 2;
   public static final byte LEVEL = 3;
   public static final byte TYPE = 4;
   public static final byte WEAR = 5;
   public static final int MAX_SORT_LIST_ITEMS = 300;
   private long _playerAdena;
   private List<WarehouseItem> _objects = new ArrayList<>();
   private int _whType;

   public SortedWareHouseWithdrawalList(Player player, int type, WarehouseListType itemtype, byte sortorder) {
      this._whType = type;
      this._playerAdena = player.getAdena();
      if (player.getActiveWarehouse() == null) {
         PacketLogger.warning("Error while sending withdraw request to: " + player.getName());
      } else {
         switch (itemtype) {
            case WEAPON:
               this._objects = this.createWeaponList(player.getActiveWarehouse().getItems());
               break;
            case ARMOR:
               this._objects = this.createArmorList(player.getActiveWarehouse().getItems());
               break;
            case ETCITEM:
               this._objects = this.createEtcItemList(player.getActiveWarehouse().getItems());
               break;
            case MATERIAL:
               this._objects = this.createMatList(player.getActiveWarehouse().getItems());
               break;
            case RECIPE:
               this._objects = this.createRecipeList(player.getActiveWarehouse().getItems());
               break;
            case AMULETT:
               this._objects = this.createAmulettList(player.getActiveWarehouse().getItems());
               break;
            case SPELLBOOK:
               this._objects = this.createSpellbookList(player.getActiveWarehouse().getItems());
               break;
            case CONSUMABLE:
               this._objects = this.createConsumableList(player.getActiveWarehouse().getItems());
               break;
            case SHOT:
               this._objects = this.createShotList(player.getActiveWarehouse().getItems());
               break;
            case SCROLL:
               this._objects = this.createScrollList(player.getActiveWarehouse().getItems());
               break;
            case SEED:
               this._objects = this.createSeedList(player.getActiveWarehouse().getItems());
               break;
            case OTHER:
               this._objects = this.createOtherList(player.getActiveWarehouse().getItems());
               break;
            case ALL:
            default:
               this._objects = this.createAllList(player.getActiveWarehouse().getItems());
         }

         try {
            switch (sortorder) {
               case -1:
               case 1:
                  this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemNameComparator(sortorder));
               case 0:
               default:
                  break;
               case 2:
                  if (itemtype == WarehouseListType.ARMOR || itemtype == WarehouseListType.WEAPON) {
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemNameComparator((byte)1));
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemGradeComparator((byte)1));
                  }
                  break;
               case 3:
                  if (itemtype == WarehouseListType.RECIPE) {
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemNameComparator((byte)1));
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemRecipeComparator(1));
                  }
                  break;
               case 4:
                  if (itemtype == WarehouseListType.MATERIAL) {
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemNameComparator((byte)1));
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemTypeComparator((byte)1));
                  }
                  break;
               case 5:
                  if (itemtype == WarehouseListType.ARMOR) {
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemNameComparator((byte)1));
                     this._objects.sort(new SortedWareHouseWithdrawalList.WarehouseItemBodypartComparator((byte)1));
                  }
            }
         } catch (Exception var6) {
         }
      }
   }

   public static byte getOrder(String order) {
      if (order == null) {
         return 1;
      } else if (order.startsWith("A2Z")) {
         return 1;
      } else if (order.startsWith("Z2A")) {
         return -1;
      } else if (order.startsWith("GRADE")) {
         return 2;
      } else if (order.startsWith("TYPE")) {
         return 4;
      } else if (order.startsWith("WEAR")) {
         return 5;
      } else {
         try {
            return Byte.parseByte(order);
         } catch (NumberFormatException var2) {
            return 1;
         }
      }
   }

   private List<WarehouseItem> createWeaponList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((
               item.isWeapon()
                  || item.getTemplate().getType2() == 0
                  || item.isEtcItem() && item.getItemType() == EtcItemType.ARROW
                  || item.getTemplate().getType2() == 4
            )
            && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createArmorList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isArmor() || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createEtcItemList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createMatList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getEtcItem().getItemType() == EtcItemType.MATERIAL || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createRecipeList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getEtcItem().getItemType() == EtcItemType.RECIPE || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createAmulettList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getItemName().toUpperCase().startsWith("AMULET") || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createSpellbookList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && !item.getItemName().toUpperCase().startsWith("AMULET") || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createConsumableList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((
               item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.SCROLL || item.getEtcItem().getItemType() == EtcItemType.SHOT)
                  || item.getTemplate().getType2() == 4
            )
            && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createShotList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getEtcItem().getItemType() == EtcItemType.SHOT || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createScrollList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getEtcItem().getItemType() == EtcItemType.SCROLL || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createSeedList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((item.isEtcItem() && item.getEtcItem().getItemType() == EtcItemType.SEED || item.getTemplate().getType2() == 4) && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createOtherList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if ((
               item.isEtcItem()
                     && item.getEtcItem().getItemType() != EtcItemType.MATERIAL
                     && item.getEtcItem().getItemType() != EtcItemType.RECIPE
                     && item.getEtcItem().getItemType() != EtcItemType.SCROLL
                     && item.getEtcItem().getItemType() != EtcItemType.SHOT
                  || item.getTemplate().getType2() == 4
            )
            && list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   private List<WarehouseItem> createAllList(Collection<Item> items) {
      List<WarehouseItem> list = new ArrayList<>();

      for (Item item : items) {
         if (list.size() < 300) {
            list.add(new WarehouseItem(item));
         }
      }

      return list;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.WAREHOUSE_WITHDRAW_LIST.writeId(this, buffer);
      buffer.writeShort(this._whType);
      buffer.writeLong(this._playerAdena);
      buffer.writeShort(this._objects.size());

      for (WarehouseItem item : this._objects) {
         buffer.writeShort(item.getItem().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getItemId());
         buffer.writeLong(item.getCount());
         buffer.writeShort(item.getItem().getType2());
         buffer.writeShort(item.getCustomType1());
         buffer.writeInt(item.getItem().getBodyPart().getMask());
         buffer.writeShort(item.getEnchantLevel());
         buffer.writeShort(0);
         buffer.writeShort(item.getCustomType2());
         buffer.writeInt(item.getObjectId());
         if (item.isAugmented()) {
            buffer.writeInt(65535 & item.getAugmentationId());
            buffer.writeInt(item.getAugmentationId() >> 16);
         } else {
            buffer.writeLong(0L);
         }

         buffer.writeShort(item.getAttackElementType());
         buffer.writeShort(item.getAttackElementPower());

         for (byte i = 0; i < 6; i++) {
            buffer.writeShort(item.getElementDefAttr(i));
         }

         buffer.writeInt(item.getMana());
         buffer.writeInt(item.getTime());

         for (int op : item.getEnchantOptions()) {
            buffer.writeShort(op);
         }
      }
   }

   private static class WarehouseItemBodypartComparator implements Comparator<WarehouseItem> {
      private byte order = 0;

      protected WarehouseItemBodypartComparator(byte sortOrder) {
         this.order = sortOrder;
      }

      public int compare(WarehouseItem o1, WarehouseItem o2) {
         if (o1.getType2() == 4 && o2.getType2() != 4) {
            return this.order == 1 ? -1 : 1;
         } else if (o2.getType2() == 4 && o1.getType2() != 4) {
            return this.order == 1 ? 1 : -1;
         } else {
            Integer i1 = o1.getBodyPart().getMask();
            Integer i2 = o2.getBodyPart().getMask();
            return this.order == 1 ? i1.compareTo(i2) : i2.compareTo(i1);
         }
      }
   }

   private static class WarehouseItemGradeComparator implements Comparator<WarehouseItem> {
      byte order = 0;

      protected WarehouseItemGradeComparator(byte sortOrder) {
         this.order = sortOrder;
      }

      public int compare(WarehouseItem o1, WarehouseItem o2) {
         if (o1.getType2() == 4 && o2.getType2() != 4) {
            return this.order == 1 ? -1 : 1;
         } else if (o2.getType2() == 4 && o1.getType2() != 4) {
            return this.order == 1 ? 1 : -1;
         } else {
            CrystalType i1 = o1.getItemGrade();
            CrystalType i2 = o2.getItemGrade();
            return this.order == 1 ? i1.compareTo(i2) : i2.compareTo(i1);
         }
      }
   }

   private static class WarehouseItemNameComparator implements Comparator<WarehouseItem> {
      private byte order = 0;

      protected WarehouseItemNameComparator(byte sortOrder) {
         this.order = sortOrder;
      }

      public int compare(WarehouseItem o1, WarehouseItem o2) {
         if (o1.getType2() == 4 && o2.getType2() != 4) {
            return this.order == 1 ? -1 : 1;
         } else if (o2.getType2() == 4 && o1.getType2() != 4) {
            return this.order == 1 ? 1 : -1;
         } else {
            String s1 = o1.getItemName();
            String s2 = o2.getItemName();
            return this.order == 1 ? s1.compareTo(s2) : s2.compareTo(s1);
         }
      }
   }

   private static class WarehouseItemRecipeComparator implements Comparator<WarehouseItem> {
      private int order = 0;
      private RecipeData rd = null;

      protected WarehouseItemRecipeComparator(int sortOrder) {
         this.order = sortOrder;
         this.rd = RecipeData.getInstance();
      }

      public int compare(WarehouseItem o1, WarehouseItem o2) {
         if (o1.getType2() == 4 && o2.getType2() != 4) {
            return this.order == 1 ? -1 : 1;
         } else if (o2.getType2() == 4 && o1.getType2() != 4) {
            return this.order == 1 ? 1 : -1;
         } else if (o1.isEtcItem() && o1.getItemType() == EtcItemType.RECIPE && o2.isEtcItem() && o2.getItemType() == EtcItemType.RECIPE) {
            try {
               RecipeList rp1 = this.rd.getRecipeByItemId(o1.getItemId());
               if (rp1 == null) {
                  return this.order == 1 ? 1 : -1;
               } else {
                  RecipeList rp2 = this.rd.getRecipeByItemId(o2.getItemId());
                  if (rp2 == null) {
                     return this.order == 1 ? -1 : 1;
                  } else {
                     Integer i1 = rp1.getLevel();
                     Integer i2 = rp2.getLevel();
                     return this.order == 1 ? i1.compareTo(i2) : i2.compareTo(i1);
                  }
               }
            } catch (Exception var7) {
               return 0;
            }
         } else {
            String s1 = o1.getItemName();
            String s2 = o2.getItemName();
            return this.order == 1 ? s1.compareTo(s2) : s2.compareTo(s1);
         }
      }
   }

   private static class WarehouseItemTypeComparator implements Comparator<WarehouseItem> {
      byte order = 0;

      protected WarehouseItemTypeComparator(byte sortOrder) {
         this.order = sortOrder;
      }

      public int compare(WarehouseItem o1, WarehouseItem o2) {
         if (o1.getType2() == 4 && o2.getType2() != 4) {
            return this.order == 1 ? -1 : 1;
         } else if (o2.getType2() == 4 && o1.getType2() != 4) {
            return this.order == 1 ? 1 : -1;
         } else {
            try {
               MaterialType i1 = o1.getItem().getMaterialType();
               MaterialType i2 = o2.getItem().getMaterialType();
               return this.order == 1 ? i1.compareTo(i2) : i2.compareTo(i1);
            } catch (Exception var5) {
               return 0;
            }
         }
      }
   }
}
