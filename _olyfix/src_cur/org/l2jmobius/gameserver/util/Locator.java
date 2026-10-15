package org.l2jmobius.gameserver.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

public class Locator {
   private Locator() {
   }

   public static File getClassSource(Class<?> c) {
      String classResource = c.getName().replace('.', '/') + ".class";
      return getResourceSource(c.getClassLoader(), classResource);
   }

   public static File getResourceSource(ClassLoader classLoader, String resource) {
      ClassLoader c = classLoader;
      if (classLoader == null) {
         c = Locator.class.getClassLoader();
      }

      URL url = null;
      if (c == null) {
         url = ClassLoader.getSystemResource(resource);
      } else {
         url = c.getResource(resource);
      }

      if (url != null) {
         String u = url.toString();
         if (u.startsWith("jar:file:")) {
            int pling = u.indexOf(33);
            String jarName = u.substring(4, pling);
            return new File(fromURI(jarName));
         }

         if (u.startsWith("file:")) {
            int tail = u.indexOf(resource);
            String dirName = u.substring(0, tail);
            return new File(fromURI(dirName));
         }
      }

      return null;
   }

   public static String fromURI(String uriValue) {
      String uri = uriValue;
      URL url = null;

      try {
         url = URI.create(uri).toURL();
      } catch (MalformedURLException var6) {
      }

      if (url != null && "file".equals(url.getProtocol())) {
         StringBuilder buf = new StringBuilder(url.getHost());
         if (!buf.isEmpty()) {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
         }

         String file = url.getFile();
         int queryPos = file.indexOf(63);
         buf.append(queryPos < 0 ? file : file.substring(0, queryPos));
         uri = buf.toString().replace('/', File.separatorChar);
         if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2 && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(58) > -1) {
            uri = uri.substring(1);
         }

         return decodeUri(uri);
      } else {
         throw new IllegalArgumentException("Can only handle valid file: URIs");
      }
   }

   private static String decodeUri(String uri) {
      if (uri.indexOf(37) == -1) {
         return uri;
      } else {
         StringBuilder sb = new StringBuilder();
         CharacterIterator iter = new StringCharacterIterator(uri);

         for (char c = iter.first(); c != '\uffff'; c = iter.next()) {
            if (c == '%') {
               char c1 = iter.next();
               if (c1 != '\uffff') {
                  int i1 = Character.digit(c1, 16);
                  char c2 = iter.next();
                  if (c2 != '\uffff') {
                     int i2 = Character.digit(c2, 16);
                     sb.append((char)((i1 << 4) + i2));
                  }
               }
            } else {
               sb.append(c);
            }
         }

         return sb.toString();
      }
   }

   public static File getToolsJar() {
      boolean toolsJarAvailable = false;

      try {
         Class.forName("com.sun.tools.javac.Main");
         toolsJarAvailable = true;
      } catch (Exception var4) {
         try {
            Class.forName("sun.tools.javac.Main");
            toolsJarAvailable = true;
         } catch (Exception var3) {
         }
      }

      if (toolsJarAvailable) {
         return null;
      } else {
         String javaHome = System.getProperty("java.home");
         if (javaHome.toLowerCase(Locale.US).endsWith("jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
         }

         File toolsJar = new File(javaHome + "/lib/tools.jar");
         if (!toolsJar.exists()) {
            System.out.println("Unable to locate tools.jar. Expected to find it in " + toolsJar.getPath());
            return null;
         } else {
            return toolsJar;
         }
      }
   }

   public static URL[] getLocationURLs(File location) throws MalformedURLException {
      return getLocationURLs(location, new String[]{".jar"});
   }

   public static URL[] getLocationURLs(File location, String[] extensions) throws MalformedURLException {
      URL[] urls = new URL[0];
      if (!location.exists()) {
         return urls;
      } else if (!location.isDirectory()) {
         urls = new URL[1];
         String path = location.getPath();

         for (String extension : extensions) {
            if (path.toLowerCase().endsWith(extension)) {
               urls[0] = location.toURI().toURL();
               break;
            }
         }

         return urls;
      } else {
         File[] matches = location.listFiles((var1x, name) -> {
            for (String extensionx : extensions) {
               if (name.toLowerCase().endsWith(extensionx)) {
                  return true;
               }
            }

            return false;
         });
         urls = new URL[matches.length];

         for (int i = 0; i < matches.length; i++) {
            urls[i] = matches[i].toURI().toURL();
         }

         return urls;
      }
   }
}
