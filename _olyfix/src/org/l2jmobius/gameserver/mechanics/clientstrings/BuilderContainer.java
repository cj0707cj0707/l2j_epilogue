package org.l2jmobius.gameserver.mechanics.clientstrings;

public class BuilderContainer extends Builder {
   private final Builder[] _builders;

   BuilderContainer(Builder[] builders) {
      this._builders = builders;
   }

   @Override
   public String toString(Object param) {
      return this.toString(param);
   }

   @Override
   public String toString(Object... params) {
      int buildersLength = this._builders.length;
      int paramsLength = params.length;
      String[] builds = new String[buildersLength];
      int buildTextLen = 0;
      if (paramsLength != 0) {
         int i = buildersLength;

         while (i-- > 0) {
            Builder builder = this._builders[i];
            int paramIndex = builder.getIndex();
            String build = paramIndex != -1 && paramIndex < paramsLength ? builder.toString(params[paramIndex]) : builder.toString();
            buildTextLen += build.length();
            builds[i] = build;
         }
      } else {
         int i = buildersLength;

         while (i-- > 0) {
            String build = this._builders[i].toString();
            buildTextLen += build.length();
            builds[i] = build;
         }
      }

      StringBuilder sb = new StringBuilder(buildTextLen);

      for (int var13 = 0; var13 < buildersLength; var13++) {
         sb.append(builds[var13]);
      }

      return sb.toString();
   }

   @Override
   public int getIndex() {
      return -1;
   }
}
