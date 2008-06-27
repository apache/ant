#!/bin/sh

cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

PWD=`pwd`
if $cygwin ; then
    PWD=`cygpath --windows "$PWD"`
fi

cd ../../..
/bin/sh ./build.sh -buildfile $PWD/build.xml $*
cd $PWD

