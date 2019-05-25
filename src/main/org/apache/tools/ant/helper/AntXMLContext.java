/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.helper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.util.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Context information for the ant processing.
 *
 */
public class AntXMLContext {
    /** The project to configure. */
    private Project project;

    /** The configuration file to parse. */
    private File buildFile;

    /** The configuration file to parse. */
    private URL buildFileURL;

    /** Vector with all the targets, in the order they are
     * defined. Project maintains a Hashtable, which is not ordered.
     * This will allow description to know the original order.
     */
    private Vector<Target> targetVector = new Vector<>();

    /**
     * Parent directory of the build file. Used for resolving entities
     * and setting the project's base directory.
     */
    private File buildFileParent;

    /**
     * Parent directory of the build file. Used for resolving entities
     * and setting the project's base directory.
     */
    private URL buildFileParentURL;

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

    /** Current target (no need for a stack as the processing model
        allows only one level of target) */
    private Target currentTarget = null;

    /** The stack of RuntimeConfigurable2 wrapping the
        objects.
    */
    private Vector<RuntimeConfigurable> wStack = new Vector<>();

    /**
     * Indicates whether the project tag attributes are to be ignored
     * when processing a particular build file.
     */
    private boolean ignoreProjectTag = false;

    /** Keeps track of prefix -> uri mapping during parsing */
    private Map<String, List<String>> prefixMapping = new HashMap<>();


    /** Keeps track of targets in files */
    private Map<String, Target> currentTargets = null;

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
        if (buildFile != null) {
            this.buildFileParent = new File(buildFile.getParent());
            implicitTarget.setLocation(new Location(buildFile.getAbsolutePath()));
            try {
                setBuildFile(FileUtils.getFileUtils().getFileURL(buildFile));
            } catch (MalformedURLException ex) {
                throw new BuildException(ex);
            }
        } else {
            this.buildFileParent = null;
        }
    }

    /**
     * sets the build file to which the XML context belongs
     * @param buildFile Ant build file
     * @throws MalformedURLException if parent URL cannot be constructed
     * @since Ant 1.8.0
     */
    public void setBuildFile(URL buildFile) throws MalformedURLException {
        this.buildFileURL = buildFile;
        this.buildFileParentURL = new URL(buildFile, ".");
        if (implicitTarget.getLocation() == null) {
            implicitTarget.setLocation(new Location(buildFile.toString()));
        }
    }

    /**
     * find out the build file
     * @return the build file to which the XML context belongs
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
     * find out the build file
     * @return the build file to which the xml context belongs
     * @since Ant 1.8.0
     */
    public URL getBuildFileURL() {
        return buildFileURL;
    }

    /**
     * find out the parent build file of this build file
     * @return the parent build file of this build file
     * @since Ant 1.8.0
     */
    public URL getBuildFileParentURL() {
        return buildFileParentURL;
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
        return wStack.elementAt(wStack.size() - 1);
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
        return wStack.elementAt(wStack.size() - 2);
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
    public Vector<RuntimeConfigurable> getWrapperStack() {
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
    public Vector<Target> getTargets() {
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
            project.addIdReference(id, element);
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
        List<String> list = prefixMapping.computeIfAbsent(prefix, k -> new ArrayList<>());
        list.add(uri);
    }

    /**
     * End of prefix to uri mapping.
     *
     * @param prefix the namespace prefix
     */
    public void endPrefixMapping(String prefix) {
        List<String> list = prefixMapping.get(prefix);
        if (list == null || list.isEmpty()) {
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
        List<String> list = prefixMapping.get(prefix);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * Get the targets in the current source file.
     * @return the current targets.
     */
    public Map<String, Target> getCurrentTargets() {
        return currentTargets;
    }

    /**
     * Set the map of the targets in the current source file.
     * @param currentTargets a map of targets.
     */
    public void setCurrentTargets(Map<String, Target> currentTargets) {
        this.currentTargets = currentTargets;
    }

}


