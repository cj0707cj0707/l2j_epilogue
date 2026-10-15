package org.l2jmobius.gameserver.entity.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.holders.Elementals;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.MaterialType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public abstract class ItemTemplate extends ListenersContainer {
   protected static final Logger LOGGER = Logger.getLogger(ItemTemplate.class.getName());
   public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
   public static final int TYPE1_SHIELD_ARMOR = 1;
   public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
   public static final int TYPE2_WEAPON = 0;
   public static final int TYPE2_SHIELD_ARMOR = 1;
   public static final int TYPE2_ACCESSORY = 2;
   public static final int TYPE2_QUEST = 3;
   public static final int TYPE2_MONEY = 4;
   public static final int TYPE2_OTHER = 5;
   private int _itemId;
   private int _displayId;
   private String _name;
   private String _icon;
   private int _weight;
   private boolean _stackable;
   private MaterialType _materialType;
   private CrystalType _crystalType;
   private int _equipReuseDelay;
   private int _duration;
   private long _time;
   private int _autoDestroyTime;
   private BodyPart _bodyPart;
   private int _referencePrice;
   private int _crystalCount;
   private boolean _sellable;
   private boolean _dropable;
   private boolean _destroyable;
   private boolean _tradeable;
   private boolean _depositable;
   private boolean _enchantable;
   private boolean _elementable;
   private boolean _questItem;
   private boolean _freightable;
   private boolean _allowSelfResurrection;
   private boolean _isOlyRestricted;
   private boolean _forNpc;
   private boolean _common;
   private boolean _heroItem;
   private boolean _pvpItem;
   private boolean _immediateEffect;
   private boolean _exImmediateEffect;
   private int _defaultEnchantLevel;
   private ActionType _defaultAction;
   protected int _type1;
   protected int _type2;
   protected Elementals[] _elementals = null;
   protected List<FuncTemplate> _funcTemplates;
   protected List<Condition> _preConditions;
   private SkillHolder[] _skillHolder;
   private SkillHolder _unequipSkill = null;
   private int _useSkillDisTime;
   protected int _reuseDelay;
   private int _sharedReuseGroup;

   protected ItemTemplate(StatSet set) {
      this.set(set);
   }

   public void set(StatSet set) {
      this._itemId = set.getInt("item_id");
      this._displayId = set.getInt("displayId", this._itemId);
      this._name = set.getString("name");
      this._icon = set.getString("icon", null);
      this._weight = set.getInt("weight", 0);
      this._materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
      this._equipReuseDelay = set.getInt("equip_reuse_delay", 0) * 1000;
      this._duration = set.getInt("duration", -1);
      this._time = set.getInt("time", -1);
      this._autoDestroyTime = set.getInt("auto_destroy_time", -1) * 1000;
      this._bodyPart = BodyPart.fromName(set.getString("bodypart", "none"));
      this._referencePrice = set.getInt("price", 0);
      this._crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
      this._crystalCount = set.getInt("crystal_count", 0);
      this._stackable = set.getBoolean("is_stackable", false);
      this._sellable = set.getBoolean("is_sellable", true);
      this._dropable = set.getBoolean("is_dropable", true);
      this._destroyable = set.getBoolean("is_destroyable", true);
      this._tradeable = set.getBoolean("is_tradable", true);
      this._questItem = set.getBoolean("is_questitem", false);
      this._depositable = set.getBoolean("is_depositable", true);
      this._elementable = set.getBoolean("element_enabled", false);
      this._enchantable = set.getBoolean("enchant_enabled", false);
      this._freightable = set.getBoolean("is_freightable", false);
      this._allowSelfResurrection = set.getBoolean("allow_self_resurrection", false);
      this._isOlyRestricted = set.getBoolean("is_oly_restricted", false);
      this._forNpc = set.getBoolean("for_npc", false);
      this._immediateEffect = set.getBoolean("immediate_effect", false);
      this._exImmediateEffect = set.getBoolean("ex_immediate_effect", false);
      this._defaultAction = set.getEnum("default_action", ActionType.class, ActionType.NONE);
      this._useSkillDisTime = set.getInt("useSkillDisTime", 0);
      this._defaultEnchantLevel = set.getInt("enchanted", 0);
      this._reuseDelay = set.getInt("reuse_delay", 0);
      this._sharedReuseGroup = set.getInt("shared_reuse_group", 0);
      String skills = set.getString("item_skill", null);
      if (skills != null) {
         String[] skillsSplit = skills.split(";");
         this._skillHolder = new SkillHolder[skillsSplit.length];
         int used = 0;

         for (String element : skillsSplit) {
            try {
               String[] skillSplit = element.split("-");
               int id = Integer.parseInt(skillSplit[0]);
               if (id == 0) {
                  LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill id is 0!");
               } else {
                  int level = Integer.parseInt(skillSplit[1]);
                  if (level == 0) {
                     LOGGER.info("Ignoring item_skill(" + element + ") for item " + this + ". Skill level is 0!");
                  } else {
                     this._skillHolder[used] = new SkillHolder(id, level);
                     used++;
                  }
               }
            } catch (Exception var13) {
               LOGGER.warning("Failed to parse item_skill(" + element + ") for item " + this + "! Format: SkillId0-SkillLevel0[;SkillIdN-SkillLevelN]");
            }
         }

         if (used != this._skillHolder.length) {
            SkillHolder[] skillHolder = new SkillHolder[used];
            System.arraycopy(this._skillHolder, 0, skillHolder, 0, used);
            this._skillHolder = skillHolder;
         }
      }

      skills = set.getString("unequip_skill", null);
      if (skills != null) {
         String[] info = skills.split("-");
         if (info != null && info.length == 2) {
            int id = 0;
            int level = 0;

            try {
               id = Integer.parseInt(info[0]);
               level = Integer.parseInt(info[1]);
            } catch (Exception var12) {
               LOGGER.info("Could not parse " + skills + " in weapon unequip skills! item " + this);
            }

            if (id > 0 && level > 0) {
               this._unequipSkill = new SkillHolder(id, level);
            }
         }
      }

      this._common = this._itemId >= 11605 && this._itemId <= 12361;
      this._heroItem = this._itemId >= 6611 && this._itemId <= 6621 || this._itemId >= 9388 && this._itemId <= 9390 || this._itemId == 6842;
      this._pvpItem = this._itemId >= 10667 && this._itemId <= 10835
         || this._itemId >= 12852 && this._itemId <= 12977
         || this._itemId >= 14363 && this._itemId <= 14525
         || this._itemId == 14528
         || this._itemId == 14529
         || this._itemId == 14558
         || this._itemId >= 15913 && this._itemId <= 16024
         || this._itemId >= 16134 && this._itemId <= 16147
         || this._itemId == 16149
         || this._itemId == 16151
         || this._itemId == 16153
         || this._itemId == 16155
         || this._itemId == 16157
         || this._itemId == 16159
         || this._itemId >= 16168 && this._itemId <= 16176
         || this._itemId >= 16179 && this._itemId <= 16220;
   }

   public abstract ItemType getItemType();

   public boolean isEtcItem() {
      return false;
   }

   public boolean isArmor() {
      return false;
   }

   public boolean isWeapon() {
      return false;
   }

   public boolean isMagicWeapon() {
      return false;
   }

   public int getEquipReuseDelay() {
      return this._equipReuseDelay;
   }

   public int getDuration() {
      return this._duration;
   }

   public long getTime() {
      return this._time;
   }

   public int getAutoDestroyTime() {
      return this._autoDestroyTime;
   }

   public int getId() {
      return this._itemId;
   }

   public int getDisplayId() {
      return this._displayId;
   }

   public abstract int getItemMask();

   public MaterialType getMaterialType() {
      return this._materialType;
   }

   public int getType2() {
      return this._type2;
   }

   public int getWeight() {
      return this._weight;
   }

   public boolean isCrystallizable() {
      return this._crystalType != CrystalType.NONE && this._crystalCount > 0;
   }

   public ItemGrade getItemGrade() {
      return ItemGrade.valueOf(this._crystalType);
   }

   public CrystalType getCrystalType() {
      return this._crystalType;
   }

   public int getCrystalItemId() {
      return this._crystalType.getCrystalId();
   }

   public CrystalType getCrystalTypePlus() {
      switch (this._crystalType) {
         case S80:
         case S84:
            return CrystalType.S;
         default:
            return this._crystalType;
      }
   }

   public int getCrystalCount() {
      return this._crystalCount;
   }

   public int getCrystalCount(int enchantLevel) {
      if (enchantLevel > 3) {
         switch (this._type2) {
            case 0:
               return this._crystalCount + this._crystalType.getCrystalEnchantBonusWeapon() * (2 * enchantLevel - 3);
            case 1:
            case 2:
               return this._crystalCount + this._crystalType.getCrystalEnchantBonusArmor() * (3 * enchantLevel - 6);
            default:
               return this._crystalCount;
         }
      } else if (enchantLevel <= 0) {
         return this._crystalCount;
      } else {
         switch (this._type2) {
            case 0:
               return this._crystalCount + this._crystalType.getCrystalEnchantBonusWeapon() * enchantLevel;
            case 1:
            case 2:
               return this._crystalCount + this._crystalType.getCrystalEnchantBonusArmor() * enchantLevel;
            default:
               return this._crystalCount;
         }
      }
   }

   public String getName() {
      return this._name;
   }

   public Elementals[] getElementals() {
      return this._elementals;
   }

   public Elementals getElemental(byte attribute) {
      for (Elementals elm : this._elementals) {
         if (elm.getElement() == attribute) {
            return elm;
         }
      }

      return null;
   }

   public void setElementals(Elementals element) {
      if (this._elementals == null) {
         this._elementals = new Elementals[1];
         this._elementals[0] = element;
      } else {
         Elementals elm = this.getElemental(element.getElement());
         if (elm != null) {
            elm.setValue(element.getValue());
         } else {
            Elementals[] array = new Elementals[this._elementals.length + 1];
            System.arraycopy(this._elementals, 0, array, 0, this._elementals.length);
            array[this._elementals.length] = element;
            this._elementals = array;
         }
      }
   }

   public BodyPart getBodyPart() {
      return this._bodyPart;
   }

   public int getType1() {
      return this._type1;
   }

   public boolean isStackable() {
      return this._stackable;
   }

   public boolean isEquipable() {
      return this._bodyPart != BodyPart.NONE && !(this.getItemType() instanceof EtcItemType);
   }

   public int getReferencePrice() {
      return this._referencePrice;
   }

   public boolean isSellable() {
      return this._sellable;
   }

   public boolean isDropable() {
      return this._dropable;
   }

   public boolean isDestroyable() {
      return this._destroyable;
   }

   public boolean isTradeable() {
      return this._tradeable;
   }

   public boolean isDepositable() {
      return this._depositable;
   }

   public boolean isEnchantable() {
      return Arrays.binarySearch(PlayerConfig.ENCHANT_BLACKLIST, this._itemId) < 0 && this._enchantable;
   }

   public boolean isElementable() {
      return this._elementable;
   }

   public boolean isCommon() {
      return this._common;
   }

   public boolean isHeroItem() {
      return this._heroItem;
   }

   public boolean isPvpItem() {
      return this._pvpItem;
   }

   public boolean isPotion() {
      return this.getItemType() == EtcItemType.POTION;
   }

   public boolean isElixir() {
      return this.getItemType() == EtcItemType.ELIXIR;
   }

   public boolean isScroll() {
      return this.getItemType() == EtcItemType.SCROLL;
   }

   public List<AbstractFunction> getStatFuncs(Item item, Creature creature) {
      if (this._funcTemplates != null && !this._funcTemplates.isEmpty()) {
         List<AbstractFunction> funcs = new ArrayList<>(this._funcTemplates.size());

         for (FuncTemplate t : this._funcTemplates) {
            AbstractFunction f = t.getFunc(creature, creature, item, item);
            if (f != null) {
               funcs.add(f);
            }
         }

         return funcs;
      } else {
         return Collections.emptyList();
      }
   }

   public boolean hasFunction(Class<? extends AbstractFunction> functionClass) {
      if (this._funcTemplates != null && !this._funcTemplates.isEmpty()) {
         for (FuncTemplate template : this._funcTemplates) {
            if (template.getClass().equals(functionClass)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void attach(FuncTemplate f) {
      switch (f.getStat()) {
         case FIRE_RES:
         case FIRE_POWER:
            this.setElementals(new Elementals((byte)0, (int)f.getValue()));
            break;
         case WATER_RES:
         case WATER_POWER:
            this.setElementals(new Elementals((byte)1, (int)f.getValue()));
            break;
         case WIND_RES:
         case WIND_POWER:
            this.setElementals(new Elementals((byte)2, (int)f.getValue()));
            break;
         case EARTH_RES:
         case EARTH_POWER:
            this.setElementals(new Elementals((byte)3, (int)f.getValue()));
            break;
         case HOLY_RES:
         case HOLY_POWER:
            this.setElementals(new Elementals((byte)4, (int)f.getValue()));
            break;
         case DARK_RES:
         case DARK_POWER:
            this.setElementals(new Elementals((byte)5, (int)f.getValue()));
      }

      if (this._funcTemplates == null) {
         this._funcTemplates = new ArrayList<>(1);
      }

      this._funcTemplates.add(f);
   }

   public void attach(Condition c) {
      if (this._preConditions == null) {
         this._preConditions = new ArrayList<>(1);
      }

      if (!this._preConditions.contains(c)) {
         this._preConditions.add(c);
      }
   }

   public boolean hasSkills() {
      return this._skillHolder != null;
   }

   public SkillHolder[] getSkills() {
      return this._skillHolder;
   }

   public Skill getUnequipSkill() {
      return this._unequipSkill == null ? null : this._unequipSkill.getSkill();
   }

   public boolean checkCondition(Creature creature, WorldObject object, boolean sendMessage) {
      if (creature.isGM() && !GeneralConfig.GM_ITEM_RESTRICTION) {
         return true;
      } else if ((this.isOlyRestrictedItem() || this._heroItem) && creature.isPlayer() && creature.asPlayer().isInOlympiadMode()) {
         if (this.isEquipable()) {
            creature.sendPacket(SystemMessageId.YOU_CANNOT_EQUIP_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
         } else {
            creature.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
         }

         return false;
      } else if (!this.isConditionAttached()) {
         return true;
      } else {
         Creature target = object.isCreature() ? object.asCreature() : null;

         for (Condition preCondition : this._preConditions) {
            if (preCondition != null && !preCondition.test(creature, target, null, null)) {
               if (creature.isSummon()) {
                  creature.sendPacket(SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
                  return false;
               }

               if (sendMessage) {
                  String msg = preCondition.getMessage();
                  int msgId = preCondition.getMessageId();
                  if (msg != null) {
                     creature.sendMessage(msg);
                  } else if (msgId != 0) {
                     SystemMessage sm = new SystemMessage(msgId);
                     if (preCondition.isAddName()) {
                        sm.addItemName(this._itemId);
                     }

                     creature.sendPacket(sm);
                  }
               }

               return false;
            }
         }

         return true;
      }
   }

   public boolean isConditionAttached() {
      return this._preConditions != null && !this._preConditions.isEmpty();
   }

   public boolean isQuestItem() {
      return this._questItem;
   }

   public boolean isFreightable() {
      return this._freightable;
   }

   public boolean isAllowSelfResurrection() {
      return this._allowSelfResurrection;
   }

   public boolean isOlyRestrictedItem() {
      return this._isOlyRestricted || OlympiadConfig.LIST_OLY_RESTRICTED_ITEMS.contains(this._itemId);
   }

   public boolean isForNpc() {
      return this._forNpc;
   }

   public boolean hasExImmediateEffect() {
      return this._exImmediateEffect;
   }

   public boolean hasImmediateEffect() {
      return this._immediateEffect;
   }

   public ActionType getDefaultAction() {
      return this._defaultAction;
   }

   public int useSkillDisTime() {
      return this._useSkillDisTime;
   }

   public int getReuseDelay() {
      return this._reuseDelay;
   }

   public int getSharedReuseGroup() {
      return this._sharedReuseGroup;
   }

   public String getIcon() {
      return this._icon;
   }

   public int getDefaultEnchantLevel() {
      return this._defaultEnchantLevel;
   }

   public boolean isPetItem() {
      return this.getItemType() == EtcItemType.PET_COLLAR;
   }

   public void addCapsuledItem(ExtractableProduct extractableProduct) {
   }

   public Skill getEnchant4Skill() {
      return null;
   }

   @Override
   public String toString() {
      return StringUtil.concat(this._name, "(", String.valueOf(this._itemId), ")");
   }
}
