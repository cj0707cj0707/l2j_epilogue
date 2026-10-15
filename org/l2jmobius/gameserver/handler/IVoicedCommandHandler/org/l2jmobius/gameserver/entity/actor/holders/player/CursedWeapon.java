package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TransformData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExRedSky;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class CursedWeapon {
   private static final Logger LOGGER = Logger.getLogger(CursedWeapon.class.getName());
   private final String _name;
   private final int _itemId;
   private final int _skillId;
   private final int _skillMaxLevel;
   private int _dropRate;
   private int _duration;
   private int _durationLost;
   private int _disapearChance;
   private int _stageKills;
   private boolean _isDropped = false;
   private boolean _isActivated = false;
   private ScheduledFuture<?> _removeTask;
   private int _nbKills = 0;
   long _endTime = 0L;
   private int _playerId = 0;
   protected Player _player = null;
   private Item _item = null;
   private int _playerKarma = 0;
   private int _playerPkKills = 0;
   protected int transformationId = 0;

   public CursedWeapon(int itemId, int skillId, String name) {
      this._name = name;
      this._itemId = itemId;
      this._skillId = skillId;
      this._skillMaxLevel = SkillData.getInstance().getMaxLevel(this._skillId);
   }

   public void endOfLife() {
      if (this._isActivated) {
         if (this._player != null && this._player.isOnline()) {
            LOGGER.info(this._name + " being removed online.");
            this._player.abortAttack();
            this._player.setKarma(this._playerKarma);
            this._player.setPkKills(this._playerPkKills);
            this._player.setCursedWeaponEquippedId(0);
            this.removeSkill();
            this._player.getInventory().unEquipItemInBodySlot(BodyPart.LR_HAND);
            this._player.storeMe();
            this._player.getInventory().destroyItemByItemId(ItemProcessType.NONE, this._itemId, 1L, this._player, null);
            this._player.sendItemList(true);
            this._player.broadcastUserInfo();
         } else {
            LOGGER.info(this._name + " being removed offline.");

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement del = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
               PreparedStatement ps = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?");
            ) {
               del.setInt(1, this._playerId);
               del.setInt(2, this._itemId);
               if (del.executeUpdate() != 1) {
                  LOGGER.warning("Error while deleting itemId " + this._itemId + " from userId " + this._playerId);
               }

               ps.setInt(1, this._playerKarma);
               ps.setInt(2, this._playerPkKills);
               ps.setInt(3, this._playerId);
               if (ps.executeUpdate() != 1) {
                  LOGGER.warning("Error while updating karma & pkkills for userId " + this._playerId);
               }
            } catch (Exception var12) {
               LOGGER.log(Level.WARNING, "Could not delete : " + var12.getMessage(), (Throwable)var12);
            }
         }
      } else if (this._player != null && this._player.getInventory().getItemByItemId(this._itemId) != null) {
         this._player.getInventory().destroyItemByItemId(ItemProcessType.NONE, this._itemId, 1L, this._player, null);
         this._player.sendItemList(true);
         this._player.broadcastUserInfo();
      } else if (this._item != null) {
         this._item.decayMe();
         LOGGER.info(this._name + " item has been removed from World.");
      }

      CursedWeaponsManager.removeFromDb(this._itemId);
      SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
      sm.addItemName(this._itemId);
      CursedWeaponsManager.announce(sm);
      this.cancelTask();
      this._isActivated = false;
      this._isDropped = false;
      this._endTime = 0L;
      this._player = null;
      this._playerId = 0;
      this._playerKarma = 0;
      this._playerPkKills = 0;
      this._item = null;
      this._nbKills = 0;
   }

   private void cancelTask() {
      if (this._removeTask != null) {
         this._removeTask.cancel(true);
         this._removeTask = null;
      }
   }

   private void dropIt(Attackable attackable, Player player) {
      this.dropIt(attackable, player, null, true);
   }

   private void dropIt(Attackable attackable, Player player, Creature killer, boolean fromMonster) {
      this._isActivated = false;
      if (fromMonster) {
         this._item = attackable.dropItem(player, this._itemId, 1L);
         this._item.setDropTime(0L);
         ExRedSky rs = new ExRedSky(10);
         Earthquake eq = new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3);
         World.broadcastToAllOnlinePlayers(rs);
         World.broadcastToAllOnlinePlayers(eq);
      } else {
         this._item = this._player.getInventory().getItemByItemId(this._itemId);
         this._player.dropItem(ItemProcessType.DEATH, this._item, killer, true);
         this._player.setKarma(this._playerKarma);
         this._player.setPkKills(this._playerPkKills);
         this._player.setCursedWeaponEquippedId(0);
         this.removeSkill();
         this._player.abortAttack();
      }

      this._isDropped = true;
      SystemMessage sm = new SystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
      if (player != null) {
         sm.addZoneName(player.getX(), player.getY(), player.getZ());
      } else if (this._player != null) {
         sm.addZoneName(this._player.getX(), this._player.getY(), this._player.getZ());
      } else {
         sm.addZoneName(killer.getX(), killer.getY(), killer.getZ());
      }

      sm.addItemName(this._itemId);
      CursedWeaponsManager.announce(sm);
   }

   public void cursedOnLogin() {
      this.doTransform();
      this.giveSkill();
      SystemMessage msg = new SystemMessage(SystemMessageId.S2_S_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
      msg.addZoneName(this._player.getX(), this._player.getY(), this._player.getZ());
      msg.addItemName(this._player.getCursedWeaponEquippedId());
      CursedWeaponsManager.announce(msg);
      CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(this._player.getCursedWeaponEquippedId());
      SystemMessage msg2 = new SystemMessage(SystemMessageId.S1_HAS_S2_MINUTE_S_OF_USAGE_TIME_REMAINING);
      int timeLeft = (int)(cw.getTimeLeft() / 60000L);
      msg2.addItemName(this._player.getCursedWeaponEquippedId());
      msg2.addInt(timeLeft);
      this._player.sendPacket(msg2);
   }

   public void giveSkill() {
      int level = 1 + this._nbKills / this._stageKills;
      if (level > this._skillMaxLevel) {
         level = this._skillMaxLevel;
      }

      Skill skill = SkillData.getInstance().getSkill(this._skillId, level);
      this._player.addSkill(skill, false);
      this._player.addSkill(CommonSkill.VOID_BURST.getSkill(), false);
      this._player.addTransformSkill(CommonSkill.VOID_BURST.getSkill());
      this._player.addSkill(CommonSkill.VOID_FLOW.getSkill(), false);
      this._player.addTransformSkill(CommonSkill.VOID_FLOW.getSkill());
      this._player.sendSkillList();
   }

   public void doTransform() {
      if (this._itemId == 8689) {
         this.transformationId = 302;
      } else if (this._itemId == 8190) {
         this.transformationId = 301;
      }

      if (!this._player.isTransformed() && !this._player.isInStance()) {
         TransformData.getInstance().transformPlayer(this.transformationId, this._player);
      } else {
         this._player.stopTransformation(true);
         ThreadPool.schedule(() -> TransformData.getInstance().transformPlayer(this.transformationId, this._player), 500L);
      }
   }

   public void removeSkill() {
      this._player.removeSkill(this._skillId);
      this._player.removeSkill(CommonSkill.VOID_BURST.getSkill().getId());
      this._player.removeSkill(CommonSkill.VOID_FLOW.getSkill().getId());
      this._player.untransform();
      this._player.sendSkillList();
   }

   public void reActivate() {
      this._isActivated = true;
      if (this._endTime - System.currentTimeMillis() <= 0L) {
         this.endOfLife();
      } else {
         this._removeTask = ThreadPool.scheduleAtFixedRate(new CursedWeapon.RemoveTask(), this._durationLost * 12000, this._durationLost * 12000);
      }
   }

   public boolean checkDrop(Attackable attackable, Player player) {
      if (Rnd.get(100000) < this._dropRate) {
         this.dropIt(attackable, player);
         this._endTime = System.currentTimeMillis() + this._duration * 60000;
         this._removeTask = ThreadPool.scheduleAtFixedRate(new CursedWeapon.RemoveTask(), this._durationLost * 12000, this._durationLost * 12000);
         return true;
      } else {
         return false;
      }
   }

   public void activate(Player player, Item item) {
      if (player.isMounted() && !player.dismount()) {
         player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
         player.dropItem(ItemProcessType.DROP, item, null, true);
      } else {
         this._isActivated = true;
         this._player = player;
         this._playerId = this._player.getObjectId();
         this._playerKarma = this._player.getKarma();
         this._playerPkKills = this._player.getPkKills();
         this.saveData();
         this._player.setCursedWeaponEquippedId(this._itemId);
         this._player.setKarma(9999999);
         this._player.setPkKills(0);
         if (this._player.isInParty()) {
            this._player.getParty().removePartyMember(this._player, PartyMessageType.EXPELLED);
         }

         this.doTransform();
         this.giveSkill();
         this._item = item;
         this._player.getInventory().equipItem(this._item);
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
         sm.addItemName(this._item);
         this._player.sendPacket(sm);
         this._player.setCurrentHpMp(this._player.getMaxHp(), this._player.getMaxMp());
         this._player.setCurrentCp(this._player.getMaxCp());
         this._player.sendItemList(false);
         this._player.broadcastUserInfo();
         ThreadPool.schedule(() -> this._player.broadcastPacket(new SocialAction(this._player.getObjectId(), 17)), 300L);
         sm = new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
         sm.addZoneName(this._player.getX(), this._player.getY(), this._player.getZ());
         sm.addItemName(this._item);
         CursedWeaponsManager.announce(sm);
      }
   }

   public void saveData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement del = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO cursed_weapons (itemId, charId, playerKarma, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)"
         );
      ) {
         del.setInt(1, this._itemId);
         del.executeUpdate();
         if (this._isActivated) {
            ps.setInt(1, this._itemId);
            ps.setInt(2, this._playerId);
            ps.setInt(3, this._playerKarma);
            ps.setInt(4, this._playerPkKills);
            ps.setInt(5, this._nbKills);
            ps.setLong(6, this._endTime);
            ps.executeUpdate();
         }
      } catch (SQLException var12) {
         LOGGER.log(Level.SEVERE, "CursedWeapon: Failed to save data.", (Throwable)var12);
      }
   }

   public void dropIt(Creature killer) {
      if (Rnd.get(100) <= this._disapearChance) {
         this.endOfLife();
      } else {
         this.dropIt(null, null, killer, false);
         this._player.setKarma(this._playerKarma);
         this._player.setPkKills(this._playerPkKills);
         this._player.setCursedWeaponEquippedId(0);
         this.removeSkill();
         this._player.abortAttack();
         this._player.broadcastUserInfo();
      }
   }

   public void increaseKills() {
      this._nbKills++;
      if (this._player != null && this._player.isOnline()) {
         this._player.setPkKills(this._nbKills);
         this._player.updateUserInfo();
         if (this._nbKills % this._stageKills == 0 && this._nbKills <= this._stageKills * (this._skillMaxLevel - 1)) {
            this.giveSkill();
         }
      }

      this._endTime = this._endTime - this._durationLost * 60000;
      this.saveData();
   }

   public void setDisapearChance(int disapearChance) {
      this._disapearChance = disapearChance;
   }

   public void setDropRate(int dropRate) {
      this._dropRate = dropRate;
   }

   public void setDuration(int duration) {
      this._duration = duration;
   }

   public void setDurationLost(int durationLost) {
      this._durationLost = durationLost;
   }

   public void setStageKills(int stageKills) {
      this._stageKills = stageKills;
   }

   public void setNbKills(int nbKills) {
      this._nbKills = nbKills;
   }

   public void setPlayerId(int playerId) {
      this._playerId = playerId;
   }

   public void setPlayerKarma(int playerKarma) {
      this._playerKarma = playerKarma;
   }

   public void setPlayerPkKills(int playerPkKills) {
      this._playerPkKills = playerPkKills;
   }

   public void setActivated(boolean isActivated) {
      this._isActivated = isActivated;
   }

   public void setDropped(boolean isDropped) {
      this._isDropped = isDropped;
   }

   public void setEndTime(long endTime) {
      this._endTime = endTime;
   }

   public void setPlayer(Player player) {
      this._player = player;
   }

   public void setItem(Item item) {
      this._item = item;
   }

   public boolean isActivated() {
      return this._isActivated;
   }

   public boolean isDropped() {
      return this._isDropped;
   }

   public long getEndTime() {
      return this._endTime;
   }

   public String getName() {
      return this._name;
   }

   public int getItemId() {
      return this._itemId;
   }

   public int getSkillId() {
      return this._skillId;
   }

   public int getPlayerId() {
      return this._playerId;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getPlayerKarma() {
      return this._playerKarma;
   }

   public int getPlayerPkKills() {
      return this._playerPkKills;
   }

   public int getNbKills() {
      return this._nbKills;
   }

   public int getStageKills() {
      return this._stageKills;
   }

   public boolean isActive() {
      return this._isActivated || this._isDropped;
   }

   public int getLevel() {
      return this._nbKills > this._stageKills * this._skillMaxLevel ? this._skillMaxLevel : this._nbKills / this._stageKills;
   }

   public long getTimeLeft() {
      return this._endTime - System.currentTimeMillis();
   }

   public void goTo(Player player) {
      if (player != null) {
         if (this._isActivated && this._player != null) {
            player.teleToLocation(this._player.getLocation(), true);
         } else if (this._isDropped && this._item != null) {
            player.teleToLocation(this._item.getLocation(), true);
         } else {
            player.sendMessage(this._name + " isn't in the World.");
         }
      }
   }

   public Location getWorldPosition() {
      if (this._isActivated && this._player != null) {
         return this._player.getLocation();
      } else {
         return this._isDropped && this._item != null ? this._item.getLocation() : null;
      }
   }

   public long getDuration() {
      return this._duration;
   }

   private class RemoveTask implements Runnable {
      protected RemoveTask() {
         Objects.requireNonNull(CursedWeapon.this);
         super();
      }

      @Override
      public void run() {
         if (System.currentTimeMillis() >= CursedWeapon.this._endTime) {
            CursedWeapon.this.endOfLife();
         }
      }
   }
}
