@echo off

rem Slurp the command line arguments.  This loop allows for an unlimited number of 
rem agruments (up to the command line limit, anyway).

set ANT_CMD_LINE_ARGS=

:setupArgs
if %1a==a goto doneArgs
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneArgs
rem The doneArgs label is here just to provide a place for the argument list loop
rem to break out to.

rem find ANT_HOME
if not "%ANT_HOME%"=="" goto checkJava

rem check for ant in Program Files on system drive
rem if not exist "%SystemDrive%\Program Files\ant" goto checkSystemDrive
rem set ANT_HOME=%SystemDrive%\Program Files\ant
rem goto checkJava

rem :checkSystemDrive
rem check for ant in root directory of system drive
rem if not exist "%SystemDrive%\ant" goto noAntHome
rem set ANT_HOME=%SystemDrive%\ant
rem goto checkJava

:noAntHome
echo ANT_HOME is not set and ant could not be located. Please set ANT_HOME.
goto end

:checkJava
if "%JAVACMD%" == "" set JAVACMD=java

set LOCALCLASSPATH="%CLASSPATH%"
for %%i in ("%ANT_HOME%\lib\*.jar") do call "%ANT_HOME%\bin\lcp.bat" "%%i"

%JAVACMD% -classpath %LOCALCLASSPATH% -Dant.home="%ANT_HOME%" org.apache.tools.ant.gui.Main %ANT_CMD_LINE_ARGS%
goto end

:end
set LOCALCLASSPATH=
set ANT_CMD_LINE_ARGS=

