@echo off

REM You will need to specify JAVA_HOME if compiling with 1.2 or later.

set OLDJAVA=%JAVA%
set OLDJAVAC=%JAVAC%
set OLDCLASSPATH=%CLASSPATH%
set OLDANTHOME=%ANT_HOME%

set ANT_HOME=.

if "" == "%JAVA%"  if "" == "%JAVA_HOME%" set JAVA=java
if "" == "%JAVA%"                         set JAVA=%JAVA_HOME%\bin\java

if "" == "%JAVAC%" if "" == "%JAVA_HOME%" set JAVAC=javac
if "" == "%JAVAC%"                        set JAVAC=%JAVA_HOME%\bin\javac

echo.
echo ... Bootstrapping Ant Distribution

if exist lib\ant.jar erase lib\ant.jar

SET LOCALCLASSPATH=
for %%i in (lib\*.jar) do call src\bin\lcp.bat %%i

if exist %JAVA_HOME%\lib\tools.jar call src\bin\lcp.bat %JAVA_HOME%\lib\tools.jar
if exist %JAVA_HOME%\lib\classes.zip call src\bin\lcp.bat %JAVA_HOME%\lib\classes.zip

set TOOLS=src\main\org\apache\tools
set CLASSDIR=classes

SET CLASSPATH=%CLASSPATH%;%LOCALCLASSPATH%;%CLASSDIR%;src\main

echo JAVA_HOME=%JAVA_HOME%
echo JAVA=%JAVA%
echo JAVAC=%JAVAC%
echo CLASSPATH=%CLASSPATH%

if     "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul rmdir/s/q %CLASSDIR%
if not "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul deltree/y %CLASSDIR%

mkdir %CLASSDIR%

echo.
echo ... Compiling Ant Classes

%JAVAC% -d %CLASSDIR% %TOOLS%\tar\*.java %TOOLS%\ant\*.java %TOOLS%\ant\taskdefs\*.java

echo.
echo ... Copying Required Files

copy %TOOLS%\ant\taskdefs\*.properties %CLASSDIR%\org\apache\tools\ant\taskdefs

echo.
echo ... Building Ant Distribution

%JAVA% org.apache.tools.ant.Main clean main bootstrap %1 %2 %3 %4 %5

echo.
echo ... Cleaning Up Build Directories

if     "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul rmdir/s/q %CLASSDIR%
if not "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul deltree/y %CLASSDIR%

echo.
echo ... Done Bootstrapping Ant Distribution

set JAVA=%OLDJAVA%
set JAVAC=%OLDJAVAC%
set CLASSPATH=%OLDCLASSPATH%
set ANT_HOME=%OLDANTHOME%
set OLDJAVA=
set OLDJAVAC=
set OLDCLASSPATH=
set LOCALCLASSPATH=
set OLDANTHOME=
set TOOLS=

