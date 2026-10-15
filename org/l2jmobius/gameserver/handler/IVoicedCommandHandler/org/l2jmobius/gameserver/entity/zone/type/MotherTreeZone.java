package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class MotherTreeZone extends ZoneType {
   private int _enterMsg;
   private int _leaveMsg;
   private int _mpRegen;
   private int _hpRegen;

   public MotherTreeZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("enterMsgId")) {
         this._enterMsg = Integer.parseInt(value);
      } else if (name.equals("leaveMsgId")) {
         this._leaveMsg = Integer.parseInt(value);
      } else if (name.equals("MpRegenBonus")) {
         this._mpRegen = Integer.parseInt(value);
      } else if (name.equals("HpRegenBonus")) {
         this._hpRegen = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         creature.setInsideZone(ZoneId.MOTHER_TREE, true);
         if (this._enterMsg != 0) {
            player.sendPacket(new SystemMessage(this._enterMsg));
         }
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         player.setInsideZone(ZoneId.MOTHER_TREE, false);
         if (this._leaveMsg != 0) {
            player.sendPacket(new SystemMessage(this._leaveMsg));
         }
      }
   }

   public int getMpRegenBonus() {
      return this._mpRegen;
   }

   public int getHpRegenBonus() {
      return this._hpRegen;
   }
}
