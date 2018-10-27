@echo off

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

set REAL_ANT_HOME=%ANT_HOME%
set ANT_HOME=%~dp0\bootstrap
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
call bootstrap.bat
if exist bootstrap\lib\ant.jar if exist bootstrap\bin\ant.bat if exist bootstrap\bin\lcp.bat if exist bootstrap\bin\antRun.bat goto runAnt
echo Bootstrap FAILED
REM set the error code
color 00
goto cleanup

:runAnt
if not "%REAL_ANT_HOME%" == "" goto install_ant
call bootstrap\bin\ant.bat -lib lib/optional %1 %2 %3 %4 %5 %6 %7 %8 %9
goto cleanup

:install_ant
call bootstrap\bin\ant.bat -nouserlib -lib lib/optional -Dant.install="%REAL_ANT_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up
:cleanup
set ANT_HOME=%REAL_ANT_HOME%
set REAL_ANT_HOME=

