@echo off

REM You will need to specify JAVA_HOME if compiling with 1.2 or later.

set JAVA_HOME=
set JAVA=
set JAVAC=

if exist ..\antrc.bat call ..\antrc.bat

if "" == "%JAVA%"  if "" == "%JAVA_HOME%" set JAVA=java
if "" == "%JAVA%"                         set JAVA=%JAVA_HOME%\bin\java

if "" == "%JAVAC%" if "" == "%JAVA_HOME%" set JAVAC=javac
if "" == "%JAVAC%"                        set JAVAC=%JAVA_HOME%\bin\javac

echo.
echo ... Bootstrapping Ant Distribution

set CLASSPATH=src\main;classes;lib\xml.jar
if exist %JAVA_HOME%\lib\tools.jar set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar

echo JAVA_HOME=%JAVA_HOME%
echo JAVA=%JAVA%
echo JAVAC=%JAVAC%
echo CLASSPATH=%CLASSPATH%

if     "%OS%" == "Windows_NT" if exist classes\nul rmdir/s/q classes
if not "%OS%" == "Windows_NT" if exist classes\nul deltree/y classes
mkdir classes

set TOOLS=src\main\org\apache\tools

echo.
echo ... Compiling Ant Classes

%JAVAC% -d classes %TOOLS%\tar\*.java %TOOLS%\ant\*.java %TOOLS%\ant\taskdefs\*.java

echo.
echo ... Copying Required Files

copy %TOOLS%\ant\taskdefs\*.properties classes\org\apache\tools\ant\taskdefs
copy %TOOLS%\ant\*.properties          classes\org\apache\tools\ant

echo.
echo ... Building Ant Distribution

%JAVA% org.apache.tools.ant.Main main %1 %2 %3 %4 %5

echo.
echo ... Cleaning Up Build Directories

%JAVA% org.apache.tools.ant.Main clean %1 %2 %3 %4 %5

if     "%OS%" == "Windows_NT" if exist classes\nul rmdir/s/q classes
if not "%OS%" == "Windows_NT" if exist classes\nul deltree/y classes

echo.
echo ... Done Bootstrapping Ant Distribution

set JAVA_HOME=
set JAVA=
set JAVAC=
set CLASSPATH=
set TOOLS=
