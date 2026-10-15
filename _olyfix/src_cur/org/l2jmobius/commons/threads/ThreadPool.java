package org.l2jmobius.commons.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;

public class ThreadPool {
   private static final Logger LOGGER = Logger.getLogger(ThreadPool.class.getName());
   private static final long ONE_HUNDRED_YEARS_MS = 3155695200000L;
   private static final long MIN_DELAY_MS = 0L;
   private static final long PURGE_INTERVAL_MS = 60000L;
   private static final int INSTANT_POOL_KEEP_ALIVE_MINUTES = 1;
   private static ScheduledThreadPoolExecutor HIGH_PRIORITY_SCHEDULED_POOL;
   private static ScheduledThreadPoolExecutor SCHEDULED_POOL;
   private static ThreadPoolExecutor INSTANT_POOL;

   public static void init() {
      LOGGER.info("ThreadPool: Initializing.");
      ThreadConfig.load();
      if (ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE > 0) {
         HIGH_PRIORITY_SCHEDULED_POOL = new ScheduledThreadPoolExecutor(
            ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE,
            new ThreadProvider("L2jMobius High Priority ScheduledThread", ThreadPriority.PRIORITY_8),
            new CallerRunsPolicy()
         );
         LOGGER.info(
            StringUtil.concat(
               "...scheduled pool executor with ", String.valueOf(ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE), " high priority threads."
            )
         );
      }

      SCHEDULED_POOL = new ScheduledThreadPoolExecutor(
         ThreadConfig.SCHEDULED_THREAD_POOL_SIZE, new ThreadProvider("L2jMobius ScheduledThread"), new CallerRunsPolicy()
      );
      SCHEDULED_POOL.setRejectedExecutionHandler(new ThreadPool.RejectedExecutionHandlerImpl());
      SCHEDULED_POOL.setRemoveOnCancelPolicy(true);
      SCHEDULED_POOL.prestartAllCoreThreads();
      INSTANT_POOL = new ThreadPoolExecutor(
         ThreadConfig.INSTANT_THREAD_POOL_SIZE, Integer.MAX_VALUE, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadProvider("L2jMobius Thread")
      );
      INSTANT_POOL.setRejectedExecutionHandler(new ThreadPool.RejectedExecutionHandlerImpl());
      INSTANT_POOL.prestartAllCoreThreads();
      scheduleAtFixedRate(ThreadPool::purge, 60000L, 60000L);
      LOGGER.info(StringUtil.concat("...scheduled pool executor with ", String.valueOf(ThreadConfig.SCHEDULED_THREAD_POOL_SIZE), " total threads."));
      LOGGER.info(StringUtil.concat("...instant pool executor with ", String.valueOf(ThreadConfig.INSTANT_THREAD_POOL_SIZE), " total threads."));
   }

   public static void purge() {
      SCHEDULED_POOL.purge();
      INSTANT_POOL.purge();
      if (HIGH_PRIORITY_SCHEDULED_POOL != null) {
         HIGH_PRIORITY_SCHEDULED_POOL.purge();
      }
   }

   public static ScheduledFuture<?> schedule(Runnable runnable, long delay) {
      try {
         return SCHEDULED_POOL.schedule(new ThreadPool.RunnableWrapper(runnable), validateDelay(delay), TimeUnit.MILLISECONDS);
      } catch (Exception var4) {
         LOGGER.warning(
            StringUtil.concat(
               "ThreadPool: Failed to schedule task ",
               runnable.getClass().getSimpleName(),
               " with delay ",
               String.valueOf(delay),
               "ms: ",
               var4.getMessage(),
               System.lineSeparator(),
               TraceUtil.getStackTrace(var4)
            )
         );
         return null;
      }
   }

   public static ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period) {
      try {
         return SCHEDULED_POOL.scheduleAtFixedRate(
            new ThreadPool.RunnableWrapper(runnable), validateDelay(initialDelay), validateDelay(period), TimeUnit.MILLISECONDS
         );
      } catch (Exception var6) {
         LOGGER.warning(
            StringUtil.concat(
               "ThreadPool: Failed to schedule recurring task ",
               runnable.getClass().getSimpleName(),
               " with initial delay ",
               String.valueOf(initialDelay),
               "ms and period ",
               String.valueOf(period),
               "ms: ",
               var6.getMessage(),
               System.lineSeparator(),
               TraceUtil.getStackTrace(var6)
            )
         );
         return null;
      }
   }

   public static ScheduledFuture<?> schedulePriorityTaskAtFixedRate(Runnable runnable, long initialDelay, long period) {
      if (HIGH_PRIORITY_SCHEDULED_POOL == null) {
         return scheduleAtFixedRate(runnable, initialDelay, period);
      } else {
         try {
            return HIGH_PRIORITY_SCHEDULED_POOL.scheduleAtFixedRate(
               new ThreadPool.RunnableWrapper(runnable), validateDelay(initialDelay), validateDelay(period), TimeUnit.MILLISECONDS
            );
         } catch (Exception var6) {
            LOGGER.warning(
               StringUtil.concat(
                  "ThreadPool: Failed to schedule high priority task ",
                  runnable.getClass().getSimpleName(),
                  " with initial delay ",
                  String.valueOf(initialDelay),
                  "ms and period ",
                  String.valueOf(period),
                  "ms: ",
                  var6.getMessage(),
                  System.lineSeparator(),
                  TraceUtil.getStackTrace(var6)
               )
            );
            return null;
         }
      }
   }

   public static void execute(Runnable runnable) {
      try {
         INSTANT_POOL.execute(new ThreadPool.RunnableWrapper(runnable));
      } catch (Exception var2) {
         LOGGER.warning(
            StringUtil.concat(
               "ThreadPool: Failed to execute task ",
               runnable.getClass().getSimpleName(),
               ": ",
               var2.getMessage(),
               System.lineSeparator(),
               TraceUtil.getStackTrace(var2)
            )
         );
      }
   }

   private static long validateDelay(long delay) {
      if (delay < 0L) {
         LOGGER.warning(
            StringUtil.concat("ThreadPool: Invalid delay ", String.valueOf(delay), "ms is below minimum, using ", String.valueOf(0L), "ms instead.")
         );
         LOGGER.warning(TraceUtil.getStackTrace(new Exception()));
         return 0L;
      } else if (delay > 3155695200000L) {
         LOGGER.warning(
            StringUtil.concat("ThreadPool: Invalid delay ", String.valueOf(delay), "ms exceeds maximum, using ", String.valueOf(3155695200000L), "ms instead.")
         );
         LOGGER.warning(TraceUtil.getStackTrace(new Exception()));
         return 3155695200000L;
      } else {
         return delay;
      }
   }

   public static void shutdown() {
      try {
         LOGGER.info("ThreadPool: Shutting down all thread pools.");
         SCHEDULED_POOL.shutdownNow();
         INSTANT_POOL.shutdownNow();
         if (HIGH_PRIORITY_SCHEDULED_POOL != null) {
            HIGH_PRIORITY_SCHEDULED_POOL.shutdownNow();
         }
      } catch (Throwable var1) {
         LOGGER.warning(StringUtil.concat("ThreadPool: Exception occurred during shutdown: ", var1.getMessage()));
      }
   }

   private static class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
      private static final Logger LOGGER = Logger.getLogger(ThreadPool.RejectedExecutionHandlerImpl.class.getName());

      @Override
      public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
         if (!executor.isShutdown()) {
            LOGGER.warning(
               StringUtil.concat(
                  "ThreadPool: Task ",
                  runnable.getClass().getSimpleName(),
                  " rejected by executor ",
                  String.valueOf(executor),
                  ", attempting recovery execution."
               )
            );
            if (Thread.currentThread().getPriority() > 5) {
               new Thread(runnable).start();
            } else {
               runnable.run();
            }
         }
      }
   }

   private static class RunnableWrapper implements Runnable {
      private final Runnable _wrappedRunnable;

      public RunnableWrapper(Runnable runnable) {
         this._wrappedRunnable = runnable;
      }

      @Override
      public void run() {
         try {
            this._wrappedRunnable.run();
         } catch (Throwable var4) {
            Thread currentThread = Thread.currentThread();
            UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
            if (exceptionHandler != null) {
               exceptionHandler.uncaughtException(currentThread, var4);
            }
         }
      }
   }
}
