package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSelect;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.CharSelected;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SSQInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;

public class CharacterSelect extends ClientPacket {
   protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private int _charSlot;
   private int _unk1;
   private int _unk2;
   private int _unk3;
   private int _unk4;

   @Override
   protected void readImpl() {
      this._charSlot = this.readInt();
      this._unk1 = this.readShort();
      this._unk2 = this.readInt();
      this._unk3 = this.readInt();
      this._unk4 = this.readInt();
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      if (client.getFloodProtectors().canSelectCharacter()) {
         if (client.getPlayerLock().tryLock()) {
            try {
               if (client.getPlayer() == null) {
                  CharacterInfoHolder info = client.getCharSelection(this._charSlot);
                  if (info == null) {
                     return;
                  }

                  Player player = World.getPlayer(info.getObjectId());
                  if (player != null) {
                     if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS && player.isAutoPlaying()) {
                        OfflinePlayTable.getInstance().removeOfflinePlay(player);
                     }

                     Disconnection.of(player).storeAndDelete();
                  }

                  if (PunishmentManager.getInstance().hasPunishment(info.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.BAN)
                     || PunishmentManager.getInstance().hasPunishment(client.getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.BAN)
                     || PunishmentManager.getInstance().hasPunishment(client.getIp(), PunishmentAffect.IP, PunishmentType.BAN)) {
                     client.close(ServerClose.STATIC_PACKET);
                     return;
                  }

                  if (info.getAccessLevel() < 0) {
                     client.close(ServerClose.STATIC_PACKET);
                     return;
                  }

                  if (client.getCharSelection(this._charSlot).getAccessLevel() < 100) {
                     if (player != null
                        && player.hasPremiumStatus()
                        && DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP > 0
                        && !AntiFeedManager.getInstance().tryAddPlayer(4, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP)) {
                        NpcHtmlMessage msg = new NpcHtmlMessage();
                        msg.setFile(null, "data/html/mods/IPRestriction.htm");
                        msg.replace(
                           "%max%",
                           String.valueOf(AntiFeedManager.getInstance().getLimit(client, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP))
                        );
                        client.sendPacket(msg);
                        return;
                     }

                     if (DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP > 0
                        && !AntiFeedManager.getInstance().tryAddClient(0, client, DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP)) {
                        NpcHtmlMessage msg = new NpcHtmlMessage();
                        msg.setFile(null, "data/html/mods/IPRestriction.htm");
                        msg.replace(
                           "%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(client, DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP))
                        );
                        client.sendPacket(msg);
                        return;
                     }
                  }

                  if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS) {
                     if (info.isGood()
                        && World.getAllGoodPlayers().size() >= World.getAllEvilPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT) {
                        NpcHtmlMessage msg = new NpcHtmlMessage();
                        msg.setFile(null, "data/html/mods/Faction/ExceededOnlineLimit.htm");
                        msg.replace("%more%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
                        msg.replace("%less%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
                        client.sendPacket(msg);
                        return;
                     }

                     if (info.isEvil()
                        && World.getAllEvilPlayers().size() >= World.getAllGoodPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT) {
                        NpcHtmlMessage msg = new NpcHtmlMessage();
                        msg.setFile(null, "data/html/mods/Faction/ExceededOnlineLimit.htm");
                        msg.replace("%more%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
                        msg.replace("%less%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
                        client.sendPacket(msg);
                        return;
                     }
                  }

                  Player cha = client.load(this._charSlot);
                  if (cha == null) {
                     return;
                  }

                  CharInfoTable.getInstance().addName(cha);
                  if (cha.isGM() && GeneralConfig.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", cha.getAccessLevel())) {
                     cha.setInvisible(true);
                  }

                  PlayerVariables vars = cha.getVariables();
                  String restore = vars.getString("RESTORE_LOCATION", "");
                  if (!restore.isEmpty()) {
                     String[] split = restore.split(";");
                     cha.setXYZ(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                     vars.remove("RESTORE_LOCATION");
                  }

                  cha.setClient(client);
                  client.setPlayer(cha);
                  cha.setOnlineStatus(true, true);
                  if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SELECT, Containers.Players())) {
                     TerminateReturn terminate = EventDispatcher.getInstance()
                        .notifyEvent(new OnPlayerSelect(cha, cha.getObjectId(), cha.getName(), client), Containers.Players(), TerminateReturn.class);
                     if (terminate != null && terminate.terminate()) {
                        Disconnection.of(cha).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
                        return;
                     }
                  }

                  client.sendPacket(new SSQInfo());
                  client.setConnectionState(ConnectionState.ENTERING);
                  client.sendPacket(new CharSelected(cha, client.getSessionId().playOkID1));
               }
            } finally {
               client.getPlayerLock().unlock();
            }

            LOGGER_ACCOUNTING.info("Logged in, " + client);
         }
      }
   }
}
