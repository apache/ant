/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
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
 *    &lt;/ant&gt;</SPAN>
 *  &lt;/target&gt;</SPAN>
 *
 *  &lt;target name=&quot;bar&quot; depends=&quot;init&quot;&gt;
 *    &lt;echo message=&quot;prop is ${property1} ${foo}&quot; /&gt;
 *  &lt;/target&gt;
 * </pre>
 *
 *
 * @author Costin Manolache
 *
 * @since Ant 1.1
 *
 * @ant.task category="control"
 */
public class Ant extends Task {

    /** the basedir where is executed the build file */
    private File dir = null;

    /**
     * the build.xml file (can be absolute) in this case dir will be
     * ignored
     */
    private String antFile = null;

    /** the target to call if any */
    private String target = null;

    /** the output */
    private String output  = null;

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
        newProject = new Project();
        newProject.setDefaultInputStream(getProject().getDefaultInputStream());
        newProject.setJavaVersionProperty();
    }

    /**
     * Called in execute or createProperty if newProject is null.
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

        Vector listeners = getProject().getBuildListeners();
        final int count = listeners.size();
        for (int i = 0; i < count; i++) {
            newProject.addBuildListener((BuildListener) listeners.elementAt(i));
        }

        if (output != null) {
            File outfile = null;
            if (dir != null) {
                outfile = FileUtils.newFileUtils().resolveFile(dir, output);
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

        getProject().initSubProject(newProject);

        // set user-defined properties
        getProject().copyUserProperties(newProject);

        if (!inheritAll) {
           // set Java built-in properties separately,
           // b/c we won't inherit them.
           newProject.setSystemProperties();

        } else {
            // set all properties from calling project
            addAlmostAll(getProject().getProperties());
        }

        Enumeration e;
        e = propertySets.elements();
        while (e.hasMoreElements()) {
            PropertySet ps = (PropertySet) e.nextElement();
            addAlmostAll(ps.getProperties());
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @param output a line of output
     * @since Ant 1.5
     */
    public void handleOutput(String output) {
        if (newProject != null) {
            newProject.demuxOutput(output, false);
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Process input into the ant task
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException if the data cannot be read
     *
     * @see Task#handleInput(byte[], int, int)
     *
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (newProject != null) {
            return newProject.demuxInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @param output The output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.5.2
     */
    public void handleFlush(String output) {
        if (newProject != null) {
            newProject.demuxFlush(output, false);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.5
     */
    public void handleErrorOutput(String output) {
        if (newProject != null) {
            newProject.demuxOutput(output, true);
        } else {
            super.handleErrorOutput(output);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String output) {
        if (newProject != null) {
            newProject.demuxFlush(output, true);
        } else {
            super.handleErrorFlush(output);
        }
    }

    /**
     * Do the execution.
     * @throws BuildException if a target tries to call itself
     * probably also if a BuildException is thrown by the new project
     */
    public void execute() throws BuildException {
        File savedDir = dir;
        String savedAntFile = antFile;
        String savedTarget = target;
        try {
            if (newProject == null) {
                reinit();
            }

            if ((dir == null) && (inheritAll)) {
                dir = getProject().getBaseDir();
            }

            initializeProject();

            if (dir != null) {
                newProject.setBaseDir(dir);
                if (savedDir != null) { // has been set explicitly
                    newProject.setInheritedProperty("basedir" ,
                                                    dir.getAbsolutePath());
                }
            } else {
                dir = getProject().getBaseDir();
            }

            overrideProperties();

            if (antFile == null) {
                antFile = "build.xml";
            }

            File file = FileUtils.newFileUtils().resolveFile(dir, antFile);
            antFile = file.getAbsolutePath();

            log("calling target " + (target != null ? target : "[default]")
                    + " in build file " +  antFile.toString(),
                    Project.MSG_VERBOSE);
            newProject.setUserProperty("ant.file" , antFile);

            // Are we trying to call the target in which we are defined (or
            // the build file if this is a top level task)?
            if (newProject.getProperty("ant.file")
                .equals(getProject().getProperty("ant.file"))
                && getOwningTarget() != null) {

                if (getOwningTarget().getName().equals("")) {
                    if (getTaskName().equals("antcall")) {
                        throw new BuildException("antcall must not be used at"
                                                 + " the top level.");
                    } else {
                        throw new BuildException(getTaskName() + " task at the"
                                                 + " top level must not invoke"
                                                 + " its own build file.");
                    }
                }
            }

            ProjectHelper.configureProject(newProject, new File(antFile));

            if (target == null) {
                target = newProject.getDefaultTarget();
            }

            if (newProject.getProperty("ant.file")
                .equals(getProject().getProperty("ant.file"))
                && getOwningTarget() != null) {

                String owningTargetName = getOwningTarget().getName();

                if (owningTargetName.equals(target)) {
                    throw new BuildException(getTaskName() + " task calling "
                                             + "its own parent target.");
                } else {
                    Target other =
                        (Target) getProject().getTargets().get(target);
                    if (other != null && other.dependsOn(owningTargetName)) {
                        throw new BuildException(getTaskName()
                                                 + " task calling a target"
                                                 + " that depends on"
                                                 + " its parent target \'"
                                                 + owningTargetName
                                                 + "\'.");
                    }
                }
            }

            addReferences();

            if (target != null) {
                if (!"".equals(target)) {
                    newProject.executeTarget(target);
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
            target = savedTarget;
        }
    }

    /**
     * Override the properties in the new project with the one
     * explicitly defined as nested elements here.
     * @throws BuildException under unknown circumstances
     */
    private void overrideProperties() throws BuildException {
        // remove duplicate properties - last property wins
        // Needed for backward compatibility
        Set set = new HashSet();
        for (int i = properties.size() - 1; i >= 0; --i) {
            Property p = (Property) properties.get(i);
            if (set.contains(p.getName())) {
                properties.remove(i);
            } else {
                set.add(p.getName());
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
     * @throws BuildException if a reference does not have a refid
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
            }
        }
    }

    /**
     * Try to clone and reconfigure the object referenced by oldkey in
     * the parent project and add it to the new project with the key
     * newkey.
     *
     * <p>If we cannot clone it, copy the referenced object itself and
     * keep our fingers crossed.</p>
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
     * ommiting those that have already been set in the new project as
     * well as properties named basedir or ant.file.
     * @param props properties to copy to the new project
     * @since Ant 1.6
     */
    private void addAlmostAll(Hashtable props) {
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            if ("basedir".equals(key) || "ant.file".equals(key)) {
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
     * @param d new directory
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * The build file to use.
     * Defaults to "build.xml". This file is expected to be a filename relative
     * to the dir attribute given.
     * @param s build file to use
     */
    public void setAntfile(String s) {
        // @note: it is a string and not a file to handle relative/absolute
        // otherwise a relative file will be resolved based on the current
        // basedir.
        this.antFile = s;
    }

    /**
     * The target of the new Ant project to execute.
     * Defaults to the new project's default target.
     * @param s target to invoke
     */
    public void setTarget(String s) {
        if (s.equals("")) {
            throw new BuildException("target attribute must not be empty");
        }

        this.target = s;
    }

    /**
     * Filename to write the output to.
     * This is relative to the value of the dir attribute
     * if it has been set or to the base directory of the
     * current project otherwise.
     * @param s file to which the output should go to
     */
    public void setOutput(String s) {
        this.output = s;
    }

    /**
     * Property to pass to the new project.
     * The property is passed as a 'user property'
     * @return new property created
     */
    public Property createProperty() {
        if (newProject == null) {
            reinit();
        }
        Property p = new Property(true, getProject());
        p.setProject(newProject);
        p.setTaskName("property");
        properties.addElement(p);
        return p;
    }

    /**
     * Reference element identifying a data type to carry
     * over to the new project.
     * @param r reference to add
     */
    public void addReference(Reference r) {
        references.addElement(r);
    }

    /**
     * Set of properties to pass to the new project.
     *
     * @param ps property set to add
     * @since Ant 1.6
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.addElement(ps);
    }

    /**
     * Helper class that implements the nested &lt;reference&gt;
     * element of &lt;ant&gt; and &lt;antcall&gt;.
     */
    public static class Reference
        extends org.apache.tools.ant.types.Reference {

        /** Creates a reference to be configured by Ant */
        public Reference() {
            super();
        }

        private String targetid = null;

        /**
         * Set the id that this reference to be stored under in the
         * new project.
         *
         * @param targetid the id under which this reference will be passed to
         *        the new project */
        public void setToRefid(String targetid) {
            this.targetid = targetid;
        }

        /**
         * Get the id under which this reference will be stored in the new
         * project
         *
         * @return the id of the reference in the new project.
         */
        public String getToRefid() {
            return targetid;
        }
    }
}
