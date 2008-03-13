@echo off

REM You will need to specify JAVA_HOME if compiling with 1.2 or later.

REM  Licensed to the Apache Software Foundation (ASF) under one or more
REM  contributor license agreements.  See the NOTICE file distributed with
REM  this work for additional information regarding copyright ownership.
REM  The ASF licenses this file to You under the Apache License, Version 2.0
REM  (the "License"); you may not use this file except in compliance with
REM  the License.  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

set OLDJAVA=%JAVA%
set OLDJAVAC=%JAVAC%
set BOOTOLDCLASSPATH=%CLASSPATH%
set OLDANTHOME=%ANT_HOME%

set ANT_HOME=.

if "" == "%JAVA%"  if "" == "%JAVA_HOME%" set JAVA=java
if "" == "%JAVA%"                         set JAVA=%JAVA_HOME%\bin\java

if "" == "%JAVAC%" if "" == "%JAVA_HOME%" set JAVAC=javac
if "" == "%JAVAC%"                        set JAVAC=%JAVA_HOME%\bin\javac

echo.
echo ... Bootstrapping Ant Distribution

if     "%OS%" == "Windows_NT" if exist bootstrap\nul rmdir/s/q bootstrap
if not "%OS%" == "Windows_NT" if exist bootstrap\nul deltree/y bootstrap
if     "%OS%" == "Windows_NT" if exist build\nul rmdir/s/q build
if not "%OS%" == "Windows_NT" if exist build\nul deltree/y build

SET LOCALCLASSPATH=lib\xercesImpl.jar;lib\xml-apis.jar
for %%i in (lib\optional\*.jar) do call src\script\lcp.bat %%i
if exist "%JAVA_HOME%\lib\tools.jar" call src\script\lcp.bat %JAVA_HOME%\lib\tools.jar
if exist "%JAVA_HOME%\lib\classes.zip" call src\script\lcp.bat %JAVA_HOME%\lib\classes.zip

set TOOLS=src\main\org\apache\tools
set CLASSDIR=build\classes

SET CLASSPATH=%LOCALCLASSPATH%;%CLASSDIR%;src\main;%CLASSPATH%

echo JAVA_HOME=%JAVA_HOME%
echo JAVA=%JAVA%
echo JAVAC=%JAVAC%
echo CLASSPATH=%CLASSPATH%

if     "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul rmdir/s/q %CLASSDIR%
if not "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul deltree/y %CLASSDIR%

if not exist build\nul mkdir build
if not exist build\classes\nul mkdir build\classes

echo.
echo ... Compiling Ant Classes

"%JAVAC%" %BOOTJAVAC_OPTS% -d %CLASSDIR% %TOOLS%\bzip2\*.java %TOOLS%\tar\*.java %TOOLS%\zip\*.java %TOOLS%\ant\*.java %TOOLS%\ant\types\*.java %TOOLS%\ant\taskdefs\*.java %TOOLS%\ant\util\regexp\RegexpMatcher.java %TOOLS%\ant\util\regexp\RegexpMatcherFactory.java %TOOLS%\ant\taskdefs\condition\*.java %TOOLS%\ant\taskdefs\compilers\*.java %TOOLS%\ant\types\resources\*.java

if ERRORLEVEL 1 goto mainend

echo.
echo ... Copying Required Files

copy %TOOLS%\ant\taskdefs\*.properties %CLASSDIR%\org\apache\tools\ant\taskdefs
copy %TOOLS%\ant\types\*.properties %CLASSDIR%\org\apache\tools\ant\types

echo.
echo ... Building Ant Distribution

if not "%OS%"=="Windows_NT" goto win9xStart
:winNTStart
@setlocal

REM parse command line arguments
rem Need to check if we are using the 4NT shell...
if "%eval[2+2]" == "4" goto setup4NT

rem On NT/2K grab all arguments at once
set ANT_CMD_LINE_ARGS=%*
goto doneStart

:setup4NT
set ANT_CMD_LINE_ARGS=%$
goto doneStart

:win9xStart
rem Slurp the command line arguments.  This loop allows for an unlimited number of
rem agruments (up to the command line limit, anyway).

set ANT_CMD_LINE_ARGS=

:setupArgs
if %1a==a goto doneStart
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneStart
rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

"%JAVA%" %ANT_OPTS% org.apache.tools.ant.Main -emacs %ANT_CMD_LINE_ARGS% bootstrap

set ANT_CMD_LINE_ARGS=
if not "%OS%"=="Windows_NT" goto mainEnd
:winNTend
@endlocal

:mainEnd

echo.
echo ... Cleaning Up Build Directories

if     "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul rmdir/s/q %CLASSDIR%
if not "%OS%" == "Windows_NT" if exist %CLASSDIR%\nul deltree/y %CLASSDIR%

echo.
echo ... Done Bootstrapping Ant Distribution

set JAVA=%OLDJAVA%
set JAVAC=%OLDJAVAC%
set CLASSPATH=%BOOTOLDCLASSPATH%
set ANT_HOME=%OLDANTHOME%
set OLDJAVA=
set OLDJAVAC=
set BOOTOLDCLASSPATH=
set LOCALCLASSPATH=
set OLDANTHOME=
set TOOLS=
