package org.l2jmobius.gameserver.entity.actor.tasks.player;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class IllegalPlayerActionTask implements Runnable {
   private static final Logger AUDIT_LOGGER = Logger.getLogger("audit");
   private final String _message;
   private final IllegalActionPunishmentType _punishment;
   private final Player _actor;

   public IllegalPlayerActionTask(Player actor, String message, IllegalActionPunishmentType punishment) {
      this._message = message;
      this._punishment = punishment;
      this._actor = actor;
      switch (punishment) {
         case KICK:
            this._actor.sendMessage("You will be kicked for illegal action, GM informed.");
            break;
         case KICKBAN:
            if (!this._actor.isGM()) {
               this._actor.setAccessLevel(-1);
               this._actor.setAccountAccesslevel(-1);
            }

            this._actor.sendMessage("You are banned for illegal action, GM informed.");
            break;
         case JAIL:
            this._actor.sendMessage("Illegal action performed!");
            this._actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
      }
   }

   @Override
   public void run() {
      StringBuilder sb = new StringBuilder();
      sb.append("AUDIT, ");
      sb.append(this._message);
      sb.append(", ");
      sb.append(this._actor);
      sb.append(", ");
      sb.append(this._punishment);
      AUDIT_LOGGER.info(sb.toString());
      if (!this._actor.isGM()) {
         switch (this._punishment) {
            case KICK:
               Disconnection.of(this._actor).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
               break;
            case KICKBAN:
               PunishmentManager.getInstance()
                  .startPunishment(
                     new PunishmentTask(
                        this._actor.getObjectId(),
                        PunishmentAffect.CHARACTER,
                        PunishmentType.BAN,
                        System.currentTimeMillis() + GeneralConfig.DEFAULT_PUNISH_PARAM * 1000L,
                        this._message,
                        this.getClass().getSimpleName()
                     )
                  );
               break;
            case JAIL:
               PunishmentManager.getInstance()
                  .startPunishment(
                     new PunishmentTask(
                        this._actor.getObjectId(),
                        PunishmentAffect.CHARACTER,
                        PunishmentType.JAIL,
                        System.currentTimeMillis() + GeneralConfig.DEFAULT_PUNISH_PARAM * 1000L,
                        this._message,
                        this.getClass().getSimpleName()
                     )
                  );
               break;
            case BROADCAST:
               AdminData.getInstance().broadcastMessageToGMs(this._message);
               return;
         }
      }
   }
}
