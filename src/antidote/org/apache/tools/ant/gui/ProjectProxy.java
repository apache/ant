/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui;
import org.apache.tools.ant.*;
import org.apache.tools.ant.gui.event.*;
import org.apache.tools.ant.gui.acs.*;
import java.io.File;
import java.io.IOException;
import javax.swing.tree.TreeModel;
import javax.swing.text.Document;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class provides the gateway interface to the data model for
 * the application. The translation between the Ant datamodel, 
 * (or other external datamodel) occurs. This class also provides various
 * views into the data model, such as TreeModel, Documenet, etc.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ProjectProxy {

    /** Application context */
    private AppContext _context = null;
    /** The file where the project was last saved. */
    private File _file = null;
    /** The real Ant Project instance. */
    private ACSProjectElement _project = null;
    /** The current thread executing a build. */
    private Thread _buildThread = null;
    /** The selection model for selected targets. */
    private ElementSelectionModel _selections = null;

	/** 
	 * File loading ctor.
	 * 
	 * @param file File containing build file to load.
	 */
    public ProjectProxy(AppContext context, File file) throws IOException {
        _file = file;
        _context = context;
        loadProject();
    }

	/** 
	 * Load the project from the build file.
	 * 
	 */
    private void loadProject() throws IOException {
        _project = ACSFactory.getInstance().load(_file);
        _selections = new ElementSelectionModel();
        _selections.addTreeSelectionListener(new SelectionForwarder());
    }

	/** 
	 * Called to indicate that the project is no longer going to be used
     * by the GUI, and that any cleanup should occur.
	 * 
	 */
    //public void close() {
    //
    //}

	/** 
	 * Build the project with the current target (or the default target
     * if none is selected.  Build occurs on a separate thread, so method
     * returns immediately.
	 * 
	 */
    public void build() throws BuildException {
        Project project = new Project();
        project.init();
        // XXX there is a bunch of stuff in the class
        // org.apache.tools.ant.Main that needs to be
        // abstracted out so that it doesn't have to be
        // replicated here.
        
        // XXX need to provide a way to pass in externally
        // defined properties.  Perhaps define an external
        // Antidote properties file.
        project.setUserProperty("ant.file" , _file.getAbsolutePath());
        ProjectHelper.configureProject(project, _file);


        _buildThread = new Thread(new BuildRunner(project));
        _buildThread.start();
    }

	/** 
	 * Get the file where the project is saved to. If the project
     * is a new one that has never been saved the this will return null.
	 * 
	 * @return Project file, or null if not save yet.
	 */
    public File getFile() {
        return _file;
    }

	/** 
	 * Get the TreeModel perspective on the data.
	 * 
	 * @return TreeModel view on project.
	 */
    public TreeModel getTreeModel() {
        if(_project != null) {
            return new ProjectTreeModel(_project);
        }
        return null;
    }

	/** 
	 * Get the tree selection model for selected targets.
	 * 
	 * @return Selection model.
	 */
    public ElementSelectionModel getTreeSelectionModel() {
        return _selections;
    }

	/** 
	 * Get the Document perspective on the data.
	 * 
	 * @return Document view on project.
	 */
    public Document getDocument() {
        // This is what the call should look like
        //return new ProjectDocument(_project);
        if(_file != null) {
            return new ProjectDocument(_file);
        }
        return null;
    }

    /** Class for executing the build in a separate thread. */
    private class BuildRunner implements Runnable {
        private Project _project = null;
        public BuildRunner(Project project) {
            _project = project;
        }

        /** 
         * Convenience method for causeing the project to fire a build event.
         * Implemented because the corresponding method in the Project class
         * is not publically accessible.
         * 
         * @param event Event to fire.
         */
        private void fireBuildEvent(BuildEvent event, BuildEventType type) {
            Enumeration enum = _project.getBuildListeners().elements();
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
            // Add the build listener for
            // dispatching BuildEvent objects to the
            // EventBus.
            BuildEventForwarder handler = 
                new BuildEventForwarder(_context);
            _project.addBuildListener(handler);
            try {
                fireBuildEvent(new BuildEvent(
                    _project), BuildEventType.BUILD_STARTED);
                
                // Generate list of targets to execute.
                ACSTargetElement[] targets = _selections.getSelectedTargets();
                Vector targetNames = new Vector();
                if(targets.length == 0) {
                    targetNames.add(_project.getDefaultTarget());
                }
                else {
                    for(int i = 0; i < targets.length; i++) {
                        targetNames.add(targets[i].getName());
                    }
                }
                
                // Execute build on selected targets. XXX It would be 
                // nice if the Project API supported passing in target 
                // objects rather than String names.
                _project.executeTargets(targetNames);
            }
            catch(BuildException ex) {
                BuildEvent errorEvent = new BuildEvent(_project);
                errorEvent.setException(ex);
                errorEvent.setMessage(ex.getMessage(), Project.MSG_ERR);
                fireBuildEvent(errorEvent, BuildEventType.MESSAGE_LOGGED);
            }
            finally {
                fireBuildEvent(new BuildEvent(
                    _project), BuildEventType.BUILD_FINISHED);
                _project.removeBuildListener(handler);
                _buildThread = null;
            }
        }
    }

    /** Forwards selection events to the event bus. */
    private class SelectionForwarder implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            _context.getEventBus().postEvent(new ElementSelectionEvent(
                _context, _selections.getSelectedElements()));
        }
    }

}
