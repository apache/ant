/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * <p>
 * Task to import another build file into the current project.
 * <p>
 * It must be 'top level'. On execution it will read another Ant file
 * into the same Project.
 * <p>
 * <b>Important</b>: we have not finalized how relative file references
 * will be resolved in deep/complex build hierarchies -such as what happens
 * when an imported file imports another file. Use absolute references for
 * enhanced build file stability, especially in the imported files.
 *
 * Examples
 * <pre>
 * &lt;import file="../common-targets.xml" /&gt;
 * </pre>
 * Import targets from a file in a parent directory.
 *<p>
 * <pre>
 * &lt;import file="${deploy-platform}.xml" /&gt;
 * </pre>
 * Import the project defined by the property deploy-platform
 *
 * @author Nicola Ken Barozzi nicolaken@apache.org
 * @author Dominique Devienne DDevienne@lgc.com
 * @author Costin Manolache
 * @since Ant1.6
 * @ant.task category="control"
 */
public class ImportTask extends Task {
    private String file;
    private boolean optional;
    private static final FileUtils FILE_UTILS = FileUtils.newFileUtils();

    /**
     * sets the optional attribute
     *
     * @param optional if true ignore files that are not present,
     *                 default is false
     */
    public void setOptional(boolean optional) {
        this.optional = true;
    }
    
    /**
     * the name of the file to import. How relative paths are resolved is still
     * in flux: use absolute paths for safety.
     * @param file the name of the file
     */
    public void setFile(String file) {
        // I don't think we can use File - different rules
        // for relative paths.
        this.file = file;
    }

    /**
     *  This relies on the task order model.
     *
     */
    public void execute() {
        if (file == null) {
            throw new BuildException("import requires file attribute");
        }
        if (getOwningTarget() == null
            || !"".equals(getOwningTarget().getName())) {
            throw new BuildException("import only allowed as a top-level task");
        }
        
        ProjectHelper helper =
                (ProjectHelper) getProject().getReference("ant.projectHelper");
        Vector importStack = helper.getImportStack();

        if (importStack.size() == 0) {
            // this happens if ant is used with a project
            // helper that doesn't set the import.
            throw new BuildException("import requires support in ProjectHelper");
        }

        if (getLocation() == null || getLocation().getFileName() == null) {
            throw new BuildException("Unable to get location of import task");
        }

        File buildFile = new File(getLocation().getFileName());
        buildFile = new File(buildFile.getAbsolutePath());

        getProject().log("Importing file " + file + " from "
                         + buildFile.getAbsolutePath(), Project.MSG_VERBOSE);

        // Paths are relative to the build file they're imported from,
        // *not* the current directory (same as entity includes).

        File buildFileParent = new File(buildFile.getParent());
        File importedFile = FILE_UTILS.resolveFile(buildFileParent,  file);

        if (!importedFile.exists()) {
            String message =
                "Cannot find " + file + " imported from "
                + buildFile.getAbsolutePath();
            if (optional) {
                getProject().log(message, Project.MSG_VERBOSE);
                return;
            } else {
                throw new BuildException(message);
            }
        }

        importedFile = new File(getPath(importedFile));

        if (importStack.contains(importedFile)) {
            getProject().log(
                "Skipped already imported file:\n   "
                + importedFile + "\n", Project.MSG_WARN);
            return;
        }

        helper.parse(getProject(), importedFile);
    }

    private static String getPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
