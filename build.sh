#!/bin/sh

if [ "$ANT_HOME" == "" ] ; then
  ANT_HOME=`pwd`
fi
export ANT_HOME

ADDL_CLASSPATH=$ANT_HOME/lib/ant.jar

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$CLASSPATH:$ADDL_CLASSPATH
else
 CLASSPATH=$ADDL_CLASSPATH
fi
export CLASSPATH

echo Building with classpath $CLASSPATH

java org.apache.tools.ant.Main $*
