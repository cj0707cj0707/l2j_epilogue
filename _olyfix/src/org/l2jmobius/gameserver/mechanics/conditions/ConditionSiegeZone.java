package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionSiegeZone extends Condition {
   public static final int COND_NOT_ZONE = 1;
   public static final int COND_CAST_ATTACK = 2;
   public static final int COND_CAST_DEFEND = 4;
   public static final int COND_CAST_NEUTRAL = 8;
   public static final int COND_FORT_ATTACK = 16;
   public static final int COND_FORT_DEFEND = 32;
   public static final int COND_FORT_NEUTRAL = 64;
   public static final int COND_TW_CHANNEL = 128;
   public static final int COND_TW_PROGRESS = 256;
   private final int _value;
   private final boolean _self;

   public ConditionSiegeZone(int value, boolean self) {
      this._value = value;
      this._self = self;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if ((this._value & 256) != 0 && !TerritoryWarManager.getInstance().isTWInProgress()) {
         return false;
      } else if ((this._value & 128) != 0 && !TerritoryWarManager.getInstance().isTWChannelOpen()) {
         return false;
      } else {
         Creature target = this._self ? effector : effected;
         Castle castle = CastleManager.getInstance().getCastle(target);
         Fort fort = FortManager.getInstance().getFort(target);
         if (castle == null && fort == null) {
            return (this._value & 1) != 0;
         } else {
            return castle != null ? checkIfOk(target, castle, this._value) : checkIfOk(target, fort, this._value);
         }
      }
   }

   public static boolean checkIfOk(Creature creature, Castle castle, int value) {
      if (creature != null && creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (castle != null && castle.getResidenceId() > 0) {
            if (!castle.getZone().isActive()) {
               if ((value & 1) != 0) {
                  return true;
               }
            } else {
               if ((value & 2) != 0 && player.isRegisteredOnThisSiegeField(castle.getResidenceId()) && player.getSiegeState() == 1) {
                  return true;
               }

               if ((value & 4) != 0 && player.isRegisteredOnThisSiegeField(castle.getResidenceId()) && player.getSiegeState() == 2) {
                  return true;
               }

               if ((value & 8) != 0 && player.getSiegeState() == 0) {
                  return true;
               }
            }
         } else if ((value & 1) != 0) {
            return true;
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean checkIfOk(Creature creature, Fort fort, int value) {
      if (creature != null && creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (fort != null && fort.getResidenceId() > 0) {
            if (!fort.getZone().isActive()) {
               if ((value & 1) != 0) {
                  return true;
               }
            } else {
               if ((value & 16) != 0 && player.isRegisteredOnThisSiegeField(fort.getResidenceId()) && player.getSiegeState() == 1) {
                  return true;
               }

               if ((value & 32) != 0 && player.isRegisteredOnThisSiegeField(fort.getResidenceId()) && player.getSiegeState() == 2) {
                  return true;
               }

               if ((value & 64) != 0 && player.getSiegeState() == 0) {
                  return true;
               }
            }
         } else if ((value & 1) != 0) {
            return true;
         }

         return false;
      } else {
         return false;
      }
   }
}
