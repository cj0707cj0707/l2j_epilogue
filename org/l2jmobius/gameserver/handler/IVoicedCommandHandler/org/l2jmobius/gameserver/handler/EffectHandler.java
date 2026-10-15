package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

public class EffectHandler implements IHandler<Class<? extends AbstractEffect>, String> {
   private final Map<String, Class<? extends AbstractEffect>> _handlers = new HashMap<>();

   protected EffectHandler() {
   }

   public void registerHandler(Class<? extends AbstractEffect> handler) {
      this._handlers.put(handler.getSimpleName(), handler);
   }

   public synchronized void removeHandler(Class<? extends AbstractEffect> handler) {
      this._handlers.remove(handler.getSimpleName());
   }

   public Class<? extends AbstractEffect> getHandler(String name) {
      return this._handlers.get(name);
   }

   @Override
   public int size() {
      return this._handlers.size();
   }

   public void executeScript() {
      try {
         ScriptEngine.getInstance().executeScript(ScriptEngine.EFFECT_MASTER_HANDLER_FILE);
      } catch (Exception var2) {
         throw new Error("Problems while running EffectMasterHandler", var2);
      }
   }

   public static EffectHandler getInstance() {
      return EffectHandler.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EffectHandler INSTANCE = new EffectHandler();
   }
}
