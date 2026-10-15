package org.l2jmobius.gameserver.network.loginserverpackets.send;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public final class BlowFishKey extends SimpleWritablePacket {
   private static final Logger LOGGER = Logger.getLogger(BlowFishKey.class.getName());
   private static final int PACKET_OPCODE = 0;
   private static final String RSA_ECB_NOPADDING = "RSA/ECB/nopadding";
   private static final String LOG_PREFIX = "BlowFishKey: ";
   private static final int EXPECTED_MIN_BF_LEN = 16;
   private static final int EXPECTED_MAX_BF_LEN = 64;

   public BlowFishKey(byte[] blowfishKey, RSAPublicKey publicKey) {
      if (blowfishKey == null || blowfishKey.length == 0) {
         throw new IllegalArgumentException("blowfishKey must not be null/empty");
      } else if (publicKey == null) {
         throw new IllegalArgumentException("publicKey must not be null");
      } else {
         if (blowfishKey.length < 16 || blowfishKey.length > 64) {
            LOGGER.warning("BlowFishKey: Unexpected Blowfish key length: " + blowfishKey.length + " bytes.");
         }

         try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(1, publicKey);
            byte[] encryptedKey = rsaCipher.doFinal(blowfishKey);
            this.writeByte(0);
            this.writeInt(encryptedKey.length);
            this.writeBytes(encryptedKey);
         } catch (GeneralSecurityException var5) {
            LOGGER.log(Level.SEVERE, "BlowFishKey: RSA encryption failed.", (Throwable)var5);
         }
      }
   }
}
