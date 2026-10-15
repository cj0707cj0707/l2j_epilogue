package org.l2jmobius.gameserver.mechanics.html.styles;

import org.l2jmobius.gameserver.mechanics.html.IHtmlStyle;

public class ButtonsStyle implements IHtmlStyle {
   private static final String DEFAULT_PAGE_LINK_FORMAT = "<td><button action=\"%s\" value=\"%s\" width=\"%d\" height=\"%d\" back=\"%s\" fore=\"%s\"></td>";
   private static final String DEFAULT_PAGE_TEXT_FORMAT = "<td>%s</td>";
   private static final String DEFAULT_PAGER_SEPARATOR = "<td align=center> | </td>";
   public static final ButtonsStyle INSTANCE = new ButtonsStyle(40, 15, "L2UI_CT1.Button_DF_Down", "L2UI_CT1.Button_DF");
   private final int _width;
   private final int _height;
   private final String _back;
   private final String _fore;

   public ButtonsStyle(int width, int height, String back, String fore) {
      this._width = width;
      this._height = height;
      this._back = back;
      this._fore = fore;
   }

   @Override
   public String applyBypass(String bypass, String name, boolean isEnabled) {
      return isEnabled
         ? String.format("<td>%s</td>", name)
         : String.format(
            "<td><button action=\"%s\" value=\"%s\" width=\"%d\" height=\"%d\" back=\"%s\" fore=\"%s\"></td>",
            bypass,
            name,
            this._width,
            this._height,
            this._back,
            this._fore
         );
   }

   @Override
   public String applySeparator() {
      return "<td align=center> | </td>";
   }
}
