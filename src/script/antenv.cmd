/* 
    Copyright (c) 2003 The Apache Software Foundation.  All rights
    reserved.

    Ant environment 
*/

call RxFuncAdd "SysLoadFuncs", "RexxUtil", "SysLoadFuncs"
call SysLoadFuncs

/* Prepare the parameters for later use */
parse arg argv
mode = ''
args = ''
opts = ''
cp = ''
lcp = ''

do i = 1 to words(argv)
  param = word(argv, i)
  select
    when param='-lcp' then mode = 'l'
    when param='-cp' | param='-classpath' then mode = 'c'
    when abbrev('-opts', param, 4) then mode = 'o'
    when abbrev('-args', param, 4) then mode = 'a'
  otherwise
    select
      when mode = 'a' then args = space(args param, 1)
      when mode = 'c' then cp = space(cp param, 1)
      when mode = 'l' then lcp = space(lcp param, 1)
      when mode = 'o' then opts = space(opts param, 1)
    otherwise
      say 'Option' param 'ignored'
    end
  end
end

env="OS2ENVIRONMENT"
antconf = _getenv_('antconf' 'antconf.cmd')
runrc = _getenv_('runrc')
interpret 'call "' || runrc || '"' '"' || antconf || '"' 'ETC'
ANT_HOME = value('ANT_HOME',,env)
JAVA_HOME = value('JAVA_HOME',,env)
classpath = value('CLASSPATH',,env)
classes = stream(JAVA_HOME || "\lib\classes.zip", "C", "QUERY EXISTS")
if classes \= '' then classpath = prepend(classpath classes)
classes = stream(JAVA_HOME || "\lib\tools.jar", "C", "QUERY EXISTS")
if classes \= '' then classpath = prepend(classpath classes)

mincp = classpath
call SysFileTree ANT_HOME || '\lib\*.jar', 'jar', 'FO'
do i = 1 to jar.0
  nm = filespec('name', jar.i)
  if pos('ant-', nm) == 0 then classpath = prepend(classpath jar.i)
end
if length(classpath) > 512 then do
  say 'Classpath is too long, switching to the minimal version...'
  say '... some tasks will not work'
  classpath = mincp
  classpath = prepend(classpath ANT_HOME || '\lib\ant.jar')
  classpath = prepend(classpath ANT_HOME || '\lib\optional.jar')
end

'SET CLASSPATH=' || classpath

/* Setting classpathes, options and arguments */
envset = _getenv_('envset')
if cp\=''   then interpret 'call "' || envset || '"' '"; CLASSPATH"' '"' || cp || '"'
if lcp\=''  then interpret 'call "' || envset || '"' '"; LOCALCLASSPATH"' '"' || lcp || '"'
if opts\='' then interpret 'call "' || envset || '"' '"-D ANT_OPTS"' '"' || opts || '"'
if args\='' then interpret 'call "' || envset || '"' '"ANT_ARGS"' '"' || args || '"'

exit 0

addpath: procedure
parse arg path elem
if elem = '' then do
  if path\='' & right(path, 1)\=';' then path = path || ';'
  return path
end
if substr(path, length(path)) = ';' then glue = ''
else glue = ';'
if pos(translate(elem), translate(path)) = 0 then path = path || glue || elem || ';'
return path

prepend: procedure
parse arg path elem
if elem = '' then do
  if path\='' & right(path, 1)\=';' then path = path || ';'
  return path
end
if pos(translate(elem), translate(path)) = 0 then path = elem || ';' || path
return path

_getenv_: procedure expose env
parse arg envar default
if default = '' then default = envar
var = value(translate(envar),,env)
if var = '' then var = default
return var
