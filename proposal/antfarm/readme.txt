*** Installing ***

To get things started you'll need to run the bootstrap.bat file, which will manually compile a version of ant into the "boot" directory. From then on you can use Ant to build Ant by running the build.bat file. (There aren't any unix scripts yet, unfortunately. Any help here would be appreciated!)

To "install" ant, you just need to have the ant.jar file in your classpath. Ant will figure out the rest. To run it, type:

java ant project:target

When ant is run, the Main class scans the classpath to find the ant.jar file, and can figure out from there where the rest of the files are. In particular, the jars in the "tasks" directory get added to the project path automatically. And the jars in the "xml" directory get loaded using a separate class loader, so that they don't conflict with the xml parsers that various tasks might be using.


*** Running ***

For now, the targets specified con the command line must be in the form project:target. For example, if you want to build target "all" in file "foo.ant", the target name would be "foo:all". In the future there will be a way to specify the default project, so that only the target name would need to be specified. 

Ant searches along the "project path" to find projects specified on the command line or in "import" statements. The project path defaults to ".", ie the current directory, but can be overridden by setting the "ant.project.path" system property. Variables and targets from other projects can be accessed by prefixing them with the project name and a colon.


*** Concepts ***

The main thing I'd like people to check out is the whole workspace concept, ie the ability to pull multiple ant files into a single build. I personally think this will make it easier to reuse ant files from other projects, and avoid recursive make syndrome at the same time. Plus, I think this model lends itself to CJAN implementation quite nicely.

I've also tried to make the tasks more compliant with the javabean spec. As a result of this, the word "property" became so overloaded that I decided to use the term "variable" for values defined in ant projects. If anyone feels strongly about it I'll change it back.

The depends attribute on targets uses a whitespace delimited list of target names, instead of comma separated. This is more consistent with the "list" datatype in the xml schema spec.

The parser is namespace aware, and attributes for any namespace other than the default namespace are ignored.

The code relies heavily on JDK 1.2 features. Making it JDK 1.1 compatible would be a lot of work, but is definitely doable.

There's obviously a lot of stuff missing from this prototype, such as datatypes, real tasks, a way to access system properties, default targets, etc. If enough people like the basic design I'll start tackling those next...

