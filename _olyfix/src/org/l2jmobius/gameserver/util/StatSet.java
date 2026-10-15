package org.l2jmobius.gameserver.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public class StatSet {
   private static final Logger LOGGER = Logger.getLogger(StatSet.class.getName());
   public static final StatSet EMPTY_STATSET = new StatSet(Collections.emptyMap());
   private final Map<String, Object> _set;

   public StatSet() {
      this(HashMap::new);
   }

   public StatSet(Supplier<Map<String, Object>> mapFactory) {
      this(mapFactory.get());
   }

   public StatSet(Map<String, Object> map) {
      this._set = map;
   }

   public Map<String, Object> getSet() {
      return this._set;
   }

   public void merge(StatSet newSet) {
      this._set.putAll(newSet.getSet());
   }

   public boolean isEmpty() {
      return this._set.isEmpty();
   }

   public boolean getBoolean(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Boolean value required, but not specified");
      } else if (val instanceof Boolean) {
         return (Boolean)val;
      } else {
         try {
            return Boolean.parseBoolean((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Boolean value required, but found: " + val);
         }
      }
   }

   public boolean getBoolean(String key, boolean defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Boolean) {
         return (Boolean)val;
      } else {
         try {
            return Boolean.parseBoolean((String)val);
         } catch (Exception var5) {
            return defaultValue;
         }
      }
   }

   public byte getByte(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Byte value required, but not specified");
      } else if (val instanceof Number) {
         return ((Number)val).byteValue();
      } else {
         try {
            return Byte.parseByte((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Byte value required, but found: " + val);
         }
      }
   }

   public byte getByte(String key, byte defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).byteValue();
      } else {
         try {
            return Byte.parseByte((String)val);
         } catch (Exception var5) {
            throw new IllegalArgumentException("Byte value required, but found: " + val);
         }
      }
   }

   public short increaseByte(String key, byte increaseWith) {
      byte newValue = (byte)(this.getByte(key) + increaseWith);
      this.set(key, newValue);
      return newValue;
   }

   public short increaseByte(String key, byte defaultValue, byte increaseWith) {
      byte newValue = (byte)(this.getByte(key, defaultValue) + increaseWith);
      this.set(key, newValue);
      return newValue;
   }

   public byte[] getByteArray(String key, String splitOn) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Byte value required, but not specified");
      } else if (val instanceof Number) {
         return new byte[]{((Number)val).byteValue()};
      } else {
         int c = 0;
         String[] vals = ((String)val).split(splitOn);
         byte[] result = new byte[vals.length];

         for (String v : vals) {
            try {
               result[c++] = Byte.parseByte(v);
            } catch (Exception var12) {
               throw new IllegalArgumentException("Byte value required, but found: " + val);
            }
         }

         return result;
      }
   }

   public List<Byte> getByteList(String key, String splitOn) {
      List<Byte> result = new ArrayList<>();
      byte[] var4 = this.getByteArray(key, splitOn);
      int var5 = var4.length;

      for (int var6 = 0; var6 < var5; var6++) {
         Byte i = var4[var6];
         result.add(i);
      }

      return result;
   }

   public short getShort(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Short value required, but not specified");
      } else if (val instanceof Number) {
         return ((Number)val).shortValue();
      } else {
         try {
            return Short.parseShort((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Short value required, but found: " + val);
         }
      }
   }

   public short getShort(String key, short defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).shortValue();
      } else {
         try {
            return Short.parseShort((String)val);
         } catch (Exception var5) {
            throw new IllegalArgumentException("Short value required, but found: " + val);
         }
      }
   }

   public short increaseShort(String key, short increaseWith) {
      short newValue = (short)(this.getShort(key) + increaseWith);
      this.set(key, newValue);
      return newValue;
   }

   public short increaseShort(String key, short defaultValue, short increaseWith) {
      short newValue = (short)(this.getShort(key, defaultValue) + increaseWith);
      this.set(key, newValue);
      return newValue;
   }

   public int getInt(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Integer value required, but not specified: " + key + "!");
      } else if (val instanceof Number) {
         return ((Number)val).intValue();
      } else {
         try {
            return Integer.parseInt((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Integer value required, but found: " + val + "!");
         }
      }
   }

   public int getInt(String key, int defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).intValue();
      } else {
         try {
            return Integer.parseInt((String)val);
         } catch (Exception var5) {
            throw new IllegalArgumentException("Integer value required, but found: " + val);
         }
      }
   }

   public int increaseInt(String key, int increaseWith) {
      int newValue = this.getInt(key) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public int increaseInt(String key, int defaultValue, int increaseWith) {
      int newValue = this.getInt(key, defaultValue) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public int[] getIntArray(String key, String splitOn) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Integer value required, but not specified");
      } else if (val instanceof Number) {
         return new int[]{((Number)val).intValue()};
      } else {
         int c = 0;
         String[] vals = ((String)val).split(splitOn);
         int[] result = new int[vals.length];

         for (String v : vals) {
            try {
               result[c++] = Integer.parseInt(v);
            } catch (Exception var12) {
               throw new IllegalArgumentException("Integer value required, but found: " + val);
            }
         }

         return result;
      }
   }

   public List<Integer> getIntegerList(String key) {
      String val = this.getString(key, null);
      List<Integer> result;
      if (val != null) {
         String[] splitVal = val.split(",");
         result = new ArrayList<>(splitVal.length);

         for (String split : splitVal) {
            result.add(Integer.parseInt(split));
         }
      } else {
         result = new ArrayList<>(1);
      }

      return result;
   }

   public void setIntegerList(String key, List<Integer> list) {
      if (key != null) {
         if (list != null && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for (int element : list) {
               sb.append(element);
               sb.append(',');
            }

            sb.deleteCharAt(sb.length() - 1);
            this.set(key, sb.toString());
         } else {
            this.remove(key);
         }
      }
   }

   public Map<Integer, Integer> getIntegerMap(String key) {
      String val = this.getString(key, null);
      Map<Integer, Integer> result;
      if (val != null) {
         String[] splitVal = val.split(",");
         result = new HashMap<>(splitVal.length + 1);

         for (String split : splitVal) {
            String[] entry = split.split("-");
            result.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[1]));
         }
      } else {
         result = new HashMap<>(1);
      }

      return result;
   }

   public void setIntegerMap(String key, Map<Integer, Integer> map) {
      if (key != null) {
         if (map != null && !map.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            for (Entry<Integer, Integer> entry : map.entrySet()) {
               sb.append(entry.getKey());
               sb.append('-');
               sb.append(entry.getValue());
               sb.append(',');
            }

            sb.deleteCharAt(sb.length() - 1);
            this.set(key, sb.toString());
         } else {
            this.remove(key);
         }
      }
   }

   public long getLong(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Long value required, but not specified");
      } else if (val instanceof Number) {
         return ((Number)val).longValue();
      } else {
         try {
            return Long.parseLong((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Long value required, but found: " + val);
         }
      }
   }

   public long getLong(String key, long defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).longValue();
      } else {
         try {
            return Long.parseLong((String)val);
         } catch (Exception var6) {
            throw new IllegalArgumentException("Long value required, but found: " + val);
         }
      }
   }

   public long increaseLong(String key, long increaseWith) {
      long newValue = this.getLong(key) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public long increaseLong(String key, long defaultValue, long increaseWith) {
      long newValue = this.getLong(key, defaultValue) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public float getFloat(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Float value required, but not specified");
      } else if (val instanceof Number) {
         return ((Number)val).floatValue();
      } else {
         try {
            return Float.parseFloat((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Float value required, but found: " + val);
         }
      }
   }

   public float getFloat(String key, float defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).floatValue();
      } else {
         try {
            return Float.parseFloat((String)val);
         } catch (Exception var5) {
            throw new IllegalArgumentException("Float value required, but found: " + val);
         }
      }
   }

   public float increaseFloat(String key, float increaseWith) {
      float newValue = this.getFloat(key) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public float increaseFloat(String key, float defaultValue, float increaseWith) {
      float newValue = this.getFloat(key, defaultValue) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public double getDouble(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Double value required, but not specified");
      } else if (val instanceof Number) {
         return ((Number)val).doubleValue();
      } else {
         try {
            return Double.parseDouble((String)val);
         } catch (Exception var4) {
            throw new IllegalArgumentException("Double value required, but found: " + val);
         }
      }
   }

   public double getDouble(String key, double defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (val instanceof Number) {
         return ((Number)val).doubleValue();
      } else {
         try {
            return Double.parseDouble((String)val);
         } catch (Exception var6) {
            throw new IllegalArgumentException("Double value required, but found: " + val);
         }
      }
   }

   public double increaseDouble(String key, double increaseWith) {
      double newValue = this.getDouble(key) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public double increaseDouble(String key, double defaultValue, double increaseWith) {
      double newValue = this.getDouble(key, defaultValue) + increaseWith;
      this.set(key, newValue);
      return newValue;
   }

   public String getString(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("String value required, but not specified");
      } else {
         return String.valueOf(val);
      }
   }

   public String getString(String key, String defaultValue) {
      Object val = this._set.get(key);
      return val == null ? defaultValue : String.valueOf(val);
   }

   public Duration getDuration(String key) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("String value required, but not specified");
      } else {
         return TimeUtil.parseDuration(String.valueOf(val));
      }
   }

   public Duration getDuration(String key, Duration defaultValue) {
      Object val = this._set.get(key);
      return val == null ? defaultValue : TimeUtil.parseDuration(String.valueOf(val));
   }

   public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass) {
      Object val = this._set.get(key);
      if (val == null) {
         throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
      } else if (enumClass.isInstance(val)) {
         return (T)val;
      } else {
         try {
            return Enum.valueOf(enumClass, String.valueOf(val));
         } catch (Exception var5) {
            throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
         }
      }
   }

   public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T defaultValue) {
      Object val = this._set.get(key);
      if (val == null) {
         return defaultValue;
      } else if (enumClass.isInstance(val)) {
         return (T)val;
      } else {
         try {
            return Enum.valueOf(enumClass, String.valueOf(val));
         } catch (Exception var6) {
            throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
         }
      }
   }

   public <A> A getObject(String name, Class<A> type) {
      Object obj = this._set.get(name);
      return (A)(obj != null && type.isAssignableFrom(obj.getClass()) ? obj : null);
   }

   public <A> A getObject(String name, Class<A> type, A defaultValue) {
      Object obj = this._set.get(name);
      return (A)(obj != null && type.isAssignableFrom(obj.getClass()) ? obj : defaultValue);
   }

   public SkillHolder getSkillHolder(String key) {
      Object obj = this._set.get(key);
      return !(obj instanceof SkillHolder) ? null : (SkillHolder)obj;
   }

   public Location getLocation(String key) {
      Object obj = this._set.get(key);
      return !(obj instanceof Location) ? null : (Location)obj;
   }

   public List<MinionHolder> getMinionList(String key) {
      Object obj = this._set.get(key);
      return !(obj instanceof List) ? Collections.emptyList() : (List)obj;
   }

   public <T> List<T> getList(String key, Class<T> clazz) {
      Object obj = this._set.get(key);
      if (!(obj instanceof List<Object> originalList)) {
         return null;
      } else if (originalList.isEmpty() || originalList.stream().allMatch(clazz::isInstance)) {
         return (List<T>)obj;
      } else if (clazz.getSuperclass() == Enum.class) {
         throw new IllegalAccessError("Please use getEnumList if you want to get list of Enums!");
      } else {
         List<T> convertedList = this.convertList(originalList, clazz);
         if (convertedList == null) {
            LOGGER.log(
               Level.WARNING,
               "getList(\"" + key + "\", " + clazz.getSimpleName() + ") requested with wrong generic type: " + obj.getClass().getGenericInterfaces()[0] + "!",
               (Throwable)(new ClassNotFoundException())
            );
            return null;
         } else {
            this._set.put(key, convertedList);
            return convertedList;
         }
      }
   }

   public <T> List<T> getList(String key, Class<T> clazz, List<T> defaultValue) {
      List<T> list = this.getList(key, clazz);
      return list == null ? defaultValue : list;
   }

   public <T extends Enum<T>> List<T> getEnumList(String key, Class<T> clazz) {
      Object obj = this._set.get(key);
      if (!(obj instanceof List<Object> originalList)) {
         return null;
      } else if (!originalList.isEmpty()
         && obj.getClass().getGenericInterfaces()[0] != clazz
         && originalList.stream().allMatch(name -> StringUtil.isEnum(name.toString(), clazz))) {
         List<T> convertedList = originalList.stream().map(Object::toString).map(name -> Enum.valueOf(clazz, name)).map(clazz::cast).toList();
         this._set.put(key, convertedList);
         return convertedList;
      } else {
         return (List<T>)obj;
      }
   }

   private <T> List<T> convertList(List<Object> originalList, Class<T> clazz) {
      if (clazz == Integer.class) {
         if (originalList.stream().map(Object::toString).allMatch(StringUtil::isInteger)) {
            return originalList.stream().map(Object::toString).map(Integer::valueOf).map(clazz::cast).toList();
         }
      } else if (clazz == Float.class) {
         if (originalList.stream().map(Object::toString).allMatch(StringUtil::isFloat)) {
            return originalList.stream().map(Object::toString).map(Float::valueOf).map(clazz::cast).toList();
         }
      } else if (clazz == Double.class && originalList.stream().map(Object::toString).allMatch(StringUtil::isDouble)) {
         return originalList.stream().map(Object::toString).map(Double::valueOf).map(clazz::cast).toList();
      }

      return null;
   }

   public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass) {
      Object obj = this._set.get(key);
      if (!(obj instanceof Map<?, ?> originalList)) {
         return null;
      } else {
         if (!originalList.isEmpty()
            && (!originalList.keySet().stream().allMatch(keyClass::isInstance) || !originalList.values().stream().allMatch(valueClass::isInstance))) {
            LOGGER.log(
               Level.WARNING,
               "getMap(\""
                  + key
                  + "\", "
                  + keyClass.getSimpleName()
                  + ", "
                  + valueClass.getSimpleName()
                  + ") requested with wrong generic type: "
                  + obj.getClass().getGenericInterfaces()[0]
                  + "!",
               (Throwable)(new ClassNotFoundException())
            );
         }

         return (Map<K, V>)obj;
      }
   }

   public void set(String name, Object value) {
      if (value != null) {
         this._set.put(name, value);
      }
   }

   public void set(String name, boolean value) {
      this._set.put(name, value);
   }

   public void set(String key, byte value) {
      this._set.put(key, value);
   }

   public void set(String key, short value) {
      this._set.put(key, value);
   }

   public void set(String key, int value) {
      this._set.put(key, value);
   }

   public void set(String key, long value) {
      this._set.put(key, value);
   }

   public void set(String key, float value) {
      this._set.put(key, value);
   }

   public void set(String key, double value) {
      this._set.put(key, value);
   }

   public void set(String key, String value) {
      if (value != null) {
         this._set.put(key, value);
      }
   }

   public void set(String key, Enum<?> value) {
      if (value != null) {
         this._set.put(key, value);
      }
   }

   public void remove(String key) {
      this._set.remove(key);
   }

   public boolean contains(String name) {
      return this._set.containsKey(name);
   }

   @Override
   public String toString() {
      return "StatSet{_set=" + this._set + "}";
   }
}
