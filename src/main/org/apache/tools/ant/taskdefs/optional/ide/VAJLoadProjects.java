/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
package org.apache.tools.ant.taskdefs.optional.ide;


import com.ibm.ivj.util.base.IvjException;
import com.ibm.ivj.util.base.ProjectEdition;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Load specific project versions into the Visual Age for Java workspace.
 * Each project and version name has to be specified completely.
 * Example:  
 * <blockquote> 
 * &lt;vajload>
 * &nbsp;&lt;project name="MyVAProject" version="2.1"/>
 * &nbsp;&lt;project name="Apache Xerces" version="1.2.0"/>
 * &lt;/vajload>
 * </blockquote>
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */

public class VAJLoadProjects extends Task {
    Vector projectDescriptions = new Vector();
    Vector expandedProjectDescriptions = new Vector();

    /**
     * Class to maintain VisualAge for Java Workspace Project descriptions.
     */
    public class VAJProjectDescription {
        private String name;
        private String version;
        private boolean projectFound;

        public VAJProjectDescription() {
        }

        public VAJProjectDescription(String n, String v) {
            name = n;
            version = v;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public boolean projectFound() {
            return projectFound;
        }

        public void setName(String newName) {
            if (newName == null || newName.equals("")) {
                throw new BuildException("name attribute must be set");
            }
            name = newName;
        }

        public void setVersion(String newVersion) {
            if (newVersion == null || newVersion.equals("")) {
                throw new BuildException("version attribute must be set");
            }
            version = newVersion;
        }

        public void setProjectFound() {
            projectFound = true;
        }
    }
    
    /**
     * Add a project description entry on the project list.
     */
    public VAJProjectDescription createVAJProject() {
        VAJProjectDescription d = new VAJProjectDescription();
        projectDescriptions.addElement(d);
        return d;
    }
    /**
     * Load specified projects.
     */
    public void execute() {
        expandDescriptions();
        log(
            "Loading " + expandedProjectDescriptions.size() + " project(s) into workspace"); 
        for (Enumeration e = expandedProjectDescriptions.elements(); 
             e.hasMoreElements(); 
             ) {
            VAJProjectDescription d = (VAJProjectDescription) e.nextElement();

            ProjectEdition pe = findProjectEdition(d.getName(), d.getVersion());
            try {
                log( "Loading " + d.getName() + ", Version " + d.getVersion() + 
                     ", into Workspace", 
                     Project.MSG_VERBOSE ); 
                pe.loadIntoWorkspace();
            } catch (IvjException ex) {
                throw VAJUtil.createBuildException( "Project " + d.getName() + 
                                                    " could not be loaded.", 
                                                    ex ); 
            }
        }
    }

    /**
     */
    public void expandDescriptions() {
        String[] projectNames;
        try {
            projectNames = VAJUtil.getWorkspace().getRepository().getProjectNames();
        } catch (IvjException e) {
            throw VAJUtil.createBuildException("VA Exception occured: ", e);
        }

        for (int i = 0; i < projectNames.length; i++) {
            for (Enumeration e = projectDescriptions.elements(); e.hasMoreElements();) {
                VAJProjectDescription d = (VAJProjectDescription) e.nextElement();
                String pattern = d.getName();
                if (VAJWorkspaceScanner.match(pattern, projectNames[i])) {
                    d.setProjectFound();
                    expandedProjectDescriptions.
                        addElement(new VAJProjectDescription(projectNames[i], d.getVersion())); 
                    break;
                }
            }
        }

        for (Enumeration e = projectDescriptions.elements(); e.hasMoreElements();) {
            VAJProjectDescription d = (VAJProjectDescription) e.nextElement();
            if (!d.projectFound()) {
                log("No Projects match the name " + d.getName(), Project.MSG_WARN);
            }
        }
    }

    /**
     */
    public static Vector findMatchingProjects(String pattern) {
        String[] projectNames;
        try {
            projectNames = VAJUtil.getWorkspace().getRepository().getProjectNames();
        } catch (IvjException e) {
            throw VAJUtil.createBuildException("VA Exception occured: ", e);
        }

        Vector matchingProjects = new Vector();
        for (int i = 0; i < projectNames.length; i++) {
            if (VAJWorkspaceScanner.match(pattern, projectNames[i])) {
                matchingProjects.addElement(projectNames[i]);
            }
        }

        return matchingProjects;
    }

    /**
     * Finds a specific project edition in the repository.
     *
     * @param name project name
     * @param versionName project version name
     * @return com.ibm.ivj.util.base.ProjectEdition
     */
    public static ProjectEdition findProjectEdition(
                                                    String name, 
                                                    String versionName) {
        try {
            ProjectEdition[] editions = null;
            editions = VAJUtil.getWorkspace().getRepository().getProjectEditions(name);

            if (editions == null) {
                throw new BuildException("Project " + name + " doesn't exist");
            }

            ProjectEdition pe = null;

            for (int i = 0; i < editions.length && pe == null; i++) {
                if (versionName.equals(editions[i].getVersionName())) {
                    pe = editions[i];
                }
            }
            if (pe == null) {
                throw new BuildException( "Version " + versionName + " of Project " + 
                                          name + " doesn't exist" ); 
            }
            return pe;

        } catch (IvjException e) {
            throw VAJUtil.createBuildException("VA Exception occured: ", e);
        }

    }
}
