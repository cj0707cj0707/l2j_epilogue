@echo off
rem Applies the core patch of 09.09.2026 (no-double-booking olympiad fix) to libs\GameServer.jar.
rem Run ONLY while the GameServer is stopped.
copy /Y "C:\EpilogueServer\_olyfix\GameServer.jar.nodouble" "C:\EpilogueServer\libs\GameServer.jar"
if errorlevel 1 (
  echo FAILED: libs\GameServer.jar not replaced - the GameServer is still running?
  pause
) else (
  echo OK: libs\GameServer.jar updated ^(no-double-booking patch^).
)
