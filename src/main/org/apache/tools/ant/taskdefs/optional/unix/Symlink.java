/*
 * Copyright  2002-2004 The Apache Software Foundation
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

/*
 * Since the initial version of this file was deveolped on the clock on
 * an NSF grant I should say the following boilerplate:
 *
 * This material is based upon work supported by the National Science
 * Foundaton under Grant No. EIA-0196404. Any opinions, findings, and
 * conclusions or recommendations expressed in this material are those
 * of the author and do not necessarily reflect the views of the
 * National Science Foundation.
 */

package org.apache.tools.ant.taskdefs.optional.unix;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

import org.apache.tools.ant.util.FileUtils;

import org.apache.tools.ant.types.FileSet;

import org.apache.tools.ant.taskdefs.Execute;

/**
 * Creates, Records and Restores Symlinks.
 *
 * <p> This task performs several related operations. In the most trivial,
 * and default usage, it creates a link specified in the link atribute to
 * a resource specified in the resource atribute. The second usage of this
 * task is to traverses a directory structure specified by a fileset,
 * and write a properties file in each included directory describing the
 * links found in that directory. The third usage is to traverse a
 * directory structure specified by a fileset, looking for properties files
 * (also specified as included in the fileset) and recreate the links
 * that have been previously recorded for each directory. Finally, it can be
 * used to remove a symlink without deleting the file or directory it points
 * to.
 *
 * <p> Examples of use:
 *
 * <p> Make a link named "foo" to a resource named "bar.foo" in subdir:
 * <pre>
 * &lt;symlink link="${dir.top}/foo" resource="${dir.top}/subdir/bar.foo"/&gt;
 * </pre>
 *
 * <p> Record all links in subdir and it's descendants in files named
 * "dir.links"
 * <pre>
 * &lt;symlink action="record" linkfilename="dir.links"&gt;
 *    &lt;fileset dir="${dir.top}" includes="subdir&#47;**" /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p> Recreate the links recorded in the previous example:
 * <pre>
 * &lt;symlink action="recreate"&gt;
 *    &lt;fileset dir="${dir.top}" includes="subdir&#47;**&#47;dir.links" /&gt;
 * &lt;/symlink&gt;
 * </pre>
 *
 * <p> Delete a link named "foo" to a resource named "bar.foo" in subdir:
 * <pre>
 * &lt;symlink action="delete" link="${dir.top}/foo"/&gt;
 * </pre>
 *
 * <p><strong>LIMITATIONS:</strong> Because Java has no direct support for
 * handling symlinks this task divines them by comparing canoniacal and
 * absolute paths. On non-unix systems this may cause false positives.
 * Furthermore, any operating system on which the command
 * <code>ln -s link resource</code> is not a valid command on the comandline
 * will not be able to use action= "delete", action="single" or
 * action="recreate", but action="record" should still work. Finally, the
 * lack of support for symlinks in Java means that all links are recorded
 * as links to the <strong>canonical</strong> resource name. Therefore
 * the link: <code>link --> subdir/dir/../foo.bar</code> will be recorded
 * as <code>link=subdir/foo.bar</code> and restored as
 * <code>link --> subdir/foo.bar</code>
 *
 * @version $Revision$
 */

public class Symlink extends Task {

    // Atributes with setter methods
    private String resource;
    private String link;
    private String action;
    private Vector fileSets = new Vector();
    private String linkFileName;
    private boolean overwrite;
    private boolean failonerror;

    /** Initialize the task. */

    public void init() throws BuildException {
        super.init();
        failonerror = true;   // default behavior is to fail on an error
        overwrite = false;    // devault behavior is to not overwrite
        action = "single";      // default behavior is make a single link
        fileSets = new Vector();
    }

    /** The standard method for executing any task. */

    public void execute() throws BuildException {
        try {
            if (action.equals("single")) {
                doLink(resource, link);
            } else if (action.equals("delete")) {
                try {
                    log("Removing symlink: " + link);
                    Symlink.deleteSymlink(link);
                } catch (FileNotFoundException fnfe) {
                    handleError(fnfe.toString());
                } catch (IOException ioe) {
                    handleError(ioe.toString());
                }
            } else if (action.equals("recreate")) {
                Properties listOfLinks;
                Enumeration keys;

                if (fileSets.size() == 0) {
                    handleError("File set identifying link file(s) "
                                + "required for action recreate");
                    return;
                }

                listOfLinks = loadLinks(fileSets);

                keys = listOfLinks.keys();

                while (keys.hasMoreElements()) {
                    link = (String) keys.nextElement();
                    resource = listOfLinks.getProperty(link);
                    // handle the case where the link exists
                    // and points to a directory (bug 25181)
                    try {
                        FileUtils fu = FileUtils.newFileUtils();
                        File test = new File(link);
                        File testRes = new File(resource);
                        if (!fu.isSymbolicLink(test.getParentFile(),
                                               test.getName())) {
                            doLink(resource, link);
                        } else {
                            if (!test.getCanonicalPath().
                                equals(testRes.getCanonicalPath())) {
                                Symlink.deleteSymlink(link);
                                doLink(resource, link);
                            } // else the link exists, do nothing
                        }
                    } catch (IOException ioe) {
                        handleError("IO exception while creating "
                                    + "link");
                    }
                }
            } else if (action.equals("record")) {
                Vector vectOfLinks;
                Hashtable byDir = new Hashtable();
                Enumeration links, dirs;

                if (fileSets.size() == 0) {
                    handleError("File set identifying links to "
                                + "record required");
                    return;
                }

                if (linkFileName == null) {
                    handleError("Name of file to record links in "
                                + "required");
                    return;
                }

                // fill our vector with file objects representing
                // links (canonical)
                vectOfLinks = findLinks(fileSets);

                // create a hashtable to group them by parent directory
                links = vectOfLinks.elements();
                while (links.hasMoreElements()) {
                    File thisLink = (File) links.nextElement();
                    String parent = thisLink.getParent();
                    if (byDir.containsKey(parent)) {
                        ((Vector) byDir.get(parent)).addElement(thisLink);
                    } else {
                        byDir.put(parent, new Vector());
                        ((Vector) byDir.get(parent)).addElement(thisLink);
                    }
                }

                // write a Properties file in each directory
                dirs = byDir.keys();
                while (dirs.hasMoreElements()) {
                    String dir = (String) dirs.nextElement();
                    Vector linksInDir = (Vector) byDir.get(dir);
                    Properties linksToStore = new Properties();
                    Enumeration eachlink = linksInDir.elements();
                    File writeTo;

                    // fill up a Properties object with link and resource
                    // names
                    while (eachlink.hasMoreElements()) {
                        File alink = (File) eachlink.nextElement();
                        try {
                            linksToStore.put(alink.getName(),
                                             alink.getCanonicalPath());
                        } catch (IOException ioe) {
                            handleError("Couldn't get canonical "
                                        + "name of a parent link");
                        }
                    }


                    // Get a place to record what we are about to write
                    writeTo = new File(dir + File.separator
                                       + linkFileName);

                    writePropertyFile(linksToStore, writeTo,
                                      "Symlinks from " + writeTo.getParent());
                }
            } else {
                handleError("Invalid action specified in symlink");
            }
        } finally {
            // return all variables to their default state,
            // ready for the next invocation.
            resource = null;
            link = null;
            action = "single";
            fileSets = new Vector();
            linkFileName = null;
            overwrite = false;
            failonerror = true;
        }
    }

    /* ********************************************************** *
      *              Begin Atribute Setter Methods               *
     * ********************************************************** */

    /**
     * The setter for the overwrite atribute. If set to false (default)
     * the task will not overwrite existing links, and may stop the build
     * if a link already exists depending on the setting of failonerror.
     *
     * @param owrite If true overwrite existing links.
     */
    public void setOverwrite(boolean owrite) {
        this.overwrite = owrite;
    }

    /**
     * The setter for the failonerror atribute. If set to true (default)
     * the entire build fails upon error. If set to false, the error is
     * logged and the build will continue.
     *
     * @param foe    If true throw build exception on error else log it.
     */
    public void setFailOnError(boolean foe) {
        this.failonerror = foe;
    }


    /**
     * The setter for the "action" attribute. May be "single" "multi"
     * or "record"
     *
     * @param typ    The action of action to perform
     */
    public void setAction(String typ) {
        this.action = typ;
    }

    /**
     * The setter for the "link" attribute. Only used for action = single.
     *
     * @param lnk     The name for the link
     */
    public void setLink(String lnk) {
        this.link = lnk;
    }

    /**
     * The setter for the "resource" attribute. Only used for action = single.
     *
     * @param src      The source of the resource to be linked.
     */
    public void setResource(String src) {
        this.resource = src;
    }

    /**
     * The setter for the "linkfilename" attribute. Only used for action=record.
     *
     * @param lf      The name of the file to write links to.
     */
    public void setLinkfilename(String lf) {
        this.linkFileName = lf;
    }

    /**
     * Adds a fileset to this task.
     *
     * @param set      The fileset to add.
     */
    public void addFileset(FileSet set) {
        fileSets.addElement(set);
    }

    /* ********************************************************** *
      *               Begin Public Utility Methods               *
     * ********************************************************** */

    /**
     * Deletes a symlink without deleteing the resource it points to.
     *
     * <p>This is a convenience method that simply invokes
     * <code>deleteSymlink(java.io.File)</code>
     *
     * @param path    A string containing the path of the symlink to delete
     *
     * @throws FileNotFoundException   When the path results in a
     *                                   <code>File</code> that doesn't exist.
     * @throws IOException             If calls to <code>File.rename</code>
     *                                   or <code>File.delete</code> fail.
     */

    public static void deleteSymlink(String path)
        throws IOException, FileNotFoundException {

        File linkfil = new File(path);
        deleteSymlink(linkfil);
    }

    /**
     * Deletes a symlink without deleteing the resource it points to.
     *
     * <p>This is a utility method that removes a unix symlink without removing
     * the resource that the symlink points to. If it is accidentally invoked
     * on a real file, the real file will not be harmed, but an exception
     * will be thrown when the deletion is attempted. This method works by
     * getting the canonical path of the link, using the canonical path to
     * rename the resource (breaking the link) and then deleting the link.
     * The resource is then returned to it's original name inside a finally
     * block to ensure that the resource is unharmed even in the event of
     * an exception.
     *
     * @param linkfil    A <code>File</code> object of the symlink to delete
     *
     * @throws FileNotFoundException   When the path results in a
     *                                   <code>File</code> that doesn't exist.
     * @throws IOException             If calls to <code>File.rename</code>,
     *                                   <code>File.delete</code> or
     *                                   <code>File.getCanonicalPath</code>
     *                                   fail.
     */

    public static void deleteSymlink(File linkfil)
        throws IOException, FileNotFoundException {

        if (!linkfil.exists()) {
            throw new FileNotFoundException("No such symlink: " + linkfil);
        }

        // find the resource of the existing link
        String canstr = linkfil.getCanonicalPath();
        File canfil = new File(canstr);

        // rename the resource, thus breaking the link
        String parentStr = canfil.getParent();
        File parentDir = new File(parentStr);
        FileUtils fu = FileUtils.newFileUtils();
        File temp = fu.createTempFile("symlink", ".tmp", parentDir);
        temp.deleteOnExit();
        try {
            try {
                fu.rename(canfil, temp);
            } catch (IOException e) {
                throw new IOException("Couldn't rename resource when "
                                      + "attempting to delete " + linkfil);
            }

            // delete the (now) broken link
            if (!linkfil.delete()) {
                throw new IOException("Couldn't delete symlink: " + linkfil
                                      + " (was it a real file? is this not a "
                                      + "UNIX system?)");
            }
        } finally {
            // return the resource to its original name.
            try {
                fu.rename(temp, canfil);
            } catch (IOException e) {
                throw new IOException("Couldn't return resource " + temp
                                      + " to its original name: " + canstr
                                      + "\n THE RESOURCE'S NAME ON DISK HAS "
                                      + "BEEN CHANGED BY THIS ERROR!\n");
            }
        }
    }


    /* ********************************************************** *
      *                  Begin Private Methods                   *
     * ********************************************************** */

    /**
     * Writes a properties file.
     *
     * This method use <code>Properties.store</code>
     * and thus report exceptions that occur while writing the file.
     *
     * This is not jdk 1.1 compatible, but ant 1.6 is not anymore.
     *
     * @param properties     The properties object to be written.
     * @param propertyfile   The File to write to.
     * @param comment        The comment to place at the head of the file.
     */

    private void writePropertyFile(Properties properties,
                                   File propertyfile,
                                   String comment)
        throws BuildException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(propertyfile);
            properties.store(fos, comment);

        } catch (IOException ioe) {
            throw new BuildException(ioe, getLocation());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioex) {
                    log("Failed to close output stream");
                }
            }
        }
    }

    /**
     * Handles errors correctly based on the setting of failonerror.
     *
     * @param msg    The message to log, or include in the
     *                  <code>BuildException</code>
     */

    private void handleError(String msg) {
        if (failonerror) {
            throw new BuildException(msg);
        } else {
            log(msg);
        }
    }


    /**
     * Conducts the actual construction of a link.
     *
     * <p> The link is constructed by calling <code>Execute.runCommand</code>.
     *
     * @param resource   The path of the resource we are linking to.
     * @param link       The name of the link we wish to make
     */

    private void doLink(String resource, String link) throws BuildException {

        if (resource == null) {
            handleError("Must define the resource to symlink to!");
            return;
        }
        if (link == null) {
            handleError("Must define the link "
                        + "name for symlink!");
            return;
        }

        File linkfil = new File(link);

        String[] cmd = new String[4];
        cmd[0] = "ln";
        cmd[1] = "-s";
        cmd[2] = resource;
        cmd[3] = link;

        try {
            if (overwrite && linkfil.exists()) {
                deleteSymlink(linkfil);
            }
        } catch (FileNotFoundException fnfe) {
            handleError("Symlink dissapeared before it was deleted:" + link);
        } catch (IOException ioe) {
            handleError("Unable to overwrite preexisting link " + link);
        }

        log(cmd[0] + " " + cmd[1] + " " + cmd[2] + " " + cmd[3]);
        Execute.runCommand(this, cmd);
    }


    /**
     * Simultaneously get included directories and included files.
     *
     * @param ds   The scanner with which to get the files and directories.
     * @return     A vector of <code>String</code> objects containing the
     *                included file names and directory names.
     */

    private Vector scanDirsAndFiles(DirectoryScanner ds) {
        String[] files, dirs;
        Vector list = new Vector();

        ds.scan();

        files = ds.getIncludedFiles();
        dirs = ds.getIncludedDirectories();

        for (int i = 0; i < files.length; i++) {
            list.addElement(files[i]);
        }
        for (int i = 0; i < dirs.length; i++) {
            list.addElement(dirs[i]);
        }

        return list;
    }

    /**
     * Finds all the links in all supplied filesets.
     *
     * <p> This method is invoked when the action atribute is is "record".
     * This means that filesets are interpreted as the directories in
     * which links may be found.
     *
     * <p> The basic method follwed here is, for each file set:
     *   <ol>
     *      <li> Compile a list of all matches </li>
     *      <li> Convert matches to <code>File</code> objects </li>
     *      <li> Remove all non-symlinks using
     *             <code>FileUtils.isSymbolicLink</code> </li>
     *      <li> Convert all parent directories to the canonical form </li>
     *      <li> Add the remaining links from each file set to a
     *             master list of links unless the link is already recorded
     *             in the list</li>
     *   </ol>
     *
     * @param fileSets   The filesets specified by the user.
     * @return           A vector of <code>File</code> objects containing the
     *                     links (with canonical parent directories)
     */

    private Vector findLinks(Vector fileSets) {
        Vector result = new Vector();

        // loop through the supplied file sets
        FSLoop: for (int i = 0; i < fileSets.size(); i++) {
            FileSet fsTemp = (FileSet) fileSets.elementAt(i);
            String workingDir = null;
            Vector links = new Vector();
            Vector linksFiles = new Vector();
            Enumeration enumLinks;

            DirectoryScanner ds;

            File tmpfil = null;
            try {
                tmpfil = fsTemp.getDir(this.getProject());
                workingDir = tmpfil.getCanonicalPath();
            } catch (IOException ioe) {
                handleError("Exception caught getting "
                            + "canonical path of working dir " + tmpfil
                            + " in a FileSet passed to the symlink "
                            + "task. Further processing of this "
                            + "fileset skipped");
                continue FSLoop;
            }

            // Get a vector of String with file names that match
            // the pattern
            ds = fsTemp.getDirectoryScanner(this.getProject());
            links = scanDirsAndFiles(ds);

            // Now convert the strings to File Objects
            // using the canonical version of the working dir
            enumLinks = links.elements();

            while (enumLinks.hasMoreElements()) {
                linksFiles.addElement(new File(workingDir
                                               + File.separator
                                               + (String) enumLinks
                                               .nextElement()));
            }

            // Now loop through and remove the non-links

            enumLinks = linksFiles.elements();

            File parentNext, next;
            String nameParentNext;
            FileUtils fu = FileUtils.newFileUtils();
            Vector removals = new Vector();
            while (enumLinks.hasMoreElements()) {
                next = (File) enumLinks.nextElement();
                nameParentNext = next.getParent();

                parentNext = new File(nameParentNext);
                try {
                    if (!fu.isSymbolicLink(parentNext, next.getName())) {
                        removals.addElement(next);
                    }
                } catch (IOException ioe) {
                    handleError("Failed checking " + next
                                + " for symbolic link. FileSet skipped.");
                    continue FSLoop;
                    // Otherwise this file will be falsely recorded as a link,
                    // if failonerror = false, hence the warn and skip.
                }
            }

            enumLinks = removals.elements();

            while (enumLinks.hasMoreElements()) {
                linksFiles.removeElement(enumLinks.nextElement());
            }

            // Now we have what we want so add it to results, ensuring that
            // no link is returned twice and we have a canonical reference
            // to the link. (no symlinks in the parent dir)

            enumLinks = linksFiles.elements();
            while (enumLinks.hasMoreElements()) {
                File temp, parent;
                next = (File) enumLinks.nextElement();
                try {
                    parent = new File(next.getParent());
                    parent = new File(parent.getCanonicalPath());
                    temp = new File(parent, next.getName());
                    if (!result.contains(temp)) {
                        result.addElement(temp);
                    }
                } catch (IOException ioe) {
                    handleError("IOException: " + next + " omitted");
                }
            }

            // Note that these links are now specified with a full
            // canonical path irrespective of the working dir of the
            // file set so it is ok to mix them in the same vector.

        }
        return result;
    }

    /**
     * Load the links from a properties file.
     *
     * <p> This method is only invoked when the action atribute is set to
     * "multi". The filesets passed in are assumed to specify the names
     * of the property files with the link information and the
     * subdirectories in which to look for them.
     *
     * <p> The basic method follwed here is, for each file set:
     *   <ol>
     *      <li> Get the canonical version of the dir atribute </li>
     *      <li> Scan for properties files </li>
     *      <li> load the contents of each properties file found. </li>
     *   </ol>
     *
     * @param fileSets    The <code>FileSet</code>s for this task
     * @return            The links to be made.
     */

    private Properties loadLinks(Vector fileSets) {
        Properties finalList = new Properties();
        Enumeration keys;
        String key, value;
        String[] includedFiles;

        // loop through the supplied file sets
        FSLoop: for (int i = 0; i < fileSets.size(); i++) {
            String workingDir;
            FileSet fsTemp = (FileSet) fileSets.elementAt(i);

            DirectoryScanner ds;

            try {
                File linelength = fsTemp.getDir(this.getProject());
                workingDir = linelength.getCanonicalPath();
            } catch (IOException ioe) {
                handleError("Exception caught getting "
                            + "canonical path of working dir "
                            + "of a FileSet passed to symlink "
                            + "task. FileSet skipped.");
                continue FSLoop;
            }

            ds = fsTemp.getDirectoryScanner(this.getProject());
            ds.setFollowSymlinks(false);
            ds.scan();
            includedFiles = ds.getIncludedFiles();

            // loop through the files identified by each file set
            // and load their contents.
            for (int j = 0; j < includedFiles.length; j++) {
                File inc = new File(workingDir + File.separator
                                    + includedFiles[j]);
                String inDir;
                Properties propTemp = new Properties();

                try {
                    propTemp.load(new FileInputStream(inc));
                    inDir = inc.getParent();
                    inDir = (new File(inDir)).getCanonicalPath();
                } catch (FileNotFoundException fnfe) {
                    handleError("Unable to find " + includedFiles[j]
                                + "FileSet skipped.");
                    continue FSLoop;
                } catch (IOException ioe) {
                    handleError("Unable to open " + includedFiles[j]
                                + " or it's parent dir"
                                + "FileSet skipped.");
                    continue FSLoop;
                }

                keys = propTemp.keys();
                propTemp.list(System.out);
                // Write the contents to our master list of links
                // This method assumes that all links are defined in
                // terms of absolute paths, or paths relative to the
                // working directory
                while (keys.hasMoreElements()) {
                    key = (String) keys.nextElement();
                    value = propTemp.getProperty(key);
                    finalList.put(inDir + File.separator + key, value);
                }
            }
        }
        return finalList;
    }
}
