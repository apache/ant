/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * A task to provide "nice-ness" to the current thread, and/or to
 * query the current value.
 * Examples:
 * <pre> &lt;Nice currentPriority="current.value" &gt;</pre><p>
 * Set <code>currentPriority</code> to the current priority
 * <pre> &lt;Nice newPriority="10" &gt;</pre><p>
 * Raise the priority of the build process (But not forked programs)
 * <pre> &lt;Nice currentPriority="old" newPriority="3" &gt;</pre><p>
 * Lower the priority of the build process (But not forked programs), and save
 * the old value to the property <code>old</code>.
 *
 * @ant.task name="Nice" category="optional"
 */
public class Nice extends Task {

    /**
     * the new priority
     */
    private Integer newPriority;

    /**
     * the current priority
     */
    private String currentPriority;



    /**
     * Execute the task
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {

        Thread self = Thread.currentThread();
        int priority = self.getPriority();
        if(currentPriority!=null) {
            String current=Integer.toString(priority);
            getProject().setNewProperty(currentPriority,current);
        }
        //if there is a new priority, and it is different, change it
        if(newPriority!=null && priority!=newPriority.intValue()) {
            try {
                self.setPriority(newPriority.intValue());
            } catch (SecurityException e) {
                //catch permissions denial and keep going
                log("Unable to set new priority -a security manager is in the way",
                        Project.MSG_WARN);
            } catch(IllegalArgumentException iae) {
                throw new BuildException("Priority out of range",iae);
            }
        }
    }

    /**
     * The name of a property to set to the value of the current
     * thread priority. Optional
     * @param currentPriority
     */
    public void setCurrentPriority(String currentPriority) {
        this.currentPriority = currentPriority;
    }

    /**
     * the new priority, in the range 1-10.
     * @param newPriority
     */
    public void setNewPriority(int newPriority) {
        if(newPriority<Thread.MIN_PRIORITY || newPriority>Thread.MAX_PRIORITY) {
            throw new BuildException("The thread priority is out of the range 1-10");
        }
        this.newPriority = new Integer(newPriority);
    }

}