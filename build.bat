@echo off

set _ANTHOME=%ANT_HOME%
if "%ANT_HOME%" == "" set ANT_HOME=.

set LOCALCLASSPATH=%CLASSPATH%
for %%i in (%ANT_HOME%\lib\*.jar) do call lcp.bat %%i
if exist %JAVA_HOME%\lib\tools.jar call lcp.bat %JAVA_HOME%\lib\tools.jar

echo.
echo Building with classpath: %LOCALCLASSPATH%
echo.

java -Dant.home="%ANT_HOME%" -classpath "%LOCALCLASSPATH%" %ANT_OPTS% org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
set LOCALCLASSPATH=
set ANT_HOME=%_ANTHOME%
set _ANTHOME=
