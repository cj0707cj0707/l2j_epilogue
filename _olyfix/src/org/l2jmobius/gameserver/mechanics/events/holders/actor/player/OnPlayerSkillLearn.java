package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;

public class OnPlayerSkillLearn implements IBaseEvent {
   private final Npc _trainer;
   private final Player _player;
   private final Skill _skill;
   private final AcquireSkillType _type;

   public OnPlayerSkillLearn(Npc trainer, Player player, Skill skill, AcquireSkillType type) {
      this._trainer = trainer;
      this._player = player;
      this._skill = skill;
      this._type = type;
   }

   public Npc getTrainer() {
      return this._trainer;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public AcquireSkillType getAcquireType() {
      return this._type;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_SKILL_LEARN;
   }
}
