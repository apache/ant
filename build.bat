@echo off
REM convience bat file to build with
set TOOLSJAR=
if exist %JAVA_HOME%\lib\tools.jar set TOOLSJAR=%JAVA_HOME%\lib\tools.jar
java -classpath "%CLASSPATH%;%TOOLSJAR%;lib\ant.jar" org.apache.tools.ant.Main %1 %2 %3 %4 %5
