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
package org.apache.tools.ant.helper;

import java.io.File;
import java.util.Vector;

import org.xml.sax.Locator;
import org.xml.sax.Attributes;


import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.RuntimeConfigurable;


/**
 * Context information for the ant processing.
 *
 * @author Costin Manolache
 */
public class AntXMLContext {
    /** The project to configure. */
    private Project project;

    /** The configuration file to parse. */
    private File buildFile;

    /** Vector with all the targets, in the order they are
     * defined. Project maintains a Hashtable, which is not ordered.
     * This will allow description to know the original order.
     */
    private Vector targetVector = new Vector();

    /**
     * Parent directory of the build file. Used for resolving entities
     * and setting the project's base directory.
     */
    private File buildFileParent;

    /** Name of the current project */
    private String currentProjectName;

    /**
     * Locator for the configuration file parser.
     * Used for giving locations of errors etc.
     */
    private Locator locator;

     /**
      * Target that all other targets will depend upon implicitly.
      *
      * <p>This holds all tasks and data type definitions that have
      * been placed outside of targets.</p>
      */
    private Target implicitTarget = new Target();

    /** Current target ( no need for a stack as the processing model
        allows only one level of target ) */
    private Target currentTarget = null;

    /** The stack of RuntimeConfigurable2 wrapping the
        objects.
    */
    private Vector wStack = new Vector();

    /**
     * Indicates whether the project tag attributes are to be ignored
     * when processing a particular build file.
     */
    private boolean ignoreProjectTag = false;

    public AntXMLContext(Project project) {
        this.project = project;
        implicitTarget.setName("");
        targetVector.addElement(implicitTarget);
    }

    public void setBuildFile(File buildFile) {
        this.buildFile = buildFile;
        this.buildFileParent = new File(buildFile.getParent());
    }

    public File getBuildFile() {
        return buildFile;
    }

    public File getBuildFileParent() {
        return buildFileParent;
    }

    public Project getProject() {
        return project;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String name) {
        this.currentProjectName = name;
    }

    public RuntimeConfigurable currentWrapper() {
        if (wStack.size() < 1) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 1);
    }

    public RuntimeConfigurable parentWrapper() {
        if (wStack.size() < 2) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 2);
    }

    public void pushWrapper(RuntimeConfigurable wrapper) {
        wStack.addElement(wrapper);
    }

    public void popWrapper() {
        if (wStack.size() > 0) {
            wStack.removeElementAt(wStack.size() - 1);
        }
    }

    public Vector getWrapperStack() {
        return wStack;
    }

    public void addTarget(Target target) {
        targetVector.addElement(target);
        currentTarget = target;
    }

    public Target getCurrentTarget() {
        return currentTarget;
    }

    public Target getImplicitTarget() {
        return implicitTarget;
    }

    public void setCurrentTarget(Target target) {
        this.currentTarget = target;
    }

    public void setImplicitTarget(Target target) {
        this.implicitTarget = target;
    }

    public Vector getTargets() {
        return targetVector;
    }

    /**
     * Scans an attribute list for the <code>id</code> attribute and
     * stores a reference to the target object in the project if an
     * id is found.
     * <p>
     * This method was moved out of the configure method to allow
     * it to be executed at parse time.
     *
     * @see #configure(java.lang.Object,org.xml.sax.AttributeList,org.apache.tools.ant.Project)
     */
    public void configureId(Object element, Attributes attr) {
        String id = attr.getValue("id");
        if (id != null) {
            project.addReference(id, element);
        }
    }

    public Locator getLocator() {
        return locator;
    }

    public void setLocator(Locator locator) {
        this.locator = locator;
    }

    public boolean isIgnoringProjectTag() {
        return ignoreProjectTag;
    }

    public void setIgnoreProjectTag(boolean flag) {
        this.ignoreProjectTag = flag;
    }
}


