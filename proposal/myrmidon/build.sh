#!/bin/sh

echo
echo "Ant Build System"
echo "----------------"

export MYRMIDON_HOME=tools

chmod u+x $MYRMIDON_HOME/bin/antRun
chmod u+x $MYRMIDON_HOME/bin/ant

export ANT_HOME=
export CLASSPATH=lib/xerces.jar

$MYRMIDON_HOME/bin/ant -logger org.apache.tools.ant.NoBannerLogger -emacs $@
