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
ANT_HOME=bootstrap
export ANT_HOME

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  /bin/sh ./bootstrap.sh
fi    

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  echo Bootstrap FAILED
  exit
fi

LOCALCLASSPATH=lib/xercesImpl.jar:lib/xml-apis.jar:bootstrap/lib/ant.jar
# add in the dependency .jar files
DIRLIBS=lib/optional/*.jar
for i in ${DIRLIBS}
do
    if [ "$i" != "${DIRLIBS}" ] ; then
        LOCALCLASSPATH=$LOCALCLASSPATH:"$i"
    fi
done

# make sure the classpath is in unix format
if $cygwin ; then
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`    
fi

CLASSPATH=$LOCALCLASSPATH:$CLASSPATH

# switch back to Windows format
if $cygwin ; then
    CLASSPATH=`cygpath --path --windows "$CLASSPATH"`    
fi

export CLASSPATH


if [ "$REALANTHOME" != "" ] ; then
  ANT_INSTALL="-Dant.install=$REALANTHOME"
else
  ANT_INSTALL="-emacs"
fi

bootstrap/bin/ant -emacs "$ANT_INSTALL" $*

