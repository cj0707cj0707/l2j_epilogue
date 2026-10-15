package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.DoorAI;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.stat.DoorStat;
import org.l2jmobius.gameserver.entity.actor.status.DoorStatus;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.DoorStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.network.serverpackets.StaticObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Door extends Creature {
   public static final byte OPEN_BY_CLICK = 1;
   public static final byte OPEN_BY_TIME = 2;
   public static final byte OPEN_BY_ITEM = 4;
   public static final byte OPEN_BY_SKILL = 8;
   public static final byte OPEN_BY_CYCLE = 16;
   private int _castleIndex = -2;
   private int _fortIndex = -2;
   private ClanHall _clanHall;
   boolean _open = false;
   private boolean _isAttackableDoor = false;
   private boolean _isTargetable;
   private int _meshindex = 1;
   private Future<?> _autoCloseTask;

   public Door(DoorTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Door);
      this.setInvul(false);
      this.setLethalable(false);
      this._open = template.isOpenByDefault();
      this._isAttackableDoor = template.isAttackable();
      this._isTargetable = template.isTargetable();
      if (this.getGroupName() != null) {
         DoorData.addDoorGroup(this.getGroupName(), this.getId());
      }

      if (this.isOpenableByTime()) {
         this.startTimerOpen();
      }

      int clanhallId = template.getClanHallId();
      if (clanhallId > 0) {
         ClanHall hall = ClanHallTable.getAllClanHalls().get(clanhallId);
         if (hall != null) {
            this.setClanHall(hall);
            hall.getDoors().add(this);
         }
      }
   }

   @Override
   protected CreatureAI initAI() {
      return new DoorAI(this);
   }

   private void startTimerOpen() {
      int delay = this._open ? this.getTemplate().getOpenTime() : this.getTemplate().getCloseTime();
      if (this.getTemplate().getRandomTime() > 0) {
         delay += Rnd.get(this.getTemplate().getRandomTime());
      }

      ThreadPool.schedule(new Door.TimerOpen(), delay * 1000);
   }

   public DoorTemplate getTemplate() {
      return (DoorTemplate)super.getTemplate();
   }

   public DoorStatus getStatus() {
      return (DoorStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new DoorStatus(this));
   }

   @Override
   public void initCharStat() {
      this.setStat(new DoorStat(this));
   }

   public DoorStat getStat() {
      return (DoorStat)super.getStat();
   }

   public boolean isOpenableBySkill() {
      return (this.getTemplate().getOpenType() & 8) == 8;
   }

   public boolean isOpenableByItem() {
      return (this.getTemplate().getOpenType() & 4) == 4;
   }

   public boolean isOpenableByClick() {
      return (this.getTemplate().getOpenType() & 1) == 1;
   }

   public boolean isOpenableByTime() {
      return (this.getTemplate().getOpenType() & 2) == 2;
   }

   public boolean isOpenableByCycle() {
      return (this.getTemplate().getOpenType() & 16) == 16;
   }

   @Override
   public int getLevel() {
      return this.getTemplate().getLevel();
   }

   @Override
   public int getId() {
      return this.getTemplate().getId();
   }

   public boolean isOpen() {
      return this._open;
   }

   public void setOpen(boolean open) {
      this._open = open;
      if (this.getChildId() > 0) {
         Door sibling = this.getSiblingDoor(this.getChildId());
         if (sibling != null) {
            sibling.notifyChildEvent(open);
         } else {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": cannot find child id: " + this.getChildId());
         }
      }
   }

   public boolean isAttackableDoor() {
      return this._isAttackableDoor;
   }

   public boolean isShowHp() {
      return this.getTemplate().isShowHp();
   }

   public void setIsAttackableDoor(boolean value) {
      this._isAttackableDoor = value;
   }

   public int getDamage() {
      int dmg = 6 - (int)Math.ceil(this.getCurrentHp() / this.getMaxHp() * 6.0);
      if (dmg > 6) {
         return 6;
      } else {
         return dmg < 0 ? 0 : dmg;
      }
   }

   public Castle getCastle() {
      if (this._castleIndex < 0) {
         this._castleIndex = CastleManager.getInstance().getCastleIndex(this);
      }

      return this._castleIndex < 0 ? null : CastleManager.getInstance().getCastles().get(this._castleIndex);
   }

   public Fort getFort() {
      if (this._fortIndex < 0) {
         this._fortIndex = org.l2jmobius.gameserver.managers.FortManager.getInstance().getFortIndex(this);
      }

      return this._fortIndex < 0 ? null : org.l2jmobius.gameserver.managers.FortManager.getInstance().getForts().get(this._fortIndex);
   }

   public void setClanHall(ClanHall clanhall) {
      this._clanHall = clanhall;
   }

   public ClanHall getClanHall() {
      return this._clanHall;
   }

   public boolean isEnemy() {
      if (this.getCastle() != null && this.getCastle().getResidenceId() > 0 && this.getCastle().getZone().isActive() && this.isShowHp()) {
         return true;
      } else {
         return this.getFort() != null && this.getFort().getResidenceId() > 0 && this.getFort().getZone().isActive() && this.isShowHp()
            ? true
            : this._clanHall != null && this._clanHall.isSiegableHall() && ((SiegableHall)this._clanHall).getSiegeZone().isActive() && this.isShowHp();
      }
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (!attacker.isPlayable()) {
         return false;
      } else if (this._isAttackableDoor) {
         return true;
      } else if (!this.isShowHp()) {
         return false;
      } else {
         Player actingPlayer = attacker.asPlayer();
         if (this._clanHall != null && this._clanHall.isSiegableHall()) {
            SiegableHall hall = (SiegableHall)this._clanHall;
            return !hall.isSiegableHall()
               ? false
               : hall.isInSiege() && hall.getSiege().doorIsAutoAttackable() && hall.getSiege().checkIsAttacker(actingPlayer.getClan());
         } else if (TerritoryWarManager.getInstance().isTWInProgress()) {
            int activeSiegeId = this.getFort() != null ? this.getFort().getResidenceId() : (this.getCastle() != null ? this.getCastle().getResidenceId() : 0);
            return !TerritoryWarManager.getInstance().isAllyField(actingPlayer, activeSiegeId);
         } else {
            boolean isFort = this.getFort() != null && this.getFort().getResidenceId() > 0 && this.getFort().getZone().isActive();
            if (isFort) {
               Clan clan = actingPlayer.getClan();
               if (clan != null && clan == this.getFort().getOwnerClan()) {
                  return false;
               }
            }

            boolean isCastle = this.getCastle() != null && this.getCastle().getResidenceId() > 0 && this.getCastle().getZone().isActive();
            if (isCastle) {
               Clan clan = actingPlayer.getClan();
               if (clan != null && clan.getId() == this.getCastle().getOwnerId()) {
                  return false;
               }
            }

            return isCastle || isFort;
         }
      }
   }

   @Override
   public void updateAbnormalEffect() {
   }

   @Override
   public Item getActiveWeaponInstance() {
      return null;
   }

   @Override
   public Weapon getActiveWeaponItem() {
      return null;
   }

   @Override
   public Item getSecondaryWeaponInstance() {
      return null;
   }

   public Weapon getSecondaryWeaponItem() {
      return null;
   }

   @Override
   public void broadcastStatusUpdate() {
      StaticObjectInfo su = new StaticObjectInfo(this, false);
      StaticObjectInfo targetableSu = new StaticObjectInfo(this, true);
      DoorStatusUpdate dsu = new DoorStatusUpdate(this);
      OnEventTrigger oe = this.getEmitter() > 0 ? new OnEventTrigger(this, !this._open) : null;
      boolean useTargetable = this.getCastle() != null && this.getCastle().getResidenceId() > 0
         || this.getFort() != null && this.getFort().getResidenceId() > 0;
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            if (!player.isGM() && !useTargetable) {
               player.sendPacket(su);
            } else {
               player.sendPacket(targetableSu);
            }

            player.sendPacket(dsu);
            if (oe != null) {
               player.sendPacket(oe);
            }
         }
      });
   }

   public void openMe() {
      if (this.getGroupName() != null) {
         this.manageGroupOpen(true, this.getGroupName());
      } else {
         if (!this.isOpen()) {
            this.setOpen(true);
            this.broadcastStatusUpdate();
            this.startAutoCloseTask();
         }
      }
   }

   public void closeMe() {
      Future<?> oldTask = this._autoCloseTask;
      if (oldTask != null) {
         this._autoCloseTask = null;
         oldTask.cancel(false);
      }

      if (this.getGroupName() != null) {
         this.manageGroupOpen(false, this.getGroupName());
      } else {
         if (this.isOpen()) {
            this.setOpen(false);
            this.broadcastStatusUpdate();
         }
      }
   }

   private void manageGroupOpen(boolean open, String groupName) {
      Set<Integer> set = DoorData.getDoorsByGroup(groupName);
      Door first = null;

      for (Integer id : set) {
         Door door = this.getSiblingDoor(id);
         if (first == null) {
            first = door;
         }

         if (door.isOpen() != open) {
            door.setOpen(open);
            door.broadcastStatusUpdate();
         }
      }

      if (first != null && open) {
         first.startAutoCloseTask();
      }
   }

   private void notifyChildEvent(boolean open) {
      byte openThis = open ? this.getTemplate().getMasterDoorOpen() : this.getTemplate().getMasterDoorClose();
      if (openThis == 1) {
         this.openMe();
      } else if (openThis == -1) {
         this.closeMe();
      }
   }

   public String getDoorName() {
      return this.getTemplate().getName();
   }

   public int getX(int i) {
      return this.getTemplate().getNodeX()[i];
   }

   public int getY(int i) {
      return this.getTemplate().getNodeY()[i];
   }

   public int getZMin() {
      return this.getTemplate().getNodeZ();
   }

   public int getZMax() {
      return this.getTemplate().getNodeZ() + this.getTemplate().getHeight();
   }

   public void setMeshIndex(int mesh) {
      this._meshindex = mesh;
   }

   public int getMeshIndex() {
      return this._meshindex;
   }

   public int getEmitter() {
      return this.getTemplate().getEmmiter();
   }

   public boolean isWall() {
      return this.getTemplate().isWall();
   }

   public String getGroupName() {
      return this.getTemplate().getGroupName();
   }

   public int getChildId() {
      return this.getTemplate().getChildDoorId();
   }

   @Override
   public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill) {
      if (this.isWall() && !this.isInInstance()) {
         if (!attacker.isServitor()) {
            return;
         }

         if (attacker.getTemplate().getRace() != Race.SIEGE_WEAPON) {
            return;
         }
      }

      super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
   }

   @Override
   public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill) {
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         boolean isFort = this.getFort() != null && this.getFort().getResidenceId() > 0 && this.getFort().getSiege().isInProgress();
         boolean isCastle = this.getCastle() != null && this.getCastle().getResidenceId() > 0 && this.getCastle().getSiege().isInProgress();
         boolean isHall = this._clanHall != null && this._clanHall.isSiegableHall() && ((SiegableHall)this._clanHall).isInSiege();
         if (isFort || isCastle || isHall) {
            this.broadcastPacket(new SystemMessage(SystemMessageId.THE_CASTLE_GATE_HAS_BEEN_BROKEN_DOWN));
         }

         return true;
      }
   }

   @Override
   public void moveToLocation(int x, int y, int z, int offset) {
   }

   @Override
   public void stopMove(Location loc) {
   }

   @Override
   public void doAttack(Creature target) {
   }

   @Override
   public void doCast(Skill skill) {
   }

   @Override
   public void sendInfo(Player player) {
      if (this.isVisibleFor(player)) {
         player.sendPacket(new StaticObjectInfo(this, player.isGM()));
         player.sendPacket(new DoorStatusUpdate(this));
         if (this.getEmitter() > 0) {
            player.sendPacket(new OnEventTrigger(this, !this._open));
         }
      }
   }

   public void setTargetable(boolean value) {
      this._isTargetable = value;
      this.broadcastStatusUpdate();
   }

   @Override
   public boolean isTargetable() {
      return this._isTargetable;
   }

   public boolean checkCollision() {
      return this.getTemplate().isCheckCollision();
   }

   private Door getSiblingDoor(int doorId) {
      if (!this.isInInstance()) {
         return DoorData.getInstance().getDoor(doorId);
      } else {
         Instance inst = InstanceManager.getInstance().getInstance(this.getInstanceId());
         return inst != null ? inst.getDoor(doorId) : null;
      }
   }

   private void startAutoCloseTask() {
      DoorTemplate template = this.getTemplate();
      if (template.getCloseTime() >= 0 && !this.isOpenableByTime()) {
         if (template.getClanHallId() <= 0) {
            Future<?> oldTask = this._autoCloseTask;
            if (oldTask != null) {
               this._autoCloseTask = null;
               oldTask.cancel(false);
            }

            this._autoCloseTask = ThreadPool.schedule(new Door.AutoClose(), this.getTemplate().getCloseTime() * 1000);
         }
      }
   }

   @Override
   public boolean isDoor() {
      return true;
   }

   @Override
   public Door asDoor() {
      return this;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getClass().getSimpleName());
      sb.append('[');
      sb.append(this.getTemplate().getId());
      sb.append("](");
      sb.append(this.getObjectId());
      sb.append(')');
      return sb.toString();
   }

   class AutoClose implements Runnable {
      AutoClose() {
         Objects.requireNonNull(Door.this);
         super();
      }

      @Override
      public void run() {
         if (Door.this._open) {
            Door.this.closeMe();
         }
      }
   }

   class TimerOpen implements Runnable {
      TimerOpen() {
         Objects.requireNonNull(Door.this);
         super();
      }

      @Override
      public void run() {
         if (Door.this._open) {
            Door.this.closeMe();
         } else {
            Door.this.openMe();
         }

         int delay = Door.this._open ? Door.this.getTemplate().getCloseTime() : Door.this.getTemplate().getOpenTime();
         if (Door.this.getTemplate().getRandomTime() > 0) {
            delay += Rnd.get(Door.this.getTemplate().getRandomTime());
         }

         ThreadPool.schedule(this, delay * 1000);
      }
   }
}
