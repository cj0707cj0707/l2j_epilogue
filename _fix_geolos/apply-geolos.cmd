@echo off
rem Applies the core patch of 11.09.2026 ("no line-of-sight obstruction": GeoEngine.canSeeTarget -> true)
rem to libs\GameServer.jar. Run ONLY while the GameServer is stopped.
setlocal
set JDK=C:\Program Files\BellSoft\LibericaJDK-25\bin
set SRV=C:\EpilogueServer
set FIX=%SRV%\_fix_geolos

rem 1) Backup the current JAR (only once, so the original is never lost).
if not exist "%FIX%\GameServer.jar.pre-geolos" (
  copy /Y "%SRV%\libs\GameServer.jar" "%FIX%\GameServer.jar.pre-geolos"
  if errorlevel 1 (
    echo FAILED: cannot create backup of libs\GameServer.jar
    pause
    exit /b 1
  )
)

rem 2) Compile the patched class against the deployed JAR.
if exist "%FIX%\build" rmdir /S /Q "%FIX%\build"
mkdir "%FIX%\build"
"%JDK%\javac.exe" -encoding UTF-8 -classpath "%SRV%\libs\GameServer.jar" -d "%FIX%\build" "%FIX%\GeoEngine.java"
if errorlevel 1 (
  echo FAILED: compilation error - JAR not modified.
  pause
  exit /b 1
)

rem 3) Update the JAR with the patched class files.
"%JDK%\jar.exe" uf "%SRV%\libs\GameServer.jar" -C "%FIX%\build" "org/l2jmobius/gameserver/geoengine/GeoEngine.class" -C "%FIX%\build" "org/l2jmobius/gameserver/geoengine/GeoEngine$SingletonHolder.class"
if errorlevel 1 (
  echo FAILED: libs\GameServer.jar not updated - is the GameServer still running?
  pause
  exit /b 1
)

echo OK: libs\GameServer.jar updated (geolos patch, 11.09.2026).
echo Rollback if needed: copy /Y "%FIX%\GameServer.jar.pre-geolos" "%SRV%\libs\GameServer.jar" (GameServer stopped).
pause
