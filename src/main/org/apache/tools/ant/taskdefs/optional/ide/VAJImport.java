/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ide;


import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

import org.apache.tools.ant.types.FileSet;

import java.lang.reflect.Field;

/**
 * Import source, class files, and resources to the Visual Age for Java
 * workspace.
 * <p>
 * Example:
 * <pre>
 * &lt;vajimport project="MyVAProject"&gt;
 *   &lt;fileset dir="src"&gt;
 *     &lt;include name="org/foo/subsystem1/**" /&gt;
 *     &lt;exclude name="/org/foo/subsystem1/test/**" /&gt;
 *  &lt;/fileset&gt;
 * &lt;/vajexport&gt;
 * </pre>
 * import all source and resource files from the "src" directory
 * which start with 'org.foo.subsystem1', except of these starting with
 * 'org.foo.subsystem1.test' into the project MyVAProject.
 * </p>
 * <p>If MyVAProject isn't loaded into the Workspace, a new edition is
 * created in the repository and automatically loaded into the Workspace.
 * There has to be at least one nested FileSet element.
 * </p>
 * <p>There are attributes to choose which items to export:
 * <table border="1" cellpadding="2" cellspacing="0">
 * <tr>
 *   <td valign="top"><b>Attribute</b></td>
 *   <td valign="top"><b>Description</b></td>
 *   <td align="center" valign="top"><b>Required</b></td>
 * </tr>
 * <tr>
 *   <td valign="top">project</td>
 *   <td valign="top">the name of the Project to import to</td>
 *   <td align="center" valign="top">Yes</td>
 * </tr>
 * <tr>
 *   <td valign="top">importSources</td>
 *   <td valign="top">import Java sources, defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">importResources</td>
 *   <td valign="top">import resource files (anything that doesn't
 *                    end with .java or .class), defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">importClasses</td>
 *   <td valign="top">import class files, defaults to "no"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * </table>
 *
 * @author Glenn McAllister, inspired by a similar task written by Peter Kelley
 */
public class VAJImport extends VAJTask {
    protected Vector filesets = new Vector();
    protected boolean importSources = true;
    protected boolean importResources = true;
    protected boolean importClasses = false;
    protected String importProject = null;
    protected boolean useDefaultExcludes = true;


    /**
     * The VisualAge for Java Project name to import into.
     */
    public void setProject(String projectName) {
        this.importProject = projectName;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Flag to import .class files; optional, default false.
     */
    public void setImportClasses(boolean importClasses) {
        this.importClasses = importClasses;
    }

    /**
     * Import resource files (anything that doesn't end in
     * .class or .java); optional, default true.
     */
    public void setImportResources(boolean importResources) {
        this.importResources = importResources;
    }

    /**
     * Import .java files; optional, default true.
     */
    public void setImportSources(boolean importSources) {
        this.importSources = importSources;
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Do the import.
     */
    public void execute() throws BuildException {
        if (filesets.size() == 0) {
            throw new BuildException("At least one fileset is required!");
        }

        if (importProject == null || "".equals(importProject)) {
            throw new BuildException("The VisualAge for Java Project name is required!");
        }

        for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
            importFileset((FileSet) e.nextElement());
        }
    }

    /**
     * Import all files from the fileset into the Project in the
     * Workspace.
     */
    protected void importFileset(FileSet fileset) {
        DirectoryScanner ds = fileset.getDirectoryScanner(this.project);
        if (ds.getIncludedFiles().length == 0) {
            return;
        }

        String[] includes = null;
        String[] excludes = null;

        // Hack to get includes and excludes. We could also use getIncludedFiles,
        // but that would result in very long HTTP-requests.
        // Therefore we want to send the patterns only to the remote tool server
        // and let him figure out the files.
        try {
            Class directoryScanner = ds.getClass();

            Field includesField = directoryScanner.getDeclaredField("includes");
            includesField.setAccessible(true);
            includes = (String[]) includesField.get(ds);

            Field excludesField = directoryScanner.getDeclaredField("excludes");
            excludesField.setAccessible(true);
            excludes = (String[]) excludesField.get(ds);
        } catch (NoSuchFieldException nsfe) {
            throw new BuildException(
                "DirectoryScanner.includes or .excludes missing" + nsfe.getMessage());
        } catch (IllegalAccessException iae) {
            throw new BuildException(
                "Access to DirectoryScanner.includes or .excludes not allowed");
        }

        getUtil().importFiles(importProject, ds.getBasedir(),
                includes, excludes,
                importClasses, importResources, importSources,
                useDefaultExcludes);
    }
}
