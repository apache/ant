@echo off

set REAL_ANT_HOME=%ANT_HOME%
set ANT_HOME=.
if exist lib\ant.jar if exist bin\ant.bat if exist bin\lcp.bat if exist bin\antRun.bat goto runAnt
call bootstrap.bat

:runAnt
if not "%REAL_ANT_HOME%" == "" goto install_ant
call .\bin\ant.bat %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
call .\bin\ant.bat -Dant.install="%REAL_ANT_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REAL_ANT_HOME%
set REAL_ANT_HOME=
