package org.l2jmobius.gameserver.data.xml;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;

public class FakePlayerData {
   private static final Logger LOGGER = Logger.getLogger(FakePlayerData.class.getName());
   private final Map<String, String> _fakePlayerNames = new ConcurrentHashMap<>();
   private final Map<String, Integer> _fakePlayerIds = new ConcurrentHashMap<>();
   private final Set<String> _talkableFakePlayerNames = ConcurrentHashMap.newKeySet();

   protected FakePlayerData() {
   }

   public void report() {
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED) {
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._fakePlayerIds.size() + " templates.");
      } else {
         LOGGER.info(this.getClass().getSimpleName() + ": Disabled.");
      }
   }

   public void addFakePlayerId(String name, int npcId) {
      this._fakePlayerIds.put(name, npcId);
   }

   public int getNpcIdByName(String name) {
      return this._fakePlayerIds.get(name);
   }

   public void addFakePlayerName(String lowercaseName, String name) {
      this._fakePlayerNames.put(lowercaseName, name);
   }

   public String getProperName(String name) {
      return this._fakePlayerNames.get(name.toLowerCase());
   }

   public void addTalkableFakePlayerName(String lowercaseName) {
      this._talkableFakePlayerNames.add(lowercaseName);
   }

   public boolean isTalkable(String name) {
      return this._talkableFakePlayerNames.contains(name.toLowerCase());
   }

   public static FakePlayerData getInstance() {
      return FakePlayerData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FakePlayerData INSTANCE = new FakePlayerData();
   }
}
