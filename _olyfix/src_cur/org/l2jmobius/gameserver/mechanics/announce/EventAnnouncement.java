package org.l2jmobius.gameserver.mechanics.announce;

import java.util.Date;
import org.l2jmobius.gameserver.managers.IdManager;

public class EventAnnouncement implements IAnnouncement {
   private final int _id;
   private Date _startDate = null;
   private Date _endDate = null;
   private String _content;

   public EventAnnouncement(Date startDate, Date endDate, String content) {
      this._id = IdManager.getInstance().getNextId();
      this._startDate = startDate;
      this._endDate = endDate;
      this._content = content;
   }

   @Override
   public int getId() {
      return this._id;
   }

   @Override
   public AnnouncementType getType() {
      return AnnouncementType.EVENT;
   }

   @Override
   public void setType(AnnouncementType type) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isValid() {
      Date now = new Date();
      return this._startDate != null && this._endDate != null && !now.before(this._startDate) && !now.after(this._endDate);
   }

   @Override
   public String getContent() {
      return this._content;
   }

   @Override
   public void setContent(String content) {
      this._content = content;
   }

   @Override
   public String getAuthor() {
      return "N/A";
   }

   @Override
   public void setAuthor(String author) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean deleteMe() {
      IdManager.getInstance().releaseId(this._id);
      return true;
   }

   @Override
   public boolean storeMe() {
      return true;
   }

   @Override
   public boolean updateMe() {
      throw new UnsupportedOperationException();
   }
}
