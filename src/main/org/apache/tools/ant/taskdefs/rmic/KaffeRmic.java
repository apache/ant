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
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 * @since Ant 1.4
 */
public class KaffeRmic extends DefaultRmicAdapter {

    public boolean execute() throws BuildException {
        getRmic().log("Using Kaffe rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand();

        try {

            Class c = Class.forName("kaffe.rmi.rmic.RMIC");
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
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting Kaffe rmic: ",
                                         ex, getRmic().getLocation());
            }
        }
    }
}
