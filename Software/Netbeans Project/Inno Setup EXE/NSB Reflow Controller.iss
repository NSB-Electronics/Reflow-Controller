; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "NSB Reflow Controller"
#define MyAppVersionMajor "1"
#define MyAppVersionMinor "_10"
#define MyAppPublisher "NSB Electronics"
#define MyAppURL "www.nsb-electronics.co.za"
#define MyAppExeName "Reflow Controller"
#define MyAppCopyright "Copyright (C) 2017"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{D08F8375-015C-44A3-8C5A-B2F415A33A74}
AppName={#MyAppName} {#MyAppVersionMajor}
AppVersion={#MyAppVersionMajor}{#MyAppVersionMinor}      
AppVerName={#MyAppName} {#MyAppVersionMajor}{#MyAppVersionMinor}   
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
AppCopyright={#MyAppCopyright}
DisableProgramGroupPage=Yes
DefaultGroupName={#MyAppName} {#MyAppVersionMajor} 
DefaultDirName={pf}\{#MyAppName} {#MyAppVersionMajor} 
OutputBaseFilename={#MyAppName} {#MyAppVersionMajor}{#MyAppVersionMinor}   
Compression=lzma
SolidCompression=Yes
;Win Vista or above
MinVersion=0,5.1
SetupIconFile={#MyAppName}.ico
UninstallDisplayIcon={app}\{#MyAppName}.ico
UninstallDisplayName={#MyAppName} {#MyAppVersionMajor}{#MyAppVersionMinor}   
WizardImageStretch=No
WizardSmallImageFile={#MyAppName}-setup-icon.bmp

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\dist\bundles\Reflow Controller\Reflow Controller.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\dist\bundles\Reflow Controller\Reflow Controller.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\dist\bundles\Reflow Controller\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\dist\bundles\Reflow Controller\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs 
Source: "..\config\*"; DestDir: "{app}\app\config"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\help\*"; DestDir: "{app}\app\help"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\drivers\*"; DestDir: "{app}\drivers"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\avrdude\*"; DestDir: "{app}\app\avrdude"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\rxtxSerial.dll"; DestDir: "{app}\app"; Flags: ignoreversion
Source: "..\listComPorts.exe"; DestDir: "{app}\app"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{commonprograms}\{#MyAppPublisher}\{#MyAppName} {#MyAppVersionMajor}"; Filename: "{app}\{#MyAppExeName}.exe"; IconFilename: "{app}\{#MyAppExeName}.ico"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}.exe"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppExeName}.ico"
;Name: "{commonprograms}\{#MyAppPublisher}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Run]             
Filename: "{app}\drivers\PJRC CDC serial_install.exe"; StatusMsg: "Installing drivers"
Filename: "{app}\{#MyAppExeName}.exe"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

