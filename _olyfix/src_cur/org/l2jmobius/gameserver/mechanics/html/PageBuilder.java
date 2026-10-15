package org.l2jmobius.gameserver.mechanics.html;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.l2jmobius.gameserver.mechanics.html.formatters.DefaultFormatter;
import org.l2jmobius.gameserver.mechanics.html.pagehandlers.DefaultPageHandler;
import org.l2jmobius.gameserver.mechanics.html.styles.DefaultStyle;

public class PageBuilder<T> {
   private final Collection<T> _elements;
   private final int _elementsPerPage;
   private final String _bypass;
   private int _currentPage = 0;
   private IPageHandler _pageHandler = DefaultPageHandler.INSTANCE;
   private IBypassFormatter _formatter = DefaultFormatter.INSTANCE;
   private IHtmlStyle _style = DefaultStyle.INSTANCE;
   private IBodyHandler<T> _bodyHandler;

   private PageBuilder(Collection<T> elements, int elementsPerPage, String bypass) {
      this._elements = elements;
      this._elementsPerPage = elementsPerPage;
      this._bypass = bypass;
   }

   public PageBuilder<T> currentPage(int currentPage) {
      this._currentPage = Math.max(currentPage, 0);
      return this;
   }

   public PageBuilder<T> bodyHandler(IBodyHandler<T> bodyHandler) {
      Objects.requireNonNull(bodyHandler, "Body Handler cannot be null!");
      this._bodyHandler = bodyHandler;
      return this;
   }

   public PageBuilder<T> pageHandler(IPageHandler pageHandler) {
      Objects.requireNonNull(pageHandler, "Page Handler cannot be null!");
      this._pageHandler = pageHandler;
      return this;
   }

   public PageBuilder<T> formatter(IBypassFormatter formatter) {
      Objects.requireNonNull(formatter, "Formatter cannot be null!");
      this._formatter = formatter;
      return this;
   }

   public PageBuilder<T> style(IHtmlStyle style) {
      Objects.requireNonNull(style, "Style cannot be null!");
      this._style = style;
      return this;
   }

   public PageResult build() {
      Objects.requireNonNull(this._bodyHandler, "Body was not set!");
      StringBuilder pagerTemplate = new StringBuilder();
      int pages = this._elements.size() / this._elementsPerPage + (this._elements.size() % this._elementsPerPage > 0 ? 1 : 0);
      if (pages > 1) {
         this._pageHandler.apply(this._bypass, this._currentPage, pages - 1, pagerTemplate, this._formatter, this._style);
      }

      if (this._currentPage > pages) {
         this._currentPage = pages - 1;
      }

      StringBuilder sb = new StringBuilder();
      int start = Math.max(this._elementsPerPage * this._currentPage, 0);
      this._bodyHandler.create(this._elements, pages, start, this._elementsPerPage, sb);
      return new PageResult(pages, pagerTemplate, sb);
   }

   public static <T> PageBuilder<T> newBuilder(Collection<T> elements, int elementsPerPage, String bypass) {
      return new PageBuilder<>(elements, elementsPerPage, bypass.trim());
   }

   public static <T> PageBuilder<T> newBuilder(T[] elements, int elementsPerPage, String bypass) {
      return new PageBuilder<>(Arrays.asList(elements), elementsPerPage, bypass.trim());
   }
}
