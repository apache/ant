#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Simple shell script that checks whether changed files contain a copyright
# statement.
#
#
# Use this script instead of your usual git pull command.
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

SCM_COMMAND=git
SCM_ARGS="diff --name-only ..origin"

git fetch

"$SCM_COMMAND" $SCM_ARGS > "$TEMP_DIR"/update


thefile=$(cat "$TEMP_DIR"/update)
for afile in $thefile
do
    if [ -f ${afile} ]; then
       echo $afile >> "$TEMP_DIR"/changed-files
    fi
done
echo "Changed:"
echo "========"
cat "$TEMP_DIR"/changed-files
echo

xargs fgrep -L Copyright < "$TEMP_DIR"/changed-files > "$TEMP_DIR"/no-copyright

echo "No Copyright line"
echo "================="
cat "$TEMP_DIR"/no-copyright
echo

rm "$TEMP_DIR"/no-copyright "$TEMP_DIR"/changed-files "$TEMP_DIR"/update
