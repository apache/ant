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
package org.apache.tools.ant.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Keeps track of prefix -> uri mapping during parsing */
    private Map prefixMapping = new HashMap();


    /**
     * constructor
     * @param project the project to which this antxml context belongs to
     */
    public AntXMLContext(Project project) {
        this.project = project;
        implicitTarget.setProject(project);
        implicitTarget.setName("");
        targetVector.addElement(implicitTarget);
    }

    /**
     * sets the build file to which the XML context belongs
     * @param buildFile  ant build file
     */
    public void setBuildFile(File buildFile) {
        this.buildFile = buildFile;
        this.buildFileParent = new File(buildFile.getParent());
    }

    /**
     * find out the build file
     * @return  the build file to which the xml context belongs
     */
    public File getBuildFile() {
        return buildFile;
    }

    /**
     * find out the parent build file of this build file
     * @return the parent build file of this build file
     */
    public File getBuildFileParent() {
        return buildFileParent;
    }

    /**
     * find out the project to which this antxml context belongs
     * @return project
     */
    public Project getProject() {
        return project;
    }

    /**
     * find out the current project name
     * @return current project name
     */
    public String getCurrentProjectName() {
        return currentProjectName;
    }

    /**
     * set the name of the current project
     * @param name name of the current project
     */
    public void setCurrentProjectName(String name) {
        this.currentProjectName = name;
    }

    /**
     * get the current runtime configurable wrapper
     * can return null
     * @return runtime configurable wrapper
     */
    public RuntimeConfigurable currentWrapper() {
        if (wStack.size() < 1) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 1);
    }

    /**
     * get the runtime configurable wrapper of the parent project
     * can return null
     * @return runtime configurable wrapper  of the parent project
     */
    public RuntimeConfigurable parentWrapper() {
        if (wStack.size() < 2) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 2);
    }

    /**
     * add a runtime configurable wrapper to the internal stack
     * @param wrapper runtime configurable wrapper
     */
    public void pushWrapper(RuntimeConfigurable wrapper) {
        wStack.addElement(wrapper);
    }

    /**
     * remove a runtime configurable wrapper from the stack
     */
    public void popWrapper() {
        if (wStack.size() > 0) {
            wStack.removeElementAt(wStack.size() - 1);
        }
    }

    /**
     * access the stack of wrappers
     * @return the stack of wrappers
     */
    public Vector getWrapperStack() {
        return wStack;
    }

    /**
     * add a new target
     * @param target target to add
     */
    public void addTarget(Target target) {
        targetVector.addElement(target);
        currentTarget = target;
    }

    /**
     * get the current target
     * @return current target
     */
    public Target getCurrentTarget() {
        return currentTarget;
    }

    /**
     * get the implicit target
     * @return implicit target
     */
    public Target getImplicitTarget() {
        return implicitTarget;
    }

    /**
     * sets the current target
     * @param target current target
     */
    public void setCurrentTarget(Target target) {
        this.currentTarget = target;
    }

    /**
     * sets the implicit target
     * @param target the implicit target
     */
    public void setImplicitTarget(Target target) {
        this.implicitTarget = target;
    }

    /**
     * access the vector of targets
     * @return vector of targets
     */
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
     * @param element the current element
     * @param attr attributes of the current element
     */
    public void configureId(Object element, Attributes attr) {
        String id = attr.getValue("id");
        if (id != null) {
            project.addReference(id, element);
        }
    }

    /**
     * access the locator
     * @return locator
     */
    public Locator getLocator() {
        return locator;
    }

    /**
     * sets the locator
     * @param locator locator
     */
    public void setLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * tells whether the project tag is being ignored
     * @return whether the project tag is being ignored
     */
    public boolean isIgnoringProjectTag() {
        return ignoreProjectTag;
    }

    /**
     *  sets the flag to ignore the project tag
     * @param flag to ignore the project tag
     */
    public void setIgnoreProjectTag(boolean flag) {
        this.ignoreProjectTag = flag;
    }

    /**
     * Called during parsing, stores the prefix to uri mapping.
     *
     * @param prefix a namespace prefix
     * @param uri    a namespace uri
     */
    public void startPrefixMapping(String prefix, String uri) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null) {
            list = new ArrayList();
            prefixMapping.put(prefix, list);
        }
        list.add(uri);
    }

    /**
     * End of prefix to uri mapping.
     *
     * @param prefix the namespace prefix
     */
    public void endPrefixMapping(String prefix) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null || list.size() == 0) {
            return; // Should not happen
        }
        list.remove(list.size() - 1);
    }

    /**
     * prefix to namespace uri mapping
     *
     * @param prefix the prefix to map
     * @return the uri for this prefix, null if not present
     */
    public String getPrefixMapping(String prefix) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null || list.size() == 0) {
            return null;
        }
        return (String) list.get(list.size() - 1);
    }
}


