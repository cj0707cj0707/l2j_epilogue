package org.l2jmobius.commons.network;

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.l2jmobius.commons.network.pool.BufferPool;
import org.l2jmobius.commons.network.pool.ResourcePool;
import org.l2jmobius.commons.util.ConfigReader;

public class ConnectionConfig {
   public static final int HEADER_SIZE = 2;
   private static final Pattern BUFFER_POOL_PROPERTY = Pattern.compile("(BufferPool\\.\\w+?\\.)Size", 2);
   private static final int MINIMUM_POOL_GROUPS = 3;
   public ResourcePool resourcePool;
   public SocketAddress address;
   public float initBufferPoolFactor;
   public long shutdownWaitTime;
   public int threadPoolSize;
   public boolean useNagle;
   public boolean dropPackets;
   public int dropPacketThreshold;
   public int threadPriority;
   public boolean autoExpandPoolCapacity;

   public ConnectionConfig(SocketAddress socketAddress) {
      this.address = socketAddress;
      this.threadPoolSize = 2;
      this.resourcePool = new ResourcePool();
      this.resourcePool.addBufferPool(2, new BufferPool(100, 2));
      ConfigReader networkConfig = new ConfigReader("config/Network.ini");
      this.shutdownWaitTime = networkConfig.getInt("ShutdownWaitTime", 5) * 1000L;
      int processors = Runtime.getRuntime().availableProcessors();
      this.threadPoolSize = networkConfig.getInt("ThreadPoolSize", this.threadPoolSize);
      this.threadPoolSize = this.threadPoolSize < 1 ? processors * 4 : this.threadPoolSize;
      this.threadPriority = networkConfig.getInt("ThreadPriority", 5);
      this.autoExpandPoolCapacity = networkConfig.getBoolean("BufferPool.AutoExpandCapacity", true);
      this.initBufferPoolFactor = networkConfig.getFloat("BufferPool.InitFactor", 0.0F);
      this.dropPackets = networkConfig.getBoolean("DropPackets", this.dropPackets);
      this.dropPacketThreshold = networkConfig.getInt("DropPacketThreshold", 250);
      this.resourcePool.setBufferSegmentSize(networkConfig.getInt("BufferSegmentSize", this.resourcePool.getSegmentSize()));
      networkConfig.getStringPropertyNames().forEach(property -> {
         Matcher matcher = BUFFER_POOL_PROPERTY.matcher(property);
         if (matcher.matches()) {
            int size = networkConfig.getInt(property, 10);
            int bufferSizex = networkConfig.getInt(matcher.group(1) + "BufferSize", 1024);
            this.resourcePool.addBufferPool(bufferSizex, new BufferPool(size, bufferSizex));
         }
      });
      this.resourcePool.addBufferPool(this.resourcePool.getSegmentSize(), new BufferPool(100, this.resourcePool.getSegmentSize()));
      int missingPools = 3 - this.resourcePool.bufferPoolSize();

      for (int i = 0; i < missingPools; i++) {
         int bufferSize = 256 << i;
         this.resourcePool.addBufferPool(bufferSize, new BufferPool(10, bufferSize));
      }

      this.resourcePool.initializeBuffers(this.autoExpandPoolCapacity, this.initBufferPoolFactor);
   }
}
