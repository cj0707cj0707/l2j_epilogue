package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.List;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.managers.FishingChampionshipManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Fisherman extends Merchant {
   public Fisherman(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Fisherman);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/fisherman/" + pom + ".htm";
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (command.equalsIgnoreCase("FishSkillList")) {
         showFishSkillList(player);
      } else if (command.startsWith("FishingChampionship")) {
         if (GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED) {
            FishingChampionshipManager.getInstance().showChampScreen(player, this);
         } else {
            this.sendHtml(player, this, "no_fish_event001.htm");
         }
      } else if (command.startsWith("FishingReward")) {
         if (GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED) {
            if (FishingChampionshipManager.getInstance().isWinner(player.getName())) {
               FishingChampionshipManager.getInstance().getReward(player);
            } else {
               this.sendHtml(player, this, "no_fish_event_reward001.htm");
            }
         } else {
            this.sendHtml(player, this, "no_fish_event001.htm");
         }
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   public static void showFishSkillList(Player player) {
      List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableFishingSkills(player);
      AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.FISHING);
      int count = 0;

      for (SkillLearn s : skills) {
         Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
         if (sk != null) {
            count++;
            asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 1);
         }
      }

      if (count == 0) {
         int minlLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getFishingSkillTree());
         if (minlLevel > 0) {
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
            sm.addInt(minlLevel);
            player.sendPacket(sm);
         } else {
            player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
         }
      } else {
         player.sendPacket(asl);
      }
   }

   private void sendHtml(Player player, Npc npc, String htmlName) {
      NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
      html.setFile(player, "data/html/fisherman/championship/" + htmlName);
      player.sendPacket(html);
   }
}
