package org.l2jmobius.gameserver.geoengine.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class NodeBuffer {
   private static final int MAX_ITERATIONS = 7000;
   private static final int COST_SCALE = 1000;
   private static final int Z_TOLERANCE = 64;
   private static final int Z_STEP_LIMIT = 16;
   private static final byte FLAG_TOUCHED = 1;
   private static final byte FLAG_OPEN = 2;
   private static final byte FLAG_CLOSED = 4;
   private final int _mapSize;
   private final int _capacity;
   private final int _wLow;
   private final int _wMedium;
   private final int _wHigh;
   private final int _wDiagonal;
   private final int _hStraight;
   private final int _hDiagonalExcess;
   private final boolean _diagonalStrategy;
   private final int[] _parent;
   private final int[] _gCost;
   private final int[] _hCost;
   private final int[] _fCost;
   private final byte[] _flags;
   private final byte[] _nswe;
   private final short[] _z;
   private final int[] _touched;
   private int _touchedCount;
   private final int[] _heap;
   private final int[] _heapPos;
   private int _heapSize;
   private int _baseX;
   private int _baseY;
   private int _targetX;
   private int _targetY;
   private int _targetZ;

   public NodeBuffer(int size) {
      this._mapSize = size;
      this._capacity = size * size;
      this._parent = new int[this._capacity];
      this._gCost = new int[this._capacity];
      this._hCost = new int[this._capacity];
      this._fCost = new int[this._capacity];
      this._flags = new byte[this._capacity];
      this._nswe = new byte[this._capacity];
      this._z = new short[this._capacity];
      this._touched = new int[this._capacity];
      this._heap = new int[this._capacity];
      this._heapPos = new int[this._capacity];
      this._wLow = (int)(GeoEngineConfig.LOW_WEIGHT * 1000.0F);
      this._wMedium = (int)(GeoEngineConfig.MEDIUM_WEIGHT * 1000.0F);
      this._wHigh = (int)(GeoEngineConfig.HIGH_WEIGHT * 1000.0F);
      this._wDiagonal = (int)(GeoEngineConfig.DIAGONAL_WEIGHT * 1000.0F);
      this._hStraight = this._wLow;
      this._hDiagonalExcess = this._wDiagonal - this._wLow;
      this._diagonalStrategy = GeoEngineConfig.ADVANCED_DIAGONAL_STRATEGY;
   }

   public int getMapSize() {
      return this._mapSize;
   }

   public List<GeoLocation> findPath(int x, int y, int z, int tx, int ty, int tz) {
      this._baseX = x + (tx - x - this._mapSize) / 2;
      this._baseY = y + (ty - y - this._mapSize) / 2;
      this._targetX = tx;
      this._targetY = ty;
      this._targetZ = tz;
      int startIdx = this.cellIdx(x, y);
      if (startIdx < 0) {
         return null;
      } else {
         this.touch(startIdx, x, y, z);
         this._gCost[startIdx] = 0;
         this._fCost[startIdx] = this._hCost[startIdx];
         this.heapPush(startIdx);

         for (int count = 0; count < 7000; count++) {
            if (this._heapSize == 0) {
               return null;
            }

            int currIdx = this.heapPoll();
            this._flags[currIdx] = (byte)(this._flags[currIdx] & -3);
            this._flags[currIdx] = (byte)(this._flags[currIdx] | 4);
            int currX = this.idxToX(currIdx);
            int currY = this.idxToY(currIdx);
            int currZ = this._z[currIdx];
            if (currX == this._targetX && currY == this._targetY && Math.abs(currZ - this._targetZ) < 64) {
               return this.constructPath(currIdx);
            }

            this.expandNeighbors(currIdx, currX, currY, currZ);
         }

         return null;
      }
   }

   public void free() {
      for (int i = 0; i < this._touchedCount; i++) {
         this._flags[this._touched[i]] = 0;
      }

      this._touchedCount = 0;
      this._heapSize = 0;
   }

   private int cellIdx(int gx, int gy) {
      int aX = gx - this._baseX;
      if (aX >= 0 && aX < this._mapSize) {
         int aY = gy - this._baseY;
         return aY >= 0 && aY < this._mapSize ? aY * this._mapSize + aX : -1;
      } else {
         return -1;
      }
   }

   private int idxToX(int idx) {
      return this._baseX + idx % this._mapSize;
   }

   private int idxToY(int idx) {
      return this._baseY + idx / this._mapSize;
   }

   private void touch(int idx, int gx, int gy, int refZ) {
      if ((this._flags[idx] & 1) == 0) {
         GeoEngine geo = GeoEngine.getInstance();
         byte nswe = 0;
         if (geo.checkNearestNswe(gx, gy, refZ, 8)) {
            nswe = (byte)(nswe | 8);
         }

         if (geo.checkNearestNswe(gx, gy, refZ, 1)) {
            nswe = (byte)(nswe | 1);
         }

         if (geo.checkNearestNswe(gx, gy, refZ, 4)) {
            nswe = (byte)(nswe | 4);
         }

         if (geo.checkNearestNswe(gx, gy, refZ, 2)) {
            nswe = (byte)(nswe | 2);
         }

         this._nswe[idx] = nswe;
         this._z[idx] = (short)geo.getNearestZ(gx, gy, refZ);
         this._flags[idx] = 1;
         this._parent[idx] = -1;
         this._gCost[idx] = Integer.MAX_VALUE;
         this._hCost[idx] = this.computeH(gx, gy);
         this._fCost[idx] = Integer.MAX_VALUE;
         this._heapPos[idx] = -1;
         this._touched[this._touchedCount++] = idx;
      }
   }

   private int computeH(int gx, int gy) {
      int dx = Math.abs(gx - this._targetX);
      int dy = Math.abs(gy - this._targetY);
      int maxD = Math.max(dx, dy);
      int minD = Math.min(dx, dy);
      return maxD * this._hStraight + minD * this._hDiagonalExcess;
   }

   private void expandNeighbors(int currIdx, int x, int y, int z) {
      byte nswe = this._nswe[currIdx];
      if (nswe != 0) {
         int idxE = -1;
         int idxS = -1;
         int idxW = -1;
         int idxN = -1;
         if ((nswe & 1) != 0) {
            idxE = this.considerNeighbor(currIdx, x + 1, y, z, false);
         }

         if ((nswe & 4) != 0) {
            idxS = this.considerNeighbor(currIdx, x, y + 1, z, false);
         }

         if ((nswe & 2) != 0) {
            idxW = this.considerNeighbor(currIdx, x - 1, y, z, false);
         }

         if ((nswe & 8) != 0) {
            idxN = this.considerNeighbor(currIdx, x, y - 1, z, false);
         }

         if (this._diagonalStrategy) {
            if (idxE >= 0 && idxS >= 0 && (this._nswe[idxE] & 4) != 0 && (this._nswe[idxS] & 1) != 0) {
               this.considerNeighbor(currIdx, x + 1, y + 1, z, true);
            }

            if (idxS >= 0 && idxW >= 0 && (this._nswe[idxW] & 4) != 0 && (this._nswe[idxS] & 2) != 0) {
               this.considerNeighbor(currIdx, x - 1, y + 1, z, true);
            }

            if (idxN >= 0 && idxE >= 0 && (this._nswe[idxE] & 8) != 0 && (this._nswe[idxN] & 1) != 0) {
               this.considerNeighbor(currIdx, x + 1, y - 1, z, true);
            }

            if (idxN >= 0 && idxW >= 0 && (this._nswe[idxW] & 8) != 0 && (this._nswe[idxN] & 2) != 0) {
               this.considerNeighbor(currIdx, x - 1, y - 1, z, true);
            }
         }
      }
   }

   private int considerNeighbor(int parentIdx, int nx, int ny, int parentZ, boolean diagonal) {
      int idx = this.cellIdx(nx, ny);
      if (idx < 0) {
         return -1;
      } else {
         this.touch(idx, nx, ny, parentZ);
         if ((this._flags[idx] & 4) != 0) {
            return idx;
         } else {
            int neighborZ = this._z[idx];
            int stepZ = Math.abs(neighborZ - parentZ);
            int weight;
            if ((this._nswe[idx] & 15) == 15 && stepZ <= 16) {
               if (!this.isHighWeight(nx + 1, ny, neighborZ)
                  && !this.isHighWeight(nx - 1, ny, neighborZ)
                  && !this.isHighWeight(nx, ny + 1, neighborZ)
                  && !this.isHighWeight(nx, ny - 1, neighborZ)) {
                  weight = diagonal ? this._wDiagonal : this._wLow;
               } else {
                  weight = this._wMedium;
               }
            } else {
               weight = this._wHigh;
            }

            int newGCost = this._gCost[parentIdx] + weight;
            boolean inOpen = (this._flags[idx] & 2) != 0;
            if (inOpen && newGCost >= this._gCost[idx]) {
               return idx;
            } else {
               this._parent[idx] = parentIdx;
               this._gCost[idx] = newGCost;
               this._fCost[idx] = newGCost + this._hCost[idx];
               if (inOpen) {
                  this.heapSiftUp(this._heapPos[idx]);
               } else {
                  this.heapPush(idx);
               }

               return idx;
            }
         }
      }
   }

   private boolean isHighWeight(int gx, int gy, int refZ) {
      int idx = this.cellIdx(gx, gy);
      if (idx < 0) {
         return true;
      } else {
         this.touch(idx, gx, gy, refZ);
         return (this._nswe[idx] & 15) != 15 || Math.abs(this._z[idx] - refZ) > 16;
      }
   }

   private void heapPush(int idx) {
      int pos = this._heapSize++;
      this._heap[pos] = idx;
      this._heapPos[idx] = pos;
      this._flags[idx] = (byte)(this._flags[idx] | 2);
      this.heapSiftUp(pos);
   }

   private int heapPoll() {
      int root = this._heap[0];
      this._heapPos[root] = -1;
      int lastPos = --this._heapSize;
      if (lastPos > 0) {
         int moved = this._heap[lastPos];
         this._heap[0] = moved;
         this._heapPos[moved] = 0;
         this.heapSiftDown(0);
      }

      return root;
   }

   private void heapSiftUp(int startPos) {
      int pos = startPos;
      int idx = this._heap[startPos];
      int fCost = this._fCost[idx];
      int hCost = this._hCost[idx];

      while (pos > 0) {
         int parentPos = pos - 1 >>> 1;
         int parentIdx = this._heap[parentPos];
         int parentF = this._fCost[parentIdx];
         if (fCost > parentF || fCost == parentF && hCost >= this._hCost[parentIdx]) {
            break;
         }

         this._heap[pos] = parentIdx;
         this._heapPos[parentIdx] = pos;
         pos = parentPos;
      }

      this._heap[pos] = idx;
      this._heapPos[idx] = pos;
   }

   private void heapSiftDown(int startPos) {
      int pos = startPos;
      int idx = this._heap[startPos];
      int fCost = this._fCost[idx];
      int hCost = this._hCost[idx];
      int half = this._heapSize >>> 1;

      while (pos < half) {
         int childPos = (pos << 1) + 1;
         int childIdx = this._heap[childPos];
         int childF = this._fCost[childIdx];
         int rightPos = childPos + 1;
         if (rightPos < this._heapSize) {
            int rightIdx = this._heap[rightPos];
            int rightF = this._fCost[rightIdx];
            if (rightF < childF || rightF == childF && this._hCost[rightIdx] < this._hCost[childIdx]) {
               childPos = rightPos;
               childIdx = rightIdx;
               childF = rightF;
            }
         }

         if (fCost < childF || fCost == childF && hCost <= this._hCost[childIdx]) {
            break;
         }

         this._heap[pos] = childIdx;
         this._heapPos[childIdx] = pos;
         pos = childPos;
      }

      this._heap[pos] = idx;
      this._heapPos[idx] = pos;
   }

   private List<GeoLocation> constructPath(int endIdx) {
      List<GeoLocation> reversed = new ArrayList<>();
      int prevDirX = Integer.MIN_VALUE;
      int prevDirY = Integer.MIN_VALUE;
      int curr = endIdx;

      while (this._parent[curr] != -1) {
         int parent = this._parent[curr];
         int grand = this._parent[parent];
         int currX = this.idxToX(curr);
         int currY = this.idxToY(curr);
         int parentX = this.idxToX(parent);
         int parentY = this.idxToY(parent);
         int dirX;
         int dirY;
         if (!this._diagonalStrategy && grand != -1) {
            int tmpX = currX - this.idxToX(grand);
            int tmpY = currY - this.idxToY(grand);
            if (Math.abs(tmpX) == Math.abs(tmpY)) {
               dirX = tmpX;
               dirY = tmpY;
            } else {
               dirX = currX - parentX;
               dirY = currY - parentY;
            }
         } else {
            dirX = currX - parentX;
            dirY = currY - parentY;
         }

         if (dirX != prevDirX || dirY != prevDirY) {
            prevDirX = dirX;
            prevDirY = dirY;
            reversed.add(new GeoLocation(currX, currY, this._z[curr]));
         }

         curr = parent;
      }

      Collections.reverse(reversed);
      return reversed;
   }
}
