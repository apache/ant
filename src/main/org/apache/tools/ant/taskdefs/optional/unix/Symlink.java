/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * Foundation under Grant No. EIA-0196404. Any opinions, findings, and
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.FileSet;

/**
 * Creates, Deletes, Records and Restores Symlinks.
 *
 * <p>This task performs several related operations. In the most trivial
 * and default usage, it creates a link specified in the link attribute to
 * a resource specified in the resource attribute. The second usage of this
 * task is to traverse a directory structure specified by a fileset,
 * and write a properties file in each included directory describing the
 * links found in that directory. The third usage is to traverse a
 * directory structure specified by a fileset, looking for properties files
 * (also specified as included in the fileset) and recreate the links
 * that have been previously recorded for each directory. Finally, it can be
 * used to remove a symlink without deleting the associated resource.</p>
 *
 * <p>Usage examples:</p>
 *
 * <p>Make a link named &quot;foo&quot; to a resource named
 * &quot;bar.foo&quot; in subdir:</p>
 * <pre>
 * &lt;symlink link=&quot;${dir.top}/foo&quot; resource=&quot;${dir.top}/subdir/bar.foo&quot;/&gt;
 * </pre>
 *
 * <p>Record all links in subdir and its descendants in files named
 * &quot;dir.links&quot;:</p>
 * <pre>
 * &lt;symlink action=&quot;record&quot; linkfilename=&quot;dir.links&quot;&gt;
 *    &lt;fileset dir=&quot;${dir.top}&quot; includes=&quot;subdir&#47;**&quot; /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p>Recreate the links recorded in the previous example:</p>
 * <pre>
 * &lt;symlink action=&quot;recreate&quot;&gt;
 *    &lt;fileset dir=&quot;${dir.top}&quot; includes=&quot;subdir&#47;**&#47;dir.links&quot; /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p>Delete a link named &quot;foo&quot; to a resource named
 * &quot;bar.foo&quot; in subdir:</p>
 * <pre>
 * &lt;symlink action=&quot;delete&quot; link=&quot;${dir.top}/foo&quot;/&gt;
 * </pre>
 *
 * <p><strong>Note:</strong> Starting Ant version 1.10.2, this task relies on the symbolic link support
 * introduced in Java 7 through the {@link Files} APIs.
 */
public class Symlink extends DispatchTask {

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
            final Path linkPath = Paths.get(link);
            if (!Files.isSymbolicLink(linkPath)) {
                log("Skipping deletion of " + linkPath + " since it's not a symlink", Project.MSG_VERBOSE);
                // just ignore and silently return (this is consistent
                // with the current, 1.9.x versions, of Ant)
                return;

            }
            log("Removing symlink: " + link);
            deleteSymLink(linkPath);
        } catch (IOException ioe) {
            handleError(ioe.getMessage());
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
            final Properties links = loadLinks(fileSets);
            for (final String link : links.stringPropertyNames()) {
                final String resource = links.getProperty(link);
                try {
                    if (Files.isSymbolicLink(Paths.get(link)) &&
                            new File(link).getCanonicalPath().equals(new File(resource).getCanonicalPath())) {
                        // it's already a symlink and the symlink target is the same
                        // as the target noted in the properties file. So there's no
                        // need to recreate it
                        log("not recreating " + link + " as it points to the correct target already",
                            Project.MSG_DEBUG);
                        continue;
                    }
                } catch (IOException e) {
                    final String errMessage = "Failed to check if path " + link + " is a symbolic link, linking to " + resource;
                    if (failonerror) {
                        throw new BuildException(errMessage, e);
                    }
                    // log and continue
                    log(errMessage, Project.MSG_INFO);
                    continue;
                }
                // create the link
                this.doLink(resource, link);
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
            findLinks(fileSets).forEach(link -> byDir
                .computeIfAbsent(link.getParentFile(), k -> new ArrayList<>())
                .add(link));

            // write a Properties file in each directory:
            byDir.forEach((dir, linksInDir) -> {
                Properties linksToStore = new Properties();

                // fill up a Properties object with link and resource names:
                for (File link : linksInDir) {
                    try {
                        linksToStore.put(link.getName(), link.getCanonicalPath());
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
     * @param link     The name for the link.
     */
    public void setLink(String link) {
        this.link = link;
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
     * <p>This is a convenience method that simply invokes {@link #deleteSymlink(File)}</p>
     *
     * @param path A string containing the path of the symlink to delete.
     * @throws IOException If the deletion attempt fails
     *
     * @deprecated use {@link Files#delete(Path)} instead
     */
    @Deprecated
    public static void deleteSymlink(final String path)
            throws IOException {
        deleteSymlink(Paths.get(path).toFile());
    }

    /**
     * Delete a symlink (without deleting the associated resource).
     *
     * <p>This is a utility method that removes a symlink without removing
     * the resource that the symlink points to. If it is accidentally invoked
     * on a real file, the real file will not be harmed and instead this method
     * returns silently.</p>
     *
     * <p>Since Ant 1.10.2 this method relies on the {@link Files#isSymbolicLink(Path)}
     * and {@link Files#delete(Path)} to check and delete the symlink
     * </p>
     *
     * @param linkfil A <code>File</code> object of the symlink to delete. Cannot be null.
     * @throws IOException If the attempt to delete runs into exception
     *
     * @deprecated use {@link Files#delete(Path)} instead
     */
    @Deprecated
    public static void deleteSymlink(final File linkfil)
            throws IOException {
        if (!Files.isSymbolicLink(linkfil.toPath())) {
            return;
        }
        deleteSymLink(linkfil.toPath());
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
     * @param resource The path of the resource we are linking to.
     * @param link The name of the link we wish to make.
     * @throws BuildException when things go wrong
     */
    private void doLink(String resource, String link) throws BuildException {
        final Path linkPath = Paths.get(link);
        final Path target = Paths.get(resource);
        final boolean alreadyExists = Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS);
        if (!alreadyExists) {
            // if the path (at which the link is expected to be created) isn't already present
            // then we just go ahead and attempt to symlink
            try {
                log("creating symlink " + linkPath + " -> " + target, Project.MSG_DEBUG);
                Files.createSymbolicLink(linkPath, target);
            } catch (IOException e) {
                if (failonerror) {
                    throw new BuildException("Failed to create symlink " + link + " to target " + resource, e);
                }
                log("Unable to create symlink " + link + " to target " + resource, e, Project.MSG_INFO);
            }
            return;
        }
        // file already exists, see if we are allowed to overwrite
        if (!overwrite) {
            log("Skipping symlink creation, since file at " + link + " already exists and overwrite is set to false", Project.MSG_INFO);
            return;
        }
        // we have been asked to overwrite, so we now do the necessary steps

        // initiate a deletion of the existing file
        final boolean existingFileDeleted = linkPath.toFile().delete();
        if (!existingFileDeleted) {
            handleError("Deletion of file at " + link + " failed, while trying to overwrite it with a symlink");
            return;
        }
        try {
            log("creating symlink " + linkPath + " -> " + target + " after removing original",
                Project.MSG_DEBUG);
            Files.createSymbolicLink(linkPath, target);
        } catch (IOException e) {
            if (failonerror) {
                throw new BuildException("Failed to create symlink " + link + " to target " + resource, e);
            }
            log("Unable to create symlink " + link + " to target " + resource, e, Project.MSG_INFO);
        }
    }

    /**
     * Find all the links in all supplied filesets.
     *
     * <p>This method is invoked when the action attribute is
     * &quot;record&quot;. This means that filesets are interpreted
     * as the directories in which links may be found.</p>
     *
     * @param fileSets The filesets specified by the user.
     * @return A Set of <code>File</code> objects containing the
     * links (with canonical parent directories).
     */
    private Set<File> findLinks(List<FileSet> fileSets) {
        final Set<File> result = new HashSet<>();
        for (FileSet fs : fileSets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            File dir = fs.getDir(getProject());

            Stream.of(ds.getIncludedFiles(), ds.getIncludedDirectories())
                    .flatMap(Stream::of).forEach(path -> {
                        try {
                            final File f = new File(dir, path);
                            final File pf = f.getParentFile();
                            final String name = f.getName();
                            // we use the canonical path of the parent dir in which the (potential)
                            // link resides
                            final File parentDirCanonicalizedFile = new File(pf.getCanonicalPath(), name);
                            if (Files.isSymbolicLink(parentDirCanonicalizedFile.toPath())) {
                                result.add(parentDirCanonicalizedFile);
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
     * <p>This method is only invoked when the action attribute is set to
     * &quot;recreate&quot;. The filesets passed in are assumed to specify the
     * names of the property files with the link information and the
     * subdirectories in which to look for them.</p>
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
                Properties links = new Properties();
                try (InputStream is = new BufferedInputStream(
                    Files.newInputStream(inc.toPath()))) {
                    links.load(is);
                    pf = pf.getCanonicalFile();
                } catch (FileNotFoundException fnfe) {
                    handleError("Unable to find " + name + "; skipping it.");
                    continue;
                } catch (IOException ioe) {
                    handleError("Unable to open " + name
                        + " or its parent dir; skipping it.");
                    continue;
                }
                try {
                    links.store(new PrintStream(
                        new LogOutputStream(this, Project.MSG_INFO)),
                        "listing properties");
                } catch (IOException ex) {
                    log("failed to log unshortened properties");
                    links.list(new PrintStream(
                        new LogOutputStream(this, Project.MSG_INFO)));
                }
                // Write the contents to our master list of links
                // This method assumes that all links are defined in
                // terms of absolute paths, or paths relative to the
                // working directory:
                for (String key : links.stringPropertyNames()) {
                    finalList.put(new File(pf, key).getAbsolutePath(),
                        links.getProperty(key));
                }
            }
        }
        return finalList;
    }

    private static void deleteSymLink(final Path path) throws IOException {
        // Implementation note: We intentionally use java.io.File#delete() instead of
        // java.nio.file.Files#delete(Path) since it turns out that the latter doesn't
        // update/clear the "canonical file paths cache" maintained by the JRE FileSystemProvider.
        // Not clearing/updating that cache results in this deleted (and later recreated) symlink
        // to point to a wrong/outdated target for a few seconds (30 seconds is the time the JRE
        // maintains the cache entries for). All this is implementation detail of the JRE and
        // is a JRE bug https://mail.openjdk.java.net/pipermail/core-libs-dev/2017-December/050540.html,
        // but given that it affects our tests (SymlinkTest#testRecreate consistently fails
        // on MacOS/Unix) as well as the Symlink task, it makes sense to use this API instead of
        // the Files#delete(Path) API
        final boolean deleted = path.toFile().delete();
        if (!deleted) {
            throw new IOException("Could not delete symlink at " + path);
        }
    }
}
