#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Compiles and builds Ant with various different recent versions of Java and
# then runs (only the) tests with the minimal supported version of Java runtime
# (which as of Aug 2019, is Java 8).
# This ensures/verifies that various Ant functionality works as expected
# when Ant is built with a higher version of Java and is run against the minimal
# supported version.
# This script is ideal for using in a CI environment where it can be invoked
# through a job which is configured to use various different JDK versions.

# Fail the script on error
set -e

# Build (compile and generate the dist) the project using the Java version
# that's already set in the environment
echo "Using  \"${JAVA_HOME}\" to build Ant"
java -version

# Fetch all the necessary thirdparty libs, before boostraping Ant
ant -f fetch.xml -Ddest=optional

# Now bootstrap Ant with all necessary thirdparty libs already fetched
./build.sh allclean dist

# Switch the JDK to Java 8 to run *only* the tests.
# This will ensure that Ant built with different (higher) version of Ant
# can be used by Java 8 runtime and can function properly for all Ant
# functionality
mkdir -p build/java-8-latest
cd build/java-8-latest
# Download latest Java 8 (we use Adopt OpenJDK binaries)
wget https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u232-b09/OpenJDK8U-jdk_x64_linux_hotspot_8u232b09.tar.gz
tar -zxf ./*.tar.gz
# set JAVA_HOME to point to the newly extracted tar's content
export JAVA_HOME="`echo \`pwd\`/\`echo */\``"
export PATH="${JAVA_HOME}"/bin:"${PATH}"
cd ../..

echo "Using \"${JAVA_HOME}\" to run Ant tests"
java -version

# Set ANT_HOME to the boostraped version - the one which was built, using a different Java version, a few steps
# earlier in this script
export ANT_HOME="`pwd`/bootstrap"
# Run the tests. We intentionally skip the build (compilation etc) to avoid compiling the project
# with the newly set Java version.
ant -nouserlib -lib lib/optional test -Dskip.build=true -Dignore.tests.failed=true -Doptional.jars.whenmanifestonly=skip -Djenkins=t

