/* 
    Copyright (c) 2003 The Apache Software Foundation.  All rights
    reserved.

    Run ant 
*/

parse arg mode envarg '::' antarg

if mode\='.' & mode\='..' & mode\='/' then do
  envarg = mode envarg
  mode = ''
end

if antarg = '' then do
  antarg = envarg
  envarg = ''
end

x = setlocal()

env="OS2ENVIRONMENT"
antenv = _getenv_('antenv')
if _testenv_() = 0 then do
  interpret 'call "' || antenv || '"' '"' || envarg || '"'
  if _testenv_() = 0 then do
    say 'Ant environment is not set properly'
    x = endlocal()
    exit 16
  end
end

if mode = '' then mode = _getenv_('ANT_MODE' '..')
if mode \= '/' then do
  runrc = _getenv_('runrc')
  antrc = _getenv_('antrc' 'antrc.cmd')
  if mode = '..' then mode = '-r'
  else mode = ''
  interpret 'call "' || runrc || '"' antrc '"' || mode || '"'
end

settings = '-Dant.home=' || ANT_HOME '-Djava.home=' || JAVA_HOME

java = _getenv_('javacmd' 'java')
opts = value('ANT_OPTS',,env)
args = value('ANT_ARGS',,env)
lcp = value('LOCALCLASSPATH',,env)
if lcp\='' then lcp = '-cp' lcp

java opts lcp 'org.apache.tools.ant.Main' settings args antarg

x = endlocal()

return rc

_testenv_: procedure expose env ANT_HOME JAVA_HOME
ANT_HOME = value('ANT_HOME',,env)
if ANT_HOME = '' then return 0
JAVA_HOME = value('JAVA_HOME',,env)
if JAVA_HOME = '' then return 0
cp = translate(value('CLASSPATH',,env))
if pos(translate(ANT_HOME), cp) = 0 then return 0
if pos(translate(JAVA_HOME), cp) = 0 then return 0
return 1

_getenv_: procedure expose env
parse arg envar default
if default = '' then default = envar
var = value(translate(envar),,env)
if var = '' then var = default
return var
