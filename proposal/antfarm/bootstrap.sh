#!/bin/sh

if ! test -d boot ; then
    mkdir -p boot/tasks
    mkdir -p boot/xml
fi

if ! test -d  temp ; then
    mkdir -p temp/core
    mkdir -p temp/xml
    mkdir -p temp/tasks
fi

if  test -z "$JAVAC" ; then
    JAVAC=javac;
fi

TOOLS=core/org/apache/tools

${JAVAC} -d temp/core ${TOOLS}/ant/*.java \
                      ${TOOLS}/ant/cmdline/*.java \
                      core/*.java

jar -cfm boot/ant.jar core/META-INF/manifest.mf -C temp/core .

${JAVAC} -classpath boot/ant.jar:jaxp/jaxp.jar:jaxp/crimson.jar -d temp/xml xml/org/apache/tools/ant/xml/*.java

jar -cf boot/xml/ant-xml.jar -C temp/xml .

${JAVAC} -classpath boot/ant.jar -d temp/tasks tasks/org/apache/tools/ant/tasks/*.java

cp tasks/java2sdk.ant temp/tasks/java2sdk.ant

jar -cf boot/tasks/java2sdk.jar -C temp/tasks .

cp jaxp/jaxp.jar boot/xml/jaxp.jar
cp jaxp/crimson.jar boot/xml/crimson.jar

#rm -rf temp
