package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.util.HtmlUtil;

public abstract class AbstractHtmlPacket extends ServerPacket {
   public static final char VAR_PARAM_START_CHAR = '$';
   private final int _npcObjId;
   private String _html = null;
   private boolean _disabledValidation = false;

   protected AbstractHtmlPacket() {
      this._npcObjId = 0;
   }

   protected AbstractHtmlPacket(int npcObjId) {
      if (npcObjId < 0) {
         throw new IllegalArgumentException();
      } else {
         this._npcObjId = npcObjId;
      }
   }

   protected AbstractHtmlPacket(String html) {
      this._npcObjId = 0;
      this.setHtml(html);
   }

   protected AbstractHtmlPacket(int npcObjId, String html) {
      if (npcObjId < 0) {
         throw new IllegalArgumentException();
      } else {
         this._npcObjId = npcObjId;
         this.setHtml(html);
      }
   }

   public void disableValidation() {
      this._disabledValidation = true;
   }

   public void setHtml(String html) {
      if (html.length() > 17200) {
         org.l2jmobius.gameserver.network.PacketLogger.warning(this.getClass().getSimpleName() + ": Html is too long! this will crash the client!");
         this._html = html.substring(0, 17200);
      } else if (!html.contains("<html") && !html.startsWith("..\\L2")) {
         this._html = "<html><body>" + html + "</body></html>";
      } else {
         this._html = html;
      }
   }

   public boolean setFile(Player player, String path) {
      String content = HtmCache.getInstance().getHtm(player, path);
      if (content == null) {
         this.setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
         org.l2jmobius.gameserver.network.PacketLogger.warning(this.getClass().getSimpleName() + ": Missing html page " + path);
         return false;
      } else {
         this.setHtml(content);
         return true;
      }
   }

   public void replace(String pattern, String value) {
      this._html = this._html.replaceAll(pattern, value.replaceAll("\\$", "\\\\\\$"));
   }

   public void replace(String pattern, CharSequence value) {
      this.replace(pattern, String.valueOf(value));
   }

   public void replace(String pattern, boolean value) {
      this.replace(pattern, String.valueOf(value));
   }

   public void replace(String pattern, int value) {
      this.replace(pattern, String.valueOf(value));
   }

   public void replace(String pattern, long value) {
      this.replace(pattern, String.valueOf(value));
   }

   public void replace(String pattern, double value) {
      this.replace(pattern, String.valueOf(value));
   }

   @Override
   public void runImpl(Player player) {
      if (player != null) {
         player.clearHtmlActions(this.getScope());
      }

      if (!this._disabledValidation) {
         if (player != null) {
            HtmlUtil.buildHtmlActionCache(player, this.getScope(), this._npcObjId, this._html);
         }
      }
   }

   public int getNpcObjId() {
      return this._npcObjId;
   }

   public String getHtml() {
      return this._html;
   }

   public abstract HtmlActionScope getScope();
}
