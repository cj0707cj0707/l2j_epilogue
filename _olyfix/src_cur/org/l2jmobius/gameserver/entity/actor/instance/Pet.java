package org.l2jmobius.gameserver.entity.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.stat.PetStat;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

public class Pet extends Summon {
   protected static final Logger LOGGER_PET = Logger.getLogger(Pet.class.getName());
   private static final String ADD_SKILL_SAVE = "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?)";
   private static final String RESTORE_SKILL_SAVE = "SELECT petObjItemId,skill_id,skill_level,remaining_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC";
   private static final String DELETE_SKILL_SAVE = "DELETE FROM character_pet_skills_save WHERE petObjItemId=?";
   int _curFed;
   final PetInventory _inventory;
   private final int _controlObjectId;
   private boolean _respawned;
   private final boolean _mountable;
   private Future<?> _feedTask;
   private PetData _data;
   private PetLevelData _leveldata;
   private long _expBeforeDeath = 0L;
   private int _curWeightPenalty = 0;

   public Pet(NpcTemplate template, Player owner, Item control) {
      this(template, owner, control, (byte)(template.getDisplayId() == 12564 ? owner.getLevel() : template.getLevel()));
   }

   public Pet(NpcTemplate template, Player owner, Item control, byte level) {
      super(template, owner);
      this.setInstanceType(InstanceType.Pet);
      this._controlObjectId = control.getObjectId();
      this.getStat().setLevel((byte)Math.max(level, PetDataTable.getInstance().getPetMinLevel(template.getId())));
      this._inventory = new PetInventory(this);
      this._inventory.restore();
      int npcId = template.getId();
      this._mountable = PetDataTable.isMountable(npcId);
      this.getPetData();
      this.getPetLevelData();
   }

   public PetLevelData getPetLevelData() {
      if (this._leveldata == null) {
         this._leveldata = PetDataTable.getInstance().getPetLevelData(this.getTemplate().getId(), this.getStat().getLevel());
      }

      return this._leveldata;
   }

   public PetData getPetData() {
      if (this._data == null) {
         this._data = PetDataTable.getInstance().getPetData(this.getTemplate().getId());
      }

      return this._data;
   }

   public void setPetData(PetLevelData value) {
      this._leveldata = value;
   }

   public static synchronized Pet spawnPet(NpcTemplate template, Player owner, Item control) {
      Pet existingPet = World.getPet(owner.getObjectId());
      if (existingPet != null) {
         existingPet.unSummon(owner);
      }

      PetData data = PetDataTable.getInstance().getPetData(template.getId());
      Pet pet = restore(control, template, owner);
      if (pet != null) {
         pet.setTitle(owner.getName());
         if (data.isSynchLevel() && pet.getLevel() != owner.getLevel()) {
            byte availableLevel = (byte)Math.min(data.getMaxLevel(), owner.getLevel());
            pet.getStat().setLevel(availableLevel);
            pet.getStat().setExp(pet.getStat().getExpForLevel(availableLevel));
         }

         World.addPet(owner.getObjectId(), pet);
      }

      return pet;
   }

   public PetStat getStat() {
      return (PetStat)super.getStat();
   }

   @Override
   public void initCharStat() {
      this.setStat(new PetStat(this));
   }

   public boolean isRespawned() {
      return this._respawned;
   }

   @Override
   public int getSummonType() {
      return 2;
   }

   @Override
   public int getControlObjectId() {
      return this._controlObjectId;
   }

   public Item getControlItem() {
      return this.getOwner().getInventory().getItemByObjectId(this._controlObjectId);
   }

   public int getCurrentFed() {
      return this._curFed;
   }

   public void setCurrentFed(int num) {
      if (num <= 0) {
         this.sendPacket(new ExChangeNpcState(this.getObjectId(), 100));
      } else if (this._curFed <= 0 && num > 0) {
         this.sendPacket(new ExChangeNpcState(this.getObjectId(), 101));
      }

      this._curFed = num > this.getMaxFed() ? this.getMaxFed() : num;
   }

   @Override
   public Item getActiveWeaponInstance() {
      for (Item item : this._inventory.getItems()) {
         if (item.getItemLocation() == ItemLocation.PET_EQUIP && item.getTemplate().getBodyPart() == BodyPart.R_HAND) {
            return item;
         }
      }

      return null;
   }

   @Override
   public Weapon getActiveWeaponItem() {
      Item weapon = this.getActiveWeaponInstance();
      return weapon == null ? null : (Weapon)weapon.getTemplate();
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return null;
   }

   @Override
   public Weapon getSecondaryWeaponItem() {
      return null;
   }

   @Override
   public PetInventory getInventory() {
      return this._inventory;
   }

   @Override
   public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage) {
      Item item = this._inventory.destroyItem(process, objectId, count, this.getOwner(), reference);
      if (item == null) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         PetInventoryUpdate petIU = new PetInventoryUpdate();
         petIU.addItem(item);
         this.sendPacket(petIU);
         if (sendMessage) {
            if (count > 1L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
               sm.addItemName(item.getId());
               sm.addLong(count);
            } else {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
               sm.addItemName(item.getId());
            }
         }

         return true;
      }
   }

   @Override
   public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage) {
      Item item = this._inventory.destroyItemByItemId(process, itemId, count, this.getOwner(), reference);
      if (item == null) {
         if (sendMessage) {
            this.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
         }

         return false;
      } else {
         PetInventoryUpdate petIU = new PetInventoryUpdate();
         petIU.addItem(item);
         this.sendPacket(petIU);
         if (sendMessage) {
            SystemMessage sm;
            if (count > 1L) {
               sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
               sm.addItemName(item.getId());
               sm.addLong(count);
            } else {
               sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
               sm.addItemName(item.getId());
            }

            this.sendPacket(sm);
         }

         return true;
      }
   }

   @Override
   public void doPickupItem(WorldObject object) {
      if (!this.isDead()) {
         this.getAI().setIntentionIdle();
         this.broadcastPacket(new StopMove(this));
         if (!object.isItem()) {
            LOGGER_PET.warning(this + " trying to pickup wrong target." + object);
            this.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            Item target = (Item)object;
            if (CursedWeaponsManager.getInstance().isCursed(target.getId())) {
               SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
               smsg.addItemName(target.getId());
               this.sendPacket(smsg);
            } else if (!FortSiegeManager.getInstance().isCombat(target.getId())) {
               boolean follow = this.getFollowStatus();
               SystemMessage smsg = null;
               synchronized (target) {
                  if (!target.isSpawned()) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (!target.getDropProtection().tryPickUp(this)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                     smsg.addItemName(target);
                     this.sendPacket(smsg);
                     return;
                  }

                  if ((this.isInParty() && this.getParty().getDistributionType() == PartyDistributionType.FINDERS_KEEPERS || !this.isInParty())
                     && !this._inventory.validateCapacity(target)) {
                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
                     return;
                  }

                  if (target.getOwnerId() != 0 && target.getOwnerId() != this.getOwner().getObjectId() && !this.getOwner().isInLooterParty(target.getOwnerId())
                     )
                   {
                     if (target.getId() == 57) {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
                        smsg.addLong(target.getCount());
                     } else if (target.getCount() > 1L) {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
                        smsg.addItemName(target);
                        smsg.addLong(target.getCount());
                     } else {
                        smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
                        smsg.addItemName(target);
                     }

                     this.sendPacket(ActionFailed.STATIC_PACKET);
                     this.sendPacket(smsg);
                     return;
                  }

                  if (target.getItemLootShedule() != null
                     && (target.getOwnerId() == this.getOwner().getObjectId() || this.getOwner().isInLooterParty(target.getOwnerId()))) {
                     target.resetOwnerTimer();
                  }

                  target.pickupMe(this);
                  if (GeneralConfig.SAVE_DROPPED_ITEM) {
                     ItemsOnGroundManager.getInstance().removeObject(target);
                  }
               }

               if (target.getTemplate().hasExImmediateEffect()) {
                  IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
                  if (handler == null) {
                     LOGGER.warning("No item handler registered for item ID: " + target.getId() + ".");
                  } else {
                     handler.onItemUse(this, target, false);
                  }

                  ItemManager.destroyItem(ItemProcessType.NONE, target, this.getOwner(), null);
                  this.broadcastStatusUpdate();
               } else {
                  if (target.getId() == 57) {
                     smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_ADENA);
                     smsg.addLong(target.getCount());
                     this.sendPacket(smsg);
                  } else if (target.isEnchanted()) {
                     smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_S2);
                     smsg.addInt(target.getEnchantLevel());
                     smsg.addItemName(target);
                     this.sendPacket(smsg);
                  } else if (target.getCount() > 1L) {
                     smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S2_S1_S);
                     smsg.addLong(target.getCount());
                     smsg.addItemName(target);
                     this.sendPacket(smsg);
                  } else {
                     smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1);
                     smsg.addItemName(target);
                     this.sendPacket(smsg);
                  }

                  if (this.getOwner().isInParty() && this.getOwner().getParty().getDistributionType() != PartyDistributionType.FINDERS_KEEPERS) {
                     this.getOwner().getParty().distributeItem(this.getOwner(), target);
                  } else {
                     Item item = this._inventory.addItem(ItemProcessType.PICKUP, target, this.getOwner(), this);
                     this.sendPacket(new PetInventoryUpdate(item));
                  }
               }

               this.getAI().setIntentionIdle();
               if (follow) {
                  this.followOwner();
               }
            }
         }
      }
   }

   @Override
   public void deleteMe(Player owner) {
      this._inventory.transferItemsToOwner();
      super.deleteMe(owner);
      this.destroyControlItem(owner, false);
      CharSummonTable.getInstance().getPets().remove(this.getOwner().getObjectId());
   }

   @Override
   public boolean doDie(Creature killer) {
      Player owner = this.getOwner();
      if (owner != null && !owner.isInDuel() && (!this.isInsideZone(ZoneId.PVP) || this.isInsideZone(ZoneId.SIEGE))) {
         this.deathPenalty();
      }

      if (!super.doDie(killer, true)) {
         return false;
      } else {
         this.stopFeed();
         this.sendPacket(
            SystemMessageId.THE_PET_HAS_BEEN_KILLED_IF_YOU_DO_NOT_RESURRECT_IT_WITHIN_24_HOURS_THE_PET_S_BODY_WILL_DISAPPEAR_ALONG_WITH_ALL_THE_PET_S_ITEMS
         );
         DecayTaskManager.getInstance().add(this);
         return true;
      }
   }

   @Override
   public void doRevive() {
      this.getOwner().removeReviving();
      super.doRevive();
      DecayTaskManager.getInstance().cancel(this);
      this.startFeed();
      if (!this.isHungry()) {
         this.setRunning();
      }

      this.getAI().setIntentionActive();
   }

   @Override
   public void doRevive(double revivePower) {
      this.restoreExp(revivePower);
      this.doRevive();
   }

   public Item transferItem(ItemProcessType process, int objectId, long count, Inventory target, Player actor, WorldObject reference) {
      Item oldItem = this._inventory.getItemByObjectId(objectId);
      Item playerOldItem = target.getItemByItemId(oldItem.getId());
      Item newItem = this._inventory.transferItem(process, objectId, count, target, actor, reference);
      if (newItem == null) {
         return null;
      } else {
         PetInventoryUpdate petIU = new PetInventoryUpdate();
         if (oldItem.getCount() > 0L && oldItem != newItem) {
            petIU.addModifiedItem(oldItem);
         } else {
            petIU.addRemovedItem(oldItem);
         }

         this.sendPacket(petIU);
         if (!newItem.isStackable()) {
            Player owner = this.getOwner();
            if (owner != null) {
               InventoryUpdate iu = new InventoryUpdate();
               iu.addNewItem(newItem);
               owner.sendInventoryUpdate(iu);
            }
         } else if (playerOldItem != null && newItem.isStackable()) {
            Player owner = this.getOwner();
            if (owner != null) {
               InventoryUpdate iu = new InventoryUpdate();
               iu.addModifiedItem(newItem);
               owner.sendInventoryUpdate(iu);
            }
         }

         return newItem;
      }
   }

   public void destroyControlItem(Player owner, boolean evolve) {
      World.removePet(owner.getObjectId());

      try {
         Item removedItem;
         if (evolve) {
            removedItem = owner.getInventory().destroyItem(ItemProcessType.FEE, this._controlObjectId, 1L, this.getOwner(), this);
         } else {
            removedItem = owner.getInventory().destroyItem(ItemProcessType.DESTROY, this._controlObjectId, 1L, this.getOwner(), this);
            if (removedItem != null) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
               sm.addItemName(removedItem);
               owner.sendPacket(sm);
            }
         }

         if (removedItem == null) {
            LOGGER.warning("Couldn't destroy pet control item for " + owner + " pet: " + this + " evolve: " + evolve);
         } else {
            InventoryUpdate iu = new InventoryUpdate();
            iu.addRemovedItem(removedItem);
            owner.sendInventoryUpdate(iu);
            StatusUpdate su = new StatusUpdate(owner);
            su.addAttribute(14, owner.getCurrentLoad());
            owner.sendPacket(su);
            owner.broadcastUserInfo();
            World.removeObject(removedItem);
         }
      } catch (Exception var9) {
         LOGGER_PET.log(Level.WARNING, "Error while destroying control item: " + var9.getMessage(), (Throwable)var9);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?");
      ) {
         ps.setInt(1, this._controlObjectId);
         ps.execute();
      } catch (Exception var12) {
         LOGGER_PET.log(Level.SEVERE, "Failed to delete Pet [ObjectId: " + this.getObjectId() + "]", (Throwable)var12);
      }
   }

   public void dropAllItems() {
      try {
         for (Item item : this._inventory.getItems()) {
            this.dropItemHere(item);
         }
      } catch (Exception var3) {
         LOGGER_PET.log(Level.WARNING, "Pet Drop Error: " + var3.getMessage(), (Throwable)var3);
      }
   }

   public void dropItemHere(Item item, boolean protect) {
      Item dropit = this._inventory.dropItem(ItemProcessType.DROP, item.getObjectId(), item.getCount(), this.getOwner(), this);
      if (dropit != null) {
         if (protect) {
            dropit.getDropProtection().protect(this.getOwner());
         }

         LOGGER_PET.finer("Item id to drop: " + dropit.getId() + " amount: " + dropit.getCount());
         dropit.dropMe(this, this.getX(), this.getY(), this.getZ() + 100);
      }
   }

   public void dropItemHere(Item dropit) {
      this.dropItemHere(dropit, false);
   }

   @Override
   public boolean isMountable() {
      return this._mountable;
   }

   private static Pet restore(Item control, NpcTemplate template, Player owner) {
      try {
         Object var18;
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?");
         ) {
            ps.setInt(1, control.getObjectId());

            Pet pet;
            try (ResultSet rs = ps.executeQuery()) {
               if (!rs.next()) {
                  if (template.isType("BabyPet")) {
                     pet = new BabyPet(template, owner, control);
                  } else {
                     pet = new Pet(template, owner, control);
                  }

                  return pet;
               }

               if (template.isType("BabyPet")) {
                  pet = new BabyPet(template, owner, control, rs.getByte("level"));
               } else {
                  pet = new Pet(template, owner, control, rs.getByte("level"));
               }

               pet._respawned = true;
               pet.setName(rs.getString("name"));
               long exp = rs.getLong("exp");
               PetLevelData info = PetDataTable.getInstance().getPetLevelData(pet.getId(), pet.getLevel());
               if (info != null && exp < info.getPetMaxExp()) {
                  exp = info.getPetMaxExp();
               }

               pet.getStat().setExp(exp);
               pet.getStat().setSp(rs.getInt("sp"));
               pet.getStatus().setCurrentHp(rs.getInt("curHp"));
               pet.getStatus().setCurrentMp(rs.getInt("curMp"));
               pet.getStatus().setCurrentCp(pet.getMaxCp());
               if (rs.getDouble("curHp") < 1.0) {
                  pet.setDead(true);
                  pet.stopHpMpRegeneration();
               }

               pet.setCurrentFed(rs.getInt("fed"));
            }

            var18 = pet;
         }

         return (Pet)var18;
      } catch (Exception var16) {
         LOGGER_PET.log(Level.WARNING, "Could not restore pet data for owner: " + owner + " - " + var16.getMessage(), (Throwable)var16);
         return null;
      }
   }

   @Override
   public void setRestoreSummon(boolean value) {
      this._restoreSummon = value;
   }

   @Override
   public void stopSkillEffects(SkillFinishType type, int skillId) {
      super.stopSkillEffects(type, skillId);
      SummonEffectTable.getInstance().removePetEffects(this.getControlObjectId(), skillId);
   }

   @Override
   public void storeMe() {
      if (this._controlObjectId != 0) {
         if (!PlayerConfig.RESTORE_PET_ON_RECONNECT) {
            this._restoreSummon = false;
         }

         String req = !this._respawned
            ? "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,ownerId,restore,item_obj_id) VALUES (?,?,?,?,?,?,?,?,?,?)"
            : "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,ownerId=?,restore=? WHERE item_obj_id = ?";

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(req);
         ) {
            ps.setString(1, this.getName());
            ps.setInt(2, this.getStat().getLevel());
            ps.setDouble(3, this.getStatus().getCurrentHp());
            ps.setDouble(4, this.getStatus().getCurrentMp());
            ps.setLong(5, this.getStat().getExp());
            ps.setLong(6, this.getStat().getSp());
            ps.setInt(7, this._curFed);
            ps.setInt(8, this.getOwner().getObjectId());
            ps.setString(9, String.valueOf(this._restoreSummon));
            ps.setInt(10, this._controlObjectId);
            ps.executeUpdate();
            this._respawned = true;
            if (this._restoreSummon) {
               CharSummonTable.getInstance().getPets().put(this.getOwner().getObjectId(), this.getControlObjectId());
            } else {
               CharSummonTable.getInstance().getPets().remove(this.getOwner().getObjectId());
            }
         } catch (Exception var10) {
            LOGGER_PET.log(Level.SEVERE, "Failed to store Pet [ObjectId: " + this.getObjectId() + "] data", (Throwable)var10);
         }

         Item itemInst = this.getControlItem();
         if (itemInst != null && itemInst.getEnchantLevel() != this.getStat().getLevel()) {
            itemInst.setEnchantLevel(this.getStat().getLevel());
            itemInst.updateDatabase();
         }
      }
   }

   @Override
   public void storeEffect(boolean storeEffects) {
      if (PlayerConfig.SUMMON_STORE_SKILL_COOLTIME) {
         SummonEffectTable.getInstance().clearPetEffects(this.getControlObjectId());

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM character_pet_skills_save WHERE petObjItemId=?");
            PreparedStatement ps2 = con.prepareStatement(
               "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?)"
            );
         ) {
            ps1.setInt(1, this._controlObjectId);
            ps1.execute();
            int buffIndex = 0;
            List<Integer> storedSkills = new ArrayList<>();
            if (storeEffects) {
               for (BuffInfo info : this.getEffectList().getEffects()) {
                  if (info != null) {
                     Skill skill = info.getSkill();
                     if (skill.getAbnormalType() != AbnormalType.LIFE_FORCE_OTHERS
                        && !skill.isToggle()
                        && (!skill.isDance() || PlayerConfig.ALT_STORE_DANCES)
                        && !storedSkills.contains(skill.getReuseHashCode())) {
                        storedSkills.add(skill.getReuseHashCode());
                        ps2.setInt(1, this._controlObjectId);
                        ps2.setInt(2, skill.getId());
                        ps2.setInt(3, skill.getLevel());
                        ps2.setInt(4, info.getTime());
                        ps2.setInt(5, ++buffIndex);
                        ps2.addBatch();
                        SummonEffectTable.getInstance().addPetEffect(this.getControlObjectId(), skill, info.getTime());
                     }
                  }
               }

               ps2.executeBatch();
            }
         } catch (Exception var16) {
            LOGGER.log(Level.WARNING, "Could not store pet effect data: ", (Throwable)var16);
         }
      }
   }

   @Override
   public void restoreEffects() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement(
            "SELECT petObjItemId,skill_id,skill_level,remaining_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC"
         );
         PreparedStatement ps2 = con.prepareStatement("DELETE FROM character_pet_skills_save WHERE petObjItemId=?");
      ) {
         if (!SummonEffectTable.getInstance().containsPetId(this.getControlObjectId())) {
            ps1.setInt(1, this._controlObjectId);

            try (ResultSet rs = ps1.executeQuery()) {
               while (rs.next()) {
                  Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
                  if (skill != null && skill.hasEffects(EffectScope.GENERAL)) {
                     SummonEffectTable.getInstance().addPetEffect(this.getControlObjectId(), skill, rs.getInt("remaining_time"));
                  }
               }
            }
         }

         ps2.setInt(1, this._controlObjectId);
         ps2.executeUpdate();
      } catch (Exception var26) {
         LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + var26.getMessage(), (Throwable)var26);
      } finally {
         SummonEffectTable.getInstance().applyPetEffects(this, this.getControlObjectId());
      }
   }

   public synchronized void stopFeed() {
      if (this._feedTask != null) {
         this._feedTask.cancel(false);
         this._feedTask = null;
      }
   }

   public synchronized void startFeed() {
      this.stopFeed();
      if (!this.isDead() && this.getOwner().getSummon() == this) {
         this._feedTask = ThreadPool.scheduleAtFixedRate(new Pet.FeedTask(), 10000L, 10000L);
      }
   }

   @Override
   public synchronized void unSummon(Player owner) {
      this.stopFeed();
      super.unSummon(owner);
      if (!this.isDead()) {
         if (this._inventory != null) {
            this._inventory.deleteMe();
         }

         World.removePet(owner.getObjectId());
      }
   }

   public void restoreExp(double restorePercent) {
      if (this._expBeforeDeath > 0L) {
         this.getStat().addExp(Math.round((this._expBeforeDeath - this.getStat().getExp()) * restorePercent / 100.0));
         this._expBeforeDeath = 0L;
      }
   }

   private void deathPenalty() {
      int level = this.getStat().getLevel();
      double percentLost = -0.07 * level + 6.5;
      long lostExp = Math.round((this.getStat().getExpForLevel(level + 1) - this.getStat().getExpForLevel(level)) * percentLost / 100.0);
      this._expBeforeDeath = this.getStat().getExp();
      this.getStat().addExp(-lostExp);
   }

   @Override
   public synchronized void addExpAndSp(double addToExp, double addToSp) {
      if (this.getId() == 12564) {
         this.getStat().addExpAndSp(Math.round(addToExp * RatesConfig.SINEATER_XP_RATE), addToSp);
      } else {
         this.getStat().addExpAndSp(Math.round(addToExp * RatesConfig.PET_XP_RATE), addToSp);
      }
   }

   @Override
   public long getExpForThisLevel() {
      return this.getStat().getExpForLevel(this.getLevel());
   }

   @Override
   public long getExpForNextLevel() {
      return this.getStat().getExpForLevel(this.getLevel() + 1);
   }

   @Override
   public int getLevel() {
      return this.getStat().getLevel();
   }

   public int getMaxFed() {
      return this.getStat().getMaxFeed();
   }

   @Override
   public int getCriticalHit(Creature target, Skill skill) {
      return this.getStat().getCriticalHit(target, skill);
   }

   @Override
   public int getSkillLevel(int skillId) {
      if (this.getKnownSkill(skillId) == null) {
         return 0;
      } else {
         int level = this.getLevel();
         return level > 70 ? 7 + (level - 70) / 5 : level / 10;
      }
   }

   public void updateRefOwner(Player owner) {
      int oldOwnerId = this.getOwner().getObjectId();
      this.setOwner(owner);
      World.removePet(oldOwnerId);
      World.addPet(oldOwnerId, this);
   }

   public int getInventoryLimit() {
      return NpcConfig.INVENTORY_MAXIMUM_PET;
   }

   public void refreshOverloaded() {
      if (this.getInventory() != null && this.getOwner() != null) {
         int maxLoad = this.getMaxLoad();
         if (maxLoad > 0) {
            long weightproc = (this.getCurrentLoad() - this.getBonusWeightPenalty()) * 1000 / maxLoad;
            int newWeightPenalty;
            if (weightproc < 500L || this.getOwner().getDietMode()) {
               newWeightPenalty = 0;
            } else if (weightproc < 666L) {
               newWeightPenalty = 1;
            } else if (weightproc < 800L) {
               newWeightPenalty = 2;
            } else if (weightproc < 1000L) {
               newWeightPenalty = 3;
            } else {
               newWeightPenalty = 4;
            }

            if (this._curWeightPenalty != newWeightPenalty) {
               this._curWeightPenalty = newWeightPenalty;
               if (newWeightPenalty > 0) {
                  this.addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
                  this.setOverloaded(this.getCurrentLoad() >= maxLoad);
               } else {
                  this.removeSkill(this.getKnownSkill(4270), true);
                  this.setOverloaded(false);
               }
            }
         }
      }
   }

   @Override
   public void updateAndBroadcastStatus(int value) {
      if (this.getInventory() != null && this.getOwner() != null) {
         this.refreshOverloaded();
      }

      super.updateAndBroadcastStatus(value);
   }

   @Override
   public boolean isHungry() {
      return this._curFed < this.getPetData().getHungryLimit() / 100.0F * this.getPetLevelData().getPetMaxFeed();
   }

   public boolean isUncontrollable() {
      return this._curFed <= 0;
   }

   @Override
   public int getWeapon() {
      Item weapon = this._inventory.getPaperdollItem(5);
      return weapon != null ? weapon.getId() : 0;
   }

   @Override
   public int getArmor() {
      Item weapon = this._inventory.getPaperdollItem(6);
      return weapon != null ? weapon.getId() : 0;
   }

   public int getJewel() {
      Item weapon = this._inventory.getPaperdollItem(4);
      return weapon != null ? weapon.getId() : 0;
   }

   @Override
   public short getSoulShotsPerHit() {
      return this.getPetLevelData().getPetSoulShot();
   }

   @Override
   public short getSpiritShotsPerHit() {
      return this.getPetLevelData().getPetSpiritShot();
   }

   @Override
   public void setName(String name) {
      Item controlItem = this.getControlItem();
      if (controlItem != null) {
         if (controlItem.getCustomType2() == (name == null ? 1 : 0)) {
            controlItem.setCustomType2(name != null ? 1 : 0);
            controlItem.updateDatabase();
            Player owner = this.getOwner();
            if (owner != null) {
               InventoryUpdate iu = new InventoryUpdate();
               iu.addModifiedItem(controlItem);
               owner.sendInventoryUpdate(iu);
            }
         }
      } else {
         LOGGER.log(Level.WARNING, "Pet control item null, for pet: " + this.toString());
      }

      super.setName(name);
   }

   public boolean canEatFoodId(int itemId) {
      return this._data.getFood().contains(itemId);
   }

   @Override
   public boolean isPet() {
      return true;
   }

   @Override
   public Pet asPet() {
      return this;
   }

   @Override
   public double getRunSpeed() {
      return super.getRunSpeed() * (this.isUncontrollable() ? 0.5 : 1.0);
   }

   @Override
   public double getWalkSpeed() {
      return super.getWalkSpeed() * (this.isUncontrollable() ? 0.5 : 1.0);
   }

   @Override
   public double getMovementSpeedMultiplier() {
      return super.getMovementSpeedMultiplier() * (this.isUncontrollable() ? 0.5 : 1.0);
   }

   @Override
   public double getMoveSpeed() {
      if (this.isInsideZone(ZoneId.WATER)) {
         return this.isRunning() ? this.getSwimRunSpeed() : this.getSwimWalkSpeed();
      } else {
         return this.isRunning() ? this.getRunSpeed() : this.getWalkSpeed();
      }
   }

   class FeedTask implements Runnable {
      FeedTask() {
         Objects.requireNonNull(Pet.this);
         super();
      }

      @Override
      public void run() {
         try {
            if (Pet.this.getOwner() == null || !Pet.this.getOwner().hasSummon() || Pet.this.getOwner().getSummon().getObjectId() != Pet.this.getObjectId()) {
               Pet.this.stopFeed();
               return;
            }

            Pet.this.setCurrentFed(Pet.this._curFed > this.getFeedConsume() ? Pet.this._curFed - this.getFeedConsume() : 0);
            Pet.this.broadcastStatusUpdate();
            if (Pet.this.isUncontrollable()) {
               if (Rnd.get(100) < 30) {
                  if (Pet.this.getTemplate().getId() == 16050 && Pet.this.getOwner() != null) {
                     Pet.this.getOwner().setPkKills(Math.max(0, Pet.this.getOwner().getPkKills() - Rnd.get(1, 6)));
                  }

                  Pet.this.sendPacket(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
                  Pet.this.unSummon(Pet.this.getOwner());
                  Pet.this.stopFeed();
                  return;
               }

               Pet.this.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
               return;
            }

            Set<Integer> foodIds = Pet.this.getPetData().getFood();
            if (foodIds.isEmpty()) {
               if (Pet.this.isHungry()) {
                  Pet.this.sendPacket(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
               }

               return;
            }

            Item food = null;

            for (int id : foodIds) {
               food = Pet.this._inventory.getItemByItemId(id);
               if (food != null) {
                  break;
               }
            }

            if (food != null && Pet.this.isHungry()) {
               IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
               if (handler != null) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_WAS_HUNGRY_SO_IT_ATE_S1);
                  sm.addItemName(food.getId());
                  Pet.this.sendPacket(sm);
                  handler.onItemUse(Pet.this, food, false);
               }
            }

            if (Pet.this.isUncontrollable()) {
               Pet.this.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
            }
         } catch (Exception var5) {
            Pet.LOGGER_PET.log(Level.SEVERE, "Pet [ObjectId: " + Pet.this.getObjectId() + "] a feed task error has occurred", (Throwable)var5);
         }
      }

      private int getFeedConsume() {
         return Pet.this.isAttackingNow() ? Pet.this.getPetLevelData().getPetFeedBattle() : Pet.this.getPetLevelData().getPetFeedNormal();
      }
   }
}
