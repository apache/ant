/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights 
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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.SourceFileScanner;

import java.util.Hashtable;
import java.util.Vector;
import java.io.File;
import java.io.IOException;

/**
 * Executes a given command, supplying a set of files as arguments. 
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 *
 * @since Ant 1.2
 *
 * @ant.task category="control" name="apply"
 */
public class ExecuteOn extends ExecTask {

    protected Vector filesets = new Vector();
    private boolean relative = false;
    private boolean parallel = false;
    protected String type = "file";
    protected Commandline.Marker srcFilePos = null;
    private boolean skipEmpty = false;
    protected Commandline.Marker targetFilePos = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper mapper = null;
    protected File destDir = null;

    /**
     * Has &lt;srcfile&gt; been specified before &lt;targetfile&gt;
     */
    protected boolean srcIsFirst = true;

    /**
     * Source files to operate upon.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Whether the filenames should be passed on the command line as
     * absolute or relative pathnames. Paths are relative to the base
     * directory of the corresponding fileset for source files or the
     * dest attribute for target files.
     */
    public void setRelative(boolean relative) {
        this.relative = relative;
    }


    /**
     * If true, run the command only once, appending all files as arguments.
     * If false, command will be executed once for every file. Defaults to false.
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Whether the command works only on files, directories or both?
     */
    public void setType(FileDirBoth type) {
        this.type = type.getValue();
    }

    /**
     * If no source files have been found or are newer than their
     * corresponding target files, do not run the command.
     */
    public void setSkipEmptyFilesets(boolean skip) {
        skipEmpty = skip;
    }

    /**
     * The directory where target files are to be placed.
     */
    public void setDest(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Marker that indicates where the name of the source file should
     * be put on the command line.
     */
    public Commandline.Marker createSrcfile() {
        if (srcFilePos != null) {
            throw new BuildException(taskType + " doesn\'t support multiple "
                                     + "srcfile elements.", location);
        }
        srcFilePos = cmdl.createMarker();
        return srcFilePos;
    }

    /**
     * Marker that indicates where the name of the target file should
     * be put on the command line.
     */
    public Commandline.Marker createTargetfile() {
        if (targetFilePos != null) {
            throw new BuildException(taskType + " doesn\'t support multiple "
                                     + "targetfile elements.", location);
        }
        targetFilePos = cmdl.createMarker();
        srcIsFirst = (srcFilePos != null);
        return targetFilePos;
    }

    /**
     * Mapper to use for mapping source files to target files.
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     location);
        }
        mapperElement = new Mapper(project);
        return mapperElement;
    }

    /**
     * @todo using taskName here is brittle, as a user could override it.
     *       this should probably be modified to use the classname instead.
     */
    protected void checkConfiguration() {
        if ("execon".equals(taskName)) {
            log("!! execon is deprecated. Use apply instead. !!");
        }
        
        super.checkConfiguration();
        if (filesets.size() == 0) {
            throw new BuildException("no filesets specified", location);
        }

        if (targetFilePos != null || mapperElement != null 
            || destDir != null) {

            if (mapperElement == null) {
                throw new BuildException("no mapper specified", location);
            }
            if (destDir == null) {
                throw new BuildException("no dest attribute specified", 
                                         location);
            }
            mapper = mapperElement.getImplementation();
        }
    }

    protected void runExec(Execute exe) throws BuildException {
        try {

            Vector fileNames = new Vector();
            Vector baseDirs = new Vector();
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                File base = fs.getDir(project);
                DirectoryScanner ds = fs.getDirectoryScanner(project);

                if (!"dir".equals(type)) {
                    String[] s = getFiles(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }

                if (!"file".equals(type)) {
                    String[] s = getDirs(base, ds);;
                    for (int j = 0; j < s.length; j++) {
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }

                if (fileNames.size() == 0 && skipEmpty) {
                    log("Skipping fileset for directory "
                        + base + ". It is empty.", Project.MSG_INFO);
                    continue;
                }

                if (!parallel) {
                    String[] s = new String[fileNames.size()];
                    fileNames.copyInto(s);
                    for (int j = 0; j < s.length; j++) {
                        String[] command = getCommandline(s[j], base);
                        log(Commandline.describeCommand(command), 
                            Project.MSG_VERBOSE);
                        exe.setCommandline(command);
                        runExecute(exe);
                    }
                    fileNames.removeAllElements();
                    baseDirs.removeAllElements();
                }
            }

            if (parallel && (fileNames.size() > 0 || !skipEmpty)) {
                String[] s = new String[fileNames.size()];
                fileNames.copyInto(s);
                File[] b = new File[baseDirs.size()];
                baseDirs.copyInto(b);
                String[] command = getCommandline(s, b);
                log(Commandline.describeCommand(command), Project.MSG_VERBOSE);
                exe.setCommandline(command);
                runExecute(exe);
            }

        } catch (IOException e) {
            throw new BuildException("Execute failed: " + e, e, location);
        } finally {
            // close the output file if required
            logFlush();
        }
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     * @param baseDir filenames are relative to this dir
     */
    protected String[] getCommandline(String[] srcFiles, File[] baseDirs) {
        Vector targets = new Vector();
        if (targetFilePos != null) {
            Hashtable addedFiles = new Hashtable();
            for (int i = 0; i < srcFiles.length; i++) {
                String[] subTargets = mapper.mapFileName(srcFiles[i]);
                if (subTargets != null) {
                    for (int j = 0; j < subTargets.length; j++) {
                        String name = null;
                        if (!relative) {
                            name = (new File(destDir, subTargets[j])).getAbsolutePath();
                        } else {
                            name = subTargets[j];
                        }
                        if (!addedFiles.contains(name)) {
                            targets.addElement(name);
                            addedFiles.put(name, name);
                        }
                    }
                }
            }
        }
        String[] targetFiles = new String[targets.size()];
        targets.copyInto(targetFiles);
        
        String[] orig = cmdl.getCommandline();
        String[] result 
            = new String[orig.length + srcFiles.length + targetFiles.length];

        int srcIndex = orig.length;
        if (srcFilePos != null) {
            srcIndex = srcFilePos.getPosition();
        }

        if (targetFilePos != null) {
            int targetIndex = targetFilePos.getPosition();

            if (srcIndex < targetIndex 
                || (srcIndex == targetIndex && srcIsFirst)) {

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

        } else { // no targetFilePos

            // 0 --> srcIndex
            System.arraycopy(orig, 0, result, 0, srcIndex);
            // srcIndex --> end
            System.arraycopy(orig, srcIndex, result, 
                             srcIndex + srcFiles.length,
                             orig.length - srcIndex);

        }

        // fill in source file names
        for (int i = 0; i < srcFiles.length; i++) {
            if (!relative) {
                result[srcIndex + i] = 
                    (new File(baseDirs[i], srcFiles[i])).getAbsolutePath();
            } else {
                result[srcIndex + i] = srcFiles[i];
            }
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
        return getCommandline(new String[] {srcFile}, new File[] {baseDir});
    }

    /**
     * Return the list of files from this DirectoryScanner that should
     * be included on the command line.
     */
    protected String[] getFiles(File baseDir, DirectoryScanner ds) {
        if (mapper != null) {
            SourceFileScanner sfs = new SourceFileScanner(this);
            return sfs.restrict(ds.getIncludedFiles(), baseDir, destDir, 
                                mapper);
        } else {
            return ds.getIncludedFiles();
        }
    }

    /**
     * Return the list of Directories from this DirectoryScanner that
     * should be included on the command line.
     */
    protected String[] getDirs(File baseDir, DirectoryScanner ds) {
        if (mapper != null) {
            SourceFileScanner sfs = new SourceFileScanner(this);
            return sfs.restrict(ds.getIncludedDirectories(), baseDir, destDir, 
                                mapper);
        } else {
            return ds.getIncludedDirectories();
        }
    }

    /**
     * Enumerated attribute with the values "file", "dir" and "both"
     * for the type attribute.  
     */
    public static class FileDirBoth extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues
         */
        public String[] getValues() {
            return new String[] {"file", "dir", "both"};
        }
    }
}
