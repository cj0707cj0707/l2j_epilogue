package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ExHeroList;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class OlympiadManager extends Npc {
   private static Logger _logOlymp = Logger.getLogger(OlympiadManager.class.getName());
   private static final int GATE_PASS = OlympiadConfig.OLYMPIAD_COMP_RITEM;
   private static final String FEWER_THAN = "Fewer than " + OlympiadConfig.OLYMPIAD_REG_DISPLAY;
   private static final String MORE_THAN = "More than " + OlympiadConfig.OLYMPIAD_REG_DISPLAY;
   private static final SkillHolder[] ALLOWED_BUFFS = new SkillHolder[]{
      new SkillHolder(4357, 2),
      new SkillHolder(4342, 2),
      new SkillHolder(4356, 3),
      new SkillHolder(4355, 3),
      new SkillHolder(4351, 6),
      new SkillHolder(4345, 3),
      new SkillHolder(4358, 3),
      new SkillHolder(4359, 3),
      new SkillHolder(4360, 3),
      new SkillHolder(4352, 2)
   };

   public OlympiadManager(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.OlympiadManager);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.startsWith("OlympiadDesc")) {
         int val = Integer.parseInt(command.substring(13, 14));
         String suffix = command.substring(14);
         this.showChatWindow(player, val, suffix);
      } else if (command.startsWith("OlympiadNoble")) {
         if (player.getClassIndex() != 0) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, "data/html/olympiad/noble_desc5.htm");
            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
            return;
         }

         if (!player.isNoble() || player.getPlayerClass().level() < 3) {
            NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
            html.setFile(player, "data/html/olympiad/noble_desc6.htm");
            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
            return;
         }

         int val = Integer.parseInt(command.substring(14));
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         switch (val) {
            case 1:
               Olympiad.getInstance().unRegisterNoble(player);
               break;
            case 2:
               int classed = 0;
               int nonClassed = 0;
               int[] array = Olympiad.getInstance().getWaitingList();
               if (array != null) {
                  classed = array[0];
                  nonClassed = array[1];
               }

               html.setFile(player, "data/html/olympiad/noble_registered.htm");
               if (OlympiadConfig.OLYMPIAD_REG_DISPLAY > 0) {
                  html.replace("%listClassed%", classed < OlympiadConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
                  html.replace("%listNonClassedTeam%", FEWER_THAN);
                  html.replace("%listNonClassed%", nonClassed < OlympiadConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
               } else {
                  html.replace("%listClassed%", String.valueOf(classed));
                  html.replace("%listNonClassedTeam%", "0");
                  html.replace("%listNonClassed%", String.valueOf(nonClassed));
               }

               html.replace("%objectId%", String.valueOf(this.getObjectId()));
               player.sendPacket(html);
               break;
            case 3:
               int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
               html.setFile(player, "data/html/olympiad/noble_points1.htm");
               html.replace("%points%", String.valueOf(points));
               html.replace("%objectId%", String.valueOf(this.getObjectId()));
               player.sendPacket(html);
               break;
            case 4:
               Olympiad.getInstance().registerNoble(player, false);
               break;
            case 5:
               Olympiad.getInstance().registerNoble(player, true);
               break;
            case 6:
               if (player.getVariables().getInt("UNCLAIMED_OLYMPIAD_POINTS", 0) > 0) {
                  html.setFile(player, "data/html/olympiad/noble_settle.htm");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
               } else {
                  html.setFile(player, "data/html/olympiad/noble_nopoints.htm");
                  html.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(html);
               }
               break;
            case 7:
               MultisellData.getInstance().separateAndSend(102, player, this, false);
               break;
            case 8:
               MultisellData.getInstance().separateAndSend(103, player, this, false);
               break;
            case 9:
               int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
               html.setFile(player, "data/html/olympiad/noble_points2.htm");
               html.replace("%points%", String.valueOf(point));
               html.replace("%objectId%", String.valueOf(this.getObjectId()));
               player.sendPacket(html);
               break;
            case 10:
               int passes = player.getVariables().getInt("UNCLAIMED_OLYMPIAD_POINTS", 0);
               if (passes > 0) {
                  player.getVariables().remove("UNCLAIMED_OLYMPIAD_POINTS");
                  player.addItem(ItemProcessType.REWARD, GATE_PASS, passes * OlympiadConfig.OLYMPIAD_GP_PER_POINT, player, true);
               }
               break;
            default:
               _logOlymp.warning("Olympiad System: Could not send packet for request " + val);
         }
      } else if (command.startsWith("OlyBuff")) {
         String[] params = command.split(" ");
         if (!StringUtil.isNumeric(params[1])) {
            LOGGER.warning("Olympiad Buffer Warning: npcId = " + this + " has invalid buffGroup set in the bypass for the buff selected: " + params[1]);
            return;
         }

         int index = Integer.parseInt(params[1]);
         if (index < 0 || index >= ALLOWED_BUFFS.length) {
            LOGGER.warning("Olympiad Buffer Warning: npcId = " + this + " has invalid index sent in the bypass: " + index);
            return;
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         this.setTarget(player);
         if (player.getOlympiadBuffCount() > 0) {
            Skill skill = ALLOWED_BUFFS[index].getSkill();
            if (skill != null) {
               int buffCount = player.getOlympiadBuffCount();
               this.broadcastPacket(new MagicSkillUse(this, player, skill.getId(), skill.getLevel(), 0, 0));
               skill.applyEffects(player, player);
               player.setOlympiadBuffCount(--buffCount);
            }
         }

         if (player.getOlympiadBuffCount() > 0) {
            html.setFile(player, player.getOlympiadBuffCount() == 5 ? "data/html/olympiad/olympiad_buffs.htm" : "data/html/olympiad/olympiad_5buffs.htm");
            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
         } else {
            html.setFile(player, "data/html/olympiad/olympiad_nobuffs.htm");
            html.replace("%objectId%", String.valueOf(this.getObjectId()));
            player.sendPacket(html);
            this.deleteMe();
         }
      } else if (command.startsWith("Olympiad")) {
         int val = Integer.parseInt(command.substring(9, 10));
         NpcHtmlMessage reply = new NpcHtmlMessage(this.getObjectId());
         switch (val) {
            case 1:
               Map<Integer, String> matches = Olympiad.getInstance().getMatchList();
               reply.setFile(player, "data/html/olympiad/olympiad_observe1.htm");

               for (int i = 0; i < Olympiad.getStadiumCount(); i++) {
                  int arenaID = i + 1;
                  reply.replace("%title" + arenaID + "%", matches.getOrDefault(i, "\\&$906;"));
               }

               reply.replace("%objectId%", String.valueOf(this.getObjectId()));
               player.sendPacket(reply);
               break;
            case 2:
               int classId = Integer.parseInt(command.substring(11));
               if (classId >= 88 && classId <= 118 || classId >= 131 && classId <= 134 || classId == 136) {
                  List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
                  reply.setFile(player, "data/html/olympiad/olympiad_ranking.htm");
                  int indexx = 1;

                  for (String name : names) {
                     reply.replace("%place" + indexx + "%", String.valueOf(indexx));
                     reply.replace("%rank" + indexx + "%", name);
                     if (++indexx > 10) {
                        break;
                     }
                  }

                  while (indexx <= 10) {
                     reply.replace("%place" + indexx + "%", "");
                     reply.replace("%rank" + indexx + "%", "");
                     indexx++;
                  }

                  reply.replace("%objectId%", String.valueOf(this.getObjectId()));
                  player.sendPacket(reply);
               }
               break;
            case 3:
               int id = Integer.parseInt(command.substring(11));
               Olympiad.addSpectator(id, player, true);
               break;
            case 4:
               player.sendPacket(new ExHeroList());
               break;
            default:
               _logOlymp.warning("Olympiad System: Could not send packet for request " + val);
         }
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   public void showChatWindow(Player player, int value, String suffix) {
      String filename = "data/html/olympiad/";
      filename = filename + "noble_desc" + value;
      filename = filename + (suffix != null ? suffix + ".htm" : ".htm");
      if (filename.equals("data/html/olympiad/noble_desc0.htm")) {
         filename = "data/html/olympiad/noble_main.htm";
      }

      this.showChatWindow(player, filename);
   }
}
