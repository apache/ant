@echo off
rem --------------------------------------------------------------------------
rem build.bat - Build Script for Frantic (lifted from Tomcat...thx guys)
rem
rem Environment Variable Prerequisites:
rem
rem   JAVA_HOME        Must point at your Java Development Kit [REQUIRED]
rem
rem ---------------------------------------------------------------------------


rem ----- Save Environment Variables ------------------------------------------

set _CLASSPATH=%CLASSPATH%
set _CLASSES=%CLASSES%

rem ----- Verify and Set Required Environment Variables -----------------------

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto cleanup
:gotJavaHome

rem ----- Set Up The Runtime Classpath ----------------------------------------

if not "%CLASSPATH%" == "" set CLASSPATH=%CLASSPATH%;
set CLASSPATH=%CLASSPATH%;./src

rem ----- Execute The Requested Build -----------------------------------------

if not exist dist mkdir dist
if not exist dist\lib mkdir dist\lib
if not exist dist\lib\classes mkdir dist\lib\classes
if not exist dist\doc mkdir dist\doc
if not exist dist\doc\api mkdir dist\doc\api

set CLASSES=dist\lib\classes

%JAVA_HOME%\bin\javac -d %CLASSES% src/org/apache/ant/test/*.java
%JAVA_HOME%\bin\jar cvf dist\lib\frantic.jar -C dist\lib\classes .

xcopy website\*.html dist\doc /s /y
xcopy website\*.gif dist\doc /s /y

%JAVA_HOME%\bin\javadoc -protected -sourcepath src -d dist\doc\api -author org.apache.ant org.apache.ant.engine org.apache.ant.tasks org.apache.ant.tasks.build org.apache.ant.tasks.util

rem ----- Restore Environment Variables ---------------------------------------
:cleanup
set CLASSPATH=%_CLASSPATH%
set CLASSES=%_CLASSES%
set _CLASSPATH=
set _CLASSES=

:finish

