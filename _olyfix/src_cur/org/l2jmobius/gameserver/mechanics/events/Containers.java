package org.l2jmobius.gameserver.mechanics.events;

public class Containers {
   private static final ListenersContainer _globalContainer = new ListenersContainer();
   private static final ListenersContainer _globalNpcsContainer = new ListenersContainer();
   private static final ListenersContainer _globalMonstersContainer = new ListenersContainer();
   private static final ListenersContainer _globalPlayersContainer = new ListenersContainer();

   protected Containers() {
   }

   public static ListenersContainer Global() {
      return _globalContainer;
   }

   public static ListenersContainer Npcs() {
      return _globalNpcsContainer;
   }

   public static ListenersContainer Monsters() {
      return _globalMonstersContainer;
   }

   public static ListenersContainer Players() {
      return _globalPlayersContainer;
   }
}
