#!/bin/sh

if test ! -f build\classes\org\apache\tools\ant\Main.class ; then
  ./bootstrap.sh
fi    


# Cygwin support.  $cygwin _must_ be set to either true or false.
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  *) cygwin=false ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

LOCALCLASSPATH=`echo lib/*.jar | tr ' ' ':'`

if [ "$CLASSPATH" != "" ] ; then
  LOCALCLASSPATH=$CLASSPATH:$LOCALCLASSPATH
fi

if [ "$JAVA_HOME" != "" ] ; then
  if test -f $JAVA_HOME/lib/tools.jar ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if test -f $JAVA_HOME/lib/classes.zip ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
else
  echo "Warning: JAVA_HOME environment variable is not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

if [ ! -x "$JAVA_HOME/bin/java" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute JAVA_HOME/bin/java"
  exit
fi

# For Cygwin, switch paths to Windows format before running javac
if $cygwin; then
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
fi

${JAVA_HOME}/bin/java -classpath $LOCALCLASSPATH org.apache.tools.ant.Main -logger org.apache.tools.ant.NoBannerLogger -emacs $*


