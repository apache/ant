/* 
    Copyright (c) 2003 The Apache Software Foundation.  All rights
    reserved.

    Run RC file, name is in the first arg, second arg is either PATH
    ENV  or -r or nothing 
*/

parse arg name path rest

if name = '' then do
  say 'RC file name is missing'
  exit 1
end

if rest \= '' then do
  say 'Too many parameters'
  exit 1
end

call runit name path
exit 0

runit: procedure
parse arg name path dir

if path \= '' & path \= '-r' then do
  dir = value(translate(path),,'OS2ENVIRONMENT')
  if dir = '' then return
  dir = translate(dir, '\', '/') /* change UNIX-like path to OS/2 */
end

if dir = '' then dir = directory()

if path = '-r' then do /* recursive call */
  subdir = filespec('path', dir)
  if subdir \= '\' then do
    subdir = left(subdir, length(subdir)-1)
    call runit name path filespec('drive', dir) || subdir
  end
end

/* Look for the file and run it */
if right(dir, 1) \= '\' then dir = dir || '\'
rcfile = stream(dir || name, 'c', 'query exists')
if rcfile \= '' then interpret 'call "' || rcfile || '"'

return
