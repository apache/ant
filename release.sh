#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#######################################################################
# this is a first attempt to document the build of the distribution
# paths are hard-coded and obviously this is for a Cygwin/Windows combo
#######################################################################
rm -rf bootstrap build dist distribution
unset ANT_HOME
export JAVA_HOME=/cygdrive/c/j2sdk1.4.2_19
export PATH=$JAVA_HOME/bin:$PATH
echo ANT_HOME=$ANT_HOME
echo JAVA_HOME=$JAVA_HOME
which java
echo running first build under JDK 1.4
./build.sh
export JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.5.0_22"
export PATH=$JAVA_HOME/bin:$PATH
echo ANT_HOME=$ANT_HOME
echo JAVA_HOME=$JAVA_HOME
which java
echo running second build under JDK 1.5 including tests
./build.sh distribution run-tests

