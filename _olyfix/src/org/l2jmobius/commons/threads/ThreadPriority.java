package org.l2jmobius.commons.threads;

public enum ThreadPriority {
   PRIORITY_1(1),
   PRIORITY_2(2),
   PRIORITY_3(3),
   PRIORITY_4(4),
   PRIORITY_5(5),
   PRIORITY_6(6),
   PRIORITY_7(7),
   PRIORITY_8(8),
   PRIORITY_9(9),
   PRIORITY_10(10);

   private final int _id;

   private ThreadPriority(int id) {
      this._id = id;
   }

   public int getId() {
      return this._id;
   }
}
