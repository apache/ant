@echo off
REM convience bat file to build with
java -classpath "%CLASSPATH%;lib\ant.jar" org.apache.tools.ant.Main %1 %2 %3 %4 %5
