#!/bin/sh

# You will need to specify JAVA_HOME if compiling with 1.2 or later.

ANT_HOME=.
export ANT_HOME

if [ -z "$JAVAC" ] ; then
  JAVAC=javac;
fi

echo ... Bootstrapping Ant Distribution

if test -f lib/ant.jar ; then
  rm lib/ant.jar
fi

LOCALCLASSPATH=`echo $ANT_HOME/lib/*.jar | tr ' ' ':'`

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$LOCALCLASSPATH:$CLASSPATH
else
  CLASSPATH=$LOCALCLASSPATH
fi

if [ "$JAVA_HOME" != "" ] ; then
  if test -f $JAVA_HOME/lib/tools.jar ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
  fi
 
  if test -f $JAVA_HOME/lib/classes.zip ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
else
  echo "Warning: JAVA_HOME environment variable not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

TOOLS=src/main/org/apache/tools
CLASSDIR=classes

CLASSPATH=${CLASSDIR}:src/main:${CLASSPATH}
export CLASSPATH

mkdir -p ${CLASSDIR}

echo ... Compiling Ant Classes

${JAVAC} -d ${CLASSDIR} ${TOOLS}/tar/*.java
${JAVAC} -d ${CLASSDIR} ${TOOLS}/ant/types/*.java
${JAVAC} -d ${CLASSDIR} ${TOOLS}/ant/*.java
${JAVAC} -d ${CLASSDIR} ${TOOLS}/ant/taskdefs/*.java

echo ... Copying Required Files

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties ${CLASSDIR}/org/apache/tools/ant/taskdefs
cp src/main/org/apache/tools/ant/types/defaults.properties ${CLASSDIR}/org/apache/tools/ant/types

echo ... Building Ant Distribution

java ${ANT_OPTS} org.apache.tools.ant.Main clean main bootstrap

echo ... Cleaning Up Build Directories

chmod +x bin/ant bin/antRun

rm -rf ${CLASSDIR}

echo ... Done Bootstrapping Ant Distribution
