#!/bin/sh

# You will need to specify JAVA_HOME if compiling with 1.2 or later.

if [ "$JAVA_HOME" != "" ] ; then
  if [ -f $JAVA_HOME/lib/tools.jar ] ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
  fi
 
  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
else
  echo "Warning: JAVA_HOME environment variable not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

if [ ! -x "$JAVA_HOME/bin/java" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute JAVA_HOME/bin/java"
  exit
fi

# More Cygwin support
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
  CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

ANT_HOME=.
export ANT_HOME

if [ -z "$JAVAC" ] ; then
  JAVAC=${JAVA_HOME}/bin/javac;
fi

echo ... Bootstrapping Ant Distribution

if [ -f "lib/ant.jar" ] ; then
  rm lib/ant.jar
fi
if [ -f "lib/optional.jar" ] ; then
  rm lib/optional.jar
fi

# add in the dependency .jar files
DIRLIBS=${ANT_HOME}/lib/*.jar
for i in ${DIRLIBS}
do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
    if [ "$i" != "${DIRLIBS}" ] ; then
        CLASSPATH=$CLASSPATH:"$i"
    fi
done
DIRCORELIBS=${ANT_HOME}/lib/core/*.jar
for i in ${DIRCORELIBS}
do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
    if [ "$i" != "${DIRCORELIBS}" ] ; then
        CLASSPATH=$CLASSPATH:"$i"
    fi
done

TOOLS=src/main/org/apache/tools
CLASSDIR=classes

CLASSPATH=${CLASSDIR}:src/main:${CLASSPATH}

# convert the unix path to windows
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
   CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

export CLASSPATH

mkdir -p ${CLASSDIR}

echo ... Compiling Ant Classes

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

${JAVA_HOME}/bin/java -classpath ${CLASSPATH} org.apache.tools.ant.Main \
                      -buildfile build.xml clean main bootstrap

echo ... Cleaning Up Build Directories

chmod +x bin/ant bin/antRun

rm -rf ${CLASSDIR}

echo ... Done Bootstrapping Ant Distribution
