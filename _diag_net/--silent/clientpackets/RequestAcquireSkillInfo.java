package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillInfo;

public class RequestAcquireSkillInfo extends ClientPacket {
   private int _id;
   private int _level;
   private AcquireSkillType _skillType;

   @Override
   protected void readImpl() {
      this._id = this.readInt();
      this._level = this.readInt();
      this._skillType = AcquireSkillType.getAcquireSkillType(this.readInt());
   }

   @Override
   protected void runImpl() {
      if (this._id > 0 && this._level > 0) {
         Player player = this.getPlayer();
         if (player != null) {
            Npc trainer = player.getLastFolkNPC();
            if (trainer != null && trainer.isNpc() && (trainer.canInteract(player) || player.isGM())) {
               Skill skill = SkillData.getInstance().getSkill(this._id, this._level);
               if (skill == null) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     RequestAcquireSkillInfo.class.getSimpleName()
                        + ": Skill Id: "
                        + this._id
                        + " level: "
                        + this._level
                        + " is undefined. "
                        + RequestAcquireSkillInfo.class.getName()
                        + " failed."
                  );
               } else {
                  SkillLearn s = SkillTreeData.getInstance().getSkillLearn(this._skillType, this._id, this._level, player);
                  if (s != null) {
                     switch (this._skillType) {
                        case TRANSFORM:
                        case FISHING:
                        case SUBCLASS:
                        case COLLECT:
                        case TRANSFER:
                           player.sendPacket(new AcquireSkillInfo(this._skillType, s));
                           break;
                        case CLASS:
                           if (trainer.getTemplate().canTeach(player.getLearningClass())) {
                              int customSp = s.getCalculatedLevelUpSp(player.getPlayerClass(), player.getLearningClass());
                              player.sendPacket(new AcquireSkillInfo(this._skillType, s, customSp));
                           }
                           break;
                        case PLEDGE:
                           if (!player.isClanLeader()) {
                              return;
                           }

                           player.sendPacket(new AcquireSkillInfo(this._skillType, s));
                           break;
                        case SUBPLEDGE:
                           if (!player.isClanLeader() || !player.hasAccess(ClanAccess.MEMBER_FAME)) {
                              return;
                           }

                           player.sendPacket(new AcquireSkillInfo(this._skillType, s));
                     }
                  }
               }
            }
         }
      } else {
         org.l2jmobius.gameserver.network.PacketLogger.warning(
            RequestAcquireSkillInfo.class.getSimpleName() + ": Invalid Id: " + this._id + " or level: " + this._level + "!"
         );
      }
   }
}
