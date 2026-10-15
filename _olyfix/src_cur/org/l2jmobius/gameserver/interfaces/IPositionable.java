package org.l2jmobius.gameserver.interfaces;

import org.l2jmobius.gameserver.entity.Location;

public interface IPositionable extends ILocational {
   void setXYZ(int var1, int var2, int var3);

   void setXYZ(ILocational var1);

   void setHeading(int var1);

   void setInstanceId(int var1);

   void setLocation(Location var1);
}
