/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Build a sub-project.
 *
 *  <pre>
 *  &lt;target name=&quot;foo&quot; depends=&quot;init&quot;&gt;
 *    &lt;ant antfile=&quot;build.xml&quot; target=&quot;bar&quot; &gt;
 *      &lt;property name=&quot;property1&quot; value=&quot;aaaaa&quot; /&gt;
 *      &lt;property name=&quot;foo&quot; value=&quot;baz&quot; /&gt;
 *    &lt;/ant&gt;</span>
 *  &lt;/target&gt;</span>
 *
 *  &lt;target name=&quot;bar&quot; depends=&quot;init&quot;&gt;
 *    &lt;echo message=&quot;prop is ${property1} ${foo}&quot; /&gt;
 *  &lt;/target&gt;
 * </pre>
 *
 *
 * @since Ant 1.1
 *
 * @ant.task category="control"
 */
public class Ant extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** the basedir where is executed the build file */
    private File dir = null;

    /**
     * the build.xml file (can be absolute) in this case dir will be
     * ignored
     */
    private String antFile = null;

    /** the output */
    private String output = null;

    /** should we inherit properties from the parent ? */
    private boolean inheritAll = true;

    /** should we inherit references from the parent ? */
    private boolean inheritRefs = false;

    /** the properties to pass to the new project */
    private Vector properties = new Vector();

    /** the references to pass to the new project */
    private Vector references = new Vector();

    /** the temporary project created to run the build file */
    private Project newProject;

    /** The stream to which output is to be written. */
    private PrintStream out = null;

    /** the sets of properties to pass to the new project */
    private Vector propertySets = new Vector();

    /** the targets to call on the new project */
    private Vector targets = new Vector();

    /** whether the target attribute was specified **/
    private boolean targetAttributeSet = false;

    /**
     * simple constructor
     */
    public Ant() {
        //default
    }

    /**
     * create a task bound to its creator
     * @param owner owning task
     */
    public Ant(Task owner) {
        bindToOwner(owner);
    }


    /**
     * If true, pass all properties to the new Ant project.
     * Defaults to true.
     * @param value if true pass all properties to the new Ant project.
     */
    public void setInheritAll(boolean value) {
        inheritAll = value;
    }

    /**
     * If true, pass all references to the new Ant project.
     * Defaults to false.
     * @param value if true, pass all references to the new Ant project
     */
    public void setInheritRefs(boolean value) {
        inheritRefs = value;
    }

    /**
     * Creates a Project instance for the project to call.
     */
    public void init() {
        newProject = getProject().createSubProject();
        newProject.setJavaVersionProperty();
    }

    /**
     * Called in execute or createProperty (via getNewProject())
     * if newProject is null.
     *
     * <p>This can happen if the same instance of this task is run
     * twice as newProject is set to null at the end of execute (to
     * save memory and help the GC).</p>
     * <p>calls init() again</p>
     *
     */
    private void reinit() {
        init();
    }

    /**
     * Attaches the build listeners of the current project to the new
     * project, configures a possible logfile, transfers task and
     * data-type definitions, transfers properties (either all or just
     * the ones specified as user properties to the current project,
     * depending on inheritall), transfers the input handler.
     */
    private void initializeProject() {
        newProject.setInputHandler(getProject().getInputHandler());

        Iterator iter = getBuildListeners();
        while (iter.hasNext()) {
            newProject.addBuildListener((BuildListener) iter.next());
        }

        if (output != null) {
            File outfile = null;
            if (dir != null) {
                outfile = FILE_UTILS.resolveFile(dir, output);
            } else {
                outfile = getProject().resolveFile(output);
            }
            try {
                out = new PrintStream(new FileOutputStream(outfile));
                DefaultLogger logger = new DefaultLogger();
                logger.setMessageOutputLevel(Project.MSG_INFO);
                logger.setOutputPrintStream(out);
                logger.setErrorPrintStream(out);
                newProject.addBuildListener(logger);
            } catch (IOException ex) {
                log("Ant: Can't set output to " + output);
            }
        }
        // set user-defined properties
        getProject().copyUserProperties(newProject);

        if (!inheritAll) {
           // set Ant's built-in properties separately,
           // because they are not being inherited.
           newProject.initProperties();

        } else {
            // set all properties from calling project
            addAlmostAll(getProject().getProperties());
        }

        Enumeration e = propertySets.elements();
        while (e.hasMoreElements()) {
            PropertySet ps = (PropertySet) e.nextElement();
            addAlmostAll(ps.getProperties());
        }
    }

    /**
     * Handles output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param outputToHandle The string output to output.
     * @see Task#handleOutput(String)
     * @since Ant 1.5
     */
    public void handleOutput(String outputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(outputToHandle, false);
        } else {
            super.handleOutput(outputToHandle);
        }
    }

    /**
     * Handles input.
     * Deleate to the created project, if present, otherwise
     * call the super class.
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @see Task#handleInput(byte[], int, int)
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (newProject != null) {
            return newProject.demuxInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    /**
     * Handles output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param toFlush The string to output.
     * @see Task#handleFlush(String)
     * @since Ant 1.5.2
     */
    public void handleFlush(String toFlush) {
        if (newProject != null) {
            newProject.demuxFlush(toFlush, false);
        } else {
            super.handleFlush(toFlush);
        }
    }

    /**
     * Handle error output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param errorOutputToHandle The string to output.
     *
     * @see Task#handleErrorOutput(String)
     * @since Ant 1.5
     */
    public void handleErrorOutput(String errorOutputToHandle) {
        if (newProject != null) {
            newProject.demuxOutput(errorOutputToHandle, true);
        } else {
            super.handleErrorOutput(errorOutputToHandle);
        }
    }

    /**
     * Handle error output.
     * Send it the the new project if is present, otherwise
     * call the super class.
     * @param errorOutputToFlush The string to output.
     * @see Task#handleErrorFlush(String)
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String errorOutputToFlush) {
        if (newProject != null) {
            newProject.demuxFlush(errorOutputToFlush, true);
        } else {
            super.handleErrorFlush(errorOutputToFlush);
        }
    }

    /**
     * Do the execution.
     * @throws BuildException if a target tries to call itself;
     * probably also if a BuildException is thrown by the new project.
     */
    public void execute() throws BuildException {
        File savedDir = dir;
        String savedAntFile = antFile;
        Vector locals = new Vector(targets);
        try {
            getNewProject();

            if (dir == null && inheritAll) {
                dir = getProject().getBaseDir();
            }

            initializeProject();

            if (dir != null) {
                newProject.setBaseDir(dir);
                if (savedDir != null) {
                    // has been set explicitly
                    newProject.setInheritedProperty(MagicNames.PROJECT_BASEDIR,
                                                    dir.getAbsolutePath());
                }
            } else {
                dir = getProject().getBaseDir();
            }

            overrideProperties();

            if (antFile == null) {
                antFile = Main.DEFAULT_BUILD_FILENAME;
            }

            File file = FILE_UTILS.resolveFile(dir, antFile);
            antFile = file.getAbsolutePath();

            log("calling target(s) "
                + ((locals.size() > 0) ? locals.toString() : "[default]")
                + " in build file " + antFile, Project.MSG_VERBOSE);
            newProject.setUserProperty(MagicNames.ANT_FILE , antFile);

            String thisAntFile = getProject().getProperty(MagicNames.ANT_FILE);
            // Are we trying to call the target in which we are defined (or
            // the build file if this is a top level task)?
            if (thisAntFile != null
                && file.equals(getProject().resolveFile(thisAntFile))
                && getOwningTarget() != null) {

                if (getOwningTarget().getName().equals("")) {
                    if (getTaskName().equals("antcall")) {
                        throw new BuildException("antcall must not be used at"
                                                 + " the top level.");
                    }
                    throw new BuildException(getTaskName() + " task at the"
                                + " top level must not invoke"
                                + " its own build file.");
                }
            }

            try {
                ProjectHelper.configureProject(newProject, file);
            } catch (BuildException ex) {
                throw ProjectHelper.addLocationToBuildException(
                    ex, getLocation());
            }

            if (locals.size() == 0) {
                String defaultTarget = newProject.getDefaultTarget();
                if (defaultTarget != null) {
                    locals.add(defaultTarget);
                }
            }

            if (newProject.getProperty(MagicNames.ANT_FILE)
                .equals(getProject().getProperty(MagicNames.ANT_FILE))
                && getOwningTarget() != null) {

                String owningTargetName = getOwningTarget().getName();

                if (locals.contains(owningTargetName)) {
                    throw new BuildException(getTaskName() + " task calling "
                                             + "its own parent target.");
                }
                boolean circular = false;
                for (Iterator it = locals.iterator();
                     !circular && it.hasNext();) {
                    Target other =
                        (Target) (getProject().getTargets().get(it.next()));
                    circular |= (other != null
                                 && other.dependsOn(owningTargetName));
                }
                if (circular) {
                    throw new BuildException(getTaskName()
                                             + " task calling a target"
                                             + " that depends on"
                                             + " its parent target \'"
                                             + owningTargetName
                                             + "\'.");
                }
            }

            addReferences();

            if (locals.size() > 0 && !(locals.size() == 1
                                       && "".equals(locals.get(0)))) {
                BuildException be = null;
                try {
                    log("Entering " + antFile + "...", Project.MSG_VERBOSE);
                    newProject.fireSubBuildStarted();
                    newProject.executeTargets(locals);
                } catch (BuildException ex) {
                    be = ProjectHelper
                        .addLocationToBuildException(ex, getLocation());
                    throw be;
                } finally {
                    log("Exiting " + antFile + ".", Project.MSG_VERBOSE);
                    newProject.fireSubBuildFinished(be);
                }
            }
        } finally {
            // help the gc
            newProject = null;
            Enumeration e = properties.elements();
            while (e.hasMoreElements()) {
                Property p = (Property) e.nextElement();
                p.setProject(null);
            }

            if (output != null && out != null) {
                try {
                    out.close();
                } catch (final Exception ex) {
                    //ignore
                }
            }
            dir = savedDir;
            antFile = savedAntFile;
        }
    }

    /**
     * Override the properties in the new project with the one
     * explicitly defined as nested elements here.
     * @throws BuildException under unknown circumstances.
     */
    private void overrideProperties() throws BuildException {
        // remove duplicate properties - last property wins
        // Needed for backward compatibility
        Set set = new HashSet();
        for (int i = properties.size() - 1; i >= 0; --i) {
            Property p = (Property) properties.get(i);
            if (p.getName() != null && !p.getName().equals("")) {
                if (set.contains(p.getName())) {
                    properties.remove(i);
                } else {
                    set.add(p.getName());
                }
            }
        }
        Enumeration e = properties.elements();
        while (e.hasMoreElements()) {
            Property p = (Property) e.nextElement();
            p.setProject(newProject);
            p.execute();
        }
        getProject().copyInheritedProperties(newProject);
    }

    /**
     * Add the references explicitly defined as nested elements to the
     * new project.  Also copy over all references that don't override
     * existing references in the new project if inheritrefs has been
     * requested.
     * @throws BuildException if a reference does not have a refid.
     */
    private void addReferences() throws BuildException {
        Hashtable thisReferences
            = (Hashtable) getProject().getReferences().clone();
        Hashtable newReferences = newProject.getReferences();
        Enumeration e;
        if (references.size() > 0) {
            for (e = references.elements(); e.hasMoreElements();) {
                Reference ref = (Reference) e.nextElement();
                String refid = ref.getRefId();
                if (refid == null) {
                    throw new BuildException("the refid attribute is required"
                                             + " for reference elements");
                }
                if (!thisReferences.containsKey(refid)) {
                    log("Parent project doesn't contain any reference '"
                        + refid + "'",
                        Project.MSG_WARN);
                    continue;
                }

                thisReferences.remove(refid);
                String toRefid = ref.getToRefid();
                if (toRefid == null) {
                    toRefid = refid;
                }
                copyReference(refid, toRefid);
            }
        }

        // Now add all references that are not defined in the
        // subproject, if inheritRefs is true
        if (inheritRefs) {
            for (e = thisReferences.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                if (newReferences.containsKey(key)) {
                    continue;
                }
                copyReference(key, key);
                newProject.inheritIDReferences(getProject());
            }
        }
    }

    /**
     * Try to clone and reconfigure the object referenced by oldkey in
     * the parent project and add it to the new project with the key newkey.
     *
     * <p>If we cannot clone it, copy the referenced object itself and
     * keep our fingers crossed.</p>
     * @param oldKey the reference id in the current project.
     * @param newKey the reference id in the new project.
     */
    private void copyReference(String oldKey, String newKey) {
        Object orig = getProject().getReference(oldKey);
        if (orig == null) {
            log("No object referenced by " + oldKey + ". Can't copy to "
                + newKey,
                Project.MSG_WARN);
            return;
        }

        Class c = orig.getClass();
        Object copy = orig;
        try {
            Method cloneM = c.getMethod("clone", new Class[0]);
            if (cloneM != null) {
                copy = cloneM.invoke(orig, new Object[0]);
                log("Adding clone of reference " + oldKey, Project.MSG_DEBUG);
            }
        } catch (Exception e) {
            // not Clonable
        }


        if (copy instanceof ProjectComponent) {
            ((ProjectComponent) copy).setProject(newProject);
        } else {
            try {
                Method setProjectM =
                    c.getMethod("setProject", new Class[] {Project.class});
                if (setProjectM != null) {
                    setProjectM.invoke(copy, new Object[] {newProject});
                }
            } catch (NoSuchMethodException e) {
                // ignore this if the class being referenced does not have
                // a set project method.
            } catch (Exception e2) {
                String msg = "Error setting new project instance for "
                    + "reference with id " + oldKey;
                throw new BuildException(msg, e2, getLocation());
            }
        }
        newProject.addReference(newKey, copy);
    }

    /**
     * Copies all properties from the given table to the new project -
     * omitting those that have already been set in the new project as
     * well as properties named basedir or ant.file.
     * @param props properties <code>Hashtable</code> to copy to the
     * new project.
     * @since Ant 1.6
     */
    private void addAlmostAll(Hashtable props) {
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            if (MagicNames.PROJECT_BASEDIR.equals(key) || MagicNames.ANT_FILE.equals(key)) {
                // basedir and ant.file get special treatment in execute()
                continue;
            }

            String value = props.get(key).toString();
            // don't re-set user properties, avoid the warning message
            if (newProject.getProperty(key) == null) {
                // no user property
                newProject.setNewProperty(key, value);
            }
        }
    }

    /**
     * The directory to use as a base directory for the new Ant project.
     * Defaults to the current project's basedir, unless inheritall
     * has been set to false, in which case it doesn't have a default
     * value. This will override the basedir setting of the called project.
     * @param dir new directory as <code>File</code>.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * The build file to use. Defaults to "build.xml". This file is expected
     * to be a filename relative to the dir attribute given.
     * @param antFile the <code>String</code> build file name.
     */
    public void setAntfile(String antFile) {
        // @note: it is a string and not a file to handle relative/absolute
        // otherwise a relative file will be resolved based on the current
        // basedir.
        this.antFile = antFile;
    }

    /**
     * The target of the new Ant project to execute.
     * Defaults to the new project's default target.
     * @param targetToAdd the name of the target to invoke.
     */
    public void setTarget(String targetToAdd) {
        if (targetToAdd.equals("")) {
            throw new BuildException("target attribute must not be empty");
        }
        targets.add(targetToAdd);
        targetAttributeSet = true;
    }

    /**
     * Set the filename to write the output to. This is relative to the value
     * of the dir attribute if it has been set or to the base directory of the
     * current project otherwise.
     * @param outputFile the name of the file to which the output should go.
     */
    public void setOutput(String outputFile) {
        this.output = outputFile;
    }

    /**
     * Property to pass to the new project.
     * The property is passed as a 'user property'.
     * @return the created <code>Property</code> object.
     */
    public Property createProperty() {
        Property p = new Property(true, getProject());
        p.setProject(getNewProject());
        p.setTaskName("property");
        properties.addElement(p);
        return p;
    }

    /**
     * Add a Reference element identifying a data type to carry
     * over to the new project.
     * @param ref <code>Reference</code> to add.
     */
    public void addReference(Reference ref) {
        references.addElement(ref);
    }

    /**
     * Add a target to this Ant invocation.
     * @param t the <code>TargetElement</code> to add.
     * @since Ant 1.6.3
     */
    public void addConfiguredTarget(TargetElement t) {
        if (targetAttributeSet) {
            throw new BuildException(
                "nested target is incompatible with the target attribute");
        }
        String name = t.getName();
        if (name.equals("")) {
            throw new BuildException("target name must not be empty");
        }
        targets.add(name);
    }

    /**
     * Add a set of properties to pass to the new project.
     *
     * @param ps <code>PropertySet</code> to add.
     * @since Ant 1.6
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.addElement(ps);
    }

    /**
     * Get the (sub)-Project instance currently in use.
     * @return Project
     * @since Ant 1.7
     */
    protected Project getNewProject() {
        if (newProject == null) {
            reinit();
        }
        return newProject;
    }

    /**
     * @since Ant 1.6.2
     */
    private Iterator getBuildListeners() {
        return getProject().getBuildListeners().iterator();
    }

    /**
     * Helper class that implements the nested &lt;reference&gt;
     * element of &lt;ant&gt; and &lt;antcall&gt;.
     */
    public static class Reference
        extends org.apache.tools.ant.types.Reference {

        /** Creates a reference to be configured by Ant. */
        public Reference() {
                super();
        }

        private String targetid = null;

        /**
         * Set the id that this reference to be stored under in the
         * new project.
         *
         * @param targetid the id under which this reference will be passed to
         *        the new project. */
        public void setToRefid(String targetid) {
            this.targetid = targetid;
        }

        /**
         * Get the id under which this reference will be stored in the new
         * project.
         *
         * @return the id of the reference in the new project.
         */
        public String getToRefid() {
            return targetid;
        }
    }

    /**
     * Helper class that implements the nested &lt;target&gt;
     * element of &lt;ant&gt; and &lt;antcall&gt;.
     * @since Ant 1.6.3
     */
    public static class TargetElement {
        private String name;

        /**
         * Default constructor.
         */
        public TargetElement() {
                //default
        }

        /**
         * Set the name of this TargetElement.
         * @param name   the <code>String</code> target name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the name of this TargetElement.
         * @return <code>String</code>.
         */
        public String getName() {
            return name;
        }
    }
}
