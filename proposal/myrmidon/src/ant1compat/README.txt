Myrmidon Ant1 compatibility layer.

This directory contains the source for the Ant1 compatibility layer.

DESCRIPTION
-----------
The layer works by reusing most of the Ant1 code, with tasks and datatypes
being prefixed with "ant1." in build files. Almost all of the main Ant1 tree
is included in the compatibility layer antlib. To insulate from changes in
the Ant1 tree, Ant1 class files are extracted from a jar, rather than
being compiled from scratch.

Here's how it works: The first time an Ant1 task is encountered, an Ant1
project is created, and stored in the TaskContext. The Ant1 versions of Task
and Project have been extended, with Task implementing Configurable so that
it may can mimic the Ant1 configuration policy using the IntrospectionHelper.

The idea is to provide hooks between the Ant1 project and the Myrmidon
project, eg
	logging: done
	properties: done but not quite working
	references: not done
	Task definitions: done.

So at present, while a <ant1:path> reference works fine in other <ant1:xxx>
tasks, it's not visible to the rest of the build, and vice-versa.

The <taskdef> task works ok, registering the task with the TypeManager using the
"ant1." prefix. Only a couple of DataTypes (Path and Patternset) are working
as top-level types, but this should be just a matter of adding references to
the Ant1 version of TypeInstanceTask in the descriptor.

The TransformingProjectBuilder (which is now the default builder for files
of type ".xml", applies a transformation stylesheet to the file, prefixing select
tasks (all at present) with "ant.". If a version attribute is encountered, the
file is not transformed

USAGE INSTRUCTIONS
------------------
Myrmidon will automatically attempt to upgrade any ".xml" build file that
doesn't have a version attribute on the root element. So, using an Ant1 build
file with Myrmidon should be as simple as:
    [myrmidon-command] -f ant1-build-file.xml

BUILD INSTRUCTIONS
------------------
* It is required that Myrmidon is first build by running the default target
  in the Myrmidon directory.
* Run "ant -f ant1compat.xml"

TODO
----
* Convert this to an Xdoc document
* Try out automatic registration of tasks - remove everything
  from ant-descriptor.xml and just use Project.addTaskDefinition()
  to register tasks? (similar for DataTypes)
* Get a version of <ant> and <antcall> working
* Test heaps more tasks
* Fix problem with classloaders and <taskdef>

