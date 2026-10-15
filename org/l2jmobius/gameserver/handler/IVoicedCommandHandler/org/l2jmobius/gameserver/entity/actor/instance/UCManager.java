package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.games.UndergroundColiseumManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCBestTeam;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCWaiting;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class UCManager extends Folk {
   private static final Logger LOGGER = Logger.getLogger(UCManager.class.getName());

   public UCManager(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.UCManagerInstance);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      StringTokenizer token = new StringTokenizer(command, " ");
      String actualCommand = token.nextToken();
      if (actualCommand.equalsIgnoreCase("register")) {
         try {
            if (!player.isInParty()) {
               html.setFile(player, "data/html/undergroundColiseum/noTeam.htm");
               player.sendPacket(html);
               return;
            }

            if (!Olympiad.getInstance().isRegistered(player) && !player.isInOlympiadMode() && !player.isOnEvent() && !player.isRegisteredOnEvent()) {
               if (player.isCursedWeaponEquipped()) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_WHILE_POSSESSING_A_CURSED_WEAPON);
                  return;
               }

               if (!UndergroundColiseumManager.getInstance().isStarted()) {
                  html.setFile(player, "data/html/undergroundColiseum/notStarted.htm");
                  player.sendPacket(html);
                  return;
               }

               Party party = player.getParty();
               if (!party.isLeader(player)) {
                  html.setFile(player, "data/html/undergroundColiseum/notPartyLeader.htm");
                  player.sendPacket(html);
                  return;
               }

               if (party.getUCState() instanceof UCWaiting) {
                  html.setFile(player, "data/html/undergroundColiseum/alreadyRegistered.htm");
                  player.sendPacket(html);
                  return;
               }

               int val = Integer.parseInt(token.nextToken());
               UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
               if (arena == null) {
                  player.sendMessage("This arena is temporarly unavailable.");
                  return;
               }

               if (arena.getTeams()[0].getParty() == null && arena.getTeams()[1].getParty() == null
                  || arena.getTeams()[0].getParty() != party && arena.getTeams()[1].getParty() != party) {
                  int realCount = 0;

                  for (Player member : party.getMembers()) {
                     if (member != null) {
                        if (member.getPlayerClass().level() < 2) {
                           NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
                           packet.setFile(member, "data/html/undergroundColiseum/wrongLevel.htm");
                           packet.replace("%name%", member.getName());
                           player.sendPacket(packet);
                           return;
                        }

                        if (member.getLevel() < arena.getMinLevel() || member.getLevel() > arena.getMaxLevel()) {
                           NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
                           packet.setFile(member, "data/html/undergroundColiseum/wrongLevel.htm");
                           packet.replace("%name%", member.getName());
                           player.sendPacket(packet);
                           return;
                        }

                        realCount++;
                     }
                  }

                  if (realCount < UndergroundColiseumConfig.UC_PARTY_SIZE) {
                     NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
                     packet.setFile(player, "data/html/undergroundColiseum/notEnoughMembers.htm");
                     player.sendPacket(packet);
                     return;
                  }

                  if (arena.getWaitingList().size() >= 5) {
                     NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
                     packet.setFile(player, "data/html/undergroundColiseum/arenaFull.htm");
                     player.sendPacket(packet);
                     return;
                  }

                  UCWaiting waiting = new UCWaiting(party, arena);
                  arena.getWaitingList().add(waiting);
                  waiting.setParty(true);
                  waiting.hasRegisterdNow();
                  html.setFile(player, "data/html/undergroundColiseum/registered.htm");
                  player.sendPacket(html);
                  if (arena.getWaitingList().size() >= 2 && !arena.isBattleNow()) {
                     arena.runTaskNow();
                  }

                  return;
               }

               html.setFile(player, "data/html/undergroundColiseum/alreadyRegistered.htm");
               player.sendPacket(html);
               return;
            }

            player.sendPacket(
               SystemMessageId.PVP_MATCHES_SUCH_AS_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEI_S_CUBE_AND_HANDY_S_BLOCK_CHECKERS_OVERLAP_AND_YOU_CANNOT_PARTICIPATE
            );
            return;
         } catch (Exception var16) {
            LOGGER.log(Level.WARNING, "onBypassFeedback: ", (Throwable)var16);
         }
      } else if (actualCommand.equalsIgnoreCase("cancel")) {
         Party partyx = player.getParty();
         if (partyx == null || !partyx.isLeader(player)) {
            return;
         }

         if (partyx.getUCState() instanceof UCWaiting) {
            NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
            UCWaiting waiting = (UCWaiting)partyx.getUCState();
            waiting.setParty(false);
            waiting.clean();
            waiting.getBaseArena().getWaitingList().remove(waiting);
            packet.setFile(player, "data/html/undergroundColiseum/registrantionCanceled.htm");
            player.sendPacket(packet);
         }
      } else if (actualCommand.equalsIgnoreCase("bestTeam")) {
         int valx = Integer.parseInt(token.nextToken());
         UCArena arenax = UndergroundColiseumManager.getInstance().getArena(valx);
         UCBestTeam bestTeam = UndergroundColiseumManager.getInstance().getBestTeam(arenax.getId());
         if (bestTeam != null) {
            NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
            packet.setFile(player, "data/html/undergroundColiseum/bestTeam.htm");
            packet.replace("%name%", bestTeam.getLeaderName());
            packet.replace("%best%", String.valueOf(bestTeam.getWins()));
            player.sendPacket(packet);
         } else {
            NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
            packet.setFile(player, "data/html/undergroundColiseum/view-most-wins.htm");
            player.sendPacket(packet);
         }
      } else if (actualCommand.equalsIgnoreCase("listTeams")) {
         int valx = Integer.parseInt(token.nextToken());
         UCArena arenax = UndergroundColiseumManager.getInstance().getArena(valx);
         if (arenax == null) {
            player.sendMessage("This arena is temporarly unavailable.");
            return;
         }

         NpcHtmlMessage packet = new NpcHtmlMessage(this.getObjectId());
         packet.setFile(player, "data/html/undergroundColiseum/view-participating-teams.htm");
         String list = "";
         int i = 0;
         int currentReg = arenax.getWaitingList().size();

         for (int var29 = 1; var29 <= 5; var29++) {
            if (var29 > currentReg) {
               list = list + var29 + ". (Participating Team: Team)<br>";
            } else {
               Party partyxx = arenax.getWaitingList().get(var29 - 1).getParty();
               if (partyxx == null) {
                  list = list + var29 + ". (Participating Team: Team)<br>";
               } else {
                  String teamList = "";

                  for (Player m : partyxx.getMembers()) {
                     if (m != null) {
                        teamList = teamList + m.getName() + ";";
                     }
                  }

                  list = list + var29 + ". (Participating Team: <font color=00ffff>" + teamList + "</font>)<br>";
               }
            }
         }

         packet.replace("%list%", list);
         player.sendPacket(packet);
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   @Override
   public String getHtmlPath(int npcId, int val) {
      String pom = "";
      if (val == 0) {
         pom = String.valueOf(npcId);
      } else {
         pom = npcId + "-" + val;
      }

      return "data/html/undergroundColiseum/" + pom + ".htm";
   }
}
