if [ -f $HOME/.antrc ] ; then 
  . $HOME/.antrc
fi

SRCDIR=src/main/org/apache/tools/ant
CLASSDIR=classes
CLASSPATH=${CLASSPATH}:${JAVA_HOME}/lib/classes.zip:${JAVA_HOME}/lib/tools.jar
CLASSPATH=${CLASSPATH}:lib/xml.jar:src/main:${CLASSDIR}

mkdir -p ${CLASSDIR}

export CLASSPATH
echo $CLASSPATH

javac  -d ${CLASSDIR} ${SRCDIR}/*.java
javac  -d ${CLASSDIR} ${SRCDIR}/taskdefs/*.java

cp src/main/org/apache/tools/ant/taskdefs/defaults.properties ${CLASSDIR}/org/apache/tools/ant/taskdefs
cp src/main/org/apache/tools/ant/parser.properties ${CLASSDIR}/org/apache/tools/ant

java org.apache.tools.ant.Main main
java org.apache.tools.ant.Main clean 

if test ! -d bin; then mkdir bin; fi
cp src/bin/antRun bin
chmod +x bin/antRun

rm -rf ${CLASSDIR}

