@echo off

echo.
echo Ant Build System
echo ----------------

set ANT_HOME=tools

set LOCALCLASSPATH=
for %%i in (lib\*.jar) do call tools\bin\lcp.bat %%i
set CLASSPATH=%LOCALCLASSPATH%
set LOCALCLASSPATH=

%ANT_HOME%\bin\ant.bat -logger org.apache.tools.ant.NoBannerLogger -emacs %1 %2 %3 %4 %5 %6 %7 %8
goto cleanup

:cleanup
set ANT_HOME=
set CLASSPATH=
