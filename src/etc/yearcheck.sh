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
  TEMP_FILE="$TMP"/changed-files
else
  if [ -n "$TEMP" ]; then
    TEMP_FILE="$TEMP"/changed-files
  else
    TEMP_FILE=/tmp/changed-files
  fi
fi

YEAR=`basename $0`

if [ $YEAR = yearcheck.sh ]; then
    YEAR=`date -R | cut -d ' ' -f 4`
fi

if [ -z "$1" ]; then
  cvs -z3 update -dP | fgrep -v proposal | cut -f 2 -d ' ' > $TEMP_FILE
else
  if [ "all" == "$1" ]; then
    cvs -z3 update -dP | cut -f 2 -d ' ' > $TEMP_FILE
  else
    echo "Usage: $YEAR [all]"
    exit
  fi
fi

echo "Changed:"
echo "========"
cat $TEMP_FILE
echo

xargs fgrep -L Copyright < $TEMP_FILE > /tmp/no-copyright

echo "No Copyright line"
echo "================="
cat /tmp/no-copyright
echo

xargs egrep -L "Copyright.*$YEAR" < $TEMP_FILE | cut -f 1 -d : > /tmp/no-$YEAR

echo "No Copyright line for year $YEAR"
echo "================================"
cat /tmp/no-$YEAR

rm $TEMP_FILE
