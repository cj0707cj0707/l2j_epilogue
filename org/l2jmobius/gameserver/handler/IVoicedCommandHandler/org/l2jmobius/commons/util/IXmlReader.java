package org.l2jmobius.commons.util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.config.ThreadConfig;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

public interface IXmlReader {
   Logger LOGGER = Logger.getLogger(IXmlReader.class.getName());
   String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
   String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

   void load();

   default void parseDatapackFile(String path) {
      this.parseFile(new File(".", path));
   }

   default void parseFile(File file) {
      if (!this.isValidXmlFile(file)) {
         LOGGER.warning("Cannot parse " + file.getName() + ": file does not exist or is not valid.");
      } else {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true);
         factory.setValidating(this.isValidating());
         factory.setIgnoringComments(true);

         try {
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.parseDocument(builder.parse(file), file);
         } catch (SAXParseException var4) {
            LOGGER.log(
               Level.WARNING,
               "Error parsing " + file.getName() + " at line " + var4.getLineNumber() + ", column " + var4.getColumnNumber() + ".",
               (Throwable)var4
            );
         } catch (Exception var5) {
            LOGGER.log(Level.WARNING, "Error parsing " + file.getName(), (Throwable)var5);
         }
      }
   }

   default boolean parseDirectory(File directory) {
      return this.parseDirectory(directory, false);
   }

   default boolean parseDatapackDirectory(String path, boolean recursive) {
      return this.parseDirectory(new File(".", path), recursive);
   }

   default boolean parseDirectory(File directory, boolean recursive) {
      if (!directory.exists()) {
         LOGGER.warning("Directory not found: " + directory.getAbsolutePath());
         return false;
      } else {
         if (ThreadConfig.THREADS_FOR_LOADING) {
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<?>> tasks = new ArrayList<>();
            File[] files = directory.listFiles();
            if (files != null) {
               for (File file : files) {
                  if (recursive && file.isDirectory()) {
                     this.parseDirectory(file, true);
                  } else if (this.isValidXmlFile(file)) {
                     tasks.add(executorService.schedule(() -> this.parseFile(file), 0L, TimeUnit.MILLISECONDS));
                  }
               }
            }

            for (Future<?> task : tasks) {
               try {
                  task.get();
               } catch (Exception var11) {
                  LOGGER.warning("Failed to parse file: " + var11.getMessage());
               }
            }

            executorService.shutdown();

            try {
               executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException var10) {
               Thread.currentThread().interrupt();
               LOGGER.warning("Parsing process was interrupted: " + var10.getMessage());
            }
         } else {
            File[] files = directory.listFiles();
            if (files != null) {
               for (File filex : files) {
                  if (recursive && filex.isDirectory()) {
                     this.parseDirectory(filex, true);
                  } else if (this.isValidXmlFile(filex)) {
                     this.parseFile(filex);
                  }
               }
            }
         }

         return true;
      }
   }

   void parseDocument(Document var1, File var2);

   default Boolean parseBoolean(Node node, Boolean defaultValue) {
      return node != null ? Boolean.valueOf(node.getNodeValue()) : defaultValue;
   }

   default Boolean parseBoolean(Node node) {
      return this.parseBoolean(node, null);
   }

   default Boolean parseBoolean(NamedNodeMap attributes, String name) {
      return this.parseBoolean(attributes.getNamedItem(name));
   }

   default Boolean parseBoolean(NamedNodeMap attributes, String name, Boolean defaultValue) {
      return this.parseBoolean(attributes.getNamedItem(name), defaultValue);
   }

   default Byte parseByte(Node node, Byte defaultValue) {
      return node != null ? Byte.decode(node.getNodeValue()) : defaultValue;
   }

   default Byte parseByte(Node node) {
      return this.parseByte(node, null);
   }

   default Byte parseByte(NamedNodeMap attributes, String name) {
      return this.parseByte(attributes.getNamedItem(name));
   }

   default Byte parseByte(NamedNodeMap attributes, String name, Byte defaultValue) {
      return this.parseByte(attributes.getNamedItem(name), defaultValue);
   }

   default Short parseShort(Node node, Short defaultValue) {
      return node != null ? Short.decode(node.getNodeValue()) : defaultValue;
   }

   default Short parseShort(Node node) {
      return this.parseShort(node, null);
   }

   default Short parseShort(NamedNodeMap attributes, String name) {
      return this.parseShort(attributes.getNamedItem(name));
   }

   default Short parseShort(NamedNodeMap attributes, String name, Short defaultValue) {
      return this.parseShort(attributes.getNamedItem(name), defaultValue);
   }

   default int parseInt(Node node, Integer defaultValue) {
      return node != null ? Integer.decode(node.getNodeValue()) : defaultValue;
   }

   default int parseInt(Node node) {
      return this.parseInt(node, -1);
   }

   default Integer parseInteger(Node node, Integer defaultValue) {
      return node != null ? Integer.decode(node.getNodeValue()) : defaultValue;
   }

   default Integer parseInteger(Node node) {
      return this.parseInteger(node, null);
   }

   default Integer parseInteger(NamedNodeMap attributes, String name) {
      return this.parseInteger(attributes.getNamedItem(name));
   }

   default Integer parseInteger(NamedNodeMap attributes, String name, Integer defaultValue) {
      return this.parseInteger(attributes.getNamedItem(name), defaultValue);
   }

   default Long parseLong(Node node, Long defaultValue) {
      return node != null ? Long.decode(node.getNodeValue()) : defaultValue;
   }

   default Long parseLong(Node node) {
      return this.parseLong(node, null);
   }

   default Long parseLong(NamedNodeMap attributes, String name) {
      return this.parseLong(attributes.getNamedItem(name));
   }

   default Long parseLong(NamedNodeMap attributes, String name, Long defaultValue) {
      return this.parseLong(attributes.getNamedItem(name), defaultValue);
   }

   default Float parseFloat(Node node, Float defaultValue) {
      return node != null ? Float.valueOf(node.getNodeValue()) : defaultValue;
   }

   default Float parseFloat(Node node) {
      return this.parseFloat(node, null);
   }

   default Float parseFloat(NamedNodeMap attributes, String name) {
      return this.parseFloat(attributes.getNamedItem(name));
   }

   default Float parseFloat(NamedNodeMap attributes, String name, Float defaultValue) {
      return this.parseFloat(attributes.getNamedItem(name), defaultValue);
   }

   default Double parseDouble(Node node, Double defaultValue) {
      return node != null ? Double.valueOf(node.getNodeValue()) : defaultValue;
   }

   default Double parseDouble(Node node) {
      return this.parseDouble(node, null);
   }

   default Double parseDouble(NamedNodeMap attributes, String name) {
      return this.parseDouble(attributes.getNamedItem(name));
   }

   default Double parseDouble(NamedNodeMap attributes, String name, Double defaultValue) {
      return this.parseDouble(attributes.getNamedItem(name), defaultValue);
   }

   default String parseString(Node node, String defaultValue) {
      return node != null ? node.getNodeValue() : defaultValue;
   }

   default String parseString(Node node) {
      return this.parseString(node, null);
   }

   default String parseString(NamedNodeMap attributes, String name) {
      return this.parseString(attributes.getNamedItem(name));
   }

   default String parseString(NamedNodeMap attributes, String name, String defaultValue) {
      return this.parseString(attributes.getNamedItem(name), defaultValue);
   }

   default <T extends Enum<T>> T parseEnum(Node node, Class<T> enumClass, T defaultValue) {
      if (node == null) {
         return defaultValue;
      } else {
         try {
            return Enum.valueOf(enumClass, node.getNodeValue());
         } catch (IllegalArgumentException var5) {
            LOGGER.warning(
               "Invalid value for node: "
                  + node.getNodeName()
                  + ", specified value: "
                  + node.getNodeValue()
                  + " should be an enum of type \""
                  + enumClass.getSimpleName()
                  + "\". Using default value: "
                  + defaultValue
            );
            return defaultValue;
         }
      }
   }

   default <T extends Enum<T>> T parseEnum(Node node, Class<T> enumClass) {
      return this.parseEnum(node, enumClass, null);
   }

   default <T extends Enum<T>> T parseEnum(NamedNodeMap attributes, Class<T> enumClass, String name) {
      return this.parseEnum(attributes.getNamedItem(name), enumClass);
   }

   default <T extends Enum<T>> T parseEnum(NamedNodeMap attributes, Class<T> enumClass, String name, T defaultValue) {
      return this.parseEnum(attributes.getNamedItem(name), enumClass, defaultValue);
   }

   default Map<String, Object> parseAttributes(Node node) {
      NamedNodeMap attributes = node.getAttributes();
      Map<String, Object> attributeMap = new LinkedHashMap<>();

      for (int i = 0; i < attributes.getLength(); i++) {
         Node attribute = attributes.item(i);
         attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
      }

      return attributeMap;
   }

   default void forEach(Node node, Consumer<Node> action) {
      this.forEach(node, var0 -> true, action);
   }

   default void forEach(Node node, String nodeName, Consumer<Node> action) {
      this.forEach(node, child -> nodeName.equalsIgnoreCase(child.getNodeName()), action);
   }

   default void forEach(Node node, Predicate<Node> filter, Consumer<Node> action) {
      NodeList children = node.getChildNodes();

      for (int i = 0; i < children.getLength(); i++) {
         Node childNode = children.item(i);
         if (filter.test(childNode)) {
            action.accept(childNode);
         }
      }
   }

   default boolean isValidXmlFile(File file) {
      return file != null && file.isFile() && file.getName().toLowerCase().endsWith(".xml");
   }

   default boolean isValidating() {
      return true;
   }

   static boolean isNode(Node node) {
      return node.getNodeType() == 1;
   }
}
