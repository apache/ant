#!/usr/bin/python
"""

 runant.py

    This script is a translation of the runant.pl written by Steve Loughran.
    It runs ant with/out arguments, it should be quite portable (thanks to
    the python os library)
    This script has been tested with Python2.0/Win2K

        Copyright (c) 2001 The Apache Software Foundation.  All rights
        reserved.

 created:         2001-04-11
 author:          Pierre Dittgen pierre.dittgen@criltelecom.com

 Assumptions:

 - the "java" executable/script is on the command path
 - ANT_HOME has been set
"""
import os, os.path, string, sys

# Change it to 1 to get extra debug information
debug = 0

#######################################################################
#
# check to make sure environment is setup
#
if os.environ.has_key('ANT_HOME'):
    ANT_HOME = os.environ['ANT_HOME']
else:
    ANT_HOME = os.path.dirname(os.path.dirname(os.path.abspath(sys.argv[0])))

# Add jar files
ANT_LIB = os.path.join(ANT_HOME, 'lib')

if not os.environ.has_key('JAVACMD'):
    JAVACMD = 'java'
else:
    JAVACMD = os.environ['JAVACMD']

# Build up standard classpath
localpath = ''
if os.environ.has_key('CLASSPATH'):
    localpath = os.environ['CLASSPATH']
else:
    if debug:
        print 'Warning: no initial classpath\n'

launcher_jar = os.path.join(ANT_LIB, 'ant-launcher.jar')
if not os.path.exists(launcher_jar):
    print 'Unable to locate ant-launcher.jar. Expected to find it in %s' % \
        ANT_LIB
if localpath:
    localpath = launcher_jar + os.pathsep + localpath
else:
    localpath = launcher_jar

ANT_OPTS = []
if os.environ.has_key('ANT_OPTS'):
    ANT_OPTS = string.split(os.environ['ANT_OPTS'])

OPTS = []
if os.environ.has_key('JIKESPATH'):
    OPTS.append('-Djikes.class.path=' + os.environ['JIKESPATH'])

# Builds the commandline
cmdline = ('%s %s -classpath %s -Dant.home=\"%s\" %s ' + \
    'org.apache.tools.ant.launch.Launcher %s') \
     % (JAVACMD, string.join(ANT_OPTS,' '), localpath, ANT_HOME, \
        string.join(OPTS,' '), string.join(sys.argv[1:], ' '))

if debug:
    print '\n%s\n\n' % (cmdline)

# Run the biniou!
os.system(cmdline)
