@echo off
rem find ANT_HOME
if not "%ANT_HOME%"=="" goto checkJava

rem check for ant in Program Files on system drive
if not exist "%SystemDrive%\Program Files\ant" goto checkSystemDrive
set ANT_HOME=%SystemDrive%\Program Files\ant
goto checkJava

:checkSystemDrive
rem check for ant in root directory of system drive
if not exist "%SystemDrive%\ant" goto noAntHome
set ANT_HOME=%SystemDrive%\ant
goto checkJava

:noAntHome
echo ANT_HOME is not set and ant could not be located. Please set ANT_HOME.
goto end

:checkJava
if "%JAVACMD%" == "" set JAVACMD=java

set LOCALCLASSPATH=%CLASSPATH%
for %%i in (%ANT_HOME%\lib\*.jar) do call %ANT_HOME%\bin\lcp.bat %%i

if "%JAVA_HOME%" == "" goto runAnt
if exist %JAVA_HOME%\lib\tools.jar call %ANT_HOME%\bin\lcp.bat %JAVA_HOME%\lib\tools.jar
if exist %JAVA_HOME%\lib\classes.zip call %ANT_HOME%\bin\lcp.bat %JAVA_HOME%\lib\classes.zip

:runAnt
%JAVACMD% -classpath "%LOCALCLASSPATH%" -Dant.home="%ANT_HOME%" %ANT_OPTS% org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

:end
set LOCALCLASSPATH=

