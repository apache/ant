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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
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

    /**
     * If true, pass all properties to the new Ant project.
     * Defaults to true.
     */
    public void setInheritAll(boolean value) {
        inheritAll = value;
    }

    /**
     * If true, pass all references to the new Ant project.
     * Defaults to false.
     */
    public void setInheritRefs(boolean value) {
        inheritRefs = value;
    }

    /**
     * Creates a Project instance for the project to call.
     */
    public void init() {
        newProject = new Project();
        newProject.setJavaVersionProperty();
        newProject.addTaskDefinition("property",
                                     (Class) getProject().getTaskDefinitions()
                                             .get("property"));
    }

    /**
     * Called in execute or createProperty if newProject is null.
     *
     * <p>This can happen if the same instance of this task is run
     * twice as newProject is set to null at the end of execute (to
     * save memory and help the GC).</p>
     *
     * <p>Sets all properties that have been defined as nested
     * property elements.</p>
     */
    private void reinit() {
        init();
        final int count = properties.size();
        for (int i = 0; i < count; i++) {
            Property p = (Property) properties.elementAt(i);
            Property newP = (Property) newProject.createTask("property");
            newP.setName(p.getName());
            if (p.getValue() != null) {
                newP.setValue(p.getValue());
            }
            if (p.getFile() != null) {
                newP.setFile(p.getFile());
            }
            if (p.getResource() != null) {
                newP.setResource(p.getResource());
            }
            if (p.getPrefix() != null) {
                newP.setPrefix(p.getPrefix());
            }
            if (p.getRefid() != null) {
                newP.setRefid(p.getRefid());
            }
            if (p.getEnvironment() != null) {
                newP.setEnvironment(p.getEnvironment());
            }
            if (p.getClasspath() != null) {
                newP.setClasspath(p.getClasspath());
            }
            properties.setElementAt(newP, i);
        }
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

        Hashtable taskdefs = getProject().getTaskDefinitions();
        Enumeration et = taskdefs.keys();
        while (et.hasMoreElements()) {
            String taskName = (String) et.nextElement();
            if (taskName.equals("property")) {
                // we have already added this taskdef in #init
                continue;
            }
            Class taskClass = (Class) taskdefs.get(taskName);
            newProject.addTaskDefinition(taskName, taskClass);
        }

        Hashtable typedefs = getProject().getDataTypeDefinitions();
        Enumeration e = typedefs.keys();
        while (e.hasMoreElements()) {
            String typeName = (String) e.nextElement();
            Class typeClass = (Class) typedefs.get(typeName);
            newProject.addDataTypeDefinition(typeName, typeClass);
        }

        // set user-defined properties
        getProject().copyUserProperties(newProject);

        if (!inheritAll) {
           // set Java built-in properties separately,
           // b/c we won't inherit them.
           newProject.setSystemProperties();

        } else {
            // set all properties from calling project

            Hashtable props = getProject().getProperties();
            e = props.keys();
            while (e.hasMoreElements()) {
                String arg = e.nextElement().toString();
                if ("basedir".equals(arg) || "ant.file".equals(arg)) {
                    // basedir and ant.file get special treatment in execute()
                    continue;
                }

                String value = props.get(arg).toString();
                // don't re-set user properties, avoid the warning message
                if (newProject.getProperty(arg) == null){
                    // no user property
                    newProject.setNewProperty(arg, value);
                }
            }
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @since Ant 1.5
     */
    public void handleOutput(String line) {
        if (newProject != null) {
            newProject.demuxOutput(line, false);
        } else {
            super.handleOutput(line);
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @since Ant 1.5.2
     */
    public void handleFlush(String line) {
        if (newProject != null) {
            newProject.demuxFlush(line, false);
        } else {
            super.handleFlush(line);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @since Ant 1.5
     */
    public void handleErrorOutput(String line) {
        if (newProject != null) {
            newProject.demuxOutput(line, true);
        } else {
            super.handleErrorOutput(line);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String line) {
        if (newProject != null) {
            newProject.demuxFlush(line, true);
        } else {
            super.handleErrorFlush(line);
        }
    }

    /**
     * Do the execution.
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
            ProjectHelper.configureProject(newProject, new File(antFile));

            if (target == null) {
                target = newProject.getDefaultTarget();
            }

            // Are we trying to call the target in which we are defined?
            if (newProject.getBaseDir().equals(project.getBaseDir()) &&
                newProject.getProperty("ant.file").equals(project.getProperty("ant.file")) &&
                getOwningTarget() != null &&
                target.equals(this.getOwningTarget().getName())) {

                throw new BuildException("ant task calling its own parent " 
                    + "target");
            }

            addReferences();

            newProject.executeTarget(target);
        } finally {
            // help the gc
            newProject = null;
            Enumeration enum = properties.elements();
            while (enum.hasMoreElements()) {
                Property p = (Property) enum.nextElement();
                p.setProject(null);
            }

            if (output != null && out != null) {
                try {
                    out.close();
                } catch (final Exception e) {
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
     */
    private void overrideProperties() throws BuildException {
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
     */
    private void addReferences() throws BuildException {
        Hashtable thisReferences = (Hashtable) getProject().getReferences().clone();
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
     * The directory to use as a base directory for the new Ant project.
     * Defaults to the current project's basedir, unless inheritall
     * has been set to false, in which case it doesn't have a default
     * value. This will override the basedir setting of the called project.
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * The build file to use.
     * Defaults to "build.xml". This file is expected to be a filename relative
     * to the dir attribute given.
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
     */
    public void setTarget(String s) {
        this.target = s;
    }

    /**
     * Filename to write the output to.
     * This is relative to the value of the dir attribute
     * if it has been set or to the base directory of the
     * current project otherwise.
     */
    public void setOutput(String s) {
        this.output = s;
    }

    /**
     * Property to pass to the new project.
     * The property is passed as a 'user property'
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
     */
    public void addReference(Reference r) {
        references.addElement(r);
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
