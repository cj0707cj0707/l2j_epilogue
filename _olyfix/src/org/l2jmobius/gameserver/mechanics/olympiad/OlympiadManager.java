package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;

class OlympiadManager implements Runnable {
   protected static final Logger _log = Logger.getLogger(OlympiadManager.class.getName());
   private final Map<Integer, OlympiadGame> _olympiadInstances = new HashMap<>();
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

   public static OlympiadManager getInstance() {
      return OlympiadManager.SingletonHolder._instance;
   }

   @Override
   public synchronized void run() {
      if (!Olympiad.getInstance().isOlympiadEnd()) {
         Map<Integer, OlympiadGameTask> gamesQueue = new HashMap<>();

         while (Olympiad.getInstance().inCompPeriod()) {
            if (Olympiad.getNobleCount() == 0) {
               try {
                  this.wait(60000L);
               } catch (InterruptedException var15) {
                  return;
               }
            } else {
               int gamesQueueSize = 0;
               List<Integer> readyClasses = Olympiad.hasEnoughRegisteredClassed();
               boolean readyNonClassed = Olympiad.hasEnoughRegisteredNonClassed();
               if (readyClasses != null || readyNonClassed) {
                  for (int i = 0;
                     i < STADIUMS.length
                        && (
                           this.existNextOpponents(Olympiad.getRegisteredNonClassBased())
                              || this.existNextOpponents(this.getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))
                        );
                     i++
                  ) {
                     if (!STADIUMS[i].isFreeToUse()) {
                        if (gamesQueue.get(i) == null || gamesQueue.get(i).isTerminated() || gamesQueue.get(i)._game == null) {
                           try {
                              this._olympiadInstances.remove(i);
                              gamesQueue.remove(i);
                              STADIUMS[i].setStadiaFree();
                              i--;
                           } catch (Exception var14) {
                              _log.log(Level.WARNING, "Exception on OlympiadManager.run(): " + var14.getMessage(), (Throwable)var14);
                           }
                        }
                     } else if (i < STADIUMS.length / 2) {
                        if (readyNonClassed && this.existNextOpponents(Olympiad.getRegisteredNonClassBased())) {
                           try {
                              this._olympiadInstances
                                 .put(i, new OlympiadGame(i, CompetitionType.NON_CLASSED, this.nextOpponents(Olympiad.getRegisteredNonClassBased())));
                              gamesQueue.put(i, new OlympiadGameTask(this._olympiadInstances.get(i)));
                              STADIUMS[i].setStadiaBusy();
                           } catch (Exception var18) {
                              if (this._olympiadInstances.get(i) != null) {
                                 for (Player player : this._olympiadInstances.get(i).getPlayers()) {
                                    player.sendMessage("Your olympiad registration was canceled due to an error");
                                    player.setInOlympiadMode(false);
                                    player.setOlympiadStart(false);
                                    player.setOlympiadSide(-1);
                                    player.setOlympiadGameId(-1);
                                 }

                                 this._olympiadInstances.remove(i);
                              }

                              if (gamesQueue.get(i) != null) {
                                 gamesQueue.remove(i);
                              }

                              STADIUMS[i].setStadiaFree();
                              i--;
                           }
                        } else if (readyClasses != null && this.existNextOpponents(this.getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))) {
                           try {
                              this._olympiadInstances
                                 .put(
                                    i,
                                    new OlympiadGame(
                                       i,
                                       CompetitionType.CLASSED,
                                       this.nextOpponents(this.getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))
                                    )
                                 );
                              gamesQueue.put(i, new OlympiadGameTask(this._olympiadInstances.get(i)));
                              STADIUMS[i].setStadiaBusy();
                           } catch (Exception var16) {
                              if (this._olympiadInstances.get(i) != null) {
                                 for (Player player : this._olympiadInstances.get(i).getPlayers()) {
                                    player.sendMessage("Your olympiad registration was canceled due to an error");
                                    player.setInOlympiadMode(false);
                                    player.setOlympiadStart(false);
                                    player.setOlympiadSide(-1);
                                    player.setOlympiadGameId(-1);
                                 }

                                 this._olympiadInstances.remove(i);
                              }

                              if (gamesQueue.get(i) != null) {
                                 gamesQueue.remove(i);
                              }

                              STADIUMS[i].setStadiaFree();
                              i--;
                           }
                        }
                     } else if (readyClasses != null && this.existNextOpponents(this.getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))) {
                        try {
                           this._olympiadInstances
                              .put(
                                 i,
                                 new OlympiadGame(
                                    i, CompetitionType.CLASSED, this.nextOpponents(this.getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))
                                 )
                              );
                           gamesQueue.put(i, new OlympiadGameTask(this._olympiadInstances.get(i)));
                           STADIUMS[i].setStadiaBusy();
                        } catch (Exception var19) {
                           if (this._olympiadInstances.get(i) != null) {
                              for (Player player : this._olympiadInstances.get(i).getPlayers()) {
                                 player.sendMessage("Your olympiad registration was canceled due to an error");
                                 player.setInOlympiadMode(false);
                                 player.setOlympiadStart(false);
                                 player.setOlympiadSide(-1);
                                 player.setOlympiadGameId(-1);
                              }

                              this._olympiadInstances.remove(i);
                           }

                           if (gamesQueue.get(i) != null) {
                              gamesQueue.remove(i);
                           }

                           STADIUMS[i].setStadiaFree();
                           i--;
                        }
                     } else if (readyNonClassed && this.existNextOpponents(Olympiad.getRegisteredNonClassBased())) {
                        try {
                           this._olympiadInstances
                              .put(i, new OlympiadGame(i, CompetitionType.NON_CLASSED, this.nextOpponents(Olympiad.getRegisteredNonClassBased())));
                           gamesQueue.put(i, new OlympiadGameTask(this._olympiadInstances.get(i)));
                           STADIUMS[i].setStadiaBusy();
                        } catch (Exception var17) {
                           if (this._olympiadInstances.get(i) != null) {
                              for (Player player : this._olympiadInstances.get(i).getPlayers()) {
                                 player.sendMessage("Your olympiad registration was canceled due to an error");
                                 player.setInOlympiadMode(false);
                                 player.setOlympiadStart(false);
                                 player.setOlympiadSide(-1);
                                 player.setOlympiadGameId(-1);
                              }

                              this._olympiadInstances.remove(i);
                           }

                           if (gamesQueue.get(i) != null) {
                              gamesQueue.remove(i);
                           }

                           STADIUMS[i].setStadiaFree();
                           i--;
                        }
                     }
                  }

                  gamesQueueSize = gamesQueue.size();

                  for (int ix = 0; ix < gamesQueueSize; ix++) {
                     if (gamesQueue.get(ix) != null && !gamesQueue.get(ix).isTerminated() && !gamesQueue.get(ix).isStarted()) {
                        Thread thread = new Thread(gamesQueue.get(ix));
                        thread.start();
                     }

                     try {
                        this.wait(1000L);
                     } catch (InterruptedException var13) {
                        return;
                     }
                  }
               }

               try {
                  this.wait(30000L);
               } catch (InterruptedException var12) {
                  return;
               }
            }
         }

         boolean allGamesTerminated = false;

         while (!allGamesTerminated) {
            try {
               this.wait(30000L);
            } catch (InterruptedException var11) {
            }

            if (gamesQueue.isEmpty()) {
               allGamesTerminated = true;
            } else {
               for (OlympiadGameTask game : gamesQueue.values()) {
                  allGamesTerminated = allGamesTerminated || game.isTerminated();
               }
            }
         }

         gamesQueue.clear();
         this._olympiadInstances.clear();
         Olympiad.clearRegistered();
         OlympiadGame._battleStarted = false;
         AntiFeedManager.getInstance().clear(1);
      }
   }

   protected OlympiadGame getOlympiadGame(int index) {
      return this._olympiadInstances != null && !this._olympiadInstances.isEmpty() ? this._olympiadInstances.get(index) : null;
   }

   protected void removeGame(OlympiadGame game) {
      if (this._olympiadInstances != null && !this._olympiadInstances.isEmpty()) {
         for (int i = 0; i < this._olympiadInstances.size(); i++) {
            if (this._olympiadInstances.get(i) == game) {
               this._olympiadInstances.remove(i);
            }
         }
      }
   }

   protected Map<Integer, OlympiadGame> getOlympiadGames() {
      return this._olympiadInstances == null ? null : this._olympiadInstances;
   }

   protected List<Player> getRandomClassList(Map<Integer, List<Player>> list, List<Integer> classList) {
      return list != null && classList != null && !list.isEmpty() && !classList.isEmpty() ? list.get(classList.get(Rnd.get(classList.size()))) : null;
   }

   protected List<Player> nextOpponents(List<Player> list) {
      List<Player> opponents = new ArrayList<>();
      if (list.isEmpty()) {
         return opponents;
      } else {
         int loopCount = list.size() / 2;
         if (loopCount < 1) {
            return opponents;
         } else {
            int first = Rnd.get(list.size());
            opponents.add(list.get(first));
            list.remove(first);
            int second = Rnd.get(list.size());
            opponents.add(list.get(second));
            list.remove(second);
            return opponents;
         }
      }
   }

   protected boolean existNextOpponents(List<Player> list) {
      if (list == null) {
         return false;
      } else if (list.isEmpty()) {
         return false;
      } else {
         int loopCount = list.size() >> 1;
         return loopCount >= 1;
      }
   }

   protected Map<Integer, String> getAllTitles() {
      Map<Integer, String> titles = new HashMap<>();

      for (OlympiadGame instance : this._olympiadInstances.values()) {
         if (instance._gamestarted) {
            titles.put(instance._stadiumID, instance.getTitle());
         }
      }

      return titles;
   }

   private static class SingletonHolder {
      protected static final OlympiadManager _instance = new OlympiadManager();
   }
}
