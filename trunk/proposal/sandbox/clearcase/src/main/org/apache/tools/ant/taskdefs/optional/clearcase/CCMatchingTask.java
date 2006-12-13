/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.io.File;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Base task for all Clearcase tasks involving multiple-file processing.
 *
 */
public abstract class CCMatchingTask extends MatchingTask {
    
    /** view path to use. Equivalent to base directory of processing */
    protected File viewpath;

    /** cc helper tools */
    protected CCUtils utils = new CCUtils(this);
    
    /** the set of collected files to checkin */
    protected Hashtable files = null;

    /** comments to use for the operation */
    protected String comment = null;

    /** the comment file created from the comments. It avoids escaping issues */
    protected File commentFile;

    /** cleartool options (and command as well) */
    protected String[] options;

    /**
     * @return a vector of options representing the cleartool arguments.
     * The last arguments is normally allocated and replaced at the last
     * moment before running the command.
     * @see #execute(String[], CCFile)
     */
    protected abstract Vector getOptions();
    
    /**
     * @param file the clearcase file
     * @return whether this file should be accepted or not by the
     * command to restrict the file processing and errors. For
     * example you might not want to checkin files that are
     * already checked in and that are collected by the fileset
     */
    protected boolean accept(CCFile file){
        return true;
    }

    public void execute() throws BuildException {
        try {
            preExecute();
            doExecute();
        } finally {
            postExecute();
        }
    }
    
    /** check for attributes and builds the options array */
    protected void preExecute() throws BuildException {
        if (viewpath == null){
            throw new BuildException("Invalid viewpath");
        }
        if (comment != null){
            commentFile = CCUtils.createCommentFile(comment);
        }
        Vector v = getOptions();
        options = new String[v.size()];
        v.copyInto(options);
    }
    
    /** clean up method calls after doExecute */
    protected void postExecute(){
        if (commentFile != null){
            commentFile.delete();
        }
    }

    /**
     * The core processing. It loops over all files and calls
     * <tt>execute(String[], CCFile)</tt>
     */
    protected void doExecute() throws BuildException {
        Enumeration elems = getFiles().elements();
        log("Processing " + files.size() + " elements...");
        while ( elems.hasMoreElements() ){
            execute(options, (CCFile)elems.nextElement());
        }
    }
    
    /**
     * Calls the cleartool command with the appropriate parameters. Note the
     * the last array element is supposed to be used by the filepath.
     * @param args the cleartool command to execute. The last element being allocated
     * and representing the filepath.
     * @param file the file element to process.
     * @throws BuildException thrown if an error occurs when processing the
     * cleartool command.
     */
    protected void execute(String[] args, CCFile file) throws BuildException {
        args[args.length - 1] = file.getPath();
        CmdResult res = utils.cleartool(args);
        if (res.getStatus() != 0){
            throw new BuildException(res.getStdErr());
        }
    }

    /**
     * Restrict the set of files/directories to be processed.
     * @return the set of files to be processed. The key is made up
     * the filepath and the value is the <tt>CCFile</tt> instance.
     * @see #accept(CCFile)
     */
    protected Hashtable getFiles(){
        if (files != null){
            return files;
        }
        files = new Hashtable();
        DirectoryScanner ds = getDirectoryScanner(viewpath);
        String[] includes = ds.getIncludedDirectories();
        addElements(files, ds.getBasedir(), includes);
        includes = ds.getIncludedFiles();
        addElements(files, ds.getBasedir(), includes);
        return files;
    }

    /**
     * Helper method to restrict a set of relative elements and add them
     * to a map.
     * @param map the map w/ a path/CCFile mapping to add elements to.
     * @param basedir the base directory for all elements in the array.
     * @param elems the set of elements to restrict.
     * @see #accept(CCFile)
     */
    protected void addElements(Hashtable map, File basedir, String[] elems){
        for (int i = 0; i < elems.length; i++){
            CCFile f = new CCFile(basedir, elems[i]);
            if ( accept(f) ){
                map.put(f.getPath(), f);
            }
        }
    }

// Ant bean setters
    
    public void setViewPath(File value){
        this.viewpath = value;
    }

    public void setComment(String value){
        comment = value;
    }

}
