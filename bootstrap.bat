@ECHO OFF
echo BOOTSTRAPPING ANT DISTRIBUTION

set C=%CLASSPATH%;lib/xml.jar
set SRCDIR=src\main\org\apache\tools
set TMPDIR=tmp

if "%OS%" == "Windows_NT" goto nt
goto windows

:doneOs

rem Delete temp directory if it exists
if exist %TMPDIR%\nul %RMDIRCMD% %TMPDIR% nul

rem make the temp directory
mkdir %TMPDIR%

echo ** COMPILING ANT CLASSES

rem Reset classpath to include base ant class files
set C=%TMPDIR%;%C%

rem Compile sub classes into the temp directory
javac -classpath "%C%" -d %TMPDIR% %SRCDIR%\tar\*.java

rem Compile the classes into the temp directory
javac -classpath "%C%" -d %TMPDIR% %SRCDIR%\ant\*.java

rem Compile sub classes into the temp directory
javac -classpath "%C%" -d %TMPDIR% %SRCDIR%\ant\taskdefs\*.java


echo ** COPYING REQUIRED FILES

rem Copy all the property/manifest files into the temp directory

%COPYCMD% src\main\org\apache\tools\ant\taskdefs\defaults.properties %TMPDIR%\org\apache\tools\ant\taskdefs
%COPYCMD% src\main\org\apache\tools\ant\parser.properties %TMPDIR%\org\apache\tools\ant

echo ** BUILDING ANT DISTRIBUTION

rem Build the distribution using the newly compiled classes in the temp directory
java -classpath "%C%" org.apache.tools.ant.Main main %1 %2 %3 %4 %5

echo ** CLEANING UP BUILD DIRECTORIES

java -classpath "%C%" org.apache.tools.ant.Main clean %1 %2 %3 %4 %5

rem remove the temp directory
%RMDIRCMD% %TMPDIR%

goto end

rem Set system dependent commands below
:windows
echo ** CONFIGURING COMMANDS FOR WINDOWS 9x SYSTEM
set RMDIRCMD=deltree /Y
set COPYCMD=copy
goto doneOs

:nt
echo ** CONFIGURING COMMANDS FOR NT SYSTEM
set RMDIRCMD=rmdir /s /q
set COPYCMD=copy
goto doneOs

:end

echo ** DONE BOOTSTRAPPING ANT DISTRIBUTION

