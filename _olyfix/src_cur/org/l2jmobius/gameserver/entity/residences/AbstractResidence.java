package org.l2jmobius.gameserver.entity.residences;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceZone;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;

public abstract class AbstractResidence extends ListenersContainer {
   private final int _residenceId;
   private String _name;
   private ResidenceZone _zone = null;
   private final List<SkillHolder> _residentialSkills = new ArrayList<>();

   public AbstractResidence(int residenceId) {
      this._residenceId = residenceId;
      this.initResidentialSkills();
   }

   protected abstract void load();

   protected abstract void initResidenceZone();

   protected void initResidentialSkills() {
      for (SkillLearn s : SkillTreeData.getInstance().getAvailableResidentialSkills(this.getResidenceId())) {
         this._residentialSkills.add(new SkillHolder(s.getSkillId(), s.getSkillLevel()));
      }
   }

   public int getResidenceId() {
      return this._residenceId;
   }

   public String getName() {
      return this._name;
   }

   public void setName(String name) {
      this._name = name;
   }

   public ResidenceZone getResidenceZone() {
      return this._zone;
   }

   protected void setResidenceZone(ResidenceZone zone) {
      this._zone = zone;
   }

   public List<SkillHolder> getResidentialSkills() {
      return this._residentialSkills;
   }

   public void giveResidentialSkills(Player player) {
      if (!this._residentialSkills.isEmpty()) {
         for (SkillHolder sh : this._residentialSkills) {
            player.addSkill(sh.getSkill(), false);
         }
      }
   }

   public void removeResidentialSkills(Player player) {
      if (!this._residentialSkills.isEmpty()) {
         for (SkillHolder sh : this._residentialSkills) {
            player.removeSkill(sh.getSkill(), false);
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof AbstractResidence && ((AbstractResidence)obj).getResidenceId() == this.getResidenceId();
   }

   @Override
   public String toString() {
      return this._name + "(" + this._residenceId + ")";
   }
}
