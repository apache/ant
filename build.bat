@echo off

set REALANTHOME=%ANT_HOME%
set ANT_HOME=.
if exist lib\ant.jar if exist bin\ant.bat if exist bin\lcp.bat if exist bin\antRun.bat goto runAnt
call bootstrap.bat

:runAnt
set ANT_INSTALL=
if not "%REALANTHOME%" == "" set ANT_INSTALL=-Dant.install %REALANTHOME%
call .\bin\ant.bat %ANT_INSTALL% %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
set ANT_HOME=%REALANTHOME%
set REALANTHOME=
set ANT_INSTALL=
