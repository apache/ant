@echo off

REM   Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
REM   reserved.

if exist "%HOME%\antrc_pre.bat" call "%HOME%\antrc_pre.bat"

if "%OS%"=="Windows_NT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_ANT_HOME=%~dp0..

if "%ANT_HOME%"=="" set ANT_HOME=%DEFAULT_ANT_HOME%
set DEFAULT_ANT_HOME=

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set ANT_CMD_LINE_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs
rem This label provides a place for the argument list loop to break out 
rem and for NT handling to skip to.

:doneStart
rem find ANT_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%ANT_HOME%\lib\ant.jar" goto checkJava

rem check for ant in Program Files on system drive
if not exist "%SystemDrive%\Program Files\ant" goto checkSystemDrive
set ANT_HOME=%SystemDrive%\Program Files\ant
goto checkJava

:checkSystemDrive
rem check for ant in root directory of system drive
if not exist %SystemDrive%\ant\lib\ant.jar goto checkCDrive
set ANT_HOME=%SystemDrive%\ant
goto checkJava

:checkCDrive
rem check for ant in C:\ant for Win9X users
if not exist C:\ant\lib\ant.jar goto noAntHome
set ANT_HOME=C:\ant
goto checkJava

:noAntHome
echo ANT_HOME is set incorrectly or ant could not be located. Please set ANT_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%
set LOCALCLASSPATH=%CLASSPATH%
for %%i in ("%ANT_HOME%\lib\*.jar") do call "%ANT_HOME%\bin\lcp.bat" %%i

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_HOME%\lib\tools.jar" set LOCALCLASSPATH=%JAVA_HOME%\lib\tools.jar;%LOCALCLASSPATH%
if exist "%JAVA_HOME%\lib\classes.zip" set LOCALCLASSPATH=%JAVA_HOME%\lib\classes.zip;%LOCALCLASSPATH%
goto checkJikes

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If build fails because sun.* classes could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.

:checkJikes
if not "%JIKESPATH%"=="" goto runAntWithJikes

:runAnt
"%_JAVACMD%" %ANT_OPTS% -classpath "%LOCALCLASSPATH%" "-Dant.home=%ANT_HOME%" org.apache.tools.ant.Main %ANT_ARGS% %ANT_CMD_LINE_ARGS%
goto end

:runAntWithJikes
"%_JAVACMD%" %ANT_OPTS% -classpath "%LOCALCLASSPATH%" "-Dant.home=%ANT_HOME%" "-Djikes.class.path=%JIKESPATH%" org.apache.tools.ant.Main %ANT_ARGS% %ANT_CMD_LINE_ARGS%
goto end

:end
set LOCALCLASSPATH=
set _JAVACMD=
set ANT_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal

:mainEnd
if exist "%HOME%\antrc_post.bat" call "%HOME%\antrc_post.bat"

