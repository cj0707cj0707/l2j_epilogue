package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;

public class CharSummonTable {
   private static final Logger LOGGER = Logger.getLogger(CharSummonTable.class.getName());
   private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
   private static final Map<Integer, Integer> _servitors = new ConcurrentHashMap<>();
   private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'";
   private static final String INIT_SUMMONS = "SELECT ownerId, summonSkillId FROM character_summons";
   private static final String LOAD_SUMMON = "SELECT curHp, curMp, time FROM character_summons WHERE ownerId = ? AND summonSkillId = ?";
   private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ?";
   private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?)";

   public Map<Integer, Integer> getPets() {
      return _pets;
   }

   public Map<Integer, Integer> getServitors() {
      return _servitors;
   }

   public void init() {
      if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT) {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT ownerId, summonSkillId FROM character_summons");
         ) {
            while (rs.next()) {
               _servitors.put(rs.getInt("ownerId"), rs.getInt("summonSkillId"));
            }
         } catch (Exception var19) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Error while loading saved servitor: " + var19);
         }
      }

      if (PlayerConfig.RESTORE_PET_ON_RECONNECT) {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'");
         ) {
            while (rs.next()) {
               _pets.put(rs.getInt("ownerId"), rs.getInt("item_obj_id"));
            }
         } catch (Exception var15) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Error while loading saved pet: " + var15);
         }
      }
   }

   public void removeServitor(Player player) {
      _servitors.remove(player.getObjectId());

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM character_summons WHERE ownerId = ?");
      ) {
         ps.setInt(1, player.getObjectId());
         ps.execute();
      } catch (SQLException var10) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Summon cannot be removed: " + var10);
      }
   }

   public void restorePet(Player player) {
      Item item = player.getInventory().getItemByObjectId(_pets.get(player.getObjectId()));
      if (item == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Null pet summoning item for: " + player);
      } else {
         PetData petData = PetDataTable.getInstance().getPetDataByItemId(item.getId());
         if (petData == null) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Null pet data for: " + player + " and summoning item: " + item);
         } else {
            NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(petData.getNpcId());
            if (npcTemplate == null) {
               LOGGER.warning(this.getClass().getSimpleName() + ": Null pet NPC template for: " + player + " and pet Id:" + petData.getNpcId());
            } else {
               Pet pet = Pet.spawnPet(npcTemplate, player, item);
               if (pet == null) {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Null pet instance for: " + player + " and pet NPC template:" + npcTemplate);
               } else {
                  pet.setShowSummonAnimation(true);
                  pet.setTitle(player.getName());
                  if (!pet.isRespawned()) {
                     pet.setCurrentHp(pet.getMaxHp());
                     pet.setCurrentMp(pet.getMaxMp());
                     pet.getStat().setExp(pet.getExpForThisLevel());
                     pet.setCurrentFed(pet.getMaxFed());
                  }

                  pet.setRunning();
                  if (!pet.isRespawned()) {
                     pet.storeMe();
                  }

                  item.setEnchantLevel(pet.getLevel());
                  player.setPet(pet);
                  pet.spawnMe(player.getX() + 50, player.getY() + 100, player.getZ());
                  pet.startFeed();
                  pet.setFollowStatus(true);
                  pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
                  pet.broadcastStatusUpdate();
               }
            }
         }
      }
   }

   public void restoreServitor(Player player) {
      int skillId = _servitors.get(player.getObjectId());

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT curHp, curMp, time FROM character_summons WHERE ownerId = ? AND summonSkillId = ?");
      ) {
         ps.setInt(1, player.getObjectId());
         ps.setInt(2, skillId);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Skill skill = SkillData.getInstance().getSkill(skillId, player.getSkillLevel(skillId));
               if (skill == null) {
                  this.removeServitor(player);
                  return;
               }

               skill.applyEffects(player, player);
               if (player.hasServitor()) {
                  int curHp = rs.getInt("curHp");
                  int curMp = rs.getInt("curMp");
                  int time = rs.getInt("time");
                  Servitor summon = player.getSummon().asServitor();
                  summon.setCurrentHp(curHp);
                  summon.setCurrentMp(curMp);
                  summon.setLifeTimeRemaining(time);
               }
            }
         }
      } catch (SQLException var17) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Servitor cannot be restored: " + var17);
      }
   }

   public void saveSummon(Servitor summon) {
      if (summon != null && summon.getLifeTimeRemaining() > 0) {
         _servitors.put(summon.getOwner().getObjectId(), summon.getReferenceSkill());

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO character_summons (ownerId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?)");
         ) {
            ps.setInt(1, summon.getOwner().getObjectId());
            ps.setInt(2, summon.getReferenceSkill());
            ps.setInt(3, (int)Math.round(summon.getCurrentHp()));
            ps.setInt(4, (int)Math.round(summon.getCurrentMp()));
            ps.setInt(5, summon.getLifeTimeRemaining());
            ps.execute();
         } catch (Exception var10) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Failed to store summon: " + summon + " from " + summon.getOwner() + ", error: " + var10);
         }
      }
   }

   public static CharSummonTable getInstance() {
      return CharSummonTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CharSummonTable INSTANCE = new CharSummonTable();
   }
}
