@echo off

:checkJava
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java
if not "%JAVA_HOME%" == "" goto setupClasspath

echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If build fails because sun.* classes could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.
goto end

:setupClasspath
set LOCALCLASSPATH=lib\xerces.jar;lib\ant.jar;lib\avalonapi.jar;%JAVA_HOME%\lib\tools.jar

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

%JAVACMD% -classpath "%LOCALCLASSPATH%" %ANT_OPTS% org.apache.ant.Main "--bin-dir=%THIS_FILE%" %ANT_CMD_LINE_ARGS%

:end
set LOCALCLASSPATH=