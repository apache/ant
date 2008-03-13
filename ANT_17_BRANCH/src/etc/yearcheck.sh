#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Simple shell script that checks whether changed files contain a copyright
# statement for a given year.
#
# Rename (or symlink) this script to the year you want to check, i.e. name it
# 2002 if you want to check for copyright statements that do not contain
# the year 2002.
#
# Use this script instead of your usual cvs update command.
#
# Usage YEAR [precommit]
#
# If the optional all argument has been omitted, the proposal directory will
# be skipped.
#

if [ -n "$TMP" ]; then
  TEMP_DIR="$TMP"
else
  if [ -n "$TEMP" ]; then
    TEMP_DIR="$TEMP"
  else
    TEMP_DIR=/tmp
  fi
fi

YEAR=`basename $0`

if [ $YEAR = yearcheck.sh ]; then
    YEAR=`date -R | cut -d ' ' -f 4`
fi

precommit_call=false
for arg in "$@" ; do
  if [ "$arg" = "precommit" ] ; then
    precommit_call=true
  fi
done

if [ -d ".svn" ]; then
  SCM_COMMAND=svn
  if $precommit_call ; then
    SCM_ARGS=status
    CUT_ARGS="-c 8-"
  else
    SCM_ARGS=up
    CUT_ARGS="-c 4-"
  fi
else
  SCM_COMMAND=cvs
  SCM_ARGS="-z3 update -dP"
  CUT_ARGS="-d ' ' -f 2"
fi

"$SCM_COMMAND" $SCM_ARGS > "$TEMP_DIR"/update-prefilter

# filter out boring lines
if [ "$SCM_COMMAND" = "svn" ]; then
  < "$TEMP_DIR"/update-prefilter fgrep -v 'At revision' | fgrep -v 'Updated to revision' | egrep -v '^\?' > "$TEMP_DIR"/update
else
  cp "$TEMP_DIR"/update-prefilter "$TEMP_DIR"/update
fi

cut $CUT_ARGS < "$TEMP_DIR"/update > "$TEMP_DIR"/changed-files

echo "Changed:"
echo "========"
cat "$TEMP_DIR"/changed-files
echo

xargs fgrep -L Copyright < "$TEMP_DIR"/changed-files > "$TEMP_DIR"/no-copyright

echo "No Copyright line"
echo "================="
cat "$TEMP_DIR"/no-copyright
echo

xargs egrep -L "Copyright.*$YEAR" < "$TEMP_DIR"/changed-files | cut -f 1 -d : > "$TEMP_DIR"/no-$YEAR

echo "No Copyright line for year $YEAR"
echo "================================"
cat "$TEMP_DIR"/no-$YEAR

rm "$TEMP_DIR"/no-$YEAR "$TEMP_DIR"/no-copyright "$TEMP_DIR"/changed-files "$TEMP_DIR"/update "$TEMP_DIR"/update-prefilter
