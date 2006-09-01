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

package org.apache.tools.ant.taskdefs.rmic;

import java.lang.reflect.Method;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the rmic for WebLogic
 *
 * @since Ant 1.4
 */
public class WLRmic extends DefaultRmicAdapter {
    /** The classname of the weblogic rmic */
    public static final String WLRMIC_CLASSNAME = "weblogic.rmic";
    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "weblogic";

    /** The error string to use if not able to find the weblogic rmic */
    public static final String ERROR_NO_WLRMIC_ON_CLASSPATH =
        "Cannot use WebLogic rmic, as it is not "
        + "available.  A common solution is to "
        + "set the environment variable "
        + "CLASSPATH.";

    /** The error string to use if not able to start the weblogic rmic */
    public static final String ERROR_WLRMIC_FAILED = "Error starting WebLogic rmic: ";
    /** The stub suffix */
    public static final String WL_RMI_STUB_SUFFIX = "_WLStub";
    /** The skeleton suffix */
    public static final String WL_RMI_SKEL_SUFFIX = "_WLSkel";

    /**
     * Carry out the rmic compilation.
     * @return true if the compilation succeeded
     * @throws  BuildException on error
     */
    public boolean execute() throws BuildException {
        getRmic().log("Using WebLogic rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand(new String[] {"-noexit"});

        AntClassLoader loader = null;
        try {
            // Create an instance of the rmic
            Class c = null;
            if (getRmic().getClasspath() == null) {
                c = Class.forName(WLRMIC_CLASSNAME);
            } else {
                loader
                    = getRmic().getProject().createClassLoader(getRmic().getClasspath());
                c = Class.forName(WLRMIC_CLASSNAME, true, loader);
            }
            Method doRmic = c.getMethod("main",
                                        new Class [] {String[].class});
            doRmic.invoke(null, new Object[] {cmd.getArguments()});
            return true;
        } catch (ClassNotFoundException ex) {
            throw new BuildException(ERROR_NO_WLRMIC_ON_CLASSPATH, getRmic().getLocation());
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException(ERROR_WLRMIC_FAILED, ex,
                                         getRmic().getLocation());
            }
        } finally {
            if (loader != null) {
                loader.cleanup();
            }
        }
    }

    /**
     * Get the suffix for the rmic stub classes
     * @return the stub suffix
     */
    public String getStubClassSuffix() {
        return WL_RMI_STUB_SUFFIX;
    }

    /**
     * Get the suffix for the rmic skeleton classes
     * @return the skeleton suffix
     */
    public String getSkelClassSuffix() {
        return WL_RMI_SKEL_SUFFIX;
    }
}
