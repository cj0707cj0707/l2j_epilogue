@echo off
rem Применяет ядерный патч 07.09.2026 (Olympiad.setNoblePoints — добор очков ботам для олимпиады).
rem Выполнять ПРИ ОСТАНОВЛЕННОМ GameServer.
copy /Y "C:\EpilogueServer\_olyfix\GameServer.jar.points" "C:\EpilogueServer\libs\GameServer.jar"
if errorlevel 1 (
  echo FAILED: libs\GameServer.jar не заменён - GameServer ещё запущен?
  pause
) else (
  echo OK: libs\GameServer.jar обновлён ^(oly-points patch^).
)
