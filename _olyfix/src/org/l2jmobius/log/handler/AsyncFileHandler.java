package org.l2jmobius.log.handler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public abstract class AsyncFileHandler extends FileHandler {
   private static final int QUEUE_CAPACITY = 8192;
   private static final long DRAIN_POLL_MILLIS = 200L;
   private static final long CLOSE_JOIN_MILLIS = 5000L;
   private static final Formatter PASSTHROUGH = new Formatter() {
      @Override
      public String format(LogRecord logRecord) {
         return logRecord.getMessage();
      }
   };
   private static final Set<AsyncFileHandler> INSTANCES = ConcurrentHashMap.newKeySet();
   private static final AtomicBoolean SHUTDOWN_HOOK_INSTALLED = new AtomicBoolean();
   private final LinkedBlockingQueue<String> _queue = new LinkedBlockingQueue<>(8192);
   private final AtomicLong _dropped = new AtomicLong();
   private final Thread _worker;
   private volatile boolean _running = true;
   private final Filter _userFilter = this.getFilter();
   private final Formatter _userFormatter = this.getFormatter();
   private final Level _userLevel = this.getLevel() == null ? Level.ALL : this.getLevel();

   protected AsyncFileHandler() throws IOException {
      this.setFilter(null);
      this.setFormatter(PASSTHROUGH);
      this.setLevel(Level.ALL);
      this._worker = new Thread(this::drain, this.getClass().getSimpleName() + "-Writer");
      this._worker.setDaemon(true);
      this._worker.start();
      INSTANCES.add(this);
      ensureShutdownHook();
   }

   @Override
   public void publish(LogRecord logRecord) {
      if (logRecord != null) {
         if (logRecord.getLevel().intValue() >= this._userLevel.intValue() && this._userLevel != Level.OFF) {
            if (this._userFilter == null || this._userFilter.isLoggable(logRecord)) {
               String formatted;
               try {
                  formatted = this._userFormatter.format(logRecord);
               } catch (Exception var4) {
                  this.reportError(null, var4, 5);
                  return;
               }

               if (!this._queue.offer(formatted)) {
                  this._dropped.incrementAndGet();
               }
            }
         }
      }
   }

   private void drain() {
      while (this._running || !this._queue.isEmpty()) {
         try {
            String s = this._queue.poll(200L, TimeUnit.MILLISECONDS);
            if (s != null) {
               LogRecord wrapper = new LogRecord(Level.INFO, s);
               super.publish(wrapper);
            }
         } catch (InterruptedException var4) {
            Thread.currentThread().interrupt();
            return;
         } catch (Exception var5) {
            this.reportError(null, var5, 1);
         }
      }

      try {
         super.flush();
      } catch (Exception var3) {
      }
   }

   @Override
   public void flush() {
      super.flush();
   }

   @Override
   public void close() throws SecurityException {
      this._running = false;

      try {
         this._worker.join(5000L);
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
      }

      long dropped = this._dropped.get();
      if (dropped > 0L) {
         System.err.println(this.getClass().getSimpleName() + ": dropped " + dropped + " log records due to queue overflow.");
      }

      INSTANCES.remove(this);
      super.close();
   }

   private static void ensureShutdownHook() {
      if (SHUTDOWN_HOOK_INSTALLED.compareAndSet(false, true)) {
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (AsyncFileHandler handler : INSTANCES) {
               try {
                  handler.close();
               } catch (Exception var3) {
               }
            }
         }, "AsyncFileHandler-Shutdown"));
      }
   }
}
