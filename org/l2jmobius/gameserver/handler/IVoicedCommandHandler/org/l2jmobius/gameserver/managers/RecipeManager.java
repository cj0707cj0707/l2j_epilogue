package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.StatType;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.holders.RecipeStatHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeItemInfo;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;
import org.l2jmobius.gameserver.network.serverpackets.RecipeItemMakeInfo;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RecipeManager {
   protected static final Map<Integer, RecipeManager.RecipeItemMaker> _activeMakers = new ConcurrentHashMap<>();

   protected RecipeManager() {
   }

   public void requestBookOpen(Player player, boolean isDwarvenCraft) {
      if (!_activeMakers.containsKey(player.getObjectId())) {
         RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
         response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
         player.sendPacket(response);
      } else {
         player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
      }
   }

   public void requestMakeItemAbort(Player player) {
      _activeMakers.remove(player.getObjectId());
   }

   public void requestManufactureItem(Player manufacturer, int recipeListId, Player player) {
      RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
      if (recipeList != null) {
         if (!manufacturer.getDwarvenRecipeBook().contains(recipeList) && !manufacturer.getCommonRecipeBook().contains(recipeList)) {
            PunishmentManager.handleIllegalPlayerAction(
               player,
               "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.",
               GeneralConfig.DEFAULT_PUNISH
            );
         } else if (PlayerConfig.ALT_GAME_CREATION && _activeMakers.containsKey(manufacturer.getObjectId())) {
            player.sendPacket(SystemMessageId.PLEASE_CLOSE_THE_THE_SETUP_WINDOW_FOR_YOUR_PRIVATE_MANUFACTURING_STORE_OR_PRIVATE_STORE_AND_TRY_AGAIN);
         } else {
            RecipeManager.RecipeItemMaker maker = new RecipeManager.RecipeItemMaker(manufacturer, recipeList, player);
            if (maker._isValid) {
               if (PlayerConfig.ALT_GAME_CREATION) {
                  _activeMakers.put(manufacturer.getObjectId(), maker);
                  ThreadPool.schedule(maker, 100L);
               } else {
                  maker.run();
               }
            }
         }
      }
   }

   public void requestMakeItem(Player player, int recipeListId) {
      if (!player.isInCombat() && !player.isInDuel()) {
         RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
         if (recipeList != null) {
            if (!player.getDwarvenRecipeBook().contains(recipeList) && !player.getCommonRecipeBook().contains(recipeList)) {
               PunishmentManager.handleIllegalPlayerAction(
                  player,
                  "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.",
                  GeneralConfig.DEFAULT_PUNISH
               );
            } else if (PlayerConfig.ALT_GAME_CREATION && _activeMakers.containsKey(player.getObjectId())) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1);
               sm.addItemName(recipeList.getItemId());
               sm.addString("You are busy creating.");
               player.sendPacket(sm);
            } else {
               RecipeManager.RecipeItemMaker maker = new RecipeManager.RecipeItemMaker(player, recipeList, player);
               if (maker._isValid) {
                  if (PlayerConfig.ALT_GAME_CREATION) {
                     _activeMakers.put(player.getObjectId(), maker);
                     ThreadPool.schedule(maker, 100L);
                  } else {
                     maker.run();
                  }
               }
            }
         }
      } else {
         player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
      }
   }

   public static RecipeManager getInstance() {
      return RecipeManager.SingletonHolder.INSTANCE;
   }

   private static class RecipeItemMaker implements Runnable {
      private static final Logger LOGGER = Logger.getLogger(RecipeManager.RecipeItemMaker.class.getName());
      protected boolean _isValid;
      protected List<RecipeItemInfo> _items = null;
      protected final RecipeList _recipeList;
      protected final Player _player;
      protected final Player _target;
      protected final Skill _skill;
      protected final int _skillId;
      protected final int _skillLevel;
      protected int _creationPasses = 1;
      protected int _itemGrab;
      protected int _exp = -1;
      protected int _sp = -1;
      protected long _price;
      protected int _totalItems;
      protected int _delay;

      public RecipeItemMaker(Player pPlayer, RecipeList pRecipeList, Player pTarget) {
         this._player = pPlayer;
         this._target = pTarget;
         this._recipeList = pRecipeList;
         this._isValid = false;
         this._skillId = this._recipeList.isDwarvenRecipe() ? CommonSkill.CREATE_DWARVEN.getId() : CommonSkill.CREATE_COMMON.getId();
         this._skillLevel = this._player.getSkillLevel(this._skillId);
         this._skill = this._player.getKnownSkill(this._skillId);
         this._player.setCrafting(true);
         if (this._player.isAlikeDead()) {
            this._player.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else if (this._target.isAlikeDead()) {
            this._target.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else if (this._target.isProcessingTransaction()) {
            this._target.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else if (this._player.isProcessingTransaction()) {
            this._player.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else if (this._recipeList.getRecipes().length == 0) {
            this._player.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else if (this._recipeList.getLevel() > this._skillLevel) {
            this._player.sendPacket(ActionFailed.STATIC_PACKET);
            this.abort();
         } else {
            if (this._player != this._target) {
               ManufactureItem item = this._player.getManufactureItems().get(this._recipeList.getId());
               if (item != null) {
                  this._price = item.getCost();
                  if (this._target.getAdena() < this._price) {
                     this._target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                     this.abort();
                     return;
                  }
               }
            }

            this._items = this.listItems(false);
            if (this._items == null) {
               this.abort();
            } else {
               for (RecipeItemInfo i : this._items) {
                  this._totalItems = this._totalItems + i.getQuantity();
               }

               if (!this.calculateStatUse(false, false)) {
                  this.abort();
               } else {
                  if (PlayerConfig.ALT_GAME_CREATION) {
                     this.calculateAltStatChange();
                  }

                  this.updateMakeInfo(true);
                  this.updateCurMp();
                  this.updateCurLoad();
                  this._player.setCrafting(false);
                  this._isValid = true;
               }
            }
         }
      }

      @Override
      public void run() {
         if (!PlayerConfig.IS_CRAFTING_ENABLED) {
            this._target.sendMessage("Item creation is currently disabled.");
            this.abort();
         } else if (this._player != null && this._target != null) {
            if (PlayerConfig.ALT_GAME_CREATION && !RecipeManager._activeMakers.containsKey(this._player.getObjectId())) {
               if (this._target != this._player) {
                  this._target.sendMessage("Manufacture aborted");
                  this._player.sendMessage("Manufacture aborted");
               } else {
                  this._player.sendMessage("Item creation aborted");
               }

               this.abort();
            } else {
               if (PlayerConfig.ALT_GAME_CREATION && !this._items.isEmpty()) {
                  if (!this.calculateStatUse(true, true)) {
                     return;
                  }

                  this.updateCurMp();
                  this.grabSomeItems();
                  if (!this._items.isEmpty()) {
                     this._delay = (int)(PlayerConfig.ALT_GAME_CREATION_SPEED * this._player.getMReuseRate(this._skill) * 10.0 * 100.0);
                     this._player.broadcastSkillPacket(new MagicSkillUse(this._player, this._skillId, this._skillLevel, this._delay, 0), this._player);
                     this._player.sendPacket(new SetupGauge(this._player.getObjectId(), 0, this._delay));
                     ThreadPool.schedule(this, 100 + this._delay);
                  } else {
                     this._player.sendPacket(new SetupGauge(this._player.getObjectId(), 0, this._delay));

                     try {
                        Thread.sleep(this._delay);
                     } catch (Exception var5) {
                     } finally {
                        this.finishCrafting();
                     }
                  }
               } else {
                  this.finishCrafting();
               }
            }
         } else {
            LOGGER.warning("player or target == null (disconnected?), aborting" + this._target + this._player);
            this.abort();
         }
      }

      private void finishCrafting() {
         if (!PlayerConfig.ALT_GAME_CREATION) {
            this.calculateStatUse(false, true);
         }

         if (this._target != this._player
            && this._price > 0L
            && this._target
                  .transferItem(
                     ItemProcessType.TRANSFER,
                     this._target.getInventory().getAdenaInstance().getObjectId(),
                     this._price,
                     this._player.getInventory(),
                     this._player
                  )
               == null) {
            this._target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            this.abort();
         } else {
            this._items = this.listItems(true);
            if (this._items != null) {
               if (Rnd.get(100) < this._recipeList.getSuccessRate()) {
                  this.rewardPlayer();
                  this.updateMakeInfo(true);
               } else {
                  if (this._target != this._player) {
                     SystemMessage msg = new SystemMessage(SystemMessageId.THE_ATTEMPT_TO_CREATE_S2_FOR_C1_AT_THE_PRICE_OF_S3_ADENA_HAS_FAILED);
                     msg.addString(this._target.getName());
                     msg.addItemName(this._recipeList.getItemId());
                     msg.addLong(this._price);
                     this._player.sendPacket(msg);
                     msg = new SystemMessage(SystemMessageId.C1_HAS_FAILED_TO_CREATE_S2_AT_THE_PRICE_OF_S3_ADENA);
                     msg.addString(this._player.getName());
                     msg.addItemName(this._recipeList.getItemId());
                     msg.addLong(this._price);
                     this._target.sendPacket(msg);
                  } else {
                     this._target.sendPacket(SystemMessageId.YOU_FAILED_AT_MIXING_THE_ITEM);
                  }

                  this.updateMakeInfo(false);
               }
            }

            this.updateCurMp();
            this.updateCurLoad();
            RecipeManager._activeMakers.remove(this._player.getObjectId());
            this._player.setCrafting(false);
            this._target.sendItemList(false);
         }
      }

      private void updateMakeInfo(boolean success) {
         if (this._target == this._player) {
            this._target.sendPacket(new RecipeItemMakeInfo(this._recipeList.getId(), this._target, success));
         } else {
            this._target.sendPacket(new RecipeShopItemInfo(this._player, this._recipeList.getId()));
         }
      }

      private void updateCurLoad() {
         StatusUpdate su = new StatusUpdate(this._target);
         su.addAttribute(14, this._target.getCurrentLoad());
         this._target.sendPacket(su);
      }

      private void updateCurMp() {
         StatusUpdate su = new StatusUpdate(this._target);
         su.addAttribute(11, (int)this._target.getCurrentMp());
         this._target.sendPacket(su);
      }

      private void grabSomeItems() {
         int grabItems = this._itemGrab;

         while (grabItems > 0 && !this._items.isEmpty()) {
            RecipeItemInfo item = this._items.get(0);
            int count = item.getQuantity() >= grabItems ? grabItems : item.getQuantity();
            item.setQuantity(item.getQuantity() - count);
            if (item.getQuantity() <= 0) {
               this._items.remove(0);
            } else {
               this._items.set(0, item);
            }

            grabItems -= count;
            if (this._target == this._player) {
               SystemMessage sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
               sm.addLong(count);
               sm.addItemName(item.getItemId());
               this._player.sendPacket(sm);
            } else {
               this._target.sendMessage("Manufacturer " + this._player.getName() + " used " + count + " " + item.getItemName());
            }
         }
      }

      private void calculateAltStatChange() {
         this._itemGrab = this._skillLevel;

         for (RecipeStatHolder altStatChange : this._recipeList.getAltStatChange()) {
            if (altStatChange.getType() == StatType.XP) {
               this._exp = altStatChange.getValue();
            } else if (altStatChange.getType() == StatType.SP) {
               this._sp = altStatChange.getValue();
            } else if (altStatChange.getType() == StatType.GIM) {
               this._itemGrab = this._itemGrab * altStatChange.getValue();
            }
         }

         this._creationPasses = this._totalItems / this._itemGrab + (this._totalItems % this._itemGrab != 0 ? 1 : 0);
         if (this._creationPasses < 1) {
            this._creationPasses = 1;
         }
      }

      private boolean calculateStatUse(boolean isWait, boolean isReduce) {
         boolean ret = true;

         for (RecipeStatHolder statUse : this._recipeList.getStatUse()) {
            double modifiedValue = statUse.getValue() / this._creationPasses;
            if (statUse.getType() == StatType.HP) {
               if (this._player.getCurrentHp() <= modifiedValue) {
                  if (PlayerConfig.ALT_GAME_CREATION && isWait) {
                     this._player.sendPacket(new SetupGauge(this._player.getObjectId(), 0, this._delay));
                     ThreadPool.schedule(this, 100 + this._delay);
                  } else {
                     this._target.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
                     this.abort();
                  }

                  ret = false;
               } else if (isReduce) {
                  this._player.reduceCurrentHp(modifiedValue, this._player, this._skill);
               }
            } else if (statUse.getType() == StatType.MP) {
               if (this._player.getCurrentMp() < modifiedValue) {
                  if (PlayerConfig.ALT_GAME_CREATION && isWait) {
                     this._player.sendPacket(new SetupGauge(this._player.getObjectId(), 0, this._delay));
                     ThreadPool.schedule(this, 100 + this._delay);
                  } else {
                     this._target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
                     this.abort();
                  }

                  ret = false;
               } else if (isReduce) {
                  this._player.reduceCurrentMp(modifiedValue);
               }
            } else {
               this._target.sendMessage("Recipe error!!!, please tell this to your GM.");
               ret = false;
               this.abort();
            }
         }

         return ret;
      }

      private List<RecipeItemInfo> listItems(boolean remove) {
         RecipeHolder[] recipes = this._recipeList.getRecipes();
         Inventory inv = this._target.getInventory();
         List<RecipeItemInfo> materials = new ArrayList<>();

         for (RecipeHolder recipe : recipes) {
            if (recipe.getQuantity() > 0) {
               Item item = inv.getItemByItemId(recipe.getItemId());
               long itemQuantityAmount = item == null ? 0L : item.getCount();
               if (itemQuantityAmount < recipe.getQuantity()) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.YOU_ARE_MISSING_S2_S1_REQUIRED_TO_CREATE_THAT);
                  sm.addItemName(recipe.getItemId());
                  sm.addLong(recipe.getQuantity() - itemQuantityAmount);
                  this._target.sendPacket(sm);
                  this.abort();
                  return null;
               }

               materials.add(new RecipeItemInfo(item, recipe.getQuantity()));
            }
         }

         if (remove) {
            for (RecipeItemInfo tmp : materials) {
               inv.destroyItemByItemId(ItemProcessType.FEE, tmp.getItemId(), tmp.getQuantity(), this._target, this._player);
               SystemMessage sm;
               if (tmp.getQuantity() > 1) {
                  sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                  sm.addItemName(tmp.getItemId());
                  sm.addLong(tmp.getQuantity());
               } else {
                  sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                  sm.addItemName(tmp.getItemId());
               }

               this._target.sendPacket(sm);
            }
         }

         return materials;
      }

      private void abort() {
         this.updateMakeInfo(false);
         this._player.setCrafting(false);
         RecipeManager._activeMakers.remove(this._player.getObjectId());
      }

      private void rewardPlayer() {
         int rareProdId = this._recipeList.getRareItemId();
         int itemId = this._recipeList.getItemId();
         int itemCount = this._recipeList.getCount();
         ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
         if (rareProdId != -1
            && (rareProdId == itemId || PlayerConfig.CRAFT_MASTERWORK)
            && Rnd.get(100) < this._recipeList.getRarity() * PlayerConfig.CRAFT_MASTERWORK_CHANCE_RATE) {
            itemId = rareProdId;
            itemCount = this._recipeList.getRareCount();
         }

         this._target.getInventory().addItem(ItemProcessType.CRAFT, itemId, itemCount, this._target, this._player);
         SystemMessage sm = null;
         if (this._target != this._player) {
            if (itemCount == 1) {
               sm = new SystemMessage(SystemMessageId.S2_HAS_BEEN_CREATED_FOR_C1_AFTER_THE_PAYMENT_OF_S3_ADENA_IS_RECEIVED);
               sm.addString(this._target.getName());
               sm.addItemName(itemId);
               sm.addLong(this._price);
               this._player.sendPacket(sm);
               sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_AFTER_RECEIVING_S3_ADENA);
               sm.addString(this._player.getName());
               sm.addItemName(itemId);
               sm.addLong(this._price);
               this._target.sendPacket(sm);
            } else {
               sm = new SystemMessage(SystemMessageId.S2_S3_HAVE_BEEN_CREATED_FOR_C1_AT_THE_PRICE_OF_S4_ADENA);
               sm.addString(this._target.getName());
               sm.addInt(itemCount);
               sm.addItemName(itemId);
               sm.addLong(this._price);
               this._player.sendPacket(sm);
               sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_S3_AT_THE_PRICE_OF_S4_ADENA);
               sm.addString(this._player.getName());
               sm.addInt(itemCount);
               sm.addItemName(itemId);
               sm.addLong(this._price);
               this._target.sendPacket(sm);
            }
         }

         if (itemCount > 1) {
            sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
            sm.addItemName(itemId);
            sm.addLong(itemCount);
         } else {
            sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
            sm.addItemName(itemId);
         }

         this._target.sendPacket(sm);
         if (PlayerConfig.ALT_GAME_CREATION) {
            int recipeLevel = this._recipeList.getLevel();
            if (this._exp < 0) {
               this._exp = template.getReferencePrice() * itemCount;
               this._exp /= recipeLevel;
            }

            if (this._sp < 0) {
               this._sp = this._exp / 10;
            }

            if (itemId == rareProdId) {
               this._exp = (int)(this._exp * PlayerConfig.ALT_GAME_CREATION_RARE_XPSP_RATE);
               this._sp = (int)(this._sp * PlayerConfig.ALT_GAME_CREATION_RARE_XPSP_RATE);
            }

            if (this._exp < 0) {
               this._exp = 0;
            }

            if (this._sp < 0) {
               this._sp = 0;
            }

            for (int i = this._skillLevel; i > recipeLevel; i--) {
               this._exp /= 4;
               this._sp /= 4;
            }

            this._player
               .addExpAndSp(
                  (int)this._player
                     .calcStat(Stat.EXPSP_RATE, this._exp * PlayerConfig.ALT_GAME_CREATION_XP_RATE * PlayerConfig.ALT_GAME_CREATION_SPEED, null, null),
                  (int)this._player
                     .calcStat(Stat.EXPSP_RATE, this._sp * PlayerConfig.ALT_GAME_CREATION_SP_RATE * PlayerConfig.ALT_GAME_CREATION_SPEED, null, null)
               );
         }

         this.updateMakeInfo(true);
      }
   }

   private static class SingletonHolder {
      protected static final RecipeManager INSTANCE = new RecipeManager();
   }
}
