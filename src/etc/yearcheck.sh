#!/bin/sh

# 
#  The Apache Software License, Version 1.1
# 
#  Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
#  reserved.
# 
#  Redistribution and use in source and binary forms, with or without
#  modification, are permitted provided that the following conditions
#  are met:
# 
#  1. Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
# 
#  2. Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in
#     the documentation and/or other materials provided with the
#     distribution.
# 
#  3. The end-user documentation included with the redistribution, if
#     any, must include the following acknowlegement:
#        "This product includes software developed by the
#         Apache Software Foundation (http://www.apache.org/)."
#     Alternately, this acknowlegement may appear in the software itself,
#     if and wherever such third-party acknowlegements normally appear.
# 
#  4. The names "Ant" and "Apache Software
#     Foundation" must not be used to endorse or promote products derived
#     from this software without prior written permission. For written
#     permission, please contact apache@apache.org.
# 
#  5. Products derived from this software may not be called "Apache"
#     nor may "Apache" appear in their names without prior written
#     permission of the Apache Group.
# 
#  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
#  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
#  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
#  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
#  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
#  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
#  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
#  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
#  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
#  SUCH DAMAGE.
#  ====================================================================
# 
#  This software consists of voluntary contributions made by many
#  individuals on behalf of the Apache Software Foundation.  For more
#  information on the Apache Software Foundation, please see
#  <http://www.apache.org/>.
# 

#
# Simple shell script that checks whether changed files contain a copyright
# statement for a given year.
#
# Rename (or symlink) this script to the year you want to check, i.e. name it
# 2002 if you want to check for copyright statements that do not contain#
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
