@echo off
@setlocal
if "%ANT_HOME%"=="" goto checkProgFiles
goto checkJavaHome

:checkProgFiles
rem check for ant on system drive
if not exist "%SystemDrive%\Program Files\ant" goto checkSystemDrive

set ANT_HOME=%SystemDrive%\Program Files\ant
goto checkJavaHome

:checkSystemDrive
if not exist "%SystemDrive%\ant" goto noAntHome
set ANT_HOME=%SystemDrive%\ant
goto checkJavaHome

:noAntHome
echo ANT_HOME is not set and ant could not be located
goto end

:checkJavaHome
if "%JAVA_HOME%" == "" goto runAnt
set CLASSPATH=%JAVA_HOME%\lib\tools.jar;%CLASSPATH%

:runAnt
set CLASSPATH=%ANT_HOME%\lib\ant.jar;%ANT_HOME%\lib\xml.jar;%CLASSPATH%
java -Dant.home="%ANT_HOME%" org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

:end
@endlocal
