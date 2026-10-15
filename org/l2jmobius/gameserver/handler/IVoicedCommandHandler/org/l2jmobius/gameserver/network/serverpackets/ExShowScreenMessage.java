package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ExShowScreenMessage extends ServerPacket {
   public static final byte TOP_LEFT = 1;
   public static final byte TOP_CENTER = 2;
   public static final byte TOP_RIGHT = 3;
   public static final byte MIDDLE_LEFT = 4;
   public static final byte MIDDLE_CENTER = 5;
   public static final byte MIDDLE_RIGHT = 6;
   public static final byte BOTTOM_CENTER = 7;
   public static final byte BOTTOM_RIGHT = 8;
   private final int _type;
   private final int _sysMessageId;
   private final int _unk1;
   private final int _unk2;
   private final int _unk3;
   private final boolean _fade;
   private final int _size;
   private final int _position;
   private final boolean _effect;
   private final String _text;
   private final int _time;
   private List<String> _parameters;

   public ExShowScreenMessage(String text, int time) {
      this._type = 1;
      this._sysMessageId = -1;
      this._unk1 = 0;
      this._unk2 = 0;
      this._unk3 = 0;
      this._fade = false;
      this._position = 2;
      this._text = text;
      this._time = time;
      this._size = 0;
      this._effect = false;
   }

   public ExShowScreenMessage(String text, int position, int time) {
      this._type = 1;
      this._sysMessageId = -1;
      this._unk1 = 0;
      this._unk2 = 0;
      this._unk3 = 0;
      this._fade = false;
      this._position = position;
      this._text = text;
      this._time = time;
      this._size = 0;
      this._effect = false;
   }

   public ExShowScreenMessage(String text, int position, int time, int size, boolean fade, boolean showEffect) {
      this._type = 1;
      this._sysMessageId = -1;
      this._unk1 = 0;
      this._unk2 = 0;
      this._unk3 = 0;
      this._fade = fade;
      this._position = position;
      this._text = text;
      this._time = time;
      this._size = size;
      this._effect = showEffect;
   }

   public ExShowScreenMessage(SystemMessageId systemMsg, int position, int time, String... params) {
      this._type = 2;
      this._sysMessageId = systemMsg.getId();
      this._unk1 = 0;
      this._unk2 = 0;
      this._unk3 = 0;
      this._fade = false;
      this._position = position;
      this._text = null;
      this._time = time;
      this._size = 0;
      this._effect = false;
      if (params != null) {
         this.addStringParameter(params);
      }
   }

   public ExShowScreenMessage(
      int type, int messageId, int position, int unk1, int size, int unk2, int unk3, boolean showEffect, int time, boolean fade, String text, String params
   ) {
      this._type = type;
      this._sysMessageId = messageId;
      this._unk1 = unk1;
      this._unk2 = unk2;
      this._unk3 = unk3;
      this._fade = fade;
      this._position = position;
      this._text = text;
      this._time = time;
      this._size = size;
      this._effect = showEffect;
   }

   public void addStringParameter(String... params) {
      if (this._parameters == null) {
         this._parameters = new ArrayList<>();
      }

      for (String param : params) {
         this._parameters.add(param);
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_SCREEN_MESSAGE.writeId(this, buffer);
      if (MultilingualSupportConfig.MULTILANG_ENABLE) {
         Player player = client.getPlayer();
         if (player != null) {
            String lang = player.getLang();
            if (lang != null && !lang.equals("en") && this._sysMessageId > -1) {
               SystemMessageId sm = SystemMessageId.getSystemMessageId(this._sysMessageId);
               if (sm != null) {
                  SystemMessageId.SMLocalisation sml = sm.getLocalisation(lang);
                  if (sml != null) {
                     buffer.writeInt(this._type);
                     buffer.writeInt(-1);
                     buffer.writeInt(this._position);
                     buffer.writeInt(this._unk1);
                     buffer.writeInt(this._size);
                     buffer.writeInt(this._unk2);
                     buffer.writeInt(this._unk3);
                     buffer.writeInt(this._effect);
                     buffer.writeInt(this._time);
                     buffer.writeInt(this._fade);
                     buffer.writeInt(-1);
                     buffer.writeString(sml.getLocalisation(this._parameters != null ? this._parameters : Collections.emptyList()));
                     return;
                  }
               }
            }
         }
      }

      buffer.writeInt(this._type);
      buffer.writeInt(this._sysMessageId);
      buffer.writeInt(this._position);
      buffer.writeInt(this._unk1);
      buffer.writeInt(this._size);
      buffer.writeInt(this._unk2);
      buffer.writeInt(this._unk3);
      buffer.writeInt(this._effect);
      buffer.writeInt(this._time);
      buffer.writeInt(this._fade);
      buffer.writeString(this._text);
      if (this._sysMessageId == -1) {
         buffer.writeString(this._text);
      } else if (this._parameters != null) {
         for (String s : this._parameters) {
            buffer.writeString(s);
         }
      }
   }
}
