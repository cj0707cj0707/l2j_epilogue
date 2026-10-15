package org.l2jmobius.gameserver.mechanics.html;

@FunctionalInterface
public interface IPageHandler {
   void apply(String var1, int var2, int var3, StringBuilder var4, IBypassFormatter var5, IHtmlStyle var6);
}
