package org.l2jmobius.commons.network.packet;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketThreadFactory implements ThreadFactory {
   private static final String DEFAULT_BASE_NAME = "Thread";
   private static final int INITIAL_SEQUENCE_VALUE = 1;
   private static final int STACK_SIZE = 0;
   private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger(1);
   private final AtomicInteger _threadSequence = new AtomicInteger(1);
   private final String _threadPrefix;
   private final int _threadPriority;

   public PacketThreadFactory(String baseName, int priority) {
      String safeBaseName = baseName != null && !baseName.isEmpty() ? baseName : "Thread";
      this._threadPrefix = safeBaseName + "-network-pool-" + POOL_SEQUENCE.getAndIncrement() + "-thread-";
      if (priority < 1) {
         this._threadPriority = 1;
      } else if (priority > 10) {
         this._threadPriority = 10;
      } else {
         this._threadPriority = priority;
      }
   }

   @Override
   public Thread newThread(Runnable task) {
      int threadIndex = this.nextIndex();
      Thread thread = new Thread(null, task, this._threadPrefix + threadIndex, 0L);
      ThreadGroup threadGroup = thread.getThreadGroup();
      int groupMaxPriority = threadGroup != null ? threadGroup.getMaxPriority() : 10;
      int effectivePriority = this._threadPriority > groupMaxPriority ? groupMaxPriority : this._threadPriority;
      thread.setPriority(effectivePriority);
      thread.setDaemon(false);
      return thread;
   }

   private int nextIndex() {
      int currentValue = this._threadSequence.getAndIncrement();
      return currentValue == Integer.MIN_VALUE ? this._threadSequence.updateAndGet(x -> x <= 0 ? 1 : x) : currentValue;
   }
}
