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
rm -rf bootstrap build dist distribution java-repository
unset ANT_HOME
# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false;
linux=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true;;
  MINGW*) mingw=true ;;
  Linux) linux=true ;;
esac
if $cygwin ; then
  export JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.5.0_22"
  JDK_VERSION=1.5
fi
if $darwin; then
   export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
   JDK_VERSION=1.6
fi  
if $linux; then
   export JAVA_HOME=/usr/lib/jvm/java-6-openjdk
   JDK_VERSION=1.6
fi
# check that one can build under maven
mvn -f src/etc/poms/pom.xml -DskipTests  package
rm -rf target
export PATH=$JAVA_HOME/bin:$PATH
echo ANT_HOME=$ANT_HOME
echo JAVA_HOME=$JAVA_HOME
which java
echo running build under JDK $JDK_VERSION
./build.sh dist-lite 
echo running the tests and doing the distribution
dist/bin/ant -nouserlib -lib lib/optional  run-tests distribution


