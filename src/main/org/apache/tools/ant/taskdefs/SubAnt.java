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
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant.TargetElement;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.StringUtils;

/**
 * Calls a given target for all defined sub-builds. This is an extension
 * of ant for bulk project execution.
 *
 * <h2>Use with directories</h2>
 * <p>
 * subant can be used with directory sets to execute a build from different directories.
 * 2 different options are offered
 * </p>
 * <ul>
 * <li>
 * run the same build file /somepath/otherpath/mybuild.xml
 * with different base directories use the genericantfile attribute
 * </li>
 * <li>if you want to run directory1/build.xml, directory2/build.xml, ....
 * use the antfile attribute. The base directory does not get set by the subant task in this case,
 * because you can specify it in each build file.
 * </li>
 * </ul>
 * @since Ant1.6
 * @ant.task name="subant" category="control"
 */
public class SubAnt extends Task {

    private Path buildpath;

    private Ant ant = null;
    private String subTarget = null;
    private String antfile = getDefaultBuildFile();
    private File genericantfile = null;
    private boolean verbose = false;
    private boolean inheritAll = false;
    private boolean inheritRefs = false;
    private boolean failOnError = true;
    private String output  = null;

    private List<Property> properties = new Vector<>();
    private List<Ant.Reference> references = new Vector<>();
    private List<PropertySet> propertySets = new Vector<>();

    /** the targets to call on the new project */
    private List<TargetElement> targets = new Vector<>();

    /**
     * Get the default build file name to use when launching the task.
     * <p>
     * This function may be overridden by providers of custom ProjectHelper so
     * they can implement easily their sub launcher.
     * </p>
     *
     * @return the name of the default file
     * @since Ant 1.8.0
     */
    protected String getDefaultBuildFile() {
        return Main.DEFAULT_BUILD_FILENAME;
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @param output a line of output
     * @since Ant 1.6.2
     */
    @Override
    public void handleOutput(String output) {
        if (ant != null) {
            ant.handleOutput(output);
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
     * @since Ant 1.6.2
     */
    @Override
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (ant != null) {
            return ant.handleInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
    }

    /**
     * Pass output sent to System.out to the new project.
     *
     * @param output The output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.6.2
     */
    @Override
    public void handleFlush(String output) {
        if (ant != null) {
            ant.handleFlush(output);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.6.2
     */
    @Override
    public void handleErrorOutput(String output) {
        if (ant != null) {
            ant.handleErrorOutput(output);
        } else {
            super.handleErrorOutput(output);
        }
    }

    /**
     * Pass output sent to System.err to the new project.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.6.2
     */
    @Override
    public void handleErrorFlush(String output) {
        if (ant != null) {
            ant.handleErrorFlush(output);
        } else {
            super.handleErrorFlush(output);
        }
    }

    /**
     * Runs the various sub-builds.
     */
    @Override
    public void execute() {
        if (buildpath == null) {
            throw new BuildException("No buildpath specified");
        }

        final String[] filenames = buildpath.list();
        final int count = filenames.length;
        if (count < 1) {
            log("No sub-builds to iterate on", Project.MSG_WARN);
            return;
        }
/*
    //REVISIT: there must be cleaner way of doing this, if it is merited at all
        if (subTarget == null) {
            subTarget = getOwningTarget().getName();
        }
*/
        BuildException buildException = null;
        for (String filename : filenames) {
            File file = null;
            String subdirPath = null;
            Throwable thrownException = null;
            try {
                File directory = null;
                file = new File(filename);
                if (file.isDirectory()) {
                    if (verbose) {
                        subdirPath = file.getPath();
                        log("Entering directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    if (genericantfile != null) {
                        directory = file;
                        file = genericantfile;
                    } else {
                        file = new File(file, antfile);
                    }
                }
                execute(file, directory);
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                }
            } catch (RuntimeException ex) {
                if (!getProject().isKeepGoingMode()) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    throw ex; // throw further
                }
                thrownException = ex;
            } catch (Throwable ex) {
                if (!getProject().isKeepGoingMode()) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    throw new BuildException(ex);
                }
                thrownException = ex;
            }
            if (thrownException != null) {
                if (thrownException instanceof BuildException) {
                    log("File '" + file
                        + "' failed with message '"
                        + thrownException.getMessage() + "'.", Project.MSG_ERR);
                    // only the first build exception is reported
                    if (buildException == null) {
                        buildException = (BuildException) thrownException;
                    }
                } else {
                    log("Target '" + file
                        + "' failed with message '"
                        + thrownException.getMessage() + "'.", Project.MSG_ERR);
                    log(StringUtils.getStackTrace(thrownException), Project.MSG_ERR);
                    if (buildException == null) {
                        buildException =
                            new BuildException(thrownException);
                    }
                }
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                }
            }
        }
        // check if one of the builds failed in keep going mode
        if (buildException != null) {
            throw buildException;
        }
    }

    /**
     * Runs the given target on the provided build file.
     *
     * @param  file the build file to execute
     * @param  directory the directory of the current iteration
     * @throws BuildException is the file cannot be found, read, is
     *         a directory, or the target called failed, but only if
     *         <code>failOnError</code> is <code>true</code>. Otherwise,
     *         a warning log message is simply output.
     */
    private void execute(File file, File directory)
                throws BuildException {
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            String msg = "Invalid file: " + file;
            if (failOnError) {
                throw new BuildException(msg);
            }
            log(msg, Project.MSG_WARN);
            return;
        }

        ant = createAntTask(directory);
        String antfilename = file.getAbsolutePath();
        ant.setAntfile(antfilename);
        targets.forEach(ant::addConfiguredTarget);

        try {
            if (verbose) {
                log("Executing: " + antfilename, Project.MSG_INFO);
            }
            ant.execute();
        } catch (BuildException e) {
            if (failOnError || isHardError(e)) {
                throw e;
            }
            log("Failure for target '" + subTarget
               + "' of: " +  antfilename + "\n"
               + e.getMessage(), Project.MSG_WARN);
        } catch (Throwable e) {
            if (failOnError || isHardError(e)) {
                throw new BuildException(e);
            }
            log("Failure for target '" + subTarget
                + "' of: " + antfilename + "\n"
                + e.toString(),
                Project.MSG_WARN);
        } finally {
            ant = null;
        }
    }

    /** whether we should even try to continue after this error */
    private boolean isHardError(Throwable t) {
        if (t instanceof BuildException) {
            return isHardError(t.getCause());
        }
        return t instanceof OutOfMemoryError || t instanceof ThreadDeath;
    }

    /**
     * This method builds the file name to use in conjunction with directories.
     *
     * <p>Defaults to "build.xml".
     * If <code>genericantfile</code> is set, this attribute is ignored.</p>
     *
     * @param  antfile the short build file name. Defaults to "build.xml".
     */
    public void setAntfile(String antfile) {
        this.antfile = antfile;
    }

    /**
     * This method builds a file path to use in conjunction with directories.
     *
     * <p>Use <code>genericantfile</code>, in order to run the same build file
     * with different basedirs.</p>
     * If this attribute is set, <code>antfile</code> is ignored.
     *
     * @param afile (path of the generic ant file, absolute or relative to
     *               project base directory)
     * */
    public void setGenericAntfile(File afile) {
        this.genericantfile = afile;
    }

    /**
     * Sets whether to fail with a build exception on error, or go on.
     *
     * @param  failOnError the new value for this boolean flag.
     */
    public void setFailonerror(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * The target to call on the different sub-builds. Set to "" to execute
     * the default target.
     *
     * @param target the target
     */
    // REVISIT: Defaults to the target name that contains this task if not specified.
    public void setTarget(String target) {
        this.subTarget = target;
    }

    /**
     * Add a target to this Ant invocation.
     * @param t the <code>TargetElement</code> to add.
     * @since Ant 1.7
     */
    public void addConfiguredTarget(TargetElement t) {
        if (t.getName().isEmpty()) {
            throw new BuildException("target name must not be empty");
        }
        targets.add(t);
    }

    /**
     * Enable/ disable verbose log messages showing when each sub-build path is entered/ exited.
     * The default value is "false".
     * @param on true to enable verbose mode, false otherwise (default).
     */
    public void setVerbose(boolean on) {
        this.verbose = on;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * <code>output</code> attribute.
     *
     * @param  s the filename to write the output to.
     */
    public void setOutput(String s) {
        this.output = s;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * <code>inheritall</code> attribute.
     *
     * @param  b the new value for this boolean flag.
     */
    public void setInheritall(boolean b) {
        this.inheritAll = b;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * <code>inheritrefs</code> attribute.
     *
     * @param  b the new value for this boolean flag.
     */
    public void setInheritrefs(boolean b) {
        this.inheritRefs = b;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * nested <code>&lt;property&gt;</code> element.
     *
     * @param  p the property to pass on explicitly to the sub-build.
     */
    public void addProperty(Property p) {
        properties.add(p);
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * nested <code>&lt;reference&gt;</code> element.
     *
     * @param  r the reference to pass on explicitly to the sub-build.
     */
    public void addReference(Ant.Reference r) {
        references.add(r);
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * nested <code>&lt;propertyset&gt;</code> element.
     * @param ps the propertyset
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.add(ps);
    }

    /**
     * Adds a directory set to the implicit build path.
     * <p>
     * <em>Note that the directories will be added to the build path
     * in no particular order, so if order is significant, one should
     * use a file list instead!</em>
     * </p>
     *
     * @param  set the directory set to add.
     */
    public void addDirset(DirSet set) {
        add(set);
    }

    /**
     * Adds a file set to the implicit build path.
     * <p>
     * <em>Note that the directories will be added to the build path
     * in no particular order, so if order is significant, one should
     * use a file list instead!</em>
     * </p>
     *
     * @param  set the file set to add.
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Adds an ordered file list to the implicit build path.
     * <p>
     * <em>Note that contrary to file and directory sets, file lists
     * can reference non-existent files or directories!</em>
     * </p>
     *
     * @param  list the file list to add.
     */
    public void addFilelist(FileList list) {
        add(list);
    }

    /**
     * Adds a resource collection to the implicit build path.
     *
     * @param  rc the resource collection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        getBuildpath().add(rc);
    }

    /**
     * Set the buildpath to be used to find sub-projects.
     *
     * @param  s an Ant Path object containing the buildpath.
     */
    public void setBuildpath(Path s) {
        getBuildpath().append(s);
    }

    /**
     * Creates a nested build path, and add it to the implicit build path.
     *
     * @return the newly created nested build path.
     */
    public Path createBuildpath() {
        return getBuildpath().createPath();
    }

    /**
     * Creates a nested <code>&lt;buildpathelement&gt;</code>,
     * and add it to the implicit build path.
     *
     * @return the newly created nested build path element.
     */
    public Path.PathElement createBuildpathElement() {
        return getBuildpath().createPathElement();
    }

    /**
     * Gets the implicit build path, creating it if <code>null</code>.
     *
     * @return the implicit build path.
     */
    private Path getBuildpath() {
        if (buildpath == null) {
            buildpath = new Path(getProject());
        }
        return buildpath;
    }

    /**
     * Buildpath to use, by reference.
     *
     * @param  r a reference to an Ant Path object containing the buildpath.
     */
    public void setBuildpathRef(Reference r) {
        createBuildpath().setRefid(r);
    }

    /**
     * Creates the &lt;ant&gt; task configured to run a specific target.
     *
     * @param directory : if not null the directory where the build should run
     *
     * @return the ant task, configured with the explicit properties and
     *         references necessary to run the sub-build.
     */
    private Ant createAntTask(File directory) {
        Ant antTask = new Ant(this);
        antTask.init();
        if (subTarget != null && !subTarget.isEmpty()) {
            antTask.setTarget(subTarget);
        }


        if (output != null) {
            antTask.setOutput(output);
        }

        if (directory != null) {
            antTask.setDir(directory);
        } else {
            antTask.setUseNativeBasedir(true);
        }

        antTask.setInheritAll(inheritAll);

        properties.forEach(p -> copyProperty(antTask.createProperty(), p));

        propertySets.forEach(antTask::addPropertyset);

        antTask.setInheritRefs(inheritRefs);

        references.forEach(antTask::addReference);

        return antTask;
    }

    /**
     * Assigns an Ant property to another.
     *
     * @param  to the destination property whose content is modified.
     * @param  from the source property whose content is copied.
     */
    private static void copyProperty(Property to, Property from) {
        to.setName(from.getName());

        if (from.getValue() != null) {
            to.setValue(from.getValue());
        }
        if (from.getFile() != null) {
            to.setFile(from.getFile());
        }
        if (from.getResource() != null) {
            to.setResource(from.getResource());
        }
        if (from.getPrefix() != null) {
            to.setPrefix(from.getPrefix());
        }
        if (from.getRefid() != null) {
            to.setRefid(from.getRefid());
        }
        if (from.getEnvironment() != null) {
            to.setEnvironment(from.getEnvironment());
        }
        if (from.getClasspath() != null) {
            to.setClasspath(from.getClasspath());
        }
    }

}
