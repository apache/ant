#!/bin/sh

REALANTHOME=$ANT_HOME
ANT_HOME=.
export ANT_HOME

if test ! -f lib/ant.jar -o  ! -x bin/ant -o ! -x bin/antRun ; then
  ./bootstrap.sh
fi    

if [ "$REALANTHOME" != "" ] ; then
  ANT_INSTALL="-Dant.install $REALANTHOME"
fi

bin/ant $ANT_INSTALL $*

