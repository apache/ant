#!/bin/sh

echo
echo "Ant Build System"
echo "----------------"

chmod u+x $PWD/bin/antRun
chmod u+x $PWD/bin/ant

export ANT_HOME=.

$PWD/bin/ant -emacs $@
