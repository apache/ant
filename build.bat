@echo off

REM   Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
REM   reserved.

set OLDCLASSPATH=%CLASSPATH%
set REAL_ANT_HOME=%ANT_HOME%
set ANT_HOME=bootstrap
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
call bootstrap.bat
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
echo Bootstrap FAILED
goto cleanup

:runAnt
set LOCALCLASSPATH=lib\xercesImpl.jar;lib\xml-apis.jar;bootstrap\lib\ant.jar
for %%i in (lib\optional\*.jar) do call bootstrap\bin\lcp.bat %%i
set CLASSPATH=lib\optional\xalanj1compat.jar;%LOCALCLASSPATH%;%CLASSPATH%
set LOCALCLASSPATH=

if not "%REAL_ANT_HOME%" == "" goto install_ant
call bootstrap\bin\ant.bat -emacs %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
call bootstrap\bin\ant.bat -emacs -Dant.install="%REAL_ANT_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REAL_ANT_HOME%
set REAL_ANT_HOME=
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=
