'Get Java path.
Dim path
Set shell = WScript.CreateObject("WScript.Shell")
path = shell.Environment.Item("JAVA_HOME")
If path = "" Then
	MsgBox "Could not find JAVA_HOME environment variable!", vbOKOnly, "Game Server"
Else
	If InStr(path, "\bin") = 0 Then
		path = path + "\bin\"
	Else
		path = path + "\"
	End If
	path = Replace(path, "\\", "\")
	path = Replace(path, "Program Files", "Progra~1")
End If

'Load java.cfg parameters.
Dim parameters
Set file = CreateObject("Scripting.FileSystemObject").OpenTextFile("java.cfg", 1)
parameters = file.ReadLine()
file.Close
Set file = Nothing

'Load GUI configuration.
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
patchCommand = Chr(34) & path & "java" & Chr(34) & " -cp " & Chr(34) & "../libs/*" & Chr(34) & " core-patches/debuff-no-refresh/InstallDebuffGuard.java ../libs/GameServer.jar core-patches/debuff-no-refresh/DebuffCategoryGuard.java"
patchExit = shell.Run(patchCommand, 1, True)
If patchExit <> 0 Then
    MsgBox "Debuff guard installation failed. GameServer was NOT started. Use Java 25 JDK; see structure/28-debuff-category-no-refresh.md. Run the installer in a console for details.", vbOKOnly + vbCritical, "Game Server"
    WScript.Quit patchExit
End If

'Generate command.
command = path & "java " & parameters & " -jar ../libs/GameServer.jar"
If window = 1 Then
	command = "cmd /c start ""L2J Mobius - Game Server Console"" " & command
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
