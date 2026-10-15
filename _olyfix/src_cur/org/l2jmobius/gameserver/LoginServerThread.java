package org.l2jmobius.gameserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;
import org.l2jmobius.commons.util.HexUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.AuthResponse;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.ChangePasswordResponse;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.InitLS;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.KickPlayer;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.LoginServerFail;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.PlayerAuthResponse;
import org.l2jmobius.gameserver.network.loginserverpackets.receive.RequestCharacters;
import org.l2jmobius.gameserver.network.loginserverpackets.send.AuthRequest;
import org.l2jmobius.gameserver.network.loginserverpackets.send.BlowFishKey;
import org.l2jmobius.gameserver.network.loginserverpackets.send.ChangeAccessLevel;
import org.l2jmobius.gameserver.network.loginserverpackets.send.ChangePassword;
import org.l2jmobius.gameserver.network.loginserverpackets.send.PlayerAuthRequest;
import org.l2jmobius.gameserver.network.loginserverpackets.send.PlayerInGame;
import org.l2jmobius.gameserver.network.loginserverpackets.send.PlayerLogout;
import org.l2jmobius.gameserver.network.loginserverpackets.send.PlayerTracert;
import org.l2jmobius.gameserver.network.loginserverpackets.send.ReplyCharacters;
import org.l2jmobius.gameserver.network.loginserverpackets.send.SendMail;
import org.l2jmobius.gameserver.network.loginserverpackets.send.ServerStatus;
import org.l2jmobius.gameserver.network.loginserverpackets.send.TempBan;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.LoginFail;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class LoginServerThread extends Thread {
   protected static final Logger LOGGER = Logger.getLogger(LoginServerThread.class.getName());
   protected static final Logger ACCOUNTING_LOGGER = Logger.getLogger("accounting");
   private static final int REVISION = 262;
   private static final int RECONNECT_DELAY = 5000;
   private static final int BLOWFISH_KEY_SIZE = 40;
   private static final int HEX_ID_SIZE = 16;
   private static final int PACKET_PADDING = 8;
   private final String _hostname;
   private final int _port;
   private final int _gamePort;
   private final boolean _acceptAlternate;
   private final boolean _reserveHost;
   private final List<String> _subnets;
   private final List<String> _hosts;
   private Socket _socket;
   private OutputStream _out;
   private NewCrypt _blowfish;
   private byte[] _hexID;
   private int _requestID;
   private int _maxPlayer;
   private final Set<LoginServerThread.WaitingClient> _waitingClients = ConcurrentHashMap.newKeySet();
   private final Map<String, GameClient> _accountsInGameServer = new ConcurrentHashMap<>();
   private int _status;
   private String _serverName;

   protected LoginServerThread() {
      super("LoginServerThread");
      this._port = ServerConfig.GAME_SERVER_LOGIN_PORT;
      this._gamePort = ServerConfig.PORT_GAME;
      this._hostname = ServerConfig.GAME_SERVER_LOGIN_HOST;
      this._acceptAlternate = ServerConfig.ACCEPT_ALTERNATE_ID;
      this._reserveHost = ServerConfig.RESERVE_HOST_ON_LOGIN;
      this._subnets = ServerConfig.GAME_SERVER_SUBNETS;
      this._hosts = ServerConfig.GAME_SERVER_HOSTS;
      this._maxPlayer = ServerConfig.MAXIMUM_ONLINE_USERS;
      this._hexID = ServerConfig.HEX_ID;
      if (this._hexID == null) {
         this._requestID = ServerConfig.REQUEST_ID;
         this._hexID = HexUtil.generateHexBytes(16);
      } else {
         this._requestID = ServerConfig.SERVER_ID;
      }
   }

   @Override
   public void run() {
      while (!this.isInterrupted()) {
         int lengthHi = 0;
         int lengthLo = 0;
         int length = 0;
         boolean checksumOk = false;

         try {
            LOGGER.info(this.getClass().getSimpleName() + ": Connecting to login on " + this._hostname + ":" + this._port);
            this._socket = new Socket(this._hostname, this._port);
            InputStream in = this._socket.getInputStream();
            this._out = new BufferedOutputStream(this._socket.getOutputStream());
            byte[] blowfishKey = HexUtil.generateHexBytes(40);
            this._blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\u0000");

            while (!this.isInterrupted()) {
               lengthLo = in.read();
               lengthHi = in.read();
               length = lengthHi * 256 + lengthLo;
               if (lengthHi < 0) {
                  LOGGER.finer(this.getClass().getSimpleName() + ": Login terminated the connection.");
                  break;
               }

               byte[] incoming = new byte[length - 2];
               int receivedBytes = 0;
               int newBytes = 0;

               for (int left = length - 2; newBytes != -1 && receivedBytes < length - 2; left -= newBytes) {
                  newBytes = in.read(incoming, receivedBytes, left);
                  receivedBytes += newBytes;
               }

               if (receivedBytes != length - 2) {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Incomplete packet received, closing connection (LS)");
                  break;
               }

               this._blowfish.decrypt(incoming, 0, incoming.length);
               checksumOk = NewCrypt.verifyChecksum(incoming);
               if (!checksumOk) {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Incorrect packet checksum, ignoring packet (LS)");
                  break;
               }

               int packetType = incoming[0] & 255;
               switch (packetType) {
                  case 0:
                     InitLS init = new InitLS(incoming);
                     if (init.getRevision() != 262) {
                        LOGGER.warning("/!\\ Revision mismatch between LS and GS /!\\");
                     } else {
                        RSAPublicKey publicKey;
                        try {
                           KeyFactory kfac = KeyFactory.getInstance("RSA");
                           BigInteger modulus = new BigInteger(init.getRSAKey());
                           RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4);
                           publicKey = (RSAPublicKey)kfac.generatePublic(kspec1);
                        } catch (GeneralSecurityException var35) {
                           LOGGER.warning(this.getClass().getSimpleName() + ": Trouble initializing RSA public key from login server.");
                           continue;
                        }

                        this.sendPacket(new BlowFishKey(blowfishKey, publicKey));
                        this._blowfish = new NewCrypt(blowfishKey);
                        this.sendPacket(
                           new AuthRequest(
                              this._requestID,
                              this._acceptAlternate,
                              this._hexID,
                              this._gamePort,
                              this._reserveHost,
                              this._maxPlayer,
                              this._subnets,
                              this._hosts
                           )
                        );
                     }
                     break;
                  case 1:
                     LoginServerFail lsf = new LoginServerFail(incoming);
                     LOGGER.info(this.getClass().getSimpleName() + ": Registration failed: " + lsf.getReasonString());
                     break;
                  case 2:
                     AuthResponse aresp = new AuthResponse(incoming);
                     int serverID = aresp.getServerId();
                     this._serverName = aresp.getServerName();
                     ServerConfig.saveHexid(serverID, this.hexToString(this._hexID));
                     LOGGER.info(this.getClass().getSimpleName() + ": Registered on login as Server " + serverID + ": " + this._serverName);
                     ServerStatus st = new ServerStatus();
                     if (ServerConfig.SERVER_LIST_BRACKET) {
                        st.addAttribute(3, 1);
                     } else {
                        st.addAttribute(3, 0);
                     }

                     st.addAttribute(2, ServerConfig.SERVER_LIST_TYPE);
                     if (GeneralConfig.SERVER_GMONLY) {
                        st.addAttribute(1, 5);
                     } else {
                        st.addAttribute(1, 0);
                     }

                     if (ServerConfig.SERVER_LIST_AGE == 15) {
                        st.addAttribute(5, 15);
                     } else if (ServerConfig.SERVER_LIST_AGE == 18) {
                        st.addAttribute(5, 18);
                     } else {
                        st.addAttribute(5, 0);
                     }

                     this.sendPacket(st);
                     List<String> playerList = new ArrayList<>();

                     for (Player player : World.getPlayers()) {
                        if (!player.isInOfflineMode()) {
                           playerList.add(player.getAccountName());
                        }
                     }

                     if (!playerList.isEmpty()) {
                        this.sendPacket(new PlayerInGame(playerList));
                     }
                     break;
                  case 3:
                     PlayerAuthResponse par = new PlayerAuthResponse(incoming);
                     String account = par.getAccount();
                     LoginServerThread.WaitingClient wcToRemove = null;
                     synchronized (this._waitingClients) {
                        for (LoginServerThread.WaitingClient wc : this._waitingClients) {
                           if (wc.account.equals(account)) {
                              wcToRemove = wc;
                              break;
                           }
                        }
                     }

                     if (wcToRemove != null) {
                        if (par.isAuthed()) {
                           PlayerInGame pig = new PlayerInGame(par.getAccount());
                           this.sendPacket(pig);
                           wcToRemove.gameClient.setConnectionState(ConnectionState.AUTHENTICATED);
                           wcToRemove.gameClient.setSessionId(wcToRemove.sessionKey);
                           CharSelectionInfo cl = new CharSelectionInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().playOkID1);
                           wcToRemove.gameClient.sendPacket(cl);
                           wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
                        } else {
                           LOGGER.warning(this.getClass().getSimpleName() + ": Session key incorrect. Closing connection for account " + wcToRemove.account);
                           wcToRemove.gameClient.close(new LoginFail(1));
                           this.sendLogout(wcToRemove.account);
                        }

                        this._waitingClients.remove(wcToRemove);
                     }
                     break;
                  case 4:
                     KickPlayer kp = new KickPlayer(incoming);
                     this.doKickPlayer(kp.getAccount());
                     break;
                  case 5:
                     RequestCharacters rc = new RequestCharacters(incoming);
                     this.getCharsOnServer(rc.getAccount());
                     break;
                  case 6:
                     new ChangePasswordResponse(incoming);
               }
            }
         } catch (UnknownHostException var37) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Unknown host: ", (Throwable)var37);
         } catch (SocketException var38) {
            LOGGER.warning(this.getClass().getSimpleName() + ": LoginServer not available, trying to reconnect...");
         } catch (IOException var39) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Disconnected from Login, trying to reconnect: ", (Throwable)var39);
         } finally {
            try {
               this._socket.close();
               if (this.isInterrupted()) {
                  return;
               }
            } catch (Exception var33) {
            }
         }

         try {
            Thread.sleep(5000L);
         } catch (InterruptedException var34) {
            Thread.currentThread().interrupt();
            break;
         }
      }
   }

   public void addWaitingClientAndSendRequest(String accountName, GameClient client, LoginServerThread.SessionKey key) {
      synchronized (this._waitingClients) {
         this._waitingClients.add(new LoginServerThread.WaitingClient(accountName, client, key));
      }

      this.sendPacket(new PlayerAuthRequest(accountName, key));
   }

   public void removeWaitingClient(GameClient client) {
      LoginServerThread.WaitingClient toRemove = null;
      synchronized (this._waitingClients) {
         for (LoginServerThread.WaitingClient c : this._waitingClients) {
            if (c.gameClient == client) {
               toRemove = c;
               break;
            }
         }

         if (toRemove != null) {
            this._waitingClients.remove(toRemove);
         }
      }
   }

   public void sendLogout(String account) {
      if (account != null) {
         GameClient removed = this._accountsInGameServer.remove(account);
         if (removed != null) {
            removed.disconnect();
         }

         this.sendPacket(new PlayerLogout(account));
      }
   }

   public boolean addGameServerLogin(String account, GameClient client) {
      return this._accountsInGameServer.putIfAbsent(account, client) == null;
   }

   public void sendAccessLevel(String account, int level) {
      this.sendPacket(new ChangeAccessLevel(account, level));
   }

   public void sendClientTracert(String account, String[] address) {
      this.sendPacket(new PlayerTracert(account, address[0], address[1], address[2], address[3], address[4]));
   }

   public void sendMail(String account, String mailId, String... args) {
      this.sendPacket(new SendMail(account, mailId, args));
   }

   public void sendTempBan(String account, String ip, long time) {
      this.sendPacket(new TempBan(account, ip, time));
   }

   private String hexToString(byte[] hex) {
      return new BigInteger(hex).toString(16);
   }

   private void doKickPlayer(String account) {
      GameClient client = this._accountsInGameServer.get(account);
      if (client != null) {
         SystemMessage msg = new SystemMessage(SystemMessageId.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT);
         if (client.isDetached()) {
            Player player = client.getPlayer();
            if (player != null) {
               player.storeMe();
               player.deleteMe();
            }

            client.close(msg);
         } else {
            Disconnection.of(client).storeAndDeleteWith(msg);
            ACCOUNTING_LOGGER.info("Kicked by login, " + client);
         }
      }

      this.sendLogout(account);
   }

   private void getCharsOnServer(String account) {
      int chars = 0;
      List<Long> charToDel = new ArrayList<>();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT deletetime FROM characters WHERE account_name=?");
      ) {
         ps.setString(1, account);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               chars++;
               long delTime = rs.getLong("deletetime");
               if (delTime != 0L) {
                  charToDel.add(delTime);
               }
            }
         }
      } catch (SQLException var15) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Exception in getCharsOnServer: " + var15.getMessage(), (Throwable)var15);
      }

      this.sendPacket(new ReplyCharacters(account, chars, charToDel));
   }

   private void sendPacket(SimpleWritablePacket packet) {
      if (this._blowfish != null && this._socket != null && !this._socket.isClosed()) {
         try {
            packet.write();
            packet.writeInt(0);
            int size = packet.getLength() - 2;
            int padding = size % 8;
            if (padding != 0) {
               for (int i = padding; i < 8; i++) {
                  packet.writeByte(0);
               }
            }

            byte[] data = packet.getSendableBytes();
            size = data.length - 2;
            synchronized (this._out) {
               NewCrypt.appendChecksum(data, 2, size);
               this._blowfish.crypt(data, 2, size);
               this._out.write(data);

               try {
                  this._out.flush();
               } catch (IOException var8) {
               }
            }
         } catch (Exception var10) {
            LOGGER.severe("LoginServerThread: IOException while sending packet " + packet.getClass().getSimpleName());
            LOGGER.severe(TraceUtil.getStackTrace(var10));
         }
      }
   }

   public void setMaxPlayer(int maxPlayer) {
      this.sendServerStatus(4, maxPlayer);
      this._maxPlayer = maxPlayer;
   }

   public int getMaxPlayer() {
      return this._maxPlayer;
   }

   public void sendServerStatus(int id, int value) {
      ServerStatus serverStatus = new ServerStatus();
      serverStatus.addAttribute(id, value);
      this.sendPacket(serverStatus);
   }

   public void sendServerType() {
      ServerStatus serverStatus = new ServerStatus();
      serverStatus.addAttribute(2, ServerConfig.SERVER_LIST_TYPE);
      this.sendPacket(serverStatus);
   }

   public void sendChangePassword(String accountName, String charName, String oldpass, String newpass) {
      this.sendPacket(new ChangePassword(accountName, charName, oldpass, newpass));
   }

   public int getServerStatus() {
      return this._status;
   }

   public String getStatusString() {
      return ServerStatus.STATUS_STRING[this._status];
   }

   public String getServerName() {
      return this._serverName;
   }

   public void setServerStatus(int status) {
      switch (status) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
            this.sendServerStatus(1, status);
            this._status = status;
            return;
         default:
            throw new IllegalArgumentException("Invalid server status: " + status);
      }
   }

   public GameClient getClient(String name) {
      return name != null ? this._accountsInGameServer.get(name) : null;
   }

   public static LoginServerThread getInstance() {
      return LoginServerThread.SingletonHolder.INSTANCE;
   }

   public static class SessionKey {
      public int playOkID1;
      public int playOkID2;
      public int loginOkID1;
      public int loginOkID2;

      public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2) {
         this.playOkID1 = playOK1;
         this.playOkID2 = playOK2;
         this.loginOkID1 = loginOK1;
         this.loginOkID2 = loginOK2;
      }

      @Override
      public String toString() {
         return "PlayOk: " + this.playOkID1 + " " + this.playOkID2 + " LoginOk:" + this.loginOkID1 + " " + this.loginOkID2;
      }
   }

   private static class SingletonHolder {
      protected static final LoginServerThread INSTANCE = new LoginServerThread();
   }

   private static class WaitingClient {
      public String account;
      public GameClient gameClient;
      public LoginServerThread.SessionKey sessionKey;

      public WaitingClient(String acc, GameClient client, LoginServerThread.SessionKey key) {
         this.account = acc;
         this.gameClient = client;
         this.sessionKey = key;
      }
   }
}
