#!/bin/sh

ADDL_CLASSPATH=./../jakarta-tools/ant.jar:./../jakarta-tools/projectx-tr2.jar

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$CLASSPATH:$ADDL_CLASSPATH
else
 CLASSPATH=$ADDL_CLASSPATH
fi
export CLASSPATH

echo Building with classpath $CLASSPATH

java org.apache.tools.ant.Main $*
