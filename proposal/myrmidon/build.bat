@echo off

echo.
echo Ant Build System
echo ----------------

set ANT_HOME=.

set CLASSPATH=

%ANT_HOME%\bin\ant.bat -emacs %1 %2 %3 %4 %5 %6 %7 %8
goto cleanup

:cleanup
set ANT_HOME=
set CLASSPATH=
