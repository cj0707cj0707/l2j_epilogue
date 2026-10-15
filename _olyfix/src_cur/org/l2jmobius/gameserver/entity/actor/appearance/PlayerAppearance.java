package org.l2jmobius.gameserver.entity.actor.appearance;

import org.l2jmobius.gameserver.entity.actor.Player;

public class PlayerAppearance {
   public static final int DEFAULT_TITLE_COLOR = 15530402;
   private Player _owner;
   private byte _face;
   private byte _hairColor;
   private byte _hairStyle;
   private boolean _isFemale;
   private String _visibleName;
   private String _visibleTitle;
   private int _nameColor = 16777215;
   private int _titleColor = 15530402;

   public PlayerAppearance(byte face, byte hColor, byte hStyle, boolean isFemale) {
      this._face = face;
      this._hairColor = hColor;
      this._hairStyle = hStyle;
      this._isFemale = isFemale;
   }

   public void setVisibleName(String visibleName) {
      this._visibleName = visibleName;
   }

   public String getVisibleName() {
      return this._visibleName == null ? this._owner.getName() : this._visibleName;
   }

   public void setVisibleTitle(String visibleTitle) {
      this._visibleTitle = visibleTitle;
   }

   public String getVisibleTitle() {
      return this._visibleTitle == null ? this._owner.getTitle() : this._visibleTitle;
   }

   public byte getFace() {
      return this._face;
   }

   public void setFace(int value) {
      this._face = (byte)value;
   }

   public byte getHairColor() {
      return this._hairColor;
   }

   public void setHairColor(int value) {
      this._hairColor = (byte)value;
   }

   public byte getHairStyle() {
      return this._hairStyle;
   }

   public void setHairStyle(int value) {
      this._hairStyle = (byte)value;
   }

   public boolean isFemale() {
      return this._isFemale;
   }

   public void setFemale() {
      this._isFemale = true;
   }

   public void setMale() {
      this._isFemale = false;
   }

   public int getNameColor() {
      return this._nameColor;
   }

   public void setNameColor(int nameColor) {
      if (nameColor >= 0) {
         this._nameColor = nameColor;
      }
   }

   public void setNameColor(int red, int green, int blue) {
      this._nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
   }

   public int getTitleColor() {
      return this._titleColor;
   }

   public void setTitleColor(int titleColor) {
      if (titleColor >= 0) {
         this._titleColor = titleColor;
      }
   }

   public void setTitleColor(int red, int green, int blue) {
      this._titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
   }

   public void setOwner(Player owner) {
      this._owner = owner;
   }

   public Player getOwner() {
      return this._owner;
   }
}
