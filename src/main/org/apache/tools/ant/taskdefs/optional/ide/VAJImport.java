/*
 * Copyright  2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.ide;


import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

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
 * <p>Parameters:
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
 * <tr>
 *   <td valign="top">remote</td>
 *   <td valign="top">remote tool server to run this command against
 *                    (format: &lt;servername&gt; : &lt;port no&gt;)</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">haltonerror</td>
 *   <td valign="top">stop the build process if an error occurs,
 *                    defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * </table>
 *
 * @author Glenn McAllister, inspired by a similar task written by Peter Kelley
 * @author Martin Landers, Beck et al. projects
 */
public class VAJImport extends VAJTask {
    protected Vector filesets = new Vector();
    protected boolean importSources = true;
    protected boolean importResources = true;
    protected boolean importClasses = false;
    protected String importProject = null;
    protected boolean useDefaultExcludes = true;


    /**
     * Extended DirectoryScanner that has accessors for the
     * includes and excludes fields.
     *
     * This is kindof a hack to get includes and excludes
     * from the directory scanner. In order to keep
     * the URLs short we only want to send the patterns to the
     * remote tool server and let him figure out the files.
     *
     * This replaces the former reflection hack that
     * didn't compile for old JDKs.
     *
     * @see VAJImport#importFileSet(FileSet)
     */
    private static class LocalDirectoryScanner extends DirectoryScanner {
        public String[] getIncludes() { return includes; }
        public String[] getExcludes() { return excludes; }
    }

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

        try {
            for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
                importFileset((FileSet) e.nextElement());
            }
        } catch (BuildException ex) {
            if (haltOnError) {
                throw ex;
            } else {
                log(ex.toString());
            }
        }
    }

    /**
     * Import all files from the fileset into the Project in the
     * Workspace.
     */
    protected void importFileset(FileSet fileset) {
        LocalDirectoryScanner ds = new LocalDirectoryScanner();
        fileset.setupDirectoryScanner(ds, this.getProject());
        ds.scan();
        if (ds.getIncludedFiles().length == 0) {
            return;
        }

        String[] includes = ds.getIncludes();
        String[] excludes = ds.getExcludes();

        getUtil().importFiles(importProject, ds.getBasedir(),
                includes, excludes,
                importClasses, importResources, importSources,
                useDefaultExcludes);
    }
}
