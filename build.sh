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

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

REALANTHOME=$ANT_HOME
if [ -z "$PWD" ]; then
    ANT_HOME=./bootstrap
else
    ANT_HOME="$PWD"/bootstrap
fi
export ANT_HOME

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  /bin/sh ./bootstrap.sh
fi

if test ! -f bootstrap/lib/ant.jar -o  ! -x bootstrap/bin/ant -o ! -x bootstrap/bin/antRun ; then
  echo Bootstrap FAILED
  exit 1
fi

if [ "$REALANTHOME" != "" ] ; then
  if $cygwin; then
     REALANTHOME=`cygpath --windows "$REALANTHOME"`
  fi
  ANT_INSTALL="-Dant.install=$REALANTHOME"
else
  ANT_INSTALL="-emacs"
fi

bootstrap/bin/ant -nouserlib -lib lib/optional "$ANT_INSTALL" $*

