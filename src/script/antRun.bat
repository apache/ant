@echo off

cd %1
set ANT_RUN_CMD=%2
shift
shift

set PARAMS=
:loop
if ""%1 == "" goto runCommand
set PARAMS=%PARAMS% %1
shift
goto loop

:runCommand
rem echo %ANT_RUN_CMD% %PARAMS%
%ANT_RUN_CMD% %PARAMS%

