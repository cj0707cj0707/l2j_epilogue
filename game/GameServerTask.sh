#!/bin/bash

# exit codes of GameServer:
#  0 normal shutdown
#  2 reboot attempt

cd -- "$(dirname -- "$0")" || exit 1
mkdir -p log || exit 1

# Install/verify before launch; do not run an unpatched or unknown core.
{ java -version; java --class-path "." -Dfile.encoding=UTF-8 core-patches/debuff-no-refresh/InstallDebuffGuard.java ../libs/GameServer.jar core-patches/debuff-no-refresh/DebuffCategoryGuard.java; } > log/debuff-guard-install.log 2>&1 || { cat log/debuff-guard-install.log >&2; exit 1; }

java --class-path "." -Dfile.encoding=UTF-8 core-patches/arena-boundary/InstallArenaBoundary.java ../libs/GameServer.jar core-patches/arena-boundary/ArenaBoundaryGuard.java >> log/debuff-guard-install.log 2>&1 || { cat log/debuff-guard-install.log >&2; exit 1; }

java --class-path "." -Dfile.encoding=UTF-8 core-patches/olympiad-diversity/InstallOlympiadDiversity.java ../libs/GameServer.jar core-patches/olympiad-diversity/OlympiadDiversity.java >> log/debuff-guard-install.log 2>&1 || { cat log/debuff-guard-install.log >&2; exit 1; }

java --class-path "." -Dfile.encoding=UTF-8 core-patches/observer-cleanup/InstallObserverCleanup.java ../libs/GameServer.jar core-patches >> log/debuff-guard-install.log 2>&1 || { cat log/debuff-guard-install.log >&2; exit 1; }

java --class-path "." -Dfile.encoding=UTF-8 core-patches/olympiad-battle-timer/InstallOlympiadBattleTimer.java ../libs/GameServer.jar core-patches >> log/debuff-guard-install.log 2>&1 || { cat log/debuff-guard-install.log >&2; exit 1; }

while :; do
	[ -f log/java0.log.0 ] && mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
	[ -f log/stdout.log ] && mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
	java $(cat "java.cfg") -jar ../libs/GameServer.jar > log/stdout.log 2>&1
	[ $? -ne 2 ] && break
#	/etc/init.d/mysql restart
	sleep 10
done
