#!/bin/sh

echo
echo "Ant Build System"
echo "----------------"

chmod u+x $PWD/bin/antRun
chmod u+x $PWD/bin/ant
#export ANT_OPTS="-Djava.compiler="

ANT_HOME=.
export ANT_HOME

$PWD/bin/ant -emacs $@ | awk -f $PWD/bin/fixPath.awk
