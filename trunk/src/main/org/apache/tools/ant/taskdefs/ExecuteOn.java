/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import java.util.Iterator;
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
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
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

    // CheckStyle:VisibilityModifier OFF - bc

    // filesets has been protected so we need to keep that even after
    // switching to resource collections.  In fact, they will still
    // get a different treatment form the other resource collections
    // even in execute since we have some subtle special features like
    // switching type to "dir" when we encounter a DirSet that would
    // be more difficult to achieve otherwise.

    protected Vector filesets = new Vector(); // contains AbstractFileSet
                                              // (both DirSet and FileSet)
    private Union resources = null;
    private boolean relative = false;
    private boolean parallel = false;
    private boolean forwardSlash = false;
    protected String type = FileDirBoth.FILE;
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

    // CheckStyle:VisibilityModifier ON
    /**
     * Add a set of files upon which to operate.
     * @param set the FileSet to add.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Add a set of directories upon which to operate.
     *
     * @param  set the DirSet to add.
     *
     * @since Ant 1.6
     */
    public void addDirset(DirSet set) {
        filesets.addElement(set);
    }

    /**
     * Add a list of source files upon which to operate.
     * @param list the FileList to add.
     */
    public void addFilelist(FileList list) {
        add(list);
    }

    /**
     * Add a collection of resources upon which to operate.
     * @param rc resource collection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        if (resources == null) {
            resources = new Union();
        }
        resources.add(rc);
    }

    /**
     * Set whether the filenames should be passed on the command line as
     * absolute or relative pathnames. Paths are relative to the base
     * directory of the corresponding fileset for source files or the
     * dest attribute for target files.
     * @param relative whether to pass relative pathnames.
     */
    public void setRelative(boolean relative) {
        this.relative = relative;
    }


    /**
     * Set whether to execute in parallel mode.
     * If true, run the command only once, appending all files as arguments.
     * If false, command will be executed once for every file. Defaults to false.
     * @param parallel whether to run in parallel.
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Set whether the command works only on files, directories or both.
     * @param type a FileDirBoth EnumeratedAttribute.
     */
    public void setType(FileDirBoth type) {
        this.type = type.getValue();
    }

    /**
     * Set whether empty filesets will be skipped.  If true and
     * no source files have been found or are newer than their
     * corresponding target files, the command will not be run.
     * @param skip whether to skip empty filesets.
     */
    public void setSkipEmptyFilesets(boolean skip) {
        skipEmpty = skip;
    }

    /**
     * Specify the directory where target files are to be placed.
     * @param destDir the File object representing the destination directory.
     */
    public void setDest(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set whether the source and target file names on Windows and OS/2
     * must use the forward slash as file separator.
     * @param forwardSlash whether the forward slash will be forced.
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
     * @param max <code>int</code> maximum number of sourcefiles
     *            passed to the executable.
     *
     * @since Ant 1.6
     */
    public void setMaxParallel(int max) {
        maxParallel = max;
    }

    /**
     * Set whether to send the source file name on the command line.
     *
     * <p>Defaults to <code>true</code>.
     *
     * @param b whether to add the source file to the command line.
     *
     * @since Ant 1.6
     */
    public void setAddsourcefile(boolean b) {
        addSourceFile = b;
    }

    /**
     * Set whether to operate in verbose mode.
     * If true, a verbose summary will be printed after execution.
     * @param b whether to operate in verbose mode.
     *
     * @since Ant 1.6
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }

    /**
     * Set whether to ignore nonexistent files from filelists.
     * @param b whether to ignore missing files.
     *
     * @since Ant 1.6.2
     */
    public void setIgnoremissing(boolean b) {
        ignoreMissing = b;
    }

    /**
     * Set whether to bypass timestamp comparisons for target files.
     * @param b whether to bypass timestamp comparisons.
     *
     * @since Ant 1.6.3
     */
    public void setForce(boolean b) {
        force = b;
    }

    /**
     * Create a placeholder indicating where on the command line
     * the name of the source file should be inserted.
     * @return <code>Commandline.Marker</code>.
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
     * Create a placeholder indicating where on the command line
     * the name of the target file should be inserted.
     * @return <code>Commandline.Marker</code>.
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
     * Create a nested Mapper element to use for mapping
     * source files to target files.
     * @return <code>Mapper</code>.
     * @throws BuildException if more than one mapper is defined.
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
     * Add a nested FileNameMapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Check the configuration of this ExecuteOn instance.
     */
    protected void checkConfiguration() {
//     * @TODO using taskName here is brittle, as a user could override it.
//     *       this should probably be modified to use the classname instead.
        if ("execon".equals(getTaskName())) {
            log("!! execon is deprecated. Use apply instead. !!");
        }
        super.checkConfiguration();
        if (filesets.size() == 0 && resources == null) {
            throw new BuildException("no resources specified",
                                     getLocation());
        }
        if (targetFilePos != null && mapperElement == null) {
            throw new BuildException("targetfile specified without mapper",
                                     getLocation());
        }
        if (destDir != null && mapperElement == null) {
            throw new BuildException("dest specified without mapper",
                                     getLocation());
        }
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        }
    }

    /**
     * Create the ExecuteStreamHandler instance that will be used
     * during execution.
     * @return <code>ExecuteStreamHandler</code>.
     * @throws BuildException on error.
     */
    protected ExecuteStreamHandler createHandler() throws BuildException {
        //if we have a RedirectorElement, return a decoy
        return (redirectorElement == null)
            ? super.createHandler() : new PumpStreamHandler();
    }

    /**
     * Set up the I/O Redirector.
     */
    protected void setupRedirector() {
        super.setupRedirector();
        redirector.setAppendProperties(true);
    }

    /**
     * Run the specified Execute object.
     * @param exe the Execute instance representing the external process.
     * @throws BuildException on error
     */
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
                    if (!FileDirBoth.DIR.equals(type)) {
                        log("Found a nested dirset but type is " + type + ". "
                            + "Temporarily switching to type=\"dir\" on the"
                            + " assumption that you really did mean"
                            + " <dirset> not <fileset>.", Project.MSG_DEBUG);
                        currentType = FileDirBoth.DIR;
                    }
                }
                File base = fs.getDir(getProject());

                DirectoryScanner ds = fs.getDirectoryScanner(getProject());

                if (!FileDirBoth.DIR.equals(currentType)) {
                    String[] s = getFiles(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalFiles++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }
                if (!FileDirBoth.FILE.equals(currentType)) {
                    String[] s = getDirs(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalDirs++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }
                if (fileNames.size() == 0 && skipEmpty) {
                    logSkippingFileset(currentType, ds, base);
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

            if (resources != null) {
                Iterator iter = resources.iterator();
                while (iter.hasNext()) {
                    Resource res = (Resource) iter.next();

                    if (!res.isExists() && ignoreMissing) {
                        continue;
                    }

                    File base = null;
                    String name = res.getName();
                    if (res instanceof FileResource) {
                        FileResource fr = (FileResource) res;
                        base = fr.getBaseDir();
                        if (base == null) {
                            name = fr.getFile().getAbsolutePath();
                        }
                    }

                    if (restrict(new String[] {name}, base).length == 0) {
                        continue;
                    }

                    if ((!res.isDirectory() || !res.isExists())
                        && !FileDirBoth.DIR.equals(type)) {
                        totalFiles++;
                    } else if (res.isDirectory()
                               && !FileDirBoth.FILE.equals(type)) {
                        totalDirs++;
                    } else {
                        continue;
                    }

                    baseDirs.add(base);
                    fileNames.add(name);

                    if (!parallel) {
                        String[] command = getCommandline(name, base);
                        log(Commandline.describeCommand(command),
                            Project.MSG_VERBOSE);
                        exe.setCommandline(command);

                        if (redirectorElement != null) {
                            setupRedirector();
                            redirectorElement.configure(redirector, name);
                        }
                        if (redirectorElement != null || haveExecuted) {
                            // need to reset the stream handler to restart
                            // reading of pipes;
                            // go ahead and do it always w/ nested redirectors
                            exe.setStreamHandler(redirector.createHandler());
                        }
                        runExecute(exe);
                        haveExecuted = true;
                        fileNames.removeAllElements();
                        baseDirs.removeAllElements();
                    }
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
     * log a message for skipping a fileset.
     * @param currentType the current type.
     * @param ds the directory scanner.
     * @param base the dir base
     */
    private void logSkippingFileset(
        String currentType, DirectoryScanner ds, File base) {
        int includedCount
            = ((!FileDirBoth.DIR.equals(currentType))
               ? ds.getIncludedFilesCount() : 0)
            + ((!FileDirBoth.FILE.equals(currentType))
               ? ds.getIncludedDirsCount() : 0);

        log("Skipping fileset for directory " + base + ". It is "
            + ((includedCount > 0) ? "up to date." : "empty."),
             verbose ? Project.MSG_INFO : Project.MSG_VERBOSE);
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline.
     * @param baseDirs filenames are relative to this dir.
     * @return the command line in the form of a String[].
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
     * @param srcFile The filename to add to the commandline.
     * @param baseDir filename is relative to this dir.
     * @return the command line in the form of a String[].
     */
    protected String[] getCommandline(String srcFile, File baseDir) {
        return getCommandline(new String[] {srcFile}, new File[] {baseDir});
    }

    /**
     * Return the list of files from this DirectoryScanner that should
     * be included on the command line.
     * @param baseDir the File base directory.
     * @param ds the DirectoryScanner to use for file scanning.
     * @return a String[] containing the filenames.
     */
    protected String[] getFiles(File baseDir, DirectoryScanner ds) {
        return restrict(ds.getIncludedFiles(), baseDir);
    }

    /**
     * Return the list of Directories from this DirectoryScanner that
     * should be included on the command line.
     * @param baseDir the File base directory.
     * @param ds the DirectoryScanner to use for file scanning.
     * @return a String[] containing the directory names.
     */
    protected String[] getDirs(File baseDir, DirectoryScanner ds) {
        return restrict(ds.getIncludedDirectories(), baseDir);
    }

    /**
     * Return the list of files or directories from this FileList that
     * should be included on the command line.
     * @param list the FileList to check.
     * @return a String[] containing the directory names.
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
     * Run the command in "parallel" mode, making sure that at most
     * maxParallel sourcefiles get passed on the command line.
     * @param exe the Executable to use.
     * @param fileNames the Vector of filenames.
     * @param baseDirs the Vector of base directories corresponding to fileNames.
     * @throws IOException  on I/O errors.
     * @throws BuildException on other errors.
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
        /** File value */
        public static final String FILE = "file";
        /** Dir value */
        public static final String DIR = "dir";
        /**
         * @see EnumeratedAttribute#getValues
         */
        /** {@inheritDoc}. */
       public String[] getValues() {
            return new String[] {FILE, DIR, "both"};
        }
    }
}
