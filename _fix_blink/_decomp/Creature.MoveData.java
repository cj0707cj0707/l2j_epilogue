package org.l2jmobius.gameserver.entity.actor;

import java.util.List;
import org.l2jmobius.gameserver.geoengine.pathfinding.GeoLocation;

public class Creature$MoveData {
   public int moveStartTime;
   public int moveTimestamp;
   public int xDestination;
   public int yDestination;
   public int zDestination;
   public double xAccurate;
   public double yAccurate;
   public double zAccurate;
   public int heading;
   public boolean disregardingGeodata;
   public int onGeodataPathIndex;
   public List<GeoLocation> geoPath;
   public int geoPathAccurateTx;
   public int geoPathAccurateTy;
   public int geoPathGtx;
   public int geoPathGty;
   public int lastBroadcastTime;
}
