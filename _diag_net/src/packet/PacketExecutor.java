package org.l2jmobius.commons.network.packet;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketExecutor<T extends org.l2jmobius.commons.network.Client<org.l2jmobius.commons.network.Connection<T>>> {
   private static final Logger LOGGER = Logger.getLogger(PacketExecutor.class.getName());
   private final ThreadPoolExecutor _executor;

   public PacketExecutor(org.l2jmobius.commons.network.ConnectionConfig config) {
      this._executor = new ThreadPoolExecutor(
         config.threadPoolSize,
         Integer.MAX_VALUE,
         1L,
         TimeUnit.MINUTES,
         new LinkedBlockingQueue<>(),
         new PacketThreadFactory("PacketExecutor", config.threadPriority)
      );
   }

   public void execute(ReadablePacket<T> packet) {
      try {
         this._executor.execute(new PacketExecutor.PacketRunnable<>(packet));
      } catch (Exception var3) {
         LOGGER.log(Level.WARNING, "Failed to submit " + packet.getClass().getSimpleName(), (Throwable)var3);
      }
   }

   private static class PacketRunnable<T extends org.l2jmobius.commons.network.Client<org.l2jmobius.commons.network.Connection<T>>> implements Runnable {
      private final ReadablePacket<T> _packet;

      public PacketRunnable(ReadablePacket<T> packet) {
         this._packet = packet;
      }

      @Override
      public void run() {
         try {
            this._packet.run();
         } catch (Throwable var4) {
            Thread currentThread = Thread.currentThread();
            UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
            if (handler != null) {
               handler.uncaughtException(currentThread, var4);
            } else {
               PacketExecutor.LOGGER.log(Level.SEVERE, "Uncaught exception in " + this._packet.getClass().getSimpleName(), var4);
            }
         }
      }
   }
}
