#!/bin/sh

#   Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
#   reserved.

# cleanup curretn boot area
rm -rf bin bootstrap dist

# compile init jar
mkdir -p bin/init
javac -d bin/init src/java/init/org/apache/ant/init/*.java

# compile bootstrap classes
mkdir bin/bootstrap
javac -classpath bin/init -d bin/bootstrap src/java/bootstrap/org/apache/ant/bootstrap/*.java

# compiler builder classes
mkdir bin/builder
javac -classpath bin/init:bin/bootstrap -d bin/builder src/java/bootstrap/org/apache/ant/builder/*.java

# run bootstrap
java -classpath bin/init:bin/bootstrap org.apache.ant.bootstrap.Bootstrap

# run full build using bootstrapped version
java -jar bootstrap/lib/start.jar $*

# Use the full build as the build used by the build script
cp -r dist/lib bootstrap

