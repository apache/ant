@echo off

REM   Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
REM   reserved.

set REAL_ANT_HOME=%ANT_HOME%
set ANT_HOME=%~dp0\bootstrap
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
call bootstrap.bat
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
echo Bootstrap FAILED
goto cleanup

:runAnt
if not "%REAL_ANT_HOME%" == "" goto install_ant
call bootstrap\bin\ant.bat -lib lib/optional %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
call bootstrap\bin\ant.bat -nouserlib -lib lib/optional -Dant.install="%REAL_ANT_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REAL_ANT_HOME%
set REAL_ANT_HOME=

