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
 * 4. The names "Ant" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived
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
import java.io.IOException;

import java.util.Vector;
import java.util.Enumeration;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Reference;

import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;

/**
 * <i>EXPERIMENTAL:</i> This task is experimental and may be under continual
 * change till Ant1.6 ships; it may even be omitted from the product.
 * <p>
 * Calls a given target for all defined sub-builds. This is an extension
 * of ant for bulk project execution.
 *
 * @since Ant1.6
 * @author <a href="mailto:ddevienne@lgc.com">Dominique Devienne</a>
 * @ant.task name="subant" category="control"
 */
public class SubAnt
             extends Task {

    private Path buildpath;

    private String target = null;
    private String antfile = "build.xml";
    private boolean inheritAll = false;
    private boolean inheritRefs = false;
    private boolean failOnError = true;

    private Vector properties = new Vector();
    private Vector references = new Vector();

    /**
     * Runs the various sub-builds.
     */
    public void execute()
                throws BuildException {
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
        if (target == null) {
            target = getOwningTarget().getName();
        }
*/
        for (int i=0; i<count; ++i) {
            File file = new File(filenames[i]);
            if (file.isDirectory()) {
                file = new File(file, antfile);
            }
            execute(file);
        }
    }

    /**
     * Runs the given target on the provided build file.
     *
     * @param  file the build file to execute
     * @throws BuildException is the file cannot be found, read, is
     *         a directory, or the target called failed, but only if
     *         <code>failOnError</code> is <code>true</code>. Otherwise,
     *         a warning log message is simply output.
     */
    private void execute(File file)
                throws BuildException {
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            String msg = "Invalid file: "+file;
            if (failOnError) {
                throw new BuildException(msg);
            }
            log(msg, Project.MSG_WARN);
            return;
        }

        Ant ant = createAntTask();
        String antfilename = null;
        try {
            antfilename = file.getCanonicalPath();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        ant.setAntfile(antfilename);
        try {
            ant.execute();
        } catch (BuildException e) {
            if (failOnError) {
                throw e;
            }
            log("Failure for target '"+target
               +"' of: "+antfilename+"\n"
               +e.getMessage(), Project.MSG_WARN);
        }
    }

    /**
     * Sets the default build file name to append to directory
     * names found in the build path -default "build.xml"
     *
     * @param  antfile the short build file name. Defaults to "build.xml".
     */
    public void setAntfile(String antfile) {
        this.antfile = antfile;
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
     * <p>
     */
    //     REVISIT: Defaults to the target name that contains this task if not specified.
    public void setTarget(String target) {
        this.target = target;
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
        properties.addElement(p);
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s
     * nested <code>&lt;reference&gt;</code> element.
     *
     * @param  r the reference to pass on explicitly to the sub-build.
     */
    public void addReference(Ant.Reference r) {
        references.addElement(r);
    }

    /**
     * Adds a directory set to the implicit build path.
     * <p>
     * <em>Note that the directories will be added to the build path
     * in no particular order, so if order is significant, one should
     * use a file list instead!</em>
     *
     * @param  set the directory set to add.
     */
    public void addDirset(DirSet set) {
        getBuildpath().addDirset(set);
    }

    /**
     * Adds a file set to the implicit build path.
     * <p>
     * <em>Note that the directories will be added to the build path
     * in no particular order, so if order is significant, one should
     * use a file list instead!</em>
     *
     * @param  set the file set to add.
     */
    public void addFileset(FileSet set) {
        getBuildpath().addFileset(set);
    }

    /**
     * Adds an ordered file list to the implicit build path.
     * <p>
     * <em>Note that contrary to file and directory sets, file lists
     * can reference non-existent files or directories!</em>
     *
     * @param  list the file list to add.
     */
    public void addFilelist(FileList list) {
        getBuildpath().addFilelist(list);
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
    public Path.PathElement createBuildpathElement()
                            throws BuildException {
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
     * @return the ant task, configured with the explicit properties and
     *         references necessary to run the sub-build.
     */
    private Ant createAntTask() {
        Ant ant = (Ant) getProject().createTask("ant");
        ant.setOwningTarget(getOwningTarget());
        ant.init();
        if(target.length()>0) {
            ant.setTarget(target);
        }

        ant.setInheritAll(inheritAll);
        for (Enumeration i = properties.elements(); i.hasMoreElements();) {
            copyProperty(ant.createProperty(), (Property) i.nextElement());
        }
        
        ant.setInheritRefs(inheritRefs);
        for (Enumeration i = references.elements(); i.hasMoreElements();) {
            ant.addReference((Ant.Reference) i.nextElement());
        }

        return ant;
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

} // END class SubAnt
