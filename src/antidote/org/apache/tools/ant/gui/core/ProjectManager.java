/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.core;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.gui.acs.*;
import org.apache.tools.ant.gui.event.BuildEventType;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * This class is responsible for managing the currently open projects,
 * and the loading of new projects.
 * 
 * XXX need to add property change listeners support.
 *
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectManager {

    /** Set of open projects. */
    private List _projects = new ArrayList(1);
    /** List of build listeners to register when build starts. */
    private List _buildListeners = new LinkedList();
    /** The current thread executing a build. */
    private Thread _buildThread = null;

    public ProjectManager() {
    }

    /** 
     * Get all the open projects.
     * 
     * @return an array of all open projects.
     */
    public ACSProjectElement[] getOpen() {
        ACSProjectElement[] retval = new ACSProjectElement[_projects.size()];
        _projects.toArray(retval);
        return retval;
    }

    /** 
     * Save the given project.
     * 
     * @param project Project to save.
     */
    public void save(ACSProjectElement project) throws IOException {
        saveAs(project, null);
    }


    /** 
     * Save the given project to the given location.
     * 
     * @param project Project to save.
     * @param location Location to save to.
     */
    public void saveAs(ACSProjectElement project, URL location) 
        throws IOException {

        if(location == null) {
            location = project.getLocation();
        }
        if(location == null) {
            // This shouldn't happen.
            throw new IOException("Logic error: Save location mising.");
        }

        Writer out = null;
        try {
            // XXX for some reason the URLConnection for protocol type "file"
            // doesn't support output (at least that is what the exception
            // says. I don't know if I'm doing something wrong or not, but 
            // since we need to handle files differently (which is fine
            // right now since files are all we really support anyway.
            if(location.getProtocol().equals("file")) {
                out = new FileWriter(location.getFile());
            }
            else {
                // XXX This is here for future support of FTP/HTTP and
                // the like. JDBC will have to be dealt with differently.
                URLConnection connection = location.openConnection();
                connection.setDoInput(false);
                connection.setDoOutput(true);
                out = new OutputStreamWriter(connection.getOutputStream());
            }

            // Persist the project.
            project.write(out);
            out.flush();
            project.setLocation(location);
        }
        finally {
            try { out.close(); } catch(Exception ex) {}
        }
    }

    /** 
     * Open the project persisted at the given location
     * 
     * @param location Location of project file.
     * @return Successfully loaded project.
     * @throws IOException thrown if there is a problem opening the project.
     */
    public ACSProjectElement open(File location) throws IOException {
        return open(new URL("file", null, location.getPath()));
    }

    /** 
     * Open the project persisted at the given location
     * 
     * @param location Location of project file.
     * @return Successfully loaded project.
     * @throws IOException thrown if there is a problem opening the project.
     */
    public ACSProjectElement open(URL location) throws IOException {
        ACSProjectElement retval = null;
        retval = ACSFactory.getInstance().load(location);
        retval.setLocation(location);
        _projects.add(retval);
        return retval;
    }

    /** 
     * Create a new, unpopulated project.
     * 
     * @return Unpopulated project.
     */
    public ACSProjectElement createNew() {
        ACSProjectElement retval = ACSFactory.getInstance().createProject();
        _projects.add(retval);
        return retval;
    }

    /** 
     * Remove the given project from the set of active projects. 
     * 
     * @param project Project to close.
     */
    public void close(ACSProjectElement project) {
        _projects.remove(project);
    }

	/** 
	 * Build the project with the given target (or the default target
     * if none is selected.  Build occurs on a separate thread, so method
     * returns immediately.
	 * 
     * @param project Project to build.
     * @param targets Targets to build in project.
	 */
    public void build(ACSProjectElement project, ACSTargetElement[] targets) 
        throws BuildException {
        _buildThread = new Thread(new BuildRunner(project, targets));
        _buildThread.start();
    }

	/** 
	 * Add a build listener.
	 * 
	 * @param l Listener to add.
	 */
    public void addBuildListener(BuildListener l) {
        synchronized(_buildListeners) {
            _buildListeners.add(l);
        }
    }

	/** 
	 * Remove a build listener.
	 * 
	 * @param l Listener to remove.
	 */
    public void removeBuildListener(BuildListener l) {
        synchronized(_buildListeners) {
            _buildListeners.remove(l);
        }
    }

	/** 
	 * Determine if the given BuildListener is registered.
	 * 
	 * @param l Listener to test for.
	 * @return True if listener has been added, false if unknown.
	 */
    public boolean isRegisteredBuildListener(BuildListener l) {
        synchronized(_buildListeners) {
            return _buildListeners.contains(l);
        }
    }

	/** 
	 * Get the set of current build listeners.
	 * 
     * @return Set of current build listeners.
	 */
    public BuildListener[] getBuildListeners() {
        synchronized(_buildListeners) {
            BuildListener[] retval = new BuildListener[_buildListeners.size()];
            _buildListeners.toArray(retval);
            return retval;
        }
    }

    /** Class for executing the build in a separate thread. */
    private class BuildRunner implements Runnable {
        /** The project to execute build on. */
        private ACSProjectElement _project = null;
        /** Targets to build. */
        private ACSTargetElement[] _targets = null;
        /** The Ant core representation of a project. */
        private Project _antProject = null;

        /** 
         * Standard ctor.
         * 
         * @param project Project to execute build on.
         * @param targets Targets to build. 
         */
        public BuildRunner(ACSProjectElement project, 
                           ACSTargetElement[] targets) throws BuildException {
            _project = project;
            _targets = targets;

            URL location = _project.getLocation();
            if(location == null) {
                // XXX this needs to be changed so that if the project is
                // not saved, or the persistence mechanism is remote
                // then a temporary version is saved first.
                throw new BuildException("Project must be saved first");
            }

            // XXX hopefully we will eventually be able to save
            // project files to any type of URL. Right now the Ant core
            // only supports Files.
            if(!location.getProtocol().equals("file")) {
                throw new IllegalArgumentException(
                    "The Ant core only supports building from locally " +
                    "stored build files.");
            }

            File f = new File(location.getFile());

            _antProject = new Project();
            _antProject.init();
            // XXX there is a bunch of stuff in the class
            // org.apache.tools.ant.Main that needs to be
            // refactored out so that it doesn't have to be
            // replicated here.
            
            // XXX need to provide a way to pass in externally
            // defined properties.  Perhaps define an external
            // Antidote properties file. JAVA_HOME may have to be set, 
            // as well as checking the .ant.properties
            _antProject.setUserProperty(
                "ant.file" , f.getAbsolutePath());
            ProjectHelper.configureProject(_antProject, f);
        }

        /** 
         * Convenience method for causeing the project to fire a build event.
         * Implemented because the corresponding method in the Project class
         * is not publically accessible.
         * 
         * @param event Event to fire.
         */
        private void fireBuildEvent(BuildEvent event, BuildEventType type) {
            Enumeration enum = _antProject.getBuildListeners().elements();
            while(enum.hasMoreElements()) {
                BuildListener l = (BuildListener) enum.nextElement();
                type.fireEvent(event, l);
            }
        }

        /** 
         * Run the build.
         * 
         */
        public void run() {
            synchronized(_antProject) {
                // Add the build listeners
                BuildListener[] listeners = getBuildListeners();
                for(int i = 0; i < listeners.length; i++) {
                    _antProject.addBuildListener(listeners[i]);
                }
                
                try {
                    
                    fireBuildEvent(new BuildEvent(
                        _antProject), BuildEventType.BUILD_STARTED);
                    
                    
                    Vector targetNames = new Vector();
                    if(_targets == null || _targets.length == 0) {
                        targetNames.add(_antProject.getDefaultTarget());
                    }
                    else {
                        for(int i = 0; i < _targets.length; i++) {
                            targetNames.add(_targets[i].getName());
                        }
                    }
                    
                    // Execute build on selected targets. XXX It would be 
                    // nice if the Project API supported passing in target 
                    // objects rather than String names.
                    _antProject.executeTargets(targetNames);
                }
                catch(BuildException ex) {
                    BuildEvent errorEvent = new BuildEvent(_antProject);
                    errorEvent.setException(ex);
                    errorEvent.setMessage(ex.getMessage(), Project.MSG_ERR);
                    fireBuildEvent(errorEvent, BuildEventType.MESSAGE_LOGGED);
                }
                finally {
                    fireBuildEvent(new BuildEvent(
                        _antProject), BuildEventType.BUILD_FINISHED);
                    
                    // Remove the build listeners.
                    for(int i = 0; i < listeners.length; i++) {
                        _antProject.removeBuildListener(listeners[i]);
                    }
                }
            }
        }
    }
}
