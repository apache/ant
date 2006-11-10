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
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Obtains or sets the value of a counter.
 *
 * <p> When used in its base form
 * (where only the counter name is provided), the counter value will be
 * printed to the output stream. When the value is provided, the counter
 * will be set to the value provided. When a property name is provided,
 * the property will be filled with the value of the counter. You may
 * not specify to both get and set the value of the counter in the same
 * Task.
 * </p>
 * <P>
 * The user performing this task must have Perforce &quot;review&quot; permissions
 * as defined by Perforce protections in order for this task to succeed.
</P>

 * Example Usage:<br>
 * &lt;p4counter name="${p4.counter}" property=${p4.change}"/&gt;
 *
 * @ant.task category="scm"
 */

public class P4Counter extends P4Base {
    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * name of the counter
     */
    public String counter = null;
    /**
     * name of an optional property
     */
    public String property = null;
    /**
     * flag telling whether the value of the counter should be set
     */
    public boolean shouldSetValue = false;
    /**
     * flag telling whether a property should be set
     */
    public boolean shouldSetProperty = false;
    /**
     * new value for the counter
     */
    public int value = 0;

    // CheckStyle:VisibilityModifier ON

    /**
     * The name of the counter; required
     * @param counter name of the counter
     */
    public void setName(String counter) {
        this.counter = counter;
    }

    /**
     * The new value for the counter; optional.
     * @param value new value for the counter
     */
    public void setValue(int value) {
        this.value = value;
        shouldSetValue = true;
    }

    /**
     * A property to be set with the value of the counter
     * @param property the name of a property to set with the value
     * of the counter
     */
    public void setProperty(String property) {
        this.property = property;
        shouldSetProperty = true;
    }

    /**
     * again, properties are mutable in this tsk
     * @throws BuildException if the required parameters are not supplied.
     */
    public void execute() throws BuildException {

        if ((counter == null) || counter.length() == 0) {
            throw new BuildException("No counter specified to retrieve");
        }

        if (shouldSetValue && shouldSetProperty) {
            throw new BuildException("Cannot both set the value of the property and retrieve the "
                + "value of the property.");
        }

        String command = "counter " + P4CmdOpts + " " + counter;
        if (!shouldSetProperty) {
            // NOTE kirk@radik.com 04-April-2001 -- If you put in the -s, you
            // have to start running through regular expressions here. Much easier
            // to just not include the scripting information than to try to add it
            // and strip it later.
            command = "-s " + command;
        }
        if (shouldSetValue) {
            command += " " + value;
        }

        if (shouldSetProperty) {
            final Project myProj = getProject();

            P4Handler handler = new P4HandlerAdapter() {
                public void process(String line) {
                    log("P4Counter retrieved line \"" + line + "\"", Project.MSG_VERBOSE);
                    try {
                        value = Integer.parseInt(line);
                        myProj.setProperty(property, "" + value);
                    } catch (NumberFormatException nfe) {
                        throw new BuildException("Perforce error. "
                        + "Could not retrieve counter value.");
                    }
                }
            };

            execP4Command(command, handler);
        } else {
            execP4Command(command, new SimpleP4OutputHandler(this));
        }
    }
}
