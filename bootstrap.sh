#!/bin/bash

# Cygwin support.  $cygwin _must_ be set to either true or false.
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  *) cygwin=false ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

LOCALCLASSPATH=lib/parser.jar:lib/jaxp.jar

if [ "$CLASSPATH" != "" ] ; then
  LOCALCLASSPATH=$CLASSPATH:$LOCALCLASSPATH
fi

if [ "$JAVA_HOME" != "" ] ; then
  if test -f $JAVA_HOME/lib/tools.jar ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if test -f $JAVA_HOME/lib/classes.zip ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
else
  echo "Warning: JAVA_HOME environment variable is not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

if [ ! -x "$JAVA_HOME/bin/java" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute JAVA_HOME/bin/java"
  exit
fi

if [ -z "$JAVAC" ] ; then
  JAVAC=${JAVA_HOME}/bin/javac;
fi

echo ... Bootstrapping Ant Distribution

rm -rf build

TOOLS=src/main/org/apache/tools
CLASSDIR=build/classes

mkdir -p build
mkdir -p ${CLASSDIR}

echo ... Compiling Ant Classes

export CLASSPATH=$LOCALCLASSPATH:src/main

# For Cygwin, switch paths to Windows format before running javac
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

${JAVAC} -d ${CLASSDIR} ${TOOLS}/tar/*.java \
    ${TOOLS}/ant/util/regexp/RegexpMatcher.java \
    ${TOOLS}/ant/util/regexp/RegexpMatcherFactory.java \
    ${TOOLS}/ant/util/*.java ${TOOLS}/ant/types/*.java \
    ${TOOLS}/ant/*.java ${TOOLS}/ant/taskdefs/*.java

echo ... Copying Required Files

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties \
    ${CLASSDIR}/org/apache/tools/ant/taskdefs
cp src/main/org/apache/tools/ant/types/defaults.properties \
    ${CLASSDIR}/org/apache/tools/ant/types

echo ... Building Ant Distribution

export CLASSPATH=$LOCALCLASSPATH:build/classes

# For Cygwin, switch paths to Windows format before running javac
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

${JAVA_HOME}/bin/java -classpath ${CLASSPATH} org.apache.tools.ant.Main $*

echo ... Done Bootstrapping Ant Distribution
