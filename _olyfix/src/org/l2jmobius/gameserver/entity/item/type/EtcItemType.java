package org.l2jmobius.gameserver.entity.item.type;

public enum EtcItemType implements ItemType {
   NONE,
   ARROW,
   POTION,
   SCRL_ENCHANT_WP,
   SCRL_ENCHANT_AM,
   SCROLL,
   RECIPE,
   MATERIAL,
   PET_COLLAR,
   CASTLE_GUARD,
   LOTTO,
   RACE_TICKET,
   DYE,
   SEED,
   CROP,
   MATURECROP,
   HARVEST,
   SEED2,
   TICKET_OF_LORD,
   LURE,
   BLESS_SCRL_ENCHANT_WP,
   BLESS_SCRL_ENCHANT_AM,
   COUPON,
   ELIXIR,
   SCRL_ENCHANT_ATTR,
   BOLT,
   SCRL_INC_ENCHANT_PROP_WP,
   SCRL_INC_ENCHANT_PROP_AM,
   ANCIENT_CRYSTAL_ENCHANT_WP,
   ANCIENT_CRYSTAL_ENCHANT_AM,
   RUNE_SELECT,
   RUNE,
   SHOT;

   @Override
   public int mask() {
      return 0;
   }
}
