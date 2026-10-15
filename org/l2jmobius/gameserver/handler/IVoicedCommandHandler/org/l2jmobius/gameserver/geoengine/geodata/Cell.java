package org.l2jmobius.gameserver.geoengine.geodata;

public class Cell {
   public static final byte NSWE_EAST = 1;
   public static final byte NSWE_WEST = 2;
   public static final byte NSWE_SOUTH = 4;
   public static final byte NSWE_NORTH = 8;
   public static final byte NSWE_NORTH_EAST = 9;
   public static final byte NSWE_NORTH_WEST = 10;
   public static final byte NSWE_SOUTH_EAST = 5;
   public static final byte NSWE_SOUTH_WEST = 6;
   public static final byte NSWE_ALL = 15;

   private Cell() {
   }
}
