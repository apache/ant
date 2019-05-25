Building Ant with Maven
-----------------------

The Ant jars can be built using Maven and the POMS present in this directory.

Libs not available in the maven repository

groupId          artifactId       version   comment
com.bea          weblogic         8.1.3.0   download it
com.bea          weblogicclasses  5.1       a newer version can do.
jai              jai-core         1.1.2_01  fetch.xml
jai              jai-codec        1.1.2.1   fetch.xml
com.ibm.netrexx  netrexx          2.0.5     fetch.xml
com.starteam     starteam-sdk     5.2       the original file is called starteam-sdk.jar
stylebook        stylebook        1.0-b2    the original file is called stylebook-1.0-b2.jar

to install a jar file into your local Maven cache, do this

mvn install:install-file -DgroupId=foo.org -DartifactId=xx -Dversion=x.y -Dpackaging=jar -Dfile=/a/b/foo.jar

HOW TO BUILD :

from this directory, type

mvn install (or mvn package)

If you do not have all the dependencies, you can remove the modules that you will not be able to build
from the pom.xml in this directory.

You also might want to disable the tests.

mvn install -Dmaven.test.skip=true

TODO :

 * see if the dependency to weblogicclasses.jar can be replaced by a dependency to some j2ee.jar from Sun,
as it supplies some javax.ejb classes which are required at compile time.

PROBLEMS :

 * the unit tests cannot run properly, the maven-surefire-plugin sets a system property basedir
which make a large part of our tests fail

 * JIRA issue https://issues.apache.org/jira/browse/SUREFIRE-184 asking the Maven colleagues to fix this. :-)

REFERENCES :

about skipping tests :
https://maven.apache.org/plugins/maven-surefire-plugin/examples/skipping-test.html
