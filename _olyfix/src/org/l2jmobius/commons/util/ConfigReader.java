package org.l2jmobius.commons.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;
import org.l2jmobius.commons.time.TimeUtil;

public class ConfigReader {
   private static final Logger LOGGER = Logger.getLogger(ConfigReader.class.getName());
   private final Properties _properties = new Properties();
   private final File _file;

   public ConfigReader(String filePath) {
      this._file = new File(filePath);
      if (!Files.exists(this._file.toPath())) {
         LOGGER.warning("Configuration file not found: " + this._file.getAbsolutePath());
      } else {
         try (
            InputStream input = Files.newInputStream(this._file.toPath());
            InputStreamReader reader = new InputStreamReader(input, Charset.defaultCharset());
         ) {
            this._properties.load(reader);
         } catch (IOException var10) {
            LOGGER.warning("Failed to load configurations from " + this._file.getName() + ": " + var10.getMessage());
         }
      }
   }

   public boolean containsKey(String config) {
      return this._properties.containsKey(config);
   }

   public String getValue(String config) {
      return this._properties.getProperty(config);
   }

   public Collection<String> getStringPropertyNames() {
      return this._properties.stringPropertyNames();
   }

   public boolean getBoolean(String config, boolean defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Boolean.parseBoolean(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid boolean for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public byte getByte(String config, byte defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Byte.parseByte(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid byte for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public short getShort(String config, short defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Short.parseShort(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid short for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public int getInt(String config, int defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Integer.parseInt(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid int for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public long getLong(String config, long defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Long.parseLong(value);
         } catch (Exception var6) {
            LOGGER.warning("Invalid long for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public float getFloat(String config, float defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Float.parseFloat(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid float for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public double getDouble(String config, double defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Double.parseDouble(value);
         } catch (Exception var6) {
            LOGGER.warning("Invalid double for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public String getString(String config, String defaultValue) {
      String value = this._properties.getProperty(config);
      if (value == null) {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         return defaultValue;
      } else {
         return value;
      }
   }

   public <T extends Enum<T>> T getEnum(String config, Class<T> clazz, T defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Enum.valueOf(clazz, value);
         } catch (Exception var6) {
            LOGGER.warning("Invalid enum for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return defaultValue;
   }

   public Duration getDuration(String config, String defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return TimeUtil.parseDuration(value);
         } catch (Exception var5) {
            LOGGER.warning("Invalid duration for config '" + config + "' in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default: " + defaultValue + ".");
      }

      return TimeUtil.parseDuration(defaultValue);
   }

   public int[] getIntArray(String config, String delimiter, String defaultValue) {
      String value = this._properties.getProperty(config);
      if (value != null) {
         try {
            return Arrays.stream(value.split(delimiter)).map(String::trim).mapToInt(Integer::parseInt).toArray();
         } catch (NumberFormatException var7) {
            LOGGER.warning("Invalid int array for config '" + config + "' in file '" + this._file.getName() + "', using default values.");
         }
      } else {
         LOGGER.warning("Config '" + config + "' not found in file '" + this._file.getName() + "', using default values.");
      }

      try {
         return Arrays.stream(defaultValue.split(delimiter)).map(String::trim).mapToInt(Integer::parseInt).toArray();
      } catch (NumberFormatException var6) {
         LOGGER.warning("Invalid default values for config '" + config + "' in file '" + this._file.getName() + "', using empty array.");
         return new int[0];
      }
   }
}
