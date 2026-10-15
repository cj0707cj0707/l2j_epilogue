package org.l2jmobius.gameserver.mechanics.html;

public class PageResult {
   private final int _pages;
   private final StringBuilder _pagerTemplate;
   private final StringBuilder _bodyTemplate;

   public PageResult(int pages, StringBuilder pagerTemplate, StringBuilder bodyTemplate) {
      this._pages = pages;
      this._pagerTemplate = pagerTemplate;
      this._bodyTemplate = bodyTemplate;
   }

   public int getPages() {
      return this._pages;
   }

   public StringBuilder getPagerTemplate() {
      return this._pagerTemplate;
   }

   public StringBuilder getBodyTemplate() {
      return this._bodyTemplate;
   }
}
