/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import java.util.Enumeration;
import java.util.Hashtable;

/*
 * This task is similar to the <antcall> task; however, <do> will not
 * re-evaluate all the dependencies of the specified target. Using <antcall>
 * to run several targets that all have the same dependency(s), for each
 * called target, the dependent target(s) will be run again. This results
 * in common targets, such as "init"-type targets, being run many times.
 * Using <do> instead of <antcall> prevents any common dependencies from
 * being run more than once. 
 *
 * Note: The <do> task will still re-run any dependent target that was run
 * prior to the <do> task(s) being run. 
 *
 * Usage:
 *
 * <target name="all">
 *   <do target="target1"/>
 *   <do target="target2"/>
 * </target>
 *
 *
 * @author Mark McMillan
 * @author Diane Holt <a href="mailto:holtdl@apache.org">holtdl@apache.org</a>
 *   (Clean-up for submission.)
 *
 * @since 1.5
 *
 * @ant.task category="control"
 *
 */

public class Do extends Task {

    private String targetName;

    private static Hashtable hasRunList = new Hashtable();

    public Do() {
    }

    public void setTarget(String targetName) {
        this.targetName = targetName;
    }

    public void execute() throws BuildException {
        // Get the target property
        if (targetName == null) {
            throw new BuildException("Missing 'target' attribute.", location);
        }

        // This will cause all dependencies to be re-evaluated:
        //     project.executeTarget(targetName);
        // so we don't want it.
        runTarget(targetName);
    }

    private void runTarget(String targetName) throws BuildException {
        Target t = (Target)project.getTargets().get(targetName);
        if (t == null) {
            throw new BuildException("Target '" + targetName + "' not found.",
                location);
        }

        // Run all dependencies that have not yet been run before running
        // the requested target.
        Enumeration enum = t.getDependencies();
        while (enum.hasMoreElements()) {
            String depName = (String)enum.nextElement();
            // This target has never been run
            if (hasRunList.get(depName) == null) {
                // Recursive call to run the dependent target
                runTarget(depName);
            }
        }

        // Now run the requested target
        log("Calling target '" + targetName + "'", Project.MSG_VERBOSE);
        t.performTasks();

        // Add to list of targets that have been run
        hasRunList.put(targetName, t);
    }

}
