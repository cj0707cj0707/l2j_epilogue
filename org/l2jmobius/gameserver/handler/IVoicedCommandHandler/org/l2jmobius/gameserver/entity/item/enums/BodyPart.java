package org.l2jmobius.gameserver.entity.item.enums;

import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public enum BodyPart {
   NONE(0, 25, "none"),
   UNDERWEAR(1, 0, "underwear", "shirt"),
   R_EAR(2, 8, "rear", "rbracelet"),
   L_EAR(4, 9, "lear"),
   LR_EAR(6, -1, "rear;lear"),
   NECK(8, 4, "neck"),
   R_FINGER(16, 13, "rfinger"),
   L_FINGER(32, 14, "lfinger"),
   LR_FINGER(48, -1, "rfinger;lfinger"),
   HEAD(64, 1, "head"),
   R_HAND(128, 5, "rhand"),
   L_HAND(256, 7, "lhand"),
   GLOVES(512, 10, "gloves"),
   CHEST(1024, 6, "chest"),
   LEGS(2048, 11, "legs"),
   FEET(4096, 12, "feet"),
   BACK(8192, 23, "back"),
   LR_HAND(16384, -1, "lrhand"),
   FULL_ARMOR(32768, -1, "fullarmor", "onepiece"),
   HAIR(65536, 2, "hair"),
   ALLDRESS(131072, -1, "alldress"),
   HAIR2(262144, 3, "hair2"),
   HAIRALL(524288, -1, "hairall", "dhair"),
   R_BRACELET(1048576, 16, "rbracelet"),
   L_BRACELET(2097152, 15, "lbracelet"),
   DECO(4194304, 17, "deco1", "talisman"),
   BELT(268435456, 24, "belt", "waist"),
   WOLF(-100, -1, "wolf"),
   HATCHLING(-101, -1, "hatchling"),
   STRIDER(-102, -1, "strider"),
   BABYPET(-103, -1, "babypet"),
   GREATWOLF(-104, -1, "greatwolf");

   private final int _mask;
   private final int _paperdollSlot;
   private final String[] _names;
   private static final Map<Integer, BodyPart> BODY_PARTS_BY_PAPERDOLL_SLOT = new HashMap<>();
   private static final Map<String, BodyPart> BODY_PARTS_BY_NAME = new HashMap<>();

   private BodyPart(int mask, int paperdollSlot, String... names) {
      this._mask = mask;
      this._paperdollSlot = paperdollSlot;
      this._names = names;
   }

   public int getMask() {
      return this._mask;
   }

   public int getPaperdollSlot() {
      return this._paperdollSlot;
   }

   public static int getPaperdollIndex(BodyPart bodyPart) {
      switch (bodyPart) {
         case LR_HAND:
            return 5;
         case FULL_ARMOR:
         case ALLDRESS:
            return 6;
         case HAIR:
         case HAIR2:
         default:
            return bodyPart.getPaperdollSlot();
         case HAIRALL:
            return 2;
      }
   }

   public static BodyPart fromPaperdollSlot(int slot) {
      return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
   }

   public static BodyPart fromItem(Item item) {
      if (item == null) {
         return NONE;
      } else {
         int slot = item.getLocationSlot();
         switch (slot) {
            case 6:
               return item.getTemplate().getBodyPart();
            default:
               return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
         }
      }
   }

   public static BodyPart fromName(String name) {
      return BODY_PARTS_BY_NAME.get(name);
   }

   static {
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(17, DECO);
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(18, DECO);
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(19, DECO);
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(20, DECO);
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(21, DECO);
      BODY_PARTS_BY_PAPERDOLL_SLOT.put(22, DECO);

      for (BodyPart bodyPart : values()) {
         if (bodyPart._paperdollSlot >= 0 && bodyPart._paperdollSlot < 25) {
            BODY_PARTS_BY_PAPERDOLL_SLOT.put(bodyPart._paperdollSlot, bodyPart);
         }

         for (String name : bodyPart._names) {
            BODY_PARTS_BY_NAME.put(name, bodyPart);
         }
      }
   }
}
