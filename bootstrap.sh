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

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# You will need to specify JAVA_HOME if compiling with 1.2 or later.

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
else
  echo "Warning: JAVA_HOME environment variable not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

# IBM's JDK on AIX uses strange locations for the executables:
# JAVA_HOME/jre/sh for java and rmid
# JAVA_HOME/sh for javac and rmic
if [ -z "$JAVAC" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/sh/javac" ] ; then
      JAVAC=${JAVA_HOME}/sh/javac;
    else
      JAVAC=${JAVA_HOME}/bin/javac;
    fi
  else
    JAVAC=javac
  fi
fi
if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      JAVACMD=$JAVA_HOME/jre/sh/java
    else
      JAVACMD=$JAVA_HOME/bin/java
    fi
  else
    JAVACMD=java
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit
fi

ANT_HOME=.
export ANT_HOME

echo ... Bootstrapping Ant Distribution

if [ -d "bootstrap" ] ; then
  rm -r bootstrap
fi

if [ -d "build" ] ; then
  rm -r build
fi

CLASSPATH=lib/xercesImpl.jar:lib/xml-apis.jar:${CLASSPATH}

DIRLIBS=lib/optional/*.jar
for i in ${DIRLIBS}
do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
    if [ "$i" != "${DIRLIBS}" ] ; then
        CLASSPATH=$CLASSPATH:"$i"
    fi
done

TOOLS=src/main/org/apache/tools
CLASSDIR=build/classes

CLASSPATH=${CLASSDIR}:src/main:${CLASSPATH}

# For Cygwin, switch to Windows format before running java
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

export CLASSPATH

mkdir -p build
mkdir -p ${CLASSDIR}
mkdir -p bin

echo ... Compiling Ant Classes

"${JAVAC}" $BOOTJAVAC_OPTS -d ${CLASSDIR} ${TOOLS}/bzip2/*.java ${TOOLS}/tar/*.java ${TOOLS}/zip/*.java \
    ${TOOLS}/ant/util/regexp/RegexpMatcher.java \
    ${TOOLS}/ant/util/regexp/RegexpMatcherFactory.java \
    ${TOOLS}/ant/types/*.java \
    ${TOOLS}/ant/*.java ${TOOLS}/ant/taskdefs/*.java \
    ${TOOLS}/ant/taskdefs/compilers/*.java \
    ${TOOLS}/ant/taskdefs/condition/*.java
ret=$?
if [ $ret != 0 ]; then  
  echo ... Failed compiling Ant classes !
  exit $ret
fi

echo ... Copying Required Files

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties \
    ${CLASSDIR}/org/apache/tools/ant/taskdefs
cp src/main/org/apache/tools/ant/types/defaults.properties \
    ${CLASSDIR}/org/apache/tools/ant/types
cp src/script/antRun bin
chmod +x bin/antRun

echo ... Building Ant Distribution

"${JAVACMD}" -classpath "${CLASSPATH}" -Dant.home=. $ANT_OPTS org.apache.tools.ant.Main -emacs "$@" bootstrap
ret=$?
if [ $ret != 0 ]; then  
  echo ... Failed Building Ant Distribution !
  exit $ret
fi


echo ... Cleaning Up Build Directories

rm -rf ${CLASSDIR}
rm -rf bin

echo ... Done Bootstrapping Ant Distribution
