@echo off

set REALANTHOME=%ANT_HOME%
set ANT_HOME=.
if not exist lib\ant.jar call bootstrap.bat

set ANT_INSTALL=
if not "%REALANTHOME%" == "" set ANT_INSTALL=-Dant.install %REALANTHOME%
call .\bin\ant %ANT_INSTALL% %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
set ANT_HOME=%REALANTHOME%
set REALANTHOME=
set ANT_INSTALL=
