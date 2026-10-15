package org.l2jmobius.commons.network.packet;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.ReadBuffer;

@FunctionalInterface
public interface PacketHandler<T extends Client<Connection<T>>> {
   ReadablePacket<T> handle(ReadBuffer var1, T var2);
}
