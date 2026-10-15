package org.l2jmobius.gameserver.mechanics.html.pagehandlers;

import org.l2jmobius.gameserver.mechanics.html.IBypassFormatter;
import org.l2jmobius.gameserver.mechanics.html.IHtmlStyle;
import org.l2jmobius.gameserver.mechanics.html.IPageHandler;

public class DefaultPageHandler implements IPageHandler {
   public static final DefaultPageHandler INSTANCE = new DefaultPageHandler(2);
   protected final int _pagesOffset;

   public DefaultPageHandler(int pagesOffset) {
      this._pagesOffset = pagesOffset;
   }

   @Override
   public void apply(String bypass, int currentPage, int pages, StringBuilder sb, IBypassFormatter bypassFormatter, IHtmlStyle style) {
      int pagerStart = Math.max(currentPage - this._pagesOffset, 0);
      int pagerFinish = Math.min(currentPage + this._pagesOffset + 1, pages);
      if (pagerStart > this._pagesOffset) {
         for (int i = 0; i < this._pagesOffset; i++) {
            sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
         }

         sb.append(style.applySeparator());
      }

      for (int i = pagerStart; i < pagerFinish; i++) {
         sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
      }

      if (pages > pagerFinish) {
         sb.append(style.applySeparator());

         for (int i = pages - this._pagesOffset; i < pages; i++) {
            sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
         }
      }
   }
}
