#!/bin/sh

#   Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
#   reserved.

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

REALANTHOME=$ANT_HOME
if [ -z "$PWD" ]; then
    ANT_HOME=./bootstrap
else
    ANT_HOME="$PWD"/bootstrap
fi
export ANT_HOME

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  /bin/sh ./bootstrap.sh
fi

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  echo Bootstrap FAILED
  exit
fi

if [ "$REALANTHOME" != "" ] ; then
  if $cygwin; then
     REALANTHOME=`cygpath --windows "$REALANTHOME"`
  fi
  ANT_INSTALL="-Dant.install=$REALANTHOME"
else
  ANT_INSTALL="-emacs"
fi

bootstrap/bin/ant -nouserlib -lib lib/optional "$ANT_INSTALL" $*

