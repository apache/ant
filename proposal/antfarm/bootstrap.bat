@if not exist boot mkdir boot
@if not exist boot\tasks mkdir boot\tasks
@if not exist boot\xml mkdir boot\xml
@if not exist temp mkdir temp
@if not exist temp\core mkdir temp\core
@if not exist temp\xml mkdir temp\xml
@if not exist temp\tasks mkdir temp\tasks

javac -classpath "" -d temp\core core\org\apache\tools\ant\*.java core\org\apache\tools\ant\cmdline\*.java core\*.java 
@if errorlevel 1 goto end

jar -cfm boot\ant.jar core\META-INF\manifest.mf -C temp\core .
@if errorlevel 1 goto end

javac -classpath "boot\ant.jar;jaxp\jaxp.jar;jaxp\crimson.jar" -d temp\xml xml\org\apache\tools\ant\xml\*.java
@if errorlevel 1 goto end

jar -cf boot\xml\ant-xml.jar -C temp\xml .
@if errorlevel 1 goto end

javac -classpath "boot\ant.jar" -d temp\tasks tasks\org\apache\tools\ant\tasks\*.java
@if errorlevel 1 goto end

copy tasks\java2sdk.ant temp\tasks\java2sdk.ant

jar -cf boot\tasks\java2sdk.jar -C temp\tasks .
@if errorlevel 1 goto end

copy jaxp\jaxp.jar boot\xml\jaxp.jar
copy jaxp\crimson.jar boot\xml\crimson.jar

@rmdir /s /q temp


:end