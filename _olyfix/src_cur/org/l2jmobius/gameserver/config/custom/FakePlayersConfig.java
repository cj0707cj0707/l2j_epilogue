package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class FakePlayersConfig {
   private static final String FAKE_PLAYERS_CONFIG_FILE = "./config/Custom/FakePlayers.ini";
   public static boolean FAKE_PLAYERS_ENABLED;
   public static boolean FAKE_PLAYER_CHAT;
   public static boolean FAKE_PLAYER_USE_SHOTS;
   public static boolean FAKE_PLAYER_KILL_PVP;
   public static boolean FAKE_PLAYER_KILL_KARMA;
   public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
   public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
   public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
   public static boolean FAKE_PLAYER_AGGRO_FPC;
   public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
   public static boolean FAKE_PLAYER_CAN_PICKUP;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/FakePlayers.ini");
      FAKE_PLAYERS_ENABLED = config.getBoolean("EnableFakePlayers", false);
      FAKE_PLAYER_CHAT = config.getBoolean("FakePlayerChat", false);
      FAKE_PLAYER_USE_SHOTS = config.getBoolean("FakePlayerUseShots", false);
      FAKE_PLAYER_KILL_PVP = config.getBoolean("FakePlayerKillsRewardPvP", false);
      FAKE_PLAYER_KILL_KARMA = config.getBoolean("FakePlayerUnflaggedKillsKarma", false);
      FAKE_PLAYER_AUTO_ATTACKABLE = config.getBoolean("FakePlayerAutoAttackable", false);
      FAKE_PLAYER_AGGRO_MONSTERS = config.getBoolean("FakePlayerAggroMonsters", false);
      FAKE_PLAYER_AGGRO_PLAYERS = config.getBoolean("FakePlayerAggroPlayers", false);
      FAKE_PLAYER_AGGRO_FPC = config.getBoolean("FakePlayerAggroFPC", false);
      FAKE_PLAYER_CAN_DROP_ITEMS = config.getBoolean("FakePlayerCanDropItems", false);
      FAKE_PLAYER_CAN_PICKUP = config.getBoolean("FakePlayerCanPickup", false);
   }
}
