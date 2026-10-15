package org.l2jmobius.gameserver.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeadlockWatcher extends Thread {
   private static final Logger LOGGER = Logger.getLogger(DeadlockWatcher.class.getName());
   private static final int MAX_STACK_DEPTH = 30;
   private static final int MAX_DEADLOCK_THREADS = 20;
   private final Duration _checkInterval;
   private final Runnable _deadlockCallback;
   private final ThreadMXBean _threadMXBean;

   public DeadlockWatcher(Duration checkInterval, Runnable deadlockCallback) {
      super("DeadlockWatcher");
      this._checkInterval = checkInterval;
      this._deadlockCallback = deadlockCallback;
      this._threadMXBean = ManagementFactory.getThreadMXBean();
   }

   @Override
   public void run() {
      LOGGER.info("DeadlockWatcher: Thread started.");

      while (!this.isInterrupted()) {
         try {
            long[] deadlockedThreadIds = this._threadMXBean.findDeadlockedThreads();
            if (deadlockedThreadIds != null) {
               LOGGER.warning("DeadlockWatcher: Deadlock detected!");
               if (deadlockedThreadIds.length > 20) {
                  this.generateMinimalDeadlockReport(deadlockedThreadIds);
               } else {
                  this.generateDeadlockReport(deadlockedThreadIds);
               }

               if (this._deadlockCallback != null) {
                  try {
                     this._deadlockCallback.run();
                  } catch (Exception var3) {
                     LOGGER.log(Level.SEVERE, "DeadlockWatcher: Exception in deadlock callback: ", (Throwable)var3);
                  }
               }
            }

            Thread.sleep(this._checkInterval.toMillis());
         } catch (InterruptedException var4) {
            LOGGER.info("DeadlockWatcher: Thread interrupted and will exit.");
            Thread.currentThread().interrupt();
            break;
         } catch (Exception var5) {
            LOGGER.log(Level.WARNING, "DeadlockWatcher: Exception during deadlock check: ", (Throwable)var5);
         }
      }

      LOGGER.info("DeadlockWatcher: Thread terminated.");
   }

   private void generateDeadlockReport(long[] deadlockedThreadIds) {
      LOGGER.warning("========== DEADLOCK REPORT ==========");

      for (ThreadInfo info : this._threadMXBean.getThreadInfo(deadlockedThreadIds, true, true)) {
         if (info != null) {
            LOGGER.warning("Thread: " + info.getThreadName() + " (ID: " + info.getThreadId() + ")");
            LOGGER.warning("State: " + info.getThreadState());
            String lockName = info.getLockName();
            if (lockName != null) {
               LOGGER.warning("Waiting for lock: " + lockName);
               LOGGER.warning("Lock owner: " + info.getLockOwnerName() + " (ID: " + info.getLockOwnerId() + ")");
            }

            LOGGER.warning("Stack trace:");
            StackTraceElement[] stack = info.getStackTrace();

            for (int i = 0; i < Math.min(stack.length, 30); i++) {
               LOGGER.warning("\tat " + stack[i]);
            }

            if (stack.length > 30) {
               LOGGER.warning("\t... (stack trace truncated)");
            }

            MonitorInfo[] lockedMonitors = info.getLockedMonitors();
            if (lockedMonitors.length > 0) {
               LOGGER.warning("Locked monitors:");

               for (MonitorInfo monitor : lockedMonitors) {
                  LOGGER.warning("\t- " + monitor.getClassName() + " at line " + monitor.getLockedStackFrame().getLineNumber());
               }
            }

            LockInfo[] lockedSynchronizers = info.getLockedSynchronizers();
            if (lockedSynchronizers.length > 0) {
               LOGGER.warning("Locked synchronizers:");

               for (LockInfo lock : lockedSynchronizers) {
                  LOGGER.warning("\t- " + lock.getClassName());
               }
            }
         }
      }

      LOGGER.warning("========== END DEADLOCK REPORT ==========");
   }

   private void generateMinimalDeadlockReport(long[] deadlockedThreadIds) {
      LOGGER.warning("========== MINIMAL DEADLOCK REPORT ==========");

      for (long id : deadlockedThreadIds) {
         ThreadInfo info = this._threadMXBean.getThreadInfo(id, 10);
         if (info != null) {
            LOGGER.warning("Thread: " + info.getThreadName() + " (State: " + info.getThreadState() + ")");
            String lockName = info.getLockName();
            if (lockName != null) {
               LOGGER.warning("\tWaiting for: " + lockName);
            }

            StackTraceElement[] stack = info.getStackTrace();
            int frames = Math.min(5, stack.length);

            for (int i = 0; i < frames; i++) {
               LOGGER.warning("\tat " + stack[i]);
            }
         }
      }

      LOGGER.warning("========== END MINIMAL REPORT ==========");
   }
}
