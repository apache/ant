#
#  Copyright  2002-2005 Apache Software Foundation
# 
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
# 
#       http://www.apache.org/licenses/LICENSE-2.0
# 
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
# 
#

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
# Usage YEAR [all]
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

if [ -d ".svn" ]; then
    svn up | fgrep -v 'At revision' > "$TEMP_DIR"/update
else
    cvs -z3 update -dP > "$TEMP_DIR"/update
fi

if [ -z "$1" ]; then
   fgrep -v proposal < "$TEMP_DIR"/update | cut -f 2 -d ' ' > "$TEMP_DIR"/changed-files
else
  if [ "all" == "$1" ]; then
    cut -f 2 -d ' ' < "$TEMP_DIR"/update > "$TEMP_DIR"/changed-files
  else
    echo "Usage: $YEAR [all]"
    exit
  fi
fi

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

rm "$TEMP_DIR"/no-$YEAR "$TEMP_DIR"/no-copyright "$TEMP_DIR"/changed-files "$TEMP_DIR"/update
