package org.l2jmobius.gameserver.geoengine.util;

public class GridLineIterator3D {
   private int _currentX;
   private int _currentY;
   private int _currentZ;
   private final int _targetX;
   private final int _targetY;
   private final int _targetZ;
   private final int _deltaX;
   private final int _deltaY;
   private final int _deltaZ;
   private final int _stepX;
   private final int _stepY;
   private final int _stepZ;
   private int _accumulatedErrorXY;
   private int _accumulatedErrorXZ;
   private boolean _hasStarted;

   public GridLineIterator3D(int startX, int startY, int startZ, int endX, int endY, int endZ) {
      this._currentX = startX;
      this._currentY = startY;
      this._currentZ = startZ;
      this._targetX = endX;
      this._targetY = endY;
      this._targetZ = endZ;
      this._deltaX = Math.abs(endX - startX);
      this._deltaY = Math.abs(endY - startY);
      this._deltaZ = Math.abs(endZ - startZ);
      this._stepX = startX < endX ? 1 : -1;
      this._stepY = startY < endY ? 1 : -1;
      this._stepZ = startZ < endZ ? 1 : -1;
      if (this._deltaX >= this._deltaY && this._deltaX >= this._deltaZ) {
         this._accumulatedErrorXY = this._accumulatedErrorXZ = this._deltaX / 2;
      } else if (this._deltaY >= this._deltaX && this._deltaY >= this._deltaZ) {
         this._accumulatedErrorXY = this._accumulatedErrorXZ = this._deltaY / 2;
      } else {
         this._accumulatedErrorXY = this._accumulatedErrorXZ = this._deltaZ / 2;
      }

      this._hasStarted = false;
   }

   public boolean next() {
      if (!this._hasStarted) {
         this._hasStarted = true;
         return true;
      } else if (this._currentX == this._targetX && this._currentY == this._targetY && this._currentZ == this._targetZ) {
         return false;
      } else {
         if (this._deltaX >= this._deltaY && this._deltaX >= this._deltaZ) {
            this._currentX = this._currentX + this._stepX;
            this._accumulatedErrorXY = this._accumulatedErrorXY + this._deltaY;
            if (this._accumulatedErrorXY >= this._deltaX) {
               this._currentY = this._currentY + this._stepY;
               this._accumulatedErrorXY = this._accumulatedErrorXY - this._deltaX;
            }

            this._accumulatedErrorXZ = this._accumulatedErrorXZ + this._deltaZ;
            if (this._accumulatedErrorXZ >= this._deltaX) {
               this._currentZ = this._currentZ + this._stepZ;
               this._accumulatedErrorXZ = this._accumulatedErrorXZ - this._deltaX;
            }
         } else if (this._deltaY >= this._deltaX && this._deltaY >= this._deltaZ) {
            this._currentY = this._currentY + this._stepY;
            this._accumulatedErrorXY = this._accumulatedErrorXY + this._deltaX;
            if (this._accumulatedErrorXY >= this._deltaY) {
               this._currentX = this._currentX + this._stepX;
               this._accumulatedErrorXY = this._accumulatedErrorXY - this._deltaY;
            }

            this._accumulatedErrorXZ = this._accumulatedErrorXZ + this._deltaZ;
            if (this._accumulatedErrorXZ >= this._deltaY) {
               this._currentZ = this._currentZ + this._stepZ;
               this._accumulatedErrorXZ = this._accumulatedErrorXZ - this._deltaY;
            }
         } else {
            this._currentZ = this._currentZ + this._stepZ;
            this._accumulatedErrorXY = this._accumulatedErrorXY + this._deltaX;
            if (this._accumulatedErrorXY >= this._deltaZ) {
               this._currentX = this._currentX + this._stepX;
               this._accumulatedErrorXY = this._accumulatedErrorXY - this._deltaZ;
            }

            this._accumulatedErrorXZ = this._accumulatedErrorXZ + this._deltaY;
            if (this._accumulatedErrorXZ >= this._deltaZ) {
               this._currentY = this._currentY + this._stepY;
               this._accumulatedErrorXZ = this._accumulatedErrorXZ - this._deltaZ;
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

   public int z() {
      return this._currentZ;
   }

   @Override
   public String toString() {
      return "[" + this._currentX + ", " + this._currentY + ", " + this._currentZ + "]";
   }
}
