/* 
 * Copyright  2001-2002,2004 Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Creates the necessary rmic adapter, given basic criteria.
 *
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @since 1.4
 */
public class RmicAdapterFactory {

    /** This is a singleton -- can't create instances!! */
    private RmicAdapterFactory() {
    }

    /**
     * Based on the parameter passed in, this method creates the necessary
     * factory desired.
     *
     * <p>The current mapping for rmic names are as follows:</p>
     * <ul><li>sun = SUN's rmic
     * <li>kaffe = Kaffe's rmic
     * <li><i>a fully quallified classname</i> = the name of a rmic
     * adapter
     * </ul>
     *
     * @param rmicType either the name of the desired rmic, or the
     * full classname of the rmic's adapter.
     * @param task a task to log through.
     * @throws BuildException if the rmic type could not be resolved into
     * a rmic adapter.
     */
    public static RmicAdapter getRmic(String rmicType, Task task)
        throws BuildException {
        if (rmicType.equalsIgnoreCase("sun")) {
            return new SunRmic();
        } else if (rmicType.equalsIgnoreCase("kaffe")) {
            return new KaffeRmic();
        } else if (rmicType.equalsIgnoreCase("weblogic")) {
            return new WLRmic();
        }
        return resolveClassName(rmicType);
    }

    /**
     * Tries to resolve the given classname into a rmic adapter.
     * Throws a fit if it can't.
     *
     * @param className The fully qualified classname to be created.
     * @throws BuildException This is the fit that is thrown if className
     * isn't an instance of RmicAdapter.
     */
    private static RmicAdapter resolveClassName(String className)
        throws BuildException {
        try {
            Class c = Class.forName(className);
            Object o = c.newInstance();
            return (RmicAdapter) o;
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(className + " can\'t be found.", cnfe);
        } catch (ClassCastException cce) {
            throw new BuildException(className + " isn\'t the classname of "
                                     + "a rmic adapter.", cce);
        } catch (Throwable t) {
            // for all other possibilities
            throw new BuildException(className + " caused an interesting "
                                     + "exception.", t);
        }
    }
}
