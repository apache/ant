#!/bin/sh

REALANTHOME=$ANT_HOME
ANT_HOME=bootstrap
export ANT_HOME

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  /bin/sh ./bootstrap.sh
fi    

CLASSPATH=$CLASSPATH:lib/parser.jar:lib/jaxp.jar
export CLASSPATH

if [ "$REALANTHOME" != "" ] ; then
  ANT_INSTALL="-Dant.install=$REALANTHOME"
fi

bootstrap/bin/ant $ANT_INSTALL $*

