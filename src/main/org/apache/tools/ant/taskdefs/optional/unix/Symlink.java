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

/*
 * Since the initial version of this file was developed on the clock on
 * an NSF grant I should say the following boilerplate:
 *
 * This material is based upon work supported by the National Science
 * Foundaton under Grant No. EIA-0196404. Any opinions, findings, and
 * conclusions or recommendations expressed in this material are those
 * of the author and do not necessarily reflect the views of the
 * National Science Foundation.
 */

package org.apache.tools.ant.taskdefs.optional.unix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.dispatch.DispatchTask;
import org.apache.tools.ant.dispatch.DispatchUtils;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.SymbolicLinkUtils;

/**
 * Creates, Deletes, Records and Restores Symlinks.
 *
 * <p> This task performs several related operations. In the most trivial
 * and default usage, it creates a link specified in the link attribute to
 * a resource specified in the resource attribute. The second usage of this
 * task is to traverse a directory structure specified by a fileset,
 * and write a properties file in each included directory describing the
 * links found in that directory. The third usage is to traverse a
 * directory structure specified by a fileset, looking for properties files
 * (also specified as included in the fileset) and recreate the links
 * that have been previously recorded for each directory. Finally, it can be
 * used to remove a symlink without deleting the associated resource.
 *
 * <p> Usage examples:
 *
 * <p> Make a link named &quot;foo&quot; to a resource named
 * &quot;bar.foo&quot; in subdir:
 * <pre>
 * &lt;symlink link=&quot;${dir.top}/foo&quot; resource=&quot;${dir.top}/subdir/bar.foo&quot;/&gt;
 * </pre>
 *
 * <p> Record all links in subdir and its descendants in files named
 * &quot;dir.links&quot;:
 * <pre>
 * &lt;symlink action=&quot;record&quot; linkfilename=&quot;dir.links&quot;&gt;
 *    &lt;fileset dir=&quot;${dir.top}&quot; includes=&quot;subdir&#47;**&quot; /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p> Recreate the links recorded in the previous example:
 * <pre>
 * &lt;symlink action=&quot;recreate&quot;&gt;
 *    &lt;fileset dir=&quot;${dir.top}&quot; includes=&quot;subdir&#47;**&#47;dir.links&quot; /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p> Delete a link named &quot;foo&quot; to a resource named
 * &quot;bar.foo&quot; in subdir:
 * <pre>
 * &lt;symlink action=&quot;delete&quot; link=&quot;${dir.top}/foo&quot;/&gt;
 * </pre>
 *
 * <p><strong>LIMITATIONS:</strong> Because Java has no direct support for
 * handling symlinks this task divines them by comparing canonical and
 * absolute paths. On non-unix systems this may cause false positives.
 * Furthermore, any operating system on which the command
 * <code>ln -s link resource</code> is not a valid command on the command line
 * will not be able to use action=&quot;delete&quot;, action=&quot;single&quot;
 * or action=&quot;recreate&quot;, but action=&quot;record&quot; should still
 * work. Finally, the lack of support for symlinks in Java means that all links
 * are recorded as links to the <strong>canonical</strong> resource name.
 * Therefore the link: <code>link --> subdir/dir/../foo.bar</code> will be
 * recorded as <code>link=subdir/foo.bar</code> and restored as
 * <code>link --> subdir/foo.bar</code>.
 *
 */
public class Symlink extends DispatchTask {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final SymbolicLinkUtils SYMLINK_UTILS =
        SymbolicLinkUtils.getSymbolicLinkUtils();

    private String resource;
    private String link;
    private List<FileSet> fileSets = new ArrayList<>();
    private String linkFileName;
    private boolean overwrite;
    private boolean failonerror;
    private boolean executing = false;

    /**
     * Initialize the task.
     * @throws BuildException on error.
     */
    @Override
    public void init() throws BuildException {
        super.init();
        setDefaults();
    }

    /**
     * The standard method for executing any task.
     * @throws BuildException on error.
     */
    @Override
    public synchronized void execute() throws BuildException {
        if (executing) {
            throw new BuildException(
                "Infinite recursion detected in Symlink.execute()");
        }
        try {
            executing = true;
            DispatchUtils.execute(this);
        } finally {
            executing = false;
        }
    }

    /**
     * Create a symlink.
     * @throws BuildException on error.
     * @since Ant 1.7
     */
    public void single() throws BuildException {
        try {
            if (resource == null) {
                handleError("Must define the resource to symlink to!");
                return;
            }
            if (link == null) {
                handleError("Must define the link name for symlink!");
                return;
            }
            doLink(resource, link);
        } finally {
            setDefaults();
        }
    }

    /**
     * Delete a symlink.
     * @throws BuildException on error.
     * @since Ant 1.7
     */
    public void delete() throws BuildException {
        try {
            if (link == null) {
                handleError("Must define the link name for symlink!");
                return;
            }
            log("Removing symlink: " + link);
            SYMLINK_UTILS.deleteSymbolicLink(FILE_UTILS
                                             .resolveFile(new File("."), link),
                                             this);
        } catch (IOException ioe) {
            handleError(ioe.toString());
        } finally {
            setDefaults();
        }
    }

    /**
     * Restore symlinks.
     * @throws BuildException on error.
     * @since Ant 1.7
     */
    public void recreate() throws BuildException {
        try {
            if (fileSets.isEmpty()) {
                handleError(
                    "File set identifying link file(s) required for action recreate");
                return;
            }
            Properties links = loadLinks(fileSets);

            for (String lnk : links.stringPropertyNames()) {
                String res = links.getProperty(lnk);
                // handle the case where lnk points to a directory (bug 25181)
                try {
                    File test = new File(lnk);
                    if (!SYMLINK_UTILS.isSymbolicLink(lnk)) {
                        doLink(res, lnk);
                    } else if (!test.getCanonicalPath().equals(
                        new File(res).getCanonicalPath())) {
                        SYMLINK_UTILS.deleteSymbolicLink(test, this);
                        doLink(res, lnk);
                    } // else lnk exists, do nothing
                } catch (IOException ioe) {
                    handleError("IO exception while creating link");
                }
            }
        } finally {
            setDefaults();
        }
    }

    /**
     * Record symlinks.
     * @throws BuildException on error.
     * @since Ant 1.7
     */
    public void record() throws BuildException {
        try {
            if (fileSets.isEmpty()) {
                handleError("Fileset identifying links to record required");
                return;
            }
            if (linkFileName == null) {
                handleError("Name of file to record links in required");
                return;
            }
            // create a map to group them by parent directory:
            Map<File, List<File>> byDir = new HashMap<>();

            // get an Iterator of file objects representing links (canonical):
            findLinks(fileSets).forEach(lnk -> byDir
                .computeIfAbsent(lnk.getParentFile(), k -> new ArrayList<>())
                .add(lnk));

            // write a Properties file in each directory:
            byDir.forEach((dir, linksInDir) -> {
                Properties linksToStore = new Properties();

                // fill up a Properties object with link and resource names:
                for (File lnk : linksInDir) {
                    try {
                        linksToStore.put(lnk.getName(), lnk.getCanonicalPath());
                    } catch (IOException ioe) {
                        handleError("Couldn't get canonical name of parent link");
                    }
                }
                writePropertyFile(linksToStore, dir);
            });
        } finally {
            setDefaults();
        }
    }

    /**
     * Return all variables to their default state for the next invocation.
     * @since Ant 1.7
     */
    private void setDefaults() {
        resource = null;
        link = null;
        linkFileName = null;
        failonerror = true;   // default behavior is to fail on an error
        overwrite = false;    // default behavior is to not overwrite
        setAction("single");      // default behavior is make a single link
        fileSets.clear();
    }

    /**
     * Set overwrite mode. If set to false (default)
     * the task will not overwrite existing links, and may stop the build
     * if a link already exists depending on the setting of failonerror.
     *
     * @param owrite If true overwrite existing links.
     */
    public void setOverwrite(boolean owrite) {
        this.overwrite = owrite;
    }

    /**
     * Set failonerror mode. If set to true (default) the entire build fails
     * upon error; otherwise the error is logged and the build will continue.
     *
     * @param foe    If true throw BuildException on error, else log it.
     */
    public void setFailOnError(boolean foe) {
        this.failonerror = foe;
    }

    /**
     * Set the action to be performed.  May be &quot;single&quot;,
     * &quot;delete&quot;, &quot;recreate&quot; or &quot;record&quot;.
     *
     * @param action    The action to perform.
     */
    @Override
    public void setAction(String action) {
        super.setAction(action);
    }

    /**
     * Set the name of the link. Used when action = &quot;single&quot;.
     *
     * @param lnk     The name for the link.
     */
    public void setLink(String lnk) {
        this.link = lnk;
    }

    /**
     * Set the name of the resource to which a link should be created.
     * Used when action = &quot;single&quot;.
     *
     * @param src      The resource to be linked.
     */
    public void setResource(String src) {
        this.resource = src;
    }

    /**
     * Set the name of the file to which links will be written.
     * Used when action = &quot;record&quot;.
     *
     * @param lf      The name of the file to write links to.
     */
    public void setLinkfilename(String lf) {
        this.linkFileName = lf;
    }

    /**
     * Add a fileset to this task.
     *
     * @param set      The fileset to add.
     */
    public void addFileset(FileSet set) {
        fileSets.add(set);
    }

    /**
     * Delete a symlink (without deleting the associated resource).
     *
     * <p>This is a convenience method that simply invokes
     * <code>deleteSymlink(java.io.File)</code>.
     *
     * @param path    A string containing the path of the symlink to delete.
     *
     * @throws IOException             If calls to <code>File.rename</code>
     *                                   or <code>File.delete</code> fail.
     * @deprecated use
     * org.apache.tools.ant.util.SymbolicLinkUtils#deleteSymbolicLink
     * instead
     */
    @Deprecated
    public static void deleteSymlink(String path)
        throws IOException {
        SYMLINK_UTILS.deleteSymbolicLink(new File(path), null);
    }

    /**
     * Delete a symlink (without deleting the associated resource).
     *
     * <p>This is a utility method that removes a unix symlink without removing
     * the resource that the symlink points to. If it is accidentally invoked
     * on a real file, the real file will not be harmed.</p>
     *
     * <p>This method works by
     * getting the canonical path of the link, using the canonical path to
     * rename the resource (breaking the link) and then deleting the link.
     * The resource is then returned to its original name inside a finally
     * block to ensure that the resource is unharmed even in the event of
     * an exception.</p>
     *
     * <p>Since Ant 1.8.0 this method will try to delete the File object if
     * it reports it wouldn't exist (as symlinks pointing nowhere usually do).
     * Prior version would throw a FileNotFoundException in that case.</p>
     *
     * @param linkfil    A <code>File</code> object of the symlink to delete.
     *
     * @throws IOException             If calls to <code>File.rename</code>,
     *                                   <code>File.delete</code> or
     *                                   <code>File.getCanonicalPath</code>
     *                                   fail.
     * @deprecated use
     * org.apache.tools.ant.util.SymbolicLinkUtils#deleteSymbolicLink
     * instead
     */
    @Deprecated
    public static void deleteSymlink(File linkfil)
        throws IOException {
        SYMLINK_UTILS.deleteSymbolicLink(linkfil, null);
    }

    /**
     * Write a properties file. This method uses <code>Properties.store</code>
     * and thus may throw exceptions that occur while writing the file.
     *
     * @param properties     The properties object to be written.
     * @param dir            The directory for which we are writing the links.
     * @throws BuildException if the property file could not be written
     */
    private void writePropertyFile(Properties properties, File dir)
        throws BuildException {
        try (BufferedOutputStream bos = new BufferedOutputStream(
            Files.newOutputStream(new File(dir, linkFileName).toPath()))) {
            properties.store(bos, "Symlinks from " + dir);
        } catch (IOException ioe) {
            throw new BuildException(ioe, getLocation());
        }
    }

    /**
     * Handle errors based on the setting of failonerror.
     *
     * @param msg    The message to log, or include in the
     *                  <code>BuildException</code>.
     * @throws BuildException with the message if failonerror=true
     */
    private void handleError(String msg) {
        if (failonerror) {
            throw new BuildException(msg);
        }
        log(msg);
    }

    /**
     * Conduct the actual construction of a link.
     *
     * <p> The link is constructed by calling <code>Execute.runCommand</code>.
     *
     * @param res   The path of the resource we are linking to.
     * @param lnk       The name of the link we wish to make.
     * @throws BuildException when things go wrong
     */
    private void doLink(String res, String lnk) throws BuildException {
        File linkfil = new File(lnk);
        String options = "-s";
        if (overwrite) {
            options += "f";
            if (linkfil.exists()) {
                try {
                    SYMLINK_UTILS.deleteSymbolicLink(linkfil, this);
                } catch (FileNotFoundException fnfe) {
                    log("Symlink disappeared before it was deleted: " + lnk);
                } catch (IOException ioe) {
                    log("Unable to overwrite preexisting link or file: " + lnk,
                        ioe, Project.MSG_INFO);
                }
            }
        }
        try {
            Execute.runCommand(this, "ln", options, res, lnk);
        } catch (BuildException failedToExecute) {
            if (failonerror) {
                throw failedToExecute;
            }
            //log at the info level, and keep going.
            log(failedToExecute.getMessage(), failedToExecute, Project.MSG_INFO);
        }
    }

    /**
     * Find all the links in all supplied filesets.
     *
     * <p> This method is invoked when the action attribute is
     * &quot;record&quot;. This means that filesets are interpreted
     * as the directories in which links may be found.
     *
     * @param fileSets   The filesets specified by the user.
     * @return A HashSet of <code>File</code> objects containing the
     *         links (with canonical parent directories).
     */
    private Set<File> findLinks(List<FileSet> fileSets) {
        Set<File> result = new HashSet<>();
        for (FileSet fs : fileSets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            File dir = fs.getDir(getProject());

            Stream.of(ds.getIncludedFiles(), ds.getIncludedDirectories())
                .flatMap(Stream::of).forEach(path -> {
                    try {
                        File f = new File(dir, path);
                        File pf = f.getParentFile();
                        String name = f.getName();
                        if (SYMLINK_UTILS.isSymbolicLink(pf, name)) {
                            result.add(new File(pf.getCanonicalFile(), name));
                        }
                    } catch (IOException e) {
                        handleError("IOException: " + path + " omitted");
                    }
                });
        }
        return result;
    }

    /**
     * Load links from properties files included in one or more FileSets.
     *
     * <p> This method is only invoked when the action attribute is set to
     * &quot;recreate&quot;. The filesets passed in are assumed to specify the
     * names of the property files with the link information and the
     * subdirectories in which to look for them.
     *
     * @param fileSets    The <code>FileSet</code>s for this task.
     * @return            The links to be made.
     */
    private Properties loadLinks(List<FileSet> fileSets) {
        Properties finalList = new Properties();
        // loop through the supplied file sets:
        for (FileSet fs : fileSets) {
            DirectoryScanner ds = new DirectoryScanner();
            fs.setupDirectoryScanner(ds, getProject());
            ds.setFollowSymlinks(false);
            ds.scan();
            File dir = fs.getDir(getProject());

            // load included files as properties files:
            for (String name : ds.getIncludedFiles()) {
                File inc = new File(dir, name);
                File pf = inc.getParentFile();
                Properties lnks = new Properties();
                try (InputStream is = new BufferedInputStream(
                    Files.newInputStream(inc.toPath()))) {
                    lnks.load(is);
                    pf = pf.getCanonicalFile();
                } catch (FileNotFoundException fnfe) {
                    handleError("Unable to find " + name + "; skipping it.");
                    continue;
                } catch (IOException ioe) {
                    handleError("Unable to open " + name
                        + " or its parent dir; skipping it.");
                    continue;
                }
                lnks.list(new PrintStream(
                    new LogOutputStream(this, Project.MSG_INFO)));
                // Write the contents to our master list of links
                // This method assumes that all links are defined in
                // terms of absolute paths, or paths relative to the
                // working directory:
                for (String key : lnks.stringPropertyNames()) {
                    finalList.put(new File(pf, key).getAbsolutePath(),
                        lnks.getProperty(key));
                }
            }
        }
        return finalList;
    }
}
