package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.FortSiegeGuardAI;
import org.l2jmobius.gameserver.ai.SiegeGuardAI;
import org.l2jmobius.gameserver.ai.SpecialSiegeGuardAI;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class Defender extends Attackable {
   private Castle _castle = null;
   private Fort _fort = null;
   private SiegableHall _hall = null;

   public Defender(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Defender);
   }

   @Override
   protected CreatureAI initAI() {
      if (this.getConquerableHall() == null && this.getCastle(10000L) == null) {
         return new FortSiegeGuardAI(this);
      } else {
         return (CreatureAI)(this.getCastle(10000L) != null ? new SiegeGuardAI(this) : new SpecialSiegeGuardAI(this));
      }
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      if (!attacker.isPlayable()) {
         return false;
      } else {
         Player player = attacker.asPlayer();
         if (this._fort != null && this._fort.getZone().isActive()
            || this._castle != null && this._castle.getZone().isActive()
            || this._hall != null && this._hall.getSiegeZone().isActive()) {
            int activeSiegeId = this._fort != null
               ? this._fort.getResidenceId()
               : (this._castle != null ? this._castle.getResidenceId() : (this._hall != null ? this._hall.getId() : 0));
            if (player != null
               && (
                  player.getSiegeState() == 2 && !player.isRegisteredOnThisSiegeField(activeSiegeId)
                     || player.getSiegeState() == 1 && !TerritoryWarManager.getInstance().isAllyField(player, activeSiegeId)
                     || player.getSiegeState() == 0
               )) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean hasRandomAnimation() {
      return false;
   }

   @Override
   public void returnHome() {
      if (!(this.getWalkSpeed() <= 0.0)) {
         if (this.getSpawn() != null) {
            if (!this.isInsideRadius2D(this.getSpawn(), 40)) {
               this.clearAggroList();
               if (this.hasAI()) {
                  this.getAI().setIntentionMoveTo(this.getSpawn().getLocation());
               }
            }
         }
      }
   }

   @Override
   public void onSpawn() {
      super.onSpawn();
      this._fort = org.l2jmobius.gameserver.managers.FortManager.getInstance().getFort(this.getX(), this.getY(), this.getZ());
      this._castle = CastleManager.getInstance().getCastle(this.getX(), this.getY(), this.getZ());
      this._hall = this.getConquerableHall();
      if (this._fort == null && this._castle == null && this._hall == null) {
         LOGGER.warning(
            "Defender spawned outside of Fortress, Castle or Siegable hall Zone! NpcId: "
               + this.getId()
               + " x="
               + this.getX()
               + " y="
               + this.getY()
               + " z="
               + this.getZ()
         );
      }
   }

   @Override
   public void onAction(Player player, boolean interact) {
      if (!this.canTarget(player)) {
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         if (this != player.getTarget()) {
            player.setTarget(this);
         } else if (interact) {
            if (this.isAutoAttackable(player) && !this.isAlikeDead() && Math.abs(player.getZ() - this.getZ()) < 600) {
               player.getAI().setIntentionAttack(this);
            }

            if (!this.isAutoAttackable(player) && !this.canInteract(player)) {
               player.getAI().setIntentionInteract(this);
            }
         }

         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   @Override
   public void addDamageHate(Creature attacker, long damage, long aggro) {
      if (attacker != null) {
         if (!(attacker instanceof Defender)) {
            if (damage == 0L && aggro <= 1L && attacker.isPlayable()) {
               Player player = attacker.asPlayer();
               if (this._fort != null && this._fort.getZone().isActive()
                  || this._castle != null && this._castle.getZone().isActive()
                  || this._hall != null && this._hall.getSiegeZone().isActive()) {
                  int activeSiegeId = this._fort != null
                     ? this._fort.getResidenceId()
                     : (this._castle != null ? this._castle.getResidenceId() : (this._hall != null ? this._hall.getId() : 0));
                  if (player.getSiegeState() == 2 && player.isRegisteredOnThisSiegeField(activeSiegeId)) {
                     return;
                  }
               }
            }

            super.addDamageHate(attacker, damage, aggro);
         }
      }
   }
}
