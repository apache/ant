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

SET CLASSDIR=build\classes
SET LOCALCLASSPATH=lib\parser.jar;lib\jaxp.jar

if exist %JAVA_HOME%\lib\tools.jar call src\script\lcp.bat %JAVA_HOME%\lib\tools.jar
if exist %JAVA_HOME%\lib\classes.zip call src\script\lcp.bat %JAVA_HOME%\lib\classes.zip

echo JAVA_HOME=%JAVA_HOME%
echo JAVA=%JAVA%
echo JAVAC=%JAVAC%
echo CLASSPATH=%LOCALCLASSPATH%

if     "%OS%" == "Windows_NT" if exist build rmdir/s/q build
if not "%OS%" == "Windows_NT" deltree/y build

mkdir build
mkdir build\classes

echo.
echo ... Compiling Ant Classes

SET TOOLS=src\main\org\apache\tools

%JAVAC% -classpath %LOCALCLASSPATH%;src\main -d build\classes %TOOLS%\tar\*.java %TOOLS%\ant\*.java %TOOLS%\ant\types\*.java %TOOLS%\ant\taskdefs\*.java %TOOLS%\ant\util\*.java %TOOLS%\ant\util\regexp\RegexpMatcher.java %TOOLS%\ant\util\regexp\RegexpMatcherFactory.java

echo.
echo ... Copying Required Files

copy %TOOLS%\ant\taskdefs\*.properties %CLASSDIR%\org\apache\tools\ant\taskdefs
copy %TOOLS%\ant\types\*.properties %CLASSDIR%\org\apache\tools\ant\types

echo.
echo ... Building Ant Distribution

SET CLASSPATH=%LOCALCLASSPATH%;build\classes
call build.bat

echo.
echo ... Done Bootstrapping Ant Distribution

set JAVA=%OLDJAVA%
set JAVAC=%OLDJAVAC%
set CLASSPATH=%OLDCLASSPATH%
set ANT_HOME=%OLDANTHOME%
set OLDJAVA=
set OLDJAVAC=
set OLDCLASSPATH=
set CLASSPATH=
set LOCALCLASSPATH=
set OLDANTHOME=
set TOOLS=

