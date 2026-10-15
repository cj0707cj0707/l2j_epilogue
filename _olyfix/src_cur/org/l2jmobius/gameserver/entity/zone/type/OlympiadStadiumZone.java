package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class OlympiadStadiumZone extends ZoneType {
   private int _stadiumId;

   public OlympiadStadiumZone(int id) {
      super(id);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("stadiumId")) {
         this._stadiumId = Integer.parseInt(value);
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature character) {
      character.setInsideZone(ZoneId.PVP, true);
      character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
      character.setInsideZone(ZoneId.NO_LANDING, true);
      character.setInsideZone(ZoneId.NO_RESTART, true);
      character.setInsideZone(ZoneId.NO_BOOKMARK, true);
      if (character instanceof Player) {
         character.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
      }

      if (character instanceof Playable) {
         Player player = character.asPlayer();
         if (player != null && !player.isGM() && !player.isInOlympiadMode() && !player.inObserverMode()) {
            if (character instanceof Summon) {
               character.asSummon().unSummon(player);
            }

            player.teleToLocation(TeleportWhereType.TOWN);
         }
      }
   }

   @Override
   protected void onExit(Creature character) {
      character.setInsideZone(ZoneId.PVP, false);
      character.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
      character.setInsideZone(ZoneId.NO_LANDING, false);
      character.setInsideZone(ZoneId.NO_RESTART, false);
      character.setInsideZone(ZoneId.NO_BOOKMARK, false);
      if (character instanceof Player) {
         character.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
      }
   }

   @Override
   public void onDieInside(Creature character) {
   }

   @Override
   public void onReviveInside(Creature character) {
   }

   public int getStadiumId() {
      return this._stadiumId;
   }
}
