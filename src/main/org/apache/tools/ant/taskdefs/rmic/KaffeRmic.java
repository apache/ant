/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the rmic for Kaffe
 *
 * @since Ant 1.4
 */
public class KaffeRmic extends DefaultRmicAdapter {
    public static final String RMIC_CLASSNAME = "kaffe.rmi.rmic.RMIC";
    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "kaffe";


    public boolean execute() throws BuildException {
        getRmic().log("Using Kaffe rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand();

        try {

            Class c = Class.forName(RMIC_CLASSNAME);
            Constructor cons = c.getConstructor(new Class[] {String[].class});
            Object rmic = cons.newInstance(new Object[] {cmd.getArguments()});
            Method doRmic = c.getMethod("run", null);
            Boolean ok = (Boolean) doRmic.invoke(rmic, null);

            return ok.booleanValue();
        } catch (ClassNotFoundException ex) {
            throw new BuildException("Cannot use Kaffe rmic, as it is not "
                                     + "available.  A common solution is to "
                                     + "set the environment variable "
                                     + "JAVA_HOME or CLASSPATH.",
                                     getRmic().getLocation());
        } catch (BuildException ex) {
            //rethrow
            throw ex;
        } catch (Exception ex) {
            //wrap
           throw new BuildException("Error starting Kaffe rmic: ",
                                         ex, getRmic().getLocation());
        }
    }

    /**
     * test for kaffe being on the system
     * @return true if kaffe is on the current classpath
     */
    public static boolean isAvailable() {
        try {
            Class.forName(RMIC_CLASSNAME);
            return true;
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }
}
