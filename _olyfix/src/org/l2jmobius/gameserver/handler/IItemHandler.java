package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public interface IItemHandler {
   Logger LOGGER = Logger.getLogger(IItemHandler.class.getName());

   boolean onItemUse(Playable var1, Item var2, boolean var3);
}
