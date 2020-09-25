/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

import org.apache.tools.ant.launch.Launcher;

/**
 * Magic names used within Ant.
 *
 * Not all magic names are here yet.
 *
 * @since Ant 1.6
 */
public final class MagicNames {

    private MagicNames() {
    }

    /**
     * prefix for antlib URIs:
     * {@value}
     */
    public static final String ANTLIB_PREFIX = "antlib:";

    /**
     * Ant version property.
     * Value: {@value}
     */
    public static final String ANT_VERSION = "ant.version";

    /**
     * System classpath policy.
     * Value: {@value}
     */
    public static final String BUILD_SYSCLASSPATH = "build.sysclasspath";

    /**
     * The name of the script repository used by the script repo task.
     * Value {@value}
     */
    public static final String SCRIPT_REPOSITORY = "org.apache.ant.scriptrepo";

    /**
     * The name of the script cache used by the script runner.
     * Value {@value}
     */
    public static final String SCRIPT_CACHE = "org.apache.ant.scriptcache";

    /**
     * The name of the reference to the System Class Loader.
     * Value {@value}
     **/
    public static final String SYSTEM_LOADER_REF = "ant.coreLoader";

    /**
     * Name of the property which can provide an override of the repository dir.
     * for the libraries task
     * Value {@value}
     */
    public static final String REPOSITORY_DIR_PROPERTY = "ant.maven.repository.dir";

    /**
     * Name of the property which can provide an override of the repository URL.
     * for the libraries task
     * Value {@value}
     */
    public static final String REPOSITORY_URL_PROPERTY = "ant.maven.repository.url";

    /**
     * name of the resource that taskdefs are stored under.
     * Value: {@value}
     */
    public static final String TASKDEF_PROPERTIES_RESOURCE =
            "/org/apache/tools/ant/taskdefs/defaults.properties";

    /**
     * name of the resource that typedefs are stored under.
     * Value: {@value}
     */
    public static final String TYPEDEFS_PROPERTIES_RESOURCE =
            "/org/apache/tools/ant/types/defaults.properties";

    /**
     * Reference to the current Ant executor.
     * Value: {@value}
     */
    public static final String ANT_EXECUTOR_REFERENCE = "ant.executor";

    /**
     * Property defining the classname of an executor.
     * Value: {@value}
     */
    public static final String ANT_EXECUTOR_CLASSNAME = "ant.executor.class";

    /**
     * property name for basedir of the project.
     * Value: {@value}
     */
    public static final String PROJECT_BASEDIR = "basedir";

    /**
     * property for ant file name.
     * Value: {@value}
     */
    public static final String ANT_FILE = "ant.file";

    /**
     * property for type of ant build file (either file or url)
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String ANT_FILE_TYPE = "ant.file.type";

    /**
     * ant build file of type file
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String ANT_FILE_TYPE_FILE = "file";

    /**
     * ant build file of type url
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String ANT_FILE_TYPE_URL = "url";

    /**
     * Property used to store the java version ant is running in.
     * Value: {@value}
     * @since Ant 1.7
     */
    public static final String ANT_JAVA_VERSION = "ant.java.version";

    /**
     * Property used to store the location of ant.
     * Value: {@value}
     * @since Ant 1.7
     */
    public static final String ANT_HOME = Launcher.ANTHOME_PROPERTY;

    /**
     * Property used to store the location of the ant library (typically the ant.jar file.)
     * Value: {@value}
     * @since Ant 1.7
     */
    public static final String ANT_LIB = "ant.core.lib";

    /**
     * property for regular expression implementation.
     * Value: {@value}
     */
    public static final String REGEXP_IMPL = "ant.regexp.regexpimpl";

    /**
     * property that provides the default value for javac's and
     * javadoc's source attribute.
     * Value: {@value}
     * @since Ant 1.7
     */
    public static final String BUILD_JAVAC_SOURCE = "ant.build.javac.source";

    /**
     * property that provides the default value for javac's target attribute.
     * Value: {@value}
     * @since Ant 1.7
     */
    public static final String BUILD_JAVAC_TARGET = "ant.build.javac.target";

    /**
     * Name of the magic property that controls classloader reuse.
     * Value: {@value}
     * @since Ant 1.4.
     */
    public static final String REFID_CLASSPATH_REUSE_LOADER = "ant.reuse.loader";

    /**
     * Prefix used to store classloader references.
     * Value: {@value}
     */
    public static final String REFID_CLASSPATH_LOADER_PREFIX = "ant.loader.";

    /**
     * Reference used to store the property helper.
     * Value: {@value}
     */
    public static final String REFID_PROPERTY_HELPER = "ant.PropertyHelper";

    /**
     * Reference used to store the local properties.
     * Value: {@value}
     */
    public static final String REFID_LOCAL_PROPERTIES = "ant.LocalProperties";

    /**
     * Name of Ant core package
     * Value: {@value}
     * @since Ant 1.10.9
     */
    public static final String ANT_CORE_PACKAGE = "org.apache.tools.ant";

    /**
     * Name of JVM system property which provides the name of the ProjectHelper class to use.
     * Value: {@value}
     */
    public static final String PROJECT_HELPER_CLASS = ANT_CORE_PACKAGE + ".ProjectHelper";

    /**
     * The service identifier in jars which provide ProjectHelper implementations.
     * Value: {@value}
     */
    public static final String PROJECT_HELPER_SERVICE =
        "META-INF/services/" + PROJECT_HELPER_CLASS;

    /**
     * Name of ProjectHelper reference that we add to a project.
     * Value: {@value}
     */
    public static final String REFID_PROJECT_HELPER = "ant.projectHelper";

    /**
     * Name of the property holding the name of the currently
     * executing project, if one has been specified.
     *
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String PROJECT_NAME = "ant.project.name";

    /**
     * Name of the property holding the default target of the
     * currently executing project, if one has been specified.
     *
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String PROJECT_DEFAULT_TARGET
        = "ant.project.default-target";

    /**
     * Name of the property holding a comma separated list of targets
     * that have been invoked (from the command line).
     *
     * Value: {@value}
     * @since Ant 1.8.0
     */
    public static final String PROJECT_INVOKED_TARGETS
        = "ant.project.invoked-targets";

    /**
     * Name of the project reference holding an instance of {@link
     * org.apache.tools.ant.taskdefs.launcher.CommandLauncher} to use
     * when executing commands with the help of an external script.
     *
     * <p>Alternatively this is the name of a system property holding
     * the fully qualified class name of a {@link
     * org.apache.tools.ant.taskdefs.launcher.CommandLauncher}.</p>
     *
     * Value: {@value}
     * @since Ant 1.9.0
     */
    public static final String ANT_SHELL_LAUNCHER_REF_ID = "ant.shellLauncher";

    /**
     * Name of the project reference holding an instance of {@link
     * org.apache.tools.ant.taskdefs.launcher.CommandLauncher} to use
     * when executing commands without the help of an external script.
     *
     * <p>Alternatively this is the name of a system property holding
     * the fully qualified class name of a {@link
     * org.apache.tools.ant.taskdefs.launcher.CommandLauncher}.</p>
     *
     * Value: {@value}
     * @since Ant 1.9.0
     */
    public static final String ANT_VM_LAUNCHER_REF_ID = "ant.vmLauncher";
    /**
     * Name of the namespace "type".
     * (Note: cannot be used as an element.)
     * @since Ant 1.9.1
     */
    public static final String ATTRIBUTE_NAMESPACE = "attribute namespace";

    /**
     * Name of the property which can provide an override of the
     * User-Agent used in &lt;get&gt; tasks.
     * Value {@value}
     */
    public static final String HTTP_AGENT_PROPERTY = "ant.http.agent";

    /**
     * Magic property that can be set to contain a value for tstamp's
     * "now" in order to make builds that use the task create
     * reproducible results.
     *
     * <p>The value is expected to be a number representing the date
     * as seconds since the epoch.</p>
     *
     * Value: {@value}
     * @since Ant 1.10.2
     */
    public static final String TSTAMP_NOW = "ant.tstamp.now";

    /**
     * Magic property that can be set to contain a value for tstamp's
     * "now" in order to make builds that use the task create
     * reproducible results.
     *
     * <p>The value is expected to be in ISO time format
     * (<i>1972-04-17T08:07</i>)</p>
     *
     * Value: {@value}
     * @since Ant 1.10.2
     */
    public static final String TSTAMP_NOW_ISO = "ant.tstamp.now.iso";

    /**
     * Magic property that can be set to override the java.io.tmpdir
     * system property as the location for Ant's default temporary
     * directory.
     * Value: {@value}
     * @since Ant 1.10.8
     */
    public static final String TMPDIR = "ant.tmpdir";

    /**
     * Magic property that will be set to override java.io.tmpdir
     * system property as the location for Ant's default temporary
     * directory if a temp file is created and {@link #TMPDIR} is not
     * set.
     * Value: {@value}
     * @since Ant 1.10.9
     */
    public static final String AUTO_TMPDIR = "ant.auto.tmpdir";

    /**
     * Magic property that can be used to disable Nashorn compatibility mode when using GraalVM JavaScript as script
     * engine.
     *
     * <p>Set this to "true" if you want to disable Nashorn compatibility mode.</p>
     *
     * Value: {@value}
     * @since Ant 1.10.9
     */
    public static final String DISABLE_NASHORN_COMPAT = "ant.disable.graal.nashorn.compat";

}

