@echo off
REM convience bat file to build with
java -classpath "%CLASSPATH%;lib\ant.jar;lib\projectx-tr2.jar" org.apache.tools.ant.Main %1 %2 %3 %4 %5
