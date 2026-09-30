@echo off
setlocal
set "SAO_CLIENT_DIR=%~dp0"
if exist "%SAO_CLIENT_DIR%aincrad-client-java-0.1.0.jar" (
  set "SAO_CLIENT_CP=%SAO_CLIENT_DIR%aincrad-client-java-0.1.0.jar;%SAO_CLIENT_DIR%lib\*"
) else (
  set "SAO_CLIENT_CP=%SAO_CLIENT_DIR%target\aincrad-client-java-0.1.0.jar;%SAO_CLIENT_DIR%target\lib\*"
)
java -Xmx1536m -Dfile.encoding=UTF-8 -cp "%SAO_CLIENT_CP%" dev.aincrad.client.Launcher %*
