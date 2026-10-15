package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager.SingletonHolder;

class OlympiadManager implements Runnable {
   protected static final Logger _log = Logger.getLogger(org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager.class.getName());
   private final Map<Integer, org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame> _olympiadInstances = new HashMap<>();
   private final Map<Integer, Integer> _lastOpponentIds = new HashMap<>();
   protected static final OlympiadStadium[] STADIUMS = new OlympiadStadium[]{
      new OlympiadStadium(-88000, -252637, -3331, 17100001, 17100002),
      new OlympiadStadium(-83760, -252637, -3331, 17100003, 17100004),
      new OlympiadStadium(-79600, -252637, -3331, 17100005, 17100006),
      new OlympiadStadium(-75648, -252637, -3331, 17100007, 17100008),
      new OlympiadStadium(-88000, -249762, -3331, 17100009, 17100010),
      new OlympiadStadium(-83760, -249762, -3331, 17100011, 17100012),
      new OlympiadStadium(-79600, -249762, -3331, 17100013, 17100014),
      new OlympiadStadium(-75648, -249762, -3331, 17100015, 17100016),
      new OlympiadStadium(-88000, -246951, -3331, 17100017, 17100018),
      new OlympiadStadium(-83760, -246951, -3331, 17100019, 17100020),
      new OlympiadStadium(-79600, -246951, -3331, 17100021, 17100022),
      new OlympiadStadium(-75648, -246951, -3331, 17100023, 17100024),
      new OlympiadStadium(-88000, -244290, -3331, 17100025, 17100026),
      new OlympiadStadium(-83760, -244290, -3331, 17100027, 17100028),
      new OlympiadStadium(-79600, -244290, -3331, 17100029, 17100030),
      new OlympiadStadium(-75648, -244290, -3331, 17100031, 17100032),
      new OlympiadStadium(-88000, -241490, -3331, 17100033, 17100034),
      new OlympiadStadium(-83760, -241490, -3331, 17100035, 17100036),
      new OlympiadStadium(-79600, -241490, -3331, 17100037, 17100038),
      new OlympiadStadium(-75648, -241490, -3331, 17100039, 17100040),
      new OlympiadStadium(-88000, -238825, -3331, 17100041, 17100042),
      new OlympiadStadium(-83760, -238825, -3331, 17100043, 17100044)
   };

   public static org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager getInstance() {
      return SingletonHolder._instance;
   }

   @Override
   public synchronized void run() {
      if (!org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getInstance().isOlympiadEnd()) {
         HashMap var1 = new HashMap();

         while (org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getInstance().inCompPeriod()) {
            try {
               HashSet var2 = new HashSet();

               for (org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame var4 : this._olympiadInstances.values()) {
                  Player[] var5 = var4.getPlayers();
                  if (var5 != null) {
                     for (Player var9 : var5) {
                        if (var9 != null) {
                           var2.add(var9.getObjectId());
                        }
                     }
                  }
               }

               if (!var2.isEmpty()) {
                  org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased().removeIf(var1x -> var2.contains(var1x.getObjectId()));

                  for (List var27 : org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased().values()) {
                     var27.removeIf(var1x -> var2.contains(var1x.getObjectId()));
                  }
               }
            } catch (Exception var16) {
               _log.warning("OlympiadManager: registration purge failed: " + var16);
            }

            if (org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getNobleCount() == 0) {
               try {
                  this.wait(60000L);
               } catch (InterruptedException var15) {
                  return;
               }
            } else {
               int var21 = 0;
               List var25 = org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.hasEnoughRegisteredClassed();
               boolean var28 = org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.hasEnoughRegisteredNonClassed();
               if (var25 != null || var28) {
                  for (int var30 = 0;
                     var30 < STADIUMS.length
                        && (
                           this.existNextOpponents(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased())
                              || this.existNextOpponents(
                                 this.getRandomClassList(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased(), var25)
                              )
                        );
                     var30++
                  ) {
                     if (!STADIUMS[var30].isFreeToUse()) {
                        if (var1.get(var30) == null
                           || ((org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask)var1.get(var30)).isTerminated()
                           || ((org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask)var1.get(var30))._game == null) {
                           try {
                              this._olympiadInstances.remove(var30);
                              var1.remove(var30);
                              STADIUMS[var30].setStadiaFree();
                              var30--;
                           } catch (Exception var14) {
                              _log.log(Level.WARNING, "Exception on OlympiadManager.run(): " + var14.getMessage(), (Throwable)var14);
                           }
                        }
                     } else if (var30 < STADIUMS.length / 2) {
                        if (var28 && this.existNextOpponents(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased())) {
                           try {
                              this._olympiadInstances
                                 .put(
                                    var30,
                                    new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame(
                                       var30,
                                       CompetitionType.NON_CLASSED,
                                       this.nextOpponents(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased())
                                    )
                                 );
                              var1.put(var30, new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask(this._olympiadInstances.get(var30)));
                              STADIUMS[var30].setStadiaBusy();
                           } catch (Exception var19) {
                              if (this._olympiadInstances.get(var30) != null) {
                                 for (Player var47 : this._olympiadInstances.get(var30).getPlayers()) {
                                    var47.sendMessage("Your olympiad registration was canceled due to an error");
                                    var47.setInOlympiadMode(false);
                                    var47.setOlympiadStart(false);
                                    var47.setOlympiadSide(-1);
                                    var47.setOlympiadGameId(-1);
                                 }

                                 this._olympiadInstances.remove(var30);
                              }

                              if (var1.get(var30) != null) {
                                 var1.remove(var30);
                              }

                              STADIUMS[var30].setStadiaFree();
                              var30--;
                           }
                        } else if (var25 != null
                           && this.existNextOpponents(
                              this.getRandomClassList(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased(), var25)
                           )) {
                           try {
                              this._olympiadInstances
                                 .put(
                                    var30,
                                    new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame(
                                       var30,
                                       CompetitionType.CLASSED,
                                       this.nextOpponents(
                                          this.getRandomClassList(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased(), var25)
                                       )
                                    )
                                 );
                              var1.put(var30, new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask(this._olympiadInstances.get(var30)));
                              STADIUMS[var30].setStadiaBusy();
                           } catch (Exception var17) {
                              if (this._olympiadInstances.get(var30) != null) {
                                 for (Player var46 : this._olympiadInstances.get(var30).getPlayers()) {
                                    var46.sendMessage("Your olympiad registration was canceled due to an error");
                                    var46.setInOlympiadMode(false);
                                    var46.setOlympiadStart(false);
                                    var46.setOlympiadSide(-1);
                                    var46.setOlympiadGameId(-1);
                                 }

                                 this._olympiadInstances.remove(var30);
                              }

                              if (var1.get(var30) != null) {
                                 var1.remove(var30);
                              }

                              STADIUMS[var30].setStadiaFree();
                              var30--;
                           }
                        }
                     } else if (var25 != null
                        && this.existNextOpponents(
                           this.getRandomClassList(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased(), var25)
                        )) {
                        try {
                           this._olympiadInstances
                              .put(
                                 var30,
                                 new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame(
                                    var30,
                                    CompetitionType.CLASSED,
                                    this.nextOpponents(
                                       this.getRandomClassList(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredClassBased(), var25)
                                    )
                                 )
                              );
                           var1.put(var30, new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask(this._olympiadInstances.get(var30)));
                           STADIUMS[var30].setStadiaBusy();
                        } catch (Exception var20) {
                           if (this._olympiadInstances.get(var30) != null) {
                              for (Player var45 : this._olympiadInstances.get(var30).getPlayers()) {
                                 var45.sendMessage("Your olympiad registration was canceled due to an error");
                                 var45.setInOlympiadMode(false);
                                 var45.setOlympiadStart(false);
                                 var45.setOlympiadSide(-1);
                                 var45.setOlympiadGameId(-1);
                              }

                              this._olympiadInstances.remove(var30);
                           }

                           if (var1.get(var30) != null) {
                              var1.remove(var30);
                           }

                           STADIUMS[var30].setStadiaFree();
                           var30--;
                        }
                     } else if (var28 && this.existNextOpponents(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased())) {
                        try {
                           this._olympiadInstances
                              .put(
                                 var30,
                                 new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame(
                                    var30,
                                    CompetitionType.NON_CLASSED,
                                    this.nextOpponents(org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.getRegisteredNonClassBased())
                                 )
                              );
                           var1.put(var30, new org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask(this._olympiadInstances.get(var30)));
                           STADIUMS[var30].setStadiaBusy();
                        } catch (Exception var18) {
                           if (this._olympiadInstances.get(var30) != null) {
                              for (Player var10 : this._olympiadInstances.get(var30).getPlayers()) {
                                 var10.sendMessage("Your olympiad registration was canceled due to an error");
                                 var10.setInOlympiadMode(false);
                                 var10.setOlympiadStart(false);
                                 var10.setOlympiadSide(-1);
                                 var10.setOlympiadGameId(-1);
                              }

                              this._olympiadInstances.remove(var30);
                           }

                           if (var1.get(var30) != null) {
                              var1.remove(var30);
                           }

                           STADIUMS[var30].setStadiaFree();
                           var30--;
                        }
                     }
                  }

                  var21 = var1.size();

                  for (int var31 = 0; var31 < var21; var31++) {
                     if (var1.get(var31) != null
                        && !((org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask)var1.get(var31)).isTerminated()
                        && !((org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask)var1.get(var31)).isStarted()) {
                        Thread var32 = new Thread((Runnable)var1.get(var31));
                        var32.start();
                     }

                     try {
                        this.wait(1000L);
                     } catch (InterruptedException var13) {
                        return;
                     }
                  }
               }

               try {
                  this.wait(5000L);
               } catch (InterruptedException var12) {
                  return;
               }
            }
         }

         boolean var23 = false;

         while (!var23) {
            try {
               this.wait(30000L);
            } catch (InterruptedException var11) {
            }

            if (var1.isEmpty()) {
               var23 = true;
            } else {
               for (org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask var29 : var1.values()) {
                  var23 = var23 || var29.isTerminated();
               }
            }
         }

         var1.clear();
         this._olympiadInstances.clear();
         org.l2jmobius.gameserver.mechanics.olympiad.Olympiad.clearRegistered();
         org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame._battleStarted = false;
         AntiFeedManager.getInstance().clear(1);
         this._lastOpponentIds.clear();
      }
   }

   protected org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame getOlympiadGame(int var1) {
      return this._olympiadInstances != null && !this._olympiadInstances.isEmpty() ? this._olympiadInstances.get(var1) : null;
   }

   protected void removeGame(org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame var1) {
      if (this._olympiadInstances != null && !this._olympiadInstances.isEmpty()) {
         for (int var2 = 0; var2 < this._olympiadInstances.size(); var2++) {
            if (this._olympiadInstances.get(var2) == var1) {
               this._olympiadInstances.remove(var2);
            }
         }
      }
   }

   protected Map<Integer, org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame> getOlympiadGames() {
      return this._olympiadInstances == null ? null : this._olympiadInstances;
   }

   protected List<Player> getRandomClassList(Map<Integer, List<Player>> var1, List<Integer> var2) {
      return var1 != null && var2 != null && !var1.isEmpty() && !var2.isEmpty() ? (List)var1.get(var2.get(Rnd.get(var2.size()))) : null;
   }

   protected List<Player> nextOpponents(List<Player> var1) {
      ArrayList var2 = new ArrayList();
      if (var1.isEmpty()) {
         return var2;
      } else {
         int var3 = var1.size() / 2;
         if (var3 < 1) {
            return var2;
         } else {
            int var4 = Rnd.get(var1.size());
            Player var5 = (Player)var1.get(var4);
            var2.add(var5);
            var1.remove(var4);
            Integer var6 = this._lastOpponentIds.get(var5.getObjectId());
            ArrayList var7 = new ArrayList();
            ArrayList var8 = new ArrayList();

            for (int var9 = 0; var9 < var1.size(); var9++) {
               var8.add(var9);
               Player var10 = (Player)var1.get(var9);
               if (var6 == null || var10.getObjectId() != var6) {
                  var7.add(var9);
               }
            }

            ArrayList var12 = var7.isEmpty() ? var8 : var7;
            int var13 = (Integer)var12.get(Rnd.get(var12.size()));
            Player var11 = (Player)var1.get(var13);
            var2.add(var11);
            var1.remove(var13);
            this._lastOpponentIds.put(var5.getObjectId(), var11.getObjectId());
            this._lastOpponentIds.put(var11.getObjectId(), var5.getObjectId());
            return var2;
         }
      }
   }

   protected boolean existNextOpponents(List<Player> var1) {
      if (var1 == null) {
         return false;
      } else if (var1.isEmpty()) {
         return false;
      } else {
         int var2 = var1.size() >> 1;
         return var2 >= 1;
      }
   }

   protected Map<Integer, String> getAllTitles() {
      HashMap var1 = new HashMap();

      for (org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame var3 : this._olympiadInstances.values()) {
         if (var3._gamestarted) {
            var1.put(var3._stadiumID, var3.getTitle());
         }
      }

      return var1;
   }
}
