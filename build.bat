@echo off

set REALANTHOME=%ANT_HOME%
set ANT_HOME=.
if exist lib\ant.jar if exist bin\ant.bat if exist bin\lcp.bat if exist bin\antRun.bat goto runAnt
call bootstrap.bat

:runAnt
if not "%REALANTHOME%" == "" goto install_ant
call .\bin\ant.bat %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
set ANT_INSTALL="-Dant.install%REALANTHOME%"
call .\bin\ant.bat -Dant.install=%REALANTHOME% %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REALANTHOME%
set REALANTHOME=
set ANT_INSTALL=
