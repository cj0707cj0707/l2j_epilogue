package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.holders.FakePlayerChatHolder;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class FakePlayerChatManager implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(FakePlayerChatManager.class.getName());
   private static final List<FakePlayerChatHolder> MESSAGES = new ArrayList<>();
   private static final int MIN_DELAY = 5000;
   private static final int MAX_DELAY = 15000;

   protected FakePlayerChatManager() {
      this.load();
   }

   @Override
   public void load() {
      if (FakePlayersConfig.FAKE_PLAYERS_ENABLED) {
         FakePlayerData.getInstance().report();
         if (FakePlayersConfig.FAKE_PLAYER_CHAT) {
            MESSAGES.clear();
            this.parseDatapackFile("data/FakePlayerChatData.xml");
            LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + MESSAGES.size() + " chat templates.");
         }
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "fakePlayerChat", fakePlayerChatNode -> {
         StatSet set = new StatSet(this.parseAttributes(fakePlayerChatNode));
         MESSAGES.add(new FakePlayerChatHolder(set.getString("fpcName"), set.getString("searchMethod"), set.getString("searchText"), set.getString("answers")));
      }));
   }

   public void manageChat(Player player, String fpcName, String message) {
      ThreadPool.schedule(() -> this.manageResponce(player, fpcName, message), Rnd.get(5000, 15000));
   }

   public void manageChat(Player player, String fpcName, String message, int minDelay, int maxDelay) {
      ThreadPool.schedule(() -> this.manageResponce(player, fpcName, message), Rnd.get(minDelay, maxDelay));
   }

   private void manageResponce(Player player, String fpcName, String message) {
      if (player != null) {
         String text = message.toLowerCase();
         if (text.contains("can you see me")) {
            Spawn spawn = SpawnTable.getInstance().getAnySpawn(FakePlayerData.getInstance().getNpcIdByName(fpcName));
            if (spawn != null) {
               Npc npc = spawn.getLastSpawn();
               if (npc != null) {
                  if (npc.calculateDistance2D(player) < 3000.0) {
                     if (GeoEngine.getInstance().canSeeTarget(npc, (WorldObject)player) && !player.isInvisible()) {
                        this.sendChat(player, fpcName, Rnd.nextBoolean() ? "i am not blind" : (Rnd.nextBoolean() ? "of course i can" : "yes"));
                     } else {
                        this.sendChat(
                           player, fpcName, Rnd.nextBoolean() ? "i know you are around" : (Rnd.nextBoolean() ? "not at the moment :P" : "no, where are you?")
                        );
                     }
                  } else {
                     this.sendChat(player, fpcName, Rnd.nextBoolean() ? "nope, can't see you" : (Rnd.nextBoolean() ? "nope" : "no"));
                  }

                  return;
               }
            }
         }

         for (FakePlayerChatHolder chatHolder : MESSAGES) {
            if (chatHolder.getFpcName().equals(fpcName) || chatHolder.getFpcName().equals("ALL")) {
               String var7 = chatHolder.getSearchMethod();
               switch (var7) {
                  case "EQUALS":
                     if (text.equals(chatHolder.getSearchText().get(0))) {
                        this.sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
                     }
                     break;
                  case "STARTS_WITH":
                     if (text.startsWith(chatHolder.getSearchText().get(0))) {
                        this.sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
                     }
                     break;
                  case "CONTAINS":
                     boolean allFound = true;

                     for (String word : chatHolder.getSearchText()) {
                        if (!text.contains(word)) {
                           allFound = false;
                        }
                     }

                     if (allFound) {
                        this.sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
                     }
               }
            }
         }
      }
   }

   public void sendChat(Player player, String fpcName, String message) {
      Spawn spawn = SpawnTable.getInstance().getAnySpawn(FakePlayerData.getInstance().getNpcIdByName(fpcName));
      if (spawn != null) {
         Npc npc = spawn.getLastSpawn();
         if (npc != null) {
            player.sendPacket(new CreatureSay(npc, ChatType.WHISPER, fpcName, message));
         }
      }
   }

   public static FakePlayerChatManager getInstance() {
      return FakePlayerChatManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FakePlayerChatManager INSTANCE = new FakePlayerChatManager();
   }
}
