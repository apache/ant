@echo off

cd %1
set ANT_RUN_CMD=%2
shift
shift

%ANT_RUN_CMD% %1 %2 %3 %4 %5 %6 %7 %8 %9

