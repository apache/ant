/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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


import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.PatternSet;

/**
 * Export packages from the Visual Age for Java workspace.
 * The packages are specified similar to all other MatchingTasks.
 * Since the VA Workspace is not file based, this task is simulating
 * a directory hierarchy for the workspace:
 * The 'root' contains all project 'dir's, and the projects contain
 * their respective package 'dir's.
 * Example:
 * <blockquote>
 * &lt;vajexport destdir=&quot;C:/builddir/source&quot;&gt;
 * &nbsp;&lt;include name=&quot;/MyVAProject/org/foo/subsystem1/**&quot; /&gt;
 * &nbsp;&lt;exclude name=&quot;/MyVAProject/org/foo/subsystem1/test/**&quot;/&gt;
 * &lt;/vajexport&gt;
 * </blockquote>
 * exports all packages in the project MyVAProject which start with
 * 'org.foo.subsystem1' except of these starting with
 * 'org.foo.subsystem1.test'.
 *
 * <p>Parameters:
 * <table border="1" cellpadding="2" cellspacing="0">
 * <tr>
 *   <td valign="top"><b>Attribute</b></td>
 *   <td valign="top"><b>Description</b></td>
 *   <td align="center" valign="top"><b>Required</b></td>
 * </tr>
 * <tr>
 *   <td valign="top">destdir</td>
 *   <td valign="top">location to store the exported files</td>
 *   <td align="center" valign="top">Yes</td>
 * <tr>
 *   <td valign="top">exportSources</td>
 *   <td valign="top">export Java sources, defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">exportResources</td>
 *   <td valign="top">export resource files, defaults to "yes"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">exportClasses</td>
 *   <td valign="top">export class files, defaults to "no"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">exportDebugInfo</td>
 *   <td valign="top">include debug info in exported class files,
 *                    defaults to "no"</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">defaultexcludes</td>
 *   <td valign="top">use default excludes when exporting,
 *                    defaults to "yes".
 *                    Default excludes are: IBM&#x2f;**,
 *                    Java class libraries&#x2f;**, Sun class libraries&#x2f;**,
 *                    JSP Page Compile Generated Code&#x2f;**, Visual Age*&#x2f;**</td>
 *   <td align="center" valign="top">No</td>
 * </tr>
 * <tr>
 *   <td valign="top">overwrite</td>
 *   <td valign="top">overwrite existing files, defaults to "yes"</td>
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
 * @author Wolf Siberski, TUI Infotec GmbH
 * @author Martin Landers, Beck et al. projects
 */

public class VAJExport extends VAJTask {
    //set set... method comments for description
    protected File destDir;
    protected boolean exportSources = true;
    protected boolean exportResources = true;
    protected boolean exportClasses = false;
    protected boolean exportDebugInfo = false;
    protected boolean useDefaultExcludes = true;
    protected boolean overwrite = true;

    protected PatternSet patternSet = new PatternSet();

    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() {
        return patternSet.createExclude();
    }

    /**
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() {
        return patternSet.createInclude();
    }

    /**
     * do the export
     */
    public void execute() throws BuildException {
        // first off, make sure that we've got a destdir
        if (destDir == null) {
            throw new BuildException("destdir attribute must be set!");
        }

        // delegate the export to the VAJUtil object.
        try {
            getUtil().exportPackages(destDir,
                patternSet.getIncludePatterns(getProject()),
                patternSet.getExcludePatterns(getProject()),
                exportClasses, exportDebugInfo,
                exportResources, exportSources,
                useDefaultExcludes, overwrite);
        } catch (BuildException ex) {
            if (haltOnError) {
                throw ex;
            } else {
                log(ex.toString());
            }
        }
    }

    /**
     * Sets whether default exclusions should be used or not; default true.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
    }

    /**
     * Set the destination directory into which the selected
     * items should be exported; required.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space. Currently only patterns denoting packages are
     * supported
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        patternSet.setExcludes(excludes);
    }

    /**
     * optional flag to export the class files; default false.
     */
    public void setExportClasses(boolean doExport) {
        exportClasses = doExport;
    }

    /**
     * optional flag to export the debug info; default false.
     * debug info
     */
    public void setExportDebugInfo(boolean doExport) {
        exportDebugInfo = doExport;
    }

    /**
     * optional flag to export the resource file; default true.
     */
    public void setExportResources(boolean doExport) {
        exportResources = doExport;
    }

    /**
     * optional flag to export the Java files; default true.
     */
    public void setExportSources(boolean doExport) {
        exportSources = doExport;
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space. Currently only patterns denoting packages are
     * supported
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        patternSet.setIncludes(includes);
    }

    /**
     * if Overwrite is set, files will be overwritten during export
     */
    public void setOverwrite(boolean doOverwrite) {
        overwrite = doOverwrite;
    }

}
