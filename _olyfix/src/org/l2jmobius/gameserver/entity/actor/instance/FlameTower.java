package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Tower;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;

public class FlameTower extends Tower {
   private int _upgradeLevel = 0;
   private List<Integer> _zoneList;

   public FlameTower(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FlameTower);
   }

   @Override
   public boolean doDie(Creature killer) {
      this.enableZones(false);
      return super.doDie(killer);
   }

   @Override
   public boolean deleteMe() {
      this.enableZones(false);
      return super.deleteMe();
   }

   public void enableZones(boolean value) {
      if (this._zoneList != null && this._upgradeLevel != 0) {
         int maxIndex = this._upgradeLevel * 2;

         for (int i = 0; i < maxIndex; i++) {
            ZoneType zone = ZoneManager.getInstance().getZoneById(this._zoneList.get(i));
            if (zone != null) {
               zone.setEnabled(value);
            }
         }
      }
   }

   public void setUpgradeLevel(int level) {
      this._upgradeLevel = level;
   }

   public void setZoneList(List<Integer> list) {
      this._zoneList = list;
      this.enableZones(true);
   }
}
