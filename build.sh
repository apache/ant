#!/bin/sh

if [ "$ANT_HOME" = "" ] ; then
  ANT_HOME=`pwd`
fi
export ANT_HOME

LOCALCLASSPATH=`echo $ANT_HOME/lib/*.jar | tr ' ' ':'`

if [ "$CLASSPATH" != "" ] ; then
  LOCALCLASSPATH=$CLASSPATH:$LOCALCLASSPATH
fi

if test -f $JAVA_HOME/lib/tools.jar ; then
  LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
fi

if test -f $JAVA_HOME/lib/classes.zip ; then
  LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
fi

echo
echo Building with classpath: $LOCALCLASSPATH
echo

chmod 0755 $ANT_HOME/bin/antRun

java -Dant.home=$ANT_HOME -classpath $LOCALCLASSPATH $ANT_OPTS org.apache.tools.ant.Main $*
