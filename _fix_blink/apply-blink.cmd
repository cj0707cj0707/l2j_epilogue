@echo off
rem Применяет ядерный патч 13.09.2026 (фикс Blink, скилл 1448 Soul Hound:
rem self-эффекты скилла применяются даже при пустом списке целей).
rem Выполнять ПРИ ОСТАНОВЛЕННОМ GameServer.
setlocal
set JDK=C:\Program Files\BellSoft\LibericaJDK-25\bin
set SRV=C:\EpilogueServer
set FIX=%SRV%\_fix_blink

rem 1) Backup the current JAR (only once, so the original is never lost).
if not exist "%FIX%\GameServer.jar.pre-blink" (
  copy /Y "%SRV%\libs\GameServer.jar" "%FIX%\GameServer.jar.pre-blink"
  if errorlevel 1 (
    echo FAILED: cannot create backup of libs\GameServer.jar
    pause
    exit /b 1
  )
)

rem 2) Compile the patched class against the deployed JAR (if build output is missing).
if not exist "%FIX%\build\org\l2jmobius\gameserver\entity\actor\Creature.class" (
  if exist "%FIX%\build" rmdir /S /Q "%FIX%\build"
  mkdir "%FIX%\build"
  "%JDK%\javac.exe" -g -parameters -encoding UTF-8 -classpath "%SRV%\libs\GameServer.jar" -d "%FIX%\build" "%FIX%\Creature.java"
  if errorlevel 1 (
    echo FAILED: compilation error - JAR not modified.
    pause
    exit /b 1
  )
)

rem 3) Update the JAR with the patched class file.
"%JDK%\jar.exe" uf "%SRV%\libs\GameServer.jar" -C "%FIX%\build" "org/l2jmobius/gameserver/entity/actor/Creature.class"
if errorlevel 1 (
  echo FAILED: libs\GameServer.jar not updated - is the GameServer still running?
  pause
  exit /b 1
)

echo OK: libs\GameServer.jar updated (blink patch, 13.09.2026).
echo Rollback if needed: copy /Y "%FIX%\GameServer.jar.pre-blink" "%SRV%\libs\GameServer.jar" (GameServer stopped).
pause
