package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.concurrent.Future;
import java.util.logging.Level;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

public class Decoy extends Creature {
   private final Player _owner;
   private Future<?> _decoyLifeTask;
   private Future<?> _hateSpam;

   public Decoy(NpcTemplate template, Player owner, int totalLifeTime) {
      this(template, owner, totalLifeTime, true);
   }

   public Decoy(NpcTemplate template, Player owner, int totalLifeTime, boolean aggressive) {
      super(template);
      this.setInstanceType(InstanceType.Decoy);
      this._owner = owner;
      this.setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
      this.setInvul(false);
      this._decoyLifeTask = ThreadPool.schedule(() -> this.unSummon(owner), totalLifeTime);
      if (aggressive) {
         int hateSpamSkillId = 5272;
         int skilllevel = Math.min(this.getTemplate().getDisplayId() - 13070, SkillData.getInstance().getMaxLevel(5272));
         this._hateSpam = ThreadPool.scheduleAtFixedRate(new Decoy.HateSpam(this, SkillData.getInstance().getSkill(5272, skilllevel)), 2000L, 5000L);
      }
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (this._hateSpam != null) {
            this._hateSpam.cancel(true);
            this._hateSpam = null;
         }

         this.unSummon(this._owner);
         DecayTaskManager.getInstance().add(this);
         return true;
      }
   }

   public synchronized void unSummon(Player owner) {
      if (this._hateSpam != null) {
         this._hateSpam.cancel(true);
         this._hateSpam = null;
      }

      if (this.isSpawned() && !this.isDead()) {
         ZoneManager.getInstance().getRegion(this).removeFromZones(this);
         owner.setDecoy(null);
         this.decayMe();
      }

      if (this._decoyLifeTask != null) {
         this._decoyLifeTask.cancel(false);
         this._decoyLifeTask = null;
      }
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this.sendPacket(new CharInfo(this, false));
   }

   @Override
   public void updateAbnormalEffect() {
      World.forEachVisibleObject(this, Player.class, player -> {
         if (this.isVisibleFor(player)) {
            player.sendPacket(new CharInfo(this, false));
         }
      });
   }

   public void stopDecay() {
      DecayTaskManager.getInstance().cancel(this);
   }

   @Override
   public void onDecay() {
      this.deleteMe(this._owner);
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return this._owner.isAutoAttackable(attacker);
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
   public int getId() {
      return this.getTemplate().getId();
   }

   @Override
   public int getLevel() {
      return this.getTemplate().getLevel();
   }

   public void deleteMe(Player owner) {
      this.decayMe();
      owner.setDecoy(null);
   }

   public Player getOwner() {
      return this._owner;
   }

   @Override
   public Player asPlayer() {
      return this._owner;
   }

   public NpcTemplate getTemplate() {
      return (NpcTemplate)super.getTemplate();
   }

   @Override
   public void sendInfo(Player player) {
      player.sendPacket(new CharInfo(this, false));
   }

   @Override
   public void sendPacket(ServerPacket packet) {
      if (this._owner != null) {
         this._owner.sendPacket(packet);
      }
   }

   @Override
   public void sendPacket(SystemMessageId id) {
      if (this._owner != null) {
         this._owner.sendPacket(id);
      }
   }

   private static class HateSpam implements Runnable {
      private final Decoy _player;
      private final Skill _skill;

      HateSpam(Decoy player, Skill hate) {
         this._player = player;
         this._skill = hate;
      }

      @Override
      public void run() {
         try {
            this._player.setTarget(this._player);
            this._player.doCast(this._skill);
         } catch (Throwable var2) {
            Creature.LOGGER.log(Level.SEVERE, "Decoy Error: ", var2);
         }
      }
   }
}
