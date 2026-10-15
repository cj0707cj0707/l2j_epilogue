package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.ItemHandler;

public class AutoPotionTaskManager implements Runnable {
   private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
   private static boolean _working = false;

   protected AutoPotionTaskManager() {
      ThreadPool.schedulePriorityTaskAtFixedRate(this, 0L, 1000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PLAYERS.isEmpty()) {
            for (Player player : PLAYERS) {
               if (player != null
                  && !player.isAlikeDead()
                  && player.isOnlineInt() != 0
                  && (AutoPotionsConfig.AUTO_POTIONS_IN_OLYMPIAD || !player.isInOlympiadMode())) {
                  boolean success = false;
                  if (AutoPotionsConfig.AUTO_HP_ENABLED) {
                     boolean restoreHP = player.getStatus().getCurrentHp() / player.getMaxHp() * 100.0 < AutoPotionsConfig.AUTO_HP_PERCENTAGE;

                     for (int itemId : AutoPotionsConfig.AUTO_HP_ITEM_IDS) {
                        Item hpPotion = player.getInventory().getItemByItemId(itemId);
                        if (hpPotion != null && hpPotion.getCount() > 0L) {
                           success = true;
                           if (restoreHP) {
                              ItemHandler.getInstance().getHandler(hpPotion.getEtcItem()).onItemUse(player, hpPotion, false);
                              player.sendMessage("Auto potion: Restored HP.");
                              break;
                           }
                        }
                     }
                  }

                  if (AutoPotionsConfig.AUTO_CP_ENABLED) {
                     boolean restoreCP = player.getStatus().getCurrentCp() / player.getMaxCp() * 100.0 < AutoPotionsConfig.AUTO_CP_PERCENTAGE;

                     for (int itemIdx : AutoPotionsConfig.AUTO_CP_ITEM_IDS) {
                        Item cpPotion = player.getInventory().getItemByItemId(itemIdx);
                        if (cpPotion != null && cpPotion.getCount() > 0L) {
                           success = true;
                           if (restoreCP) {
                              ItemHandler.getInstance().getHandler(cpPotion.getEtcItem()).onItemUse(player, cpPotion, false);
                              player.sendMessage("Auto potion: Restored CP.");
                              break;
                           }
                        }
                     }
                  }

                  if (AutoPotionsConfig.AUTO_MP_ENABLED) {
                     boolean restoreMP = player.getStatus().getCurrentMp() / player.getMaxMp() * 100.0 < AutoPotionsConfig.AUTO_MP_PERCENTAGE;

                     for (int itemIdxx : AutoPotionsConfig.AUTO_MP_ITEM_IDS) {
                        Item mpPotion = player.getInventory().getItemByItemId(itemIdxx);
                        if (mpPotion != null && mpPotion.getCount() > 0L) {
                           success = true;
                           if (restoreMP) {
                              ItemHandler.getInstance().getHandler(mpPotion.getEtcItem()).onItemUse(player, mpPotion, false);
                              player.sendMessage("Auto potion: Restored MP.");
                              break;
                           }
                        }
                     }
                  }

                  if (!success) {
                     player.sendMessage("Auto potion: You are out of potions!");
                  }
               } else {
                  this.remove(player);
               }
            }
         }

         _working = false;
      }
   }

   public void add(Player player) {
      if (!PLAYERS.contains(player)) {
         PLAYERS.add(player);
      }
   }

   public void remove(Player player) {
      PLAYERS.remove(player);
   }

   public boolean hasPlayer(Player player) {
      return PLAYERS.contains(player);
   }

   public static AutoPotionTaskManager getInstance() {
      return AutoPotionTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final AutoPotionTaskManager INSTANCE = new AutoPotionTaskManager();
   }
}
