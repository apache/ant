(setq jprj-base-path (message "%s/" (expand-file-name (substitute-in-file-name "."))) )
(setq jprj-src-path (message "%ssrc/java/" jprj-base-path) )
(setq jprj-compile-command "./build.bat")
(setq jprj-run-command "./build.bat&");; cd dist; bin/ant.bat -f ../src/make/sample.xmk&")
;(setq tab-expansion-size 4)

(load "update-prj")
