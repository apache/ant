#!/bin/sh

# You will need to specify JAVA_HOME if compiling with 1.2 or later.

ANT_HOME=.
export ANT_HOME

echo ... Bootstrapping Ant Distribution

if test -f lib/ant.jar ; then
  rm lib/ant.jar
fi

LOCALCLASSPATH=`echo $ANT_HOME/lib/*.jar | tr ' ' ':'`

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$CLASSPATH:$LOCALCLASSPATH
else
  CLASSPATH=$LOCALCLASSPATH
fi

if test -f $JAVA_HOME/lib/tools.jar ; then
  CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
fi

if test -f $JAVA_HOME/lib/classes.zip ; then
  CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/classes.zip
fi

TOOLS=src/main/org/apache/tools
CLASSDIR=classes

CLASSPATH=${CLASSPATH}:${CLASSDIR}:src/main
export CLASSPATH

mkdir -p ${CLASSDIR}

echo ... Compiling Ant Classes

javac  -d ${CLASSDIR} ${TOOLS}/tar/*.java
javac  -d ${CLASSDIR} ${TOOLS}/ant/*.java
javac  -d ${CLASSDIR} ${TOOLS}/ant/taskdefs/*.java

echo ... Copying Required Files

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties ${CLASSDIR}/org/apache/tools/ant/taskdefs

echo ... Building Ant Distribution

java org.apache.tools.ant.Main clean main bootstrap

echo ... Cleaning Up Build Directories

chmod +x bin/ant bin/antRun

rm -rf ${CLASSDIR}

echo ... Done Bootstrapping Ant Distribution
