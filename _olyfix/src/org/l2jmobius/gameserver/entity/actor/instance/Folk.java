package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.status.FolkStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Folk extends Npc {
   public Folk(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Folk);
      this.setInvul(false);
   }

   public FolkStatus getStatus() {
      return (FolkStatus)super.getStatus();
   }

   @Override
   public void initCharStatus() {
      this.setStatus(new FolkStatus(this));
   }

   public List<PlayerClass> getClassesToTeach() {
      return this.getTemplate().getTeachInfo();
   }

   public static void showSkillList(Player player, Npc npc, PlayerClass playerClass) {
      int npcId = npc.getTemplate().getId();
      if (npcId == 32611) {
         List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableCollectSkills(player);
         AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.COLLECT);
         int counts = 0;

         for (SkillLearn s : skills) {
            Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
            if (sk != null) {
               counts++;
               asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), 0, 1);
            }
         }

         if (counts == 0) {
            int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getCollectSkillTree());
            if (minLevel > 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
               sm.addInt(minLevel);
               player.sendPacket(sm);
            } else {
               player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
            }
         } else {
            player.sendPacket(asl);
         }
      } else if (!npc.getTemplate().canTeach(playerClass)) {
         npc.showNoTeachHtml(player);
      } else if (((Folk)npc).getClassesToTeach().isEmpty()) {
         NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
         String sb = "<html><body>I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:"
            + npcId
            + ", Your playerClass:"
            + player.getPlayerClass().getId()
            + "</body></html>";
         html.setHtml(sb);
         player.sendPacket(html);
      } else {
         Collection<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSkills(player, playerClass, false, false);
         AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.CLASS);
         int count = 0;
         player.setLearningClass(playerClass);

         for (SkillLearn sx : skills) {
            if (SkillData.getInstance().getSkill(sx.getSkillId(), sx.getSkillLevel()) != null) {
               asl.addSkill(sx.getSkillId(), sx.getSkillLevel(), sx.getSkillLevel(), sx.getCalculatedLevelUpSp(player.getPlayerClass(), playerClass), 0);
               count++;
            }
         }

         if (count == 0) {
            Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass);
            int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, skillTree);
            if (minLevel > 0) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
               sm.addInt(minLevel);
               player.sendPacket(sm);
            } else if (player.getPlayerClass().level() == 1) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN_PLEASE_COME_BACK_AFTER_S1ND_CLASS_CHANGE);
               sm.addInt(2);
               player.sendPacket(sm);
            } else {
               player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
            }
         } else {
            player.sendPacket(asl);
         }
      }
   }
}
