@echo off

if exist "%HOME%\antrc_pre.bat" call "%HOME%\antrc_pre.bat"

if not "%JAVA_HOME%" == "" goto javaCmdSetup

echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If build fails because sun.* classes could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.
goto end

rem hope that there is java command in path
if "%JAVACMD%" == "" set JAVACMD=java
goto argSetup

rem if JAVA_HOME is set then make sure we use that java exe
:javaCmdSetup
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java

:argSetup

set THIS_FILE=%0
set ANT_CMD_LINE_ARGS=

rem Slurp all args...
:setupArgs
if "%0" == "" goto doneArgs
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneArgs
rem Mmmmmm tasty - finished slurping args

%JAVACMD% %ANT_OPTS% -jar lib\ant.jar "--bin-dir=%THIS_FILE%" %ANT_CMD_LINE_ARGS%

:end
if exist "%HOME%\antrc_post.bat" call "%HOME%\antrc_post.bat"
set THIS_FILE=
set ANT_CMD_LINE_ARGS=