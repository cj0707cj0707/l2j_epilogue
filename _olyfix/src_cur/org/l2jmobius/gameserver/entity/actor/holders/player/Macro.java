package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.List;

public class Macro {
   private int _id;
   private final int _icon;
   private final String _name;
   private final String _descr;
   private final String _acronym;
   private final List<MacroCmd> _commands;

   public Macro(int id, int icon, String name, String descr, String acronym, List<MacroCmd> list) {
      this._id = id;
      this._icon = icon;
      this._name = name;
      this._descr = descr;
      this._acronym = acronym;
      this._commands = list;
   }

   public int getId() {
      return this._id;
   }

   public void setId(int id) {
      this._id = id;
   }

   public int getIcon() {
      return this._icon;
   }

   public String getName() {
      return this._name;
   }

   public String getDescr() {
      return this._descr;
   }

   public String getAcronym() {
      return this._acronym;
   }

   public List<MacroCmd> getCommands() {
      return this._commands;
   }
}
