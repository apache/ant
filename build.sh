#!/bin/sh

REALANTHOME=$ANT_HOME
ANT_HOME=bootstrap
export ANT_HOME

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  /bin/sh ./bootstrap.sh
fi    

LOCALCLASSPATH=lib/parser.jar:lib/jaxp.jar
# add in the dependency .jar files
DIRLIBS=lib/optional/*.jar
for i in ${DIRLIBS}
do
    if [ "$i" != "${DIRLIBS}" ] ; then
        LOCALCLASSPATH=$LOCALCLASSPATH:"$i"
    fi
done

CLASSPATH=$LOCALCLASSPATH:$CLASSPATH
export CLASSPATH


if [ "$REALANTHOME" != "" ] ; then
  ANT_INSTALL="-Dant.install=$REALANTHOME"
fi

bootstrap/bin/ant $ANT_INSTALL $*

