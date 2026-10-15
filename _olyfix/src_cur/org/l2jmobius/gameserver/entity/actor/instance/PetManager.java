package org.l2jmobius.gameserver.entity.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PetManager extends Merchant {
   public PetManager(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.PetManager);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/petmanager/" + pom + ".htm";
   }

   @Override
   public void showChatWindow(Player player) {
      String filename = "data/html/petmanager/" + this.getId() + ".htm";
      if (this.getId() == 36478 && player.hasSummon()) {
         filename = "data/html/petmanager/restore-unsummonpet.htm";
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      if (GeneralConfig.ALLOW_RENTPET && NpcConfig.LIST_PET_RENT_NPC.contains(this.getId())) {
         html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h Script");
      }

      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcname%", this.getName());
      player.sendPacket(html);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("exchange")) {
         String[] params = command.split(" ");
         int val = Integer.parseInt(params[1]);
         switch (val) {
            case 1:
               this.exchange(player, 7585, 6650);
               break;
            case 2:
               this.exchange(player, 7583, 6648);
               break;
            case 3:
               this.exchange(player, 7584, 6649);
         }
      } else if (command.startsWith("evolve")) {
         String[] params = command.split(" ");
         int val = Integer.parseInt(params[1]);
         boolean ok = false;
         switch (val) {
            case 1:
               ok = doEvolve(player, this, 2375, 9882, 55);
               break;
            case 2:
               ok = doEvolve(player, this, 9882, 10426, 70);
               break;
            case 3:
               ok = doEvolve(player, this, 6648, 10311, 55);
               break;
            case 4:
               ok = doEvolve(player, this, 6650, 10313, 55);
               break;
            case 5:
               ok = doEvolve(player, this, 6649, 10312, 55);
         }

         if (!ok) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, "data/html/petmanager/evolve_no.htm");
            player.sendPacket(html);
         }
      } else if (command.startsWith("restore")) {
         String[] params = command.split(" ");
         int val = Integer.parseInt(params[1]);
         boolean ok = false;
         switch (val) {
            case 1:
               ok = doRestore(player, this, 10307, 9882, 55);
               break;
            case 2:
               ok = doRestore(player, this, 10611, 10426, 70);
               break;
            case 3:
               ok = doRestore(player, this, 10308, 4422, 55);
               break;
            case 4:
               ok = doRestore(player, this, 10309, 4423, 55);
               break;
            case 5:
               ok = doRestore(player, this, 10310, 4424, 55);
         }

         if (!ok) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, "data/html/petmanager/restore_no.htm");
            player.sendPacket(html);
         }
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   public void exchange(Player player, int itemIdtake, int itemIdgive) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      if (player.destroyItemByItemId(null, itemIdtake, 1L, this, true)) {
         player.addItem(ItemProcessType.NONE, itemIdgive, 1L, this, true);
         html.setFile(player, "data/html/petmanager/" + this.getId() + ".htm");
         player.sendPacket(html);
      } else {
         html.setFile(player, "data/html/petmanager/exchange_no.htm");
         player.sendPacket(html);
      }
   }

   public static boolean doEvolve(Player player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel) {
      if (itemIdtake == 0 || itemIdgive == 0 || petminLevel == 0) {
         return false;
      } else if (!player.hasPet()) {
         return false;
      } else {
         Pet currentPet = player.getSummon().asPet();
         if (currentPet.isAlikeDead()) {
            PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use death pet exploit!", GeneralConfig.DEFAULT_PUNISH);
            return false;
         } else {
            Item item = null;
            PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
            if (oldData == null) {
               return false;
            } else {
               int oldnpcID = oldData.getNpcId();
               if (currentPet.getStat().getLevel() >= petminLevel && currentPet.getId() == oldnpcID) {
                  PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
                  if (petData == null) {
                     return false;
                  } else {
                     int npcID = petData.getNpcId();
                     if (npcID == 0) {
                        return false;
                     } else {
                        long petexp = currentPet.getStat().getExp();
                        String oldname = currentPet.getName();
                        int oldX = currentPet.getX();
                        int oldY = currentPet.getY();
                        int oldZ = currentPet.getZ();
                        NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcID);
                        currentPet.unSummon(player);
                        currentPet.destroyControlItem(player, true);
                        item = player.getInventory().addItem(ItemProcessType.RESTORE, itemIdgive, 1L, player, npc);
                        Pet petSummon = Pet.spawnPet(npcTemplate, player, item);
                        if (petSummon == null) {
                           return false;
                        } else {
                           long _minimumexp = petSummon.getStat().getExpForLevel(petminLevel);
                           if (petexp < _minimumexp) {
                              petexp = _minimumexp;
                           }

                           petSummon.getStat().addExp(petexp);
                           petSummon.setCurrentHp(petSummon.getMaxHp());
                           petSummon.setCurrentMp(petSummon.getMaxMp());
                           petSummon.setCurrentFed(petSummon.getMaxFed());
                           petSummon.setTitle(player.getName());
                           petSummon.setName(oldname);
                           petSummon.setRunning();
                           petSummon.storeMe();
                           player.setPet(petSummon);
                           player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
                           player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
                           petSummon.spawnMe(oldX, oldY, oldZ);
                           petSummon.startFeed();
                           item.setEnchantLevel(petSummon.getLevel());
                           ThreadPool.schedule(new PetManager.EvolveFinalizer(player, petSummon), 900L);
                           if (petSummon.getCurrentFed() <= 0) {
                              ThreadPool.schedule(new PetManager.EvolveFeedWait(player, petSummon), 60000L);
                           } else {
                              petSummon.startFeed();
                           }

                           return true;
                        }
                     }
                  }
               } else {
                  return false;
               }
            }
         }
      }
   }

   public static boolean doRestore(Player player, Npc npc, int itemIdtake, int itemIdgive, int petminLevel) {
      if (itemIdtake != 0 && itemIdgive != 0 && petminLevel != 0) {
         Item item = player.getInventory().getItemByItemId(itemIdtake);
         if (item == null) {
            return false;
         } else {
            PetData oldData = PetDataTable.getInstance().getPetDataByItemId(itemIdtake);
            if (oldData == null) {
               return false;
            } else {
               PetData petData = PetDataTable.getInstance().getPetDataByItemId(itemIdgive);
               if (petData == null) {
                  return false;
               } else {
                  int npcId = petData.getNpcId();
                  if (npcId == 0) {
                     return false;
                  } else {
                     int oldpetlvl = item.getEnchantLevel() < petminLevel ? petminLevel : item.getEnchantLevel();
                     NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
                     Item removedItem = player.getInventory().destroyItem(ItemProcessType.RESTORE, item, player, npc);
                     SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
                     sm.addItemName(removedItem);
                     player.sendPacket(sm);
                     Item addedItem = player.getInventory().addItem(ItemProcessType.RESTORE, itemIdgive, 1L, player, npc);
                     Pet petSummon = Pet.spawnPet(npcTemplate, player, addedItem);
                     if (petSummon == null) {
                        return false;
                     } else {
                        long _maxexp = petSummon.getStat().getExpForLevel(oldpetlvl);
                        petSummon.getStat().addExp(_maxexp);
                        petSummon.setCurrentHp(petSummon.getMaxHp());
                        petSummon.setCurrentMp(petSummon.getMaxMp());
                        petSummon.setCurrentFed(petSummon.getMaxFed());
                        petSummon.setTitle(player.getName());
                        petSummon.setRunning();
                        petSummon.storeMe();
                        player.setPet(petSummon);
                        player.sendPacket(new MagicSkillUse(npc, 2046, 1, 1000, 600000));
                        player.sendPacket(SystemMessageId.SUMMONING_YOUR_PET);
                        petSummon.spawnMe(player.getX(), player.getY(), player.getZ());
                        petSummon.startFeed();
                        addedItem.setEnchantLevel(petSummon.getLevel());
                        InventoryUpdate iu = new InventoryUpdate();
                        iu.addRemovedItem(removedItem);
                        player.sendInventoryUpdate(iu);
                        StatusUpdate su = new StatusUpdate(player);
                        su.addAttribute(14, player.getCurrentLoad());
                        player.sendPacket(su);
                        player.broadcastUserInfo();
                        World.removeObject(removedItem);
                        ThreadPool.schedule(new PetManager.EvolveFinalizer(player, petSummon), 900L);
                        if (petSummon.getCurrentFed() <= 0) {
                           ThreadPool.schedule(new PetManager.EvolveFeedWait(player, petSummon), 60000L);
                        } else {
                           petSummon.startFeed();
                        }

                        try (
                           Connection con = DatabaseFactory.getConnection();
                           PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
                        ) {
                           ps.setInt(1, removedItem.getObjectId());
                           ps.execute();
                        } catch (Exception var27) {
                        }

                        return true;
                     }
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   public static class EvolveFeedWait implements Runnable {
      private final Player _player;
      private final Pet _petSummon;

      EvolveFeedWait(Player player, Pet petSummon) {
         this._player = player;
         this._petSummon = petSummon;
      }

      @Override
      public void run() {
         try {
            if (this._petSummon.getCurrentFed() <= 0) {
               this._petSummon.unSummon(this._player);
            } else {
               this._petSummon.startFeed();
            }
         } catch (Exception var2) {
            Creature.LOGGER.log(Level.WARNING, "run: ", (Throwable)var2);
         }
      }
   }

   public static class EvolveFinalizer implements Runnable {
      private final Player _player;
      private final Pet _petSummon;

      EvolveFinalizer(Player player, Pet petSummon) {
         this._player = player;
         this._petSummon = petSummon;
      }

      @Override
      public void run() {
         try {
            this._player.sendPacket(new MagicSkillLaunched(this._player, 2046, 1));
            this._petSummon.setFollowStatus(true);
            this._petSummon.setShowSummonAnimation(false);
         } catch (Throwable var2) {
            Creature.LOGGER.log(Level.WARNING, "run: ", var2);
         }
      }
   }
}
