@echo off
REM   Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
REM   reserved.

REM cleanup curretn boot area
if exist bin rmdir /s/q bin 
if exist bootstrap rmdir /s/q bootstrap 
if exist dist rmdir /s/q dist 

REM compile init jar
mkdir bin\init
javac -d bin\init src\java\init\org\apache\ant\init\*.java

REM compile bootstrap classes
mkdir bin\bootstrap
javac -classpath bin\init -d bin\bootstrap src\java\bootstrap\org\apache\ant\bootstrap\*.java

REM compiler builder classes
mkdir bin\builder
javac -classpath bin\init;bin\bootstrap -d bin\builder src\java\bootstrap\org\apache\ant\builder\*.java

REM run bootstrap
java -classpath bin\init;bin\bootstrap org.apache.ant.bootstrap.Bootstrap

REM run full build using bootstrapped version
java -jar bootstrap\lib\start.jar %*

REM Use the full build as the build used by the build script
xcopy /s /y dist bootstrap

