package org.apache.tools.ant.taskdefs.optional.ide;

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
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.*;
import java.io.File;

/**
 * This class wraps the Ant project information needed to
 * start Ant from Visual Age.
 * It serves the following purposes:
 * - acts as model for AntMakeFrame
 * - converts itself to/from String (to store the information
 *   as ToolData in the VA repository)
 * - wraps Project functions for the GUI (get target list,
 *   execute target)
 *    
 * @author Wolf Siberski, TUI Infotec GmbH
 */

public class VAJBuildInfo {
	// name of the VA project this BuildInfo belongs to
	private String vajProjectName = "";
	
		// name of the Ant build file
	private String buildFileName = "";

		// main targets found in the build file
	private Vector projectTargets = new Vector();

		// target selected for execution
	private java.lang.String target = "";

		// log level
	private int outputMessageLevel = Project.MSG_INFO;

		// Ant Project created from build file
	private transient Project project;

		// is true if Project initialization was successful
	private transient boolean projectInitialized = false;

		// Support for bound properties
	protected transient java.beans.PropertyChangeSupport propertyChange;
/**
 * The addPropertyChangeListener method was generated to support the propertyChange field.
 */
public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
	getPropertyChange().addPropertyChangeListener(listener);
}
/**
 * Returns the BuildInfo information as String. The BuildInfo can
 * be rebuilt from that String by calling parse().
 * @return java.lang.String
 */
public String asDataString() {
	String result = getOutputMessageLevel() + "|" + getBuildFileName() + "|" + getTarget();
	for ( Enumeration e = getProjectTargets().elements(); e.hasMoreElements(); ) {
		result = result + "|" + e.nextElement();
	}

	return result;
}
/**
 * Executes the target set by setTarget().
 * @param listener  BuildListener for the output of the build
 */
public void executeProject( BuildListener listener ) {
	Throwable error = null;
	try {
		if (!isProjectInitialized()) {
			project = new Project();
		}
		project.addBuildListener( listener );
		// Chris: HACK: replace when Ant-Refactoring is finished!
//		project.fireBuildStarted();
		if (!isProjectInitialized()) {
			initProject();
		}
		project.executeTarget(target);
		
	} catch (RuntimeException exc) {
		error = exc;
		throw exc;
	} catch (Error err) {
		error = err;
		throw err;
	} finally {
		// Chris: HACK: replace when Ant-Refactoring is finished!
//		project.fireBuildFinished(error);
		project.removeBuildListener( listener );
	}
}
	/**
	 * Search for the insert position to keep names a sorted list of Strings
	 * This method has been copied from org.apache.tools.ant.Main
	 */
	private static int findTargetPosition(Vector names, String name) {
		int res = names.size();
		for (int i=0; i<names.size() && res == names.size(); i++) {
			if (name.compareTo((String)names.elementAt(i)) < 0) {
				res = i;
			}
		}
		return res;
	}
/**
 * The firePropertyChange method was generated to support the propertyChange field.
 */
public void firePropertyChange(java.lang.String propertyName, java.lang.Object oldValue, java.lang.Object newValue) {
	getPropertyChange().firePropertyChange(propertyName, oldValue, newValue);
}
/**
 * Returns the build file name.
 * @return build file name.
 */
public String getBuildFileName() {
	return buildFileName;
}
/**
 * Returns the log level
 * @return log level.
 */
public int getOutputMessageLevel() {
	return outputMessageLevel;
}
/**
 * Returns the Ant project
 * @return org.apache.tools.ant.Project
 */
private Project getProject() {
	return project;
}
/**
 * return a list of all targets in the current buildfile
 */
public Vector getProjectTargets() {
	return projectTargets;
}
/**
 * Accessor for the propertyChange field.
 */
protected java.beans.PropertyChangeSupport getPropertyChange() {
	if (propertyChange == null) {
		propertyChange = new java.beans.PropertyChangeSupport(this);
	};
	return propertyChange;
}
/**
 * Insert the method's description here.
 * Creation date: (07.11.2000 10:34:18)
 * @return java.lang.String
 */
public java.lang.String getTarget() {
	return target;
}
/**
 * returns the VA project name
 * @return The projectName property value.
 */
public String getVAJProjectName() {
	return vajProjectName;
}
/**
 * Initializes the Ant project. Assumes that the
 * project attribute is already set.
 */
private void initProject() {
	try {
		project.init();
		File buildFile = new File(getBuildFileName());
		project.setUserProperty("ant.file", buildFile.getAbsolutePath());
		ProjectHelper.configureProject(project, buildFile);
		setProjectInitialized(true);
	} catch (RuntimeException exc) {
		setProjectInitialized(false);
		throw exc;
	} catch (Error err) {
		setProjectInitialized(false);
		throw err;
	}
}
/**
 * Returns true, if the Ant project is initialized
 * (i.e. buildfile loaded)
 */
public boolean isProjectInitialized() {
	return projectInitialized;
}
/**
 * Creates a BuildInfo object from a String
 * The String must be in the format
 * outputMessageLevel'|'buildFileName'|'defaultTarget'|'(project target'|')*
 *
 * @return org.apache.tools.ant.taskdefs.optional.vaj.BuildInfo
 * @param data java.lang.String
 */
public static VAJBuildInfo parse(String data) {
	VAJBuildInfo result = new VAJBuildInfo();

	try {
		java.util.StringTokenizer tok = new java.util.StringTokenizer( data, "|" );
		result.setOutputMessageLevel( tok.nextToken() );
		result.setBuildFileName( tok.nextToken() );
		result.setTarget( tok.nextToken() );
		while( tok.hasMoreTokens() ) {
			result.projectTargets.addElement( tok.nextToken() );
		}
	} catch ( Throwable t ) {
	}
	return result;
}
/**
 * The removePropertyChangeListener method was generated to support the propertyChange field.
 */
public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
	getPropertyChange().removePropertyChangeListener(listener);
}
/**
 * Sets the build file name
 * @param buildFileName build file name
 */
public void setBuildFileName(String newBuildFileName) {
	String oldValue = buildFileName;
	buildFileName = newBuildFileName;
	setProjectInitialized(false);
	firePropertyChange("buildFileName", oldValue, buildFileName);
}
/**
 * Sets the log level (value must be one of the constants in Project)
 * @param outputMessageLevel log level.
 */
public void setOutputMessageLevel(int newOutputMessageLevel) {
	int oldValue = outputMessageLevel;
	outputMessageLevel = newOutputMessageLevel;
	firePropertyChange("outputMessageLevel", new Integer(oldValue), new Integer(outputMessageLevel));
}
/**
 * Sets the log level (value must be one of the constants in Project)
 * @param outputMessageLevel log level as String.
 */
private void setOutputMessageLevel(String outputMessageLevel) {
	int level = Integer.parseInt( outputMessageLevel );
	setOutputMessageLevel( level );
}
/**
  */
private void setProjectInitialized(boolean initialized) {
	Boolean oldValue = new Boolean(projectInitialized);
	projectInitialized = initialized;
	firePropertyChange("projectInitialized", oldValue, new Boolean(projectInitialized));
}
/**
 * Sets the target to execute when executeBuild is called
 * @param newTarget build target
 */
public void setTarget(String newTarget) {
	String oldValue = target;
	target = newTarget;
	firePropertyChange("target", oldValue, target);
}
/**
 * Sets the name of the Visual Age for Java project where
 * this BuildInfo belongs to
 * @param newProjectName VAJ project
 */
public void setVAJProjectName(String newVAJProjectName) {
	String oldValue = vajProjectName;
	vajProjectName = newVAJProjectName;
	firePropertyChange("VAJProjectName", oldValue, vajProjectName);
}
/**
 * reloads the build file and updates the target list
 */
public void updateTargetList() {
	project = new Project();
	initProject();
	projectTargets.removeAllElements();
	Enumeration ptargets = project.getTargets().elements();
	while (ptargets.hasMoreElements()) {
		Target currentTarget = (Target) ptargets.nextElement();
		if ( currentTarget.getDescription() != null ) {
			String targetName = currentTarget.getName();
			int pos = findTargetPosition( projectTargets, targetName );
			projectTargets.insertElementAt(targetName, pos);
		}
	}
}
}
