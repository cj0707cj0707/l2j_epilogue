package org.l2jmobius.gameserver.geoengine.util;

public class GridLineIterator2D {
   private int _currentX;
   private int _currentY;
   private final int _targetX;
   private final int _targetY;
   private final int _stepX;
   private final int _stepY;
   private final int _deltaX;
   private final int _deltaY;
   private final boolean _isSteep;
   private int _accumulatedError;
   private boolean _hasStarted;

   public GridLineIterator2D(int startX, int startY, int endX, int endY) {
      this._currentX = startX;
      this._currentY = startY;
      this._targetX = endX;
      this._targetY = endY;
      this._deltaX = Math.abs(endX - startX);
      this._deltaY = Math.abs(endY - startY);
      this._stepX = Integer.compare(endX, startX);
      this._stepY = Integer.compare(endY, startY);
      this._isSteep = this._deltaY > this._deltaX;
      this._accumulatedError = (this._isSteep ? this._deltaY : this._deltaX) / 2;
      this._hasStarted = false;
   }

   public boolean next() {
      if (!this._hasStarted) {
         this._hasStarted = true;
         return true;
      } else if (this._currentX == this._targetX && this._currentY == this._targetY) {
         return false;
      } else {
         if (this._isSteep) {
            this._currentY = this._currentY + this._stepY;
            this._accumulatedError = this._accumulatedError - this._deltaX;
            if (this._accumulatedError < 0) {
               this._currentX = this._currentX + this._stepX;
               this._accumulatedError = this._accumulatedError + this._deltaY;
            }
         } else {
            this._currentX = this._currentX + this._stepX;
            this._accumulatedError = this._accumulatedError - this._deltaY;
            if (this._accumulatedError < 0) {
               this._currentY = this._currentY + this._stepY;
               this._accumulatedError = this._accumulatedError + this._deltaX;
            }
         }

         return true;
      }
   }

   public int x() {
      return this._currentX;
   }

   public int y() {
      return this._currentY;
   }

   @Override
   public String toString() {
      return "[" + this._currentX + ", " + this._currentY + "]";
   }
}
