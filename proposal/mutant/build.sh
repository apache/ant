#!/bin/sh

#   Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
#   reserved.

java -classpath bootstrap/lib/start.jar:bootstrap/lib/init.jar org.apache.ant.start.Main $*
