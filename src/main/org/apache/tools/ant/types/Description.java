/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.helper.ProjectHelperImpl;

import java.util.Vector;


/**
 * Description is used to provide a project-wide description element
 * (that is, a description that applies to a buildfile as a whole).
 * If present, the &lt;description&gt; element is printed out before the
 * target descriptions.
 *
 * Description has no attributes, only text.  There can only be one
 * project description per project.  A second description element will
 * overwrite the first.
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Revision$ $Date$
 *
 * @ant.datatype ignore="true"
 */
public class Description extends DataType {

    /**
     * Adds descriptive text to the project.
     *
     * @param text the descriptive text
     */
    public void addText(String text) {

        ProjectHelper ph = ProjectHelper.getProjectHelper();
        if (!(ph instanceof ProjectHelperImpl)) {
            // New behavior for delayed task creation. Description
            // will be evaluated in Project.getDescription()
            return;
        }
        String currentDescription = getProject().getDescription();
        if (currentDescription == null) {
            getProject().setDescription(text);
        } else {
            getProject().setDescription(currentDescription + text);
        }
    }

    public static String getDescription(Project project) {
        StringBuffer description = new StringBuffer();
        Vector targets = (Vector) project.getReference("ant.targets");
        for (int i = 0; i < targets.size(); i++) {
            Target t = (Target) targets.elementAt(i);
            concatDescriptions(project, t, description);
        }
        return description.toString();
    }

    private static void concatDescriptions(Project project, Target t,
                                           StringBuffer description) {
        if (t == null) {
            return;
        }
        Vector tasks = findElementInTarget(project, t, "description");
        if (tasks == null) {
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            Task task = (Task) tasks.elementAt(i);
            if (!(task instanceof UnknownElement)) {
                continue;
            }
            UnknownElement ue = ((UnknownElement) task);
            StringBuffer descComp = ue.getWrapper().getText();
            if (descComp != null) {
                description.append(descComp);
            }
        }
    }

    private static Vector findElementInTarget(Project project,
                                              Target t, String name) {
        Task[] tasks = t.getTasks();
        Vector elems = new Vector();
        for (int i = 0; i < tasks.length; i++) {
            if (name.equals(tasks[i].getTaskName())) {
                elems.addElement(tasks[i]);
            }
        }
        return elems;
    }

}
