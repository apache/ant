/*
 * Copyright  2000-2004 The Apache Software Foundation.
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * Executes a given command, supplying a set of files as arguments.
 *
 * @since Ant 1.2
 *
 * @ant.task category="control" name="apply"
 */
public class ExecuteOn extends ExecTask {

    protected Vector filesets = new Vector(); // contains AbstractFileSet
                                              // (both DirSet and FileSet)
    private Vector filelists = new Vector();
    private boolean relative = false;
    private boolean parallel = false;
    private boolean forwardSlash = false;
    protected String type = "file";
    protected Commandline.Marker srcFilePos = null;
    private boolean skipEmpty = false;
    protected Commandline.Marker targetFilePos = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper mapper = null;
    protected File destDir = null;
    private int maxParallel = -1;
    private boolean addSourceFile = true;
    private boolean verbose = false;
    private boolean ignoreMissing = true;
    private boolean force = false;

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
     * Adds directories to operate on.
     *
     * @param  set the DirSet to add.
     *
     * @since Ant 1.6
     */
    public void addDirset(DirSet set) {
        filesets.addElement(set);
    }
    /**
     * Source files to operate upon.
     */
    public void addFilelist(FileList list) {
        filelists.addElement(list);
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
     * The source and target file names on Windows and OS/2 must use
     * forward slash as file separator.
     */
    public void setForwardslash(boolean forwardSlash) {
        this.forwardSlash = forwardSlash;
    }

    /**
     * Limit the command line length by passing at maximum this many
     * sourcefiles at once to the command.
     *
     * <p>Set to &lt;= 0 for unlimited - this is the default.</p>
     *
     * @since Ant 1.6
     */
    public void setMaxParallel(int max) {
        maxParallel = max;
    }

    /**
     * Whether to send the source file name on the command line.
     *
     * <p>Defaults to <code>true</code>.
     *
     * @since Ant 1.6
     */
    public void setAddsourcefile(boolean b) {
        addSourceFile = b;
    }

    /**
     * Whether to print a verbose summary after execution.
     *
     * @since Ant 1.6
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }

    /**
     * Whether to ignore nonexistent files from filelists.
     *
     * @since Ant 1.6.2
     */
    public void setIgnoremissing(boolean b) {
        ignoreMissing = b;
    }

    /**
     * Whether to bypass timestamp comparisons for target files.
     *
     * @since Ant 1.7
     */
    public void setForce(boolean b) {
        force = b;
    }

    /**
     * Marker that indicates where the name of the source file should
     * be put on the command line.
     */
    public Commandline.Marker createSrcfile() {
        if (srcFilePos != null) {
            throw new BuildException(getTaskType() + " doesn\'t support multiple "
                                     + "srcfile elements.", getLocation());
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
            throw new BuildException(getTaskType() + " doesn\'t support multiple "
                                     + "targetfile elements.", getLocation());
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
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * @todo using taskName here is brittle, as a user could override it.
     *       this should probably be modified to use the classname instead.
     */
    protected void checkConfiguration() {
        if ("execon".equals(getTaskName())) {
            log("!! execon is deprecated. Use apply instead. !!");
        }

        super.checkConfiguration();
        if (filesets.size() == 0 && filelists.size() == 0) {
            throw new BuildException("no filesets and no filelists specified",
                                     getLocation());
        }

        if (targetFilePos != null || mapperElement != null
            || destDir != null) {

            if (mapperElement == null) {
                throw new BuildException("no mapper specified", getLocation());
            }
            if (destDir == null) {
                throw new BuildException("no dest attribute specified",
                                         getLocation());
            }
            mapper = mapperElement.getImplementation();
        }
    }

    protected ExecuteStreamHandler createHandler() throws BuildException {
        //if we have a RedirectorElement, return a decoy
        return (redirectorElement == null)
            ? super.createHandler() : new PumpStreamHandler();
    }

    protected void setupRedirector() {
        super.setupRedirector();
        redirector.setAppendProperties(true);
    }

    protected void runExec(Execute exe) throws BuildException {
        int totalFiles = 0;
        int totalDirs = 0;
        boolean haveExecuted = false;
        try {

            Vector fileNames = new Vector();
            Vector baseDirs = new Vector();
            for (int i = 0; i < filesets.size(); i++) {
                String currentType = type;
                AbstractFileSet fs = (AbstractFileSet) filesets.elementAt(i);
                if (fs instanceof DirSet) {
                    if (!"dir".equals(type)) {
                        log("Found a nested dirset but type is " + type + ". "
                            + "Temporarily switching to type=\"dir\" on the"
                            + " assumption that you really did mean"
                            + " <dirset> not <fileset>.", Project.MSG_DEBUG);
                        currentType = "dir";
                    }
                }
                File base = fs.getDir(getProject());

                DirectoryScanner ds = fs.getDirectoryScanner(getProject());

                if (!"dir".equals(currentType)) {
                    String[] s = getFiles(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalFiles++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }

                if (!"file".equals(currentType)) {
                    String[] s = getDirs(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalDirs++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }

                if (fileNames.size() == 0 && skipEmpty) {
                    int includedCount
                        = ((!"dir".equals(currentType))
                        ? ds.getIncludedFilesCount() : 0)
                        + ((!"file".equals(currentType))
                        ? ds.getIncludedDirsCount() : 0);

                    log("Skipping fileset for directory " + base + ". It is "
                        + ((includedCount > 0) ? "up to date." : "empty."),
                        Project.MSG_INFO);
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

                        if (redirectorElement != null) {
                            setupRedirector();
                            redirectorElement.configure(redirector, s[j]);
                        }

                        if (redirectorElement != null || haveExecuted) {
                            // need to reset the stream handler to restart
                            // reading of pipes;
                            // go ahead and do it always w/ nested redirectors
                            exe.setStreamHandler(redirector.createHandler());
                        }
                        runExecute(exe);
                        haveExecuted = true;
                    }
                    fileNames.removeAllElements();
                    baseDirs.removeAllElements();
                }
            }

            for (int i = 0; i < filelists.size(); i++) {
                FileList list = (FileList) filelists.elementAt(i);
                File base = list.getDir(getProject());
                String[] names = getFilesAndDirs(list);

                for (int j = 0; j < names.length; j++) {
                    File f = new File(base, names[j]);
                    if ((!ignoreMissing) || (f.isFile() && !"dir".equals(type))
                        || (f.isDirectory() && !"file".equals(type))) {

                        if (ignoreMissing || f.isFile()) {
                            totalFiles++;
                        } else {
                            totalDirs++;
                        }

                        fileNames.addElement(names[j]);
                        baseDirs.addElement(base);
                    }
                }

                if (fileNames.size() == 0 && skipEmpty) {
                    DirectoryScanner ds = new DirectoryScanner();
                    ds.setBasedir(base);
                    ds.setIncludes(list.getFiles(getProject()));
                    ds.scan();
                    int includedCount
                        = ds.getIncludedFilesCount() + ds.getIncludedDirsCount();

                    log("Skipping filelist for directory " + base + ". It is "
                        + ((includedCount > 0) ? "up to date." : "empty."),
                        Project.MSG_INFO);
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

                        if (redirectorElement != null) {
                            setupRedirector();
                            redirectorElement.configure(redirector, s[j]);
                        }

                        if (redirectorElement != null || haveExecuted) {
                            // need to reset the stream handler to restart
                            // reading of pipes;
                            // go ahead and do it always w/ nested redirectors
                            exe.setStreamHandler(redirector.createHandler());
                        }
                        runExecute(exe);
                        haveExecuted = true;
                    }
                    fileNames.removeAllElements();
                    baseDirs.removeAllElements();
                }
            }

            if (parallel && (fileNames.size() > 0 || !skipEmpty)) {
                runParallel(exe, fileNames, baseDirs);
                haveExecuted = true;
            }

            if (haveExecuted) {
                log("Applied " + cmdl.getExecutable() + " to "
                    + totalFiles + " file"
                    + (totalFiles != 1 ? "s" : "") + " and "
                    + totalDirs + " director"
                    + (totalDirs != 1 ? "ies" : "y") + ".",
                    verbose ? Project.MSG_INFO : Project.MSG_VERBOSE);
            }

        } catch (IOException e) {
            throw new BuildException("Execute failed: " + e, e, getLocation());
        } finally {
            // close the output file if required
            logFlush();
            redirector.setAppendProperties(false);
            redirector.setProperties();
        }
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     * @param baseDirs filenames are relative to this dir
     */
    protected String[] getCommandline(String[] srcFiles, File[] baseDirs) {
        final char fileSeparator = File.separatorChar;
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
                        if (forwardSlash && fileSeparator != '/') {
                            name = name.replace(fileSeparator, '/');
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

        if (!addSourceFile) {
            srcFiles = new String[0];
        }

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
            if (forwardSlash && fileSeparator != '/') {
                result[srcIndex + i] =
                    result[srcIndex + i].replace(fileSeparator, '/');
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
        return restrict(ds.getIncludedFiles(), baseDir);
    }

    /**
     * Return the list of Directories from this DirectoryScanner that
     * should be included on the command line.
     */
    protected String[] getDirs(File baseDir, DirectoryScanner ds) {
        return restrict(ds.getIncludedDirectories(), baseDir);
    }

    /**
     * Return the list of files or directories from this FileList that
     * should be included on the command line.
     *
     * @since Ant 1.6.2
     */
    protected String[] getFilesAndDirs(FileList list) {
        return restrict(list.getFiles(getProject()), list.getDir(getProject()));
    }

    private String[] restrict(String[] s, File baseDir) {
        return (mapper == null || force) ? s
            : new SourceFileScanner(this).restrict(s, baseDir, destDir, mapper);
    }

    /**
     * Runs the command in "parallel" mode, making sure that at most
     * maxParallel sourcefiles get passed on the command line.
     *
     * @since Ant 1.6
     */
    protected void runParallel(Execute exe, Vector fileNames,
                               Vector baseDirs)
        throws IOException, BuildException {
        String[] s = new String[fileNames.size()];
        fileNames.copyInto(s);
        File[] b = new File[baseDirs.size()];
        baseDirs.copyInto(b);

        if (maxParallel <= 0
            || s.length == 0 /* this is skipEmpty == false */) {
            String[] command = getCommandline(s, b);
            log(Commandline.describeCommand(command), Project.MSG_VERBOSE);
            exe.setCommandline(command);
            runExecute(exe);
        } else {
            int stillToDo = fileNames.size();
            int currentOffset = 0;
            while (stillToDo > 0) {
                int currentAmount = Math.min(stillToDo, maxParallel);
                String[] cs = new String[currentAmount];
                System.arraycopy(s, currentOffset, cs, 0, currentAmount);
                File[] cb = new File[currentAmount];
                System.arraycopy(b, currentOffset, cb, 0, currentAmount);
                String[] command = getCommandline(cs, cb);
                log(Commandline.describeCommand(command), Project.MSG_VERBOSE);
                exe.setCommandline(command);
                if (redirectorElement != null) {
                    setupRedirector();
                    redirectorElement.configure(redirector, null);
                }

                if (redirectorElement != null || currentOffset > 0) {
                    // need to reset the stream handler to restart
                    // reading of pipes;
                    // go ahead and do it always w/ nested redirectors
                    exe.setStreamHandler(redirector.createHandler());
                }
                runExecute(exe);

                stillToDo -= currentAmount;
                currentOffset += currentAmount;
            }
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
