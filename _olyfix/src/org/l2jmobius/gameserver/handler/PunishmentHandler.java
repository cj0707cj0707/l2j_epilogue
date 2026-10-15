package org.l2jmobius.gameserver.handler;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

public class PunishmentHandler implements IHandler<IPunishmentHandler, PunishmentType> {
   private final Map<PunishmentType, IPunishmentHandler> _handlers = new EnumMap<>(PunishmentType.class);

   protected PunishmentHandler() {
   }

   public void registerHandler(IPunishmentHandler handler) {
      this._handlers.put(handler.getType(), handler);
   }

   public synchronized void removeHandler(IPunishmentHandler handler) {
      this._handlers.remove(handler.getType());
   }

   public IPunishmentHandler getHandler(PunishmentType val) {
      return this._handlers.get(val);
   }

   @Override
   public int size() {
      return this._handlers.size();
   }

   public static PunishmentHandler getInstance() {
      return PunishmentHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PunishmentHandler INSTANCE = new PunishmentHandler();
   }
}
