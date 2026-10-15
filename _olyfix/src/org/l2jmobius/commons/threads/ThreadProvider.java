package org.l2jmobius.commons.threads;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadProvider implements ThreadFactory {
   private final AtomicInteger _id = new AtomicInteger();
   private final String _prefix;
   private final int _priority;
   private final boolean _daemon;

   public ThreadProvider(String prefix) {
      this(prefix, ThreadPriority.PRIORITY_5, false);
   }

   public ThreadProvider(String prefix, boolean daemon) {
      this(prefix, ThreadPriority.PRIORITY_5, daemon);
   }

   public ThreadProvider(String prefix, ThreadPriority priority) {
      this(prefix, priority, false);
   }

   public ThreadProvider(String prefix, ThreadPriority priority, boolean daemon) {
      this._prefix = prefix + " ";
      this._priority = priority.getId();
      this._daemon = daemon;
   }

   @Override
   public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, this._prefix + this._id.incrementAndGet());
      thread.setPriority(this._priority);
      thread.setDaemon(this._daemon);
      return thread;
   }
}
