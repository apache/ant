@echo off

if exist "%HOME%\antrc_pre.bat" call "%HOME%\antrc_pre.bat"

if not "%OS%"=="Windows_NT" goto start

rem %~dp0 is name of current script under NT
set DEFAULT_ANT_HOME=%~dp0

rem : operator works similar to make : operator
set DEFAULT_ANT_HOME=%DEFAULT_ANT_HOME:\bin\=%

if "%ANT_HOME%"=="" set ANT_HOME=%DEFAULT_ANT_HOME%
set DEFAULT_ANT_HOME=

:start

if not "%ANT_HOME%" == "" goto ant_home_found

echo.
echo Warning: ANT_HOME environment variable is not set.
echo   This needs to be set for Win9x as it's command prompt 
echo   scripting bites
echo.
goto end

:ant_home_found

if not "%JAVA_HOME%" == "" goto javaCmdSetup

rem hope that there is java command in path
if "%JAVACMD%" == "" set JAVACMD=java
goto argSetup

rem if JAVA_HOME is set then make sure we use that java exe
:javaCmdSetup
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java

:argSetup

set ANT_CMD_LINE_ARGS=

rem Slurp all args...
:setupArgs
if "%0" == "" goto doneArgs
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneArgs
rem Mmmmmm tasty - finished slurping args

%JAVACMD% %ANT_OPTS% -jar %ANT_HOME%\bin\myrmidon-launcher.jar %ANT_CMD_LINE_ARGS%

:end
if exist "%HOME%\antrc_post.bat" call "%HOME%\antrc_post.bat"
set ANT_CMD_LINE_ARGS=
