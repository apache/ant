@echo off

set OLDCLASSPATH=%CLASSPATH%
set REAL_ANT_HOME=%ANT_HOME%
set ANT_HOME=bootstrap
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
call bootstrap.bat

:runAnt
set CLASSPATH=bootstrap\lib\ant.jar;lib\parser.jar;lib\jaxp.jar;%CLASSPATH%
if not "%REAL_ANT_HOME%" == "" goto install_ant
call bootstrap\bin\ant.bat %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
call bootstrap\bin\ant.bat -Dant.install="%REAL_ANT_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REAL_ANT_HOME%
set REAL_ANT_HOME=
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=
