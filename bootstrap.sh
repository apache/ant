#!/bin/sh

if [ -f $HOME/.antrc ] ; then 
  . $HOME/.antrc
fi

SRCDIR=src/main/org/apache/tools
CLASSDIR=classes

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

CLASSPATH=${CLASSPATH}:${CLASSDIR}:src/main

mkdir -p ${CLASSDIR}

export CLASSPATH

echo
echo Building with classpath: $CLASSPATH
echo

javac  -d ${CLASSDIR} ${SRCDIR}/tar/*.java
javac  -d ${CLASSDIR} ${SRCDIR}/ant/*.java
javac  -d ${CLASSDIR} ${SRCDIR}/ant/taskdefs/*.java

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties ${CLASSDIR}/org/apache/tools/ant/taskdefs

java org.apache.tools.ant.Main clean main install
java org.apache.tools.ant.Main clean 

if test ! -d bin; then mkdir bin; fi
cp src/bin/antRun bin
chmod +x bin/antRun

rm -rf ${CLASSDIR}

