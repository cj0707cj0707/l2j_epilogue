package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class MessageDeletionTaskManager implements Runnable {
   private static final Map<Integer, Long> PENDING_MESSAGES = new ConcurrentHashMap<>();
   private static boolean _working = false;

   protected MessageDeletionTaskManager() {
      ThreadPool.scheduleAtFixedRate(this, 10000L, 10000L);
   }

   @Override
   public void run() {
      if (!_working) {
         _working = true;
         if (!PENDING_MESSAGES.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Entry<Integer, Long>> iterator = PENDING_MESSAGES.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Integer, Long> entry = iterator.next();
               if (currentTime > entry.getValue()) {
                  Integer messageId = entry.getKey();
                  MailMessage message = MailManager.getInstance().getMessage(messageId);
                  if (message == null) {
                     iterator.remove();
                  } else {
                     if (message.hasAttachments()) {
                        Player sender = World.getPlayer(message.getSenderId());
                        Player receiver = World.getPlayer(message.getReceiverId());
                        if (sender != null) {
                           message.getAttachments().returnToWh(sender.getWarehouse());
                           sender.sendPacket(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME);
                        } else if (message.getSenderId() == -1) {
                           if (receiver != null) {
                              message.getAttachments().returnToWh(receiver.getWarehouse());
                           }
                        } else {
                           message.getAttachments().returnToWh(null);
                        }

                        message.getAttachments().deleteMe();
                        message.removeAttachments();
                        if (receiver != null) {
                           receiver.sendPacket(new SystemMessage(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME));
                        }
                     }

                     MailManager.getInstance().deleteMessageInDb(messageId);
                     iterator.remove();
                  }
               }
            }
         }

         _working = false;
      }
   }

   public void add(int msgId, long deletionTime) {
      PENDING_MESSAGES.put(msgId, deletionTime);
   }

   public static MessageDeletionTaskManager getInstance() {
      return MessageDeletionTaskManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final MessageDeletionTaskManager INSTANCE = new MessageDeletionTaskManager();
   }
}
