' Game Server launcher with a visible log window (same as the Login Server).
' The server runs in GUI mode: the "Mobius - GameServer" window shows the live log.
' Closing that window (and confirming "Shutdown") safely shuts the server down in 1 second.

Set shell = WScript.CreateObject("WScript.Shell")
Set fs = CreateObject("Scripting.FileSystemObject")
shell.CurrentDirectory = fs.GetParentFolderName(WScript.ScriptFullName)
Dim javaExe, javaHome
javaHome = shell.Environment("PROCESS").Item("JAVA_HOME")
javaExe = "java"
If javaHome <> "" Then
    javaExe = fs.BuildPath(javaHome, "bin\java.exe")
    If Not fs.FileExists(javaExe) Then javaExe = fs.BuildPath(javaHome, "java.exe")
End If

'Load java.cfg parameters.
Dim parameters
Set file = CreateObject("Scripting.FileSystemObject").OpenTextFile("java.cfg", 1)
parameters = file.ReadLine()
file.Close
Set file = Nothing

'Load GUI configuration.
'If the GUI is enabled in Interface.ini, the server opens its own log window
'(no console window is created). Otherwise fall back to a console window.
window = 1
Set file = CreateObject("Scripting.FileSystemObject").OpenTextFile("config\Interface.ini", 1)
Do While Not file.AtEndOfStream
	If Replace(LCase(file.ReadLine()), " ", "") = "enablegui=true" Then
		window = 0
		Exit Do
	End If
Loop
file.Close
Set file = Nothing


'Install/verify the hash-pinned core guard BEFORE starting GameServer. Fail closed.
patchCommand = "cmd.exe /d /c " & Chr(34) & Chr(34) & fs.BuildPath(shell.CurrentDirectory, "InstallDebuffGuard.cmd") & Chr(34) & Chr(34)
patchExit = shell.Run(patchCommand, 0, True)
If patchExit <> 0 Then
    logPath = fs.BuildPath(shell.CurrentDirectory, "log\debuff-guard-install.log")
    detail = ""
    If fs.FileExists(logPath) Then
        Set logFile = fs.OpenTextFile(logPath, 1)
        detail = logFile.ReadAll()
        logFile.Close
        detail = Right(detail, 2200)
    End If
    MsgBox "Debuff guard installation failed. GameServer was NOT started." & vbCrLf & "Exit code: " & patchExit & vbCrLf & "Full log: " & logPath & vbCrLf & vbCrLf & detail, vbOKOnly + vbCritical, "Game Server"
    WScript.Quit patchExit
End If

'Generate command.
command = Chr(34) & javaExe & Chr(34) & " " & parameters & " -jar ../libs/GameServer.jar"
If window = 1 Then
	command = "cmd /c start ""L2J Mobius - Game Server Window"" " & command
End If

'Run the server.
exitcode = 0
Do
	exitcode = shell.Run(command, window, True)
	If exitcode <> 0 And exitcode <> 2 Then	'0 Terminated - 2 Restarted
		hint = ""
		Select Case exitcode
			Case 1
				hint = "Uncaught Java exception or wrong Java version."
			Case 130
				hint = "Interrupted by Ctrl+C."
			Case 137
				hint = "Killed by the OS (out of memory?)."
			Case 143
				hint = "Terminated by SIGTERM."
		End Select
		If hint = "" And exitcode < 0 Then
			hint = "JVM or native crash (access violation)."
		End If
		msg = "Game Server terminated abnormally!" & vbCrLf & vbCrLf & "Exit code: " & exitcode
		If hint <> "" Then
			msg = msg & "  (" & hint & ")"
		End If
		msg = msg & vbCrLf & "Check log\error0.log and log\java0.log for details."
		MsgBox msg, vbOKOnly + vbExclamation, "Game Server"
	End If
Loop While exitcode = 2
