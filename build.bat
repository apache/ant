@echo off
REM convience bat file to build with
if exist %JAVA_HOME%\lib\tools.jar set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar
java -classpath "%CLASSPATH%;lib\ant.jar" org.apache.tools.ant.Main %1 %2 %3 %4 %5
