@echo off

set _CP=CLASSPATH

if exist build\classes\org\apache\tools\ant\Main.class goto doBuild

call bootstrap.bat %1 %2 %3 %4 %5 %6 %7 %8

:doBuild

echo ----------------
echo Ant Build System
echo ----------------

rem exit

set LOCALCLASSPATH=lib\optional\junit.jar;build\classes
for %%i in (lib\*.jar) do call src\script\lcp.bat %%i

if "%JAVA_HOME%" == "" goto noJavaHome
set _JAVACMD=%JAVACMD%
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java
if exist "%JAVA_HOME%\lib\tools.jar" call src\script\lcp.bat "%JAVA_HOME%\lib\tools.jar"
if exist "%JAVA_HOME%\lib\classes.zip" call src\script\lcp.bat" "%JAVA_HOME%\lib\classes.zip"
goto runAnt

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If build fails because sun.* classes could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.

:runAnt
set NEW_ANT_HOME=%ANT_HOME%
if "%ANT_HOME%" == "" set NEW_ANT_HOME=dist
set CLASSPATH=%LOCALCLASSPATH%
set LOCALCLASSPATH=
%_JAVACMD% -classpath %CLASSPATH% %ANT_OPTS% org.apache.tools.ant.Main "-Dant.home=%NEW_ANT_HOME%" -logger org.apache.tools.ant.NoBannerLogger -emacs %1 %2 %3 %4 %5 %6 %7 %8

set CLASSPATH=%_CP%
set NEW_ANT_HOME=
set _JAVACMD=
