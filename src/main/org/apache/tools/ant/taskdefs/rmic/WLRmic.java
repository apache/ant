/* 
 * Copyright  2001-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 * @since Ant 1.4
 */
public class WLRmic extends DefaultRmicAdapter {

    public boolean execute() throws BuildException {
        getRmic().log("Using WebLogic rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand(new String[] {"-noexit"});

        AntClassLoader loader = null;
        try {
            // Create an instance of the rmic
            Class c = null;
            if (getRmic().getClasspath() == null) {
                c = Class.forName("weblogic.rmic");
            } else {
                loader
                    = getRmic().getProject().createClassLoader(getRmic().getClasspath());
                c = Class.forName("weblogic.rmic", true, loader);
            }
            Method doRmic = c.getMethod("main",
                                        new Class [] {String[].class});
            doRmic.invoke(null, new Object[] {cmd.getArguments()});
            return true;
        } catch (ClassNotFoundException ex) {
            throw new BuildException("Cannot use WebLogic rmic, as it is not "
                                     + "available.  A common solution is to "
                                     + "set the environment variable "
                                     + "CLASSPATH.", getRmic().getLocation());
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting WebLogic rmic: ", ex,
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
     */
    public String getStubClassSuffix() {
        return "_WLStub";
    }

    /**
     * Get the suffix for the rmic skeleton classes
     */
    public String getSkelClassSuffix() {
        return "_WLSkel";
    }
}
