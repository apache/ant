/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;

/**
 * Executes a given command, supplying a set of files as arguments. 
 *
 * <p>Only those files that are newer than their corresponding target
 * files will be handeled, the rest will be ignored.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class Transform extends ExecuteOn {

    protected Commandline.Marker targetFilePos = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper mapper = null;
    protected File destDir = null;

    /**
     * Has &lt;srcfile&gt; been specified before &lt;targetfile&gt;
     */
    protected boolean srcIsFirst = true;

    /**
     * Set the destination directory.
     */
    public void setDest(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Marker that indicates where the name of the target file should
     * be put on the command line.
     */
    public Commandline.Marker createTargetfile() {
        if (targetFilePos != null) {
            throw new BuildException(taskType + " doesn\'t support multiple targetfile elements.",
                                     location);
        }
        targetFilePos = cmdl.createMarker();
        srcIsFirst = (srcFilePos != null);
        return targetFilePos;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     location);
        }
        mapperElement = new Mapper(project);
        return mapperElement;
    }

    protected void checkConfiguration() {
        super.checkConfiguration();
        if (mapperElement == null) {
            throw new BuildException("no mapper specified", location);
        }
        if (destDir == null) {
            throw new BuildException("no dest attribute specified", location);
        }

        mapper = mapperElement.getImplementation();
    }

    /**
     * Return the list of files from this DirectoryScanner that should
     * be included on the command line - i.e. only those that are
     * newer than the corresponding target files.
     */
    protected String[] getFiles(File baseDir, DirectoryScanner ds) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        return sfs.restrict(ds.getIncludedFiles(), baseDir, destDir, mapper);
    }

    /**
     * Return the list of Directories from this DirectoryScanner that
     * should be included on the command line - i.e. only those that
     * are newer than the corresponding target files.
     */
    protected String[] getDirs(File baseDir, DirectoryScanner ds) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        return sfs.restrict(ds.getIncludedDirectories(), baseDir, destDir, 
                            mapper);
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     * @param baseDir filenames are relative to this dir
     */
    protected String[] getCommandline(String[] srcFiles, File baseDir) {
        if (targetFilePos == null) {
            return super.getCommandline(srcFiles, baseDir);
        }

        Vector targets = new Vector();
        Hashtable addedFiles = new Hashtable();
        for (int i=0; i<srcFiles.length; i++) {
            String[] subTargets = mapper.mapFileName(srcFiles[i]);
            if (subTargets != null) {
                for (int j=0; j<subTargets.length; j++) {
                    String name = (new File(destDir, subTargets[j])).getAbsolutePath();
                    if (!addedFiles.contains(name)) {
                        targets.addElement(name);
                        addedFiles.put(name, name);
                    }
                }
            }
        }
        String[] targetFiles = new String[targets.size()];
        targets.copyInto(targetFiles);
        
        String[] orig = cmdl.getCommandline();
        String[] result = new String[orig.length+srcFiles.length+targetFiles.length];

        int srcIndex = orig.length;
        if (srcFilePos != null) {
            srcIndex = srcFilePos.getPosition();
        }
        int targetIndex = targetFilePos.getPosition();

        if (srcIndex < targetIndex || (srcIndex == targetIndex && srcIsFirst)) {
            // 0 --> srcIndex
            System.arraycopy(orig, 0, result, 0, srcIndex);

            // srcIndex --> targetIndex
            System.arraycopy(orig, srcIndex, result, 
                             srcIndex + srcFiles.length,
                             targetIndex - srcIndex);

            // targets are already absolute file names
            System.arraycopy(targetFiles, 0, result, 
                             targetIndex + srcFiles.length, 
                             targetFiles.length);

            // targetIndex --> end
            System.arraycopy(orig, targetIndex, result, 
                             targetIndex + srcFiles.length + targetFiles.length,
                             orig.length - targetIndex);
        } else {
            // 0 --> targetIndex
            System.arraycopy(orig, 0, result, 0, targetIndex);

            // targets are already absolute file names
            System.arraycopy(targetFiles, 0, result, 
                             targetIndex,
                             targetFiles.length);

            // targetIndex --> srcIndex
            System.arraycopy(orig, targetIndex, result, 
                             targetIndex + targetFiles.length,
                             srcIndex - targetIndex);

            // srcIndex --> end
            System.arraycopy(orig, srcIndex, result, 
                             srcIndex + srcFiles.length + targetFiles.length,
                             orig.length - srcIndex);
            srcIndex += targetFiles.length;
        }


        for (int i=0; i < srcFiles.length; i++) {
            result[srcIndex+i] = 
                (new File(baseDir, srcFiles[i])).getAbsolutePath();
        }
        return result;
        
    }

    /**
     * Construct the command line for serial execution.
     *
     * @param srcFile The filename to add to the commandline
     * @param baseDir filename is relative to this dir
     */
    protected String[] getCommandline(String srcFile, File baseDir) {
        return getCommandline(new String[] {srcFile}, baseDir);
    }

}
