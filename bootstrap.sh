#!/bin/sh

if [ -f $HOME/.antrc ] ; then 
  . $HOME/.antrc
fi

SRCDIR=src/main/org/apache/tools
CLASSDIR=classes
CLASSPATH=${CLASSPATH}:${JAVA_HOME}/lib/classes.zip:${JAVA_HOME}/lib/tools.jar
CLASSPATH=${CLASSPATH}:src/main:${CLASSDIR}

mkdir -p ${CLASSDIR}

export CLASSPATH
echo $CLASSPATH

javac  -d ${CLASSDIR} ${SRCDIR}/tar/*.java
javac  -d ${CLASSDIR} ${SRCDIR}/ant/*.java
javac  -d ${CLASSDIR} ${SRCDIR}/ant/taskdefs/*.java

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties ${CLASSDIR}/org/apache/tools/ant/taskdefs

java org.apache.tools.ant.Main clean main
java org.apache.tools.ant.Main clean 

if test ! -d bin; then mkdir bin; fi
cp src/bin/antRun bin
chmod +x bin/antRun

rm -rf ${CLASSDIR}

