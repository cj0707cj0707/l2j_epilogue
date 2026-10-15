package org.l2jmobius.gameserver.network;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;
import org.l2jmobius.gameserver.network.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.FloodProtectors;

public class GameClient extends Client<Connection<GameClient>> {
   private static final Logger LOGGER = Logger.getLogger(GameClient.class.getName());
   private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private final FloodProtectors _floodProtectors = new FloodProtectors(this);
   private final ReentrantLock _playerLock = new ReentrantLock();
   private ConnectionState _connectionState = ConnectionState.CONNECTED;
   private Encryption _encryption = null;
   private String _ip = "N/A";
   private String _accountName;
   private LoginServerThread.SessionKey _sessionKey;
   private Player _player;
   private ClientHardwareInfoHolder _hardwareInfo;
   private List<CharacterInfoHolder> _charSlotMapping = null;
   private volatile boolean _isDetached = false;
   private boolean _isAuthedGG;
   private boolean _protocolOk;
   private int _protocolVersion;
   private int[][] _trace;

   public GameClient(Connection<GameClient> connection) {
      super(connection);
      this._ip = connection.getRemoteAddress();
   }

   @Override
   public void onConnected() {
      LOGGER_ACCOUNTING.finer("Client connected: " + this._ip);
   }

   @Override
   public void onDisconnection() {
      LOGGER_ACCOUNTING.finer("Client disconnected: " + this);
      LoginServerThread.getInstance().sendLogout(this._accountName);
      if (this._player == null || !this._player.isInOfflineMode()) {
         Disconnection.of(this).onDisconnection();
      }

      this._connectionState = ConnectionState.DISCONNECTED;
   }

   @Override
   public boolean encrypt(WriteBuffer data, int offset, int size) {
      if (ServerConfig.PACKET_ENCRYPTION && this._encryption != null) {
         this._encryption.encrypt(data, offset, size);
      }

      return true;
   }

   @Override
   public boolean decrypt(ReadBuffer data, int offset, int size) {
      if (ServerConfig.PACKET_ENCRYPTION && this._encryption != null) {
         this._encryption.decrypt(data, offset, size);
      }

      return true;
   }

   public void closeNow() {
      this.disconnect();
   }

   public void close(ServerPacket packet) {
      if (packet == null) {
         this.closeNow();
      } else {
         this.sendPacket(packet);
         ThreadPool.schedule(this::closeNow, 1000L);
      }
   }

   public byte[] enableCrypt() {
      byte[] key = BlowFishKeygen.getRandomKey();
      if (ServerConfig.PACKET_ENCRYPTION) {
         this._encryption = new Encryption();
         this._encryption.setKey(key);
      }

      return key;
   }

   public Player getPlayer() {
      return this._player;
   }

   public void setPlayer(Player player) {
      this._player = player;
   }

   public ReentrantLock getPlayerLock() {
      return this._playerLock;
   }

   public FloodProtectors getFloodProtectors() {
      return this._floodProtectors;
   }

   public void setGameGuardOk(boolean value) {
      this._isAuthedGG = value;
   }

   public boolean isAuthedGG() {
      return this._isAuthedGG;
   }

   public String getIp() {
      return this._ip;
   }

   public void setAccountName(String accountName) {
      this._accountName = accountName;
   }

   public String getAccountName() {
      return this._accountName;
   }

   public void setSessionId(LoginServerThread.SessionKey sessionKey) {
      this._sessionKey = sessionKey;
   }

   public LoginServerThread.SessionKey getSessionId() {
      return this._sessionKey;
   }

   public void sendPacket(ServerPacket packet) {
      if (packet == null) {
         LOGGER.warning(TraceUtil.getStackTrace(new Exception()));
      } else {
         this.writePacket(packet);
         packet.runImpl(this._player);
      }
   }

   public void sendPacket(SystemMessageId systemMessageId) {
      this.sendPacket(new SystemMessage(systemMessageId));
   }

   public boolean isDetached() {
      return this._isDetached;
   }

   public void setDetached(boolean value) {
      this._isDetached = value;
   }

   public byte markToDeleteChar(int characterSlot) {
      int objectId = this.getObjectIdForSlot(characterSlot);
      if (objectId < 0) {
         return -1;
      } else {
         try {
            byte var20;
            try (
               java.sql.Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("SELECT clanId FROM characters WHERE charId=?");
            ) {
               statement.setInt(1, objectId);
               byte answer = 0;

               try (ResultSet rs = statement.executeQuery()) {
                  int clanId = rs.next() ? rs.getInt(1) : 0;
                  if (clanId != 0) {
                     Clan clan = ClanTable.getInstance().getClan(clanId);
                     if (clan == null) {
                        answer = 0;
                     } else if (clan.getLeaderId() == objectId) {
                        answer = 2;
                     } else {
                        answer = 1;
                     }
                  }

                  if (answer == 0) {
                     if (PlayerConfig.DELETE_DAYS == 0) {
                        deleteCharByObjId(objectId);
                     } else {
                        try (PreparedStatement ps2 = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?")) {
                           ps2.setLong(1, System.currentTimeMillis() + PlayerConfig.DELETE_DAYS * 86400000);
                           ps2.setInt(2, objectId);
                           ps2.execute();
                        }
                     }

                     LOGGER_ACCOUNTING.info("Delete, " + objectId + ", " + this);
                  }
               }

               var20 = answer;
            }

            return var20;
         } catch (Exception var19) {
            LOGGER.log(Level.SEVERE, "Error updating delete time of character.", (Throwable)var19);
            return -1;
         }
      }
   }

   public void restore(int characterSlot) {
      int objectId = this.getObjectIdForSlot(characterSlot);
      if (objectId >= 0) {
         try (
            java.sql.Connection con = DatabaseFactory.getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?");
         ) {
            statement.setInt(1, objectId);
            statement.execute();
         } catch (Exception var11) {
            LOGGER.log(Level.SEVERE, "Error restoring character.", (Throwable)var11);
         }

         LOGGER_ACCOUNTING.info("Restore, " + objectId + ", " + this);
      }
   }

   public static void deleteCharByObjId(int objectId) {
      if (objectId >= 0) {
         CharInfoTable.getInstance().removeName(objectId);

         try (java.sql.Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_contacts WHERE charId=? OR contactId=?")) {
               ps.setInt(1, objectId);
               ps.setInt(2, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?")) {
               ps.setInt(1, objectId);
               ps.setInt(2, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_quests WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM heroes WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_raid_points WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_recommends WHERE charId=? OR target_id=?")) {
               ps.setInt(1, objectId);
               ps.setInt(2, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE charId=?")) {
               ps.setInt(1, objectId);
               ps.execute();
            }

            if (WeddingConfig.ALLOW_WEDDING) {
               try (PreparedStatement ps = con.prepareStatement("DELETE FROM mods_wedding WHERE player1Id = ? OR player2Id = ?")) {
                  ps.setInt(1, objectId);
                  ps.setInt(2, objectId);
                  ps.execute();
               }
            }
         } catch (Exception var53) {
            LOGGER.log(Level.SEVERE, "Error deleting character.", (Throwable)var53);
         }
      }
   }

   public Player load(int characterSlot) {
      int objectId = this.getObjectIdForSlot(characterSlot);
      if (objectId < 0) {
         return null;
      } else {
         Player player = World.getPlayer(objectId);
         if (player != null) {
            if (player.isOnlineInt() == 1) {
               LOGGER.severe("Attempt of double login: " + player.getName() + "(" + objectId + ") " + this._accountName);
            }

            if (player.getClient() != null) {
               Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
            } else {
               player.storeMe();
               player.deleteMe();
            }

            return null;
         } else {
            player = Player.load(objectId);
            if (player != null) {
               player.setRunning();
               player.standUp();
               player.refreshOverloaded();
               player.refreshExpertisePenalty();
            } else {
               LOGGER.severe("Could not restore in slot: " + characterSlot);
            }

            return player;
         }
      }
   }

   public void setCharSelection(List<CharacterInfoHolder> characters) {
      this._charSlotMapping = characters;
   }

   public CharacterInfoHolder getCharSelection(int charslot) {
      return this._charSlotMapping != null && charslot >= 0 && charslot < this._charSlotMapping.size() ? this._charSlotMapping.get(charslot) : null;
   }

   private int getObjectIdForSlot(int characterSlot) {
      CharacterInfoHolder info = this.getCharSelection(characterSlot);
      if (info == null) {
         LOGGER.warning(this.toString() + " tried to delete Character in slot " + characterSlot + " but no characters exits at that slot.");
         return -1;
      } else {
         return info.getObjectId();
      }
   }

   public void setConnectionState(ConnectionState connectionState) {
      this._connectionState = connectionState;
   }

   public ConnectionState getConnectionState() {
      return this._connectionState;
   }

   public void setProtocolVersion(int version) {
      this._protocolVersion = version;
   }

   public int getProtocolVersion() {
      return this._protocolVersion;
   }

   public boolean isProtocolOk() {
      return this._protocolOk;
   }

   public void setProtocolOk(boolean value) {
      this._protocolOk = value;
   }

   public void setClientTracert(int[][] tracert) {
      this._trace = tracert;
   }

   public int[][] getTrace() {
      return this._trace;
   }

   public Encryption getEncryption() {
      return this._encryption;
   }

   public ClientHardwareInfoHolder getHardwareInfo() {
      return this._hardwareInfo;
   }

   public void setHardwareInfo(ClientHardwareInfoHolder hardwareInfo) {
      this._hardwareInfo = hardwareInfo;
   }

   @Override
   public String toString() {
      try {
         String ip = this.getIp();
         ConnectionState state = this.getConnectionState();
         switch (state) {
            case DISCONNECTED:
               if (this._accountName != null) {
                  return "[Account: " + this._accountName + " - IP: " + (ip == null ? "disconnected" : ip) + "]";
               }

               return "[IP: " + (ip == null ? "disconnected" : ip) + "]";
            case CONNECTED:
               return "[IP: " + (ip == null ? "disconnected" : ip) + "]";
            case AUTHENTICATED:
               return "[Account: " + this._accountName + " - IP: " + (ip == null ? "disconnected" : ip) + "]";
            case ENTERING:
            case IN_GAME:
               return "[Character: "
                  + (this._player == null ? "disconnected" : this._player.getName() + "[" + this._player.getObjectId() + "]")
                  + " - Account: "
                  + this._accountName
                  + " - IP: "
                  + (ip == null ? "disconnected" : ip)
                  + "]";
            default:
               throw new IllegalStateException("Missing state on switch.");
         }
      } catch (NullPointerException var3) {
         return "[Character read failed due to disconnect]";
      }
   }
}
