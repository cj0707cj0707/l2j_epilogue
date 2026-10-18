@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
if not exist "log" mkdir "log"
set "GUARD_LOG=%CD%\log\debuff-guard-install.log"
set "JAVA_EXE=java"
if not defined JAVA_HOME goto java_ready
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_EXE%" goto java_ready
set "JAVA_EXE=%JAVA_HOME%\java.exe"
:java_ready
>"%GUARD_LOG%" echo Debuff category guard installation / verification
>>"%GUARD_LOG%" echo Working directory: "%CD%"
>>"%GUARD_LOG%" echo Java executable: "%JAVA_EXE%"
"%JAVA_EXE%" -version >>"%GUARD_LOG%" 2>&1
"%JAVA_EXE%" --class-path "." -Dfile.encoding=UTF-8 "core-patches\debuff-no-refresh\InstallDebuffGuard.java" "..\libs\GameServer.jar" "core-patches\debuff-no-refresh\DebuffCategoryGuard.java" >>"%GUARD_LOG%" 2>&1
set "GUARD_RESULT=%ERRORLEVEL%"
>>"%GUARD_LOG%" echo Installer exit code: %GUARD_RESULT%
if not "%GUARD_RESULT%"=="0" goto guard_failed
"%JAVA_EXE%" --class-path "." -Dfile.encoding=UTF-8 "core-patches\arena-boundary\InstallArenaBoundary.java" "..\libs\GameServer.jar" "core-patches\arena-boundary\ArenaBoundaryGuard.java" >>"%GUARD_LOG%" 2>&1
set "GUARD_RESULT=%ERRORLEVEL%"
if not "%GUARD_RESULT%"=="0" goto guard_failed
"%JAVA_EXE%" --class-path "." -Dfile.encoding=UTF-8 "core-patches\olympiad-diversity\InstallOlympiadDiversity.java" "..\libs\GameServer.jar" "core-patches\olympiad-diversity\OlympiadDiversity.java" >>"%GUARD_LOG%" 2>&1
set "GUARD_RESULT=%ERRORLEVEL%"
if not "%GUARD_RESULT%"=="0" goto guard_failed
"%JAVA_EXE%" --class-path "." -Dfile.encoding=UTF-8 "core-patches\observer-cleanup\InstallObserverCleanup.java" "..\libs\GameServer.jar" "core-patches" >>"%GUARD_LOG%" 2>&1
set "GUARD_RESULT=%ERRORLEVEL%"
if not "%GUARD_RESULT%"=="0" goto guard_failed
"%JAVA_EXE%" --class-path "." -Dfile.encoding=UTF-8 "core-patches\olympiad-battle-timer\InstallOlympiadBattleTimer.java" "..\libs\GameServer.jar" "core-patches" >>"%GUARD_LOG%" 2>&1
set "GUARD_RESULT=%ERRORLEVEL%"
if "%GUARD_RESULT%"=="0" exit /b 0
:guard_failed
type "%GUARD_LOG%"
exit /b %GUARD_RESULT%
