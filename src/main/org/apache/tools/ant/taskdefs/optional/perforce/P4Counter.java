/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author <a href="mailto:kirk@radik.com">Kirk Wylie</a>
 */

public class P4Counter extends P4Base {
    public String counter = null;
    public String property = null;
    public boolean shouldSetValue = false;
    public boolean shouldSetProperty = false;
    public int value = 0;

    /**
     * The name of the counter; required
     */
    public void setName(String counter) {
        this.counter = counter;
    }

    /**
     * The new value for the counter; optional.
     */
    public void setValue(int value) {
        this.value = value;
        shouldSetValue = true;
    }

    /**
     * A property to be set with the value of the counter
     */
    public void setProperty(String property) {
        this.property = property;
        shouldSetProperty = true;
    }

    /**
     * again, properties are mutable in this tsk
     */
    public void execute() throws BuildException {

        if ((counter == null) || counter.length() == 0) {
            throw new BuildException("No counter specified to retrieve");
        }

        if (shouldSetValue && shouldSetProperty) {
            throw new BuildException("Cannot both set the value of the property and retrieve the value of the property.");
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
            final Project myProj = project;

            P4Handler handler = new P4HandlerAdapter() {
                public void process(String line) {
                    log("P4Counter retrieved line \"" + line + "\"", Project.MSG_VERBOSE);
                    try {
                        value = Integer.parseInt(line);
                        myProj.setProperty(property, "" + value);
                    } catch (NumberFormatException nfe) {
                        throw new BuildException("Perforce error. Could not retrieve counter value.");
                    }
                }
            };

            execP4Command(command, handler);
        } else {
            execP4Command(command, new SimpleP4OutputHandler(this));
        }
    }
}
