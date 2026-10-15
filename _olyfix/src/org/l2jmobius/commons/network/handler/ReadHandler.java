package org.l2jmobius.commons.network.handler;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.packet.PacketExecutor;
import org.l2jmobius.commons.network.packet.PacketHandler;
import org.l2jmobius.commons.network.packet.ReadablePacket;

public class ReadHandler<T extends Client<Connection<T>>> implements CompletionHandler<Integer, T> {
   private static final int MAX_PACKET_SIZE = 65533;
   private final PacketHandler<T> _packetHandler;
   private final PacketExecutor<T> _executor;

   public ReadHandler(PacketHandler<T> packetHandler, PacketExecutor<T> executor) {
      this._packetHandler = packetHandler;
      this._executor = executor;
   }

   public void completed(Integer bytesRead, T client) {
      if (client.isConnected()) {
         if (bytesRead < 0) {
            client.disconnect();
         } else if (bytesRead < client.getExpectedReadSize()) {
            client.resumeRead(bytesRead);
         } else {
            if (client.isReadingPayload()) {
               this.handlePayload(client);
            } else {
               this.handleHeader(client);
            }
         }
      }
   }

   private void handleHeader(T client) {
      ByteBuffer buffer = client.getConnection().getReadingBuffer();
      if (buffer == null) {
         client.disconnect();
      } else {
         buffer.flip();
         int dataSize = Short.toUnsignedInt(buffer.getShort()) - 2;
         if (dataSize <= 0) {
            client.read();
         } else if (dataSize > 65533) {
            client.disconnect();
         } else {
            client.readPayload(dataSize);
         }
      }
   }

   private void handlePayload(T client) {
      ByteBuffer buffer = client.getConnection().getReadingBuffer();
      if (buffer == null) {
         client.disconnect();
      } else {
         buffer.flip();
         this.parseAndExecutePacket(client, buffer);
         client.read();
      }
   }

   private void parseAndExecutePacket(T client, ByteBuffer incomingBuffer) {
      try {
         ReadBuffer buffer = ReadBuffer.of(incomingBuffer);
         if (client.decrypt(buffer, 0, buffer.remaining())) {
            ReadablePacket<T> packet = this._packetHandler.handle(buffer, client);
            if (packet != null) {
               packet.init(client, buffer);
               if (packet.read()) {
                  this._executor.execute(packet);
               }
            }
         }
      } catch (Exception var5) {
         this.failed(var5, client);
      }
   }

   public void failed(Throwable e, T client) {
      client.disconnect();
   }
}
