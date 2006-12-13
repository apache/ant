/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
     * Ant version property. {@value}
     */
    public static final String ANT_VERSION = "ant.version";

    /**
     * System classpath policy. {@value}
     */
    public static final String BUILD_SYSCLASSPATH = "build.sysclasspath";

    /**
     * The name of the script repository used by the script repo task
     * Value {@value}
     */
    public static final String SCRIPT_REPOSITORY = "org.apache.ant.scriptrepo";

    /**
     * The name of the reference to the System Class Loader
     * Value {@value}
     **/
    public static final String SYSTEM_LOADER_REF = "ant.coreLoader";

    /**
     * Name of the property which can provide an override of the repository dir
     * for the libraries task
     * Value {@value}
     */
    public static final String REPOSITORY_DIR_PROPERTY = "ant.maven.repository.dir";
    /**
     * Name of the property which can provide an override of the repository URL
     * for the libraries task
     * Value {@value}
     */
    public static final String REPOSITORY_URL_PROPERTY = "ant.maven.repository.url";

    /**
     * name of the resource that taskdefs are stored under
     * Value: {@value}
     */
    public static final String TASKDEF_PROPERTIES_RESOURCE =
            "/org/apache/tools/ant/taskdefs/defaults.properties";
    /**
     * name of the resource that typedefs are stored under
     * Value: {@value}
     */
    public static final String TYPEDEFS_PROPERTIES_RESOURCE =
            "/org/apache/tools/ant/types/defaults.properties";

    /**
     * Reference to the current Ant executor
     * Value: {@value}
     */
    public static final String ANT_EXECUTOR_REFERENCE = "ant.executor";

    /**
     * Property defining the classname of an executor.
     * Value: {@value}
     */
    public static final String ANT_EXECUTOR_CLASSNAME = "ant.executor.class";
    /**
     * property name for basedir of the project
     * Value: {@value}
     */
    public static final String PROJECT_BASEDIR = "basedir";
    /**
     * property for ant file name
     * Value: {@value}
     */
    public static final String ANT_FILE = "ant.file";

    /**
     * Property used to store the java version ant is running in.
     * @since Ant 1.7
     */
    public static final String ANT_JAVA_VERSION = "ant.java.version";

    /**
     * Property used to store the location of ant.
     * @since Ant 1.7
     */
    public static final String ANT_HOME = Launcher.ANTHOME_PROPERTY;

    /**
     * Property used to store the location of the ant library (typically the ant.jar file.)
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
     * @since Ant 1.7
     * Value: {@value}
     */
    public static final String BUILD_JAVAC_SOURCE = "ant.build.javac.source";

    /**
     * property that provides the default value for javac's target
     * attribute.
     * @since Ant 1.7
     * Value: {@value}
     */
    public static final String BUILD_JAVAC_TARGET = "ant.build.javac.target";

    /**
     * Name of the magic property that controls classloader reuse
     * @since Ant 1.4.
     * Value: {@value}
     */
    public static final String REFID_CLASSPATH_REUSE_LOADER = "ant.reuse.loader";

    /**
     * Prefix used to store classloader references.
     * Value: {@value}
     */
    public static final String REFID_CLASSPATH_LOADER_PREFIX = "ant.loader.";

    /**
     * Reference used to store the property helper
     * Value: {@value}
     */
    public static final String REFID_PROPERTY_HELPER = "ant.PropertyHelper";

}

