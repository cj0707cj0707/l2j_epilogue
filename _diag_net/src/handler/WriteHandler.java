package org.l2jmobius.commons.network.handler;

import java.nio.channels.CompletionHandler;

public class WriteHandler<T extends org.l2jmobius.commons.network.Client<org.l2jmobius.commons.network.Connection<T>>> implements CompletionHandler<Long, T> {
   public void completed(Long result, T client) {
      if (client != null) {
         int bytesWritten = result.intValue();
         if (bytesWritten < 0) {
            if (client.isConnected()) {
               client.disconnect();
            }
         } else {
            if (bytesWritten > 0 && bytesWritten < client.getDataSentSize()) {
               client.resumeSend(bytesWritten);
            } else {
               client.finishWriting();
            }
         }
      }
   }

   public void failed(Throwable e, T client) {
      if (client != null) {
         client.disconnect();
      }
   }
}
