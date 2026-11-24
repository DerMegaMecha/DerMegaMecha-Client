#pragma message "DerMegaMecha Launcher per-user installer"

[Setup]
AppName=DerMegaMecha Launcher
AppVersion=1.0.0
AppPublisher=DerMegaMecha
AppId={{D9D4D9B7-7D0C-4C3A-A1FC-D9D4D9B77D0C}
DefaultDirName={localappdata}\Programs\DerMegaMecha Launcher
UsePreviousAppDir=no
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
Uninstallable=yes
OutputDir=..\launcher\build\installer
OutputBaseFilename=DerMegaMecha-Launcher-Setup
SetupIconFile=DerMegaMecha.ico
Compression=lzma2
SolidCompression=yes
MinVersion=10.0

[Files]
Source: "..\launcher\build\jpackage\DerMegaMechaLauncher\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\DerMegaMecha Launcher"; Filename: "{app}\DerMegaMechaLauncher.exe"; WorkingDir: "{app}"; IconFilename: "{app}\DerMegaMechaLauncher.exe"
Name: "{autodesktop}\DerMegaMecha Launcher"; Filename: "{app}\DerMegaMechaLauncher.exe"; WorkingDir: "{app}"; IconFilename: "{app}\DerMegaMechaLauncher.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; Flags: unchecked

[Run]
Filename: "{app}\DerMegaMechaLauncher.exe"; Description: "Launch DerMegaMecha Launcher"; Flags: postinstall nowait skipifsilent
