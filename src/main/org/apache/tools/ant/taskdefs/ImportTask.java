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

import org.apache.tools.ant.*;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * <i>EXPERIMENTAL:</i> This task is experimental and may be under continual
 * change till Ant1.6 ships; it may even be omitted from the product.
 * <p>
 * Task to import another build file into the current project.
 * <p>
 * It must be 'top level'. On execution it will read another Ant file
 * into the same Project.
 * <p>
 * <b>Important</b>: there is one limitation related to the top level
 * elements in the imported files. The current implementation will
 * add them at the end of the top-level ( instead of replacing the
 * import element - which would be more intuitive ).
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
    String file;

    /**
     * the name of the file to import. How relative paths are resolved is still
     * in flux: use absolute paths for safety.
     * @param file
     */
    public void setFile( String file ) {
        // I don't think we can use File - different rules
        // for relative paths.
        this.file=file;
    }

    /**
     *  This relies on the task order model.
     *
     */
    public void execute() throws BuildException
    {
        if (file == null) {
            throw new BuildException("import requires file attribute");
        }

        ProjectHelper helper=
                (ProjectHelper)getProject().getReference("ant.projectHelper");
        Vector importStack=helper.getImportStack();
        if( importStack.size() == 0) {
            // this happens if ant is used with a project
            // helper that doesn't set the import.
            throw new BuildException("import requires support in ProjectHelper");
        }
        Object currentSource=importStack.elementAt(importStack.size() - 1);

//        ProjectHelper2.AntXmlContext context;
//        context=(ProjectHelper2.AntXmlContext)project.getReference("ant.parsing.context");

//        File buildFile=context.buildFile;
//        File buildFileParent=context.buildFileParent;
        File buildFile=(File)currentSource;
        buildFile=new File( buildFile.getAbsolutePath());
        //System.out.println("Importing from " + currentSource);
        File buildFileParent=new File(buildFile.getParent());

        getProject().log("Importing file "+ file +" from "+
                    buildFile.getAbsolutePath(), Project.MSG_VERBOSE);

        // Paths are relative to the build file they're imported from,
        // *not* the current directory (same as entity includes).
        File importedFile = new File(file);
        if (!importedFile.isAbsolute()) {
            importedFile = new File( buildFileParent, file);
        }

        if (!importedFile.exists()) {
                throw new BuildException("Cannot find "+file+" imported from "+
                                         buildFile.getAbsolutePath());
        }

        if( importStack.contains(importedFile) ) {
            getProject().log("\nSkipped already imported file to avoid loop:\n   "+
                    importedFile + "\n",Project.MSG_WARN);
            return;
        }

//        // Add parent build file to the map to avoid cycles...
//        String parentFilename = getPath(buildFile);
//        if (!context.importedFiles.containsKey(parentFilename)) {
//            context.importedFiles.put(parentFilename, buildFile);
//        }
//
//        // Make sure we import the file only once
//        String importedFilename = getPath(importedFile);
//        if (context.importedFiles.containsKey(importedFilename)) {
//            project.log("\nSkipped already imported file:\n   "+
//                    importedFilename+"\n",Project.MSG_WARN);
//            return;
//        } else {
//            context.importedFiles.put(importedFilename, importedFile);
//        }

//        context.ignoreProjectTag=true;
//        context.helper.parse(project, importedFile,
//                new ProjectHelper2.RootHandler(context));

        helper.parse( getProject(), importedFile );
    }

    private static String getPath(File file) {
        try {
            return file.getCanonicalPath();
        }
        catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
