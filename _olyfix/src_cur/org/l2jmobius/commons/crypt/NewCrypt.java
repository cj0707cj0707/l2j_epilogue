package org.l2jmobius.commons.crypt;

import java.nio.charset.StandardCharsets;

public class NewCrypt {
   private static final int BYTES_PER_BLOCK = 4;
   private static final int CHECKSUM_SIZE = 4;
   private static final int BLOWFISH_BLOCK_SIZE = 8;
   private static final int XOR_KEY_OFFSET = 4;
   private static final int XOR_FINAL_OFFSET = 8;
   private static final int BYTE_MASK = 255;
   private static final int SHIFT_8_BITS = 8;
   private static final int SHIFT_16_BITS = 16;
   private static final int SHIFT_24_BITS = 24;
   private static final long MASK_16_BITS = 65280L;
   private static final long MASK_24_BITS = 16711680L;
   private static final long MASK_32_BITS = -16777216L;
   private final BlowfishEngine _blowfishCipher = new BlowfishEngine();

   public NewCrypt(byte[] blowfishKey) {
      this._blowfishCipher.init(blowfishKey);
   }

   public NewCrypt(String key) {
      this(key.getBytes(StandardCharsets.UTF_8));
   }

   public static boolean verifyChecksum(byte[] rawData) {
      return verifyChecksum(rawData, 0, rawData.length);
   }

   public static boolean verifyChecksum(byte[] rawData, int offset, int size) {
      if ((size & 3) == 0 && size > 4) {
         long calculatedChecksum = 0L;
         int dataLength = size - 4;
         long currentBlock = -1L;

         int position;
         for (position = offset; position < dataLength; position += 4) {
            currentBlock = rawData[position] & 255;
            currentBlock |= rawData[position + 1] << 8 & 65280L;
            currentBlock |= rawData[position + 2] << 16 & 16711680L;
            currentBlock |= rawData[position + 3] << 24 & -16777216L;
            calculatedChecksum ^= currentBlock;
         }

         currentBlock = rawData[position] & 255;
         currentBlock |= rawData[position + 1] << 8 & 65280L;
         currentBlock |= rawData[position + 2] << 16 & 16711680L;
         currentBlock |= rawData[position + 3] << 24 & -16777216L;
         return currentBlock == calculatedChecksum;
      } else {
         return false;
      }
   }

   public static void appendChecksum(byte[] rawData) {
      appendChecksum(rawData, 0, rawData.length);
   }

   public static void appendChecksum(byte[] rawData, int offset, int size) {
      long calculatedChecksum = 0L;
      int dataLength = size - 4;

      int position;
      for (position = offset; position < dataLength; position += 4) {
         long currentBlock = rawData[position] & 255;
         currentBlock |= rawData[position + 1] << 8 & 65280L;
         currentBlock |= rawData[position + 2] << 16 & 16711680L;
         currentBlock |= rawData[position + 3] << 24 & -16777216L;
         calculatedChecksum ^= currentBlock;
      }

      long currentBlock = rawData[position] & 255;
      currentBlock |= rawData[position + 1] << 8 & 65280L;
      currentBlock |= rawData[position + 2] << 16 & 16711680L;
      currentBlock |= rawData[position + 3] << 24 & -16777216L;
      rawData[position] = (byte)(calculatedChecksum & 255L);
      rawData[position + 1] = (byte)(calculatedChecksum >> 8 & 255L);
      rawData[position + 2] = (byte)(calculatedChecksum >> 16 & 255L);
      rawData[position + 3] = (byte)(calculatedChecksum >> 24 & 255L);
   }

   public static void encXORPass(byte[] rawData, int xorKey) {
      encXORPass(rawData, 0, rawData.length, xorKey);
   }

   public static void encXORPass(byte[] rawData, int offset, int size, int xorKey) {
      int endPosition = size - 8;
      int currentPosition = 4 + offset;
      int encryptionKey = xorKey;

      while (currentPosition < endPosition) {
         int dataBlock = rawData[currentPosition] & 255;
         dataBlock |= (rawData[currentPosition + 1] & 255) << 8;
         dataBlock |= (rawData[currentPosition + 2] & 255) << 16;
         dataBlock |= (rawData[currentPosition + 3] & 255) << 24;
         encryptionKey += dataBlock;
         dataBlock ^= encryptionKey;
         rawData[currentPosition++] = (byte)(dataBlock & 0xFF);
         rawData[currentPosition++] = (byte)(dataBlock >> 8 & 0xFF);
         rawData[currentPosition++] = (byte)(dataBlock >> 16 & 0xFF);
         rawData[currentPosition++] = (byte)(dataBlock >> 24 & 0xFF);
      }

      rawData[currentPosition++] = (byte)(encryptionKey & 0xFF);
      rawData[currentPosition++] = (byte)(encryptionKey >> 8 & 0xFF);
      rawData[currentPosition++] = (byte)(encryptionKey >> 16 & 0xFF);
      rawData[currentPosition++] = (byte)(encryptionKey >> 24 & 0xFF);
   }

   public void decrypt(byte[] rawData, int offset, int size) {
      for (int i = offset; i < offset + size; i += 8) {
         this._blowfishCipher.decryptBlock(rawData, i);
      }
   }

   public void crypt(byte[] rawData, int offset, int size) {
      for (int i = offset; i < offset + size; i += 8) {
         this._blowfishCipher.encryptBlock(rawData, i);
      }
   }
}
