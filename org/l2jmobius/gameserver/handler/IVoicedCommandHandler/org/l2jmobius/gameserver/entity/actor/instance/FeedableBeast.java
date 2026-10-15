package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class FeedableBeast extends Monster {
   public FeedableBeast(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FeedableBeast);
   }
}
